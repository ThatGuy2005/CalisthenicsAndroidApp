package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.calisthenicsapp.Export.CsvExport;
import com.example.calisthenicsapp.Export.ExportData;
import com.example.calisthenicsapp.Export.JsonExport;
import com.example.calisthenicsapp.Scheduling.ScheduleCalendar;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Account management for the signed-in user: change password or email (FR-1.7), delete the
 * account (FR-1.10), and export all their data as JSON or CSV (NFR-5).
 *
 * Changing the password or email and deleting the account need a connection, and first confirm
 * the current password (Firebase only allows these right after a sign-in). Their onSuccess waits
 * for the server. The export reads SQLite, so it also works offline.
 */
public class AccountService extends UserDataService {
    private static final String TAG = "AccountService";
    private static final String NO_CONNECTION =
            "Something went wrong. Please check your connection and try again.";
    /** Firestore allows at most 500 writes in one batch. */
    private static final int BATCH_LIMIT = 500;
    private static final String[] SUBCOLLECTIONS = {"customWorkouts", "workoutLogs", "schedules"};

    public AccountService(Context context) {
        super(context);
    }

    /** With dependencies of your own, e.g. in a test; see {@link UserDataService}. */
    public AccountService(Context context, Supplier<SQLiteDatabase> database,
                          SessionManager sessionManager, Supplier<FirebaseFirestore> firestore) {
        super(context, database, sessionManager, firestore);
    }

    public void changePassword(String currentPassword, String newPassword,
                               ServiceCallback<Void> callback) {
        if (!UserValidator.isValidPassword(newPassword)) {
            fail(callback, "Password must be 8-64 characters with at least one letter and one digit.");
            return;
        }
        reauthenticate(currentPassword, callback, user ->
                user.updatePassword(newPassword).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) succeed(callback, null);
                    else fail(callback, AuthService.describeError(task.getException()));
                }));
    }

    /**
     * Sends a confirmation link to the new address. The email only changes once the user
     * clicks it; Firebase then signs them out, and AuthService.signIn copies the new address
     * into the profile when they sign in with it. Until then everything stays as it was.
     */
    public void changeEmail(String currentPassword, String newEmail, ServiceCallback<Void> callback) {
        String normalized = UserValidator.normalizeEmail(newEmail);
        if (!UserValidator.isValidEmail(normalized)) {
            fail(callback, "Enter a valid email address.");
            return;
        }
        FirebaseUser current = sessionManager.getCurrentUser();
        if (current != null && normalized.equals(current.getEmail())) {
            fail(callback, "That is already your email address.");
            return;
        }
        reauthenticate(currentPassword, callback, user ->
                user.verifyBeforeUpdateEmail(normalized).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) succeed(callback, null);
                    else fail(callback, AuthService.describeError(task.getException()));
                }));
    }

    /**
     * Permanently deletes everything (FR-1.10): the custom workouts, logs and schedules, the
     * profile, the username claim (so the name is free again) and finally the Firebase account.
     * Deleting the account signs the user out, and SyncManager then clears the local cache.
     * After that the schedules' events go from this device's calendar, and Firestore's own
     * offline copy on the device is cleared last (see removeCalendarEvents, clearOfflineCache).
     * If something fails part-way, the user is still signed in and can simply retry.
     */
    public void deleteAccount(String password, ServiceCallback<Void> callback) {
        reauthenticate(password, callback, user -> {
            DocumentReference profileDoc = userDoc(user.getUid());
            // Read from the server, not the cache: everything that exists must be found.
            List<Task<QuerySnapshot>> reads = new ArrayList<>();
            for (String name : SUBCOLLECTIONS) {
                reads.add(profileDoc.collection(name).get(Source.SERVER));
            }
            Task<DocumentSnapshot> profileRead = profileDoc.get(Source.SERVER);
            List<Task<?>> all = new ArrayList<>(reads);
            all.add(profileRead);
            Tasks.whenAll(all).addOnCompleteListener(readTask -> {
                if (!readTask.isSuccessful()) {
                    Log.w(TAG, "Reading data to delete failed", readTask.getException());
                    fail(callback, NO_CONNECTION);
                    return;
                }
                List<DocumentReference> children = new ArrayList<>();
                List<String> scheduleIds = new ArrayList<>();
                for (Task<QuerySnapshot> read : reads) {
                    for (DocumentSnapshot doc : read.getResult().getDocuments()) {
                        children.add(doc.getReference());
                        if (doc.getReference().getParent().getId().equals("schedules")) {
                            scheduleIds.add(doc.getId());
                        }
                    }
                }
                deleteChildren(user, children, scheduleIds, profileRead.getResult(), callback);
            });
        });
    }

    /** Step 2: the subcollection documents, in batches. */
    private void deleteChildren(FirebaseUser user, List<DocumentReference> children,
                                List<String> scheduleIds, DocumentSnapshot profile,
                                ServiceCallback<Void> callback) {
        List<Task<Void>> commits = new ArrayList<>();
        for (int start = 0; start < children.size(); start += BATCH_LIMIT) {
            WriteBatch batch = firestore().batch();
            for (DocumentReference doc
                    : children.subList(start, Math.min(start + BATCH_LIMIT, children.size()))) {
                batch.delete(doc);
            }
            commits.add(batch.commit());
        }
        Tasks.whenAll(commits).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Deleting workouts, logs or schedules failed", task.getException());
                fail(callback, NO_CONNECTION);
                return;
            }
            deleteProfileAndAccount(user, scheduleIds, profile, callback);
        });
    }

    /** Step 3: the username claim and the profile together, then the Firebase account. */
    private void deleteProfileAndAccount(FirebaseUser user, List<String> scheduleIds,
                                         DocumentSnapshot profile, ServiceCallback<Void> callback) {
        String username = profile.exists() ? profile.getString("username") : null;
        DocumentReference claimDoc = username == null ? null
                : firestore().collection("usernames").document(UserValidator.usernameKey(username));
        Task<DocumentSnapshot> claimRead = claimDoc == null
                ? Tasks.forResult(null) : claimDoc.get(Source.SERVER);

        claimRead.continueWithTask(readTask -> {
            DocumentSnapshot claim = readTask.getResult();     // throws if the read failed
            WriteBatch batch = firestore().batch();
            // Only release the name if it is ours; accounts made before usernames/ have none.
            if (claim != null && claim.exists() && user.getUid().equals(claim.getString("uid"))) {
                batch.delete(claimDoc);
            }
            batch.delete(profile.getReference());
            return batch.commit();
        }).continueWithTask(commitTask -> {
            commitTask.getResult();                            // throws if the commit failed
            return user.delete();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                removeCalendarEvents(scheduleIds, callback);
            } else {
                Log.w(TAG, "Deleting the profile or account failed", task.getException());
                Exception e = task.getException();
                fail(callback, e instanceof FirebaseAuthException
                        ? AuthService.describeError(e) : NO_CONNECTION);
            }
        });
    }

    /**
     * Step 4: deletes the events the app added to this device's calendar for the deleted
     * schedules (FR-6.2). SyncManager may have removed some already when the schedules went;
     * remove does nothing for those. Without calendar permission the events cannot be deleted,
     * so only their ids go. Runs only once the account is gone, so a failed deletion keeps them.
     */
    private void removeCalendarEvents(List<String> scheduleIds, ServiceCallback<Void> callback) {
        onDb(() -> {
            ScheduleCalendar calendar = new ScheduleCalendar(context(), db());
            for (String scheduleId : scheduleIds) calendar.remove(scheduleId);
            clearOfflineCache(callback);
        });
    }

    /**
     * Step 5: removes Firestore's offline copy from the device (FR-1.10). clearPersistence only
     * works on a stopped instance, so the instance is stopped first; the services and SyncManager
     * ask FirebaseFirestore.getInstance() on every use and get a fresh one afterwards. Listeners
     * on the old instance become no-ops. The account is already gone by now, so a failure here is
     * only logged and the deletion still counts as done.
     */
    private void clearOfflineCache(ServiceCallback<Void> callback) {
        FirebaseFirestore stopped = firestore();
        stopped.terminate()
                .continueWithTask(task -> stopped.clearPersistence())
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Clearing Firestore's offline cache failed", task.getException());
                    }
                    succeed(callback, null);
                });
    }

    /**
     * Saves all of the user's data as CSV files in one ZIP (NFR-5), see {@link CsvExport} for
     * the files. target is where to write it, typically the Uri the user picked in the system's
     * "save as" screen (ActivityResultContracts.CreateDocument("application/zip")). Read from
     * SQLite, so it also works offline.
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
     * it holds. Read from SQLite, so it also works offline.
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

    private interface SignedInStep {
        void run(FirebaseUser user);
    }

    /** Confirms the current password, then runs next. Wrong password and errors go to callback. */
    private void reauthenticate(String password, ServiceCallback<?> callback, SignedInStep next) {
        FirebaseUser user = sessionManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (password == null || password.isEmpty()) {
            fail(callback, "Enter your current password.");
            return;
        }
        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        next.run(user);
                        return;
                    }
                    Exception e = task.getException();
                    String code = e instanceof FirebaseAuthException
                            ? ((FirebaseAuthException) e).getErrorCode() : "";
                    boolean wrongPassword = code.equals("ERROR_WRONG_PASSWORD")
                            || code.equals("ERROR_INVALID_CREDENTIAL");
                    fail(callback, wrongPassword
                            ? "Your current password is incorrect." : AuthService.describeError(e));
                });
    }
}
