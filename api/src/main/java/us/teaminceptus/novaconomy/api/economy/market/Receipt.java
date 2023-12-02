package us.teaminceptus.novaconomy.api.economy.market;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for keeping track of purchased items on the Market
 * @since 1.7.1
 */
public final class Receipt implements ConfigurationSerializable, Serializable {

    private static final long serialVersionUID = 7255336827661231956L;

    private final long timestamp;
    private final Material purchased;
    private final int purchaseAmount;
    private final UUID purchaserUUID;
    private final double purchasePrice;

    private transient OfflinePlayer purchaser;

    /**
     * Constructs a new Market Receipt.
     * @param purchased Material purchased
     * @param purchasePrice Price of the Material
     * @param purchaseAmount Amount of the Material purchased
     * @param player Player who purchased the Material
     */
    public Receipt(@NotNull Material purchased, double purchasePrice, int purchaseAmount, @NotNull OfflinePlayer player) {
        this(purchased, purchasePrice, purchaseAmount, player, new Date());
    }

    /**
     * Constructs a new Market Receipt.
     * @param purchased Material purchased
     * @param purchasePrice Price of the Material
     * @param purchaseAmount Amount of the Material purchased
     * @param player Player who purchased the Material
     * @param timestamp Timestamp of the purchase
     */
    public Receipt(@NotNull Material purchased, double purchasePrice, int purchaseAmount, @NotNull OfflinePlayer player, @NotNull Date timestamp) {
        this.timestamp = timestamp.getTime();
        this.purchased = purchased;
        this.purchaseAmount = purchaseAmount;
        this.purchaserUUID = player.getUniqueId();
        this.purchasePrice = purchasePrice;
        this.purchaser = player;
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
     * Fetches how much the Material was purchased for, without factoring in the {@linkplain #getPurchaseAmount() amount}.
     * @return Price of the Material purchased
     */
    public double getPurchaseSubtotal() {
        return purchasePrice;
    }

    /**
     * Fetches the amount of the Material purchased.
     * @return Amount of the Material purchased
     */
    public int getPurchaseAmount() {
        return purchaseAmount;
    }

    /**
     * Fetches the total price of the purchase.
     * @return Total Price of the purchase
     */
    public double getPurchasePrice() {
        return purchasePrice * purchaseAmount;
    }

    /**
     * Fetches the timestamp of the purchase.
     * @return Timestamp of the purchase
     */
    @NotNull
    public Date getTimestamp() {
        return new Date(timestamp);
    }

    /**
     * <p>Checks if the purchase was recent.</p>
     * <p>The definition of a "recent purchase" means that the purchase was made before the next restock event.</p>
     * @return true if the purchase was recent, false otherwise
     */
    public boolean isRecent() {
        return getTimestamp().getTime() > System.currentTimeMillis() - (NovaConfig.getMarket().getMarketRestockInterval() * 500);
    }

    // Serialization

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.purchaser = Bukkit.getOfflinePlayer(purchaserUUID);
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .put("timestamp", timestamp)
                .put("purchased", purchased.name())
                .put("amount", purchaseAmount)
                .put("purchaser", purchaserUUID.toString())
                .put("price", purchasePrice)
                .build();
    }

    /**
     * Deserializes a Receipt from a Map.
     * @param map Map to deserialize from
     * @return Deserialized Receipt
     */
    @NotNull
    public static Receipt deserialize(@NotNull Map<String, Object> map) {
        return new Receipt(
                Material.valueOf((String) map.get("purchased")),
                (double) map.get("price"),
                (int) map.get("amount"),
                Bukkit.getOfflinePlayer(UUID.fromString((String) map.get("purchaser"))),
                new Date((long) map.get("timestamp"))
        );
    }

}
