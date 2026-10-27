
package sa.madrasati.plus;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String HOME =
            "https://schools.madrasati.sa/";
    private static final int PICK_FILE = 501;

    private WebView web;
    private ProgressBar progress;
    private TextView status;
    private ValueCallback<Uri[]> pendingFiles;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Keep the app below the system status bar
        getWindow().clearFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().setStatusBarColor(
            Color.rgb(25, 75, 135)
        );
        getWindow().getDecorView()
            .setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(
            Color.rgb(235, 242, 250)
        );

        Button back = new Button(this);
        back.setText("رجوع");

        Button refresh = new Button(this);
        refresh.setText("تحديث");

        Button browser = new Button(this);
        browser.setText("فتح في المتصفح");

        toolbar.addView(back,
            new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(refresh,
            new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(browser,
            new LinearLayout.LayoutParams(0, -2, 2));

        root.addView(toolbar);

        progress = new ProgressBar(
            this, null,
            android.R.attr.progressBarStyleHorizontal
        );
        progress.setMax(100);
        root.addView(progress,
            new LinearLayout.LayoutParams(-1, 5));

        status = new TextView(this);
        status.setText("جاري فتح منصة مدرستي...");
        status.setTextSize(14);
        status.setPadding(16, 8, 16, 8);
        root.addView(status);

        web = new WebView(this);
        root.addView(web,
            new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        );

        CookieManager.getInstance()
            .setAcceptCookie(true);
        CookieManager.getInstance()
            .setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    android.graphics.Bitmap favicon) {
                status.setText("جاري تحميل الصفحة...");
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view, String url) {
                progress.setVisibility(View.GONE);
                if (!status.getText().toString()
                        .startsWith("تعذر")) {
                    status.setText("منصة مدرستي");
                }
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {
                if (request.isForMainFrame()) {
                    status.setText(
                        "تعذر تحميل الصفحة. اضغط تحديث أو افتحها في المتصفح."
                    );
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if ("https".equalsIgnoreCase(scheme))
                    return false;

                if ("http".equalsIgnoreCase(scheme)) {
                    openExternal(uri);
                    return true;
                }

                if ("intent".equalsIgnoreCase(scheme)) {
                    try {
                        Intent intent = Intent.parseUri(
                            uri.toString(),
                            Intent.URI_INTENT_SCHEME
                        );
                        intent.addCategory(
                            Intent.CATEGORY_BROWSABLE
                        );
                        intent.setComponent(null);
                        intent.setSelector(null);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(
                            MainActivity.this,
                            "تعذر فتح الرابط",
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                    return true;
                }

                if ("tel".equalsIgnoreCase(scheme)
                    || "mailto".equalsIgnoreCase(scheme)) {
                    openExternal(uri);
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(
                    WebView view, int newProgress) {
                progress.setProgress(newProgress);
            }

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (pendingFiles != null)
                    pendingFiles.onReceiveValue(null);

                pendingFiles = callback;

                try {
                    startActivityForResult(
                        params.createIntent(), PICK_FILE
                    );
                } catch (ActivityNotFoundException e) {
                    pendingFiles = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(
                        MainActivity.this,
                        "تعذر اختيار الملف",
                        Toast.LENGTH_SHORT
                    ).show();
                }
                return true;
            }
        });

        web.setDownloadListener(
            (url, userAgent, disposition, mime, size) -> {
                if (!url.startsWith("https://")) {
                    Toast.makeText(this,
                        "رابط تنزيل غير مدعوم",
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    String filename =
                        URLUtil.guessFileName(
                            url, disposition, mime
                        );

                    DownloadManager.Request request =
                        new DownloadManager.Request(
                            Uri.parse(url)
                        );

                    request.setMimeType(mime);
                    String cookies =
                        CookieManager.getInstance()
                            .getCookie(url);
                    if (cookies != null)
                        request.addRequestHeader(
                            "Cookie", cookies
                        );
                    if (userAgent != null)
                        request.addRequestHeader(
                            "User-Agent", userAgent
                        );

                    request.setTitle(filename);
                    request.setNotificationVisibility(
                        DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    );
                    request.setDestinationInExternalFilesDir(
                        this,
                        Environment.DIRECTORY_DOWNLOADS,
                        filename
                    );

                    ((DownloadManager)
                        getSystemService(DOWNLOAD_SERVICE))
                        .enqueue(request);

                    Toast.makeText(this,
                        "بدأ تنزيل الملف",
                        Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    openExternal(Uri.parse(url));
                }
            }
        );

        back.setOnClickListener(v -> {
            if (web.canGoBack()) web.goBack();
        });

        refresh.setOnClickListener(v -> {
            status.setText("جاري إعادة التحميل...");
            web.reload();
        });

        browser.setOnClickListener(v -> {
            String url = web.getUrl();
            openExternal(Uri.parse(
                url == null ? HOME : url
            ));
        });

        if (state == null) {
            web.loadUrl(HOME);
        } else {
            web.restoreState(state);
        }
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(
                new Intent(Intent.ACTION_VIEW, uri)
                    .addCategory(
                        Intent.CATEGORY_BROWSABLE
                    )
            );
        } catch (Exception e) {
            Toast.makeText(this,
                "تعذر فتح الرابط الخارجي",
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        web.saveState(out);
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(
            requestCode, resultCode, data
        );

        if (requestCode == PICK_FILE
                && pendingFiles != null) {
            pendingFiles.onReceiveValue(
                WebChromeClient.FileChooserParams
                    .parseResult(resultCode, data)
            );
            pendingFiles = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (pendingFiles != null) {
            pendingFiles.onReceiveValue(null);
            pendingFiles = null;
        }
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
