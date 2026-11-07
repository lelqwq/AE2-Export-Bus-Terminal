package dev.exportterminal;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.features.ILocatable;
import appeng.api.features.ILocatableRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityProvider;

public final class NetworkBinding {

    private static final String SECURITY_KEY = "securityKey";

    private NetworkBinding() {}

    public static boolean hasSecurityKey(NBTTagCompound binding) {
        return binding.hasKey(SECURITY_KEY, 4);
    }

    public static IGrid resolve(NBTTagCompound binding, ILocatableRegistry registry) {
        if (!hasSecurityKey(binding)) return null;
        ILocatable station = registry.getLocatableBy(binding.getLong(SECURITY_KEY));
        if (!(station instanceof ISecurityProvider) || !(station instanceof IGridHost)) return null;
        IGridNode node = ((IGridHost) station).getGridNode(ForgeDirection.UNKNOWN);
        return node == null ? null : node.getGrid();
    }
}
