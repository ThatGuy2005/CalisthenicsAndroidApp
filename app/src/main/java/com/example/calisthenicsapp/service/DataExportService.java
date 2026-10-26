package com.example.calisthenicsapp.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.calisthenicsapp.export.CsvExport;
import com.example.calisthenicsapp.export.ExportData;
import com.example.calisthenicsapp.export.JsonExport;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * "Download my data" (NFR-5): all of the signed-in user's data as JSON or as CSV files in a ZIP.
 * Read from SQLite, so it also works offline.
 */
public class DataExportService extends UserDataService {
    private static final String TAG = "DataExportService";

    /** Uses the app's database, FirebaseAuth and Firestore. */
    public DataExportService(Context context) {
        super(context);
    }

    /** With dependencies of your own, e.g. in a test; see {@link UserDataService}. */
    public DataExportService(Context context, Supplier<SQLiteDatabase> database,
                             SessionManager sessionManager, Supplier<FirebaseFirestore> firestore) {
        super(context, database, sessionManager, firestore);
    }

    /**
     * Saves all of the user's data as CSV files in one ZIP (NFR-5), see {@link CsvExport} for
     * the files. target is where to write it, typically the Uri the user picked in the system's
     * "save as" screen (ActivityResultContracts.CreateDocument("application/zip")).
     */
    public void exportMyDataCsv(Uri target, ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (target == null) {
            fail(callback, "Choose where to save the export.");
            return;
        }
        onDb(() -> {
            Map<String, String> files = CsvExport.build(ExportData.read(db(), uid));
            // "wt": truncate, in case an existing file is being replaced.
            try (OutputStream out = context().getContentResolver().openOutputStream(target, "wt")) {
                if (out == null) throw new IOException("No output stream for " + target);
                CsvExport.writeZip(files, out);
                succeed(callback, null);
            } catch (IOException | RuntimeException e) {   // RuntimeException: e.g. SecurityException
                Log.w(TAG, "Writing the CSV export failed", e);
                fail(callback, "Could not save the export.");
            }
        });
    }

    /**
     * All of the user's data as pretty-printed JSON (NFR-5), see {@link JsonExport} for what
     * it holds.
     * To let the user save it, start an Intent.ACTION_CREATE_DOCUMENT with type
     * "application/json", and write the string to the returned Uri with
     * getContentResolver().openOutputStream(uri).
     */
    public void exportMyData(ServiceCallback<String> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        onDb(() -> {
            try {
                succeed(callback, JsonExport.build(ExportData.read(db(), uid), now()).toString(2));
            } catch (JSONException e) {
                Log.w(TAG, "Building the data export failed", e);
                fail(callback, "Could not build the export.");
            }
        });
    }
}
