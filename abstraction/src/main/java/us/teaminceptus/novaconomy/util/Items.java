package us.teaminceptus.novaconomy.util;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class Items {

    public static ItemStack builder(Material m, int amount, Consumer<ItemMeta> metaC) {
        ItemStack item = new ItemStack(m, amount);
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        metaC.accept(meta);
        item.setItemMeta(meta);
        
        return item;
    }

    public static ItemStack builder(Material m, Consumer<ItemMeta> metaC) {
        return builder(m, 1, metaC);
    }

}
