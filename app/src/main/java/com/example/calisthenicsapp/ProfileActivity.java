package com.example.calisthenicsapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Service.ProfileService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.android.material.snackbar.Snackbar;

public class ProfileActivity extends AppCompatActivity {
    private EditText username, height, weight, birthYear;
    private Spinner gender;
    private Button save;
    private ProfileService profiles;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_profile);
        profiles = new ProfileService(this);
        username = findViewById(R.id.profileUsername);
        height = findViewById(R.id.profileHeight);
        weight = findViewById(R.id.profileWeight);
        birthYear = findViewById(R.id.profileBirthYear);
        gender = findViewById(R.id.profileGender);
        save = findViewById(R.id.profileSave);
        findViewById(R.id.profileBack).setOnClickListener(v -> finish());
        setupGender();
        save.setOnClickListener(v -> saveProfile());
        loadProfile();
    }

    private void setupGender() {
        String[] options = {"Select gender (optional)", "Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, options);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        gender.setPopupBackgroundResource(R.color.performance_surface);
        gender.setAdapter(adapter);
    }

    private void loadProfile() {
        setSaving(true, "Loading profile…");
        profiles.getMyProfile(new ServiceCallback<User>() {
            @Override public void onSuccess(User user) {
                setSaving(false, "Save profile");
                if (user == null) {
                    Snackbar.make(findViewById(R.id.profileRoot),
                            "Your profile isn't available yet. Check your connection and try again.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }
                username.setText(user.getUsername() == null ? "" : user.getUsername());
                ((android.widget.TextView) findViewById(R.id.profileEmail)).setText(
                        user.getEmail() == null ? "Not set" : user.getEmail());
                if (user.getHeight() != null && user.getHeight() > 0) height.setText(number(user.getHeight()));
                if (user.getWeight() != null && user.getWeight() > 0) weight.setText(number(user.getWeight()));
                if (user.getBirthYear() != null) birthYear.setText(String.valueOf(user.getBirthYear()));
                String value = user.getGender();
                if (value != null) {
                    for (int i = 1; i < gender.getCount(); i++) {
                        if (value.equalsIgnoreCase(gender.getItemAtPosition(i).toString())) {
                            gender.setSelection(i); break;
                        }
                    }
                }
            }
            @Override public void onError(String message) {
                setSaving(false, "Save profile"); show(message);
            }
        });
    }

    private void saveProfile() {
        String usernameValue = username.getText().toString().trim();
        if (!UserValidator.isValidUsername(usernameValue)) {
            username.setError("Use 3–20 letters, digits, '.', '_' or '-'."); username.requestFocus(); return;
        }
        Double heightValue = parseNumber(height);
        if (!height.getText().toString().trim().isEmpty()
                && (heightValue == null || !UserValidator.isValidHeight(heightValue))) {
            height.setError("Height must be between 50 and 300 cm."); height.requestFocus(); return;
        }
        Double weightValue = parseNumber(weight);
        if (!weight.getText().toString().trim().isEmpty()
                && (weightValue == null || !UserValidator.isValidWeight(weightValue))) {
            weight.setError("Weight must be between 20 and 500 kg."); weight.requestFocus(); return;
        }
        String genderValue = gender.getSelectedItemPosition() == 0
                ? null : gender.getSelectedItem().toString();
        if (!UserValidator.isValidGender(genderValue)) { show("Gender can be at most 30 characters."); return; }
        Integer yearValue = null;
        String yearText = birthYear.getText().toString().trim();
        if (!yearText.isEmpty()) {
            try { yearValue = Integer.valueOf(yearText); }
            catch (NumberFormatException ignored) { birthYear.setError("Enter a valid birth year."); birthYear.requestFocus(); return; }
        }
        if (!UserValidator.isValidBirthYear(yearValue)) {
            birthYear.setError("Enter a year from 1900 to the current year."); birthYear.requestFocus(); return;
        }

        setSaving(true, "Saving…");
        profiles.updateProfile(usernameValue, heightValue, weightValue, genderValue, yearValue,
                new ServiceCallback<Void>() {
                    @Override public void onSuccess(Void result) {
                        setSaving(false, "Save profile");
                        Snackbar.make(findViewById(R.id.profileRoot), "Profile saved.", Snackbar.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String message) { setSaving(false, "Save profile"); show(message); }
                });
    }

    private Double parseNumber(EditText field) {
        String text = field.getText().toString().trim();
        if (text.isEmpty()) return null;
        try { return Double.parseDouble(text.replace(',', '.')); }
        catch (NumberFormatException ignored) { return null; }
    }
    private String number(Double value) {
        if (value == null) return "";
        if (value == Math.rint(value)) return String.valueOf(value.intValue());
        return String.valueOf(value);
    }
    private void setSaving(boolean saving, String label) {
        save.setEnabled(!saving); save.setText(label);
        username.setEnabled(!saving); height.setEnabled(!saving); weight.setEnabled(!saving);
        birthYear.setEnabled(!saving); gender.setEnabled(!saving);
    }
    private void show(String message) {
        Snackbar.make(findViewById(R.id.profileRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
