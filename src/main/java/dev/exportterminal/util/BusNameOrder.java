package dev.exportterminal.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import dev.exportterminal.BusEntry;

/** Stable sorting retains the server's distance/position order for equal names. */
public final class BusNameOrder implements Comparator<BusEntry> {

    private static final Pattern FORMATTING = Pattern.compile("\u00a7[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);
    private final Map<BusEntry, String> names = new IdentityHashMap<>();
    private final Collator collator;
    private final int dimension;

    public BusNameOrder(int dimension, Locale locale) {
        this.dimension = dimension;
        collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);
    }

    public void setNames(BusEntry entry, Iterable<String> configuredNames) {
        String first = "";
        for (String name : configuredNames) {
            String plain = FORMATTING.matcher(name)
                .replaceAll("")
                .trim();
            if (!plain.isEmpty() && (first.isEmpty() || collator.compare(plain, first) < 0)) first = plain;
        }
        names.put(entry, first);
    }

    @Override
    public int compare(BusEntry a, BusEntry b) {
        if ((a.dimension == dimension) != (b.dimension == dimension)) return a.dimension == dimension ? -1 : 1;
        int dims = Integer.compare(a.dimension, b.dimension);
        if (dims != 0) return dims;
        String nameA = names.getOrDefault(a, "");
        String nameB = names.getOrDefault(b, "");
        if (nameA.isEmpty() != nameB.isEmpty()) return nameA.isEmpty() ? 1 : -1;
        return collator.compare(nameA, nameB);
    }
}
