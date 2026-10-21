package com.example.calisthenicsapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.CoreComponents.UnitSystem;
import com.example.calisthenicsapp.CoreComponents.Units;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Service.ProfileService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private EditText username, weight, birthYear;
    private HeightInput height;
    private UnitSystem units;
    private Spinner gender;
    private Button save;
    private ProfileService profiles;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_profile);
        profiles = new ProfileService(this);
        username = findViewById(R.id.profileUsername);
        units = UnitPreference.get(this);
        height = new HeightInput(findViewById(R.id.profileHeight),
                findViewById(R.id.profileHeightImperial), findViewById(R.id.profileHeightFeet),
                findViewById(R.id.profileHeightInches), units);
        weight = findViewById(R.id.profileWeight);
        birthYear = findViewById(R.id.profileBirthYear);
        gender = findViewById(R.id.profileGender);
        save = findViewById(R.id.profileSave);
        if (units == UnitSystem.IMPERIAL) {
            ((TextView) findViewById(R.id.profileHeightLabel)).setText(R.string.profile_height_label_imperial);
            ((TextView) findViewById(R.id.profileWeightLabel)).setText(R.string.profile_weight_label_imperial);
            weight.setHint(R.string.profile_hint_weight_imperial);
        }
        findViewById(R.id.profileBack).setOnClickListener(v -> finish());
        setupGender();
        save.setOnClickListener(v -> saveProfile());
        loadProfile();
    }

    private void setupGender() {
        // The same choices as sign-up, so a saved gender is found again here.
        String[] options = getResources().getStringArray(R.array.signup_genders);
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
                height.show(user.getHeight());
                weight.setText(number(Units.weightFromKg(user.getWeight(), units)));
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
        Locale locale = Locale.getDefault();
        Double heightValue = height.toCm();
        if (!height.isEmpty() && (heightValue == null || !UserValidator.isValidHeight(heightValue))) {
            height.errorField().setError(Units.heightError(units, locale));
            height.errorField().requestFocus(); return;
        }
        Double weightValue = Units.weightToKg(parseNumber(weight), units);
        if (!weight.getText().toString().trim().isEmpty()
                && (weightValue == null || !UserValidator.isValidWeight(weightValue))) {
            weight.setError(Units.weightError(units, locale)); weight.requestFocus(); return;
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
