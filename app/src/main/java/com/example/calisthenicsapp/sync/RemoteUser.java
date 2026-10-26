package com.example.calisthenicsapp.sync;

import com.google.firebase.firestore.DocumentSnapshot;

/** Mirrors a users/{uid} Firestore document. */
public class RemoteUser {
    /** The Firebase Auth uid, also the document id. */
    public final String uid;
    /** As the user typed it. */
    public final String username;
    /** The sign-in email, lowercased. */
    public final String email;
    /** In cm; null if not given. */
    public final Double height;
    /** In kg; null if not given. */
    public final Double weight;
    /** Free text; null if not given. */
    public final String gender;
    /** Null if not given. */
    public final Integer birthYear;
    /** Epoch seconds; 0 if missing. */
    public final long createdAt;
    /** Epoch seconds; 0 if missing. */
    public final long updatedAt;

    /** Made by {@link #fromSnapshot}. */
    RemoteUser(String uid, String username, String email, Double height, Double weight,
                        String gender, Integer birthYear, long createdAt, long updatedAt) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.height = height;
        this.weight = weight;
        this.gender = gender;
        this.birthYear = birthYear;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Reads the profile document of the user uid. */
    public static RemoteUser fromSnapshot(String uid, DocumentSnapshot doc) {
        Long createdAt = doc.getLong("createdAt");
        Long updatedAt = doc.getLong("updatedAt");
        Long birthYear = doc.getLong("birthYear");
        return new RemoteUser(uid,
                doc.getString("username"),
                doc.getString("email"),
                doc.getDouble("height"),
                doc.getDouble("weight"),
                doc.getString("gender"),
                birthYear == null ? null : birthYear.intValue(),
                createdAt == null ? 0L : createdAt,
                updatedAt == null ? 0L : updatedAt);
    }
}
