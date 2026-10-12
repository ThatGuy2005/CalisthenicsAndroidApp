package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Database.SQLManager;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Verificate.UserValidator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps FirebaseAuth (email/password) and the users/{uid} Firestore profile document.
 * Every method is asynchronous and reports back through an {@link AuthCallback}; nothing here
 * blocks the calling thread. On a successful sign-up, the Firestore profile write is followed
 * by a local SQLite insert (on a background thread) so the per-user tables that foreign-key to
 * Users(Uid) have something to reference immediately. Stage 4's sync listeners take over
 * keeping that local row current after this point, including mirroring it on sign-in on a
 * device that has never seen this user before.
 */
public class AuthService {

    public interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final UserRepository userRepository;
    private final ExecutorService localDbExecutor = Executors.newSingleThreadExecutor();

    public AuthService(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
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

        firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().getUser() == null) {
                        callback.onError(describeError(task.getException()));
                        return;
                    }
                    saveProfile(task.getResult().getUser(), username, height, weight, gender, callback);
                });
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

        firestore.collection("users").document(firebaseUser.getUid())
                .set(profile)
                .addOnCompleteListener(docTask -> {
                    if (!docTask.isSuccessful()) {
                        callback.onError(describeError(docTask.getException()));
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

    public void signIn(String email, String password, AuthCallback<FirebaseUser> callback) {
        firebaseAuth.signInWithEmailAndPassword(UserValidator.normalizeEmail(email), password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        callback.onSuccess(task.getResult().getUser());
                    } else {
                        callback.onError(describeError(task.getException()));
                    }
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
     */
    private String describeError(Exception exception) {
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
