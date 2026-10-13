package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.util.Log;

import com.example.calisthenicsapp.CoreComponents.CustomExerciseOrderEntry;
import com.example.calisthenicsapp.CoreComponents.CustomWorkout;
import com.example.calisthenicsapp.CoreComponents.ScheduledWorkout;
import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.CoreComponents.WorkoutLog;
import com.example.calisthenicsapp.Repos.CustomExerciseOrderRepository;
import com.example.calisthenicsapp.Repos.CustomWorkoutRepository;
import com.example.calisthenicsapp.Repos.ScheduledWorkoutRepository;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Repos.WorkoutLogRepository;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Account management for the signed-in user: change password or email (FR-1.7), delete the
 * account (FR-1.10), and export all their data (NFR-5).
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
                for (Task<QuerySnapshot> read : reads) {
                    for (DocumentSnapshot doc : read.getResult().getDocuments()) {
                        children.add(doc.getReference());
                    }
                }
                deleteChildren(user, children, profileRead.getResult(), callback);
            });
        });
    }

    /** Step 2: the subcollection documents, in batches. */
    private void deleteChildren(FirebaseUser user, List<DocumentReference> children,
                                DocumentSnapshot profile, ServiceCallback<Void> callback) {
        List<Task<Void>> commits = new ArrayList<>();
        for (int start = 0; start < children.size(); start += BATCH_LIMIT) {
            WriteBatch batch = firestore.batch();
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
            deleteProfileAndAccount(user, profile, callback);
        });
    }

    /** Step 3: the username claim and the profile together, then the Firebase account. */
    private void deleteProfileAndAccount(FirebaseUser user, DocumentSnapshot profile,
                                         ServiceCallback<Void> callback) {
        String username = profile.exists() ? profile.getString("username") : null;
        DocumentReference claimDoc = username == null ? null
                : firestore.collection("usernames").document(UserValidator.usernameKey(username));
        Task<DocumentSnapshot> claimRead = claimDoc == null
                ? Tasks.forResult(null) : claimDoc.get(Source.SERVER);

        claimRead.continueWithTask(readTask -> {
            DocumentSnapshot claim = readTask.getResult();     // throws if the read failed
            WriteBatch batch = firestore.batch();
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
                succeed(callback, null);
            } else {
                Log.w(TAG, "Deleting the profile or account failed", task.getException());
                Exception e = task.getException();
                fail(callback, e instanceof FirebaseAuthException
                        ? AuthService.describeError(e) : NO_CONNECTION);
            }
        });
    }

    /**
     * All of the user's data as pretty-printed JSON (NFR-5): profile, custom workouts with
     * their exercises, workout logs with their snapshots, and schedules. Read from SQLite.
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
                succeed(callback, buildExport(uid).toString(2));
            } catch (JSONException e) {
                Log.w(TAG, "Building the data export failed", e);
                fail(callback, "Could not build the export.");
            }
        });
    }

    private JSONObject buildExport(String uid) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("exportedAt", now());

        User user = new UserRepository(db()).getOne(uid);
        JSONObject profile = new JSONObject();
        profile.put("uid", uid);
        if (user != null) {
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("heightCm", orNull(user.getHeight()));
            profile.put("weightKg", orNull(user.getWeight()));
            profile.put("gender", orNull(user.getGender()));
            profile.put("createdAt", user.getCreatedAt());
            profile.put("updatedAt", user.getUpdatedAt());
        }
        root.put("profile", profile);

        CustomExerciseOrderRepository orderRepository = new CustomExerciseOrderRepository(db());
        JSONArray workouts = new JSONArray();
        for (CustomWorkout w : new CustomWorkoutRepository(db()).getByUid(uid)) {
            JSONArray exercises = new JSONArray();
            for (CustomExerciseOrderEntry e : orderRepository.getByCustomWorkout(w.getId())) {
                JSONObject o = new JSONObject();
                o.put("exerciseName", e.getExerciseName());
                o.put("position", e.getPosition());
                o.put("sets", orNull(e.getSets()));
                o.put("reps", orNull(e.getReps()));
                o.put("seconds", orNull(e.getSeconds()));
                o.put("restSeconds", orNull(e.getRestSeconds()));
                exercises.put(o);
            }
            JSONObject o = new JSONObject();
            o.put("id", w.getId());
            o.put("name", w.getName());
            o.put("exercises", exercises);
            workouts.put(o);
        }
        root.put("customWorkouts", workouts);

        JSONArray logs = new JSONArray();
        for (WorkoutLog l : new WorkoutLogRepository(db()).getByUid(uid)) {
            JSONObject o = new JSONObject();
            o.put("id", l.getId());
            o.put("workoutName", l.getWorkoutName());
            o.put("startedAt", l.getStartedAt());
            o.put("durationSeconds", l.getDurationSeconds());
            o.put("kcal", l.getKcal());
            o.put("status", l.getStatus());
            o.put("exercises", WorkoutLogRepository.snapshotToJson(l.getExercises()));
            logs.put(o);
        }
        root.put("workoutLogs", logs);

        JSONArray schedules = new JSONArray();
        for (ScheduledWorkout s : new ScheduledWorkoutRepository(db()).getByUid(uid)) {
            JSONObject o = new JSONObject();
            o.put("id", s.getId());
            o.put("workoutName", orNull(s.getWorkoutName()));
            o.put("customWorkoutId", orNull(s.getCustomWorkoutId()));
            o.put("scheduledAt", s.getScheduledAt());
            schedules.put(o);
        }
        root.put("schedules", schedules);
        return root;
    }

    private static Object orNull(Object value) {
        return value == null ? JSONObject.NULL : value;
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
