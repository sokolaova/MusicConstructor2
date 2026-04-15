package com.example.musicconstructor2.data.repository;

import com.example.musicconstructor2.data.model.Playlist;
import com.example.musicconstructor2.data.model.Track;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistRepository {

    private final FirebaseFirestore db;
    private final String userId; // VK user ID как строка

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public PlaylistRepository(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    // =========================================================
    //  Получить информацию о плейлисте по ID
    // =========================================================
    public void getPlaylistInfo(String playlistId, Callback<Playlist> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Playlist playlist = new Playlist();
                        playlist.setId(documentSnapshot.getId());
                        playlist.setTitle(documentSnapshot.getString("title"));

                        String description = documentSnapshot.getString("description");
                        playlist.setDescription(description != null ? description : "");

                        playlist.setCoverUrl(documentSnapshot.getString("coverUrl"));

                        Long createdAt = documentSnapshot.getLong("createdAt");
                        if (createdAt != null) playlist.setCreatedAt(createdAt);

                        Long trackCount = documentSnapshot.getLong("trackCount");
                        if (trackCount != null) playlist.setTrackCount(trackCount.intValue());

                        android.util.Log.d("PlaylistRepo", "Загружено описание: " + playlist.getDescription());

                        callback.onSuccess(playlist);
                    } else {
                        callback.onError("Плейлист не найден");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PlaylistRepo", "Ошибка загрузки: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }
    // =========================================================
    //  Создать плейлист с проверкой дубликатов
    // =========================================================
    public void createPlaylist(Playlist playlist, Callback<String> callback) {
        // Проверяем, что название не пустое
        if (playlist.getTitle() == null || playlist.getTitle().trim().isEmpty()) {
            callback.onError("Название плейлиста не может быть пустым");
            return;
        }

        playlist.setTitle(playlist.getTitle().trim());

        db.collection("users")
                .document(userId)
                .collection("playlists")
                .whereEqualTo("title", playlist.getTitle())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        // Плейлист с таким названием уже существует
                        callback.onError("Плейлист с названием \"" + playlist.getTitle() + "\" уже существует");
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("title", playlist.getTitle());
                    data.put("description", playlist.getDescription() != null ? playlist.getDescription() : "");
                    data.put("coverUrl", playlist.getCoverUrl());
                    data.put("createdAt", playlist.getCreatedAt());
                    data.put("ownerId", userId);
                    data.put("trackCount", 0);

                    db.collection("users")
                            .document(userId)
                            .collection("playlists")
                            .add(data)
                            .addOnSuccessListener(ref -> {
                                // Сразу синхронизируем счетчик (на всякий случай)
                                syncPlaylistTrackCount(ref.getId(), new Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        callback.onSuccess(ref.getId());
                                    }

                                    @Override
                                    public void onError(String error) {
                                        // Даже если синхронизация не удалась, плейлист создан
                                        callback.onSuccess(ref.getId());
                                    }
                                });
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Синхронизировать счёт треков с реальным количеством
    // =========================================================
    public void syncPlaylistTrackCount(String playlistId, Callback<Void> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("tracks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int actualCount = snapshot.size();

                    db.collection("users")
                            .document(userId)
                            .collection("playlists")
                            .document(playlistId)
                            .update("trackCount", actualCount)
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onError(e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    // =========================================================
    //  Получить все плейлисты пользователя
    // =========================================================
    public void getPlaylists(Callback<List<Playlist>> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Playlist> playlists = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Playlist p = new Playlist();
                        p.setId(doc.getId());
                        p.setTitle(doc.getString("title"));
                        p.setDescription(doc.getString("description"));
                        p.setCoverUrl(doc.getString("coverUrl"));

                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null) p.setCreatedAt(createdAt);

                        Long trackCount = doc.getLong("trackCount");
                        if (trackCount != null) {
                            p.setTrackCount(trackCount.intValue());
                        } else {
                            p.setTrackCount(0);
                        }

                        playlists.add(p);
                    }
                    callback.onSuccess(playlists);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Добавить трек в плейлист с проверкой дубликатов
    // =========================================================
    public void addTrackToPlaylist(String playlistId, Track track, Callback<Void> callback) {
        // Проверяем, нет ли уже такого трека в плейлисте
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("tracks")
                .document(track.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onError("Этот трек уже есть в плейлисте");
                        return;
                    }

                    // Добавляем трек
                    addTrackInternal(playlistId, track, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void addTrackInternal(String playlistId, Track track, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", track.getId());
        data.put("artist", track.getArtist() != null ? track.getArtist() : "");
        data.put("title", track.getTitle() != null ? track.getTitle() : "");
        data.put("url", track.getUrl() != null ? track.getUrl() : "");
        data.put("coverUrl", track.getCoverUrl());
        data.put("duration", track.getDuration());
        data.put("position", track.getPosition());

        // Используем батч для атомарности
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // 1. Добавляем трек в подколлекцию
        batch.set(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId)
                        .collection("tracks")
                        .document(track.getId()),
                data
        );

        // 2. Увеличиваем счётчик треков в плейлисте
        batch.update(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId),
                "trackCount", com.google.firebase.firestore.FieldValue.increment(1)
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    // Синхронизируем счетчик для уверенности
                    syncPlaylistTrackCount(playlistId, null);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Получить треки плейлиста
    // =========================================================
    public void getPlaylistTracks(String playlistId, Callback<List<Track>> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("tracks")
                .orderBy("position")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Track> tracks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Track t = new Track();
                        t.setId(doc.getString("id"));
                        t.setArtist(doc.getString("artist"));
                        t.setTitle(doc.getString("title"));
                        t.setUrl(doc.getString("url"));
                        t.setCoverUrl(doc.getString("coverUrl"));

                        Long dur = doc.getLong("duration");
                        if (dur != null) t.setDuration(dur.intValue());

                        Long pos = doc.getLong("position");
                        if (pos != null) t.setPosition(pos.intValue());

                        tracks.add(t);
                    }
                    callback.onSuccess(tracks);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Обновить порядок треков после drag-and-drop
    // =========================================================
    public void updateTracksOrder(String playlistId, List<Track> tracks, Callback<Void> callback) {
        if (tracks == null || tracks.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            track.setPosition(i);

            batch.update(
                    db.collection("users")
                            .document(userId)
                            .collection("playlists")
                            .document(playlistId)
                            .collection("tracks")
                            .document(track.getId()),
                    "position", i
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Удалить трек из плейлиста с уменьшением счётчика
    // =========================================================
    public void removeTrack(String playlistId, String trackId, Callback<Void> callback) {
        // Используем батч для атомарности
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // 1. Удаляем трек из подколлекции
        batch.delete(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId)
                        .collection("tracks")
                        .document(trackId)
        );

        // 2. Уменьшаем счётчик треков в плейлисте
        batch.update(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId),
                "trackCount", com.google.firebase.firestore.FieldValue.increment(-1)
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    // Синхронизируем счетчик для уверенности
                    syncPlaylistTrackCount(playlistId, null);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Обновить описание плейлиста
    // =========================================================
    public void updatePlaylistDescription(String playlistId, String description, Callback<Void> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .update("description", description != null ? description : "")
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Удалить плейлист целиком со всеми треками
    // =========================================================
    public void deletePlaylist(String playlistId, Callback<Void> callback) {
        // Сначала получаем все треки плейлиста
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("tracks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Используем батч для удаления всех треков и самого плейлиста
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    // Удаляем все треки
                    for (QueryDocumentSnapshot doc : snapshot) {
                        batch.delete(doc.getReference());
                    }

                    // Удаляем сам плейлист
                    batch.delete(
                            db.collection("users")
                                    .document(userId)
                                    .collection("playlists")
                                    .document(playlistId)
                    );

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Проверить, существует ли плейлист
    // =========================================================
    public void checkPlaylistExists(String playlistId, Callback<Boolean> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .get()
                .addOnSuccessListener(documentSnapshot ->
                        callback.onSuccess(documentSnapshot.exists()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}