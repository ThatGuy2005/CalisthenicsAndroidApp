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

import com.example.calisthenicsapp.Database.AppDatabase;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Service.UserService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {
    private AppDatabase database;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        database = new AppDatabase(this);
        UserService userService = new UserService(
                new UserRepository(database.getWritableDatabase()));

        String[] genders = {"Select gender", "Female", "Male", "Non-binary", "Prefer not to say"};
        Spinner genderSpinner = findViewById(R.id.signupGender);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        TextView backToLogin = findViewById(R.id.backToLogin);
        backToLogin.setOnClickListener(view -> finish());

        Button createAccount = findViewById(R.id.createAccountButton);
        createAccount.setOnClickListener(view -> validateForm(userService, createAccount));
    }

    private void validateForm(UserService userService, Button createAccount) {
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
        } else if (passwordValue.length() < 8) {
            password.setError("Use at least 8 characters");
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
            databaseExecutor.execute(() -> {
                boolean success = userService.signUp(usernameValue, emailValue, passwordValue,
                        heightValue, weightValue, genderValue, "User");
                runOnUiThread(() -> {
                    createAccount.setEnabled(true);
                    if (success) {
                        Toast.makeText(this, "Account created. You can now log in.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Could not create account. Check the details or use another username/email.",
                                Toast.LENGTH_LONG).show();
                    }
                });
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

    @Override
    protected void onDestroy() {
        databaseExecutor.shutdown();
        if (database != null) database.close();
        super.onDestroy();
    }
}
