package us.teaminceptus.novaconomy;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import us.teaminceptus.novaconomy.abstraction.Wrapper;

public final class Wrapper1_18_R1 implements Wrapper {

    @Override
    public int getCommandVersion() { return 2; }

    @Override
    public void sendActionbar(Player p, String message) {
        sendActionbar(p, new TextComponent(message));
    }

    @Override
    public void sendActionbar(Player p, BaseComponent component) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
    }

    @Override
    public String getNBTString(org.bukkit.inventory.ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer c = meta.getPersistentDataContainer();
        return c.get(new NamespacedKey(Wrapper.getPlugin(), key), PersistentDataType.STRING);
    }

    @Override
    public void setNBTString(org.bukkit.inventory.ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer c = meta.getPersistentDataContainer();
        c.set(new NamespacedKey(Wrapper.getPlugin(), key), PersistentDataType.STRING, value);
    }

    @Override
    public void openBook(Player p, ItemStack book) {
        p.openBook(book);
    }

    @Override
    public org.bukkit.inventory.ItemStack getGUIBackground() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public org.bukkit.inventory.ItemStack createSkull(OfflinePlayer p) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(p);

        return item;
    }

}
