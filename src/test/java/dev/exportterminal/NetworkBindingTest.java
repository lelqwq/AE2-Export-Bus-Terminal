package dev.exportterminal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import appeng.api.features.ILocatable;
import appeng.api.features.ILocatableRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityProvider;

public class NetworkBindingTest {

    @Test
    public void resolvesStationToItsCurrentGridAfterReload() {
        IGrid grid = proxy(IGrid.class, (method, args) -> null);
        IGridNode node = proxy(IGridNode.class, (method, args) -> method.equals("getGrid") ? grid : null);
        Object station = Proxy.newProxyInstance(
            ILocatable.class.getClassLoader(),
            new Class<?>[] { ILocatable.class, ISecurityProvider.class, IGridHost.class },
            (p, m, a) -> m.getName()
                .equals("getGridNode") ? node : null);
        ILocatableRegistry registry = proxy(
            ILocatableRegistry.class,
            (method, args) -> method.equals("getLocatableBy") && ((Long) args[0]) == 42L ? station : null);
        NBTTagCompound binding = new NBTTagCompound();
        binding.setLong("securityKey", 42);
        NBTTagCompound reloaded = (NBTTagCompound) binding.copy();
        assertTrue(NetworkBinding.hasSecurityKey(reloaded));
        assertSame(grid, NetworkBinding.resolve(reloaded, registry));
        binding.setLong("securityKey", 43);
        assertNull(NetworkBinding.resolve(binding, registry));
    }

    @Test
    public void oldBindingCannotSelectAnUnrelatedStation() {
        NBTTagCompound binding = new NBTTagCompound();
        binding.setLong("network", 42);
        ILocatableRegistry registry = proxy(
            ILocatableRegistry.class,
            (method, args) -> { throw new AssertionError("Old binding must be rebound"); });
        assertNull(NetworkBinding.resolve(binding, registry));
    }

    private interface Call {

        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(
            Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (p, m, a) -> call.invoke(m.getName(), a)));
    }
}
