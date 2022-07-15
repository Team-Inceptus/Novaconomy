package us.teaminceptus.novaconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.List;

import static us.teaminceptus.novaconomy.Novaconomy.get;

final class Items {

    private static final String REMOVE_STRS = "!,.?/\\[]{}()*&^%$#@-=+";

    public static ItemStack yes(String id) {
        return itemBuilder(limeWool()).setName(ChatColor.GREEN + get("constants.yes")).setId("yes:" + id).build();
    }

    public static ItemStack cancel(String id) {
        return itemBuilder(redWool()).setName(ChatColor.RED + get("constants.cancel")).setId("no:" + id).build();
    }

    private static ItemStack limeWool() {
        if (Novaconomy.isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 5);
        else return new ItemStack(Material.matchMaterial("LIME_WOOL"));
    }

    private static ItemStack redWool() {
        if (Novaconomy.isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 14);
        else return new ItemStack(Material.matchMaterial("RED_WOOL"));
    }

    public static ItemStack cancel() {
        return cancel("close");
    }

    public static Builder itemBuilder(Material type) {
        return new Builder(type);
    }

    public static Builder itemBuilder(ItemStack starter) {
        return new Builder(starter);
    }

    public static Builder itemBuilder(Material type, int data) {
        return new Builder(type, data);
    }

    public static class Builder {

        ItemStack item;

        Builder(Material type) {
            this.item = new ItemStack(type);
        }

        Builder(Material type, int data) {
            this.item = new ItemStack(type, 1, (short) data);
        }

        Builder(ItemStack starter) {
            this.item = starter.clone();
        }

        public Builder setName(String display) {
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(display);
            String localizedName = ChatColor.stripColor(display).toLowerCase().replace(' ', '_');
            for (Character c : REMOVE_STRS.toCharArray()) localizedName.replace(c.toString(), "");

            getWrapper().setID(item, localizedName);
            item.setItemMeta(meta);
            return this;
        }

        public Builder setType(Material type) {
            item.setType(type);
            return this;
        }

        public Builder setId(String id) {
            item = getWrapper().setID(item,  id);
            return this;
        }

        public Builder setLore(List<String> lore) {
            ItemMeta meta = item.getItemMeta();
            meta.setLore(lore);
            item.setItemMeta(meta);
            return this;
        }

        public Builder setNBT(String key, String value) {
            item = getWrapper().setNBT(item, key, value);
            return this;
        }

        public Builder addFlags(ItemFlag... flags) {
            ItemMeta meta = item.getItemMeta();
            meta.addItemFlags(flags);
            item.setItemMeta(meta);
            return this;
        }

        public Builder addGlint() {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            return this;
        }

        public ItemStack build() {
            return this.item;
        }

    }

    private static String getServerVersion() {
        return  Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
    }

    private static Wrapper getWrapper() {
        try {
            return (Wrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".Wrapper" + getServerVersion()).getConstructor().newInstance();
        } catch (Exception e) {
            NovaConfig.getLogger().severe(e.getMessage());
            return null;
        }
    }

}
