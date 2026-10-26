package com.example.calisthenicsapp.model;

/**
 * A workout the user put together from the exercise library (FR-4.3). Unlike a premade workout it
 * belongs to one user, is identified by its Firestore document id rather than its name, and has no
 * level or category. Its exercises are {@link CustomExerciseOrderEntry} rows.
 */
public class CustomWorkout extends Workout{
    /** Without a description or an estimated length. */
    public CustomWorkout(String id, String name, String uid) {
        this(id, name, uid, null);
    }
    /** estimatedSeconds: see Workout.getEstimatedSeconds(); filled in by CustomWorkoutService. */
    public CustomWorkout(String id, String name, String uid, Integer estimatedSeconds) {
        this(id, name, uid, null, estimatedSeconds);
    }
    /** description: the user's own, optional (null). */
    public CustomWorkout(String id, String name, String uid, String description,
                         Integer estimatedSeconds) {
        super(name, null, null, description, estimatedSeconds);
        this.id = id;
        this.uid = uid;
    }
    /** The Firebase Auth uid of the user who owns it. */
    public String getUid() {
        return this.uid;
    }
    /** The Firestore document id (users/{uid}/customWorkouts/{id}), also the SQLite key. */
    public String getId() {
        return this.id;
    }
    private String uid;
    private String id;
}
