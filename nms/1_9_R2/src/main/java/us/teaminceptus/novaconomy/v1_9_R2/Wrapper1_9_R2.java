package us.teaminceptus.novaconomy.v1_9_R2;

import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_9_R2.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Crops;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.function.Consumer;

import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;

final class Wrapper1_9_R2 implements Wrapper {

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
        return b.getState().getData() instanceof Crops;
    }

    @Override
    public void removeItem(PlayerInteractEvent e) {
        PlayerInventory inv = e.getPlayer().getInventory();
        switch (e.getHand()) {
            case HEAD: inv.setHelmet(null); break;
            case CHEST: inv.setChestplate(null); break;
            case LEGS: inv.setLeggings(null); break;
            case FEET: inv.setBoots(null); break;
            case HAND: inv.setItemInMainHand(null); break;
            case OFF_HAND: inv.setItemInOffHand(null); break;
        }
    }

    @Override
    public boolean isCrop(Material m) {
        return Crops.class.isAssignableFrom(m.getData());
    }

    @Override
    public NovaInventory createInventory(String id, String name, int size) {
        return new NovaInventory1_9_R2(id, name, size);
    }

    @Override
    public NBTWrapper createNBTWrapper(org.bukkit.inventory.ItemStack item) {
        return new NBTWrapper1_9_R2(item);
    }

    @Override
    public void addPacketInjector(Player p) {
        EntityPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.playerConnection.networkManager.channel;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return;

        ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, new PacketHandler1_9_R2(p));
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
        WorldServer ws = ((CraftWorld) l.getWorld()).getHandle();
        BlockPosition pos = new BlockPosition(l.getBlockX(), 255, l.getBlockZ());

        PacketPlayOutBlockChange sent1 = new PacketPlayOutBlockChange(ws, pos);
        sent1.block = Blocks.STANDING_SIGN.getBlockData();

        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent1);

        PacketPlayOutOpenSignEditor sent2 = new PacketPlayOutOpenSignEditor(pos);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent2);

        PacketHandler1_9_R2.PACKET_HANDLERS.put(p.getUniqueId(), packetO -> {
            if (!(packetO instanceof PacketPlayInUpdateSign)) return false;
            PacketPlayInUpdateSign packet = (PacketPlayInUpdateSign) packetO;

            lines.accept(packet.b());
            return true;
        });

        scheduler.syncLater(() -> {
            PacketPlayOutBlockChange sent3 = new PacketPlayOutBlockChange(ws, pos);
            sent3.block = Blocks.AIR.getBlockData();

            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent3);
        }, 2L);
    }

}
