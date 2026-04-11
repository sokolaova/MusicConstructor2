package com.example.musicconstructor2.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicconstructor2.MainActivity;
import com.example.musicconstructor2.R;
import com.google.firebase.auth.FirebaseAuth;

public class VkAuthActivity extends AppCompatActivity {

    private static final String AUTH_URL =
            "https://oauth.vk.com/authorize" +
                    "?client_id=6463690" +
                    "&scope=1073737727" +
                    "&redirect_uri=https://oauth.vk.com/blank.html" +
                    "&display=mobile" +   // ✅ mobile вместо page
                    "&response_type=token" +
                    "&revoke=1";

    private boolean tokenProcessed = false;
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vk_auth);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                checkUrl(url);
            }
        });

        // ✅ Ловим изменение URL через WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                String url = view.getUrl();
                if (url != null) {
                    checkUrl(url);
                }
            }
        });

        webView.loadUrl(AUTH_URL);
    }

    private void checkUrl(String url) {
        if (tokenProcessed || url == null) return;

        if (url.contains("access_token=")) {
            tokenProcessed = true;
            // Останавливаем загрузку страницы с предупреждением
            webView.stopLoading();
            webView.loadUrl("about:blank");
            processToken(url);
        }
    }

    private void processToken(String url) {
        try {
            String fragment;
            if (url.contains("#")) {
                fragment = url.substring(url.indexOf("#") + 1);
            } else if (url.contains("?")) {
                fragment = url.substring(url.indexOf("?") + 1);
            } else {
                finish();
                return;
            }

            String[] params   = fragment.split("&");
            String accessToken = null;
            long   userId      = 0;

            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    switch (pair[0]) {
                        case "access_token":
                            accessToken = pair[1];
                            break;
                        case "user_id":
                            try {
                                userId = Long.parseLong(pair[1]);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            }

            if (accessToken != null && !accessToken.isEmpty()) {
                saveSession(accessToken, userId);
                loginFirebaseAndGoMain();
            } else {
                finish();
            }

        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private void saveSession(String token, long userId) {
        getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .edit()
                .putString("access_token", token)
                .putLong("user_id", userId)
                .apply();
    }

    private void loginFirebaseAndGoMain() {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(task -> goToMain());
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}