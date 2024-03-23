package us.teaminceptus.novaconomy.v1_8_R2;

import io.netty.channel.Channel;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Crops;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.Arrays;
import java.util.function.Consumer;

import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;

final class Wrapper1_8_R2 implements Wrapper {

    @Override
    public int getCommandVersion() { return 2; }

    @Override
    public void sendActionbar(Player p, String message) {
        PacketPlayOutChat packet = new PacketPlayOutChat(new ChatComponentText(message), (byte)2);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }

    @Override
    public void sendActionbar(Player p, BaseComponent component) {
        sendActionbar(p, component.toLegacyText());
    }

    @Override
    public boolean isAgeable(Block b) {
        return b.getState().getData() instanceof Crops;
    }

    @Override
    public void removeItem(PlayerInteractEvent e) {
        e.getPlayer().setItemInHand(null);
    }

    @Override
    public boolean isCrop(Material m) {
        return Crops.class.isAssignableFrom(m.getData());
    }

    @Override
    public NovaInventory createInventory(String id, String name, int size) {
        return new NovaInventory1_8_R2(id, name, size);
    }

    @Override
    public NBTWrapper createNBTWrapper(org.bukkit.inventory.ItemStack item) {
        return new NBTWrapper1_8_R2(item);
    }

    @Override
    public void addPacketInjector(Player p) {
        EntityPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.playerConnection.networkManager.k;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return;

        ch.pipeline().addAfter("decoder", PACKET_INJECTOR_ID, new PacketHandler1_8_R2(p));
    }

    @Override
    public void removePacketInjector(Player p) {
        EntityPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.playerConnection.networkManager.k;

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

        PacketHandler1_8_R2.PACKET_HANDLERS.put(p.getUniqueId(), packetO -> {
            if (!(packetO instanceof PacketPlayInUpdateSign)) return false;
            PacketPlayInUpdateSign packet = (PacketPlayInUpdateSign) packetO;

            lines.accept(Arrays.stream(packet.b())
                    .map(IChatBaseComponent::getText)
                    .toArray(String[]::new));
            return true;
        });

        scheduler.syncLater(() -> {
            PacketPlayOutBlockChange sent3 = new PacketPlayOutBlockChange(ws, pos);
            sent3.block = Blocks.AIR.getBlockData();

            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(sent3);
        }, 2L);
    }

}
