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
    private final String userId;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public PlaylistRepository(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    // =========================================================
    //  Создать плейлист
    // =========================================================
    public void createPlaylist(Playlist playlist, Callback<String> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", playlist.getTitle());
        data.put("description", playlist.getDescription());
        data.put("coverUrl", playlist.getCoverUrl());
        data.put("createdAt", playlist.getCreatedAt());
        data.put("ownerId", userId);
        data.put("trackCount", 0);

        db.collection("users")
                .document(userId)
                .collection("playlists")
                .add(data)
                .addOnSuccessListener(ref -> callback.onSuccess(ref.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
                        if (trackCount != null) p.setTrackCount(trackCount.intValue());

                        playlists.add(p);
                    }
                    callback.onSuccess(playlists);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Добавить трек в плейлист
    // =========================================================
    public void addTrackToPlaylist(String playlistId, Track track, Callback<Void> callback) {
        android.util.Log.d("PlaylistRepo", "=== ДОБАВЛЕНИЕ ТРЕКА ===");
        android.util.Log.d("PlaylistRepo", "User ID: " + userId);
        android.util.Log.d("PlaylistRepo", "Playlist ID: " + playlistId);
        android.util.Log.d("PlaylistRepo", "Track ID: " + track.getId());
        android.util.Log.d("PlaylistRepo", "Track Title: " + track.getTitle());
        android.util.Log.d("PlaylistRepo", "Track Position: " + track.getPosition());

        Map<String, Object> data = new HashMap<>();
        data.put("id", track.getId());
        data.put("artist", track.getArtist());
        data.put("title", track.getTitle());
        data.put("url", track.getUrl());
        data.put("coverUrl", track.getCoverUrl());
        data.put("duration", track.getDuration());
        data.put("position", track.getPosition());

        com.google.firebase.firestore.WriteBatch batch = db.batch();

        batch.set(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId)
                        .collection("tracks")
                        .document(track.getId()),
                data
        );

        batch.update(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId),
                "trackCount", com.google.firebase.firestore.FieldValue.increment(1)
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    android.util.Log.d("PlaylistRepo", "✅ Батч успешно выполнен!");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PlaylistRepo", "❌ Ошибка батча: " + e.getMessage());
                    e.printStackTrace();
                    callback.onError(e.getMessage());
                });
    }

    // =========================================================
    //  Обновить порядок треков
    // =========================================================
    public void updateTracksOrder(String playlistId, List<Track> tracks, Callback<Void> callback) {
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            track.setPosition(i + 1);

            batch.update(
                    db.collection("users")
                            .document(userId)
                            .collection("playlists")
                            .document(playlistId)
                            .collection("tracks")
                            .document(track.getId()),
                    "position", i + 1
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Удалить трек из плейлиста
    // =========================================================
    public void removeTrack(String playlistId, String trackId, Callback<Void> callback) {
        // Используем батч для атомарности
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // 1. Удаляем трек
        batch.delete(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId)
                        .collection("tracks")
                        .document(trackId)
        );

        // 2. Уменьшаем счетчик
        batch.update(
                db.collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId),
                "trackCount", com.google.firebase.firestore.FieldValue.increment(-1)
        );

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}