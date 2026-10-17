package com.example.calisthenicsapp;

import android.app.Application;

import com.example.calisthenicsapp.Sync.SyncManager;

public class CalisthenicsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new SyncManager(this).start();
    }
}
