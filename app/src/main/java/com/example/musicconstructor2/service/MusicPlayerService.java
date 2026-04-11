package com.example.musicconstructor2.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.musicconstructor2.R;
import com.example.musicconstructor2.data.model.Track;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService extends Service {

    private static final String TAG             = "MusicPlayerService";
    private static final String CHANNEL_ID      = "music_player_channel";
    private static final int    NOTIFICATION_ID = 1;

    // Действия для уведомления
    public static final String ACTION_PLAY     = "ACTION_PLAY";
    public static final String ACTION_PAUSE    = "ACTION_PAUSE";
    public static final String ACTION_NEXT     = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_STOP     = "ACTION_STOP";

    private MediaPlayer    mediaPlayer;
    private List<Track>    playlist = new ArrayList<>();
    private int            currentIndex = 0;
    private boolean        isPrepared   = false;
    private PlayerListener listener;

    // Binder для связи с Activity
    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    // =========================================================
    //  Интерфейс для обратной связи с UI
    // =========================================================
    public interface PlayerListener {
        void onTrackChanged(Track track, int index);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressChanged(int currentMs, int totalMs);
        void onError(String error);
    }

    public void setPlayerListener(PlayerListener listener) {
        this.listener = listener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:     resume();  break;
                case ACTION_PAUSE:    pause();   break;
                case ACTION_NEXT:     next();    break;
                case ACTION_PREVIOUS: previous(); break;
                case ACTION_STOP:     stopSelf(); break;
            }
        }
        return START_NOT_STICKY;
    }

    // =========================================================
    //  Установка плейлиста
    // =========================================================
    public void setPlaylist(List<Track> tracks, int startIndex) {
        if (tracks == null || tracks.isEmpty()) return;

        this.playlist     = new ArrayList<>(tracks);
        this.currentIndex = Math.max(0,
                Math.min(startIndex, tracks.size() - 1)); // ✅ защита от выхода за пределы

        playCurrentTrack();
    }

    // =========================================================
    //  Воспроизведение текущего трека
    // =========================================================
    private void playCurrentTrack() {
        if (playlist.isEmpty()) return;

        Track track = playlist.get(currentIndex);

        if (track.getUrl() == null || track.getUrl().isEmpty()) {
            if (listener != null) listener.onError("Трек недоступен");
            return;
        }

        // Останавливаем предыдущий
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        isPrepared = false;
        if (listener != null) listener.onTrackChanged(track, currentIndex);

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(track.getUrl());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                if (listener != null) listener.onPlaybackStateChanged(true);
                updateNotification(track);
                startProgressUpdater();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                if (currentIndex < playlist.size() - 1) {
                    next();
                } else {
                    if (listener != null) listener.onPlaybackStateChanged(false);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what + ", " + extra);
                if (listener != null) listener.onError("Ошибка воспроизведения");
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting data source: " + e.getMessage());
            if (listener != null) listener.onError(e.getMessage());
        }
    }

    // =========================================================
    //  Управление воспроизведением
    // =========================================================
    public void pause() {
        if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null) listener.onPlaybackStateChanged(false);
            updateNotification(getCurrentTrack());
        }
    }

    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            if (listener != null) listener.onPlaybackStateChanged(true);
            updateNotification(getCurrentTrack());
        }
    }

    public void next() {
        if (currentIndex < playlist.size() - 1) {
            currentIndex++;
            playCurrentTrack();
        }
    }

    public void previous() {
        // Если прошло больше 3 секунд — перемотка в начало
        if (mediaPlayer != null && isPrepared
                && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
        } else if (currentIndex > 0) {
            currentIndex--;
            playCurrentTrack();
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    // =========================================================
    //  Геттеры состояния
    // =========================================================
    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public Track getCurrentTrack() {
        if (!playlist.isEmpty() && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public int getCurrentIndex() { return currentIndex; }

    public boolean hasNext()     { return currentIndex < playlist.size() - 1; }
    public boolean hasPrevious() { return currentIndex > 0; }

    // =========================================================
    //  Обновление прогресса каждую секунду
    // =========================================================
    private final android.os.Handler progressHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private void startProgressUpdater() {
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
                    if (listener != null) {
                        listener.onProgressChanged(
                                mediaPlayer.getCurrentPosition(),
                                mediaPlayer.getDuration()
                        );
                    }
                    progressHandler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    // =========================================================
    //  Уведомление (Media Style)
    // =========================================================
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Музыкальный плеер",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Управление воспроизведением");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(Track track) {
        if (track == null) return;
        createNotificationChannel();

        // PendingIntent для кнопок
        PendingIntent pauseIntent = PendingIntent.getService(this, 0,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent playIntent = PendingIntent.getService(this, 1,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_PLAY),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent nextIntent = PendingIntent.getService(this, 2,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent prevIntent = PendingIntent.getService(this, 3,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_PREVIOUS),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .addAction(R.drawable.ic_previous, "Пред.", prevIntent)
                .addAction(isPlaying()
                                ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying() ? "Пауза" : "Играть",
                        isPlaying() ? pauseIntent : playIntent)
                .addAction(R.drawable.ic_next, "След.", nextIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying())
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}