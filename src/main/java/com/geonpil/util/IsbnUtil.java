package com.geonpil.util;

public class IsbnUtil {
    public static String extractIsbn10(String rawIsbn) {
        if (rawIsbn == null || rawIsbn.isBlank()) return null;
        String[] parts = rawIsbn.trim().split(" +");
        for (String part : parts) {
            if (part.length() == 10) return part.trim();
        }
        return null;
    }

    public static String extractIsbn13(String rawIsbn) {
        if (rawIsbn == null || rawIsbn.isBlank()) return null;
        String[] parts = rawIsbn.trim().split(" +");
        for (String part : parts) {
            if (part.length() == 13) return part.trim();
        }
        return null;
    }
}
