package dev.exportterminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.parts.automation.PartExportBus;

public final class ScanService {

    private static final AtomicInteger SESSIONS = new AtomicInteger();
    private static final String BINDING = "ExportTerminalBinding";

    private ScanService() {}

    public static void bindAndScan(EntityPlayerMP player, ItemStack stack, int x, int y, int z, int clickedSide,
        float hitX, float hitY, float hitZ) {
        TileEntity tile = player.worldObj.getTileEntity(x, y, z);
        int partSide = -1;
        IGridNode node = null;
        if (tile instanceof IPartHost) {
            IPartHost host = (IPartHost) tile;
            SelectedPart selected = host.selectPart(Vec3.createVectorHelper(hitX, hitY, hitZ));
            if (selected.part != null && selected.part.getGridNode() != null) {
                node = selected.part.getGridNode();
                partSide = selected.side.ordinal();
            } else {
                IPart cable = host.getPart(ForgeDirection.UNKNOWN);
                if (cable != null) {
                    node = cable.getGridNode();
                    partSide = ForgeDirection.UNKNOWN.ordinal();
                }
            }
        }
        if (node == null && tile instanceof IGridHost) {
            node = ((IGridHost) tile).getGridNode(ForgeDirection.getOrientation(clickedSide));
            if (node == null) node = ((IGridHost) tile).getGridNode(ForgeDirection.UNKNOWN);
        }
        if (!canRead(player, node)) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound binding = new NBTTagCompound();
        binding.setInteger("dim", player.dimension);
        binding.setInteger("x", x);
        binding.setInteger("y", y);
        binding.setInteger("z", z);
        binding.setInteger("part", partSide);
        binding.setInteger("face", clickedSide);
        stack.getTagCompound()
            .setTag(BINDING, binding);
        scan(player, stack, node);
    }

    public static void scanBound(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItem();
        if (player.isDead || stack == null || stack.getItem() != ExportBusTerminalMod.tool) return;
        if (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(BINDING)) {
            tell(player, "ae2exportterminal.error.unbound");
            return;
        }
        NBTTagCompound binding = stack.getTagCompound()
            .getCompoundTag(BINDING);
        if (binding.getInteger("dim") != player.dimension) {
            tell(player, "ae2exportterminal.error.dimension");
            return;
        }
        int x = binding.getInteger("x");
        int y = binding.getInteger("y");
        int z = binding.getInteger("z");
        World world = player.worldObj;
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) {
            tell(player, "ae2exportterminal.error.unloaded");
            return;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        IGridNode node = null;
        int side = binding.getInteger("part");
        if (side >= 0 && side <= 6 && tile instanceof IPartHost) {
            IPart part = ((IPartHost) tile).getPart(ForgeDirection.getOrientation(side));
            if (part != null) node = part.getGridNode();
        } else if (side == -1 && tile instanceof IGridHost) {
            node = ((IGridHost) tile).getGridNode(ForgeDirection.getOrientation(binding.getInteger("face")));
            if (node == null) node = ((IGridHost) tile).getGridNode(ForgeDirection.UNKNOWN);
        }
        if (canRead(player, node)) scan(player, stack, node);
    }

    private static boolean canRead(EntityPlayerMP player, IGridNode node) {
        if (node == null || node.getGrid() == null) {
            tell(player, "ae2exportterminal.error.missing");
            return false;
        }
        ISecurityGrid security = node.getGrid()
            .getCache(ISecurityGrid.class);
        if (security == null || !security.hasPermission(player, SecurityPermissions.BUILD)) {
            tell(player, "ae2exportterminal.error.permission");
            return false;
        }
        return true;
    }

    private static void scan(final EntityPlayerMP player, ItemStack stack, IGridNode anchor) {
        NBTTagCompound tag = stack.getTagCompound();
        long now = player.worldObj.getTotalWorldTime();
        long elapsed = now - tag.getLong("LastScan");
        if (tag.hasKey("LastScan") && elapsed >= 0 && elapsed < 20) return;
        tag.setLong("LastScan", now);
        IGrid grid = anchor.getGrid();
        List<BusEntry> entries = new ArrayList<>();
        Set<PartExportBus> seen = Collections.newSetFromMap(new IdentityHashMap<PartExportBus, Boolean>());
        for (Class<? extends IGridHost> type : grid.getMachinesClasses()) {
            if (!PartExportBus.class.isAssignableFrom(type)) continue;
            for (IGridNode node : grid.getMachines(type)) {
                PartExportBus bus = (PartExportBus) node.getMachine();
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
                entry.enabledSlots = Math.min(9, 1 + bus.getInstalledUpgrades(Upgrades.CAPACITY) * 4);
                entry.craftingCard = bus.getInstalledUpgrades(Upgrades.CRAFTING) > 0;
                entry.redstoneCard = bus.getInstalledUpgrades(Upgrades.REDSTONE) > 0;
                entry.redstoneMode = bus.getRSMode()
                    .name();
                if (bus.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
                    entry.fuzzyMode = bus.getConfigManager()
                        .getSetting(Settings.FUZZY_MODE)
                        .name();
                }
                entry.oreFilter = bus.getFilter();
                IInventory filters = bus.getInventoryByName("config");
                for (int i = 0; i < Math.min(9, filters.getSizeInventory()); i++) {
                    ItemStack filter = filters.getStackInSlot(i);
                    if (filter != null) {
                        entry.filters[i] = filter.copy();
                        entry.filters[i].stackSize = 1;
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
        TerminalNetwork.CHANNEL.sendTo(new TerminalNetwork.Result(session, entries.size()), player);
        for (BusEntry entry : entries) {
            TerminalNetwork.CHANNEL.sendTo(new TerminalNetwork.Result(session, entry), player);
        }
    }

    private static void tell(EntityPlayerMP player, String key) {
        player.addChatMessage(new ChatComponentTranslation(key));
    }
}
