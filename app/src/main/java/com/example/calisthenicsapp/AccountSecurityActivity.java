package com.example.calisthenicsapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.AccountService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.android.material.snackbar.Snackbar;

public class AccountSecurityActivity extends AppCompatActivity {
    private EditText currentPassword, newPassword, confirmPassword;
    private EditText emailCurrentPassword, newEmail;
    private Button passwordButton, emailButton;
    private AccountService account;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_account_security);
        account = new AccountService(this);
        currentPassword = findViewById(R.id.securityCurrentPassword);
        newPassword = findViewById(R.id.securityNewPassword);
        confirmPassword = findViewById(R.id.securityConfirmPassword);
        emailCurrentPassword = findViewById(R.id.securityEmailCurrentPassword);
        newEmail = findViewById(R.id.securityNewEmail);
        passwordButton = findViewById(R.id.securityChangePassword);
        emailButton = findViewById(R.id.securityChangeEmail);
        findViewById(R.id.securityBack).setOnClickListener(v -> finish());
        passwordButton.setOnClickListener(v -> changePassword());
        emailButton.setOnClickListener(v -> changeEmail());
    }

    private void changePassword() {
        String current = currentPassword.getText().toString();
        String next = newPassword.getText().toString();
        String confirmation = confirmPassword.getText().toString();
        if (current.isEmpty()) { currentPassword.setError(getString(R.string.security_error_current_password)); currentPassword.requestFocus(); return; }
        if (!UserValidator.isValidPassword(next)) {
            newPassword.setError(getString(R.string.signup_error_password));
            newPassword.requestFocus(); return;
        }
        if (!next.equals(confirmation)) {
            confirmPassword.setError(getString(R.string.security_error_mismatch)); confirmPassword.requestFocus(); return;
        }
        setBusy(passwordButton, true, R.string.security_updating);
        account.changePassword(current, next, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) {
                setBusy(passwordButton, false, R.string.security_update_password);
                currentPassword.setText(""); newPassword.setText(""); confirmPassword.setText("");
                show(getString(R.string.security_password_updated));
            }
            @Override public void onError(String message) {
                setBusy(passwordButton, false, R.string.security_update_password); show(message);
            }
        });
    }

    private void changeEmail() {
        String current = emailCurrentPassword.getText().toString();
        String email = UserValidator.normalizeEmail(newEmail.getText().toString());
        if (current.isEmpty()) { emailCurrentPassword.setError(getString(R.string.security_error_current_password)); emailCurrentPassword.requestFocus(); return; }
        if (!UserValidator.isValidEmail(email)) { newEmail.setError(getString(R.string.signup_error_email)); newEmail.requestFocus(); return; }
        setBusy(emailButton, true, R.string.security_sending);
        account.changeEmail(current, email, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) {
                setBusy(emailButton, false, R.string.security_send_link);
                emailCurrentPassword.setText("");
                show(getString(R.string.security_email_sent));
            }
            @Override public void onError(String message) {
                setBusy(emailButton, false, R.string.security_send_link); show(message);
            }
        });
    }

    private void setBusy(Button button, boolean busy, int label) {
        button.setEnabled(!busy); button.setText(label);
    }
    private void show(String message) {
        Snackbar.make(findViewById(R.id.securityRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
