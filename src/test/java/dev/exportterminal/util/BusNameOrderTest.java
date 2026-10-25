package dev.exportterminal.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import dev.exportterminal.BusEntry;

public class BusNameOrderTest {

    @Test
    public void namesMixBusTypesAndEqualNamesRetainServerDistanceOrder() {
        BusEntry ironNear = new BusEntry();
        BusEntry ironFar = new BusEntry();
        BusEntry fluid = new BusEntry();
        fluid.fluidBus = true;
        BusEntry empty = new BusEntry();
        BusNameOrder order = new BusNameOrder(0, Locale.ENGLISH);
        order.setNames(ironNear, Arrays.asList("Iron"));
        order.setNames(ironFar, Arrays.asList("\u00a7aIRON"));
        order.setNames(fluid, Arrays.asList("Zinc", "Copper"));
        List<BusEntry> entries = new ArrayList<>(Arrays.asList(empty, ironNear, fluid, ironFar));
        Collections.sort(entries, order);
        assertEquals(Arrays.asList(fluid, ironNear, ironFar, empty), entries);
    }

    @Test
    public void dimensionPriorityRemainsAheadOfNames() {
        BusEntry local = new BusEntry();
        local.dimension = 7;
        BusEntry negative = new BusEntry();
        negative.dimension = -1;
        BusEntry overworld = new BusEntry();
        BusNameOrder order = new BusNameOrder(7, Locale.ENGLISH);
        order.setNames(local, Arrays.asList("Zinc"));
        order.setNames(negative, Arrays.asList("Zinc"));
        order.setNames(overworld, Arrays.asList("Copper"));
        List<BusEntry> entries = new ArrayList<>(Arrays.asList(overworld, negative, local));
        Collections.sort(entries, order);
        assertEquals(Arrays.asList(local, negative, overworld), entries);
    }

    @Test
    public void chineseNamesFollowChineseCollationAndIgnoreEmptyFormattedNames() {
        BusEntry iron = new BusEntry();
        BusEntry copper = new BusEntry();
        BusEntry steam = new BusEntry();
        BusEntry empty = new BusEntry();
        BusNameOrder order = new BusNameOrder(0, Locale.SIMPLIFIED_CHINESE);
        order.setNames(iron, Arrays.asList("铁锭"));
        order.setNames(copper, Arrays.asList("铜锭"));
        order.setNames(steam, Arrays.asList("蒸汽"));
        order.setNames(empty, Arrays.asList("", "\u00a7a "));
        List<BusEntry> entries = new ArrayList<>(Arrays.asList(empty, steam, copper, iron));
        Collections.sort(entries, order);
        assertEquals(Arrays.asList(iron, copper, steam, empty), entries);
    }
}
