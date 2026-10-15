package com.example.calisthenicsapp.CoreComponents;

public class User {
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

    public String getUid() {
        return this.uid;
    }
    public String getUsername() {
        return this.username;
    }
    public Double getHeight() {
        return this.height;
    }

    public Double getWeight() {
        return this.weight;
    }

    public String getGender() {
        return this.gender;
    }

    /** E.g. 1995; null if not given. */
    public Integer getBirthYear() {
        return this.birthYear;
    }

    public String getEmail() {
        return this.email;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

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
