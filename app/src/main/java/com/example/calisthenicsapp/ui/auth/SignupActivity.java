package com.example.calisthenicsapp.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.model.UnitSystem;
import com.example.calisthenicsapp.model.Units;
import com.example.calisthenicsapp.service.AuthService;
import com.example.calisthenicsapp.ui.settings.HeightInput;
import com.example.calisthenicsapp.ui.settings.UnitPreference;
import com.example.calisthenicsapp.validation.UserValidator;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/**
 * The sign-up screen (FR-1.1 to FR-1.3): username, email and password, and the optional height,
 * weight, gender and birth year for the calorie estimate. Height and weight are typed in the units
 * chosen in Settings and sent to AuthService in cm and kg.
 */
public class SignupActivity extends AppCompatActivity {
    /** The "back to login" link, disabled while signing up. */
    private TextView backToLogin;
    /** This device's unit setting. */
    private UnitSystem units;
    /** The height field, in cm or in feet and inches. */
    private HeightInput height;

    /**
     * Blocks back while the account is being created. Firebase signs the new account in before
     * the profile is saved, so the login screen would open Home for an account that may still be
     * discarded.
     */
    private final OnBackPressedCallback backWhileSigningUp = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            Toast.makeText(SignupActivity.this, R.string.signup_in_progress,
                    Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        AuthService authService = new AuthService(this);
        units = UnitPreference.get(this);
        height = new HeightInput(findViewById(R.id.signupHeight),
                findViewById(R.id.signupHeightImperial), findViewById(R.id.signupHeightFeet),
                findViewById(R.id.signupHeightInches), units);
        if (units == UnitSystem.IMPERIAL) {
            ((EditText) findViewById(R.id.signupWeight)).setHint(R.string.signup_hint_weight_imperial);
            ((TextView) findViewById(R.id.signupProfileNote)).setText(R.string.signup_optional_note_imperial);
        }

        String[] genders = getResources().getStringArray(R.array.signup_genders);
        Spinner genderSpinner = findViewById(R.id.signupGender);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, genders);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        genderSpinner.setPopupBackgroundResource(R.color.performance_surface);
        genderSpinner.setAdapter(adapter);

        backToLogin = findViewById(R.id.backToLogin);
        backToLogin.setOnClickListener(view -> finish());
        getOnBackPressedDispatcher().addCallback(this, backWhileSigningUp);

        Button createAccount = findViewById(R.id.createAccountButton);
        createAccount.setOnClickListener(view -> validateForm(authService, createAccount));
    }

    /**
     * Checks the form field by field, putting the first problem on its field, and signs up when
     * everything is fine. The optional fields may be empty, but not wrong.
     */
    private void validateForm(AuthService authService, Button createAccount) {
        EditText username = findViewById(R.id.signupUsername);
        EditText email = findViewById(R.id.signupEmail);
        EditText password = findViewById(R.id.signupPassword);
        EditText weight = findViewById(R.id.signupWeight);
        EditText birthYear = findViewById(R.id.signupBirthYear);
        Spinner gender = findViewById(R.id.signupGender);

        String usernameValue = username.getText().toString().trim();
        String emailValue = email.getText().toString().trim();
        String passwordValue = password.getText().toString();

        if (usernameValue.isEmpty()) {
            username.setError(getString(R.string.signup_error_username));
            username.requestFocus();
        } else if (emailValue.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            email.setError(getString(R.string.signup_error_email));
            email.requestFocus();
        } else if (!UserValidator.isValidPassword(passwordValue)) {
            password.setError(getString(R.string.signup_error_password));
            password.requestFocus();
        } else {
            Locale locale = Locale.getDefault();
            // The optional fields are only checked when something was typed.
            Double heightValue = height.toCm();
            if (!height.isEmpty() && (heightValue == null || !UserValidator.isValidHeight(heightValue))) {
                height.errorField().setError(Units.heightError(units, locale));
                height.errorField().requestFocus();
                return;
            }
            Double weightValue = Units.weightToKg(parseOptionalNumber(weight), units);
            if (!weight.getText().toString().trim().isEmpty()
                    && (weightValue == null || !UserValidator.isValidWeight(weightValue))) {
                weight.setError(Units.weightError(units, locale));
                weight.requestFocus();
                return;
            }
            Integer birthYearValue = parseOptionalYear(birthYear);
            if (!birthYear.getText().toString().trim().isEmpty()
                    && (birthYearValue == null || !UserValidator.isValidBirthYear(birthYearValue))) {
                birthYear.setError(getString(R.string.signup_error_birth_year,
                        UserValidator.BIRTH_YEAR_MIN));
                birthYear.requestFocus();
                return;
            }

            // The first choice is the "not given" placeholder.
            String genderValue = gender.getSelectedItemPosition() == 0
                    ? null : gender.getSelectedItem().toString();
            setSigningUp(createAccount, true);
            authService.signUp(usernameValue, emailValue, passwordValue, heightValue,
                    weightValue, genderValue, birthYearValue, new AuthService.AuthCallback<FirebaseUser>() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            setSigningUp(createAccount, false);
                            Toast.makeText(SignupActivity.this, R.string.signup_success,
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onError(String message) {
                            setSigningUp(createAccount, false);
                            Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * The field as a number, with a comma accepted as the decimal point; null if it is empty or not
     * a number.
     */
    private Double parseOptionalNumber(EditText field) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** The field as a whole number; null if it is empty or not one. */
    private Integer parseOptionalYear(EditText field) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Locks the buttons and Back while the account is being created. */
    private void setSigningUp(Button createAccount, boolean signingUp) {
        createAccount.setEnabled(!signingUp);
        backToLogin.setEnabled(!signingUp);
        backWhileSigningUp.setEnabled(signingUp);
    }
}
