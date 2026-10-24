package com.example.calisthenicsapp;

import android.app.Application;
import android.content.Intent;

import com.example.calisthenicsapp.Sync.SyncManager;

public class CalisthenicsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SyncManager syncManager = new SyncManager(this);
        // The account's profile is gone and the user was signed out: back to the login screen.
        syncManager.setOnAccountGone(() -> startActivity(new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_ACCOUNT_GONE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)));
        syncManager.start();
    }
}
