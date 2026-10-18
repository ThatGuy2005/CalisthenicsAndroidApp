package com.example.calisthenicsapp;

import android.os.Bundle;
import android.text.InputType;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private AuthService authService;
    private boolean openingHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        authService = new AuthService(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button signupButton = findViewById(R.id.openSignupButton);
        signupButton.setOnClickListener(view ->
                startActivity(new android.content.Intent(this, SignupActivity.class)));

        Button previewHomeButton = findViewById(R.id.previewHomeButton);
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            previewHomeButton.setVisibility(View.VISIBLE);
            previewHomeButton.setOnClickListener(view -> {
                Intent previewIntent = new Intent(this, HomeActivity.class);
                previewIntent.putExtra(HomeActivity.EXTRA_PREVIEW_MODE, true);
                startActivity(previewIntent);
            });
        }

        Button loginButton = findViewById(R.id.loginButton);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        CheckBox showPassword = findViewById(R.id.showPasswordCheck);
        showPassword.setOnCheckedChangeListener((button, visible) -> {
            int cursor = passwordInput.getSelectionStart();
            passwordInput.setInputType(visible
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setSelection(Math.max(cursor, 0));
        });

        loginButton.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (!UserValidator.isValidEmail(email)) {
                emailInput.setError("Enter a valid email address.");
                emailInput.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                passwordInput.setError("Enter your password.");
                passwordInput.requestFocus();
                return;
            }

            loginButton.setEnabled(false);
            authService.signIn(email, password, new AuthService.AuthCallback<FirebaseUser>() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    loginButton.setEnabled(true);
                    openHome();
                }

                @Override
                public void onError(String message) {
                    loginButton.setEnabled(true);
                    Snackbar.make(findViewById(R.id.main), message,
                            Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authService != null && authService.getCurrentUser() != null) openHome();
    }

    private void openHome() {
        if (openingHome) return;
        openingHome = true;
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
