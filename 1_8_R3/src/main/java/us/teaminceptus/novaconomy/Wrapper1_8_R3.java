package us.teaminceptus.novaconomy;

import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import us.teaminceptus.novaconomy.abstraction.Wrapper;

import java.lang.reflect.Method;
import java.util.*;

public final class Wrapper1_8_R3 implements Wrapper {
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
    public String getNBTString(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getString(key);
    }

    @Override
    public void setNBT(org.bukkit.inventory.ItemStack item, String key, String value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setString(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
    }

    @Override
    public void openBook(Player p, org.bukkit.inventory.ItemStack book) {
        ((CraftPlayer) p).getHandle().openBook(CraftItemStack.asNMSCopy(book));
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
        return item;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends ConfigurationSerializable> T getNBTSerializable(org.bukkit.inventory.ItemStack item, String key, Class<T> clazz) {
        try {
            ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
            NBTTagCompound novaconomy = tag.getCompound(ROOT);

            Map map = (Map) getData(novaconomy);

            Method m = clazz.getDeclaredMethod("deserialize", Map.class);
            return clazz.cast(m.invoke(null, map));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object getData(NBTBase b) {
        switch (b.getTypeId()) {
            case 1: return ((NBTTagByte) b).f();
            case 2: return ((NBTTagShort) b).e();
            case 3: return ((NBTTagInt) b).d();
            case 4: return ((NBTTagLong) b).c();
            case 5: return ((NBTTagFloat) b).h();
            case 6: return ((NBTTagDouble) b).g();
            case 7: return ((NBTTagByteArray) b).c();
            case 8: return ((NBTTagString) b).a_();
            case 9: {
                List l = new ArrayList<>();

                NBTTagList list = (NBTTagList) b;
                for (int i = 0; i < list.size(); i++) l.add(getData(list.g(i)));
                return l;
            }
            case 10: {
                NBTTagCompound c = (NBTTagCompound) b;
                Map<String, Object> map = new HashMap<>();

                c.c().forEach(s -> map.put(s, getData(c.get(s))));
                return map;
            }
            case 11: return ((NBTTagIntArray) b).c();

            default: return null;
        }
    }

    @Override
    public void setNBT(org.bukkit.inventory.ItemStack item, String key, ConfigurationSerializable serializable) {
        try {
            ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
            NBTTagCompound novaconomy = tag.getCompound(ROOT);

            NBTTagCompound cmp = MojangsonParser.parse(serializable.serialize().toString());
            novaconomy.set(key, cmp);
            tag.set(ROOT, novaconomy);
            nmsitem.setTag(tag);
        } catch (MojangsonParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setNBT(org.bukkit.inventory.ItemStack item, String key, org.bukkit.inventory.ItemStack value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.set(key, CraftItemStack.asNMSCopy(value).getTag());
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
    }

    @Override
    public org.bukkit.inventory.ItemStack getNBTItem(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        NBTTagCompound nbt = novaconomy.getCompound(key);
        return CraftItemStack.asBukkitCopy(ItemStack.createStack(nbt));
    }

    @Override
    public double getNBTDouble(org.bukkit.inventory.ItemStack item, String key) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        return novaconomy.getDouble(key);
    }

    @Override
    public void setNBT(org.bukkit.inventory.ItemStack item, String key, double value) {
        ItemStack nmsitem = CraftItemStack.asNMSCopy(item);
        NBTTagCompound tag = nmsitem.hasTag() ? nmsitem.getTag() : new NBTTagCompound();
        NBTTagCompound novaconomy = tag.getCompound(ROOT);

        novaconomy.setDouble(key, value);
        tag.set(ROOT, novaconomy);
        nmsitem.setTag(tag);
    }
}
