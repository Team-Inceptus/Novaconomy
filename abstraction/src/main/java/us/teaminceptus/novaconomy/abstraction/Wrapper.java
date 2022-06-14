package us.teaminceptus.novaconomy.abstraction;

import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public interface Wrapper {

    String ROOT = "Novaconomy";

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    String getNBTString(ItemStack item, String key);

    void setNBT(ItemStack item, String key, String value);

    void setNBT(ItemStack item, String key, ItemStack value);

    ItemStack getNBTItem(ItemStack item, String key);

    void openBook(Player p, ItemStack item);

    ItemStack getGUIBackground();

    ItemStack createSkull(OfflinePlayer p);

    <T extends ConfigurationSerializable> T getNBTSerializable(ItemStack item, String key, Class<T> clazz);

    void setNBT(ItemStack item, String key, ConfigurationSerializable serializable);

    double getNBTDouble(ItemStack item, String key);

    void setNBT(ItemStack item, String key, double value);

    static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Novaconomy");
    }

    // Util

    default ItemStack createCheck(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        Validate.notNull(econ, "Economy cannot be null");
        Validate.isTrue(amount > 0, "Amount must be greater than 0");

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("" + ChatColor.YELLOW + amount + econ.getSymbol());
        meta.setLore(Collections.singletonList(ChatColor.GOLD + "" + amount + " " + econ.getName() + "(s)"));

        setNBT(item, "economy", econ.getUniqueId().toString());
        setNBT(item, "amount", amount + "");

        item.setItemMeta(meta);
        return item;
    }

    default Inventory generateBusinessData(Business b) {
        Inventory inv = genGUI(54, ChatColor.GOLD + b.getName());

        ItemStack owner = createSkull(b.getOwner());
        ItemMeta oMeta = owner.getItemMeta();
        oMeta.setDisplayName(String.format(get("constants.business.owner"), b.getOwner().getName()));
        owner.setItemMeta(oMeta);
        inv.setItem(12, owner);

        ItemStack icon = new ItemStack(b.getIcon());
        ItemMeta iMeta = icon.getItemMeta();
        iMeta.setDisplayName(ChatColor.GOLD + b.getName());
        iMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + b.getUniqueId()));
        icon.setItemMeta(iMeta);
        inv.setItem(14, icon);

        AtomicInteger slot = new AtomicInteger(28);
        List<BusinessProduct> bProducts = b.getProducts();
        Map<ItemStack, List<Price>> products = new HashMap<>();

        bProducts.forEach(p -> {
            if (slot.get() == 35) slot.set(37);
            if (slot.get() == 44) slot.set(46);
            if (slot.get() >= 53) return;

            ItemStack item = p.getItem().clone();
            Price price = p.getPrice();

            if (products.containsKey(item)) {
                List<Price> prices = new ArrayList<>(products.get(item));
                prices.add(price);
                products.put(item, prices);
            } else products.put(item, new ArrayList<Price>() {{ add(price); }});

            ItemStack product = item.clone();

            setNBT(product, "product", p);
            setNBT(product, "id", "product:buy");

            ItemMeta meta = product.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(" ");

            AtomicInteger i = new AtomicInteger(0);
            products.get(item).forEach(pr ->
                lore.add(String.format(get("constants.business.price"), i.incrementAndGet(), pr.getAmount(), p.getEconomy().getSymbol() ))
            );

            meta.setLore(lore);
            product.setItemMeta(meta);

            inv.setItem(slot.get(), product);
            slot.incrementAndGet();
        });

        return inv;
    }

    default Inventory genGUI(int size, String name) {
        return genGUI(size, name, null);
    }

    default Inventory genGUI(int size, String name, InventoryHolder holder) {
        if (size < 9 || size > 54) return null;
        if (size % 9 > 0) return null;

        Inventory inv = Bukkit.createInventory(holder, size, name);
        ItemStack guiBG = getGUIBackground();

        if (size < 27) return inv;

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, guiBG);
        }

        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, guiBG);
        }

        if (size >= 27) {
            inv.setItem(9, guiBG);
            inv.setItem(17, guiBG);
        }

        if (size >= 36) {
            inv.setItem(18, guiBG);
            inv.setItem(26, guiBG);
        }

        if (size >= 45) {
            inv.setItem(27, guiBG);
            inv.setItem(35, guiBG);
        }

        if (size == 54) {
            inv.setItem(36, guiBG);
            inv.setItem(44, guiBG);
        }

        return inv;
    }

    static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
    }

    static Wrapper getWrapper() {
        try {
            return (Wrapper) Class.forName("us.teaminceptus.novaconomy.Wrapper" + getServerVersion()).newInstance();
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    static String get(String key) {
        String lang = NovaConfig.getConfiguration().getLanguage();
        return Language.getById(lang).getMessage(key);
    }

    static String getMessage(String key) { return get("plugin.prefix") + get(key); }

    class CancelHolder implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

}
