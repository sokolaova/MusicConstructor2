package com.example.musicconstructor2.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.musicconstructor2.data.model.Track;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VkApiClient {

    private static final String BASE_URL = "https://api.vk.com/method/";
    private static final String VERSION  = "5.131";

    private final String accessToken;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Колбэк для возврата результата в UI поток
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public VkApiClient(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("vk_prefs", Context.MODE_PRIVATE);
        this.accessToken = prefs.getString("access_token", "");
    }

    //  Поиск треков — audio.search
    public void searchTracks(String query, int count, Callback<List<Track>> callback) {
        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlStr = BASE_URL + "audio.search"
                        + "?q=" + encodedQuery
                        + "&count=" + count
                        + "&access_token=" + accessToken
                        + "&v=" + VERSION;

                String response = makeRequest(urlStr);
                List<Track> tracks = parseTracksResponse(response);

                // Возвращаем результат в главный поток
                android.os.Handler mainHandler =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(tracks));

            } catch (Exception e) {
                android.os.Handler mainHandler =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // =========================================================
    //  Получить аудио пользователя — audio.get
    // =========================================================
    public void getUserTracks(long userId, int count, Callback<List<Track>> callback) {
        executor.execute(() -> {
            try {
                String urlStr = BASE_URL + "audio.get"
                        + "?owner_id=" + userId
                        + "&count=" + count
                        + "&access_token=" + accessToken
                        + "&v=" + VERSION;

                String response = makeRequest(urlStr);
                List<Track> tracks = parseTracksResponse(response);

                android.os.Handler mainHandler =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(tracks));

            } catch (Exception e) {
                android.os.Handler mainHandler =
                        new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // =========================================================
    //  Парсинг ответа VK API
    // =========================================================
    private List<Track> parseTracksResponse(String json) throws Exception {
        List<Track> tracks = new ArrayList<>();
        JSONObject root     = new JSONObject(json);

        // Проверяем на ошибку от VK
        if (root.has("error")) {
            JSONObject error = root.getJSONObject("error");
            throw new Exception("VK API Error: " + error.getString("error_msg"));
        }

        JSONObject response = root.getJSONObject("response");
        JSONArray  items    = response.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);

            String id       = item.getString("id");
            String artist   = item.getString("artist");
            String title    = item.getString("title");
            String url      = item.optString("url", "");
            int    duration = item.optInt("duration", 0);

            // Обложка — вложенный объект album.thumb
            String coverUrl = "";
            if (item.has("album")) {
                JSONObject album = item.getJSONObject("album");
                if (album.has("thumb")) {
                    coverUrl = album.getJSONObject("thumb")
                            .optString("photo_300", "");
                }
            }

            Track track = new Track(id, artist, title, url, coverUrl, duration);
            track.setPosition(i + 1);
            tracks.add(track);
        }

        return tracks;
    }

    // =========================================================
    //  HTTP запрос
    // =========================================================
    private String makeRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}