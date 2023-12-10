package us.teaminceptus.novaconomy.api.auction;

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
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents the Novaconomy Auction House
 */
public final class AuctionHouse {

    private AuctionHouse() { throw new UnsupportedOperationException("Do not instantiate!"); }

    static final Set<AuctionItem> items = new HashSet<>();

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

        items.remove(auction);
        write();

        PlayerPurchaseAuctionItemEvent event = new PlayerPurchaseAuctionItemEvent(purchaser, auction);
        Bukkit.getPluginManager().callEvent(event);
    }

    // I/O

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
                "PRIMARY KEY (id))");
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
            if (auction.isExpired()) continue;

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
            FileInputStream is = new FileInputStream(f);
            BukkitObjectInputStream bis = new BukkitObjectInputStream(new BufferedInputStream(is));
            items.addAll((Set<AuctionItem>) bis.readObject());
            bis.close();
        }

        items.removeIf(AuctionItem::isExpired);
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
                            "loose_price = ? " +
                            "WHERE economy = \"" + auction.getUUID() + "\"";
                else
                    sql = "INSERT INTO auction VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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

                ps.executeUpdate();
                ps.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeFile() throws IOException, ReflectiveOperationException {
        File folder = NovaConfig.getAuctionFolder();
        if (!folder.exists()) folder.mkdirs();

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
