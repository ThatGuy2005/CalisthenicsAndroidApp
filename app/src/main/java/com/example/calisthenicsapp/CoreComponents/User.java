package com.example.calisthenicsapp.CoreComponents;

public class User {
    public User(String username, String email, String passwordHash, Double height,
                Double weight, String gender, String mode) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.height = height;
        this.weight = weight;
        this.gender = gender;
        this.mode = mode;
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

    public String getEmail() {
        return this.email;
    }

    public String getMode() {
        return this.mode;
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }
    private String username;
    private String email;
    private String passwordHash;
    private Double height;
    private Double weight;
    private String gender;
    private String mode;

}
