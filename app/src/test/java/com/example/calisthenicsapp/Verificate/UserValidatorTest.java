package com.example.calisthenicsapp.Verificate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class UserValidatorTest {
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
