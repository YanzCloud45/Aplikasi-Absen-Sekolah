package com.yanzhost.absensekolah;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * MainActivity - WebView utama yang menampilkan website absen sekolah.
 * 
 * Fitur:
 * - WebView full screen dengan JavaScript enabled
 * - Cookie/session management agar login tetap tersimpan
 * - Swipe to refresh
 * - Progress bar loading
 * - Halaman error offline
 * - File upload support (untuk upload foto dll)
 * - Back button navigation dalam WebView
 */
public class MainActivity extends AppCompatActivity {

    private static final String WEB_URL = "https://yanzhost.wuaze.com";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout errorLayout;
    private ValueCallback<Uri[]> fileUploadCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Sembunyikan action bar untuk full screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inisialisasi views
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        errorLayout = findViewById(R.id.error_layout);
        Button retryButton = findViewById(R.id.btn_retry);

        // Setup WebView
        setupWebView();

        // Setup Swipe to Refresh
        swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.primary_dark),
                getResources().getColor(R.color.accent)
        );
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
        });

        // Tombol retry saat offline
        retryButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                errorLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.reload();
            } else {
                Toast.makeText(this, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });

        // Load website
        if (isNetworkAvailable()) {
            webView.loadUrl(WEB_URL);
        } else {
            showErrorPage();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Aktifkan JavaScript
        webSettings.setJavaScriptEnabled(true);

        // Aktifkan DOM Storage untuk session
        webSettings.setDomStorageEnabled(true);

        // Aktifkan database untuk local storage
        webSettings.setDatabaseEnabled(true);

        // Cache settings - agar lebih cepat loading
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());

        // Zoom settings
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Lainnya
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Media autoplay
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Mixed content (HTTP + HTTPS)
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // User Agent - tambahkan identifikasi APK
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " AbsenSekolahApp/1.0");

        // ===== COOKIE MANAGEMENT (Session Tetap Tersimpan) =====
        setupCookieManager();

        // ===== WebViewClient =====
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                // Sync cookies setelah halaman selesai loading
                CookieManager.getInstance().flush();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Buka URL eksternal di browser
                if (!url.contains("yanzhost.wuaze.com")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                return false; // Biarkan WebView yang handle
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showErrorPage();
                }
            }
        });

        // ===== WebChromeClient (Progress & File Upload) =====
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            // File upload support
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    Toast.makeText(MainActivity.this, "Tidak dapat membuka file chooser", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        // Scroll listener untuk swipe refresh (agar tidak konflik)
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            swipeRefresh.setEnabled(scrollY == 0);
        });
    }

    /**
     * Setup Cookie Manager agar login session tetap tersimpan
     * bahkan setelah aplikasi ditutup dan dibuka kembali
     */
    @SuppressWarnings("deprecation")
    private void setupCookieManager() {
        // Untuk Android < 5.0 (backward compatibility)
        CookieSyncManager.createInstance(this);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        // Untuk Android 5.0+ (Lollipop)
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Flush cookies
        cookieManager.flush();
    }

    /**
     * Tampilkan halaman error saat offline
     */
    private void showErrorPage() {
        webView.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);
    }

    /**
     * Cek apakah ada koneksi internet
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * Handle tombol back - navigasi mundur di WebView
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Handle hasil dari file chooser
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (fileUploadCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Sync cookies saat aplikasi di-pause
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync cookies saat aplikasi di-resume
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.clearCache(false); // Hanya hapus cache, bukan cookies
            webView.destroy();
        }
        super.onDestroy();
    }
}
