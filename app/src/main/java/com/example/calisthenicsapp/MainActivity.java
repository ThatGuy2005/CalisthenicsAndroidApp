package com.example.calisthenicsapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.calisthenicsapp.Service.AuthService;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        AuthService authService = new AuthService(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button signupButton = findViewById(R.id.openSignupButton);
        signupButton.setOnClickListener(view ->
                startActivity(new android.content.Intent(this, SignupActivity.class)));

        EditText emailInput = findViewById(R.id.emailInput);
        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        forgotPasswordButton.setOnClickListener(view -> startActivity(
                new android.content.Intent(this, ForgotPasswordActivity.class)
                        .putExtra(ForgotPasswordActivity.EXTRA_EMAIL,
                                emailInput.getText().toString().trim())));

        Button loginButton = findViewById(R.id.loginButton);
        EditText passwordInput = findViewById(R.id.passwordInput);
        loginButton.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show();
            } else {
                loginButton.setEnabled(false);
                authService.signIn(email, password, new AuthService.AuthCallback<FirebaseUser>() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        loginButton.setEnabled(true);
                        Toast.makeText(MainActivity.this, R.string.login_success,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        loginButton.setEnabled(true);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
