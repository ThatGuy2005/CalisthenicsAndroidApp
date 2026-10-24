package com.example.calisthenicsapp.Scheduling;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The email of a scheduled workout (FR-6.3): sent to the user's own address with the workout
 * name, date, time and exercises, optionally with an .ics calendar invitation attached.
 */
public final class WorkoutEmail {
    /** Must match file_paths.xml. */
    private static final String ICS_DIR = "ics";

    private WorkoutEmail() {
    }

    /**
     * An Intent for startActivity that opens the user's email app with the email, or lets them
     * pick one if they have several; they send it. Null if no email app is installed.
     * nowSeconds is the .ics file's creation time.
     *
     * @throws IOException if the .ics file could not be written
     */
    public static Intent build(Context context, WorkoutSummary summary, String email,
                               boolean attachIcs, long nowSeconds) throws IOException {
        // Email apps are the ones that handle mailto: (see <queries> in the manifest). SEND
        // is what carries a file, so it goes to the activity that handles mailto:, or else to
        // its app. A SENDTO selector on SEND would do the same, but some Android versions'
        // chooser then finds no app at all.
        List<ResolveInfo> mailApps = context.getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")), 0);
        if (mailApps.isEmpty()) return null;

        TimeZone zone = TimeZone.getDefault();
        Locale locale = Locale.getDefault();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        send.putExtra(Intent.EXTRA_SUBJECT, summary.getEmailSubject(zone, locale));
        send.putExtra(Intent.EXTRA_TEXT, summary.getEmailBody(zone, locale));
        if (attachIcs) {
            Uri ics = writeIcs(context, summary, nowSeconds);
            send.setType("text/calendar");
            send.putExtra(Intent.EXTRA_STREAM, ics);
            send.setClipData(ClipData.newRawUri("", ics));   // lets the grant reach the app
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        List<Intent> perApp = new ArrayList<>();
        List<String> packages = new ArrayList<>();
        for (ResolveInfo app : mailApps) {
            String packageName = app.activityInfo.packageName;
            if (packages.contains(packageName)) continue;
            Intent forApp = new Intent(send).setClassName(packageName, app.activityInfo.name);
            if (forApp.resolveActivity(context.getPackageManager()) == null) {
                forApp = new Intent(send).setPackage(packageName);
                if (forApp.resolveActivity(context.getPackageManager()) == null) continue;
            }
            packages.add(packageName);
            perApp.add(forApp);
        }
        if (perApp.isEmpty()) return null;
        if (perApp.size() == 1) return perApp.get(0);
        Intent chooser = Intent.createChooser(perApp.remove(0), "Send workout by email");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, perApp.toArray(new Intent[0]));
        return chooser;
    }

    /** cache/ics/workout-{id}.ics, shared through the FileProvider declared in the manifest. */
    private static Uri writeIcs(Context context, WorkoutSummary summary, long nowSeconds)
            throws IOException {
        File dir = new File(context.getCacheDir(), ICS_DIR);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        File file = new File(dir, "workout-" + summary.getScheduleId() + ".ics");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            out.write(summary.toIcs(nowSeconds));
        }
        try {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (IllegalArgumentException e) {   // the directory is not in file_paths.xml
            throw new IOException("Cannot share " + file, e);
        }
    }
}
