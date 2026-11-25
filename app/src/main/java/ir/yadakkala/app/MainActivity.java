package ir.yadakkala.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    // آدرس سایت (اگر آدرس عوض شد فقط همین دو خط را تغییر بده)
    private static final String SITE = "https://yadak-kala.ir/";
    private static final String HOST = "yadak-kala.ir";

    private static final String OFFLINE = "file:///android_asset/offline.html";

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                String host = u.getHost();
                if ("file".equals(scheme)) {
                    return false;
                }
                boolean http = "http".equals(scheme) || "https".equals(scheme);
                if (http && host != null && (host.equals(HOST) || host.endsWith("." + HOST))) {
                    return false; // لینک‌های خود سایت داخل اپ باز شوند
                }
                try {
                    // تلفن، واتس‌اپ، ایمیل، سایت‌های دیگر => بیرون از اپ
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadUrl(OFFLINE);
                }
            }
        });

        loadStart();
    }

    private void loadStart() {
        web.loadUrl(isOnline() ? SITE : OFFLINE);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    // پلی بین صفحه «اینترنت نیست» و اپ
    private class Bridge {
        @JavascriptInterface
        public void retry() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isOnline()) {
                        web.loadUrl(SITE);
                    } else {
                        web.evaluateJavascript("showHint()", null);
                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        String url = web.getUrl();
        boolean onOffline = url != null && url.startsWith("file:");
        if (!onOffline && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
