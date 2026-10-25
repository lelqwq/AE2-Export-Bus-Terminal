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
    public void supportsFullPinyinInitialsAndMixedSpelling() {
        String bus = "铁锭 不锈钢锭 精炼铝导线 gregtech:gt.metaitem.01";
        assertTrue(ItemSearch.matches(bus, "tieding"));
        assertTrue(ItemSearch.matches(bus, "td"));
        assertTrue(ItemSearch.matches(bus, "BXGD"));
        assertTrue(ItemSearch.matches(bus, "jinglianlvdaoxian"));
        assertTrue(ItemSearch.matches(bus, "jll dx"));
        assertTrue(ItemSearch.matches(bus, "铁ding"));
        assertTrue(ItemSearch.matches(bus, "gregtech td"));
        assertFalse(ItemSearch.matches(bus, "td tongding"));
    }

    @Test
    public void ignoresItemFormattingCodesForChineseAndPinyinSearch() {
        assertTrue(ItemSearch.matches("\u00a7a铁\u00a7r锭", "铁锭"));
        assertTrue(ItemSearch.matches("\u00a7a铁\u00a7r锭", "td"));
        assertTrue(ItemSearch.matches("\u00a76不锈钢锭", "buxiugangding"));
    }

    @Test
    public void supportsAlternativeReadingsAndDoesNotMatchUnrelatedNames() {
        assertTrue(ItemSearch.matches("重氢", "zhongqing"));
        assertTrue(ItemSearch.matches("重氢", "zq"));
        assertFalse(ItemSearch.matches("铁锭", "bxgd"));
        assertFalse(ItemSearch.matches("copper ingot", "td"));
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
