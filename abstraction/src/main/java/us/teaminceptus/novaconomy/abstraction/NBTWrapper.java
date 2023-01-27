package us.teaminceptus.novaconomy.abstraction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.w;

import java.util.UUID;
import java.util.function.Consumer;

public abstract class NBTWrapper {
    
    protected ItemStack item;

    public NBTWrapper(ItemStack item) {
        this.item = item;
    }

    // Static Util

    public static NBTWrapper of(ItemStack item) {
        return w.createNBTWrapper(item);
    }

    public static ItemStack builder(ItemStack item, Consumer<NBTWrapper> nbt) {
        NBTWrapper w = of(item);
        nbt.accept(w);
        return w.item;
    }

    public static ItemStack builder(ItemStack item, Consumer<ItemMeta> metaC, Consumer<NBTWrapper> nbt) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        metaC.accept(meta);
        item.setItemMeta(meta);
        
        return builder(item, nbt);
    }

    public static ItemStack builder(Material m, int amount, Consumer<ItemMeta> metaC, Consumer<NBTWrapper> nbt) {
        return builder(new ItemStack(m, amount), metaC, nbt);
    }

    public static ItemStack builder(Material m, Consumer<ItemMeta> metaC, Consumer<NBTWrapper> nbt) {
        return builder(new ItemStack(m), metaC, nbt);
    }

    public static String getID(ItemStack item) {
        return of(item).getID();
    }
    
    public static boolean hasID(ItemStack item) {
        return of(item).hasID();
    }

    // Abstract

    public abstract String getString(String key);

    public abstract void set(String key, String value);

    public abstract int getInt(String key);

    public abstract void set(String key, int value);

    public abstract UUID getUUID(String key);

    public abstract void set(String key, UUID id);

    public abstract double getDouble(String key);

    public abstract void set(String key, double value);

    public abstract boolean getBoolean(String key);

    public abstract void set(String key, boolean value);

    public abstract ItemStack getItem(String key);

    public abstract void set(String key, ItemStack item);

    // Defaults

    public boolean hasID() { return getID() != null && !getID().isEmpty(); }

    public boolean isProduct() {
        return hasID() && (getID().equalsIgnoreCase("product") || getBoolean("is_product")); 
    }

    public void setID(String key) { set("id", key); }

    public String getID() { return getString("id"); }

    public void set(String key, Product p) {
        set(key + "-product:amount", p.getPrice().getAmount());
        set(key + "-product:economy", p.getEconomy().getUniqueId());
        set(key + "-product:item", w.normalize(p.getItem()));

        if (p instanceof BusinessProduct) {
            BusinessProduct bp = (BusinessProduct) p;
            set(key + "-bproduct:business", bp.getBusiness().getUniqueId());
        }
    }

    public Product getProduct(String key) {
        double amount = getDouble(key + "-product:amount");
        Economy econ = Economy.getEconomy(getUUID(key + "-product:economy"));
        ItemStack product = w.normalize(getItem(key + "-product:item"));

        Product p = new Product(product, econ, amount);

        UUID business = getUUID(key + "-bproduct:business");
        if (business == null) return p;

        if (Business.exists(business)) return new BusinessProduct(p, Business.getById(business));
        else return p;
    }


}
