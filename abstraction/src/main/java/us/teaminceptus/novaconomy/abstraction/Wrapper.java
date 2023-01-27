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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.ChatPaginator;

import us.teaminceptus.novaconomy.abstraction.test.TestWrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.business.BusinessAdvertiseEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.util.Items;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.*;
import static us.teaminceptus.novaconomy.util.Items.*;

public interface Wrapper {

    String ROOT = "Novaconomy";

    SecureRandom r = new SecureRandom();

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    ItemStack getGUIBackground();

    ItemStack createSkull(OfflinePlayer p);

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

    // Defaults

    default List<Material> getCrops() {
        return Arrays.stream(Material.values()).filter(this::isCrop).collect(Collectors.toList());
    }

    static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Novaconomy");
    }

    // Util

    default ItemStack createCheck(Economy econ, double amount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        return builder(Material.PAPER,
                meta -> {
                    meta.setDisplayName("" + ChatColor.YELLOW + amount + econ.getSymbol());
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + "" + amount + " " + econ.getName() + "(s)"));
                }, nbt -> {
                    nbt.setID("economy:check");
                    nbt.set("economy", econ.getUniqueId());
                    nbt.set("amount", amount);
                });
    }

    default NovaInventory generateBusinessData(Business b, Player viewer, boolean advertising) {
        NovaInventory inv = genGUI(54, ChatColor.GOLD + b.getName());
        inv.setCancelled();

        if (!b.getRatings().isEmpty()) inv.setItem(44, CommandWrapper.loading());
        for (int i = 46; i < 53; i++) inv.setItem(i, null);

        ItemStack icon = new ItemStack(b.getIcon());
        ItemMeta iMeta = icon.getItemMeta();
        iMeta.setDisplayName(ChatColor.GOLD + b.getName());
        iMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "ID: " + b.getUniqueId().toString().replace("-", "")));
        icon.setItemMeta(iMeta);
        inv.setItem(15, icon);

        boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(viewer);
        ItemStack owner = builder(createSkull(anonymous ? null : b.getOwner()),
                meta -> {
                    meta.setDisplayName(anonymous ? ChatColor.AQUA + get("constants.business.anonymous") : String.format(get("constants.business.owner"), b.getOwner().getName()));
                    if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
                        meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                }, nbt -> {
                    if (!anonymous) {
                        nbt.setID("player_stats");
                        nbt.set("player", b.getOwner().getUniqueId().toString());
                    }
                });
        inv.setItem(11, owner);

        boolean pHome = b.getSetting(Settings.Business.PUBLIC_HOME) || b.isOwner(viewer);
        ItemStack home = builder(pHome ? (isLegacy() ? Material.matchMaterial("WORKBENCH") : Material.matchMaterial("CRAFTING_TABLE")) : Material.BARRIER,
                meta -> {
                    meta.setDisplayName(pHome ? ChatColor.AQUA + get("constants.business.home") : ChatColor.RED + get("constants.business.anonymous_home"));
                    if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_HOME))
                        meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                }, nbt -> {
                    nbt.setID("business:home");
                    nbt.set("business", b.getUniqueId());
                    nbt.set("anonymous", !pHome);
                }
        );
        ItemMeta hMeta = home.getItemMeta();
        home.setItemMeta(hMeta);
        inv.setItem(12, home);

        ItemStack settings = builder(Material.NETHER_STAR,
                meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.settings.business")),
                nbt -> {
                    nbt.setID("business:settings");
                    nbt.set("business", b.getUniqueId());
                }
        );

        if (b.isOwner(viewer)) inv.setItem(53, settings);

        AtomicInteger slot = new AtomicInteger(19);
        List<BusinessProduct> bProducts = b.getProducts();

        bProducts.forEach(p -> {
            if (slot.get() == 26) slot.set(28);
            if (slot.get() == 35) slot.set(37);
            if (slot.get() == 44) slot.set(46);
            if (slot.get() >= 53) return;

            ItemStack item = p.getItem().clone();
            if (item.getType() == Material.AIR) return;

            AtomicBoolean stock = new AtomicBoolean(true);

            ItemStack product = builder(item.clone(), 
                    meta -> {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add(" ");
                        lore.add(String.format(get("constants.business.price"), String.format("%,.2f", p.getAmount()).replace("D", ""), p.getEconomy().getSymbol() + ""));
            
                        lore.add(" ");
                        if (!b.isInStock(item)) {
                            lore.add(ChatColor.RED + get("constants.business.no_stock"));
                            stock.set(false);
                        } else {
                            AtomicInteger index = new AtomicInteger();
                            b.getResources().forEach(res -> {
                                if (item.isSimilar(res)) index.getAndAdd(res.getAmount());
                            });
            
                            lore.add(String.format(get("constants.business.stock_left"), String.format("%,.0f", (double) index.get())));
                        }
            
                        meta.setLore(lore);
                    }, nbt -> {
                        nbt.setID("product:buy");
                        nbt.set("product", p);
                    }
            );

            NBTWrapper nbt = of(product);

            nbt.set("product:in_stock", stock.get());
            nbt.set("is_product", true);

            inv.setItem(slot.get(), product);
            slot.incrementAndGet();
        });

        boolean pStats = b.getSetting(Settings.Business.PUBLIC_STATISTICS) || b.isOwner(viewer);
        ItemStack stats = builder(pStats ? Material.PAPER : Material.BARRIER,
                meta -> {
                    meta.setDisplayName(pStats ? ChatColor.AQUA + get("constants.business.statistics") : ChatColor.RED + get("constants.business.anonymous_statistics"));
                    if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_STATISTICS))
                        meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                }, nbt -> {
                    nbt.setID("business:statistics");
                    nbt.set("business", b.getUniqueId().toString());
                    nbt.set("anonymous", !pStats);
                }
        );
        inv.setItem(14, stats);

        if (b.isOwner(viewer)) {
            inv.setItem(26, builder(Material.BUCKET,
                meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.business.advertising")),
                nbt -> nbt.setID("business:advertising")
            ));
        }

        Material kMaterial;
        try {
            kMaterial = Material.valueOf("SIGN");
        } catch (IllegalArgumentException e) {
            kMaterial = Material.valueOf("OAK_SIGN");
        }

        inv.setItem(35, builder(kMaterial, meta -> {
            meta.setDisplayName(ChatColor.YELLOW + get("constants.business.keywords"));
            if (!b.getKeywords().isEmpty())
                meta.setLore(Arrays.asList(ChatPaginator.wordWrap(ChatColor.AQUA + String.join(", ", b.getKeywords()), 30)));
        }));

        if (!b.getRatings().isEmpty()) {
            boolean pRating = b.getSetting(Settings.Business.PUBLIC_RATING) || b.isOwner(viewer);
            double avg = b.getAverageRating();
            int avgI = (int) Math.round(avg - 1);

            ItemStack rating = builder(pRating ? CommandWrapper.getRatingMats()[avgI] : Material.BARRIER,
                meta -> { meta.setDisplayName(pRating ? ChatColor.YELLOW + String.format("%,.1f", avg) + "⭐" : ChatColor.RED + get("constants.business.anonymous_rating"));
                if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_RATING))
                    meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
            });
            inv.setItem(44, rating);
        } else inv.setItem(44, null);

        if (r.nextBoolean() && advertising && b.getSetting(Settings.Business.EXTERNAL_ADVERTISEMENT) && NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            Business rand = Business.randomAdvertisingBusiness();
            if (rand != null && !b.isBlacklisted(rand)) {
                BusinessAdvertiseEvent event = new BusinessAdvertiseEvent(rand);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled())
                    inv.setItem(27, builder(rand.getPublicIcon(), nbt -> {
                        nbt.setID("business:click:advertising");
                        nbt.set("business", rand.getUniqueId().toString());
                        nbt.set("from_business", b.getUniqueId().toString());
                    }));
            }
        }

        return inv;
    }

    default NovaInventory generateCorporationData(Corporation c, Player viewer) {
        NovaInventory inv = genGUI(54, String.format(get("constants.corporation.title"), c.getName()));
        inv.setCancelled();

        ItemStack icon = c.getPublicIcon();

        return inv;
    }

    default NovaInventory genGUI(int size, String name) {
        return genGUI("", size, name);
    }

    default NovaInventory genGUI(String id, int size, String name) {
        if (size < 9 || size > 54) return null;
        if (size % 9 > 0) return null;

        NovaInventory inv = createInventory(id, name, size);
        ItemStack bg = getGUIBackground();

        if (size < 27) return inv;

        for (int i = 0; i < 9; i++) inv.setItem(i, bg);
        for (int i = size - 9; i < size; i++) inv.setItem(i, bg);
        for (int i = 1; i < Math.floor((double) size / 9D) - 1; i++) {
            inv.setItem(i * 9, bg);
            inv.setItem(((i + 1) * 9) - 1, bg);
        }

        return inv;
    }

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
        String lang = NovaConfig.getConfiguration().getLanguage();
        return Language.getById(lang).getMessage(key);
    }

    static String getMessage(String key) { return get("plugin.prefix") + get(key); }

    static UUID untrimUUID(String oldUUID) {
        String p1 = oldUUID.substring(0, 8);
        String p2 = oldUUID.substring(8, 12);
        String p3 = oldUUID.substring(12, 16);
        String p4 = oldUUID.substring(16, 20);
        String p5 = oldUUID.substring(20, 32);

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
