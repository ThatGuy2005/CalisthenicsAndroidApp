package com.example.calisthenicsapp.Scheduling;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Writes a one-event iCalendar file (.ics, RFC 5545) that mail and calendar apps can import
 * (FR-6.3). Times are written in UTC, so the file needs no time zone definitions and every
 * calendar shows the event at the right local time.
 */
public final class IcsWriter {
    private static final String CRLF = "\r\n";
    /** RFC 5545 3.1: lines longer than this many octets must be folded. */
    private static final int MAX_LINE_OCTETS = 75;

    private IcsWriter() {
    }

    /**
     * @param uid      stays the same for the same event, so importing it again updates the
     *                 event instead of adding a second one
     * @param nowSeconds when the file is made (DTSTAMP)
     */
    public static String event(String uid, long startSeconds, long endSeconds, String summary,
                               String description, int reminderMinutes, long nowSeconds) {
        StringBuilder ics = new StringBuilder();
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//CalisthenicsApp//Scheduled workout//EN");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "BEGIN:VEVENT");
        line(ics, "UID:" + escape(uid));
        line(ics, "DTSTAMP:" + utc(nowSeconds));
        line(ics, "DTSTART:" + utc(startSeconds));
        line(ics, "DTEND:" + utc(endSeconds));
        line(ics, "SUMMARY:" + escape(summary));
        line(ics, "DESCRIPTION:" + escape(description));
        line(ics, "BEGIN:VALARM");
        line(ics, "ACTION:DISPLAY");
        line(ics, "DESCRIPTION:" + escape(summary));
        line(ics, "TRIGGER:-PT" + reminderMinutes + "M");
        line(ics, "END:VALARM");
        line(ics, "END:VEVENT");
        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    /** Text values: backslash, semicolon and comma are escaped, line breaks become \n. */
    static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    /**
     * Splits a content line into lines of at most 75 UTF-8 octets; each continuation line
     * starts with a space. Never splits a character.
     */
    static String fold(String contentLine) {
        StringBuilder out = new StringBuilder();
        int octets = 0;
        for (int i = 0; i < contentLine.length(); ) {
            int codePoint = contentLine.codePointAt(i);
            String ch = new String(Character.toChars(codePoint));
            int size = ch.getBytes(StandardCharsets.UTF_8).length;
            if (octets + size > MAX_LINE_OCTETS) {
                out.append(CRLF).append(' ');
                octets = 1;
            }
            out.append(ch);
            octets += size;
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }

    static String utc(long epochSeconds) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(epochSeconds * 1000L));
    }

    private static void line(StringBuilder ics, String contentLine) {
        ics.append(fold(contentLine)).append(CRLF);
    }
}
