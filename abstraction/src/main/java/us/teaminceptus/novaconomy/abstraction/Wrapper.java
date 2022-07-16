package us.teaminceptus.novaconomy.abstraction;

import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public interface Wrapper {

    String ROOT = "Novaconomy";

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    String getNBTString(ItemStack item, String key);

    ItemStack setNBT(ItemStack item, String key, String value);

    ItemStack setNBT(ItemStack item, String key, ItemStack value);

    ItemStack getNBTItem(ItemStack item, String key);

    void openBook(Player p, ItemStack item);

    ItemStack getGUIBackground();

    ItemStack createSkull(OfflinePlayer p);
    
    default ItemStack setID(ItemStack item, String id) {
        return setNBT(item, "id", id);
    }

    double getNBTDouble(ItemStack item, String key);

    ItemStack setNBT(ItemStack item, String key, double value);

    ItemStack setNBT(ItemStack item, String key, boolean value);

    boolean getNBTBoolean(ItemStack item, String key);

    ItemStack normalize(ItemStack item);

    default boolean hasID(ItemStack item) { return getID(item) != null && getID(item).length() > 0; }

    default String getID(ItemStack item) { return getNBTString(item, "id"); }

    static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Novaconomy");
    }

    default ItemStack setNBT(ItemStack item, String key, Product p) {
        ItemStack newItem = setNBT(item, key + "-product:amount", p.getPrice().getAmount());
        newItem = setNBT(newItem, key + "-product:economy", p.getEconomy().getUniqueId().toString());
        newItem = setNBT(newItem, key + "-product:item", normalize(p.getItem()));

        if (p instanceof BusinessProduct) {
            BusinessProduct bp = (BusinessProduct) p;
            newItem = setNBT(newItem, key + "-bproduct:business", bp.getBusiness().getUniqueId().toString());
        }

        return newItem;
    }

    default Product getNBTProduct(ItemStack item, String key) {
        double amount = getNBTDouble(item, key + "-product:amount");
        Economy econ = Economy.getEconomy(UUID.fromString(getNBTString(item, key + "-product:economy")));
        ItemStack product = normalize(getNBTItem(item, key + "-product:item"));

        Product p = new Product(product, econ, amount);
        try {
            UUID business = UUID.fromString(getNBTString(item, key + "-bproduct:business"));

            if (Business.exists(business)) return new BusinessProduct(p, Business.getById(business));
            else return p;
        } catch (IllegalArgumentException e) {
            return p;
        }
    }

    // Util

    default ItemStack createCheck(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        Validate.notNull(econ, "Economy cannot be null");
        Validate.isTrue(amount > 0, "Amount must be greater than 0");

        ItemStack item = new ItemStack(Material.PAPER);
        item = setID(item, "economy:check");
        item = setNBT(item, "economy", econ.getUniqueId().toString());
        item = setNBT(item, "amount", amount);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("" + ChatColor.YELLOW + amount + econ.getSymbol());
        meta.setLore(Collections.singletonList(ChatColor.GOLD + "" + amount + " " + econ.getName() + "(s)"));
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
        iMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "ID: " + b.getUniqueId()));
        icon.setItemMeta(iMeta);
        inv.setItem(14, icon);

        AtomicInteger slot = new AtomicInteger(19);
        List<BusinessProduct> bProducts = b.getProducts();

        for (int i = 46; i < 53; i++) inv.setItem(i, null);

        bProducts.forEach(p -> {
            if (slot.get() == 26) slot.set(28);
            if (slot.get() == 35) slot.set(37);
            if (slot.get() == 44) slot.set(46);
            if (slot.get() >= 53) return;

            ItemStack item = p.getItem().clone();
            if (item.getType() == Material.AIR) return;

            ItemStack product = item.clone();

            product = setNBT(product, "product", p);
            product = setID(product, "product:buy");

            ItemMeta meta = product.hasItemMeta() ? product.getItemMeta() : Bukkit.getItemFactory().getItemMeta(product.getType());

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(" ");

            AtomicInteger i = new AtomicInteger(0);
            lore.add(String.format(get("constants.business.price"), String.format("%,.2f",p.getPrice().getAmount()).replace("D", ""), p.getEconomy().getSymbol() + ""));

            boolean stock = true;

            lore.add(" ");
            if (!b.isInStock(item)) {
                lore.add(ChatColor.RED + get("constants.business.no_stock"));
                stock = false;
            } else {
                AtomicInteger index = new AtomicInteger(0);
                b.getResources().forEach(res -> {
                    if (item.isSimilar(res)) index.getAndAdd(res.getAmount());
                });

                lore.add(String.format(get("constants.business.stock_left"), index.get()));
            }

            meta.setLore(lore);
            product.setItemMeta(meta);

            product = setNBT(product, "product:in_stock", stock);

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

        for (int i = 0; i < 9; i++) inv.setItem(i, guiBG);

        for (int i = size - 9; i < size; i++) inv.setItem(i, guiBG);

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
            return (Wrapper) Class.forName("us.teaminceptus.novaconomy.Wrapper" + getServerVersion()).getConstructor().newInstance();
        } catch (Exception e) { throw new IllegalStateException("Wrapper not Found: " + getServerVersion()); }
    }

    static String get(String key) {
        String lang = NovaConfig.getConfiguration().getLanguage();
        return Language.getById(lang).getMessage(key);
    }

    static String getMessage(String key) { return get("plugin.prefix") + get(key); }

    default boolean isLegacy() {
        return getCommandVersion() == 1;
    }

    class CancelHolder implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

}
