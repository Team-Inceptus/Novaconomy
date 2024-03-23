package us.teaminceptus.novaconomy.v1_16_R3;

import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.function.Consumer;

import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;

final class Wrapper1_16_R3 implements Wrapper {

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
        return new NovaInventory1_16_R3(id, name, size);
    }

    @Override
    public NBTWrapper createNBTWrapper(org.bukkit.inventory.ItemStack item) {
        return new NBTWrapper1_16_R3(item);
    }

    @Override
    public void addPacketInjector(Player p) {
        EntityPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.playerConnection.networkManager.channel;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return;

        ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, new PacketHandler1_16_R3(p));
    }

    @Override
    public void removePacketInjector(Player p) {
        EntityPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.playerConnection.networkManager.channel;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) == null) return;
        ch.pipeline().remove(PACKET_INJECTOR_ID);
    }

    @Override
    public void sendSign(Player p, Consumer<String[]> lines) {
        addPacketInjector(p);

        Location l = p.getLocation();
        BlockPosition pos = new BlockPosition(l.getBlockX(), 255, l.getBlockZ());

        PacketPlayOutBlockChange sent1 = new PacketPlayOutBlockChange(pos, Blocks.OAK_SIGN.getBlockData());
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent1);

        PacketPlayOutOpenSignEditor sent2 = new PacketPlayOutOpenSignEditor(pos);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent2);

        PacketHandler1_16_R3.PACKET_HANDLERS.put(p.getUniqueId(), packetO -> {
            if (!(packetO instanceof PacketPlayInUpdateSign)) return false;
            PacketPlayInUpdateSign packet = (PacketPlayInUpdateSign) packetO;

            lines.accept(packet.c());
            return true;
        });

        scheduler.syncLater(() -> {
            PacketPlayOutBlockChange sent3 = new PacketPlayOutBlockChange(pos, Blocks.AIR.getBlockData());
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent3);
        }, 2L);
    }

}
