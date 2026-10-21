package com.example.calisthenicsapp.Export;

/**
 * Builds one CSV file (RFC 4180): comma-separated, CRLF line ends, and fields quoted when they
 * contain a comma, a quote or a line break (quotes inside are doubled). Null is an empty field.
 *
 * Text that starts with = + - @ (or a tab or carriage return) gets a leading apostrophe, so a
 * spreadsheet shows it as text instead of running it as a formula (CSV injection). Numbers are
 * written as they are, so negative numbers stay numbers.
 */
public final class CsvWriter {
    private final StringBuilder out = new StringBuilder();

    public CsvWriter(String... header) {
        if (header.length > 0) row((Object[]) header);
    }

    public CsvWriter row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            out.append(field(values[i]));
        }
        out.append("\r\n");
        return this;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    static String field(Object value) {
        if (value == null) return "";
        String text = value.toString();
        if (value instanceof CharSequence && !text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        return quote ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}
