package dev.exportterminal.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

public class ItemSearchTest {

    @Test
    public void findsChineseAndRegistryNamesAndRequiresEveryWord() {
        String bus = "铁锭 gregtech:gt.metaitem.01 oreIron 0 120 64 -30";
        assertTrue(ItemSearch.matches(bus, "铁锭"));
        assertTrue(ItemSearch.matches(bus, "  GREGTECH   铁锭  "));
        assertTrue(ItemSearch.matches(bus, "120 -30"));
        assertTrue(ItemSearch.matches(bus, "\t "));
        assertFalse(ItemSearch.matches(bus, "铁锭 铜"));
    }

    @Test
    public void searchDoesNotChangeWithSystemLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            assertTrue(ItemSearch.matches("IRON INGOT", "iron"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
