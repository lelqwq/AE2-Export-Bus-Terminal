package dev.exportterminal;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CommonProxy {

    private static final Queue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<>();

    public void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    public static void scheduleServer(Runnable task) {
        SERVER_TASKS.add(task);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        // A network request must never read Minecraft or AE2 world state on the Netty thread.
        for (int i = 0; i < 64; i++) {
            Runnable task = SERVER_TASKS.poll();
            if (task == null) break;
            task.run();
        }
    }

    public void receive(TerminalNetwork.Result packet) {}
}
