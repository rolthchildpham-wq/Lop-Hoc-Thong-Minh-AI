package vn.aihuedan.learn;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String LIVE_URL = "https://aihuedanlearn.edgeone.dev/";
    private static final String LOCAL_URL = "file:///android_asset/www/index.html";
    private WebView webView;
    private PermissionRequest pendingWebPermission;
    private static final int REQ_WEB_PERMS = 71;

    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private String pendingTtsText = null;
    private String pendingTtsLang = "vi-VN";
    private String pendingTtsId = null;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        getWindow().setStatusBarColor(0xFF07101F);
        getWindow().setNavigationBarColor(0xFF07101F);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF07101F);
        installSafeAreaInsets(webView);
        setContentView(webView);

        initNativeTts();

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setTextZoom(100);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        webView.addJavascriptInterface(new NativeVoiceBridge(), "AndroidVoice");

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                String url = view.getUrl();
                if (req.isForMainFrame() && (url == null || !url.startsWith("file:///android_asset/"))) {
                    Toast.makeText(MainActivity.this, "Mất mạng — chuyển sang chế độ học offline", Toast.LENGTH_SHORT).show();
                    view.loadUrl(LOCAL_URL);
                }
            }

            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectNativeVoiceHook();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }
        });

        if (isOnline()) webView.loadUrl(LIVE_URL); else webView.loadUrl(LOCAL_URL);
    }

    private void installSafeAreaInsets(View v) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        v.requestApplyInsets();
    }

    private void initNativeTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { }
                    @Override public void onDone(String utteranceId) { notifyJsTtsDone(utteranceId); }
                    @Override public void onError(String utteranceId) { notifyJsTtsDone(utteranceId); }
                });
                if (pendingTtsText != null) {
                    String txt = pendingTtsText, lang = pendingTtsLang, id = pendingTtsId;
                    pendingTtsText = null; pendingTtsId = null;
                    speakNative(txt, lang, id);
                }
            }
        });
    }

    private Locale localeFromTag(String tag) {
        if (tag == null) return Locale.forLanguageTag("vi-VN");
        String t = tag.toLowerCase(Locale.ROOT);
        if (t.startsWith("en")) return Locale.US;
        if (t.startsWith("zh")) return Locale.SIMPLIFIED_CHINESE;
        return Locale.forLanguageTag("vi-VN");
    }

    private void speakNative(String text, String lang, String utteranceId) {
        if (text == null || text.trim().isEmpty()) {
            notifyJsTtsDone(utteranceId);
            return;
        }
        if (!ttsReady || tts == null) {
            pendingTtsText = text;
            pendingTtsLang = lang;
            pendingTtsId = utteranceId;
            return;
        }
        Locale locale = localeFromTag(lang);
        int res = tts.setLanguage(locale);
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault());
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId == null ? "native_tts" : utteranceId);
    }

    private void notifyJsTtsDone(String id) {
        if (webView == null || id == null) return;
        String safe = id.replace("\\", "\\\\").replace("'", "\\'");
        runOnUiThread(() -> webView.evaluateJavascript("window.__nativeTtsDone && window.__nativeTtsDone('" + safe + "');", null));
    }

    private void injectNativeVoiceHook() {
        if (webView == null) return;
        String js = "(function(){" +
                "if(window.__nativeTtsInstalled||!window.AndroidVoice)return;" +
                "window.__nativeTtsInstalled=true;window.__nativeTtsMap={};" +
                "window.__nativeTtsDone=function(id){var u=window.__nativeTtsMap[id];if(!u)return;delete window.__nativeTtsMap[id];try{if(u.onend)u.onend({type:'end'});}catch(e){}};" +
                "var s=window.speechSynthesis;if(!s)return;" +
                "s.speak=function(u){try{var id='utt_'+Date.now()+'_'+Math.random().toString(36).slice(2);window.__nativeTtsMap[id]=u;if(u.onstart)u.onstart({type:'start'});AndroidVoice.speak(String(u.text||''),String(u.lang||'vi-VN'),id);}catch(e){try{if(u.onerror)u.onerror(e);}catch(_){}}};" +
                "s.cancel=function(){try{AndroidVoice.cancel();}catch(e){};for(var k in window.__nativeTtsMap){try{var u=window.__nativeTtsMap[k];if(u.onend)u.onend({type:'end'});}catch(e){}}window.__nativeTtsMap={};};" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    public class NativeVoiceBridge {
        @JavascriptInterface public void speak(String text, String lang, String id) {
            runOnUiThread(() -> speakNative(text, lang, id));
        }
        @JavascriptInterface public void cancel() {
            runOnUiThread(() -> { if (tts != null) tts.stop(); });
        }
        @JavascriptInterface public boolean isReady() { return ttsReady; }
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
    @Override protected void onPause() { super.onPause(); if (webView != null) webView.onPause(); if (tts != null) tts.stop(); }
    @Override protected void onResume() { super.onResume(); if (webView != null) webView.onResume(); }
    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
