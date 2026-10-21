package com.example.calisthenicsapp;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Privacy information reflecting the data flows currently implemented in the app. The text is
 * the HTML string privacy_policy_body in strings.xml.
 */
public class PrivacyPolicyActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_privacy_policy);
        findViewById(R.id.privacyPolicyBack).setOnClickListener(v -> finish());
        TextView body = findViewById(R.id.privacyPolicyBody);
        body.setText(Html.fromHtml(getString(R.string.privacy_policy_body),
                Html.FROM_HTML_MODE_COMPACT));
        body.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
