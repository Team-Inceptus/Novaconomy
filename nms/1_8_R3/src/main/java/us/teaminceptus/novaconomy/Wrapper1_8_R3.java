package us.teaminceptus.novaconomy;

import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_8_R3.*;

import java.util.Arrays;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Crops;

import io.netty.channel.Channel;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.v1_8_R3.NBTWrapper1_8_R3;
import us.teaminceptus.novaconomy.v1_8_R3.NovaInventory1_8_R3;
import us.teaminceptus.novaconomy.v1_8_R3.PacketHandler1_8_R3;

public final class Wrapper1_8_R3 implements Wrapper {

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
    public ItemStack getGUIBackground() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public ItemStack createSkull(OfflinePlayer p) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(p.getName());
        item.setItemMeta(meta);

        return item;
    }

    @Override
    public ItemStack normalize(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();

        tag.remove("id");
        tag.remove("Count");
        nmsitem.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsitem);
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
        return new NovaInventory1_8_R3(id, name, size);
    }

    @Override
    public NBTWrapper createNBTWrapper(org.bukkit.inventory.ItemStack item) {
        return new NBTWrapper1_8_R3(item);
    }

    @Override
    public void addPacketInjector(Player p) {
        EntityPlayer sp = ((CraftPlayer) p).getHandle();
        Channel ch = sp.playerConnection.networkManager.channel;

        if (ch.pipeline().get(PACKET_INJECTOR_ID) != null) return;

        ch.pipeline().addBefore("packet_handler", PACKET_INJECTOR_ID, new PacketHandler1_8_R3(p));
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
        PacketHandler1_8_R3.PACKET_HANDLERS.put(p.getUniqueId(), packetO -> {
            if (!(packetO instanceof PacketPlayInUpdateSign)) return;
            PacketPlayInUpdateSign packet = (PacketPlayInUpdateSign) packetO;

            lines.accept(Arrays.stream(packet.b())
                    .map(IChatBaseComponent::getText)
                    .toArray(String[]::new));
        });
    }

}
