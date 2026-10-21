package com.example.calisthenicsapp.Verificate;

import java.util.Calendar;
import java.util.regex.Pattern;

public final class UserValidator {
    private UserValidator() {}

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 20;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 64;
    private static final int EMAIL_MAX = 254;
    // firestore.rules checks the same limits; change both together.
    public static final double HEIGHT_MIN = 50, HEIGHT_MAX = 300;   // cm
    public static final double WEIGHT_MIN = 20, WEIGHT_MAX = 500;   // kg
    private static final int GENDER_MAX = 30;
    public static final int BIRTH_YEAR_MIN = 1900;   // up to the current year

    // Alphanumeric only (FR-1.1): ASCII letters and digits. firestore.rules checks the same.
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9]+$");

    // Pragmatic check: local@domain.tld, no spaces, at least one dot in the domain.
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        int len = username.length();
        if (len < USERNAME_MIN || len > USERNAME_MAX) return false;
        return USERNAME.matcher(username).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        int len = password.length();
        if (len < PASSWORD_MIN || len > PASSWORD_MAX) return false;

        boolean hasLetter = false, hasDigit = false;
        for (int i = 0; i < len; i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        if (email.length() > EMAIL_MAX) return false;
        return EMAIL.matcher(email).matches();
    }

    /** Height in cm; null means not given, which is fine. */
    public static boolean isValidHeight(Double height) {
        return height == null || (height >= HEIGHT_MIN && height <= HEIGHT_MAX);
    }

    /** Weight in kg; null means not given, which is fine. */
    public static boolean isValidWeight(Double weight) {
        return weight == null || (weight >= WEIGHT_MIN && weight <= WEIGHT_MAX);
    }

    /** Null means not given, which is fine. */
    public static boolean isValidGender(String gender) {
        return gender == null || gender.length() <= GENDER_MAX;
    }

    /** From 1900 to the current year; null means not given, which is fine. */
    public static boolean isValidBirthYear(Integer year) {
        return year == null
                || (year >= BIRTH_YEAR_MIN && year <= Calendar.getInstance().get(Calendar.YEAR));
    }

    /**
     * The id of the username's usernames/{key} claim document in Firestore. Lowercased, so
     * "Bob" and "bob" claim the same document and count as the same username.
     */
    public static String usernameKey(String username) {
        return username.toLowerCase(java.util.Locale.ROOT);
    }

    /** Call this before validating and storing, so "Bob@X.com" and "bob@x.com" are the same account. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}