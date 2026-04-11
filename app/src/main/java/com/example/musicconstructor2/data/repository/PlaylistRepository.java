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
        this.db     = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    // =========================================================
    //  Создать плейлист
    // =========================================================
    public void createPlaylist(Playlist playlist, Callback<String> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("title",       playlist.getTitle());
        data.put("description", playlist.getDescription());
        data.put("coverUrl",    playlist.getCoverUrl());
        data.put("createdAt",   playlist.getCreatedAt());
        data.put("ownerId",     userId);
        data.put("trackCount",  0);

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
                .orderBy("createdAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
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
                        playlists.add(p);
                    }
                    callback.onSuccess(playlists);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Добавить трек в плейлист
    // =========================================================
    public void addTrackToPlaylist(String playlistId, Track track,
                                   Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("id",       track.getId());
        data.put("artist",   track.getArtist());
        data.put("title",    track.getTitle());
        data.put("url",      track.getUrl());
        data.put("coverUrl", track.getCoverUrl());
        data.put("duration", track.getDuration());
        data.put("position", track.getPosition());

        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("tracks")
                .document(track.getId())
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // =========================================================
    //  Получить треки плейлиста
    // =========================================================
    public void getPlaylistTracks(String playlistId,
                                  Callback<List<Track>> callback) {
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
    public void updateTracksOrder(String playlistId, List<Track> tracks,
                                  Callback<Void> callback) {
        // Батч-запись — обновляем все позиции за один запрос
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
    public void removeTrack(String playlistId, String trackId,
                            Callback<Void> callback) {
        db.collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("tracks")
                .document(trackId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}