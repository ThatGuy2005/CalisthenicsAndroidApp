package com.example.calisthenicsapp.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Thin wrapper around FirebaseAuth's current-user state. SyncManager registers its sync-start /
 * sync-stop logic through {@link #addAuthStateListener}, so it reacts to sign-in and sign-out
 * wherever they happen (AuthService, this class, or Firebase itself) without AuthService
 * depending on sync.
 */
public class SessionManager {

    /** Gets told about sign-ins and sign-outs. */
    public interface SignedInListener {
        /** A user is signed in: on registering if one already is, and after every sign-in. */
        void onSignedIn(FirebaseUser user);
        /** No user is signed in (any more). */
        void onSignedOut();
    }

    private final FirebaseAuth firebaseAuth;

    /** Uses the app's FirebaseAuth. */
    public SessionManager() {
        this(FirebaseAuth.getInstance());
    }

    /** For a FirebaseAuth other than the app's default one, e.g. one pointed at the emulator. */
    public SessionManager(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    /** The signed-in user, or null. */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /** The signed-in user's uid, or null. */
    public String getCurrentUid() {
        FirebaseUser user = getCurrentUser();
        return user == null ? null : user.getUid();
    }

    /** Signs out; the listeners hear onSignedOut. */
    public void signOut() {
        firebaseAuth.signOut();
    }

    /**
     * Calls the listener right away with the current state, and again on every change. Calls run on
     * the main thread.
     */
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
