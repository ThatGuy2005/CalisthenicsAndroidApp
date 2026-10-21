package com.example.calisthenicsapp;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.Service.AccountService;
import com.example.calisthenicsapp.Service.AuthService;
import com.example.calisthenicsapp.Service.ServiceCallback;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Data export and account deletion actions, backed by AccountService. */
public class DataPrivacyActivity extends AppCompatActivity {
    private final ExecutorService fileWriter = Executors.newSingleThreadExecutor();
    private AccountService account;
    private Button jsonButton, csvButton, deleteButton;
    private ActivityResultLauncher<String> jsonPicker, csvPicker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_data_privacy);
        account = new AccountService(this);
        jsonButton = findViewById(R.id.exportJsonButton);
        csvButton = findViewById(R.id.exportCsvButton);
        deleteButton = findViewById(R.id.deleteAccountButton);
        jsonPicker = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                uri -> { if (uri != null) exportJson(uri); });
        csvPicker = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"),
                uri -> { if (uri != null) exportCsv(uri); });
        findViewById(R.id.dataPrivacyBack).setOnClickListener(v -> finish());
        findViewById(R.id.openPrivacyPolicyButton).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, PrivacyPolicyActivity.class)));
        jsonButton.setOnClickListener(v -> jsonPicker.launch("calisthenics-data.json"));
        csvButton.setOnClickListener(v -> csvPicker.launch("calisthenics-data-csv.zip"));
        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void exportJson(Uri target) {
        setExporting(true);
        account.exportMyData(new ServiceCallback<String>() {
            @Override public void onSuccess(String json) {
                fileWriter.execute(() -> {
                    String error = null;
                    try (OutputStream out = getContentResolver().openOutputStream(target, "wt")) {
                        if (out == null) throw new IOException("Could not open the selected file.");
                        out.write(json.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException | RuntimeException e) { error = "Could not save the JSON export."; }
                    String result = error;
                    runOnUiThread(() -> {
                        setExporting(false);
                        show(result == null ? "JSON export saved." : result);
                    });
                });
            }
            @Override public void onError(String message) { setExporting(false); show(message); }
        });
    }

    private void exportCsv(Uri target) {
        setExporting(true);
        account.exportMyDataCsv(target, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) { setExporting(false); show("CSV export saved as a ZIP file."); }
            @Override public void onError(String message) { setExporting(false); show(message); }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete account permanently?")
                .setMessage("Your profile, custom workouts, schedules and workout history will be deleted. This cannot be undone.")
                .setNegativeButton("Keep account", null)
                .setPositiveButton("Continue", (dialog, which) -> askDeletePassword())
                .show();
    }

    private void askDeletePassword() {
        EditText password = new EditText(this);
        password.setHint("Current password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextColor(getColor(R.color.performance_graphite));
        password.setHintTextColor(getColor(R.color.performance_muted));
        password.setBackgroundResource(R.drawable.input_background);
        password.setPadding(dp(14), dp(10), dp(14), dp(10));
        new AlertDialog.Builder(this)
                .setTitle("Confirm it's you")
                .setMessage("Enter your password to permanently delete the account.")
                .setView(password)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete account", (dialog, which) -> {
                    String value = password.getText().toString();
                    if (value.isEmpty()) { show("Enter your current password to continue."); return; }
                    deleteButton.setEnabled(false); deleteButton.setText("Deleting account…");
                    account.deleteAccount(value, new ServiceCallback<Void>() {
                        @Override public void onSuccess(Void result) {
                            new AuthService(DataPrivacyActivity.this).signOut();
                            android.content.Intent intent = new android.content.Intent(
                                    DataPrivacyActivity.this, MainActivity.class);
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        @Override public void onError(String message) {
                            deleteButton.setEnabled(true); deleteButton.setText("Delete my account"); show(message);
                        }
                    });
                }).show();
    }

    private void setExporting(boolean exporting) {
        jsonButton.setEnabled(!exporting); csvButton.setEnabled(!exporting);
        jsonButton.setText(exporting ? "Preparing export…" : "Save as JSON");
        csvButton.setText(exporting ? "Preparing export…" : "Save tables as CSV (.zip)");
    }
    private void show(String message) { Snackbar.make(findViewById(R.id.dataPrivacyRoot), message, Snackbar.LENGTH_LONG).show(); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
