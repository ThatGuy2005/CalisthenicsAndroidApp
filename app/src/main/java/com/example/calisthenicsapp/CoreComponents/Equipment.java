package com.example.calisthenicsapp.CoreComponents;

import java.util.Locale;

/**
 * What an exercise needs besides the floor (FR-3.2), NONE first, so values() gives the filter
 * options in order. SQLite stores the label ("Pull-up bar"), and its CHECK constraint allows only
 * these.
 */
public enum Equipment {
    NONE("None"),
    WALL("Wall"),
    BENCH("Bench or chair"),
    PULL_UP_BAR("Pull-up bar"),
    PARALLEL_BARS("Parallel bars");

    private final String label;

    Equipment(String label) {
        this.label = label;
    }

    /** For display, and the value stored in the database. */
    public String getLabel() {
        return label;
    }

    /**
     * The equipment with this label, ignoring case; null for null. Throws
     * IllegalArgumentException for anything else.
     */
    public static Equipment fromLabel(String label) {
        if (label == null) return null;
        for (Equipment e : values()) {
            if (e.label.equalsIgnoreCase(label.trim())) return e;
        }
        throw new IllegalArgumentException("Unknown equipment: " + label.toLowerCase(Locale.ROOT));
    }
}
