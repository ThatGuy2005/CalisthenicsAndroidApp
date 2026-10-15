package com.example.calisthenicsapp.Sync;

import com.google.firebase.firestore.DocumentSnapshot;

/** Mirrors a users/{uid} Firestore document. */
public class RemoteUser {
    public final String uid;
    public final String username;
    public final String email;
    public final Double height;
    public final Double weight;
    public final String gender;
    public final Integer birthYear;
    public final long createdAt;
    public final long updatedAt;

    private RemoteUser(String uid, String username, String email, Double height, Double weight,
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
