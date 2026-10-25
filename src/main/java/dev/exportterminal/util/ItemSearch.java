package dev.exportterminal.util;

import java.util.Locale;

public final class ItemSearch {

    private ItemSearch() {}

    public static boolean matches(String text, String query) {
        String haystack = text.toLowerCase(Locale.ROOT);
        String trimmed = query.trim()
            .toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) return true;
        for (String word : trimmed.split("\\s+")) {
            if (!haystack.contains(word)) return false;
        }
        return true;
    }
}
