package com.example.calisthenicsapp.CoreComponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;

public class DifficultyTest {
    @Test
    public void easiestFirstWithTheStoredLabels() {
        assertEquals(Arrays.asList(Difficulty.BEGINNER, Difficulty.INTERMEDIATE, Difficulty.ADVANCED),
                Arrays.asList(Difficulty.values()));
        assertEquals("Beginner", Difficulty.BEGINNER.getLabel());
        assertEquals("Intermediate", Difficulty.INTERMEDIATE.getLabel());
        assertEquals("Advanced", Difficulty.ADVANCED.getLabel());
    }

    @Test
    public void fromLabelIgnoresCaseAndSpaces() {
        assertEquals(Difficulty.ADVANCED, Difficulty.fromLabel("Advanced"));
        assertEquals(Difficulty.BEGINNER, Difficulty.fromLabel(" beginner "));
        assertNull(Difficulty.fromLabel(null));
    }

    @Test
    public void unknownLabelsAreRejected() {
        for (String label : new String[]{"", "Expert", "BEGINNER!"}) {
            try {
                Difficulty.fromLabel(label);
                fail("Accepted " + label);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }
}
