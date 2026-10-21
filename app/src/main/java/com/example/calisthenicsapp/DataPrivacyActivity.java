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
                    boolean saved = false;
                    try (OutputStream out = getContentResolver().openOutputStream(target, "wt")) {
                        if (out == null) throw new IOException("Could not open the selected file.");
                        out.write(json.getBytes(StandardCharsets.UTF_8));
                        saved = true;
                    } catch (IOException | RuntimeException ignored) { }
                    int result = saved ? R.string.data_privacy_json_saved : R.string.data_privacy_json_failed;
                    runOnUiThread(() -> {
                        setExporting(false);
                        show(getString(result));
                    });
                });
            }
            @Override public void onError(String message) { setExporting(false); show(message); }
        });
    }

    private void exportCsv(Uri target) {
        setExporting(true);
        account.exportMyDataCsv(target, new ServiceCallback<Void>() {
            @Override public void onSuccess(Void result) { setExporting(false); show(getString(R.string.data_privacy_csv_saved)); }
            @Override public void onError(String message) { setExporting(false); show(message); }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.data_privacy_confirm_title)
                .setMessage(R.string.data_privacy_confirm_message)
                .setNegativeButton(R.string.data_privacy_confirm_keep, null)
                .setPositiveButton(R.string.data_privacy_confirm_continue, (dialog, which) -> askDeletePassword())
                .show();
    }

    private void askDeletePassword() {
        EditText password = new EditText(this);
        password.setHint(R.string.security_hint_current_password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextColor(getColor(R.color.performance_graphite));
        password.setHintTextColor(getColor(R.color.performance_muted));
        password.setBackgroundResource(R.drawable.input_background);
        password.setPadding(dp(14), dp(10), dp(14), dp(10));
        new AlertDialog.Builder(this)
                .setTitle(R.string.data_privacy_password_title)
                .setMessage(R.string.data_privacy_password_message)
                .setView(password)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.data_privacy_password_delete, (dialog, which) -> {
                    String value = password.getText().toString();
                    if (value.isEmpty()) { show(getString(R.string.data_privacy_error_password)); return; }
                    deleteButton.setEnabled(false); deleteButton.setText(R.string.data_privacy_deleting);
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
                            deleteButton.setEnabled(true); deleteButton.setText(R.string.data_privacy_delete_button); show(message);
                        }
                    });
                }).show();
    }

    private void setExporting(boolean exporting) {
        jsonButton.setEnabled(!exporting); csvButton.setEnabled(!exporting);
        jsonButton.setText(exporting ? R.string.data_privacy_preparing : R.string.data_privacy_save_json);
        csvButton.setText(exporting ? R.string.data_privacy_preparing : R.string.data_privacy_save_csv);
    }
    private void show(String message) { Snackbar.make(findViewById(R.id.dataPrivacyRoot), message, Snackbar.LENGTH_LONG).show(); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
