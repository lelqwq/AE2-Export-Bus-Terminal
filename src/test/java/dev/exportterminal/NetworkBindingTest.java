package dev.exportterminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.core.worlddata.IWorldGridStorageData;
import appeng.me.GridStorage;

public class NetworkBindingTest {

    @Test
    public void bindsThePersistentNetworkWithoutKeepingAnAnchorOrDimension() {
        NBTTagCompound binding = new NBTTagCompound();
        binding.setInteger("dim", 0);
        binding.setInteger("x", 120);
        binding.setInteger("part", 6);
        assertTrue(NetworkBinding.bind(binding, grid(41)));
        assertTrue(NetworkBinding.hasNetwork(binding));
        assertEquals(41, binding.getLong("network"));
        assertFalse(binding.hasKey("dim"));
        assertFalse(binding.hasKey("x"));
        assertFalse(binding.hasKey("part"));
    }

    @Test
    public void resolvesTheSameNetworkAfterNbtReloadRegardlessOfTheOldDimension() {
        NBTTagCompound binding = new NBTTagCompound();
        NetworkBinding.bind(binding, grid(123456789012L));
        IGrid loadedNetwork = grid(123456789012L);
        AtomicLong requested = new AtomicLong(-1);
        IWorldGridStorageData registry = registry(loadedNetwork, requested);
        NBTTagCompound reloaded = (NBTTagCompound) binding.copy();
        assertSame(loadedNetwork, NetworkBinding.resolve(reloaded, registry));
        assertEquals(123456789012L, requested.get());
    }

    @Test
    public void zeroIsAValidNetworkIdAndUnloadedNetworksReturnNoGrid() {
        NBTTagCompound binding = new NBTTagCompound();
        NetworkBinding.bind(binding, grid(0));
        AtomicLong requested = new AtomicLong(-1);
        assertNull(NetworkBinding.resolve(binding, registry(null, requested)));
        assertEquals(0, requested.get());
    }

    @Test
    public void legacyOrCorruptBindingDoesNotSelectAnArbitraryNetwork() {
        NBTTagCompound binding = new NBTTagCompound();
        AtomicLong requested = new AtomicLong(-1);
        assertNull(NetworkBinding.resolve(binding, registry(grid(0), requested)));
        binding.setString("network", "0");
        assertNull(NetworkBinding.resolve(binding, registry(grid(0), requested)));
        assertEquals(-1, requested.get());
    }

    private static IGrid grid(final long storageId) {
        IGridNode pivot = (IGridNode) Proxy.newProxyInstance(
            IGridNode.class.getClassLoader(),
            new Class<?>[] { IGridNode.class },
            (proxy, method, args) -> {
                if (method.getName()
                    .equals("saveToNBT")) {
                    NBTTagCompound node = new NBTTagCompound();
                    node.setLong("g", storageId);
                    ((NBTTagCompound) args[1]).setTag((String) args[0], node);
                    return null;
                }
                throw new AssertionError("Unexpected node operation: " + method.getName());
            });
        return (IGrid) Proxy
            .newProxyInstance(IGrid.class.getClassLoader(), new Class<?>[] { IGrid.class }, (proxy, method, args) -> {
                if (method.getName()
                    .equals("getPivot")) return pivot;
                throw new AssertionError("Must not bind transient grid UUID or inspect an anchor: " + method.getName());
            });
    }

    private static IWorldGridStorageData registry(final IGrid network, final AtomicLong requested) {
        return (IWorldGridStorageData) Proxy.newProxyInstance(
            IWorldGridStorageData.class.getClassLoader(),
            new Class<?>[] { IWorldGridStorageData.class },
            (proxy, method, args) -> {
                if (!method.getName()
                    .equals("getGridStorage")) throw new AssertionError(method.getName());
                requested.set((Long) args[0]);
                return new GridStorage() {

                    @Override
                    public IGrid getGrid() {
                        return network;
                    }
                };
            });
    }
}
