package dev.exportterminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import dev.exportterminal.util.ItemSearch;

public class FluidFiltersTest {

    @Test
    public void readsAe2FcDisplayPacketWithoutChangingFluidOrVariantData() {
        NBTTagCompound fluid = new NBTTagCompound();
        fluid.setString("FluidName", "molten.solderingalloy");
        fluid.setInteger("Amount", 1000);
        NBTTagCompound variant = new NBTTagCompound();
        variant.setString("variant", "test");
        fluid.setTag("Tag", variant);
        NBTTagCompound packet = new NBTTagCompound();
        packet.setTag("FluidStack", fluid);
        packet.setBoolean("DisplayOnly", true);
        NBTTagCompound original = (NBTTagCompound) packet.copy();
        NBTTagCompound decoded = FluidFilters.packetData(packet);
        assertEquals("molten.solderingalloy", decoded.getString("FluidName"));
        assertEquals(1000, decoded.getInteger("Amount"));
        assertEquals(
            "test",
            decoded.getCompoundTag("Tag")
                .getString("variant"));
        assertEquals(original, packet);
    }

    @Test
    public void rejectsEmptyMalformedAndNonPositiveFluidPackets() {
        assertNull(FluidFilters.packetData(null));
        NBTTagCompound packet = new NBTTagCompound();
        assertNull(FluidFilters.packetData(packet));
        packet.setString("FluidStack", "invalid");
        assertNull(FluidFilters.packetData(packet));
        NBTTagCompound fluid = new NBTTagCompound();
        packet.setTag("FluidStack", fluid);
        fluid.setString("FluidName", "steam");
        assertNull(FluidFilters.packetData(packet));
        fluid.setInteger("Amount", -1);
        assertNull(FluidFilters.packetData(packet));
    }

    @Test
    public void fluidNamesAndRegistryNamesSupportPinyinSearchTogether() {
        String search = "流体 液态氧 liquidoxygen 蒸汽 steam 熔融焊锡 molten.solderingalloy";
        assertTrue(ItemSearch.matches(search, "ytY"));
        assertTrue(ItemSearch.matches(search, "zhengqi"));
        assertTrue(ItemSearch.matches(search, "steam zq"));
        assertTrue(ItemSearch.matches(search, "rrhx"));
        assertTrue(ItemSearch.matches(search, "molten.solderingalloy"));
    }
}
