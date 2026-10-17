package com.example.calisthenicsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Verificate.UserValidator;

public class ForgotPasswordActivity extends AppCompatActivity {
    /** The email typed on the login screen, to fill in the field; may be empty. */
    public static final String EXTRA_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        AuthService authService = new AuthService(this);

        TextView backToLogin = findViewById(R.id.forgotBackToLogin);
        backToLogin.setOnClickListener(view -> finish());

        EditText email = findViewById(R.id.forgotEmail);
        String typedEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (savedInstanceState == null && typedEmail != null) email.setText(typedEmail);

        Button sendButton = findViewById(R.id.sendResetButton);
        TextView sentMessage = findViewById(R.id.resetSentMessage);
        sendButton.setOnClickListener(view -> {
            String emailValue = email.getText().toString().trim();
            if (!UserValidator.isValidEmail(UserValidator.normalizeEmail(emailValue))) {
                email.setError(getString(R.string.signup_error_email));
                email.requestFocus();
                return;
            }
            sendButton.setEnabled(false);
            sentMessage.setVisibility(View.GONE);
            authService.sendPasswordReset(emailValue, new AuthService.AuthCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    sendButton.setEnabled(true);
                    sentMessage.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(String message) {
                    sendButton.setEnabled(true);
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
