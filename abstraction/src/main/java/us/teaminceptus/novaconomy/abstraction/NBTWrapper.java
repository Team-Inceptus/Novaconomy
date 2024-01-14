package us.teaminceptus.novaconomy.abstraction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.UUID;
import java.util.function.Consumer;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.PRODUCT_TAG;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.w;

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
        NBTWrapper w = of(item.clone());
        nbt.accept(w);
        return w.item;
    }

    public static ItemStack builder(@NotNull ItemStack item, Consumer<ItemMeta> metaC, Consumer<NBTWrapper> nbt) {
        ItemStack item0 = item.clone();
        ItemMeta meta = item0.hasItemMeta() ? item0.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item0.getType());
        metaC.accept(meta);
        item0.setItemMeta(meta);
        
        return builder(item0, nbt);
    }

    public static ItemStack builder(@NotNull ItemStack item, int amount, Consumer<ItemMeta> metaC, Consumer<NBTWrapper> nbt) {
        ItemStack item0 = item.clone();
        item0.setAmount(amount);
        return builder(item0, metaC, nbt);
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

    // Default & Abstract

    public ItemStack getItem() {
        return item;
    }

    public abstract String getFullTag();

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

    public abstract Product getProduct(String key);

    public abstract void set(String key, Product product);

    // Defaults

    public boolean isProduct() {
        return getProduct(PRODUCT_TAG) != null;
    }

    public boolean isProduct(String key) {
        return getProduct(key) != null;
    }

    public final void set(String key, Class<?> clazz) {
        set(key, clazz.getName());
    }

    @Nullable
    public final Class<?> getClass(String key) {
        try {
            return Class.forName(getString(key));
        } catch (ClassNotFoundException e) {
            NovaConfig.print(e);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NBTWrapper that = (NBTWrapper) o;
        return item.equals(that.item);
    }

    @Override
    public int hashCode() {
        return item.hashCode();
    }

    public boolean hasID() { return getID() != null && !getID().isEmpty(); }

    public void setID(String key) { set("id", key); }

    public String getID() { return getString("id"); }

    public void removeID() { setID(""); }


}
