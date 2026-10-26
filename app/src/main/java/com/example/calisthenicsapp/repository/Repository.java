package com.example.calisthenicsapp.repository;

/**
 * A table the app also writes to: the per-user data, which SyncManager mirrors from Firestore.
 * Premade data is read-only and implements only {@link ReadRepository}.
 */
public interface Repository<T> extends ReadRepository<T> {
    /**
     * Adds a new row. False if a constraint rejected it, e.g. the key already exists or a row it
     * refers to does not.
     */
    boolean insert(T object);

    /**
     * Changes the row with the object's key. False if there is no such row or a constraint rejected
     * the change.
     */
    boolean update(T object);

    /**
     * Removes the row with the object's key, and the rows the schema deletes with it (ON DELETE
     * CASCADE). False if there was no such row or a foreign key stopped it.
     */
    boolean delete(T object);
}
