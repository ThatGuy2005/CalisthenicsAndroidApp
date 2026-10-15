package com.example.calisthenicsapp.CoreComponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;

public class WorkoutCategoryTest {
    @Test
    public void categoriesWithTheStoredLabels() {
        assertEquals(Arrays.asList(WorkoutCategory.FULL_BODY, WorkoutCategory.UPPER_BODY,
                        WorkoutCategory.LOWER_BODY, WorkoutCategory.CORE, WorkoutCategory.CARDIO),
                Arrays.asList(WorkoutCategory.values()));
        assertEquals("Full body", WorkoutCategory.FULL_BODY.getLabel());
        assertEquals("Upper body", WorkoutCategory.UPPER_BODY.getLabel());
        assertEquals("Lower body", WorkoutCategory.LOWER_BODY.getLabel());
        assertEquals("Core", WorkoutCategory.CORE.getLabel());
        assertEquals("Cardio", WorkoutCategory.CARDIO.getLabel());
    }

    @Test
    public void fromLabelIgnoresCaseAndSpaces() {
        assertEquals(WorkoutCategory.UPPER_BODY, WorkoutCategory.fromLabel("Upper body"));
        assertEquals(WorkoutCategory.FULL_BODY, WorkoutCategory.fromLabel(" FULL BODY "));
        assertNull(WorkoutCategory.fromLabel(null));
    }

    @Test
    public void unknownLabelsAreRejected() {
        for (String label : new String[]{"", "Legs", "Full_body"}) {
            try {
                WorkoutCategory.fromLabel(label);
                fail("Accepted " + label);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }
}
