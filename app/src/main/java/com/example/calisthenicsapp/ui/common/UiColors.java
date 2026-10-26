package com.example.calisthenicsapp.ui.common;

/**
 * Colors for the cards and controls the screens build in code, as ARGB ints. Most match the
 * performance_* colors in colors.xml; despite the names from the first design, the "orange" ones
 * are now violet and GRAPHITE is the near-white text color.
 */
public final class UiColors {
    /** The accent color (violet). */
    public static final int ORANGE = 0xFFC35CFF;
    /** The accent, a shade darker. */
    public static final int ORANGE_DARK = 0xFFB84DFF;
    /** Main text, near white. */
    public static final int GRAPHITE = 0xFFF2F4F1;
    /** Secondary text. */
    public static final int MUTED = 0xFFA0AAA5;
    /** The window background. */
    public static final int CANVAS = 0xFF090B0D;
    /** Card and input outlines. */
    public static final int BORDER = 0xFF30363B;
    /** A dark accent tint, behind selected chips and tags. */
    public static final int ORANGE_TINT = 0xFF321A42;
    /** A raised surface, a step lighter than SURFACE. */
    public static final int SURFACE_ALT = 0xFF24292E;
    /** Card backgrounds. */
    public static final int SURFACE = 0xFF191D21;
    /** Dark text on an accent background, e.g. a selected chip. */
    public static final int INK = 0xFF10140B;
    /** Completed, e.g. a completed workout. */
    public static final int SUCCESS = 0xFFD08BFF;
    /** Background behind SUCCESS text. */
    public static final int SUCCESS_TINT = 0xFF321943;
    /** Partial or missed. */
    public static final int WARNING = 0xFFFFC66E;
    /** Background behind WARNING text. */
    public static final int WARNING_TINT = 0xFF3A2D1B;
    /** The Beginner tag. */
    public static final int DIFFICULTY_BEGINNER = 0xFF4DDE91;
    /** The Intermediate tag. */
    public static final int DIFFICULTY_INTERMEDIATE = 0xFFFFD166;
    /** The Advanced tag. */
    public static final int DIFFICULTY_ADVANCED = 0xFFFF6B72;

    private UiColors() { }
}
