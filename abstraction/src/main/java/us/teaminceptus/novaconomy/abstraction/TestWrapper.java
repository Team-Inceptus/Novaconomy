package us.teaminceptus.novaconomy.abstraction;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TestWrapper implements Wrapper {

    @Override
    public int getCommandVersion() {
        return 0;
    }

    private final Map<ItemStack, Map<String, Object>> nbtMap = new HashMap<>();

    @Override
    public void sendActionbar(Player p, String message) {}

    @Override
    public void sendActionbar(Player p, BaseComponent component) {}

    @Override
    public String getNBTString(ItemStack item, String key) {
        return nbtMap.get(item).get(key).toString();
    }

    @Override
    public ItemStack setNBT(ItemStack item, String key, String value) {
        Map<String, Object> map = nbtMap.getOrDefault(item, new HashMap<>());
        map.put(key, value);
        nbtMap.put(item, map);
        return item;
    }

    @Override
    public ItemStack setNBT(ItemStack item, String key, ItemStack value) {
        Map<String, Object> map = nbtMap.getOrDefault(item, new HashMap<>());
        map.put(key, value);
        nbtMap.put(item, map);
        return item;
    }

    @Override
    public ItemStack getNBTItem(ItemStack item, String key) {
        return (ItemStack) nbtMap.get(item).get(key);
    }

    @Override
    public ItemStack getGUIBackground() {
        return new ItemStack(Material.AIR);
    }

    @Override
    public ItemStack createSkull(OfflinePlayer p) {
        return new ItemStack(Material.AIR);
    }

    @Override
    public double getNBTDouble(ItemStack item, String key) {
        return (double) nbtMap.get(item).get(key);
    }

    @Override
    public ItemStack setNBT(ItemStack item, String key, double value) {
        Map<String, Object> map = nbtMap.getOrDefault(item, new HashMap<>());
        map.put(key, value);
        nbtMap.put(item, map);
        return item;
    }

    @Override
    public ItemStack setNBT(ItemStack item, String key, boolean value) {
        Map<String, Object> map = nbtMap.getOrDefault(item, new HashMap<>());
        map.put(key, value);
        nbtMap.put(item, map);
        return item;
    }

    @Override
    public boolean getNBTBoolean(ItemStack item, String key) {
        return (boolean) nbtMap.get(item).get(key);
    }

    @Override
    public ItemStack normalize(ItemStack item) {
        return item;
    }

    @Override
    public boolean isAgeable(Block b) {
        return false;
    }

    @Override
    public void removeItem(PlayerInteractEvent p) {}

    @Override
    public boolean isCrop(Material m) {
        return false;
    }

    @Override
    public List<Material> getCrops() {
        return new ArrayList<>();
    }

}
