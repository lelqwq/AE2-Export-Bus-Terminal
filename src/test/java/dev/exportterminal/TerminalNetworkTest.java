package dev.exportterminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TerminalNetworkTest {

    @Test
    public void emptyNetworkStillStartsACompleteSnapshot() {
        ByteBuf wire = Unpooled.buffer();
        try {
            new TerminalNetwork.Result(42, 0).toBytes(wire);
            TerminalNetwork.Result result = new TerminalNetwork.Result();
            result.fromBytes(wire);
            assertEquals(42, result.session);
            assertEquals(0, result.total);
            assertNull(result.entry);
            assertFalse(wire.isReadable());
        } finally {
            wire.release();
        }
    }

    @Test
    public void busSnapshotPreservesEmptySlotsDimensionFaceAndConfiguration() {
        BusEntry entry = new BusEntry();
        entry.dimension = -1;
        entry.x = -120;
        entry.y = 64;
        entry.z = 320;
        entry.side = 5;
        entry.online = true;
        entry.enabledSlots = 5;
        entry.craftingCard = true;
        entry.redstoneCard = true;
        entry.redstoneMode = "LOW_SIGNAL";
        entry.fuzzyMode = "IGNORE_ALL";
        entry.oreFilter = "ingotIron | ingotCopper";
        ByteBuf wire = Unpooled.buffer();
        try {
            new TerminalNetwork.Result(43, entry).toBytes(wire);
            TerminalNetwork.Result result = new TerminalNetwork.Result();
            result.fromBytes(wire);
            BusEntry copy = result.entry;
            assertNotNull(copy);
            assertEquals(43, result.session);
            assertEquals(-1, copy.dimension);
            assertEquals(-120, copy.x);
            assertEquals(64, copy.y);
            assertEquals(320, copy.z);
            assertEquals(5, copy.side);
            assertEquals(5, copy.enabledSlots);
            assertTrue(copy.online);
            assertTrue(copy.craftingCard);
            assertTrue(copy.redstoneCard);
            assertEquals(entry.redstoneMode, copy.redstoneMode);
            assertEquals(entry.fuzzyMode, copy.fuzzyMode);
            assertEquals(entry.oreFilter, copy.oreFilter);
            for (int slot = 0; slot < 9; slot++) assertNull(copy.filters[slot]);
            assertFalse(wire.isReadable());
        } finally {
            wire.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidFaceFromWire() {
        ByteBuf wire = Unpooled.buffer();
        try {
            wire.writeInt(1)
                .writeBoolean(true)
                .writeInt(0)
                .writeInt(0)
                .writeInt(64)
                .writeInt(0)
                .writeByte(6)
                .writeBoolean(false)
                .writeByte(9);
            new TerminalNetwork.Result().fromBytes(wire);
        } finally {
            wire.release();
        }
    }
}
