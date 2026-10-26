package com.example.calisthenicsapp.ui.settings;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calisthenicsapp.R;

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
        // Makes the links in the text tappable.
        body.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
