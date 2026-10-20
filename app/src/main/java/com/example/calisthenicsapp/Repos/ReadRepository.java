package com.example.calisthenicsapp.Repos;

import java.util.List;

/**
 * Read access to a table. The premade exercises and workouts implement only this: seed.sql
 * loads them (DR-7) and the app never changes them (FR-4.3).
 */
public interface ReadRepository<T> {
    List<T> getAll();
}
