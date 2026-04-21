package com.example.musicconstructor2.ui.player;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.RatingInfo;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.repository.PlaylistRepository;
import com.example.musicconstructor2.service.MusicPlayerService;
import com.example.musicconstructor2.service.MusicPlayerServiceHolder;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PlayerActivity extends AppCompatActivity
        implements MusicPlayerService.PlayerListener {

    private ImageView ivCover;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private SeekBar seekBarProgress, seekBarVolume;
    private ImageView btnPlay, btnNext, btnPrevious;
    private MaterialButton btnAddToPlaylist;
    private MusicPlayerService playerService;
    private boolean isBound = false;

    // Рейтинг
    private LinearLayout layoutRatingStars;
    private LinearLayout layoutVotingStars;
    private TextView tvAverageRating;
    private TextView tvRatingCount;
    private TextView tvUserRating;

    private PlaylistRepository repository;
    private String userId;

    private static final int MAX_RATING = 10;
    private static final int STAR_COUNT = 10;
    private float currentTrackRating = 0f;
    private String currentTrackId;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            playerService = binder.getService();
            playerService.setPlayerListener(PlayerActivity.this);
            isBound = true;

            // Передаём плейлист из Holder сразу после подключения
            if (MusicPlayerServiceHolder.tracks != null && !MusicPlayerServiceHolder.tracks.isEmpty()) {
                playerService.setPlaylist(
                        MusicPlayerServiceHolder.tracks,
                        MusicPlayerServiceHolder.startIndex
                );
                // Очищаем после передачи
                MusicPlayerServiceHolder.tracks = null;
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

        // Получаем userId из SharedPreferences
        userId = String.valueOf(
                getSharedPreferences("vk_prefs", MODE_PRIVATE)
                        .getLong("user_id", 0)
        );

        repository = new PlaylistRepository(userId);

        initViews();
        initRatingViews();
        setupClickListeners();

        // Запускаем и привязываем сервис
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, BIND_AUTO_CREATE);
    }

    private void initViews() {
        ivCover = findViewById(R.id.ivCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        seekBarProgress = findViewById(R.id.seekBarProgress);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnAddToPlaylist = findViewById(R.id.btnAddToPlaylist);

        seekBarVolume.setProgress(80);

        // Кнопка назад
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initRatingViews() {
        layoutRatingStars = findViewById(R.id.layoutRatingStars);
        layoutVotingStars = findViewById(R.id.layoutVotingStars);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvUserRating = findViewById(R.id.tvUserRating);

        // Создаём звёзды для отображения среднего рейтинга
        createStars(layoutRatingStars, true);

        // Создаём интерактивные звёзды для голосования
        createVotingStars();
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
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) {
                    playerService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Громкость
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isBound) {
                    float volume = progress / 100f;
                    playerService.setVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnAddToPlaylist.setOnClickListener(v -> {
            if (!isBound) return;
            Track current = playerService.getCurrentTrack();
            if (current == null) {
                Toast.makeText(this, "Нет активного трека", Toast.LENGTH_SHORT).show();
                return;
            }

            PlaylistPickerDialog dialog = new PlaylistPickerDialog(
                    PlayerActivity.this,
                    current,
                    userId
            );
            dialog.show();
        });
    }

    private void createStars(LinearLayout container, boolean isReadOnly) {
        container.removeAllViews();

        for (int i = 0; i < STAR_COUNT; i++) {
            ImageView star = new ImageView(this);
            int size = isReadOnly ? 60 : 66; // ✅ Меньше для отображения, больше для голосования
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(2, 0, 2, 0); // ✅ Меньшие отступы для 10 звёзд
            star.setLayoutParams(params);
            star.setImageResource(R.drawable.ic_star_empty);
            container.addView(star);
        }
    }
    private void createVotingStars() {
        layoutVotingStars.removeAllViews();

        for (int i = 0; i < STAR_COUNT; i++) {
            ImageView star = new ImageView(this);
            int size = 80;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(3, 0, 3, 0);
            star.setLayoutParams(params);
            star.setImageResource(R.drawable.ic_star_empty);

            // ✅ Делаем звезду кликабельной и фокусируемой
            star.setClickable(true);
            star.setFocusable(true);
            star.setLongClickable(true); // ✅ Важно для долгого нажатия

            final int starIndex = i;
            star.setOnClickListener(v -> {
                float rating = starIndex + 1;
                submitRating(rating);
            });

            // ✅ Добавляем отдельный слушатель долгого нажатия на каждую звезду
            star.setOnLongClickListener(v -> {
                showResetRatingDialog();
                return true; // ✅ Возвращаем true, что событие обработано
            });

            layoutVotingStars.addView(star);
        }

        // ✅ Также оставляем слушатель на всём контейнере для надёжности
        layoutVotingStars.setLongClickable(true);
        layoutVotingStars.setOnLongClickListener(v -> {
            showResetRatingDialog();
            return true;
        });
    }

    private void loadTrackRating(String trackId) {
        if (repository == null) {
            Toast.makeText(this, "Репозиторий не инициализирован", Toast.LENGTH_SHORT).show();
            return;
        }

        this.currentTrackId = trackId;

        repository.getTrackRating(trackId, new PlaylistRepository.Callback<RatingInfo>() {
            @Override
            public void onSuccess(RatingInfo ratingInfo) {
                tvAverageRating.setText(ratingInfo.getFormattedAverage());
                tvRatingCount.setText(ratingInfo.getRatingText());
                updateStarsDisplay(ratingInfo.getAverageRating());

                if (ratingInfo.hasUserRated()) {
                    currentTrackRating = ratingInfo.getUserRating();
                    tvUserRating.setText("Ваша оценка: " + (int)ratingInfo.getUserRating() + "/10");
                    updateStars(layoutVotingStars, ratingInfo.getUserRating());
                } else {
                    currentTrackRating = 0f;
                    tvUserRating.setText("Нажмите на звёзды");
                    resetVotingStars();
                }
            }

            @Override
            public void onError(String error) {
                // ✅ Если трек ещё не имеет оценок - это нормально
                currentTrackRating = 0f;
                tvAverageRating.setText("0.0");
                tvRatingCount.setText("0 оценок");
                tvUserRating.setText("Нажмите на звёзды");
                resetVotingStars();
                updateStars(layoutRatingStars, 0f);
            }
        });
    }


    private void updateStarsDisplay(float rating) {
        // ✅ rating уже от 0 до 10, не нужно делить
        updateStars(layoutRatingStars, rating);

        // Обновляем интерактивные звёзды (если пользователь уже оценил)
        if (currentTrackRating > 0) {
            updateStars(layoutVotingStars, currentTrackRating);
        }
    }

    private void updateStars(LinearLayout container, float rating) {
        int fullStars = (int) rating;
        boolean hasHalfStar = (rating - fullStars) >= 0.5f;

        for (int i = 0; i < container.getChildCount(); i++) {
            ImageView star = (ImageView) container.getChildAt(i);

            if (i < fullStars) {
                star.setImageResource(R.drawable.ic_star_filled);
            } else if (i == fullStars && hasHalfStar) {
                star.setImageResource(R.drawable.ic_star_half);
            } else {
                star.setImageResource(R.drawable.ic_star_empty);
            }
        }
    }


    private void submitRating(float rating) {
        if (currentTrackId == null || currentTrackId.isEmpty()) {
            Toast.makeText(this, "Трек не загружен", Toast.LENGTH_SHORT).show();
            return;
        }

        if (repository == null) {
            Toast.makeText(this, "Репозиторий не инициализирован", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.rateTrack(currentTrackId, rating, new PlaylistRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(PlayerActivity.this,
                        "Оценка сохранена: " + (int)rating + "/10",
                        Toast.LENGTH_SHORT).show();

                // Обновляем отображение
                loadTrackRating(currentTrackId);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PlayerActivity.this,
                        "Ошибка сохранения: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void resetVotingStars() {
        for (int i = 0; i < layoutVotingStars.getChildCount(); i++) {
            ImageView star = (ImageView) layoutVotingStars.getChildAt(i);
            star.setImageResource(R.drawable.ic_star_empty);
        }
    }
    private void showResetRatingDialog() {
        // Проверяем, есть ли оценка для сброса
        if (currentTrackRating == 0) {
            Toast.makeText(this, "Вы ещё не оценили этот трек", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаём кастомный диалог
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_confirm_reset_rating);

        // Настраиваем размеры и прозрачный фон
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnReset = dialog.findViewById(R.id.btnReset);

        // Обновляем сообщение с текущей оценкой
        tvMessage.setText("Ваша оценка " + (int)currentTrackRating + "/10 будет удалена.\nСредний рейтинг трека обновится.");

        // Обработчики
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnReset.setOnClickListener(v -> {
            dialog.dismiss();

            if (repository == null) {
                Toast.makeText(PlayerActivity.this,
                        "Репозиторий не инициализирован", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.removeTrackRating(currentTrackId, new PlaylistRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(PlayerActivity.this,
                            "Оценка удалена", Toast.LENGTH_SHORT).show();
                    currentTrackRating = 0f;
                    resetVotingStars();
                    tvUserRating.setText("Нажмите на звёзды");
                    loadTrackRating(currentTrackId);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(PlayerActivity.this,
                            "Ошибка: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
    // =========================================================
    //  PlayerListener callbacks
    // =========================================================
    @Override
    public void onTrackChanged(Track track, int index) {
        runOnUiThread(() -> {
            updateTrackInfo(track);
            // ✅ Загружаем рейтинг при смене трека
            if (track != null && track.getId() != null) {
                loadTrackRating(track.getId());
            }
        });
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
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    private void updateTrackInfo(Track track) {
        tvTitle.setText(track.getTitle() != null ? track.getTitle() : "Без названия");
        tvArtist.setText(track.getArtist() != null ? track.getArtist() : "Неизвестный исполнитель");
        tvTotalTime.setText(track.getFormattedDuration());
        seekBarProgress.setMax(track.getDuration() * 1000);

        if (track.getCoverUrl() != null && !track.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .load(track.getCoverUrl())
                    .placeholder(R.drawable.ic_default_cover)
                    .error(R.drawable.ic_default_cover)
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