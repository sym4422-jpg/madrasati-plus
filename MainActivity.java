package sa.madrasati.plus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends Activity {
    private static final String HOME = "https://schools.madrasati.sa/";
    private static final int PICK_FILE = 501;
    private WebView web;
    private ValueCallback<Uri[]> pendingFiles;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout toolbar = new LinearLayout(this);
        Button back = new Button(this); back.setText("رجوع");
        Button refresh = new Button(this); refresh.setText("تحديث");
        Button browser = new Button(this); browser.setText("فتح في المتصفح");
        toolbar.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        toolbar.addView(refresh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        toolbar.addView(browser, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        root.addView(toolbar);
        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)) return false;
                if ("http".equalsIgnoreCase(scheme)) { openExternal(uri); return true; }
                if ("intent".equalsIgnoreCase(scheme)) {
                    try { Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME); intent.addCategory(Intent.CATEGORY_BROWSABLE); intent.setComponent(null); intent.setSelector(null); startActivity(intent); }
                    catch (Exception e) { Toast.makeText(MainActivity.this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show(); }
                    return true;
                }
                if ("tel".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) { openExternal(uri); return true; }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                try { startActivityForResult(params.createIntent(), PICK_FILE); }
                catch (ActivityNotFoundException ex) { pendingFiles = null; callback.onReceiveValue(null); Toast.makeText(MainActivity.this, "لا يوجد تطبيق لاختيار الملفات", Toast.LENGTH_SHORT).show(); }
                return true;
            }
        });
        CookieManager.getInstance().setAcceptCookie(true);
        // Third-party cookies may be required by federated login; the identity provider may still block embedded browsers.
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setDownloadListener((url, userAgent, disposition, mime, size) -> {
            if (!url.startsWith("https://")) { Toast.makeText(this, "رابط تنزيل غير مدعوم", Toast.LENGTH_SHORT).show(); return; }
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mime);
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) == null ? "" : CookieManager.getInstance().getCookie(url));
                request.addRequestHeader("User-Agent", userAgent);
                request.setTitle(URLUtil.guessFileName(url, disposition, mime));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, disposition, mime));
                ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "بدأ تنزيل الملف", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { openExternal(Uri.parse(url)); }
        });
        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); });
        refresh.setOnClickListener(v -> web.reload());
        browser.setOnClickListener(v -> openExternal(Uri.parse(web.getUrl() == null ? HOME : web.getUrl())));
        web.loadUrl(HOME);
    }
    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
        catch (Exception e) { Toast.makeText(this, "تعذر فتح الرابط الخارجي", Toast.LENGTH_SHORT).show(); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && pendingFiles != null) {
            pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            pendingFiles = null;
        }
    }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if (pendingFiles != null) pendingFiles.onReceiveValue(null); web.destroy(); super.onDestroy(); }
}
