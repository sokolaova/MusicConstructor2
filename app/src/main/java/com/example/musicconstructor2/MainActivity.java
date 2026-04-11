package com.example.musicconstructor2;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.data.model.Playlist;
import com.example.musicconstructor2.data.model.VkProfile;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.example.musicconstructor2.data.repository.VkNetworkRepository;
import com.example.musicconstructor2.data.repository.VkNetworkRepositoryImpl;
import com.example.musicconstructor2.ui.search.SearchActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView             tvGreeting, tvUsername;
    private RecyclerView         rvPlaylists, rvRecentPlaylists;
    private BottomNavigationView bottomNav;
    private FirebaseAuth         mAuth;

    private String               userId;
    private String               vkToken;
    private String               currentPlaylistId = null;
    private PlaylistRepository   repository;
    private VkNetworkRepository  vkRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }

        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        // Получаем сохранённые данные VK сессии
        userId   = String.valueOf(
                getSharedPreferences("vk_prefs", MODE_PRIVATE)
                        .getLong("user_id", 0));
        vkToken  = getSharedPreferences("vk_prefs", MODE_PRIVATE)
                .getString("access_token", "");

        repository   = new PlaylistRepository(userId);
        vkRepository = new VkNetworkRepositoryImpl();

        initViews();
        setupGreeting();
        setupBottomNav();
        loadUserProfile();
        loadPlaylists();
    }

    private void initViews() {
        tvGreeting        = findViewById(R.id.tvGreeting);
        tvUsername        = findViewById(R.id.tvUsername);
        rvPlaylists       = findViewById(R.id.rvPlaylists);
        rvRecentPlaylists = findViewById(R.id.rvRecentPlaylists);
        bottomNav         = findViewById(R.id.bottomNav);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        rvRecentPlaylists.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        findViewById(R.id.ivAvatar).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,
                    com.example.musicconstructor2.ui.profile.ProfileActivity.class);
            startActivity(intent);
        });
        // Кнопка добавить плейлист
        findViewById(R.id.btnAdd).setOnClickListener(v -> showCreatePlaylistDialog());
    }

    // Приветствие по времени суток
    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if      (hour >= 5  && hour < 12) greeting = "Доброе утро!";
        else if (hour >= 12 && hour < 17) greeting = "Добрый день!";
        else if (hour >= 17 && hour < 22) greeting = "Добрый вечер!";
        else                              greeting = "Доброй ночи!";
        tvGreeting.setText(greeting);
    }

    // Загружаем имя и аватар из VK
    private void loadUserProfile() {
        if (vkToken.isEmpty()) return;

        vkRepository.getVkProfile(vkToken,
                new VkNetworkRepository.VkApiCallback<VkProfile>() {
                    @Override
                    public void onSuccess(VkProfile profile) {
                        // Показываем имя пользователя
                        tvUsername.setText(profile.getFirstName() + "'s топы");

                        // Сохраняем имя для использования в других экранах
                        getSharedPreferences("vk_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_name", profile.getFullName())
                                .putString("user_photo", profile.getPhoto())
                                .apply();
                    }

                    @Override
                    public void onError(String error) {
                        // Имя не загрузилось — ставим дефолтное
                        tvUsername.setText("Твои топы");
                    }
                });
    }

    // Загружаем плейлисты из Firestore
    private void loadPlaylists() {
        repository.getPlaylists(new PlaylistRepository.Callback<List<Playlist>>() {
            @Override
            public void onSuccess(List<Playlist> playlists) {
                if (!playlists.isEmpty()) {
                    currentPlaylistId = playlists.get(0).getId();
                }
                // TODO: передать в адаптер когда будет готов PlaylistAdapter
                // playlistAdapter.setPlaylists(playlists);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this,
                        "Ошибка загрузки: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreatePlaylistDialog() {
        // Создаем кастомный диалог
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_create_playlist);

        // Настраиваем размеры и прозрачный фон
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Находим элементы
        android.widget.EditText input = dialog.findViewById(R.id.inputPlaylistName);
        android.widget.TextView btnCreate = dialog.findViewById(R.id.btnCreate);
        android.widget.TextView btnCancel = dialog.findViewById(R.id.btnCancel);

        // Обработчики
        btnCreate.setOnClickListener(v -> {
            String title = input.getText().toString().trim();
            if (!title.isEmpty()) {
                createPlaylist(title);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void createPlaylist(String title) {
        Playlist playlist = new Playlist(title, "", userId);
        repository.createPlaylist(playlist, new PlaylistRepository.Callback<String>() {
            @Override
            public void onSuccess(String playlistId) {
                currentPlaylistId = playlistId;
                Toast.makeText(MainActivity.this,
                        "Топ создан", Toast.LENGTH_SHORT).show();
                loadPlaylists(); // обновляем список
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this,
                        "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_search) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra("playlist_id", currentPlaylistId);
                intent.putExtra("vk_token", vkToken);
                startActivity(intent);
                return true;
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent(MainActivity.this,
                    com.example.musicconstructor2.ui.profile.ProfileActivity.class);
            startActivity(intent);
            return true;
        }
            return false;
        });
    }
}