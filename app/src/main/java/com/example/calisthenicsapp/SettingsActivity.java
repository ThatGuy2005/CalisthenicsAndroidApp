package com.example.calisthenicsapp;

import android.os.Bundle;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import com.example.calisthenicsapp.CoreComponents.UnitSystem;
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
        UnitSystem[] systems = UnitSystem.values();
        String[] choices = new String[systems.length];
        for (int i = 0; i < systems.length; i++) choices[i] = systems[i].getLabel();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, choices);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        units.setAdapter(adapter);
        units.setSelection(UnitPreference.get(this).ordinal());
        units.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                                 android.view.View view, int position, long id) {
                // Also called once for the saved choice when the screen opens; that is no change.
                UnitSystem chosen = systems[position];
                if (chosen != UnitPreference.get(SettingsActivity.this)) {
                    UnitPreference.set(SettingsActivity.this, chosen);
                    Snackbar.make(findViewById(R.id.settingsRoot),
                            chosen == UnitSystem.IMPERIAL ? "Units set to pounds and feet."
                                    : "Units set to kilograms and centimetres.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }
}
