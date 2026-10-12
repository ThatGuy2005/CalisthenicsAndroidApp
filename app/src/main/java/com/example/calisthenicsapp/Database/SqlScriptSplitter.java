package com.example.calisthenicsapp.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits an SQL script into single statements, because SQLiteDatabase.execSQL()
 * runs only one statement per call.
 *
 * Handles: -- line comments, block comments, quoted strings (a ';' inside quotes does not end
 * a statement), and CREATE TRIGGER ... BEGIN ... END; (the ';' inside the trigger body does not
 * end the statement).
 *
 * Known limit: inside a trigger body, a statement ending in "CASE ... END;" would be mistaken
 * for the end of the trigger. Your schema has no such trigger.
 *
 * Pure Java (no Android classes), so it can be unit-tested on a normal JVM.
 */
public final class SqlScriptSplitter {
    private SqlScriptSplitter() {}

    private static final Pattern CREATE_TRIGGER = Pattern.compile(
            "^CREATE\\s+(TEMP\\s+|TEMPORARY\\s+)?TRIGGER\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ENDS_WITH_END = Pattern.compile(
            ".*\\bEND$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        if (script == null) return statements;
        if (script.startsWith("﻿")) script = script.substring(1);   // UTF-8 byte order mark

        StringBuilder current = new StringBuilder();
        boolean inString = false;
        int n = script.length();

        for (int i = 0; i < n; i++) {
            char ch = script.charAt(i);

            if (inString) {
                current.append(ch);
                if (ch == '\'') inString = false;   // an escaped '' leaves and re-enters the string
                continue;
            }
            if (ch == '\'') {
                inString = true;
                current.append(ch);
                continue;
            }
            if (ch == '-' && i + 1 < n && script.charAt(i + 1) == '-') {     // -- line comment
                while (i < n && script.charAt(i) != '\n') i++;
                current.append('\n');
                continue;
            }
            if (ch == '/' && i + 1 < n && script.charAt(i + 1) == '*') {     // /* block comment */
                int end = script.indexOf("*/", i + 2);
                i = (end < 0) ? n : end + 1;
                current.append(' ');
                continue;
            }
            if (ch == ';' && !insideTriggerBody(current)) {
                addIfNotEmpty(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        addIfNotEmpty(statements, current);   // last statement without a closing ';'
        return statements;
    }

    /** True while we are inside CREATE TRIGGER ... and have not yet reached its closing END. */
    private static boolean insideTriggerBody(StringBuilder current) {
        String text = current.toString().trim();
        return CREATE_TRIGGER.matcher(text).matches()
                && !ENDS_WITH_END.matcher(text).matches();
    }

    private static void addIfNotEmpty(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) statements.add(statement);
    }
}
