package com.example.calisthenicsapp.Verificate;

import java.util.regex.Pattern;

public final class UserValidator {
    private UserValidator() {}

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 20;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 64;
    private static final int EMAIL_MAX = 254;

    // Letters, digits, '_', '.', '-'; must start and end with a letter or digit.
    private static final Pattern USERNAME = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?$");

    // Pragmatic check: local@domain.tld, no spaces, at least one dot in the domain.
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        int len = username.length();
        if (len < USERNAME_MIN || len > USERNAME_MAX) return false;
        if (username.contains("..")) return false;      // no consecutive dots
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

    /** Call this before validating and storing, so "Bob@X.com" and "bob@x.com" are the same account. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}