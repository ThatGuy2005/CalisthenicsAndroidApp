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

import com.example.calisthenicsapp.Database.AppDatabase;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Service.UserService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private AppDatabase database;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        database = new AppDatabase(this);
        UserService userService = new UserService(
                new UserRepository(database.getWritableDatabase()));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button signupButton = findViewById(R.id.openSignupButton);
        signupButton.setOnClickListener(view ->
                startActivity(new android.content.Intent(this, SignupActivity.class)));

        Button loginButton = findViewById(R.id.loginButton);
        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        loginButton.setOnClickListener(view -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter your username and password.", Toast.LENGTH_SHORT).show();
            } else {
                loginButton.setEnabled(false);
                databaseExecutor.execute(() -> {
                    boolean success = userService.login(username, password);
                    runOnUiThread(() -> {
                        loginButton.setEnabled(true);
                        Toast.makeText(this,
                                success ? "Login successful." : "Login failed. Check your details.",
                                Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        databaseExecutor.shutdown();
        if (database != null) database.close();
        super.onDestroy();
    }
}
