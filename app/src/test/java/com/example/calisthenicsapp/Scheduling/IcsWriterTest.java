package com.example.calisthenicsapp.Scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class IcsWriterTest {
    // 2026-10-15 16:00:00 UTC
    private static final long START = 1792080000L;

    @Test
    public void escapesSpecialCharacters() {
        assertEquals("a\\\\b\\;c\\,d\\ne\\nf", IcsWriter.escape("a\\b;c,d\ne\r\nf"));
    }

    @Test
    public void writesTimesInUtc() {
        assertEquals("20261015T160000Z", IcsWriter.utc(START));
    }

    @Test
    public void foldsLongLinesAt75OctetsWithoutSplittingCharacters() {
        // "Huzodzkodas" with accents: two-byte characters in UTF-8.
        String line = "DESCRIPTION:" + "Húzódzkodás, ".repeat(20);
        String folded = IcsWriter.fold(line);
        for (String part : folded.split("\r\n")) {
            assertTrue(part.getBytes(StandardCharsets.UTF_8).length <= 75);
        }
        // Unfolding (removing CRLF + one space) gives back the original line.
        assertEquals(line, folded.replace("\r\n ", ""));
    }

    @Test
    public void shortLinesAreNotFolded() {
        assertEquals("SUMMARY:Workout", IcsWriter.fold("SUMMARY:Workout"));
    }

    @Test
    public void eventHasRequiredPropertiesAndCrlfLines() {
        String ics = IcsWriter.event("abc@calisthenicsapp", START, START + 1800,
                "Workout: Legs", "Squat, lunge", 30, START - 3600);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"));
        assertTrue(ics.endsWith("END:VEVENT\r\nEND:VCALENDAR\r\n"));
        assertTrue(ics.contains("\r\nUID:abc@calisthenicsapp\r\n"));
        assertTrue(ics.contains("\r\nDTSTAMP:20261015T150000Z\r\n"));
        assertTrue(ics.contains("\r\nDTSTART:20261015T160000Z\r\n"));
        assertTrue(ics.contains("\r\nDTEND:20261015T163000Z\r\n"));
        assertTrue(ics.contains("\r\nSUMMARY:Workout: Legs\r\n"));
        assertTrue(ics.contains("\r\nDESCRIPTION:Squat\\, lunge\r\n"));
        assertTrue(ics.contains("\r\nTRIGGER:-PT30M\r\n"));
        assertFalse(ics.replace("\r\n", "").contains("\n"));   // no bare line feeds
    }
}
