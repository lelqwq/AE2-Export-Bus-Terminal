package dev.exportterminal.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import dev.exportterminal.CommonProxy;
import dev.exportterminal.TerminalNetwork;

public class ClientProxy extends CommonProxy {

    private final Queue<TerminalNetwork.Result> results = new ConcurrentLinkedQueue<>();
    private GuiExportBusTerminal terminal;
    private volatile boolean disconnected;

    @Override
    public void init() {
        super.init();
        MinecraftForge.EVENT_BUS.register(new BusHighlight());
    }

    @Override
    public void receive(TerminalNetwork.Result packet) {
        results.add(packet);
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (disconnected) {
            results.clear();
            terminal = null;
            BusHighlight.clear();
            disconnected = false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        for (int i = 0; i < 512; i++) {
            TerminalNetwork.Result packet = results.poll();
            if (packet == null) break;
            if (minecraft.thePlayer == null || minecraft.theWorld == null) continue;
            if (packet.entry == null) {
                if (!(minecraft.currentScreen instanceof GuiExportBusTerminal)) {
                    terminal = new GuiExportBusTerminal();
                    minecraft.displayGuiScreen(terminal);
                } else {
                    terminal = (GuiExportBusTerminal) minecraft.currentScreen;
                }
                terminal.begin(packet.session, packet.total);
            } else if (terminal != null) {
                terminal.accept(packet.session, packet.entry);
            }
        }
    }

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        disconnected = true;
    }
}
