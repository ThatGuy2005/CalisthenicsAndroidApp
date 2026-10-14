package com.example.calisthenicsapp.CoreComponents;

import java.util.Locale;

/**
 * How hard an exercise is (FR-3.2), easiest first, so values() gives the filter options in order.
 * SQLite stores the label ("Beginner"), and its CHECK constraint allows only these three.
 */
public enum Difficulty {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    /** For display, and the value stored in the database. */
    public String getLabel() {
        return label;
    }

    /**
     * The difficulty with this label, ignoring case; null for null. Throws
     * IllegalArgumentException for anything else.
     */
    public static Difficulty fromLabel(String label) {
        if (label == null) return null;
        for (Difficulty d : values()) {
            if (d.label.equalsIgnoreCase(label.trim())) return d;
        }
        throw new IllegalArgumentException("Unknown difficulty: " + label.toLowerCase(Locale.ROOT));
    }
}
