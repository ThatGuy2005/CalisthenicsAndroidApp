package com.example.calisthenicsapp.model;

/**
 * The units height and weight are shown and typed in (FR-1.7). Only the screens use it: the
 * back end always stores kg and cm, and {@link Units} converts. The choice is a setting of this
 * device; save it with name() and read it back with {@link #fromName}.
 */
public enum UnitSystem {
    /** Kilograms and centimetres; the default. */
    METRIC("Metric (kg, cm)"),
    /** Pounds, and feet and inches. */
    IMPERIAL("Imperial (lb, ft)");

    private final String label;

    UnitSystem(String label) {
        this.label = label;
    }

    /** For the toggle. */
    public String getLabel() {
        return label;
    }

    /** The system saved with name(); METRIC when nothing (or something unknown) was saved. */
    public static UnitSystem fromName(String name) {
        for (UnitSystem system : values()) {
            if (system.name().equals(name)) return system;
        }
        return METRIC;
    }
}
