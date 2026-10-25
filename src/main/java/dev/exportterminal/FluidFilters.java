package dev.exportterminal;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public final class FluidFilters {

    private FluidFilters() {}

    public static NBTTagCompound packetData(NBTTagCompound packet) {
        if (packet == null || !packet.hasKey("FluidStack", 10)) return null;
        NBTTagCompound fluid = packet.getCompoundTag("FluidStack");
        if (fluid.getString("FluidName")
            .isEmpty() || fluid.getInteger("Amount") <= 0) return null;
        return fluid;
    }

    public static FluidStack fromPacket(ItemStack stack) {
        NBTTagCompound fluid = stack == null ? null : packetData(stack.getTagCompound());
        return fluid == null ? null : FluidStack.loadFluidStackFromNBT(fluid);
    }
}
