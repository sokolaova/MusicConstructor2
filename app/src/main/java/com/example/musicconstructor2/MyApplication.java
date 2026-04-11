package com.example.musicconstructor2;

import android.app.Application;
import com.vk.id.VKID;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VKID.Companion.init(this);
    }
}