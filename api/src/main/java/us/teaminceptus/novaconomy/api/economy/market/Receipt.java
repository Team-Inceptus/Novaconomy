package us.teaminceptus.novaconomy.api.economy.market;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for keeping track of purchased items on the Market
 */
public final class Receipt implements Serializable {
    
    private final long timestamp;
    private final Material purchased;
    private final UUID purchaserUUID;
    private final double purchasePrice;

    private transient OfflinePlayer purchaser;

    /**
     * Constructs a new Market Receipt.
     * @param purchased Material purchased
     * @param purchasePrice Price of the Material
     * @param player Player who purchased the Material
     */
    public Receipt(@NotNull Material purchased, double purchasePrice, @NotNull OfflinePlayer player) {
        this(purchased, purchasePrice, player, new Date());
    }

    /**
     * Constructs a new Market Receipt.
     * @param purchased Material purchased
     * @param purchasePrice Price of the Material
     * @param player Player who purchased the Material
     * @param timestamp Timestamp of the purchase
     */
    public Receipt(@NotNull Material purchased, double purchasePrice, @NotNull OfflinePlayer player, @NotNull Date timestamp) {
        this.timestamp = timestamp.getTime();
        this.purchased = purchased;
        this.purchaserUUID = player.getUniqueId();
        this.purchasePrice = purchasePrice;
        this.purchaser = player;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.purchaser = Bukkit.getOfflinePlayer(purchaserUUID);
    }

    /**
     * Fetches the player who purchased the Material.
     * @return Player who purchased the Material
     */
    @NotNull
    public OfflinePlayer getPurchaser() {
        return purchaser;
    }

    /**
     * Fetches the Material purchased.
     * @return Material purchased
     */
    @NotNull
    public Material getPurchased() {
        return purchased;
    }

    /**
     * Fetches how much the Material was purchased for.
     * @return Price of the Material purchased
     */
    public double getPurchasePrice() {
        return purchasePrice;
    }

    /**
     * Fetches the timestamp of the purchase.
     * @return Timestamp of the purchase
     */
    @NotNull
    public Date getTimestamp() {
        return new Date(timestamp);
    }

}
