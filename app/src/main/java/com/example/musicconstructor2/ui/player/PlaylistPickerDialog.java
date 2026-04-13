package com.example.musicconstructor2.ui.player;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Playlist;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;

import java.util.List;

public class PlaylistPickerDialog extends Dialog {

    private final Track              track;
    private final PlaylistRepository repository;
    private       ProgressBar        progressBar;
    private       RecyclerView       rvPlaylists;
    private       TextView           tvEmpty;
    private       ImageView          btnClose;

    public PlaylistPickerDialog(@NonNull Context context,
                                Track track,
                                String userId) {
        super(context);
        this.track      = track;
        this.repository = new PlaylistRepository(userId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_playlist_picker);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Инициализация элементов
        progressBar = findViewById(R.id.progressBar);
        rvPlaylists = findViewById(R.id.rvPlaylists);
        tvEmpty     = findViewById(R.id.tvEmpty);
        btnClose    = findViewById(R.id.btnClose);

        // Настройка RecyclerView
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPlaylists.setHasFixedSize(true);

        // Обработчик закрытия
        btnClose.setOnClickListener(v -> dismiss());

        // Загружаем плейлисты
        loadPlaylists();
    }

    private void loadPlaylists() {
        showLoading(true);

        repository.getPlaylists(new PlaylistRepository.Callback<List<Playlist>>() {
            @Override
            public void onSuccess(List<Playlist> playlists) {
                showLoading(false);

                if (playlists == null || playlists.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvPlaylists.setVisibility(View.GONE);
                    return;
                }

                tvEmpty.setVisibility(View.GONE);
                rvPlaylists.setVisibility(View.VISIBLE);

                PlaylistPickerAdapter adapter = new PlaylistPickerAdapter(
                        playlists,
                        playlist -> addTrackToPlaylist(playlist)
                );
                rvPlaylists.setAdapter(adapter);
                adapter.notifyDataSetChanged(); // Принудительное обновление
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Ошибка загрузки: " + error);
                rvPlaylists.setVisibility(View.GONE);
                Toast.makeText(getContext(),
                        "Ошибка загрузки топов: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTrackToPlaylist(Playlist playlist) {
        android.util.Log.d("PlaylistPicker", "Добавление трека в плейлист: " + playlist.getTitle());
        android.util.Log.d("PlaylistPicker", "ID плейлиста: " + playlist.getId());
        android.util.Log.d("PlaylistPicker", "ID трека: " + track.getId());
        android.util.Log.d("PlaylistPicker", "Название трека: " + track.getTitle());

        if (playlist == null || playlist.getId() == null) {
            android.util.Log.e("PlaylistPicker", "Ошибка: плейлист или ID = null");
            Toast.makeText(getContext(),
                    "Ошибка: топ не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        rvPlaylists.setEnabled(false);
        track.setPosition(playlist.getTrackCount() + 1);

        repository.addTrackToPlaylist(
                playlist.getId(),
                track,
                new PlaylistRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        android.util.Log.d("PlaylistPicker", "✅ Трек успешно добавлен!");
                        Toast.makeText(getContext(),
                                "Добавлено в «" + playlist.getTitle() + "»",
                                Toast.LENGTH_SHORT).show();
                        dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.e("PlaylistPicker", "❌ Ошибка добавления: " + error);
                        rvPlaylists.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Ошибка добавления: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }


    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (rvPlaylists != null) {
            rvPlaylists.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
    }
}