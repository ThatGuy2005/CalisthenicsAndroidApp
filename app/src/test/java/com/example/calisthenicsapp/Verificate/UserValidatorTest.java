package com.example.calisthenicsapp.Verificate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class UserValidatorTest {
    @Test
    public void usernameIs3To20LettersOrDigits() {
        assertTrue(UserValidator.isValidUsername("abc"));
        assertTrue(UserValidator.isValidUsername("Alice1"));
        assertTrue(UserValidator.isValidUsername("123"));
        assertTrue(UserValidator.isValidUsername("abcdefghij0123456789"));     // 20
        assertFalse(UserValidator.isValidUsername(null));
        assertFalse(UserValidator.isValidUsername("ab"));
        assertFalse(UserValidator.isValidUsername("abcdefghij0123456789x"));   // 21
        // FR-1.1 says alphanumeric, so no punctuation, spaces or letters outside A-Z.
        assertFalse(UserValidator.isValidUsername("alice_1"));
        assertFalse(UserValidator.isValidUsername("al.ice"));
        assertFalse(UserValidator.isValidUsername("al-ice"));
        assertFalse(UserValidator.isValidUsername("al ice"));
        assertFalse(UserValidator.isValidUsername("álice"));
    }

    @Test
    public void birthYearFrom1900ToThisYear() {
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        assertTrue(UserValidator.isValidBirthYear(null));   // optional
        assertTrue(UserValidator.isValidBirthYear(1900));
        assertTrue(UserValidator.isValidBirthYear(1995));
        assertTrue(UserValidator.isValidBirthYear(thisYear));
        assertFalse(UserValidator.isValidBirthYear(1899));
        assertFalse(UserValidator.isValidBirthYear(thisYear + 1));
    }
}
