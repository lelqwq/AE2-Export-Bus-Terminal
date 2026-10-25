package dev.exportterminal;

import net.minecraft.nbt.NBTTagCompound;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.core.worlddata.IWorldGridStorageData;
import appeng.me.GridStorage;

public final class NetworkBinding {

    private static final String NETWORK = "network";

    private NetworkBinding() {}

    public static boolean hasNetwork(NBTTagCompound binding) {
        return binding.hasKey(NETWORK, 4);
    }

    public static boolean bind(NBTTagCompound binding, IGrid grid) {
        IGridNode pivot = grid.getPivot();
        if (pivot == null) return false;
        NBTTagCompound saved = new NBTTagCompound();
        pivot.saveToNBT("node", saved);
        NBTTagCompound node = saved.getCompoundTag("node");
        // AE2's saved grid-storage ID survives restarts; IGrid#getId() does not.
        if (!node.hasKey("g", 4)) return false;
        binding.setLong(NETWORK, node.getLong("g"));
        for (String key : new String[] { "dim", "x", "y", "z", "part", "face" }) {
            binding.removeTag(key);
        }
        return true;
    }

    public static IGrid resolve(NBTTagCompound binding, IWorldGridStorageData registry) {
        if (!hasNetwork(binding)) return null;
        GridStorage storage = registry.getGridStorage(binding.getLong(NETWORK));
        return storage == null ? null : storage.getGrid();
    }
}
