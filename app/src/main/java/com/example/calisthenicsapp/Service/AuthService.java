package com.example.calisthenicsapp.Service;

import android.content.Context;
import android.util.Log;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Database.SQLManager;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps FirebaseAuth (email/password) and the users/{uid} Firestore profile document.
 * Every method is asynchronous and reports back through an {@link AuthCallback}; nothing here
 * blocks the calling thread. Sign-up claims the username in usernames/{lowercased name} and
 * writes the profile in one transaction; if that fails (name taken, no connection) the new
 * Auth account is deleted again. On a successful sign-up, the Firestore profile write is followed
 * by a local SQLite insert (on a background thread) so the per-user tables that foreign-key to
 * Users(Uid) have something to reference immediately. Stage 4's sync listeners take over
 * keeping that local row current after this point, including mirroring it on sign-in on a
 * device that has never seen this user before.
 */
public class AuthService {

    private static final String TAG = "AuthService";
    private static final String USERNAME_TAKEN = "That username is taken.";

    public interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final ExecutorService localDbExecutor = Executors.newSingleThreadExecutor();

    /** Asked for on every use, never kept: see UserDataService.firestore(). */
    private static FirebaseFirestore firestore() {
        return FirebaseFirestore.getInstance();
    }

    public AuthService(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository(
                SQLManager.getInstance(context).getWritableDatabase());
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void signUp(String username, String email, String password, Double height,
                        Double weight, String gender, AuthCallback<FirebaseUser> callback) {
        String normalizedEmail = UserValidator.normalizeEmail(email);
        if (!UserValidator.isValidUsername(username)) {
            callback.onError("Username must be 3-20 letters, digits, '.', '_' or '-'.");
            return;
        }
        if (!UserValidator.isValidEmail(normalizedEmail)) {
            callback.onError("Enter a valid email address.");
            return;
        }
        if (!UserValidator.isValidPassword(password)) {
            callback.onError("Password must be 8-64 characters with at least one letter and one digit.");
            return;
        }
        String error = checkBodyData(height, weight, gender);
        if (error != null) {
            callback.onError(error);
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().getUser() == null) {
                        callback.onError(describeError(task.getException()));
                        return;
                    }
                    saveProfile(task.getResult().getUser(), username, height, weight, gender, callback);
                });
    }

    /** The optional profile fields; null if they are fine. Shared with ProfileService. */
    static String checkBodyData(Double height, Double weight, String gender) {
        if (!UserValidator.isValidHeight(height)) return "Height must be between 50 and 300 cm.";
        if (!UserValidator.isValidWeight(weight)) return "Weight must be between 20 and 500 kg.";
        if (!UserValidator.isValidGender(gender)) return "Gender can be at most 30 characters.";
        return null;
    }

    private void saveProfile(FirebaseUser firebaseUser, String username, Double height,
                              Double weight, String gender, AuthCallback<FirebaseUser> callback) {
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", username);
        profile.put("email", firebaseUser.getEmail());
        profile.put("height", height);
        profile.put("weight", weight);
        profile.put("gender", gender);
        profile.put("createdAt", now);
        profile.put("updatedAt", now);

        DocumentReference claimDoc = firestore().collection("usernames")
                .document(UserValidator.usernameKey(username));
        DocumentReference profileDoc = firestore().collection("users").document(firebaseUser.getUid());

        // Claim and profile are written together, so neither can exist without the other.
        // Returns false, without writing anything, if someone else already holds the username.
        firestore().runTransaction(transaction -> {
                    DocumentSnapshot claim = transaction.get(claimDoc);
                    if (claim.exists()) return false;
                    transaction.set(claimDoc, Collections.singletonMap("uid", firebaseUser.getUid()));
                    transaction.set(profileDoc, profile);
                    return true;
                })
                .addOnCompleteListener(txTask -> {
                    if (!txTask.isSuccessful() || !Boolean.TRUE.equals(txTask.getResult())) {
                        String message = txTask.isSuccessful()
                                ? USERNAME_TAKEN : describeError(txTask.getException());
                        discardAccount(firebaseUser, message, callback);
                        return;
                    }
                    User localUser = new User(firebaseUser.getUid(), username, firebaseUser.getEmail(),
                            height == null ? 0 : height, weight == null ? 0 : weight, gender, now, now);
                    localDbExecutor.execute(() -> {
                        userRepository.insert(localUser);   // best-effort local mirror, see class Javadoc
                        callback.onSuccess(firebaseUser);
                    });
                });
    }

    /**
     * Deletes an Auth account whose profile could not be saved, so no account is left without a
     * users/{uid} document and the email can be used to sign up again. Deleting also signs it out.
     */
    private void discardAccount(FirebaseUser firebaseUser, String message,
                                AuthCallback<FirebaseUser> callback) {
        firebaseUser.delete().addOnCompleteListener(deleteTask -> {
            if (!deleteTask.isSuccessful()) {
                Log.w(TAG, "Could not delete Auth user " + firebaseUser.getUid()
                        + " after a failed sign-up", deleteTask.getException());
                firebaseAuth.signOut();
            }
            callback.onError(message);
        });
    }

    public void signIn(String email, String password, AuthCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(UserValidator.normalizeEmail(email), password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        syncProfileEmail(task.getResult().getUser());
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError(describeError(task.getException()));
                    }
                });
    }

    /**
     * An email change (AccountService.changeEmail) only takes effect in Firebase Auth once the
     * user clicks the link, and then they have to sign in again with the new address. This is
     * that sign-in, so copy the new address into users/{uid} if the profile still has the old one.
     */
    private void syncProfileEmail(FirebaseUser user) {
        String email = user.getEmail();
        DocumentReference profileDoc = firestore().collection("users").document(user.getUid());
        profileDoc.get().addOnSuccessListener(snapshot -> {
            if (email == null || !snapshot.exists() || email.equals(snapshot.getString("email"))) {
                return;
            }
            profileDoc.update("email", email, "updatedAt", System.currentTimeMillis() / 1000L)
                    .addOnFailureListener(e -> Log.w(TAG, "Could not update the profile email", e));
        });
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void sendPasswordReset(String email, AuthCallback<Void> callback) {
        firebaseAuth.sendPasswordResetEmail(UserValidator.normalizeEmail(email))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError(describeError(task.getException()));
                    }
                });
    }

    /**
     * Translates a FirebaseAuthException into a message safe to show the user.
     * Wrong-password and no-such-user both read as "Incorrect email or password" on purpose,
     * so a failed sign-in never reveals whether the email or the password was the problem.
     * Also used by AccountService.
     */
    static String describeError(Exception exception) {
        if (!(exception instanceof FirebaseAuthException)) {
            return "Something went wrong. Please check your connection and try again.";
        }
        String code = ((FirebaseAuthException) exception).getErrorCode();
        switch (code) {
            case "ERROR_INVALID_EMAIL":
                return "That email address looks invalid.";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "An account already exists for that email.";
            case "ERROR_WEAK_PASSWORD":
                return "Password must be at least 8 characters.";
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_USER_NOT_FOUND":
            case "ERROR_INVALID_CREDENTIAL":
                return "Incorrect email or password.";
            case "ERROR_REQUIRES_RECENT_LOGIN":
                return "Please sign in again and retry.";
            case "ERROR_USER_DISABLED":
                return "This account has been disabled.";
            case "ERROR_TOO_MANY_REQUESTS":
                return "Too many attempts. Please wait and try again.";
            case "ERROR_NETWORK_REQUEST_FAILED":
                return "Network error. Please check your connection.";
            default:
                return "Authentication failed. Please try again.";
        }
    }
}
