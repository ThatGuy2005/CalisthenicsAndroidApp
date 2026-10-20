package com.example.calisthenicsapp;

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

import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {
    private TextView backToLogin;

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

    private void validateForm(AuthService authService, Button createAccount) {
        EditText username = findViewById(R.id.signupUsername);
        EditText email = findViewById(R.id.signupEmail);
        EditText password = findViewById(R.id.signupPassword);
        EditText height = findViewById(R.id.signupHeight);
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
            Double heightValue = parseOptionalNumber(height);
            Double weightValue = parseOptionalNumber(weight);
            if (!height.getText().toString().trim().isEmpty() && heightValue == null) {
                height.setError(getString(R.string.signup_error_height));
                height.requestFocus();
                return;
            }
            if (!weight.getText().toString().trim().isEmpty() && weightValue == null) {
                weight.setError(getString(R.string.signup_error_weight));
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

    private Double parseOptionalNumber(EditText field) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseOptionalYear(EditText field) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void setSigningUp(Button createAccount, boolean signingUp) {
        createAccount.setEnabled(!signingUp);
        backToLogin.setEnabled(!signingUp);
        backWhileSigningUp.setEnabled(signingUp);
    }
}
