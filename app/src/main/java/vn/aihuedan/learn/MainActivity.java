package vn.aihuedan.learn;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String LIVE_URL = "https://aihuedanlearn.edgeone.dev/";
    private static final String OFFLINE_URL = "file:///android_asset/www/offline.html";
    private static final int REQ_WEB_PERMS = 71;
    private static final int REQ_TTS_CHECK = 72;

    private FrameLayout root;
    private WebView webView;
    private PermissionRequest pendingWebPermission;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private NeuralTtsManager neuralTts;

    private String pendingText = null;
    private String pendingLang = "vi-VN";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF07101F);
        getWindow().setNavigationBarColor(0xFF07101F);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);

        root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        applySystemBarInsets();
        neuralTts = new NeuralTtsManager(this);
        initTts();
        configureWebView();

        if (isOnline()) webView.loadUrl(LIVE_URL); else webView.loadUrl(OFFLINE_URL);
    }

    private void applySystemBarInsets() {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) webView.getLayoutParams();
            lp.setMargins(left, top, right, bottom);
            webView.setLayoutParams(lp);
            return insets;
        });
        root.requestApplyInsets();
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setTextZoom(100);
        s.setUseWideViewPort(true);
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.addJavascriptInterface(new NativeVoiceBridge(), "AndroidVoice");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String host = u.getHost();
                if (host != null && host.endsWith("aihuedanlearn.edgeone.dev")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                injectMobilePatch();
                injectNativeVoiceHook();
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame() && !OFFLINE_URL.equals(view.getUrl())) {
                    Toast.makeText(MainActivity.this, "Mất mạng — chuyển sang trang offline", Toast.LENGTH_SHORT).show();
                    view.loadUrl(OFFLINE_URL);
                }
            }
        });
    }

    private void injectMobilePatch() {
        String js = "(function(){var id='apk-mobile-safe-patch';if(document.getElementById(id))return;var s=document.createElement('style');s.id=id;s.textContent='html,body{max-width:100%;overflow-x:hidden!important;}*{box-sizing:border-box;}img,video,canvas,svg{max-width:100%;}.bottom-console,.input-bar,.bottom-bar{max-width:100vw!important;}@media(max-width:700px){body{width:100%!important;min-width:0!important}.app,.main,.main-container{max-width:100%!important;min-width:0!important}}';document.head.appendChild(s);})();";
        webView.evaluateJavascript(js, null);
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);
                if (pendingText != null) {
                    String text = pendingText;
                    String lang = pendingLang;
                    pendingText = null;
                    speakNative(text, lang);
                }
            } else {
                Toast.makeText(this, "Không khởi tạo được giọng đọc Android", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void injectNativeVoiceHook() {
        String js = "(function(){if(window.__apkTtsHook||!window.AndroidVoice)return;window.__apkTtsHook=true;var ss=window.speechSynthesis;if(!ss)return;var oldSpeak=ss.speak.bind(ss);ss.speak=function(u){try{AndroidVoice.speak(String(u.text||''),String(u.lang||'vi-VN'));if(u.onstart)u.onstart({type:'start'});setTimeout(function(){try{if(u.onend)u.onend({type:'end'});}catch(e){}},Math.max(900,String(u.text||'').length*60));}catch(e){oldSpeak(u);}};ss.cancel=function(){try{AndroidVoice.cancel();}catch(e){}};})();";
        webView.evaluateJavascript(js, null);
    }

    public class NativeVoiceBridge {
        @JavascriptInterface public void speak(String text, String lang) { runOnUiThread(() -> speakNative(text, lang)); }
        @JavascriptInterface public void cancel() { runOnUiThread(() -> { if (neuralTts != null) neuralTts.cancel(); if (tts != null) tts.stop(); }); }
        @JavascriptInterface public boolean isReady() { return ttsReady; }
        @JavascriptInterface public boolean hasEnglish() { return true; }
        @JavascriptInterface public boolean hasChinese() { return true; }
        @JavascriptInterface public void installVoices() { }
    }

    private void speakNative(String text, String lang) {
        if (text == null || text.trim().isEmpty()) return;

        String l = lang == null ? "vi-VN" : lang.toLowerCase(Locale.ROOT);

        if (neuralTts != null && neuralTts.supports(l)) {
            neuralTts.speak(text, l,
                    () -> { },
                    () -> runOnUiThread(() -> speakSystemFallback(text, l)));
            return;
        }

        speakSystemFallback(text, l);
    }

    private void speakSystemFallback(String text, String lang) {
        if (!ttsReady || tts == null) {
            pendingText = text;
            pendingLang = lang;
            return;
        }

        Locale locale = Locale.forLanguageTag("vi-VN");
        if (lang.startsWith("en")) locale = Locale.US;
        else if (lang.startsWith("zh")) locale = Locale.SIMPLIFIED_CHINESE;

        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault());
        }
        chooseBestVoice(locale);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "teacher");
    }

    private void chooseBestVoice(Locale locale) {
        if (tts == null || Build.VERSION.SDK_INT < 21) return;
        try {
            Set<Voice> voices = tts.getVoices();
            Voice best = null;
            int bestScore = Integer.MIN_VALUE;

            for (Voice v : voices) {
                if (v == null || v.getLocale() == null) continue;
                String wantedLang = locale.getLanguage();
                if (!wantedLang.equalsIgnoreCase(v.getLocale().getLanguage())) continue;

                int score = 0;
                if (locale.getCountry().equalsIgnoreCase(v.getLocale().getCountry())) score += 50;
                if (!v.isNetworkConnectionRequired()) score += 30;
                score += v.getQuality() * 5;
                score -= v.getLatency();

                String name = v.getName() == null ? "" : v.getName().toLowerCase(Locale.ROOT);
                if (name.contains("neural")) score += 40;
                if (name.contains("natural")) score += 35;
                if (name.contains("premium")) score += 30;

                if (score > bestScore) {
                    bestScore = score;
                    best = v;
                }
            }

            if (best != null) tts.setVoice(best);
        } catch (Exception ignored) {}
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities c = cm.getNetworkCapabilities(n);
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void handleWebPermission(PermissionRequest request) {
        boolean mic = false, cam = false;
        for (String r : request.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) mic = true;
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) cam = true;
        }

        boolean needMic = mic && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
        boolean needCam = cam && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;

        if (needMic || needCam) {
            pendingWebPermission = request;
            java.util.ArrayList<String> ps = new java.util.ArrayList<>();
            if (needMic) ps.add(Manifest.permission.RECORD_AUDIO);
            if (needCam) ps.add(Manifest.permission.CAMERA);
            requestPermissions(ps.toArray(new String[0]), REQ_WEB_PERMS);
        } else request.grant(request.getResources());
    }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] g) {
        super.onRequestPermissionsResult(rc, p, g);
        if (rc == REQ_WEB_PERMS && pendingWebPermission != null) {
            boolean all = true;
            for (int x : g) if (x != PackageManager.PERMISSION_GRANTED) all = false;
            if (all) pendingWebPermission.grant(pendingWebPermission.getResources()); else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
        if (neuralTts != null) neuralTts.cancel();
        if (tts != null) tts.stop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override protected void onDestroy() {
        if (neuralTts != null) { neuralTts.release(); neuralTts = null; }
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
