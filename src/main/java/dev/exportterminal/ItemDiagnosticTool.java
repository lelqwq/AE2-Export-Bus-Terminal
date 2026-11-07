package dev.exportterminal;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import appeng.api.features.INetworkEncodable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemDiagnosticTool extends Item implements INetworkEncodable {

    public ItemDiagnosticTool() {
        setUnlocalizedName(ExportBusTerminalMod.MOD_ID + ".diagnostic_tool");
        setTextureName(ExportBusTerminalMod.MOD_ID + ":diagnostic_tool");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public String getEncryptionKey(ItemStack stack) {
        if (!stack.hasTagCompound()) return "";
        NBTTagCompound binding = stack.getTagCompound()
            .getCompoundTag("ExportTerminalBinding");
        return NetworkBinding.hasSecurityKey(binding) ? Long.toString(binding.getLong("securityKey")) : "";
    }

    @Override
    public void setEncryptionKey(ItemStack stack, String key, String name) {
        long securityKey = Long.parseLong(key);
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound binding = new NBTTagCompound();
        binding.setLong("securityKey", securityKey);
        stack.getTagCompound()
            .setTag("ExportTerminalBinding", binding);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            ScanService.scanBound((EntityPlayerMP) player);
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        boolean linked = !getEncryptionKey(stack).isEmpty();
        lines.add(
            (linked ? "\u00a7a" : "\u00a7c") + StatCollector
                .translateToLocal(linked ? "ae2exportterminal.tool.linked" : "ae2exportterminal.tool.unlinked"));
        lines.add(StatCollector.translateToLocal("ae2exportterminal.tool.bind"));
        lines.add(StatCollector.translateToLocal("ae2exportterminal.tool.open"));
        lines.add(StatCollector.translateToLocal("ae2exportterminal.tool.readonly"));
    }
}
