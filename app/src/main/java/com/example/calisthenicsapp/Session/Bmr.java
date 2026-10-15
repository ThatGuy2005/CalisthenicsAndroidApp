package com.example.calisthenicsapp.Session;

import com.example.calisthenicsapp.CoreComponents.User;

import java.util.Locale;

/**
 * The calorie rate of a workout session (FR-1.2, FR-5.6). One MET is the body's resting energy
 * use, so an exercise burns MET x (resting kcal per hour) x hours. The textbook shortcut takes
 * the resting rate as 1 kcal per kg per hour; with height, birth year and gender we use the
 * user's own basal metabolic rate (Mifflin-St Jeor) instead: BMR / 24 kcal per hour.
 *
 * Pure logic, no Android, so plain JVM tests reach it.
 */
public final class Bmr {
    /** The gender words recognised; anything else (or nothing) uses the average of the two. */
    public enum Sex { MALE, FEMALE, UNSPECIFIED }

    private Bmr() {
    }

    /**
     * kcal burned per MET per hour for this user: BMR / 24 when height, birth year and weight
     * are known, otherwise the weight in kg (the 1 kcal/kg/h shortcut). weightKg is the weight
     * to use, already defaulted if the profile has none.
     */
    public static double kcalPerMetHour(User user, double weightKg, int currentYear) {
        Double height = user == null ? null : user.getHeight();
        Integer birthYear = user == null ? null : user.getBirthYear();
        if (height == null || height <= 0 || birthYear == null) return weightKg;
        int age = currentYear - birthYear;
        if (age <= 0) return weightKg;
        return kcalPerDay(weightKg, height, age, sexOf(user.getGender())) / 24.0;
    }

    /**
     * Mifflin-St Jeor: 10 x kg + 6.25 x cm - 5 x years, plus 5 for men and minus 161 for women;
     * the average of the two (minus 78) when the sex is not given.
     */
    public static double kcalPerDay(double weightKg, double heightCm, int age, Sex sex) {
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        switch (sex) {
            case MALE: return base + 5;
            case FEMALE: return base - 161;
            default: return base - 78;
        }
    }

    /**
     * Reads the free-text gender of the profile: "male", "man" or "m" and "female", "woman" or
     * "f", ignoring case and spaces. Anything else is UNSPECIFIED.
     */
    public static Sex sexOf(String gender) {
        if (gender == null) return Sex.UNSPECIFIED;
        switch (gender.trim().toLowerCase(Locale.ROOT)) {
            case "male": case "man": case "m": return Sex.MALE;
            case "female": case "woman": case "f": return Sex.FEMALE;
            default: return Sex.UNSPECIFIED;
        }
    }
}
