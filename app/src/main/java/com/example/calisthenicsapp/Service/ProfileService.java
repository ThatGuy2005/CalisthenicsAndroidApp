package com.example.calisthenicsapp.Service;

import android.content.Context;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Verificate.UserValidator;

import java.util.HashMap;
import java.util.Map;

/**
 * The signed-in user's profile (users/{uid}). Height is in cm and weight in kg.
 * The email belongs to Firebase Authentication and is not changed here.
 */
public class ProfileService extends UserDataService {
    private static final double HEIGHT_MIN = 50, HEIGHT_MAX = 300;
    private static final double WEIGHT_MIN = 20, WEIGHT_MAX = 500;

    public ProfileService(Context context) {
        super(context);
    }

    /** Returns null if the profile has not been synced to this device yet. */
    public void getMyProfile(ServiceCallback<User> callback) {
        readForUser(callback, uid -> new UserRepository(db()).getOne(uid));
    }

    /** Overwrites all four fields; pass the current value for anything that did not change. */
    public void updateProfile(String username, Double height, Double weight, String gender,
                              ServiceCallback<Void> callback) {
        String uid = sessionManager.getCurrentUid();
        if (uid == null) {
            fail(callback, NOT_SIGNED_IN);
            return;
        }
        if (!UserValidator.isValidUsername(username)) {
            fail(callback, "Username must be 3-20 letters, digits, '.', '_' or '-'.");
            return;
        }
        if (height != null && (height < HEIGHT_MIN || height > HEIGHT_MAX)) {
            fail(callback, "Height must be between 50 and 300 cm.");
            return;
        }
        if (weight != null && (weight < WEIGHT_MIN || weight > WEIGHT_MAX)) {
            fail(callback, "Weight must be between 20 and 500 kg.");
            return;
        }

        Map<String, Object> changes = new HashMap<>();
        changes.put("username", username);
        changes.put("height", height);
        changes.put("weight", weight);
        changes.put("gender", gender);
        changes.put("updatedAt", now());
        watchWrite(userDoc(uid).update(changes), "Updating profile of " + uid);
        succeed(callback, null);
    }
}
