package com.example.calisthenicsapp.Export;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CsvWriterTest {
    @Test
    public void headerThenRowsWithCrlfLineEnds() {
        assertEquals("name,sets,kcal\r\nPush-up,3,2.5\r\n",
                new CsvWriter("name", "sets", "kcal").row("Push-up", 3, 2.5).toString());
        assertEquals("", new CsvWriter().toString());   // no header, no empty first line
    }

    @Test
    public void nullIsAnEmptyField() {
        assertEquals("x,,y", CsvWriter.field("x") + "," + CsvWriter.field(null) + "," + CsvWriter.field("y"));
    }

    @Test
    public void commasQuotesAndLineBreaksAreQuoted() {
        assertEquals("\"Legs, hard\"", CsvWriter.field("Legs, hard"));
        assertEquals("\"The \"\"best\"\" one\"", CsvWriter.field("The \"best\" one"));
        assertEquals("\"two\nlines\"", CsvWriter.field("two\nlines"));
        assertEquals("\"cr\rhere\"", CsvWriter.field("cr\rhere"));
    }

    @Test
    public void textThatLooksLikeAFormulaIsDefused() {
        assertEquals("'=HYPERLINK(1)", CsvWriter.field("=HYPERLINK(1)"));
        assertEquals("'+1", CsvWriter.field("+1"));
        assertEquals("'-cmd", CsvWriter.field("-cmd"));
        assertEquals("'@SUM(A1)", CsvWriter.field("@SUM(A1)"));
        assertEquals("\"'=1,2\"", CsvWriter.field("=1,2"));   // defused, then quoted for the comma
        assertEquals("-5", CsvWriter.field(-5));               // numbers stay numbers
        assertEquals("-2.5", CsvWriter.field(-2.5));
    }
}
