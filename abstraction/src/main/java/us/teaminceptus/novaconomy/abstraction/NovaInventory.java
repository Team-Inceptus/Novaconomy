package us.teaminceptus.novaconomy.abstraction;

import java.util.Map;

import org.bukkit.inventory.Inventory;

public interface NovaInventory extends Inventory {
    
    default String getTitle() {
        return getAttribute("_name", String.class);
    }

    default String getIdentifier() {
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

}
