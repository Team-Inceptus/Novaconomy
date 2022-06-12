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
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface Wrapper {

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    String getNBTString(ItemStack item, String key);

    void setNBTString(ItemStack item, String key, String value);

    void openBook(Player p, ItemStack item);

    ItemStack getGUIBackground();

    ItemStack createSkull(OfflinePlayer p);

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

        setNBTString(item, "economy", econ.getUniqueId().toString());
        setNBTString(item, "amount", amount + "");

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

        AtomicInteger slot = new AtomicInteger(19);
        Map<ItemStack, List<Price>> products = new HashMap<>();
        List<Product> bProducts = b.getProducts();

        b.getProducts().forEach(p -> {
            ItemStack item = p.getItem();
            Price price = p.getPrice();
            if (products.containsKey(item)) {
                List<Price> prices = products.get(item);
                prices.add(price);
                products.put(item, prices);
            } else products.put(item, new ArrayList<Price>() {{ add(price); }});
        });

        bProducts.forEach(p -> {
            if (slot.get() == 26) slot.set(28);
            if (slot.get() == 35) slot.set(37);
            if (slot.get() >= 44) return;

            ItemStack product = p.getItem().clone();
            ItemMeta meta = product.getItemMeta();

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(" ");

            AtomicInteger i = new AtomicInteger(0);
            products.get(p.getItem()).forEach(pr -> {
                lore.add(String.format(getMessage("constants.business.price"), i.get(), pr.getAmount(), p.getEconomy().getSymbol() ));
                i.incrementAndGet();
            });

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

}
