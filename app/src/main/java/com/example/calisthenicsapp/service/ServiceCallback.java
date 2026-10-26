package com.example.calisthenicsapp.service;

/**
 * Result of a data service call. Both methods are always called on the main thread,
 * so the UI can update views directly from them.
 */
public interface ServiceCallback<T> {
    /** The call worked; result is null for calls with no result. */
    void onSuccess(T result);
    /** The call failed; message can be shown to the user as it is. */
    void onError(String message);
}
