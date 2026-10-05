package com.hunik.fbweb;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {

    private static final String START_URL = "https://m.facebook.com/";

    private WebView web;
    private String injection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        FrameLayout root = new FrameLayout(this);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        try {
            String css = asset("fb.css");
            String b64 = Base64.encodeToString(css.getBytes("UTF-8"), Base64.NO_WRAP);
            injection = "(function(){var css=atob('" + b64 + "');" + asset("inject.js") + "})();";
        } catch (Exception e) {
            injection = null;
        }

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        // Force the phone layout (tablet UA lacks "Mobile").
        String ua = s.getUserAgentString();
        if (!ua.contains("Mobile")) ua = ua.replace(" Safari/", " Mobile Safari/");
        s.setUserAgentString(ua);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Restyle Facebook pages only.
                if (injection != null && url != null && url.contains("facebook.com")) {
                    view.evaluateJavascript(injection, null);
                }
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START_URL);
    }

    private String asset(String name) throws Exception {
        InputStream in = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    // Volume Up   = save page structure to fb-dom.txt (for tuning the CSS)
    // Volume Down = toggle the theme on/off (to compare)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            web.evaluateJavascript("window.__hunikToggle&&window.__hunikToggle()", null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            web.evaluateJavascript("window.__hunikDump?window.__hunikDump():''", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String v) {
                    try {
                        if (v == null || v.length() < 3) {
                            Toast.makeText(MainActivity.this, "Open Facebook first", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        v = v.substring(1, v.length() - 1)
                                .replace("\\n", "\n").replace("\\\"", "\"")
                                .replace("\\u003C", "<").replace("\\\\", "\\");
                        File f = new File(getExternalFilesDir(null), "fb-dom.txt");
                        FileOutputStream o = new FileOutputStream(f);
                        o.write(v.getBytes("UTF-8"));
                        o.close();
                        Toast.makeText(MainActivity.this, "Saved: " + f.getPath(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Dump failed: " + e, Toast.LENGTH_LONG).show();
                    }
                }
            });
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() { super.onPause(); web.onPause(); }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); }

    @Override
    protected void onDestroy() {
        ((ViewGroup) web.getParent()).removeAllViews();
        web.destroy();
        super.onDestroy();
    }
}
