package dev.exportterminal;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public final class TerminalNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ae2exportterm");

    private TerminalNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(ResultHandler.class, Result.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(RefreshHandler.class, Refresh.class, 1, Side.SERVER);
    }

    public static class Refresh implements IMessage {

        @Override
        public void fromBytes(ByteBuf buf) {}

        @Override
        public void toBytes(ByteBuf buf) {}
    }

    public static class RefreshHandler implements IMessageHandler<Refresh, IMessage> {

        private static final Set<UUID> PENDING = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

        @Override
        public IMessage onMessage(Refresh message, MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().playerEntity;
            final UUID id = player.getUniqueID();
            if (!PENDING.add(id)) return null;
            CommonProxy.scheduleServer(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (player.playerNetServerHandler == null
                            || !player.playerNetServerHandler.netManager.isChannelOpen()) return;
                        ScanService.scanBound(player);
                    } finally {
                        PENDING.remove(id);
                    }
                }
            });
            return null;
        }
    }

    public static class Result implements IMessage {

        public int session;
        public int total;
        public BusEntry entry;

        public Result() {}

        public Result(int session, int total) {
            this.session = session;
            this.total = total;
        }

        public Result(int session, BusEntry entry) {
            this.session = session;
            this.entry = entry;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(session);
            buf.writeBoolean(entry != null);
            if (entry == null) {
                buf.writeInt(total);
                return;
            }
            buf.writeInt(entry.dimension);
            buf.writeInt(entry.x);
            buf.writeInt(entry.y);
            buf.writeInt(entry.z);
            buf.writeByte(entry.side);
            buf.writeBoolean(entry.online);
            buf.writeByte(entry.enabledSlots);
            buf.writeBoolean(entry.craftingCard);
            buf.writeBoolean(entry.redstoneCard);
            ByteBufUtils.writeUTF8String(buf, entry.redstoneMode);
            ByteBufUtils.writeUTF8String(buf, entry.fuzzyMode);
            ByteBufUtils.writeUTF8String(buf, entry.oreFilter);
            for (int i = 0; i < 9; i++) ByteBufUtils.writeItemStack(buf, entry.filters[i]);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            session = buf.readInt();
            if (!buf.readBoolean()) {
                total = buf.readInt();
                if (total < 0) throw new IllegalArgumentException("Negative bus count");
                return;
            }
            entry = new BusEntry();
            entry.dimension = buf.readInt();
            entry.x = buf.readInt();
            entry.y = buf.readInt();
            entry.z = buf.readInt();
            entry.side = buf.readUnsignedByte();
            entry.online = buf.readBoolean();
            entry.enabledSlots = buf.readUnsignedByte();
            if (entry.side > 5 || entry.enabledSlots < 1 || entry.enabledSlots > 9) {
                throw new IllegalArgumentException("Invalid bus side or capacity");
            }
            entry.craftingCard = buf.readBoolean();
            entry.redstoneCard = buf.readBoolean();
            entry.redstoneMode = ByteBufUtils.readUTF8String(buf);
            entry.fuzzyMode = ByteBufUtils.readUTF8String(buf);
            entry.oreFilter = ByteBufUtils.readUTF8String(buf);
            for (int i = 0; i < 9; i++) entry.filters[i] = ByteBufUtils.readItemStack(buf);
        }
    }

    public static class ResultHandler implements IMessageHandler<Result, IMessage> {

        @Override
        public IMessage onMessage(Result message, MessageContext context) {
            ExportBusTerminalMod.proxy.receive(message);
            return null;
        }
    }
}
