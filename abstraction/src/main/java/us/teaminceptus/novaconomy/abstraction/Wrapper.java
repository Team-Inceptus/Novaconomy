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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.ChatPaginator;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

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
import java.util.concurrent.atomic.AtomicInteger;

public interface Wrapper {

    String ROOT = "Novaconomy";

    default int getCommandVersion() {
        return 1;
    }

    void sendActionbar(Player p, String message);

    void sendActionbar(Player p, BaseComponent component);

    String getNBTString(ItemStack item, String key);

    ItemStack setNBT(ItemStack item, String key, String value);

    ItemStack setNBT(ItemStack item, String key, ItemStack value);

    ItemStack getNBTItem(ItemStack item, String key);

    ItemStack getGUIBackground();

    ItemStack createSkull(OfflinePlayer p);
    
    default ItemStack setID(ItemStack item, String id) {
        return setNBT(item, "id", id);
    }

    default ItemStack removeID(ItemStack item) {
        return setID(item, "");
    }

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

    double getNBTDouble(ItemStack item, String key);

    ItemStack setNBT(ItemStack item, String key, double value);

    ItemStack setNBT(ItemStack item, String key, boolean value);

    boolean getNBTBoolean(ItemStack item, String key);

    ItemStack normalize(ItemStack item);

    boolean isAgeable(Block b);

    void removeItem(PlayerInteractEvent p);

    default boolean hasID(ItemStack item) { return getID(item) != null && getID(item).length() > 0; }

    default boolean isProduct(ItemStack item) { return hasID(item) && (getID(item).equalsIgnoreCase("product") || getNBTBoolean(item, "is_product")); }

    default String getID(ItemStack item) { return getNBTString(item, "id"); }

    static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Novaconomy");
    }

    default ItemStack setNBT(ItemStack item, String key, Product p) {
        ItemStack newItem = setNBT(item, key + "-product:amount", p.getPrice().getAmount());
        newItem = setNBT(newItem, key + "-product:economy", p.getEconomy().getUniqueId().toString());
        newItem = setNBT(newItem, key + "-product:item", normalize(p.getItem()));

        if (p instanceof BusinessProduct) {
            BusinessProduct bp = (BusinessProduct) p;
            newItem = setNBT(newItem, key + "-bproduct:business", bp.getBusiness().getUniqueId().toString());
        }

        return newItem;
    }

    default Product getNBTProduct(ItemStack item, String key) {
        double amount = getNBTDouble(item, key + "-product:amount");
        Economy econ = Economy.getEconomy(UUID.fromString(getNBTString(item, key + "-product:economy")));
        ItemStack product = normalize(getNBTItem(item, key + "-product:item"));

        Product p = new Product(product, econ, amount);
        try {
            UUID business = UUID.fromString(getNBTString(item, key + "-bproduct:business"));

            if (Business.exists(business)) return new BusinessProduct(p, Business.getById(business));
            else return p;
        } catch (IllegalArgumentException e) {
            return p;
        }
    }

    // Util

    default ItemStack createCheck(Economy econ, double amount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        ItemStack item = new ItemStack(Material.PAPER);
        item = setID(item, "economy:check");
        item = setNBT(item, "economy", econ.getUniqueId().toString());
        item = setNBT(item, "amount", amount);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("" + ChatColor.YELLOW + amount + econ.getSymbol());
        meta.setLore(Collections.singletonList(ChatColor.GOLD + "" + amount + " " + econ.getName() + "(s)"));
        item.setItemMeta(meta);
        return item;
    }

    default Inventory generateBusinessData(Business b, Player viewer, boolean advertising) {
        Inventory inv = genGUI(54, ChatColor.GOLD + b.getName(), new Wrapper.CancelHolder());

        if (!b.getRatings().isEmpty()) inv.setItem(44, CommandWrapper.loading());
        for (int i = 46; i < 53; i++) inv.setItem(i, null);

        ItemStack icon = new ItemStack(b.getIcon());
        ItemMeta iMeta = icon.getItemMeta();
        iMeta.setDisplayName(ChatColor.GOLD + b.getName());
        iMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "ID: " + b.getUniqueId().toString().replace("-", "")));
        icon.setItemMeta(iMeta);
        inv.setItem(15, icon);

        boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(viewer);
        ItemStack owner = createSkull(anonymous ? null : b.getOwner());
        ItemMeta oMeta = owner.getItemMeta();
        oMeta.setDisplayName(anonymous ? ChatColor.AQUA + get("constants.business.anonymous") : String.format(get("constants.business.owner"), b.getOwner().getName()));
        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
            oMeta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
        owner.setItemMeta(oMeta);
        if (!anonymous) {
            owner = setID(owner, "player_stats");
            owner = setNBT(owner, "player", b.getOwner().getUniqueId().toString());
        }
        inv.setItem(11, owner);

        boolean pHome = b.getSetting(Settings.Business.PUBLIC_HOME) || b.isOwner(viewer);
        ItemStack home = new ItemStack(pHome ? (isLegacy() ? Material.matchMaterial("WORKBENCH") : Material.matchMaterial("CRAFTING_TABLE")) : Material.BARRIER);
        ItemMeta hMeta = home.getItemMeta();
        hMeta.setDisplayName(pHome ? ChatColor.AQUA + get("constants.business.home") : ChatColor.RED + get("constants.business.anonymous_home"));
        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_HOME))
            hMeta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
        home.setItemMeta(hMeta);
        home = setID(home, "business:home");
        home = setNBT(home, "business", b.getUniqueId().toString());
        home = setNBT(home, "anonymous", !pHome);
        inv.setItem(12, home);

        ItemStack settings = new ItemStack(Material.NETHER_STAR);
        ItemMeta stMeta = settings.getItemMeta();
        stMeta.setDisplayName(ChatColor.GREEN + get("constants.settings.business"));
        settings.setItemMeta(stMeta);
        settings = setID(settings, "business:settings");
        settings = setNBT(settings, "business", b.getUniqueId().toString());
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

            ItemStack product = item.clone();

            product = setNBT(product, "product", p);
            product = setID(product, "product:buy");

            ItemMeta meta = product.hasItemMeta() ? product.getItemMeta() : Bukkit.getItemFactory().getItemMeta(product.getType());

            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(" ");
            lore.add(String.format(get("constants.business.price"), String.format("%,.2f", p.getAmount()).replace("D", ""), p.getEconomy().getSymbol() + ""));

            boolean stock = true;

            lore.add(" ");
            if (!b.isInStock(item)) {
                lore.add(ChatColor.RED + get("constants.business.no_stock"));
                stock = false;
            } else {
                AtomicInteger index = new AtomicInteger();
                b.getResources().forEach(res -> {
                    if (item.isSimilar(res)) index.getAndAdd(res.getAmount());
                });

                lore.add(String.format(get("constants.business.stock_left"), String.format("%,.0f", (double) index.get())));
            }

            meta.setLore(lore);
            product.setItemMeta(meta);

            product = setNBT(product, "product:in_stock", stock);
            product = setNBT(product, "is_product", true);

            inv.setItem(slot.get(), product);
            slot.incrementAndGet();
        });

        boolean pStats = b.getSetting(Settings.Business.PUBLIC_STATISTICS) || b.isOwner(viewer);
        ItemStack stats = new ItemStack(pStats ? Material.PAPER : Material.BARRIER);
        ItemMeta sMeta = stats.getItemMeta();
        sMeta.setDisplayName(pStats ? ChatColor.AQUA + get("constants.business.statistics") : ChatColor.RED + get("constants.business.anonymous_statistics"));
        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_STATISTICS))
            sMeta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
        stats.setItemMeta(sMeta);
        stats = setID(stats, "business:statistics");
        stats = setNBT(stats, "business", b.getUniqueId().toString());
        stats = setNBT(stats, "anonymous", !pStats);
        inv.setItem(14, stats);

        if (b.isOwner(viewer)) {
            ItemStack adInfo = new ItemStack(Material.BUCKET);
            ItemMeta adMeta = adInfo.getItemMeta();
            adMeta.setDisplayName(ChatColor.AQUA + get("constants.business.advertising"));
            adInfo.setItemMeta(adMeta);
            adInfo = setID(adInfo, "business:advertising");
            inv.setItem(26, adInfo);
        }

        Material kMaterial;
        try {
            kMaterial = Material.valueOf("SIGN");
        } catch (IllegalArgumentException e) {
            kMaterial = Material.valueOf("OAK_SIGN");
        }

        ItemStack keywords = new ItemStack(kMaterial);
        ItemMeta kMeta = keywords.getItemMeta();
        kMeta.setDisplayName(ChatColor.YELLOW + get("constants.business.keywords"));
        if (!b.getKeywords().isEmpty())
            kMeta.setLore(Arrays.asList(ChatPaginator.wordWrap(ChatColor.AQUA + String.join(", ", b.getKeywords()), 30)));
        keywords.setItemMeta(kMeta);
        inv.setItem(35, keywords);

        if (!b.getRatings().isEmpty()) {
            boolean pRating = b.getSetting(Settings.Business.PUBLIC_RATING) || b.isOwner(viewer);
            double avg = b.getAverageRating();
            int avgI = (int) Math.round(avg - 1);

            ItemStack rating = new ItemStack(pRating ? CommandWrapper.RATING_MATS[avgI] : Material.BARRIER);
            ItemMeta rMeta = rating.getItemMeta();
            rMeta.setDisplayName(pRating ? ChatColor.YELLOW + String.format("%,.1f", avg) + "⭐" : ChatColor.RED + get("constants.business.anonymous_rating"));
            if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_RATING))
                rMeta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
            rating.setItemMeta(rMeta);
            inv.setItem(44, rating);
        } else inv.setItem(44, null);

        if (r.nextBoolean() && advertising && b.getSetting(Settings.Business.EXTERNAL_ADVERTISEMENT) && NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            Business rand = Business.randomAdvertisingBusiness();
            if (rand != null && !b.isBlacklisted(rand)) {
                ItemStack rIcon = rand.getPublicIcon();
                rIcon = setID(rIcon, "business:click:advertising");
                rIcon = setNBT(rIcon, "business", rand.getUniqueId().toString());
                rIcon = setNBT(rIcon, "from_business", b.getUniqueId().toString());
                inv.setItem(27, rIcon);
            }
        }

        return inv;
    }

    SecureRandom r = new SecureRandom();

    default Inventory genGUI(int size, String name) {
        return genGUI(size, name, null);
    }

    default Inventory genGUI(int size, String name, InventoryHolder holder) {
        if (size < 9 || size > 54) return null;
        if (size % 9 > 0) return null;

        Inventory inv = Bukkit.createInventory(holder, size, name);
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
        } catch (Exception e) { throw new IllegalStateException("Wrapper not Found: " + getServerVersion()); }
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

    class CancelHolder implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return null;
        }
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
