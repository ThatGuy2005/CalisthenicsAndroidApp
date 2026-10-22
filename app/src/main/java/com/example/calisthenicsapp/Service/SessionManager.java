package com.example.calisthenicsapp.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Thin wrapper around FirebaseAuth's current-user state. Stage 4 registers its sync-start /
 * sync-stop logic through {@link #addAuthStateListener}, so it reacts to sign-in and sign-out
 * wherever they happen (this class, or a token refresh) without AuthService depending on sync.
 */
public class SessionManager {

    public interface SignedInListener {
        void onSignedIn(FirebaseUser user);
        void onSignedOut();
    }

    private final FirebaseAuth firebaseAuth;

    public SessionManager() {
        this(FirebaseAuth.getInstance());
    }

    /** For a FirebaseAuth other than the app's default one, e.g. one pointed at the emulator. */
    public SessionManager(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public void addAuthStateListener(SignedInListener listener) {
        firebaseAuth.addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                listener.onSignedIn(user);
            } else {
                listener.onSignedOut();
            }
        });
    }
}
