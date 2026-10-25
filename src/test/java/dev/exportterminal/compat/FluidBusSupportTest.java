package dev.exportterminal.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.glodblock.github.common.parts.PartFluidExportBus;
import com.glodblock.github.common.parts.PartFluidImportBus;

import appeng.parts.automation.PartExportBus;

public class FluidBusSupportTest {

    @Test
    public void detectsTheRealAe2FcExportBusWithoutIncludingImportOrItemBuses() {
        assertTrue(FluidBusSupport.isExportBus(PartFluidExportBus.class));
        assertFalse(FluidBusSupport.isExportBus(PartFluidImportBus.class));
        assertFalse(FluidBusSupport.isExportBus(PartExportBus.class));
    }
}
