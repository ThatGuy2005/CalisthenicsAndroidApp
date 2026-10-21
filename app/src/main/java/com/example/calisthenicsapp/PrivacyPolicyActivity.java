package com.example.calisthenicsapp;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Privacy information reflecting the data flows currently implemented in the app. */
public class PrivacyPolicyActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_privacy_policy);
        findViewById(R.id.privacyPolicyBack).setOnClickListener(v -> finish());
        TextView body = findViewById(R.id.privacyPolicyBody);
        body.setText(Html.fromHtml(POLICY, Html.FROM_HTML_MODE_COMPACT));
        body.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private static final String POLICY =
            "<b>Last updated: 21 October 2026</b><br><br>" +
            "<b>Who is responsible?</b><br>" +
            "Tamas Csongor is the controller for personal data processed by Calisthenics. " +
            "Contact: <a href=\"mailto:tamas.csongor@stud.umfst.ro\">tamas.csongor@stud.umfst.ro</a>.<br><br>" +
            "<b>What information is used?</b><br>" +
            "Your account username and email address; optional profile details such as height, " +
            "weight, gender and birth year; and the custom workouts, scheduled workouts and " +
            "workout history you create. Passwords are handled by Firebase Authentication and " +
            "are not stored in the app's workout database. Firebase Authentication may also " +
            "process IP addresses and device/browser information for sign-in security. The app " +
            "keeps a local database copy so your training information can be displayed on the device.<br><br>" +
            "<b>Why is it used?</b><br>" +
            "Account and workout information is used to sign you in, sync your information " +
            "between devices, show your training plans and history, and calculate estimates " +
            "when relevant profile details are provided. This processing is needed to provide " +
            "the features you choose to use.<br><br>" +
            "<b>Service providers and sharing</b><br>" +
            "Firebase Authentication and Cloud Firestore, provided by Google, support sign-in " +
            "and cloud storage/synchronization. Google may process service data as described in " +
            "its <a href=\"https://firebase.google.com/support/privacy\">Firebase privacy information</a> " +
            "and <a href=\"https://policies.google.com/privacy\">Privacy Policy</a>. Firebase " +
            "Authentication processes data in the United States; Cloud Firestore uses Google's " +
            "global infrastructure, and its processing location depends on the project's database " +
            "configuration. This means data may be processed outside the European Economic Area. " +
            "Google describes its data-processing safeguards in its " +
            "<a href=\"https://firebase.google.com/terms/data-processing-terms\">Firebase data processing terms</a>. When you " +
            "choose to add a workout to your device calendar or share it by email, the app passes " +
            "the requested workout details to the selected device app. That app's provider then " +
            "handles the information under its own terms.<br><br>" +
            "<b>Storage and deletion</b><br>" +
            "Account information is kept while your account is active. You can export a JSON " +
            "copy or CSV files from Settings → Data &amp; privacy. If you request account deletion, " +
            "the app removes your profile, custom workouts, schedules, workout history and sign-in " +
            "account from active systems after password confirmation. We aim to complete deletion " +
            "of our active account records within 30 days (the in-app deletion normally starts " +
            "immediately). Google states that some Firebase account data may remain in live or " +
            "backup systems for up to 180 days after deletion is initiated. Limited technical " +
            "records may also be retained under provider terms or where required by law.<br><br>" +
            "<b>Your choices and rights</b><br>" +
            "You can update profile details in the app, export your information, or delete your " +
            "account. Depending on applicable law, you may also request access, correction, " +
            "restriction or portability, or object to processing, by contacting the address above. " +
            "You may lodge a complaint with the Hungarian National Authority for Data Protection " +
            "and Freedom of Information (<a href=\"https://www.naih.hu/\">NAIH</a>).<br><br>" +
            "<b>Security</b><br>" +
            "The app uses authenticated accounts and account-scoped cloud records. Keep your " +
            "password private and contact us if you believe your account has been accessed " +
            "without permission.";
}
