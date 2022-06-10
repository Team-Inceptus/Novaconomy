package us.teaminceptus.novaconomy;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import us.teaminceptus.novaconomy.abstraction.Wrapper;

public final class Wrapper1_16_R3 implements Wrapper {

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

}
