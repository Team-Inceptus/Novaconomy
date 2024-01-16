package us.teaminceptus.novaconomy.abstraction;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.test.TestWrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Wrapper {

    String ROOT = "Novaconomy";

    String PACKET_INJECTOR_ID = "novaconomy:packet_injector";

    String USER_AGENT = "Team-Inceptus/Novaconomy Java 8 Minecraft Plugin";

    SecureRandom r = new SecureRandom();

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    Wrapper w = getWrapper();

    boolean isAgeable(Block b);

    void removeItem(PlayerInteractEvent p);

    boolean isCrop(Material m);

    NovaInventory createInventory(String id, String name, int size);

    NBTWrapper createNBTWrapper(ItemStack item);

    void addPacketInjector(Player p);

    void removePacketInjector(Player p);

    void sendSign(Player p, Consumer<String[]> lines);

    // Defaults

    default List<Material> getCrops() {
        return Arrays.stream(Material.values()).filter(this::isCrop).collect(Collectors.toList());
    }

    default boolean isItem(Material m) {
        if (m == null) return false;
        if (getCommandVersion() == 2 && m.name().startsWith("LEGACY_")) return false;

        try {
            Method isItem = Material.class.getDeclaredMethod("isItem");
            isItem.setAccessible(true);
            return (boolean) isItem.invoke(m);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            NovaConfig.print(e);
        } catch (ReflectiveOperationException ignored) {}
        return true;
    }

    // Util

    static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
    }

    static Wrapper getWrapper() {
        try {
            Constructor<? extends Wrapper> constr = Class.forName("us.teaminceptus.novaconomy.v" + getServerVersion() + ".Wrapper" + getServerVersion())
                    .asSubclass(Wrapper.class)
                    .getDeclaredConstructor();
            constr.setAccessible(true);
            return constr.newInstance();
        } catch (IndexOutOfBoundsException e) { // using test configuration
            return new TestWrapper();
        } catch (Exception e) {
            throw new IllegalStateException("Wrapper not Found: " + getServerVersion());
        }
    }

    default boolean isLegacy() {
        return getCommandVersion() == 1;
    }

}
