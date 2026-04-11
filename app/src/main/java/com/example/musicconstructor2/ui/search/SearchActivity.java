package com.example.musicconstructor2.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.MainActivity;
import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.example.musicconstructor2.data.repository.VkNetworkRepository;
import com.example.musicconstructor2.data.repository.VkNetworkRepositoryImpl;
import com.example.musicconstructor2.service.MusicPlayerServiceHolder;
import com.example.musicconstructor2.ui.player.PlayerActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private ProgressBar       progressBar;
    private LinearLayout      layoutEmpty;
    private RecyclerView      rvResults;
    private TrackAdapter      adapter;

    private VkNetworkRepository  vkRepository;
    private PlaylistRepository   repository;
    private String               playlistId;
    private String               vkToken;

    // Все треки пользователя — ищем локально по ним
    private List<Track> allTracks = new ArrayList<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Получаем данные из Intent
        playlistId = getIntent().getStringExtra("playlist_id");
        vkToken    = getIntent().getStringExtra("vk_token");

        // Если токен не пришёл через Intent — берём из SharedPreferences
        if (vkToken == null || vkToken.isEmpty()) {
            vkToken = getSharedPreferences("vk_prefs", MODE_PRIVATE)
                    .getString("access_token", "");
        }

        String userId = String.valueOf(
                getSharedPreferences("vk_prefs", MODE_PRIVATE)
                        .getLong("user_id", 0));

        vkRepository = new VkNetworkRepositoryImpl();
        repository   = new PlaylistRepository(userId);

        initViews();
        loadUserTracks(); // загружаем все треки при открытии
    }

    private void initViews() {
        etSearch    = findViewById(R.id.etSearch);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        rvResults   = findViewById(R.id.rvSearchResults);

        // ✅ Сначала создаём адаптер
        adapter = new TrackAdapter(new TrackAdapter.OnTrackClickListener() {
            @Override
            public void onAddClick(Track track) {
                addTrackToPlaylist(track);
            }

            @Override
            public void onPlayClick(Track track, int position) {
                MusicPlayerServiceHolder.tracks     = new ArrayList<>(adapter.getTracks());
                MusicPlayerServiceHolder.startIndex = position;
                Intent intent = new Intent(SearchActivity.this, PlayerActivity.class);
                startActivity(intent);
            }
        });

        // ✅ Потом привязываем к RecyclerView
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        // Кнопка назад
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_search);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(SearchActivity.this, MainActivity.class);
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (id == R.id.nav_search) {
                return true;
            } else if (id == R.id.nav_profile) {
                Intent intent = new Intent(SearchActivity.this,
                        com.example.musicconstructor2.ui.profile.ProfileActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        // Поиск по введённому тексту
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    showResults(allTracks);
                    return;
                }

                searchRunnable = () -> filterTracks(query);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });

        // Поиск по Enter
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                filterTracks(query);
                return true;
            }
            return false;
        });
    }

    // =========================================================
    //  Загружаем все треки пользователя через audio.get
    // =========================================================
    private void loadUserTracks() {
        if (vkToken.isEmpty()) {
            showEmpty();
            Toast.makeText(this, "Токен VK не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        vkRepository.getVkMusic(vkToken, 200,
                new VkNetworkRepository.VkApiCallback<List<Track>>() {
                    @Override
                    public void onSuccess(List<Track> tracks) {
                        allTracks = tracks;
                        showLoading(false);

                        if (tracks.isEmpty()) {
                            showEmpty();
                        } else {
                            showResults(tracks); // показываем все треки сразу
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showEmpty();

                        // Токен истёк — перелогин
                        if (error != null && error.contains("access_token")) {
                            Toast.makeText(SearchActivity.this,
                                    "Сессия истекла, войдите снова",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SearchActivity.this,
                                    "Ошибка загрузки: " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // =========================================================
    //  Локальный поиск по загруженным трекам
    // =========================================================
    private void filterTracks(String query) {
        if (allTracks.isEmpty()) return;

        List<Track> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Track track : allTracks) {
            if (track.getTitle().toLowerCase().contains(lowerQuery) ||
                    track.getArtist().toLowerCase().contains(lowerQuery)) {
                filtered.add(track);
            }
        }

        if (filtered.isEmpty()) {
            showEmpty();
        } else {
            showResults(filtered);
        }
    }

    // =========================================================
    //  Добавляем трек в плейлист Firestore
    // =========================================================
    private void addTrackToPlaylist(Track track) {
        if (playlistId == null || playlistId.isEmpty()) {
            Toast.makeText(this,
                    "Сначала создай топ на главном экране",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        repository.addTrackToPlaylist(playlistId, track,
                new PlaylistRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(SearchActivity.this,
                                "✓ " + track.getArtist() + " — " + track.getTitle(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(SearchActivity.this,
                                "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================================================
    //  Состояния UI
    // =========================================================
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            rvResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showResults(List<Track> tracks) {
        rvResults.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        adapter.setTracks(tracks);
    }

    private void showEmpty() {
        rvResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchHandler.removeCallbacksAndMessages(null);
    }
}