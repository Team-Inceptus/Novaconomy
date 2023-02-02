package us.teaminceptus.novaconomy.v1_19_R1;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import java.util.function.Consumer;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;

public final class PacketHandler1_19_R1 extends ChannelDuplexHandler {
    
    public static final Map<UUID, Consumer<Packet<?>>> PACKET_HANDLERS = new HashMap<>();

    private final Player p; 

    public PacketHandler1_19_R1(Player p) {
        this.p = p;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packetO) throws Exception{
        if (!(packetO instanceof Packet<?> packet)) {
            super.channelRead(ctx, packetO);
            return;
        }

        Consumer<Packet<?>> handler = PACKET_HANDLERS.get(p.getUniqueId());
        if (handler != null) {
            handler.accept(packet);
            PACKET_HANDLERS.remove(p.getUniqueId());
        }

    }

}