package com.example.calisthenicsapp.Sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfileGoneTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final Long OLD_ACCOUNT = NOW - 24 * 60 * 60 * 1000L;

    @Test
    public void goneWhenTheServerHasNoProfileForAnOldAccount() {
        assertTrue(SyncManager.isProfileGone(false, false, OLD_ACCOUNT, NOW));
    }

    @Test
    public void theCacheIsNotEnough() {
        assertFalse(SyncManager.isProfileGone(true, false, OLD_ACCOUNT, NOW));
    }

    /** This device's own Delete account, or another device's: not this check's business. */
    @Test
    public void notAfterTheServerShowedTheProfile() {
        assertFalse(SyncManager.isProfileGone(false, true, OLD_ACCOUNT, NOW));
    }

    /** Sign-up saves the profile just after creating the account. */
    @Test
    public void notWhileANewAccountSavesItsProfile() {
        assertFalse(SyncManager.isProfileGone(false, false, NOW - 5_000L, NOW));
        assertFalse(SyncManager.isProfileGone(false, false, NOW - SyncManager.NEW_ACCOUNT_MILLIS, NOW));
        assertTrue(SyncManager.isProfileGone(false, false, NOW - SyncManager.NEW_ACCOUNT_MILLIS - 1, NOW));
    }

    @Test
    public void notWhenTheAccountAgeIsUnknown() {
        assertFalse(SyncManager.isProfileGone(false, false, null, NOW));
    }
}
