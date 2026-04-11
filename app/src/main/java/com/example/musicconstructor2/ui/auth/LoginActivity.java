package com.example.musicconstructor2.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.vk.id.AccessToken;
import com.vk.id.VKID;
import com.vk.id.VKIDAuthFail;
import com.vk.id.auth.AuthCodeData;
import com.vk.id.auth.VKIDAuthCallback;
import com.example.musicconstructor2.MainActivity;
import com.example.musicconstructor2.R;

public class LoginActivity extends AppCompatActivity {

    private MaterialButton btnVkLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    private final VKIDAuthCallback vkAuthCallback = new VKIDAuthCallback() {
        @Override
        public void onAuth(@NonNull AccessToken accessToken) {
            String token = accessToken.getToken();
            long userId  = accessToken.getUserID();

            saveVkSession(token, userId);
            loginFirebaseAnonymously();
        }

        @Override
        public void onFail(@NonNull VKIDAuthFail vkidAuthFail) {
            // ✅ При ошибке показываем кнопку
            showLoginButton();
            Toast.makeText(LoginActivity.this,
                    "Ошибка входа: " + vkidAuthFail.getClass().getSimpleName(),
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthCode(@NonNull AuthCodeData authCodeData, boolean b) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth       = FirebaseAuth.getInstance();
        btnVkLogin  = findViewById(R.id.btnVkLogin);
        progressBar = findViewById(R.id.progressBar);

        btnVkLogin.setOnClickListener(v -> startVkAuth());

        if (mAuth.getCurrentUser() != null) {
            checkVkToken();
        } else {
            // ✅ Явно показываем кнопку при первом входе
            showLoginButton();
        }
    }

    // ✅ Добавь onResume — восстанавливает кнопку
    // когда пользователь возвращается из VkAuthActivity
    @Override
    protected void onResume() {
        super.onResume();
        // Если Firebase не авторизован — показываем кнопку
        if (mAuth.getCurrentUser() == null) {
            showLoginButton();
        }
    }

    private void checkVkToken() {
        if (progressBar == null) return;

        String token = getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (token == null || token.isEmpty()) {
            showLoginButton();
            return;
        }

        showLoading(true);
        new Thread(() -> {
            try {
                String urlStr = "https://api.vk.com/method/users.get"
                        + "?access_token=" + token
                        + "&v=5.131";

                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                );
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String response = sb.toString();
                org.json.JSONObject json = new org.json.JSONObject(response);

                runOnUiThread(() -> {
                    showLoading(false);
                    if (json.has("error")) {
                        clearSession();
                        showLoginButton();
                    } else {
                        goToMain();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    goToMain();
                });
            }
        }).start();
    }

    private void startVkAuth() {
        Intent intent = new Intent(LoginActivity.this, VkAuthActivity.class);
        startActivity(intent);
    }

    private void loginFirebaseAnonymously() {
        mAuth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    showLoading(false);
                    Toast.makeText(this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    // Firebase не подключился — всё равно пускаем
                    goToMain();
                });
    }

    private void saveVkSession(String token, long userId) {
        getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .edit()
                .putString("access_token", token)
                .putLong("user_id", userId)
                .apply();
    }

    private void clearSession() {
        getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .edit().clear().apply();
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }
    }

    // ✅ Исправленный showLoginButton
    private void showLoginButton() {
        if (btnVkLogin == null || progressBar == null) return;
        progressBar.setVisibility(View.GONE);
        btnVkLogin.setVisibility(View.VISIBLE);
        btnVkLogin.setEnabled(true);
        btnVkLogin.setText("Войти через VK");
        btnVkLogin.setOnClickListener(v -> startVkAuth());
    }

    private void showLoading(boolean isLoading) {
        if (progressBar == null || btnVkLogin == null) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnVkLogin.setEnabled(!isLoading);
        btnVkLogin.setVisibility(View.VISIBLE); // ✅ кнопка всегда видима
        btnVkLogin.setText(isLoading ? "Выполняется вход..." : "Войти через VK");
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}