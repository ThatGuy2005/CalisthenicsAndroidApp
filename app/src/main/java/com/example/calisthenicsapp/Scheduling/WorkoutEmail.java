package com.example.calisthenicsapp.Scheduling;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
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
     * An Intent for startActivity that lets the user pick one of their email apps, which sends
     * it. Null if no email app is installed. nowSeconds is the .ics file's creation time.
     *
     * @throws IOException if the .ics file could not be written
     */
    public static Intent build(Context context, WorkoutSummary summary, String email,
                               boolean attachIcs, long nowSeconds) throws IOException {
        // The selector limits the choice to email apps; SEND itself is what carries a file.
        Intent mailApps = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        if (mailApps.resolveActivity(context.getPackageManager()) == null) return null;

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
        send.setSelector(mailApps);
        return Intent.createChooser(send, "Send workout by email");
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
