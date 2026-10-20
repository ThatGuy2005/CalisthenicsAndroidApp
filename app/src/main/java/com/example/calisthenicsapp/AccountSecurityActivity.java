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
        if (current.isEmpty()) { currentPassword.setError("Enter your current password."); currentPassword.requestFocus(); return; }
        if (!UserValidator.isValidPassword(next)) {
            newPassword.setError("Use 8–64 characters with at least one letter and one digit.");
            newPassword.requestFocus(); return;
        }
        if (!next.equals(confirmation)) {
            confirmPassword.setError("The passwords don't match."); confirmPassword.requestFocus(); return;
        }
        setBusy(passwordButton, true, "Updating…");
        account.changePassword(current, next, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) {
                setBusy(passwordButton, false, "Update password");
                currentPassword.setText(""); newPassword.setText(""); confirmPassword.setText("");
                show("Your password has been updated.");
            }
            @Override public void onError(String message) {
                setBusy(passwordButton, false, "Update password"); show(message);
            }
        });
    }

    private void changeEmail() {
        String current = emailCurrentPassword.getText().toString();
        String email = UserValidator.normalizeEmail(newEmail.getText().toString());
        if (current.isEmpty()) { emailCurrentPassword.setError("Enter your current password."); emailCurrentPassword.requestFocus(); return; }
        if (!UserValidator.isValidEmail(email)) { newEmail.setError("Enter a valid email address."); newEmail.requestFocus(); return; }
        setBusy(emailButton, true, "Sending…");
        account.changeEmail(current, email, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) {
                setBusy(emailButton, false, "Send confirmation link");
                emailCurrentPassword.setText("");
                show("Confirmation link sent. Your email will change after you open it.");
            }
            @Override public void onError(String message) {
                setBusy(emailButton, false, "Send confirmation link"); show(message);
            }
        });
    }

    private void setBusy(Button button, boolean busy, String label) {
        button.setEnabled(!busy); button.setText(label);
    }
    private void show(String message) {
        Snackbar.make(findViewById(R.id.securityRoot), message, Snackbar.LENGTH_LONG).show();
    }
}
