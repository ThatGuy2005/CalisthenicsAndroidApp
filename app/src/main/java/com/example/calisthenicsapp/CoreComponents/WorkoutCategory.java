package com.example.calisthenicsapp.CoreComponents;

import java.util.Locale;

/**
 * The general category of a premade workout (FR-4.1): the part of the body it trains, or
 * cardio. values() gives the filter options in order. SQLite stores the label ("Full body").
 */
public enum WorkoutCategory {
    FULL_BODY("Full body"),
    UPPER_BODY("Upper body"),
    LOWER_BODY("Lower body"),
    CORE("Core"),
    CARDIO("Cardio");

    private final String label;

    WorkoutCategory(String label) {
        this.label = label;
    }

    /** For display, and the value stored in the database. */
    public String getLabel() {
        return label;
    }

    /**
     * The category with this label, ignoring case; null for null. Throws
     * IllegalArgumentException for anything else.
     */
    public static WorkoutCategory fromLabel(String label) {
        if (label == null) return null;
        for (WorkoutCategory c : values()) {
            if (c.label.equalsIgnoreCase(label.trim())) return c;
        }
        throw new IllegalArgumentException("Unknown workout category: " + label.toLowerCase(Locale.ROOT));
    }
}
