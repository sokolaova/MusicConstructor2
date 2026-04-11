package com.example.musicconstructor2.data.repository;

import android.util.Log;

import com.example.musicconstructor2.data.model.Album;
import com.example.musicconstructor2.data.model.Thumb;
import com.example.musicconstructor2.data.model.VkSong;
import com.example.musicconstructor2.network.core.VkApi;
import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.model.VkProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

public class VkNetworkRepositoryImpl implements VkNetworkRepository {

    private static final String TAG = "VkNetworkRepository";
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    //  Получить музыку пользователя — audio.get + Маруся токен
    @Override
    public void getVkMusic(String token, int count,
                           VkApiCallback<List<Track>> callback) {
        executor.execute(() -> {
            try {
                String urlStr = VkApi.SONGS_URL
                        + "?v="            + VkApi.VERSION
                        + "&access_token=" + token
                        + "&sig="          + VkApi.MD5
                        + "&count="        + count;

                String response = makeRequest(urlStr);
                List<Track> tracks = parseTracks(response);

                mainHandler.post(() -> callback.onSuccess(tracks));

            } catch (Exception e) {
                Log.e(TAG, "getVkMusic error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    //  Получить профиль пользователя — users.get

    @Override
    public void getVkProfile(String token, VkApiCallback<VkProfile> callback) {
        executor.execute(() -> {
            try {
                String urlStr = VkApi.PROFILE_URL
                        + "?v="            + VkApi.VERSION
                        + "&access_token=" + token
                        + "&fields=photo_400_orig";

                String response = makeRequest(urlStr);
                VkProfile profile = parseProfile(response);

                mainHandler.post(() -> callback.onSuccess(profile));

            } catch (Exception e) {
                Log.e(TAG, "getVkProfile error: " + e.getMessage(), e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    //  Парсинг треков

    private List<Track> parseTracks(String json) throws Exception {
        List<Track> tracks = new ArrayList<>();
        JSONObject root = new JSONObject(json);

        if (root.has("error")) {
            throw new Exception(root.getJSONObject("error")
                    .optString("error_msg", "VK API Error"));
        }

        JSONObject responseData = root.getJSONObject("response");
        JSONArray  items        = responseData.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);

            VkSong song = new VkSong();
            song.setId(item.optLong("id", i));
            song.setArtist(item.optString("artist", ""));
            song.setTitle(item.optString("title", ""));
            song.setUrl(item.optString("url", ""));
            song.setDuration(item.optInt("duration", 0));
            song.setAccessKey(item.optString("access_key", ""));
            song.setTrackCode(item.optString("track_code", ""));
            song.setSubtitle(item.optString("subtitle", null));

            // Парсим альбом
            if (item.has("album")) {
                JSONObject albumJson = item.getJSONObject("album");
                Album album = new Album();
                album.setId(albumJson.optInt("id", 0));
                album.setTitle(albumJson.optString("title", ""));
                album.setOwnerId(albumJson.optInt("owner_id", 0));
                album.setAccessKey(albumJson.optString("access_key", ""));
                album.setMainColor(albumJson.optString("main_color", null));

                // Парсим обложку
                if (albumJson.has("thumb")) {
                    JSONObject thumbJson = albumJson.getJSONObject("thumb");
                    Thumb thumb = new Thumb();
                    thumb.setWidth(thumbJson.optInt("width", 0));
                    thumb.setHeight(thumbJson.optInt("height", 0));
                    thumb.setId(thumbJson.optString("id", ""));
                    thumb.setPhoto34(thumbJson.optString("photo_34", null));
                    thumb.setPhoto68(thumbJson.optString("photo_68", null));
                    thumb.setPhoto135(thumbJson.optString("photo_135", null));
                    thumb.setPhoto270(thumbJson.optString("photo_270", null));
                    thumb.setPhoto300(thumbJson.optString("photo_300", null));
                    thumb.setPhoto600(thumbJson.optString("photo_600", null));
                    thumb.setPhoto1200(thumbJson.optString("photo_1200", null));
                    album.setThumb(thumb);
                }
                song.setAlbum(album);
            }

            // Конвертируем VkSong → Track для адаптера
            tracks.add(song.toTrack(i + 1));
        }

        return tracks;
    }
    // =========================================================
    //  Парсинг профиля
    // =========================================================
    private VkProfile parseProfile(String json) throws Exception {
        JSONObject root = new JSONObject(json);

        if (root.has("error")) {
            JSONObject error = root.getJSONObject("error");
            throw new Exception(error.optString("error_msg", "VK API Error"));
        }

        JSONArray  response = root.getJSONArray("response");
        JSONObject user     = response.getJSONObject(0);

        VkProfile profile = new VkProfile();
        profile.setId(user.optLong("id", 0));
        profile.setFirstName(user.optString("first_name", ""));
        profile.setLastName(user.optString("last_name", ""));
        profile.setPhoto(user.optString("photo_400_orig", ""));

        return profile;
    }

    //  HTTP запрос

    private String makeRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "VKAndroidApp/5.52-4543");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP Error: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }
}