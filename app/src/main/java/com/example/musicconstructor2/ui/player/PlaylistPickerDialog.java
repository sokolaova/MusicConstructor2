package com.example.musicconstructor2.ui.player;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

import java.util.ArrayList;
import java.util.List;

public class PlaylistPickerDialog extends Dialog {

    private final Track            track;
    private final PlaylistRepository repository;
    private final String           userId;
    private       ProgressBar      progressBar;
    private       RecyclerView     rvPlaylists;
    private       TextView         tvEmpty;

    public PlaylistPickerDialog(@NonNull Context context,
                                Track track,
                                String userId) {
        super(context);
        this.track      = track;
        this.userId     = userId;
        this.repository = new PlaylistRepository(userId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_playlist_picker);

        // Скругляем углы диалога
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
            getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        progressBar = findViewById(R.id.progressBar);
        rvPlaylists = findViewById(R.id.rvPlaylists);
        tvEmpty     = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));

        loadPlaylists();
    }

    private void loadPlaylists() {
        showLoading(true);

        repository.getPlaylists(new PlaylistRepository.Callback<List<Playlist>>() {
            @Override
            public void onSuccess(List<Playlist> playlists) {
                showLoading(false);
                if (playlists.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvPlaylists.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvPlaylists.setVisibility(View.VISIBLE);
                    rvPlaylists.setAdapter(
                            new PlaylistPickerAdapter(playlists, playlist -> {
                                addTrackToPlaylist(playlist);
                            })
                    );
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(getContext(),
                        "Ошибка загрузки топов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTrackToPlaylist(Playlist playlist) {
        // Устанавливаем позицию в конец списка
        track.setPosition(playlist.getTrackCount() + 1);

        repository.addTrackToPlaylist(playlist.getId(), track,
                new PlaylistRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(getContext(),
                                "✓ Добавлено в «" + playlist.getTitle() + "»",
                                Toast.LENGTH_SHORT).show();
                        dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(),
                                "Ошибка: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvPlaylists.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }
}