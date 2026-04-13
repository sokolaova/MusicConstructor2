package com.example.musicconstructor2.ui.home;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.example.musicconstructor2.service.MusicPlayerServiceHolder;
import com.example.musicconstructor2.ui.player.PlayerActivity;
import com.example.musicconstructor2.ui.search.TrackAdapter;

import java.util.ArrayList;
import java.util.List;

public class PlaylistViewActivity extends AppCompatActivity {

    private TextView tvTitle, tvEmpty;
    private RecyclerView rvTracks;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private TrackAdapter trackAdapter;

    private String playlistId;
    private String playlistTitle;
    private String userId;
    private PlaylistRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_view);

        // Получаем данные из Intent
        playlistId = getIntent().getStringExtra("playlist_id");
        playlistTitle = getIntent().getStringExtra("playlist_title");

        userId = String.valueOf(
                getSharedPreferences("vk_prefs", MODE_PRIVATE)
                        .getLong("user_id", 0)
        );

        repository = new PlaylistRepository(userId);

        initViews();
        loadPlaylistTracks();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvTracks = findViewById(R.id.rvTracks);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        tvTitle.setText(playlistTitle != null ? playlistTitle : "Плейлист");

        rvTracks.setLayoutManager(new LinearLayoutManager(this));

        // Создаём адаптер для треков
        trackAdapter = new TrackAdapter(userId, (track, position) -> {
            // Переход в плеер при клике на трек
            MusicPlayerServiceHolder.tracks = new ArrayList<>(trackAdapter.getTracks());
            MusicPlayerServiceHolder.startIndex = position;
            startActivity(new android.content.Intent(this, PlayerActivity.class));
        });
        rvTracks.setAdapter(trackAdapter);

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadPlaylistTracks() {
        if (playlistId == null || playlistId.isEmpty()) {
            showEmpty();
            return;
        }

        showLoading(true);

        // Исправить в PlaylistViewActivity.java строку 88-91:
        repository.getPlaylistTracks(playlistId,
                new PlaylistRepository.Callback<List<Track>>() {
                    @Override
                    public void onSuccess(List<Track> tracks) {
                        showLoading(false);

                        // ✅ Синхронизируем trackCount в БД с реальным количеством треков
                        if (tracks != null && !tracks.isEmpty()) {
                            repository.syncPlaylistTrackCount(playlistId,
                                    new PlaylistRepository.Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void result) {
                                            // Успешно синхронизировано
                                        }

                                        @Override
                                        public void onError(String error) {
                                        }
                                    });
                        }

                        if (tracks == null || tracks.isEmpty()) {
                            showEmpty();
                        } else {
                            showTracks(tracks);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showEmpty();
                        Toast.makeText(PlaylistViewActivity.this,
                                "Ошибка загрузки: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
        private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvTracks.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showTracks(List<Track> tracks) {
        rvTracks.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        trackAdapter.setTracks(tracks);
    }

    private void showEmpty() {
        rvTracks.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        tvEmpty.setText("В этом плейлисте нет треков");
    }
}
