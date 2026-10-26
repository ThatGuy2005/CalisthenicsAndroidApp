package com.example.calisthenicsapp.model;

/**
 * The signed-in user's profile (FR-1.2), the users/{uid} document mirrored in the User table.
 * Height is in cm and weight in kg whatever units the user picked (see {@link Units}); 0 means not
 * given. Times are epoch seconds.
 */
public class User {
    /** Without a birth year. */
    public User(String uid, String username, String email, double height,
                double weight, String gender, long createdAt, long updatedAt) {
        this(uid, username, email, height, weight, gender, null, createdAt, updatedAt);
    }

    /** birthYear: optional, used with height, weight and gender for the BMR (FR-1.2). */
    public User(String uid, String username, String email, double height,
                double weight, String gender, Integer birthYear, long createdAt, long updatedAt) {
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

    /** The Firebase Auth uid, which is also the Firestore document id. */
    public String getUid() {
        return this.uid;
    }
    /**
     * Unique, ignoring case (see {@link
     * com.example.calisthenicsapp.validation.UserValidator#usernameKey}).
     */
    public String getUsername() {
        return this.username;
    }
    /** In cm; 0 if not given. */
    public Double getHeight() {
        return this.height;
    }

    /** In kg; 0 if not given. */
    public Double getWeight() {
        return this.weight;
    }

    /**
     * As the user wrote or picked it; null if not given. The BMR reads it with {@link
     * com.example.calisthenicsapp.session.Bmr#sexOf}.
     */
    public String getGender() {
        return this.gender;
    }

    /** E.g. 1995; null if not given. */
    public Integer getBirthYear() {
        return this.birthYear;
    }

    /** The sign-in email, lowercased. */
    public String getEmail() {
        return this.email;
    }

    /** When the account was created. */
    public long getCreatedAt() {
        return this.createdAt;
    }

    /** When the profile was last changed. */
    public long getUpdatedAt() {
        return this.updatedAt;
    }

    private String uid;
    private String username;
    private String email;
    private Double height;
    private Double weight;
    private String gender;
    private Integer birthYear;
    private long createdAt;
    private long updatedAt;

}
