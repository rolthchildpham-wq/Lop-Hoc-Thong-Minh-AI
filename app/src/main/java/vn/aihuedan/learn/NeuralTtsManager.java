package vn.aihuedan.learn;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.GenerationConfig;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NeuralTtsManager {
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile OfflineTts englishTts;
    private volatile OfflineTts chineseTts;
    private volatile AudioTrack currentTrack;
    private volatile long generation = 0;

    public NeuralTtsManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean supports(String lang) {
        if (lang == null) return false;
        String l = lang.toLowerCase();
        return l.startsWith("en") || l.startsWith("zh");
    }

    public void speak(String text, String lang, Runnable onDone, Runnable onError) {
        if (text == null || text.trim().isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }

        final long token = ++generation;

        executor.execute(() -> {
            try {
                stopTrackOnly();

                boolean chinese = lang != null && lang.toLowerCase().startsWith("zh");
                OfflineTts engine = chinese ? getChineseTts() : getEnglishTts();

                GenerationConfig gc = new GenerationConfig();
                gc.setSpeed(1.0f);
                gc.setSilenceScale(0.18f);
                gc.setSid(chinese ? 48 : 0);

                GeneratedAudio audio = engine.generateWithConfigAndCallback(
                        text,
                        gc,
                        samples -> token == generation ? 1 : 0
                );

                if (token != generation) return;

                play(audio.getSamples(), audio.getSampleRate(), token);

                if (token == generation && onDone != null) onDone.run();
            } catch (Throwable t) {
                t.printStackTrace();
                if (token == generation && onError != null) onError.run();
            }
        });
    }

    public void cancel() {
        ++generation;
        stopTrackOnly();
    }

    private synchronized OfflineTts getEnglishTts() {
        if (englishTts != null) return englishTts;

        OfflineTtsVitsModelConfig vits = new OfflineTtsVitsModelConfig();
        vits.setModel("tts/en/en_US-lessac-medium.onnx");
        vits.setTokens("tts/en/tokens.txt");
        vits.setLexicon("tts/en/lexicon.txt");
        vits.setLengthScale(1.0f);

        OfflineTtsModelConfig model = new OfflineTtsModelConfig();
        model.setVits(vits);
        model.setNumThreads(2);
        model.setDebug(false);
        model.setProvider("cpu");

        OfflineTtsConfig cfg = new OfflineTtsConfig();
        cfg.setModel(model);
        cfg.setSilenceScale(0.18f);

        englishTts = new OfflineTts(context.getAssets(), cfg);
        return englishTts;
    }

    private synchronized OfflineTts getChineseTts() {
        if (chineseTts != null) return chineseTts;

        OfflineTtsKokoroModelConfig kokoro = new OfflineTtsKokoroModelConfig();
        kokoro.setModel("tts/zh/model.onnx");
        kokoro.setVoices("tts/zh/voices.bin");
        kokoro.setTokens("tts/zh/tokens.txt");
        kokoro.setLexicon("tts/zh/lexicon-us-en.txt,tts/zh/lexicon-zh.txt");
        kokoro.setLang("zh");
        kokoro.setLengthScale(1.0f);

        OfflineTtsModelConfig model = new OfflineTtsModelConfig();
        model.setKokoro(kokoro);
        model.setNumThreads(4);
        model.setDebug(false);
        model.setProvider("cpu");

        OfflineTtsConfig cfg = new OfflineTtsConfig();
        cfg.setModel(model);
        cfg.setRuleFsts("tts/zh/date-zh.fst,tts/zh/phone-zh.fst,tts/zh/number-zh.fst");
        cfg.setSilenceScale(0.18f);

        chineseTts = new OfflineTts(context.getAssets(), cfg);
        return chineseTts;
    }

    private void play(float[] samples, int sampleRate, long token) {
        if (samples == null || samples.length == 0 || token != generation) return;

        int min = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
        );

        int buffer = Math.max(min, 32768);

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                )
                .setAudioFormat(
                        new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                )
                .setBufferSizeInBytes(buffer)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        currentTrack = track;
        track.play();

        int offset = 0;
        while (offset < samples.length && token == generation) {
            int n = Math.min(8192, samples.length - offset);
            int written = track.write(samples, offset, n, AudioTrack.WRITE_BLOCKING);
            if (written <= 0) break;
            offset += written;
        }

        try { track.stop(); } catch (Exception ignored) {}
        try { track.release(); } catch (Exception ignored) {}

        if (currentTrack == track) currentTrack = null;
    }

    private synchronized void stopTrackOnly() {
        AudioTrack t = currentTrack;
        currentTrack = null;

        if (t != null) {
            try { t.pause(); } catch (Exception ignored) {}
            try { t.flush(); } catch (Exception ignored) {}
            try { t.stop(); } catch (Exception ignored) {}
            try { t.release(); } catch (Exception ignored) {}
        }
    }

    public void release() {
        cancel();

        executor.execute(() -> {
            try {
                if (englishTts != null) englishTts.release();
            } catch (Throwable ignored) {}

            try {
                if (chineseTts != null) chineseTts.release();
            } catch (Throwable ignored) {}

            englishTts = null;
            chineseTts = null;
        });

        executor.shutdown();
    }
}
