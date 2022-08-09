package us.teaminceptus.novaconomy;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.Crops;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Wrapper1_12_R1 implements Wrapper {

    @Override
    public void sendActionbar(Player p, String message) {
        sendActionbar(p, new TextComponent(message));
    }

    @Override
    public void sendActionbar(Player p, BaseComponent component) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
    }

    @Override
    public String getNBTString(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getString(key);
    }

    @Override
    public org.bukkit.inventory.ItemStack setNBT(org.bukkit.inventory.ItemStack item, String key, String value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setString(key, value);
        tag.set(ROOT, novaconomy);
        return CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public org.bukkit.inventory.ItemStack getGUIBackground() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.STAINED_GLASS_PANE, 1, (short)15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public org.bukkit.inventory.ItemStack createSkull(OfflinePlayer p) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(p.getName());
        item.setItemMeta(meta);

        return item;
    }

    private Object getData(NBTBase b) {
        switch (b.getTypeId()) {
            case 1: return ((NBTTagByte) b).f();
            case 2: return ((NBTTagShort) b).e();
            case 3: return ((NBTTagInt) b).d();
            case 4: return ((NBTTagLong) b).c();
            case 5: return ((NBTTagFloat) b).i();
            case 6: return ((NBTTagDouble) b).g();
            case 7: return ((NBTTagByteArray) b).c();
            case 8: return ((NBTTagString) b).c_();
            case 9: {
                List<Object> l = new ArrayList<>();

                NBTTagList list = (NBTTagList) b;
                for (int i = 0; i < list.size(); i++) l.add(getData(list.i(i)));
                return l;
            }
            case 10: {
                NBTTagCompound c = (NBTTagCompound) b;
                Map<String, Object> map = new HashMap<>();

                c.c().forEach(s -> map.put(s, getData(c.get(s))));
                return map;
            }
            case 11: return ((NBTTagIntArray) b).d();
            case 12: {
                try {
                    Field f = NBTTagLongArray.class.getDeclaredField("b");
                    f.setAccessible(true);

                    return f.get(b);
                } catch (Exception e) {
                    NovaConfig.getLogger().severe(e.getMessage());
                    return null;
                }
            }

            default: return null;
        }
    }

    @Override
    public org.bukkit.inventory.ItemStack setNBT(org.bukkit.inventory.ItemStack item, String key, org.bukkit.inventory.ItemStack value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        ItemStack nmsvalue = CraftItemStack.asNMSCopy(value);
        novaconomy.set(key, nmsvalue.save(nmsvalue.hasTag() ? nmsvalue.getTag() : new NBTTagCompound()));
        tag.set(ROOT, novaconomy);
        return CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public org.bukkit.inventory.ItemStack getNBTItem(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        NBTTagCompound nbt = novaconomy.getCompound(key);
        return CraftItemStack.asBukkitCopy(new ItemStack(nbt));
    }

    @Override
    public double getNBTDouble(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getDouble(key);
    }

    @Override
    public org.bukkit.inventory.ItemStack setNBT(org.bukkit.inventory.ItemStack item, String key, double value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setDouble(key, value);
        tag.set(ROOT, novaconomy);
        return CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public org.bukkit.inventory.ItemStack setNBT(org.bukkit.inventory.ItemStack item, String key, boolean value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setBoolean(key, value);
        tag.set(ROOT, novaconomy);
        return CraftItemStack.asBukkitCopy(nmsitem);
    }

    @Override
    public boolean getNBTBoolean(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getBoolean(key);
    }

    @Override
    public org.bukkit.inventory.ItemStack normalize(org.bukkit.inventory.ItemStack item) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
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
        switch (e.getHand()) {
            case OFF_HAND: e.getPlayer().getEquipment().setItemInOffHand(null);
            case HAND: e.getPlayer().getEquipment().setItemInMainHand(null);
        }
    }
}
