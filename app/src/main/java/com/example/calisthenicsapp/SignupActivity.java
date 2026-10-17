package com.example.calisthenicsapp;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        AuthService authService = new AuthService(this);

        String[] genders = {"Select gender", "Female", "Male", "Non-binary", "Prefer not to say"};
        Spinner genderSpinner = findViewById(R.id.signupGender);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        TextView backToLogin = findViewById(R.id.backToLogin);
        backToLogin.setOnClickListener(view -> finish());

        Button createAccount = findViewById(R.id.createAccountButton);
        createAccount.setOnClickListener(view -> validateForm(authService, createAccount));
    }

    private void validateForm(AuthService authService, Button createAccount) {
        EditText username = findViewById(R.id.signupUsername);
        EditText email = findViewById(R.id.signupEmail);
        EditText password = findViewById(R.id.signupPassword);
        EditText height = findViewById(R.id.signupHeight);
        EditText weight = findViewById(R.id.signupWeight);
        Spinner gender = findViewById(R.id.signupGender);

        String usernameValue = username.getText().toString().trim();
        String emailValue = email.getText().toString().trim();
        String passwordValue = password.getText().toString();

        if (usernameValue.isEmpty()) {
            username.setError("Username is required");
            username.requestFocus();
        } else if (emailValue.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            email.setError("Enter a valid email address");
            email.requestFocus();
        } else if (!UserValidator.isValidPassword(passwordValue)) {
            password.setError("Use 8-64 characters with at least one letter and one digit");
            password.requestFocus();
        } else {
            Double heightValue = parseOptionalNumber(height);
            Double weightValue = parseOptionalNumber(weight);
            if (!height.getText().toString().trim().isEmpty() && heightValue == null) {
                height.setError("Enter a valid height");
                height.requestFocus();
                return;
            }
            if (!weight.getText().toString().trim().isEmpty() && weightValue == null) {
                weight.setError("Enter a valid weight");
                weight.requestFocus();
                return;
            }

            String genderValue = gender.getSelectedItemPosition() == 0
                    ? null : gender.getSelectedItem().toString();
            createAccount.setEnabled(false);
            // No birth year field yet, so it is left out (null).
            authService.signUp(usernameValue, emailValue, passwordValue, heightValue,
                    weightValue, genderValue, null, new AuthService.AuthCallback<FirebaseUser>() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            createAccount.setEnabled(true);
                            Toast.makeText(SignupActivity.this, "Account created.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onError(String message) {
                            createAccount.setEnabled(true);
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
}
