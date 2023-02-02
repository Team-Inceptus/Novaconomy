package us.teaminceptus.novaconomy.api.economy.market;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an ownable Stock
 */
public final class Stock implements ConfigurationSerializable {

    private final OfflinePlayer owner;
    private final long purchaseDate;
    private final double initialPrice;

    /**
     * Creates a new Stock with the timestamp of {@link System#currentTimeMillis()}.
     * @param owner The owner of the Stock
     * @param initialPrice How much the stock was originally purchased for
     */
    public Stock(@NotNull OfflinePlayer owner, double initialPrice) {
        this(owner, initialPrice, new Date());
    }

    /**
     * Creates a new Stock.
     * @param owner The owner of the Stock
     * @param initialPrice How much the stock was originally purchased for
     * @param purchaseDate The date the Stock was purchased
     * @throws IllegalArgumentException if the owner, type, or purchaseDate is null, and if the initialPrice is not positive
     */
    public Stock(@NotNull OfflinePlayer owner, double initialPrice, @NotNull Date purchaseDate) throws IllegalArgumentException {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        if (purchaseDate == null) throw new IllegalArgumentException("purchaseDate cannot be null");
        if (initialPrice <= 0) throw new IllegalArgumentException("initialPrice must be positive");

        this.owner = owner;
        this.purchaseDate = purchaseDate.getTime();
        this.initialPrice = initialPrice;
    }

    /**
     * Fetches the owner of this Stock.
     * @return The owner of this Stock
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return owner;
    }

    /**
     * Fetches the date this Stock was purchased.
     * @return Stock Purchase Date
     */
    @NotNull
    public Date getPurchaseDate() {
        return new Date(purchaseDate);
    }

    /**
     * Fetches how much the Stock was originally purchased for.
     * @return Stock Purchase Price
     */
    public double getInitialPrice() {
        return initialPrice;
    }

    // Serialization

    /**
     * Deserializes a Stock from a Map.
     * @param map The Map to deserialize from
     * @return The deserialized Stock
     */
    @NotNull
    public static Stock deserialize(@NotNull Map<String, Object> map) {
        return new Stock(
                (OfflinePlayer) map.get("owner"),
                (double) map.get("initial_price"),
                new Date((long) map.get("purchase_date"))
        );
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("owner", owner);
            put("initial_price", initialPrice);
            put("purchase_date", purchaseDate);
        }};
    }
}
