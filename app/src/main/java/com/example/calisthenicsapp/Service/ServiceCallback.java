package com.example.calisthenicsapp.Service;

/**
 * Result of a data service call. Both methods are always called on the main thread,
 * so the UI can update views directly from them.
 */
public interface ServiceCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}
