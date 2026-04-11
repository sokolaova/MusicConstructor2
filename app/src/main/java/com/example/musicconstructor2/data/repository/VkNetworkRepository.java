package com.example.musicconstructor2.data.repository;

import com.example.musicconstructor2.data.model.Track;
import com.example.musicconstructor2.data.model.VkProfile;
import java.util.List;

public interface VkNetworkRepository {
    void getVkMusic(String token, int count, VkApiCallback<List<Track>> callback);
    void getVkProfile(String token, VkApiCallback<VkProfile> callback);

    interface VkApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
}