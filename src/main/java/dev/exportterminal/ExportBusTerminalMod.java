package dev.exportterminal;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = ExportBusTerminalMod.MOD_ID,
    name = "AE2 Export Bus Terminal",
    version = "0.1.3",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:appliedenergistics2@[rv3-beta-695-GTNH];after:ae2fc@[1.4.120-gtnh]")
public class ExportBusTerminalMod {

    public static final String MOD_ID = "ae2exportterminal";
    public static Item tool;

    @SidedProxy(clientSide = "dev.exportterminal.client.ClientProxy", serverSide = "dev.exportterminal.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        tool = new ItemDiagnosticTool();
        GameRegistry.registerItem(tool, "diagnostic_tool");
        TerminalNetwork.init();
        proxy.init();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ItemStack exportBus = AEApi.instance()
            .definitions()
            .parts()
            .exportBus()
            .maybeStack(1)
            .get();
        GameRegistry.addShapelessRecipe(
            new ItemStack(tool),
            exportBus,
            new ItemStack(Items.compass),
            new ItemStack(Items.redstone),
            new ItemStack(Items.quartz));
    }
}
