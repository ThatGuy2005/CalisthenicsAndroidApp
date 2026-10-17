package com.example.calisthenicsapp.CoreComponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;

public class EquipmentTest {
    @Test
    public void noneFirstWithTheStoredLabels() {
        assertEquals(Arrays.asList(Equipment.NONE, Equipment.WALL, Equipment.BENCH,
                        Equipment.PULL_UP_BAR, Equipment.PARALLEL_BARS),
                Arrays.asList(Equipment.values()));
        assertEquals("None", Equipment.NONE.getLabel());
        assertEquals("Wall", Equipment.WALL.getLabel());
        assertEquals("Bench or chair", Equipment.BENCH.getLabel());
        assertEquals("Pull-up bar", Equipment.PULL_UP_BAR.getLabel());
        assertEquals("Parallel bars", Equipment.PARALLEL_BARS.getLabel());
    }

    @Test
    public void fromLabelIgnoresCaseAndSpaces() {
        assertEquals(Equipment.PULL_UP_BAR, Equipment.fromLabel("Pull-up bar"));
        assertEquals(Equipment.BENCH, Equipment.fromLabel(" bench OR chair "));
        assertNull(Equipment.fromLabel(null));
    }

    @Test
    public void unknownLabelsAreRejected() {
        for (String label : new String[]{"", "Rings", "Pull-up"}) {
            try {
                Equipment.fromLabel(label);
                fail("Accepted " + label);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }
}
