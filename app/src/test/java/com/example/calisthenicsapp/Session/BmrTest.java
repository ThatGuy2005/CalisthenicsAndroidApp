package com.example.calisthenicsapp.Session;

import static org.junit.Assert.assertEquals;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Session.Bmr.Sex;

import org.junit.Test;

public class BmrTest {
    private static final int YEAR = 2026;

    /** height 0 means not given, as in the profile. */
    private static User user(double heightCm, String gender, Integer birthYear) {
        return new User("u1", "alice", "a@b.c", heightCm, 80, gender, birthYear, 0L, 0L);
    }

    @Test
    public void mifflinStJeor() {
        // 10 x 80 + 6.25 x 180 - 5 x 30 = 1775
        assertEquals(1780, Bmr.kcalPerDay(80, 180, 30, Sex.MALE), 1e-9);
        assertEquals(1614, Bmr.kcalPerDay(80, 180, 30, Sex.FEMALE), 1e-9);
        assertEquals(1697, Bmr.kcalPerDay(80, 180, 30, Sex.UNSPECIFIED), 1e-9);   // the average
    }

    @Test
    public void genderWords() {
        assertEquals(Sex.MALE, Bmr.sexOf("Male"));
        assertEquals(Sex.MALE, Bmr.sexOf(" man "));
        assertEquals(Sex.MALE, Bmr.sexOf("M"));
        assertEquals(Sex.FEMALE, Bmr.sexOf("female"));
        assertEquals(Sex.FEMALE, Bmr.sexOf("WOMAN"));
        assertEquals(Sex.FEMALE, Bmr.sexOf("f"));
        assertEquals(Sex.UNSPECIFIED, Bmr.sexOf("Non-binary"));
        assertEquals(Sex.UNSPECIFIED, Bmr.sexOf(""));
        assertEquals(Sex.UNSPECIFIED, Bmr.sexOf(null));
    }

    @Test
    public void rateIsBmrPerHourWhenTheProfileIsComplete() {
        assertEquals(1780 / 24.0, Bmr.kcalPerMetHour(user(180, "Male", 1996), 80, YEAR), 1e-9);
        assertEquals(1697 / 24.0, Bmr.kcalPerMetHour(user(180, null, 1996), 80, YEAR), 1e-9);
    }

    @Test
    public void rateFallsBackToTheWeight() {
        assertEquals(80, Bmr.kcalPerMetHour(user(0, "Male", 1996), 80, YEAR), 1e-9);    // no height
        assertEquals(80, Bmr.kcalPerMetHour(user(180, "Male", null), 80, YEAR), 1e-9);  // no birth year
        assertEquals(80, Bmr.kcalPerMetHour(user(180, "Male", YEAR), 80, YEAR), 1e-9);  // age 0
        assertEquals(70, Bmr.kcalPerMetHour(null, 70, YEAR), 1e-9);                      // no profile
    }
}
