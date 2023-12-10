package us.teaminceptus.novaconomy.api.auction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.auction.PlayerPurchaseAuctionItemEvent;
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

    private AuctionHouse() { throw new UnsupportedOperationException("Do not instantiate!"); }

    static final Set<AuctionItem> items = new HashSet<>();
    static final Map<UUID, Set<Bid>> bids = new HashMap<>();

    // Utility

    /**
     * Refreshes the auction house's cache.
     */
    public static void refreshAuctionHouse() {
        items.clear();
        read();
    }

    /**
     * Adds a regular item to the auction house.
     * @param owner The owner of the item.
     * @param item The item to add.
     * @param price The price of the item.
     */
    public static void addItem(@NotNull OfflinePlayer owner, @NotNull ItemStack item, @NotNull Price price) {
        addItem(owner, item, price, false, false);
    }

    /**
     * Adds a non-loose item to the auction house.
     * @param owner The owner of the item.
     * @param item The item to add.
     * @param price The price of the item.
     * @param buyNow Whether or not the item is a buy now item.
     */
    public static void addItem(@NotNull OfflinePlayer owner, @NotNull ItemStack item, @NotNull Price price, boolean buyNow) {
        addItem(owner, item, price, buyNow, false);
    }

    /**
     * Adds an item to the auction house.
     * @param owner The owner of the item.
     * @param item The item to add.
     * @param price The price of the item.
     * @param buyNow Whether or not the item is a buy now item.
     * @param loose Whether or not the price is loose.
     */
    public static void addItem(@NotNull OfflinePlayer owner, @NotNull ItemStack item, @NotNull Price price, boolean buyNow, boolean loose) {
        if (owner == null) throw new IllegalArgumentException("Owner cannot be null!");
        if (item == null) throw new IllegalArgumentException("Item cannot be null!");
        if (price == null) throw new IllegalArgumentException("Price cannot be null!");

        AuctionItem auction = new AuctionItem(UUID.randomUUID(), owner, System.currentTimeMillis(), item, price, buyNow, loose);
        items.add(auction);
        write();
    }

    /**
     * Purchases a buy now item from the auction house.
     * @param purchaser The purchaser of the item.
     * @param auction The auction to purchase.
     * @throws IllegalArgumentException If the purchaser or auction is null, or if the auction is not a buy now auction.
     */
    public static void purchase(@NotNull Player purchaser, @NotNull AuctionItem auction) throws IllegalArgumentException {
        if (purchaser == null) throw new IllegalArgumentException("Purchaser cannot be null!");
        if (auction == null) throw new IllegalArgumentException("Auction cannot be null!");
        if (!auction.isBuyNow()) throw new IllegalArgumentException("Auction must be a buy now auction!");
        if (auction.isExpired()) throw new IllegalArgumentException("Auction is expired!");
        if (!items.contains(auction)) throw new IllegalArgumentException("Auction must be in the auction house!");

        purchaser.getInventory().addItem(auction.getItem());

        Price p = auction.getPrice();
        NovaPlayer purchaser0 = new NovaPlayer(purchaser);
        purchaser0.remove(p);

        NovaPlayer owner = new NovaPlayer(auction.getOwner());
        owner.add(p);

        items.remove(auction);
        write();

        PlayerPurchaseAuctionItemEvent event = new PlayerPurchaseAuctionItemEvent(purchaser, auction);
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Gets an immutable copy of the bids on an auction item.
     * @param item The auction item to get the bids for.
     * @return An immutable copy of the bids on the auction item.
     */
    @NotNull
    public static Set<Bid> getBids(@NotNull AuctionItem item) {
        return ImmutableSet.copyOf(bids.get(item.getUUID()));
    }

    /**
     * Gets whether the auction house item has any bids on it.
     * @param item The auction item to check.
     * @return Whether the auction house item has any bids on it.
     */
    public static boolean isBiddedOn(@NotNull AuctionItem item) {
        if (item.isBuyNow()) return false;
        return !bids.get(item.getUUID()).isEmpty();
    }

    /**
     * Bids on an item in the Auction House.
     * @param item The item to bid on.
     * @param owner The owner of the bid.
     * @param bid The bid to place.
     * @return The bid that was placed.
     */
    @NotNull
    public static Bid bid(@NotNull AuctionItem item, @NotNull OfflinePlayer owner, @NotNull Price bid) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null!");
        if (owner == null) throw new IllegalArgumentException("Owner cannot be null!");
        if (bid == null) throw new IllegalArgumentException("Bid cannot be null!");
        if (item.isBuyNow()) throw new IllegalArgumentException("Item must not be a buy now item!");
        if (item.isExpired()) throw new IllegalArgumentException("Item is expired!");
        if (!items.contains(item)) throw new IllegalArgumentException("Item must be in the auction house!");

        Bid b = new Bid(owner.getUniqueId(), bid.getEconomy().getUniqueId(), bid.getAmount());
        bids.computeIfAbsent(item.getUUID(), k -> new HashSet<>()).add(b);
        write();
        return b;
    }

    /**
     * Gets an immutable copy of all the auction items in the auction house.
     * @return All Items in the auction house.
     */
    public static Set<AuctionItem> getItems() {
        return ImmutableSet.copyOf(items);
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
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("owner")));
            long timestamp = rs.getLong("timestamp");

            ByteArrayInputStream itemIs = new ByteArrayInputStream(rs.getBytes("item"));
            BukkitObjectInputStream itemBis = new BukkitObjectInputStream(itemIs);
            ItemStack item = (ItemStack) itemBis.readObject();
            itemBis.close();

            Economy economy = Economy.byId(UUID.fromString(rs.getString("economy")));
            double price = rs.getDouble("price");
            boolean buyNow = rs.getBoolean("buy_now");
            boolean loosePrice = rs.getBoolean("loose_price");

            AuctionItem auction = new AuctionItem(id, owner, timestamp, item, new Price(economy, price), buyNow, loosePrice);
            items.add(auction);
        }

        rs.close();
    }

    @SuppressWarnings("unchecked")
    private static void readFile() throws IOException, ReflectiveOperationException {
        File folder = NovaConfig.getAuctionFolder();
        if (!folder.exists()) folder.mkdirs();

        for (File f : folder.listFiles()) {
            if (f == null) continue;

            if (f.getName().equals("bids.dat")) {
                FileInputStream bidsIs = new FileInputStream(f);
                BukkitObjectInputStream bidsBis = new BukkitObjectInputStream(new BufferedInputStream(bidsIs));
                bids.putAll((Map<UUID, Set<Bid>>) bidsBis.readObject());
                bidsBis.close();
                continue;
            }

            FileInputStream is = new FileInputStream(f);
            BukkitObjectInputStream bis = new BukkitObjectInputStream(new BufferedInputStream(is));
            items.addAll((Set<AuctionItem>) bis.readObject());
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

        for (AuctionItem auction : items) {
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
                ps.setLong(3, auction.getPostedTimestmap().getTime());

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

    @SuppressWarnings("unchecked")
    private static void writeFile() throws IOException, ReflectiveOperationException {
        File folder = NovaConfig.getAuctionFolder();
        if (!folder.exists()) folder.mkdirs();

        File bidsFile = new File(folder, "bids.dat");
        if (!bidsFile.exists()) bidsFile.createNewFile();

        FileOutputStream bidsOs = new FileOutputStream(bidsFile);
        BukkitObjectOutputStream bidsBos = new BukkitObjectOutputStream(bidsOs);
        bidsBos.writeObject(bids);
        bidsBos.close();

        List<UUID> owners = items.stream()
                .map(AuctionItem::getOwner)
                .map(OfflinePlayer::getUniqueId)
                .distinct()
                .collect(Collectors.toList());

        for (UUID owner : owners) {
            Set<AuctionItem> ownerItems = items.stream()
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

        // Remove old files that are empty

        for (File f : folder.listFiles()) {
            if (f == null) continue;

            if (f.length() == 0) {
                f.delete();
                continue;
            }

            FileInputStream is = new FileInputStream(f);
            BukkitObjectInputStream bis = new BukkitObjectInputStream(new BufferedInputStream(is));
            Set<AuctionItem> ownerItems = (Set<AuctionItem>) bis.readObject();
            bis.close();

            if (ownerItems == null || ownerItems.isEmpty()) f.delete();
        }
    }

}
