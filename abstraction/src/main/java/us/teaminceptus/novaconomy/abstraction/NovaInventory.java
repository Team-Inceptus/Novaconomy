package us.teaminceptus.novaconomy.abstraction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface NovaInventory extends Inventory {
    
    @Override
    default String getTitle() {
        return getAttribute("_name", String.class);
    }

    default String getId() {
        return getAttribute("_id", String.class);
    }

    Map<String, Object> getAttribtes();

    void setAttribute(String key, Object value);

    default void setCancelled() {
        setAttribute("_cancel", true);
    }

    default boolean isCancelled() {
        return getAttribute("_cancel", Boolean.class, false);
    }

    default Object getAttribute(String key) {
        return getAttribtes().get(key);
    }

    default Object getAttribute(String key, Object def) {
        return getAttribtes().getOrDefault(key, def);
    }

    default <T> T getAttribute(String key, Class<T> type) {
        return type.cast(getAttribute(key));
    }

    default <T> T getAttribute(String key, Class<T> type, T def) {
        return type.cast(getAttribute(key, def));
    }

    // Util

    default void setItem(int a, int b, ItemStack item) {
        setItem(a, item);
        setItem(b, item);
    }

    default void setItem(int a, int b, int c, ItemStack item) {
        setItem(a, item);
        setItem(b, item);
        setItem(c, item);
    }

    default void setItem(int a, int b, int c, int d, ItemStack item) {
        setItem(a, item);
        setItem(b, item);
        setItem(c, item);
        setItem(d, item);
    }

    default void setItem(int a, int b, int c, int d, int e, ItemStack item) {
        setItem(a, item);
        setItem(b, item);
        setItem(c, item);
        setItem(d, item);
        setItem(e, item);
    }

    default void setItem(int a, int b, int c, int d, int e, int f, ItemStack item) {
        setItem(a, item);
        setItem(b, item);
        setItem(c, item);
        setItem(d, item);
        setItem(e, item);
        setItem(f, item);
    }

    default void addItem(int times, ItemStack item) {
        for (int i = 0; i < times; i++) {
            int next = firstEmpty();
            if (next == -1) break;
            setItem(next, item);
        }
    }

}
