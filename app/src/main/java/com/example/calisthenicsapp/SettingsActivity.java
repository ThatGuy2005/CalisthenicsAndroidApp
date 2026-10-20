package com.example.calisthenicsapp;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settingsBack).setOnClickListener(view -> finish());
        findViewById(R.id.settingsProfile).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }
}
