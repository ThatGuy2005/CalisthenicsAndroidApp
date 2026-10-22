package com.example.calisthenicsapp.CoreComponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.calisthenicsapp.Verificate.UserValidator;

import org.junit.Test;

import java.util.Locale;

public class UnitsTest {
    private static final Locale US = Locale.US;

    @Test
    public void convertsWeightBothWays() {
        assertEquals(154.3, Units.kgToLb(70), 0);
        assertEquals(70.0, Units.lbToKg(154.3), 0);
        assertEquals(45.4, Units.lbToKg(100), 0);
    }

    @Test
    public void weightShownInLbAndSavedUnchangedKeepsItsKg() {
        for (int tenths = 200; tenths <= 5000; tenths++) {
            double kg = tenths / 10.0;
            assertEquals(kg, Units.lbToKg(Units.kgToLb(kg)), 0);
        }
    }

    @Test
    public void heightInFeetAndInches() {
        Units.FeetInches h = Units.cmToFeetInches(175);
        assertEquals(5, h.getFeet());
        assertEquals(9, h.getInches());
        assertEquals("5 ft 9 in", h.toString());
        assertEquals(175.3, Units.feetInchesToCm(5, 9), 0);
        assertEquals(182.9, Units.feetInchesToCm(5, 12), 0);   // 12 inches carry over
    }

    @Test
    public void justUnderAWholeFootRoundsUpToIt() {
        Units.FeetInches h = Units.cmToFeetInches(182.6);   // 71.9 in
        assertEquals(6, h.getFeet());
        assertEquals(0, h.getInches());
    }

    @Test
    public void feetAndInchesTypedInSurviveTheRoundTrip() {
        for (int inches = 20; inches <= 118; inches++) {
            Units.FeetInches back = Units.cmToFeetInches(Units.feetInchesToCm(0, inches));
            assertEquals(inches, back.getFeet() * Units.INCHES_PER_FOOT + back.getInches());
        }
    }

    @Test
    public void typedValuesBecomeKgAndStoredOnesComeBack() {
        assertEquals(70.0, Units.weightToKg(154.3, UnitSystem.IMPERIAL), 0);
        assertEquals(70.0, Units.weightToKg(70.0, UnitSystem.METRIC), 0);
        assertNull(Units.weightToKg(null, UnitSystem.IMPERIAL));
        assertEquals(154.3, Units.weightFromKg(70.0, UnitSystem.IMPERIAL), 0);
        assertNull(Units.weightFromKg(0.0, UnitSystem.METRIC));   // 0 means not given
    }

    @Test
    public void metricErrorsAreTheBackEndsOwnMessages() {
        assertEquals("Height must be between 50 and 300 cm.", Units.heightError(UnitSystem.METRIC, US));
        assertEquals("Weight must be between 20 and 500 kg.", Units.weightError(UnitSystem.METRIC, US));
    }

    @Test
    public void imperialLimitsShownAreAccepted() {
        assertEquals("Height must be between 1 ft 8 in and 9 ft 10 in.",
                Units.heightError(UnitSystem.IMPERIAL, US));
        assertEquals("Weight must be between 44.1 and 1102.3 lb.",
                Units.weightError(UnitSystem.IMPERIAL, US));
        assertTrue(UserValidator.isValidHeight(Units.feetInchesToCm(1, 8)));
        assertTrue(UserValidator.isValidHeight(Units.feetInchesToCm(9, 10)));
        assertTrue(UserValidator.isValidWeight(Units.lbToKg(44.1)));
        assertTrue(UserValidator.isValidWeight(Units.lbToKg(1102.3)));
    }

    @Test
    public void savedSystemComesBackAndUnknownMeansMetric() {
        assertEquals(UnitSystem.IMPERIAL, UnitSystem.fromName(UnitSystem.IMPERIAL.name()));
        assertEquals(UnitSystem.METRIC, UnitSystem.fromName(null));
        assertEquals(UnitSystem.METRIC, UnitSystem.fromName("STONE"));
        assertEquals("Imperial (lb, ft)", UnitSystem.IMPERIAL.getLabel());
    }
}
