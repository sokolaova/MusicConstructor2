package com.example.musicconstructor2.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.VkProfile;
import com.example.musicconstructor2.data.repository.VkNetworkRepository;
import com.example.musicconstructor2.data.repository.VkNetworkRepositoryImpl;
import com.example.musicconstructor2.ui.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private ImageView       ivAvatar;
    private TextView        tvFullName;
    private TextView        tvVkId;
    private TextView        tvPlaylistCount;
    private ProgressBar     progressBar;
    private MaterialButton  btnLogout;
    private View            layoutProfile;

    private VkNetworkRepository vkRepository;
    private String              vkToken;
    private long                userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        vkToken = getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .getString("access_token", "");
        userId  = getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .getLong("user_id", 0);

        vkRepository = new VkNetworkRepositoryImpl();

        initViews();
        setupBottomNav();
        loadProfile();
    }

    private void initViews() {
        ivAvatar       = findViewById(R.id.ivAvatar);
        tvFullName     = findViewById(R.id.tvFullName);
        tvVkId         = findViewById(R.id.tvVkId);
        tvPlaylistCount = findViewById(R.id.tvPlaylistCount);
        progressBar    = findViewById(R.id.progressBar);
        btnLogout      = findViewById(R.id.btnLogout);
        layoutProfile  = findViewById(R.id.layoutProfile);

        btnLogout.setOnClickListener(v -> logout());
    }

    // =========================================================
    //  Загрузка профиля через VK API
    // =========================================================
    private void loadProfile() {
        showLoading(true);

        vkRepository.getVkProfile(vkToken,
                new VkNetworkRepository.VkApiCallback<VkProfile>() {
                    @Override
                    public void onSuccess(VkProfile profile) {
                        showLoading(false);
                        displayProfile(profile);

                        // Сохраняем имя для приветствия в MainActivity
                        getSharedPreferences("vk_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_name", profile.getFullName())
                                .apply();
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        // Показываем данные из кэша если есть
                        loadCachedProfile();
                        Toast.makeText(ProfileActivity.this,
                                "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================================================
    //  Отображение профиля
    // =========================================================
    private void displayProfile(VkProfile profile) {
        layoutProfile.setVisibility(View.VISIBLE);

        tvFullName.setText(profile.getFullName());
        tvVkId.setText("ID: " + profile.getId());

        // Загрузка аватара через Glide
        if (profile.getPhoto() != null && !profile.getPhoto().isEmpty()) {
            Glide.with(this)
                    .load(profile.getPhoto())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivAvatar);
        }

        // Загружаем количество плейлистов
        loadPlaylistCount();
    }

    // =========================================================
    //  Загрузка кэшированного профиля из SharedPreferences
    // =========================================================
    private void loadCachedProfile() {
        String cachedName = getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .getString("user_name", "");

        if (!cachedName.isEmpty()) {
            layoutProfile.setVisibility(View.VISIBLE);
            tvFullName.setText(cachedName);
            tvVkId.setText("ID: " + userId);
        }
    }

    // =========================================================
    //  Количество плейлистов из Firestore
    // =========================================================
    private void loadPlaylistCount() {
        String userIdStr = String.valueOf(userId);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userIdStr)
                .collection("playlists")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = snapshot.size();
                    tvPlaylistCount.setText("Топов создано: " + count);
                })
                .addOnFailureListener(e ->
                        tvPlaylistCount.setText("Топов создано: 0"));
    }

    // =========================================================
    //  Выход из аккаунта
    // =========================================================
    private void logout() {
        // Очищаем VK сессию
        getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .edit().clear().apply();

        // Выходим из Firebase
        FirebaseAuth.getInstance().signOut();

        // Переходим на экран входа
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        layoutProfile.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(ProfileActivity.this,
                        com.example.musicconstructor2.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_search) {
                Intent intent = new Intent(ProfileActivity.this,
                        com.example.musicconstructor2.ui.search.SearchActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}