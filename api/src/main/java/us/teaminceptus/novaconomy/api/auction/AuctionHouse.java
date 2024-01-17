package us.teaminceptus.novaconomy.api.auction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.auction.PlayerBidEvent;
import us.teaminceptus.novaconomy.api.events.auction.PlayerPurchaseAuctionItemEvent;
import us.teaminceptus.novaconomy.api.events.auction.PlayerWinAuctionEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the Novaconomy Auction House
 */
public final class AuctionHouse {

    private static final String BIDS_FILE = "bids.dat";

    private AuctionHouse() { throw new UnsupportedOperationException("Do not instantiate!"); }

    static final Set<AuctionProduct> products = new HashSet<>();
    static final Map<UUID, Set<Bid>> bids = new HashMap<>();

    // Utility

    /**
     * Refreshes the auction house.
     * @param refreshCache Whether or not to clear and refresh the cache.
     */
    public static void refreshAuctionHouse(boolean refreshCache) {
        if (refreshCache) {
            products.clear();
            bids.clear();
            read();
        }

        for (AuctionProduct product : products)
            if (product.isExpired() && !bids.getOrDefault(product.getUUID(), new HashSet<>()).isEmpty())
                endAuction(product);
    }

    /**
     * Adds a regular item to the auction house.
     * @param owner The owner of the item.
     * @param item The item to add.
     * @param price The price of the item.
     * @return The auction product that was added.
     */
    public static AuctionProduct addItem(@NotNull OfflinePlayer owner, @NotNull ItemStack item, @NotNull Price price) {
        return addItem(owner, item, price, false, false);
    }

    /**
     * Adds a non-loose item to the auction house.
     * @param owner The owner of the item.
     * @param item The item to add.
     * @param price The price of the item.
     * @param buyNow Whether or not the item is a buy now item.
     * @return The auction product that was added.
     */
    public static AuctionProduct addItem(@NotNull OfflinePlayer owner, @NotNull ItemStack item, @NotNull Price price, boolean buyNow) {
        return addItem(owner, item, price, buyNow, false);
    }

    /**
     * Adds an item to the auction house.
     * @param owner The owner of the item.
     * @param item The item to add.
     * @param price The price of the item.
     * @param buyNow Whether or not the item is a buy now item.
     * @param loose Whether or not the price is loose.
     * @return The auction product that was added.
     */
    public static AuctionProduct addItem(@NotNull OfflinePlayer owner, @NotNull ItemStack item, @NotNull Price price, boolean buyNow, boolean loose) {
        if (owner == null) throw new IllegalArgumentException("Owner cannot be null!");
        if (item == null) throw new IllegalArgumentException("Item cannot be null!");
        if (price == null) throw new IllegalArgumentException("Price cannot be null!");

        AuctionProduct auction = new AuctionProduct(UUID.randomUUID(), owner.getUniqueId(), System.currentTimeMillis(), item, price, buyNow, loose);
        products.add(auction);
        write();

        return auction;
    }

    /**
     * Purchases a buy now item from the auction house.
     * @param purchaser The purchaser of the item.
     * @param product The auction to purchase.
     * @throws IllegalArgumentException If the purchaser or auction is null, or if the auction is not a buy now auction.
     */
    public static void purchase(@NotNull Player purchaser, @NotNull AuctionProduct product) throws IllegalArgumentException {
        if (purchaser == null) throw new IllegalArgumentException("Purchaser cannot be null!");
        if (product == null) throw new IllegalArgumentException("Auction cannot be null!");
        if (!product.isBuyNow()) throw new IllegalArgumentException("Auction must be a buy now auction!");
        if (!products.contains(product)) throw new IllegalArgumentException("Auction must be in the auction house!");

        Price p = product.getPrice();
        NovaPlayer purchaser0 = new NovaPlayer(purchaser);
        purchaser0.remove(p);
        purchaser0.awardAuction(product);

        NovaPlayer owner = new NovaPlayer(product.getOwner());
        owner.add(p);
        if (owner.isOnline() && owner.hasNotifications())
            owner.getOnlinePlayer().sendMessage(Language.getCurrentMessage("plugin.prefix") + String.format(Language.getCurrentLocale(), Language.getCurrentMessage("notification.business.purchase"), purchaser.getName(), product.getItemName()));

        removeProduct(product);

        PlayerPurchaseAuctionItemEvent event = new PlayerPurchaseAuctionItemEvent(purchaser, product);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Gets an immutable copy of the bids on an auction product.
     * @param product The auction product to get the bids for.
     * @return An immutable copy of the bids on the auction product.
     */
    @NotNull
    public static Set<Bid> getBids(@NotNull AuctionProduct product) {
        if (product == null) throw new IllegalArgumentException("Item cannot be null!");
        return ImmutableSet.copyOf(bids.getOrDefault(product.getUUID(), new HashSet<>()));
    }

    /**
     * Gets an immutable copy of the bids on an auction product by a player.
     * @param player The player to get the bids for.
     * @return All Bids made by this Player
     */
    @NotNull
    public static Map<Bid, AuctionProduct> getBidsBy(@NotNull OfflinePlayer player) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null!");

        Map<Bid, AuctionProduct> map = new HashMap<>();
        for (AuctionProduct product : products)
            for (Bid bid : bids.getOrDefault(product.getUUID(), new HashSet<>()))
                if (bid.getBidder().equals(player)) {
                    map.put(bid, product);
                    break;
                }

        return ImmutableMap.copyOf(map);
    }

    /**
     * Gets the top bid on an auction product.
     * @param product The auction product to get the top bid for.
     * @return The top bid on the auction product, or null if there are no bids.
     */
    @Nullable
    public static Bid getTopBid(@NotNull AuctionProduct product) {
        if (product == null) throw new IllegalArgumentException("Item cannot be null!");
        if (getBids(product).isEmpty()) return null;

        return bids.getOrDefault(product.getUUID(), new HashSet<>())
                .stream()
                .max(Comparator.comparingDouble(b -> b.getPrice().getRealAmount()))
                .orElse(null);
    }

    /**
     * Attempts to find an Auction House Product by its UUID.
     * @param id The UUID of the product.
     * @return The Auction Product, or null if it could not be found.
     */
    @Nullable
    public static AuctionProduct byId(@NotNull UUID id) {
        return products.stream()
                .filter(auction -> auction.getUUID().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets whether the auction house item has any bids on it.
     * @param item The auction item to check.
     * @return Whether the auction house item has any bids on it.
     */
    public static boolean isBiddedOn(@NotNull AuctionProduct item) {
        if (item.isBuyNow()) return false;
        return !bids.get(item.getUUID()).isEmpty();
    }

    /**
     * Bids on an product in the Auction House.
     * @param owner The owner of the bid.
     * @param product The product to bid on.
     * @param bid The bid to place.
     * @return The bid that was placed.
     */
    @NotNull
    public static Bid bid(@NotNull Player owner, @NotNull AuctionProduct product, @NotNull Price bid) {
        if (product == null) throw new IllegalArgumentException("Item cannot be null!");
        if (owner == null) throw new IllegalArgumentException("Owner cannot be null!");
        if (bid == null) throw new IllegalArgumentException("Bid cannot be null!");
        if (product.isBuyNow()) throw new IllegalArgumentException("Item must not be a buy now product!");
        if (product.isExpired()) throw new IllegalArgumentException("Item is expired!");
        if (!products.contains(product)) throw new IllegalArgumentException("Item must be in the auction house!");

        Bid b = new Bid(owner.getUniqueId(), bid.getEconomy().getUniqueId(), bid.getAmount());
        bids.computeIfAbsent(product.getUUID(), k -> new HashSet<>()).add(b);
        write();

        Set<Player> bidders = bids.get(product.getUUID()).stream()
                .map(Bid::getBidder)
                .filter(player -> player != owner)
                .filter(OfflinePlayer::isOnline)
                .map(OfflinePlayer::getPlayer)
                .collect(Collectors.toSet());

        for (Player bidder : bidders) {
            NovaPlayer nb = new NovaPlayer(bidder);
            if (nb.hasNotifications())
                bidder.sendMessage(Language.getCurrentMessage("plugin.prefix") + ChatColor.YELLOW + String.format(Language.getCurrentLocale(), Language.getCurrentMessage("success.auction.bid"), owner.getName(), bid, product.getItemName()));
        }

        PlayerBidEvent event = new PlayerBidEvent(owner, b, product);
        Bukkit.getPluginManager().callEvent(event);
        return b;
    }

    /**
     * Ends the auction on a product, giving the item to the top bidder and the money to the owner.
     * @param product The product to end the auction on.
     */
    public static void endAuction(@NotNull AuctionProduct product) {
        if (product == null) throw new IllegalArgumentException("Item cannot be null!");
        if (product.isBuyNow()) throw new IllegalArgumentException("Item must not be a buy now product!");
        if (!products.contains(product)) throw new IllegalArgumentException("Item must be in the auction house!");

        Bid top = getTopBid(product);
        if (top != null) {
            Price price = top.getPrice();
            NovaPlayer bidder = new NovaPlayer(top.getBidder());

            if (bidder.isOnline() && bidder.canAfford(price, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseAuction())) {
                bidder.remove(price);
                bidder.awardAuction(product);

                if (bidder.hasNotifications())
                    bidder.getPlayer().getPlayer().sendMessage(Language.getCurrentMessage("plugin.prefix") + ChatColor.GREEN + String.format(Language.getCurrentLocale(), Language.getCurrentMessage("success.auction.win"), product.getItemName()));
            } else
                bidder.addWonAuction(product.cloneWithPrice(price));

            NovaPlayer owner = new NovaPlayer(product.getOwner());
            owner.add(price);

            PlayerWinAuctionEvent event = new PlayerWinAuctionEvent(top.getBidder(), product);
            Bukkit.getPluginManager().callEvent(event);
        }

        removeProduct(product);
    }

    /**
     * Removes a product from the auction house.
     * @param product The product to remove.
     */
    public static void removeProduct(@NotNull AuctionProduct product) {
        if (product == null) throw new IllegalArgumentException("Item cannot be null!");
        if (!products.contains(product)) throw new IllegalArgumentException("Item must be in the auction house!");

        products.remove(product);
        bids.remove(product.getUUID());
        write();
    }

    /**
     * Gets an immutable copy of all the auction products in the auction house.
     * @return All Products in the auction house.
     */
    public static Set<AuctionProduct> getProducts() {
        return ImmutableSet.copyOf(products);
    }

    /**
     * Gets an immutable copy of all the auction products in the auction house owned by a player.
     * @param owner The owner of the products.
     * @return All Products in the auction house owned by a player, or an empty set.
     */
    @NotNull
    public static Set<AuctionProduct> getProducts(@Nullable OfflinePlayer owner) {
        if (owner == null) return ImmutableSet.of();
        return ImmutableSet.copyOf(products.stream()
                .filter(auction -> auction.getOwner().getUniqueId().equals(owner.getUniqueId()))
                .collect(Collectors.toSet()));
    }

    // I/O

    private static void checkTable() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS auction (" +
                "id CHAR(36) NOT NULL, " +
                "owner CHAR(36) NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "item BLOB(65535) NOT NULL, " +
                "economy CHAR(36) NOT NULL, " +
                "price DOUBLE NOT NULL, " +
                "buy_now BOOLEAN NOT NULL, " +
                "loose_price BOOLEAN NOT NULL, " +
                "bids MEDIUMBLOB NOT NULL, " +
                "PRIMARY KEY (id))");
    }

    private static void read() {
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                readDB();
            } else
                readFile();
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private static void readDB() throws SQLException, IOException, ReflectiveOperationException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();
        ResultSet rs = db.createStatement().executeQuery("SELECT * FROM auction");

        while (rs.next()) {
            UUID id = UUID.fromString(rs.getString("id"));
            UUID owner = UUID.fromString(rs.getString("owner"));
            long timestamp = rs.getLong("timestamp");

            ByteArrayInputStream itemIs = new ByteArrayInputStream(rs.getBytes("item"));
            BukkitObjectInputStream itemBis = new BukkitObjectInputStream(itemIs);
            ItemStack item = (ItemStack) itemBis.readObject();
            itemBis.close();

            Economy economy = Economy.byId(UUID.fromString(rs.getString("economy")));
            double price = rs.getDouble("price");
            boolean buyNow = rs.getBoolean("buy_now");
            boolean loosePrice = rs.getBoolean("loose_price");

            AuctionProduct auction = new AuctionProduct(id, owner, timestamp, item, new Price(economy, price), buyNow, loosePrice);
            products.add(auction);
        }

        rs.close();
    }

    @SuppressWarnings("unchecked")
    private static void readFile() throws IOException, ReflectiveOperationException {
        File folder = NovaConfig.getAuctionFolder();
        if (!folder.exists()) folder.mkdirs();

        clearUnused();
        for (File f : folder.listFiles()) {
            if (f == null) continue;

            if (f.getName().equals(BIDS_FILE)) {
                FileInputStream bidsIs = new FileInputStream(f);
                BukkitObjectInputStream bidsBis = new BukkitObjectInputStream(new BufferedInputStream(bidsIs));
                bids.putAll((Map<UUID, Set<Bid>>) bidsBis.readObject());
                bidsBis.close();
                continue;
            }

            FileInputStream is = new FileInputStream(f);
            BukkitObjectInputStream bis = new BukkitObjectInputStream(new BufferedInputStream(is));
            products.addAll((Set<AuctionProduct>) bis.readObject());
            bis.close();
        }
    }

    private static void write() {
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                writeDB();
            } else
                writeFile();
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private static void writeDB() throws SQLException, IOException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        for (AuctionProduct auction : products) {
            String sql;

            try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM auction WHERE id = \"" + auction.getUUID() + "\"")) {
                if (rs.next())
                    sql = "UPDATE auction SET " +
                            "id = ?, " +
                            "owner = ?, " +
                            "timestamp = ?, " +
                            "item = ?, " +
                            "economy = ?, " +
                            "price = ?, " +
                            "buy_now = ?, " +
                            "loose_price = ?, " +
                            "bids = ? " +
                            "WHERE economy = \"" + auction.getUUID() + "\"";
                else
                    sql = "INSERT INTO auction VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                PreparedStatement ps = db.prepareStatement(sql);
                ps.setString(1, auction.getUUID().toString());
                ps.setString(2, auction.getOwner().getUniqueId().toString());
                ps.setLong(3, auction.getPostedTimestamp().getTime());

                ByteArrayOutputStream itemOs = new ByteArrayOutputStream();
                BukkitObjectOutputStream itemBos = new BukkitObjectOutputStream(itemOs);
                itemBos.writeObject(auction.getItem());
                ps.setBytes(4, itemOs.toByteArray());

                ps.setString(5, auction.getPrice().getEconomy().getUniqueId().toString());
                ps.setDouble(6, auction.getPrice().getAmount());
                ps.setBoolean(7, auction.isBuyNow());
                ps.setBoolean(8, auction.isLoosePrice());

                ByteArrayOutputStream bidsOs = new ByteArrayOutputStream();
                BukkitObjectOutputStream bidsBos = new BukkitObjectOutputStream(bidsOs);
                bidsBos.writeObject(bids.get(auction.getUUID()));
                ps.setBytes(9, bidsOs.toByteArray());

                ps.executeUpdate();
                ps.close();
            }
        }
    }

    private static void writeFile() throws IOException, ReflectiveOperationException {
        File folder = NovaConfig.getAuctionFolder();
        if (!folder.exists()) folder.mkdirs();

        clearUnused();

        File bidsFile = new File(folder, BIDS_FILE);
        if (bids.isEmpty()) {
            if (bidsFile.exists()) bidsFile.delete();
        } else {
            if (!bidsFile.exists()) bidsFile.createNewFile();

            FileOutputStream bidsOs = new FileOutputStream(bidsFile);
            BukkitObjectOutputStream bidsBos = new BukkitObjectOutputStream(bidsOs);
            bidsBos.writeObject(bids);
            bidsBos.close();
        }

        List<UUID> owners = products.stream()
                .map(AuctionProduct::getOwner)
                .map(OfflinePlayer::getUniqueId)
                .distinct()
                .collect(Collectors.toList());

        for (UUID owner : owners) {
            Set<AuctionProduct> ownerItems = products.stream()
                    .filter(auction -> auction.getOwner().getUniqueId().equals(owner))
                    .filter(auction -> !auction.isExpired())
                    .collect(Collectors.toSet());
            File target = new File(folder, owner.toString().replace("-", "") + ".dat");

            if (ownerItems.isEmpty()) {
                if (target.exists()) target.delete();
                continue;
            }

            if (!target.exists()) target.createNewFile();

            FileOutputStream os = new FileOutputStream(target);
            BukkitObjectOutputStream bos = new BukkitObjectOutputStream(os);
            bos.writeObject(ownerItems);
            bos.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearUnused() throws IOException, ReflectiveOperationException {
        File folder = NovaConfig.getAuctionFolder();
        if (!folder.exists()) folder.mkdirs();

        for (File f : folder.listFiles()) {
            if (f == null) continue;
            if (f.getName().equals(BIDS_FILE)) continue;

            if (f.length() == 0) {
                f.delete();
                continue;
            }

            FileInputStream is = new FileInputStream(f);
            BukkitObjectInputStream bis = new BukkitObjectInputStream(new BufferedInputStream(is));
            Set<AuctionProduct> ownerItems = (Set<AuctionProduct>) bis.readObject();
            bis.close();

            if (ownerItems == null || !ownerItems.stream().filter(products::contains).findAny().isPresent()) f.delete();
        }
    }

}
