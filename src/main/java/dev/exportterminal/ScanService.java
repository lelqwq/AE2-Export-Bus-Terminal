package dev.exportterminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.parts.automation.PartExportBus;
import appeng.parts.automation.PartSharedItemBus;
import appeng.parts.automation.PartUpgradeable;
import appeng.tile.inventory.IAEStackInventory;
import cpw.mods.fml.common.Loader;
import dev.exportterminal.compat.FluidBusSupport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class ScanService {

    private static final AtomicInteger SESSIONS = new AtomicInteger();
    private static final String BINDING = "ExportTerminalBinding";
    private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();

    private static final class Snapshot {

        int session;
        long key;
        List<byte[]> entries;
    }

    public static void forget(UUID player) {
        SNAPSHOTS.remove(player);
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }

    private ScanService() {}

    public static void scanBound(EntityPlayerMP player) {
        scanBound(player, 0);
    }

    public static void scanBound(EntityPlayerMP player, int previousSession) {
        ItemStack stack = player.getHeldItem();
        if (player.isDead || stack == null || stack.getItem() != ExportBusTerminalMod.tool) return;
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(BINDING)) {
            tell(player, "ae2exportterminal.error.unbound");
            return;
        }
        NBTTagCompound binding = stack.getTagCompound()
            .getCompoundTag(BINDING);
        if (!NetworkBinding.hasSecurityKey(binding)) {
            tell(player, "ae2exportterminal.error.rebind");
            return;
        }
        IGrid grid = NetworkBinding.resolve(
            binding,
            AEApi.instance()
                .registries()
                .locatable());
        if (!canRead(player, grid)) return;
        scan(player, stack, grid, previousSession, binding.getLong("securityKey"));
    }

    private static boolean canRead(EntityPlayerMP player, IGrid grid) {
        if (grid == null || grid.isEmpty()) {
            tell(player, "ae2exportterminal.error.missing");
            return false;
        }
        ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        if (security == null || !security.hasPermission(player, SecurityPermissions.BUILD)) {
            tell(player, "ae2exportterminal.error.permission");
            return false;
        }
        return true;
    }

    private static void scan(final EntityPlayerMP player, ItemStack stack, IGrid grid, int previousSession, long key) {
        NBTTagCompound tag = stack.getTagCompound();
        long now = player.worldObj.getTotalWorldTime();
        long elapsed = now - tag.getLong("LastScan");
        if (tag.hasKey("LastScan") && elapsed >= 0 && elapsed < 20) return;
        tag.setLong("LastScan", now);
        List<BusEntry> entries = new ArrayList<>();
        Set<PartUpgradeable> seen = Collections.newSetFromMap(new IdentityHashMap<PartUpgradeable, Boolean>());
        for (Class<? extends IGridHost> type : grid.getMachinesClasses()) {
            boolean fluidBus = Loader.isModLoaded("ae2fc") && FluidBusSupport.isExportBus(type);
            String kind = busKind(type, fluidBus);
            if (kind == null) continue;
            for (IGridNode node : grid.getMachines(type)) {
                PartUpgradeable bus = (PartUpgradeable) node.getMachine();
                if (!seen.add(bus) || bus.getHost() == null) continue;
                TileEntity tile = bus.getHost()
                    .getTile();
                if (tile == null || tile.isInvalid() || tile.getWorldObj() == null) continue;
                if (!tile.getWorldObj()
                    .getChunkProvider()
                    .chunkExists(tile.xCoord >> 4, tile.zCoord >> 4)) continue;
                BusEntry entry = new BusEntry();
                entry.dimension = tile.getWorldObj().provider.dimensionId;
                entry.x = tile.xCoord;
                entry.y = tile.yCoord;
                entry.z = tile.zCoord;
                entry.side = bus.getSide()
                    .ordinal();
                entry.online = node.isActive();
                entry.fluidBus = fluidBus;
                entry.kind = kind;
                entry.enabledSlots = kind.equals("manabus") ? 0
                    : Math.min(9, 1 + bus.getInstalledUpgrades(Upgrades.CAPACITY) * 4);
                entry.craftingCard = bus.getInstalledUpgrades(Upgrades.CRAFTING) > 0;
                entry.redstoneCard = bus.getInstalledUpgrades(Upgrades.REDSTONE) > 0;
                entry.redstoneMode = bus.getRSMode()
                    .name();
                if ((kind.equals("itembus") || kind.equals("universalbus"))
                    && bus.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                    entry.fuzzyMode = bus.getConfigManager()
                        .getSetting(Settings.FUZZY_MODE)
                        .name();
                }
                if (bus instanceof PartSharedItemBus<?>) {
                    entry.oreFilter = ((PartSharedItemBus<?>) bus).getFilter();
                    IAEStackInventory filters = ((PartSharedItemBus<?>) bus).getAEInventoryByName(StorageName.CONFIG);
                    for (int i = 0; i < Math.min(9, filters.getSizeInventory()); i++) {
                        IAEStack<?> filter = filters.getAEStackInSlot(i);
                        if (filter instanceof IAEFluidStack) {
                            if (Loader.isModLoaded("ae2fc")) {
                                entry.fluidFilters[i] = true;
                                entry.filters[i] = ItemFluidPacket
                                    .newDisplayStack(((IAEFluidStack) filter).getFluidStack());
                            } else {
                                entry.filterNames[i] = filter.getLocalizedName();
                            }
                        } else if (filter instanceof IAEItemStack) {
                            ItemStack item = ((IAEItemStack) filter).getItemStack();
                            if (item != null) {
                                entry.filters[i] = item.copy();
                                entry.filters[i].stackSize = 1;
                            }
                        } else if (filter != null) {
                            entry.filterNames[i] = filter.getLocalizedName();
                            ItemStack icon = filter.getItemStackForNEI();
                            if (icon != null) {
                                entry.filters[i] = icon.copy();
                                entry.filters[i].stackSize = 1;
                            }
                        }
                    }
                }
                entries.add(entry);
            }
        }
        Collections.sort(entries, new Comparator<BusEntry>() {

            @Override
            public int compare(BusEntry a, BusEntry b) {
                boolean localA = a.dimension == player.dimension;
                boolean localB = b.dimension == player.dimension;
                if (localA != localB) return localA ? -1 : 1;
                int dimension = Integer.compare(a.dimension, b.dimension);
                if (dimension != 0) return dimension;
                int distance = Double.compare(
                    a.distanceSquared(player.posX, player.posY, player.posZ),
                    b.distanceSquared(player.posX, player.posY, player.posZ));
                if (distance != 0) return distance;
                int x = Integer.compare(a.x, b.x);
                if (x != 0) return x;
                int y = Integer.compare(a.y, b.y);
                if (y != 0) return y;
                int z = Integer.compare(a.z, b.z);
                return z != 0 ? z : Integer.compare(a.side, b.side);
            }
        });
        int session = SESSIONS.incrementAndGet();
        Snapshot old = SNAPSHOTS.get(player.getUniqueID());
        boolean delta = previousSession != 0 && old != null && old.session == previousSession && old.key == key;
        List<byte[]> encoded = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ByteBuf buffer = Unpooled.buffer();
            byte[] bytes;
            try {
                new TerminalNetwork.Result(0, 0, entries.get(i)).toBytes(buffer);
                bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
            } finally {
                buffer.release();
            }
            encoded.add(bytes);
        }
        List<Integer> changed = changedIndices(delta ? old.entries : Collections.<byte[]>emptyList(), encoded);
        Snapshot snapshot = new Snapshot();
        snapshot.session = session;
        snapshot.key = key;
        snapshot.entries = encoded;
        SNAPSHOTS.put(player.getUniqueID(), snapshot);
        TerminalNetwork.CHANNEL
            .sendTo(new TerminalNetwork.Result(session, entries.size(), changed.size(), delta), player);
        for (int index : changed)
            TerminalNetwork.CHANNEL.sendTo(new TerminalNetwork.Result(session, index, entries.get(index)), player);
    }

    static List<Integer> changedIndices(List<byte[]> previous, List<byte[]> current) {
        List<Integer> changed = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            if (i >= previous.size() || !Arrays.equals(current.get(i), previous.get(i))) changed.add(i);
        }
        return changed;
    }

    static String busKind(Class<?> type, boolean fluidBus) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            String name = current.getName();
            if (name.equals("com.asdflj.ae2thing.common.parts.universal.PartUniversalExportBus")) return "universalbus";
            if (name.equals("com.vcwdfca.appbot.part.PartManaExportBus")) return "manabus";
            if (name.equals("thaumicenergistics.common.parts.PartEssentiaExportBus")) return "essentiabus";
        }
        if (fluidBus) return "fluidbus";
        return PartExportBus.class.isAssignableFrom(type) ? "itembus" : null;
    }

    private static void tell(EntityPlayerMP player, String key) {
        player.addChatMessage(new ChatComponentTranslation(key));
    }
}
