package com.example.calisthenicsapp;

import android.os.Bundle;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settingsBack).setOnClickListener(view -> finish());
        findViewById(R.id.settingsProfile).setOnClickListener(view ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.settingsSecurity).setOnClickListener(view ->
                startActivity(new Intent(this, AccountSecurityActivity.class)));
        Spinner units = findViewById(R.id.settingsUnits);
        String[] choices = {"Metric  ·  kg and cm", "Imperial  ·  lb and ft"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, choices);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        units.setAdapter(adapter);
        units.setSelection(UnitSystem.isImperial(this) ? 1 : 0);
        units.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            private boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                                 android.view.View view, int position, long id) {
                if (first) { first = false; return; }
                boolean imperial = position == 1;
                if (imperial != UnitSystem.isImperial(SettingsActivity.this)) {
                    UnitSystem.setImperial(SettingsActivity.this, imperial);
                    Snackbar.make(findViewById(R.id.settingsRoot),
                            imperial ? "Units set to pounds and feet." : "Units set to kilograms and centimetres.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }
}
