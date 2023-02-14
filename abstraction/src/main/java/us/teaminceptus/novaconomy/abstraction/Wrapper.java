package us.teaminceptus.novaconomy.abstraction;

import com.google.gson.Gson;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.test.TestWrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Wrapper {

    String ROOT = "Novaconomy";

    String PACKET_INJECTOR_ID = "novaconomy:packet_injector";

    SecureRandom r = new SecureRandom();

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    ItemStack createSkull(OfflinePlayer p);

    Wrapper w = getWrapper();

    default boolean isItem(Material m) {
        try {
            Method isItem = Material.class.getDeclaredMethod("isItem");
            isItem.setAccessible(true);
            return (boolean) isItem.invoke(m);
        } catch (NoSuchMethodException | InvocationTargetException e) {
            NovaConfig.print(e);
        } catch (ReflectiveOperationException ignored) {}
        return true;
    }

    boolean isAgeable(Block b);

    void removeItem(PlayerInteractEvent p);

    boolean isCrop(Material m);

    ItemStack normalize(ItemStack item);

    NovaInventory createInventory(String id, String name, int size);

    NBTWrapper createNBTWrapper(ItemStack item);

    void addPacketInjector(Player p);

    void removePacketInjector(Player p);

    void sendSign(Player p, Consumer<String[]> lines);

    // Defaults

    default List<Material> getCrops() {
        return Arrays.stream(Material.values()).filter(this::isCrop).collect(Collectors.toList());
    }

    // Util

    static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
    }

    static Wrapper getWrapper() {
        try {
            return (Wrapper) Class.forName("us.teaminceptus.novaconomy.Wrapper" + getServerVersion()).getConstructor().newInstance();
        } catch (IndexOutOfBoundsException e) { // using test configuration
            return new TestWrapper();
        } catch (Exception e) {
            throw new IllegalStateException("Wrapper not Found: " + getServerVersion());
        }
    }

    static String get(String key) {
        return Language.getCurrentMessage(key);
    }

    static String getMessage(String key) { return get("plugin.prefix") + get(key); }

    static String getError(String key) { return get("plugin.prefix") + ChatColor.RED + get(key); }

    static String getSuccess(String key) { return get("plugin.prefix") + ChatColor.GREEN + get(key); }

    static UUID untrimUUID(String old) {
        String p1 = old.substring(0, 8);
        String p2 = old.substring(8, 12);
        String p3 = old.substring(12, 16);
        String p4 = old.substring(16, 20);
        String p5 = old.substring(20, 32);

        String newUUID = p1 + "-" + p2 + "-" + p3 + "-" + p4 + "-" + p5;

        return UUID.fromString(newUUID);
    }

    static String getID(ItemStack item) {
        return NBTWrapper.getID(item);
    }

    static boolean hasID(ItemStack item) {
        return NBTWrapper.hasID(item);
    }
    

    static OfflinePlayer getPlayer(String name) {
        if (Bukkit.getOnlineMode()) try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Java 8 Novaconomy Plugin");
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String inputLine;
                while ((inputLine = input.readLine()) != null) builder.append(inputLine);

                Gson g = new Gson();
                return Bukkit.getOfflinePlayer(untrimUUID(g.fromJson(builder.toString(), APIPlayer.class).id));
            }

        } catch (IOException e) {
            NovaConfig.print(e);
        } catch (Exception e) {
            return null;
        }
        else return Bukkit.getPlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
        return null;
    }

    default boolean isLegacy() {
        return getCommandVersion() == 1;
    }

    class APIPlayer {

        public final String name;
        public final String id;

        public APIPlayer(String name, String id) {
            this.name = name;
            this.id = id;
        }

    }
}
