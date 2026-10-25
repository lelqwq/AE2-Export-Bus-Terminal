package dev.exportterminal.util;

import java.util.Locale;
import java.util.regex.Pattern;

import me.towdium.pinin.PinIn;

public final class ItemSearch {

    private static final PinIn PINYIN = new PinIn();
    private static final Pattern FORMATTING = Pattern.compile("\u00a7[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);

    private ItemSearch() {}

    public static boolean matches(String text, String query) {
        String haystack = FORMATTING.matcher(text)
            .replaceAll("")
            .toLowerCase(Locale.ROOT);
        String trimmed = query.trim()
            .toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) return true;
        for (String word : trimmed.split("\\s+")) {
            if (!haystack.contains(word) && !PINYIN.contains(haystack, word)) return false;
        }
        return true;
    }
}
