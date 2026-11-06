package vn.aihuedan.learn;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.GenerationConfig;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
                OfflineTts engine = lang != null && lang.toLowerCase().startsWith("zh")
                        ? getChineseTts() : getEnglishTts();

                GenerationConfig gc = new GenerationConfig();
                gc.setSpeed(1.0f);
                gc.setSilenceScale(0.18f);
                gc.setSid(lang != null && lang.toLowerCase().startsWith("zh") ? 3 : 0);

                GeneratedAudio audio = engine.generateWithConfigAndCallback(text, gc, samples -> {
                    return token == generation ? 1 : 0;
                });
                if (token != generation) return;
                play(audio.getSamples(), audio.getSampleRate(), token);
                if (token == generation && onDone != null) onDone.run();
            } catch (Throwable t) {
                if (token == generation && onError != null) onError.run();
            }
        });
    }

    public void cancel() {
        ++generation;
        stopTrackOnly();
    }

    private synchronized OfflineTts getEnglishTts() throws Exception {
        if (englishTts != null) return englishTts;
        OfflineTtsVitsModelConfig vits = OfflineTtsVitsModelConfig.builder()
                .setModel("tts/en/en_US-lessac-medium.onnx")
                .setTokens("tts/en/tokens.txt")
                .setDataDir("tts/zh/espeak-ng-data")
                .build();
        OfflineTtsModelConfig model = OfflineTtsModelConfig.builder()
                .setVits(vits).setNumThreads(2).setDebug(false).build();
        OfflineTtsConfig cfg = OfflineTtsConfig.builder().setModel(model).build();
        englishTts = new OfflineTts(context.getAssets(), cfg);
        return englishTts;
    }

    private synchronized OfflineTts getChineseTts() throws Exception {
        if (chineseTts != null) return chineseTts;
        OfflineTtsKokoroModelConfig kokoro = OfflineTtsKokoroModelConfig.builder()
                .setModel("tts/zh/model.onnx")
                .setVoices("tts/zh/voices.bin")
                .setTokens("tts/zh/tokens.txt")
                .setLexicon("tts/zh/lexicon-us-en.txt,tts/zh/lexicon-zh.txt")
                .setDataDir("tts/en/espeak-ng-data")
                .build();
        OfflineTtsModelConfig model = OfflineTtsModelConfig.builder()
                .setKokoro(kokoro).setNumThreads(2).setDebug(false).build();
        OfflineTtsConfig cfg = OfflineTtsConfig.builder().setModel(model)
                .setRuleFsts("tts/zh/date-zh.fst,tts/zh/phone-zh.fst,tts/zh/number-zh.fst")
                .build();
        chineseTts = new OfflineTts(context.getAssets(), cfg);
        return chineseTts;
    }

    private void play(float[] samples, int sampleRate, long token) {
        if (samples == null || samples.length == 0 || token != generation) return;
        int min = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT);
        int buffer = Math.max(min, Math.min(samples.length * 4, 1024 * 1024));

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
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
        if (token == generation) {
            try { track.stop(); } catch (Exception ignored) {}
        }
        track.release();
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

    private String copyAssetDirToFiles(String assetDir) throws Exception {
        File outDir = new File(context.getFilesDir(), assetDir);
        if (outDir.exists() && new File(outDir, ".ready").exists()) {
            return outDir.getAbsolutePath();
        }
        copyRecursive(assetDir, outDir);
        outDir.mkdirs();
        new File(outDir, ".ready").createNewFile();
        return outDir.getAbsolutePath();
    }

    private void copyRecursive(String assetPath, File out) throws Exception {
        String[] children = context.getAssets().list(assetPath);
        if (children == null || children.length == 0) {
            out.getParentFile().mkdirs();
            try (InputStream in = context.getAssets().open(assetPath);
                 FileOutputStream fos = new FileOutputStream(out)) {
                byte[] buf = new byte[32768];
                int n;
                while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            }
            return;
        }
        out.mkdirs();
        for (String child : children) {
            copyRecursive(assetPath + "/" + child, new File(out, child));
        }
    }

    public void release() {
        cancel();
        executor.execute(() -> {
            try { if (englishTts != null) englishTts.release(); } catch (Throwable ignored) {}
            try { if (chineseTts != null) chineseTts.release(); } catch (Throwable ignored) {}
            englishTts = null;
            chineseTts = null;
        });
        executor.shutdown();
    }
}
