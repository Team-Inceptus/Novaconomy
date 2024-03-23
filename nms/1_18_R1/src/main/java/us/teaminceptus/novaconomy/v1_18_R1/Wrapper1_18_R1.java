package us.teaminceptus.novaconomy.v1_18_R1;

import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.function.Consumer;

import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;

final class Wrapper1_18_R1 implements Wrapper {

    @Override
    public int getCommandVersion() { return 2; }

    @Override
    public void sendActionbar(Player p, String message) {
        sendActionbar(p, new TextComponent(message));
    }

    @Override
    public void sendActionbar(Player p, BaseComponent component) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
    }

    @Override
    public boolean isAgeable(Block b) {
        return b.getBlockData() instanceof Ageable;
    }

    @Override
    public void removeItem(PlayerInteractEvent e) {
        e.getPlayer().getEquipment().setItem(e.getHand(), null);
    }

    @Override
    public boolean isCrop(Material m) {
        BlockData d = m.createBlockData();
        return d instanceof Ageable && !(d instanceof Fire);
    }

    @Override
    public NovaInventory createInventory(String id, String name, int size) {
        return new NovaInventory1_18_R1(id, name, size);
    }

    @Override
    public NBTWrapper createNBTWrapper(org.bukkit.inventory.ItemStack item) {
        return new NBTWrapper1_18_R1(item);
    }

    @Override
    public void addPacketInjector(Player p) {
        ServerPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.connection.connection.channel;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return;

        ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, new PacketHandler1_18_R1(p));
    }

    @Override
    public void removePacketInjector(Player p) {
        ServerPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.connection.connection.channel;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) == null) return;
        ch.pipeline().remove(PACKET_INJECTOR_ID);
    }

    @Override
    public void sendSign(Player p, Consumer<String[]> lines) {
        addPacketInjector(p);

        Location l = p.getLocation();
        BlockPos pos = new BlockPos(l.getBlockX(), 255, l.getBlockZ());

        ClientboundBlockUpdatePacket sent1 = new ClientboundBlockUpdatePacket(pos, Blocks.OAK_SIGN.defaultBlockState());
        ((CraftPlayer) p).getHandle().connection.send(sent1);

        ClientboundOpenSignEditorPacket sent2 = new ClientboundOpenSignEditorPacket(pos);
        ((CraftPlayer) p).getHandle().connection.send(sent2);

        PacketHandler1_18_R1.PACKET_HANDLERS.put(p.getUniqueId(), packetO -> {
            if (!(packetO instanceof ServerboundSignUpdatePacket packet)) return false;

            lines.accept(packet.getLines());
            return true;
        });

        scheduler.syncLater(() -> {
            ClientboundBlockUpdatePacket sent3 = new ClientboundBlockUpdatePacket(pos, Blocks.AIR.defaultBlockState());
            ((CraftPlayer) p).getHandle().connection.send(sent3);
        }, 2L);
    }

}
