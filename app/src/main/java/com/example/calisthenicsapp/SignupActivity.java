package com.example.calisthenicsapp;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.android.material.snackbar.Snackbar;

public class SignupActivity extends AppCompatActivity {
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        authService = new AuthService(this);

        String[] genders = {"Select gender (optional)", "Male", "Female", "Other"};
        Spinner genderSpinner = findViewById(R.id.signupGender);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, genders);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        genderSpinner.setPopupBackgroundResource(R.color.performance_surface);
        genderSpinner.setAdapter(adapter);

        TextView backToLogin = findViewById(R.id.backToLogin);
        backToLogin.setOnClickListener(view -> finish());

        EditText password = findViewById(R.id.signupPassword);
        CheckBox showPassword = findViewById(R.id.signupShowPasswordCheck);
        showPassword.setOnCheckedChangeListener((button, visible) -> {
            int cursor = password.getSelectionStart();
            password.setInputType(visible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            password.setSelection(Math.max(cursor, 0));
        });

        Button createAccount = findViewById(R.id.createAccountButton);
        createAccount.setOnClickListener(view -> validateForm(createAccount));
    }

    private void validateForm(Button createAccount) {
        EditText username = findViewById(R.id.signupUsername);
        EditText email = findViewById(R.id.signupEmail);
        EditText password = findViewById(R.id.signupPassword);
        EditText height = findViewById(R.id.signupHeight);
        EditText weight = findViewById(R.id.signupWeight);
        Spinner gender = findViewById(R.id.signupGender);

        String usernameValue = username.getText().toString().trim();
        String emailValue = email.getText().toString().trim();
        String passwordValue = password.getText().toString();

        if (!UserValidator.isValidUsername(usernameValue)) {
            username.setError("Username must be 3-20 letters, digits, '.', '_' or '-'.");
            username.requestFocus();
        } else if (!UserValidator.isValidEmail(emailValue)) {
            email.setError("Enter a valid email address.");
            email.requestFocus();
        } else if (!UserValidator.isValidPassword(passwordValue)) {
            password.setError("Password must be 8-64 characters with at least one letter and one digit.");
            password.requestFocus();
        } else {
            Double heightValue = parseOptionalNumber(height);
            Double weightValue = parseOptionalNumber(weight);
            if (!height.getText().toString().trim().isEmpty()
                    && (heightValue == null || !UserValidator.isValidHeight(heightValue))) {
                height.setError("Height must be between 50 and 300 cm.");
                height.requestFocus();
                return;
            }
            if (!weight.getText().toString().trim().isEmpty()
                    && (weightValue == null || !UserValidator.isValidWeight(weightValue))) {
                weight.setError("Weight must be between 20 and 500 kg.");
                weight.requestFocus();
                return;
            }

            String genderValue = gender.getSelectedItemPosition() == 0
                    ? null : gender.getSelectedItem().toString();
            if (!UserValidator.isValidGender(genderValue)) {
                Snackbar.make(findViewById(R.id.signupRoot),
                        "Gender can be at most 30 characters.", Snackbar.LENGTH_LONG).show();
                return;
            }

            createAccount.setEnabled(false);
            authService.signUp(usernameValue, emailValue, passwordValue,
                    heightValue, weightValue, genderValue, null,
                    new AuthService.AuthCallback<com.google.firebase.auth.FirebaseUser>() {
                @Override
                public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                    runOnUiThread(() -> {
                        createAccount.setEnabled(true);
                        Toast.makeText(SignupActivity.this,
                                "Account created. Welcome!", Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        createAccount.setEnabled(true);
                        Snackbar.make(findViewById(R.id.signupRoot), message,
                                Snackbar.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private Double parseOptionalNumber(EditText field) {
        String value = field.getText().toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
