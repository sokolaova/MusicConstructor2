package com.example.musicconstructor2.ui.player;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.service.MusicPlayerService;
import com.example.musicconstructor2.service.MusicPlayerServiceHolder;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PlayerActivity extends AppCompatActivity
        implements MusicPlayerService.PlayerListener {

    private ImageView  ivCover;
    private TextView   tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar    seekBarProgress, seekBarVolume;
    private ImageView  btnPlay, btnNext, btnPrevious;
    private MaterialButton btnAddToPlaylist;
    private MusicPlayerService playerService;
    private boolean            isBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder =
                    (MusicPlayerService.MusicBinder) service;
            playerService = binder.getService();
            playerService.setPlayerListener(PlayerActivity.this);
            isBound = true;

            // Передаём плейлист из Holder сразу после подключения
            if (MusicPlayerServiceHolder.tracks != null
                    && !MusicPlayerServiceHolder.tracks.isEmpty()) {
                playerService.setPlaylist(
                        MusicPlayerServiceHolder.tracks,
                        MusicPlayerServiceHolder.startIndex
                );
                // Очищаем после передачи
                MusicPlayerServiceHolder.tracks     = null;
                MusicPlayerServiceHolder.startIndex = 0;
            } else {
                // Плейлист уже установлен — просто обновляем UI
                Track current = playerService.getCurrentTrack();
                if (current != null) {
                    updateTrackInfo(current);
                    updatePlayButton(playerService.isPlaying());
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        setupClickListeners();

        // Запускаем и привязываем сервис
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, BIND_AUTO_CREATE);
    }

    private void initViews() {
        ivCover         = findViewById(R.id.ivCover);
        tvTitle         = findViewById(R.id.tvTitle);
        tvArtist        = findViewById(R.id.tvArtist);
        tvCurrentTime   = findViewById(R.id.tvCurrentTime);
        tvTotalTime     = findViewById(R.id.tvTotalTime);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        seekBarVolume   = findViewById(R.id.seekBarVolume);
        btnPlay         = findViewById(R.id.btnPlay);
        btnNext         = findViewById(R.id.btnNext);
        btnPrevious     = findViewById(R.id.btnPrevious);
        btnAddToPlaylist = findViewById(R.id.btnAddToPlaylist);
        seekBarVolume.setProgress(80);
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v -> {
            if (!isBound) return;
            if (playerService.isPlaying()) {
                playerService.pause();
            } else {
                playerService.resume();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) playerService.next();
        });

        btnPrevious.setOnClickListener(v -> {
            if (isBound) playerService.previous();
        });

        // Перемотка
        seekBarProgress.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        if (fromUser && isBound) {
                            playerService.seekTo(progress);
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

        // Громкость
        seekBarVolume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        if (isBound) {
                            float volume = progress / 100f;
                            playerService.setVolume(volume);
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
        btnAddToPlaylist.setOnClickListener(v -> {
            if (!isBound) return;
            Track current = playerService.getCurrentTrack();
            if (current == null) {
                Toast.makeText(this,
                        "Нет активного трека", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = String.valueOf(
                    getSharedPreferences("vk_prefs", MODE_PRIVATE)
                            .getLong("user_id", 0)
            );

            // ✅ Открываем диалог выбора плейлиста
            PlaylistPickerDialog dialog = new PlaylistPickerDialog(
                    PlayerActivity.this,
                    current,
                    userId
            );
            dialog.show();
        });
        // Кнопка назад
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // =========================================================
    //  Публичный метод для запуска воспроизведения из другого экрана
    // =========================================================
    public static void startWithPlaylist(android.content.Context context,
                                         List<Track> tracks, int index) {
        // Сохраняем плейлист в static поле сервиса
        MusicPlayerServiceHolder.tracks      = tracks;
        MusicPlayerServiceHolder.startIndex  = index;

        Intent intent = new Intent(context, PlayerActivity.class);
        context.startActivity(intent);
    }

    // =========================================================
    //  PlayerListener callbacks
    // =========================================================
    @Override
    public void onTrackChanged(Track track, int index) {
        runOnUiThread(() -> updateTrackInfo(track));
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> updatePlayButton(isPlaying));
    }

    @Override
    public void onProgressChanged(int currentMs, int totalMs) {
        runOnUiThread(() -> {
            seekBarProgress.setMax(totalMs);
            seekBarProgress.setProgress(currentMs);
            tvCurrentTime.setText(formatTime(currentMs));
            tvTotalTime.setText(formatTime(totalMs));
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() ->
                android.widget.Toast.makeText(this, error,
                        android.widget.Toast.LENGTH_SHORT).show());
    }

    private void updateTrackInfo(Track track) {
        tvTitle.setText(track.getTitle());
        tvArtist.setText(track.getArtist());
        tvTotalTime.setText(track.getFormattedDuration());
        seekBarProgress.setMax(track.getDuration() * 1000);

        if (track.getCoverUrl() != null && !track.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .load(track.getCoverUrl())
                    .placeholder(R.drawable.ic_default_cover)
                    .into(ivCover);
        } else {
            ivCover.setImageResource(R.drawable.ic_default_cover);
        }
    }

    private void updatePlayButton(boolean isPlaying) {
        btnPlay.setImageResource(isPlaying
                ? R.drawable.ic_pause
                : R.drawable.ic_play);
    }

    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            playerService.setPlayerListener(null);
            unbindService(connection);
            isBound = false;
        }
    }
}