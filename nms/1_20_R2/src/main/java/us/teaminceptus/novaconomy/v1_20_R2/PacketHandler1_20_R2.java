package us.teaminceptus.novaconomy.v1_20_R2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;
import us.teaminceptus.novaconomy.util.NovaUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

final class PacketHandler1_20_R2 extends ChannelDuplexHandler {
    
    public static final Map<UUID, Predicate<Packet<?>>> PACKET_HANDLERS = new HashMap<>();

    private final Player p; 

    public PacketHandler1_20_R2(Player p) {
        this.p = p;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packetO) throws Exception {
        if (!(packetO instanceof Packet<?> packet)) {
            super.channelRead(ctx, packetO);
            return;
        }

        Predicate<Packet<?>> handler = PACKET_HANDLERS.get(p.getUniqueId());
        if (handler != null) NovaUtil.sync(() -> {
            boolean success = handler.test(packet);
            if (success) PACKET_HANDLERS.remove(p.getUniqueId());
        });

        super.channelRead(ctx, packetO);
    }

}
