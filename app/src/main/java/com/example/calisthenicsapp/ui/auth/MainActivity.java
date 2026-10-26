package com.example.calisthenicsapp.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.calisthenicsapp.R;
import com.example.calisthenicsapp.service.AuthService;
import com.example.calisthenicsapp.ui.home.HomeActivity;
import com.google.firebase.auth.FirebaseUser;

/**
 * The login screen and the app's launcher activity (FR-1.4). It goes straight to Home when someone
 * is already signed in, and links to sign-up and the password reset.
 */
public class MainActivity extends AppCompatActivity {
    /** Set when the user was signed out because their account's profile is gone. */
    public static final String EXTRA_ACCOUNT_GONE = "account_gone";

    private AuthService authService;
    /** Home is already being opened, so onStart and a sign-in do not open it twice. */
    private boolean openingHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        authService = new AuthService(this);
        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_ACCOUNT_GONE, false)) {
            Toast.makeText(this, R.string.login_account_gone, Toast.LENGTH_LONG).show();
        }
        // Edge-to-edge: pad the content so it does not go under the status and navigation bars.
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
                        openHome();
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

    /**
     * Skips the login screen when someone is already signed in: on app start, and when sign-up
     * finishes, since Firebase signs a new account in.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (authService.getCurrentUser() != null) openHome();
    }

    /** Opens Home and closes the login screen, once. */
    private void openHome() {
        if (openingHome) return;
        openingHome = true;
        startActivity(new android.content.Intent(this, HomeActivity.class));
        finish();
    }
}
