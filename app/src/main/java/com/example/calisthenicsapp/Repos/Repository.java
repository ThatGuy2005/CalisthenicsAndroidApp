package com.example.calisthenicsapp.Repos;

/**
 * A table the app also writes to: the per-user data, which SyncManager mirrors from Firestore.
 * Premade data is read-only and implements only {@link ReadRepository}.
 */
public interface Repository<T> extends ReadRepository<T> {
    boolean insert(T object);

    boolean update(T object);

    boolean delete(T object);
}
