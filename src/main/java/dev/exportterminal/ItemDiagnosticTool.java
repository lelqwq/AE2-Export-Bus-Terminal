package dev.exportterminal;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import appeng.api.networking.IGridHost;
import appeng.api.parts.IPartHost;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemDiagnosticTool extends Item {

    public ItemDiagnosticTool() {
        setUnlocalizedName(ExportBusTerminalMod.MOD_ID + ".diagnostic_tool");
        setTextureName(ExportBusTerminalMod.MOD_ID + ":diagnostic_tool");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        // Returning true on the client would prevent Minecraft from sending its interaction packet.
        if (world.isRemote) return false;
        return onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGridHost) && !(tile instanceof IPartHost)) return false;
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            ScanService.bindAndScan((EntityPlayerMP) player, stack, x, y, z, side, hitX, hitY, hitZ);
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            ScanService.scanBound((EntityPlayerMP) player);
        }
        return stack;
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add(StatCollector.translateToLocal("ae2exportterminal.tool.bind"));
        lines.add(StatCollector.translateToLocal("ae2exportterminal.tool.open"));
        lines.add(StatCollector.translateToLocal("ae2exportterminal.tool.readonly"));
    }
}
