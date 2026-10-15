package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.util.Log;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The signed-in user's profile (users/{uid}). Height is in cm and weight in kg.
 * The email belongs to Firebase Authentication and is not changed here.
 *
 * Usernames are unique: each one is claimed by a usernames/{lowercased name} document holding
 * the owner's uid, and firestore.rules only accept a profile whose username the user has claimed.
 */
public class ProfileService extends UserDataService {
    private static final String TAG = "ProfileService";
    private static final String USERNAME_TAKEN = "That username is taken.";
    private static final String USERNAME_CHANGE_FAILED =
            "Could not change the username. Changing it needs an internet connection.";

    public ProfileService(Context context) {
        super(context);
    }

    /** Returns null if the profile has not been synced to this device yet. */
    public void getMyProfile(ServiceCallback<User> callback) {
        readForUser(callback, uid -> new UserRepository(db()).getOne(uid));
    }

    /**
     * Overwrites all four fields; pass the current value for anything that did not change.
     * Edits that keep the username (letter case aside) work offline like the other services.
     * Changing the username needs a connection, and onSuccess waits for the server's answer.
     */
    public void updateProfile(String username, Double height, Double weight, String gender,
                              ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (!UserValidator.isValidUsername(username)) {
            fail(callback, "Username must be 3-20 letters, digits, '.', '_' or '-'.");
            return;
        }
        String error = AuthService.checkBodyData(height, weight, gender);
        if (error != null) {
            fail(callback, error);
            return;
        }

        Map<String, Object> changes = new HashMap<>();
        changes.put("username", username);
        changes.put("height", height);
        changes.put("weight", weight);
        changes.put("gender", gender);
        changes.put("updatedAt", now());

        String newKey = UserValidator.usernameKey(username);
        onDb(() -> {
            User current = new UserRepository(db()).getOne(uid);
            if (current != null && UserValidator.usernameKey(current.getUsername()).equals(newKey)) {
                watchWrite(userDoc(uid).update(changes), "Updating profile of " + uid);
                succeed(callback, null);
            } else {
                changeUsername(uid, newKey, changes, callback);
            }
        });
    }

    /**
     * Claims the new username, releases the old one and updates the profile in one transaction.
     * The transaction reads the profile from the server rather than trusting SQLite, which may
     * be behind (e.g. the name was changed on another device).
     */
    private void changeUsername(String uid, String newKey, Map<String, Object> changes,
                                ServiceCallback<Void> callback) {
        DocumentReference profileDoc = userDoc(uid);
        DocumentReference newClaimDoc = usernameDoc(newKey);

        // Returns null on success, or the message to show. Firestore requires all reads first.
        firestore().runTransaction(transaction -> {
                    DocumentSnapshot profile = transaction.get(profileDoc);
                    if (!profile.exists()) return USERNAME_CHANGE_FAILED;
                    DocumentSnapshot newClaim = transaction.get(newClaimDoc);
                    if (newClaim.exists() && !uid.equals(newClaim.getString("uid"))) {
                        return USERNAME_TAKEN;
                    }
                    String oldName = profile.getString("username");
                    String oldKey = oldName == null ? null : UserValidator.usernameKey(oldName);
                    DocumentReference oldClaimDoc = oldKey == null || oldKey.equals(newKey)
                            ? null : usernameDoc(oldKey);
                    DocumentSnapshot oldClaim = oldClaimDoc == null ? null : transaction.get(oldClaimDoc);

                    // Accounts made before usernames/ existed have no old claim to release.
                    if (oldClaim != null && oldClaim.exists() && uid.equals(oldClaim.getString("uid"))) {
                        transaction.delete(oldClaimDoc);
                    }
                    if (!newClaim.exists()) {
                        transaction.set(newClaimDoc, Collections.singletonMap("uid", uid));
                    }
                    transaction.update(profileDoc, changes);
                    return null;
                })
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Changing the username of " + uid + " failed", task.getException());
                        fail(callback, USERNAME_CHANGE_FAILED);
                    } else if (task.getResult() != null) {
                        fail(callback, task.getResult());
                    } else {
                        succeed(callback, null);
                    }
                });
    }

    private DocumentReference usernameDoc(String key) {
        return firestore().collection("usernames").document(key);
    }
}
