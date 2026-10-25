package dev.exportterminal.compat;

import com.glodblock.github.common.parts.PartFluidExportBus;

public final class FluidBusSupport {

    private FluidBusSupport() {}

    public static boolean isExportBus(Class<?> type) {
        return PartFluidExportBus.class.isAssignableFrom(type);
    }
}
