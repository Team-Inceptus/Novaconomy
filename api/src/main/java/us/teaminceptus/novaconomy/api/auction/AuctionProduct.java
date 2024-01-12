package us.teaminceptus.novaconomy.api.auction;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an item in the Novaconomy Auction House.
 */
public final class AuctionProduct extends Product implements Serializable {

    private static final long serialVersionUID = 6553572638722360815L;

    /**
     * The duration of an auction in milliseconds.
     */
    public static final long AUCTION_DURATION = 1000 * 60 * 60 * 24 * 5; // 5 days

    /**
     * The duration of a buy now item in milliseconds.
     */
    public static final long BUY_NOW_DURATION = 1000 * 60 * 60 * 24 * 5; // 3 days

    /**
     * The date format for auction expiration.
     */
    public static final SimpleDateFormat EXPIRATION_FORMAT = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a z");

    private final UUID uuid;
    private final UUID owner;
    private final long postedTimestamp;
    private final boolean buyNow;
    private final boolean loosePrice;

    AuctionProduct(UUID uuid, UUID owner, long postedTimestamp, ItemStack item, Price price, boolean buyNow, boolean loosePrice) {
        super(item, price);
        this.uuid = uuid;
        this.owner = owner;
        this.postedTimestamp = postedTimestamp;
        this.buyNow = buyNow;
        this.loosePrice = loosePrice;
    }

    /**
     * Gets the ID of the auction item.
     * @return The ID of the auction item.
     */
    @NotNull
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Gets the owner of the auction item.
     * @return The owner of the auction item.
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(owner);
    }

    /**
     * Gets whether the price of the auction item is loose, meaning any economy can be used to pay for it.
     * @return Whether the price of the auction item is loose.
     */
    public boolean isLoosePrice() {
        return loosePrice;
    }

    /**
     * Gets whether the auction item is a buy now item, meaning it can be automatically purchased.
     * @return Whether the auction item is a buy now item.
     */
    public boolean isBuyNow() {
        return buyNow;
    }

    /**
     * Gets the timestamp of when the auction item was posted.
     * @return Timestamp of Posted Time.
     */
    @NotNull
    public Date getPostedTimestamp() {
        return new Date(postedTimestamp);
    }

    /**
     * Gets the timestamp of when the auction item will expire.
     * @return Timestamp of Expiration Time.
     */
    @NotNull
    public Date getExpirationTimestamp() {
        return new Date(postedTimestamp + (buyNow ? BUY_NOW_DURATION : AUCTION_DURATION));
    }

    /**
     * Gets whether the current Auction Item has expired.
     * @return true if the auction item has expired, false otherwise.
     */
    public boolean isExpired() {
        long millis = System.currentTimeMillis() - postedTimestamp;
        return buyNow ? millis > BUY_NOW_DURATION : millis > AUCTION_DURATION;
    }

    /**
     * Gets the name of the auction item.
     * @return The name of the auction item.
     */
    @NotNull
    public String getItemName() {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            return item.getItemMeta().getDisplayName();

        return WordUtils.capitalizeFully(item.getType().name().replace("_", " ").toLowerCase());
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .putAll(super.serialize())
                .put("uuid", uuid.toString())
                .put("owner", owner.toString())
                .put("posted", postedTimestamp)
                .put("buyNow", buyNow)
                .put("loose", loosePrice)
                .build();
    }

    AuctionProduct cloneWithPrice(Price price) {
        return new AuctionProduct(uuid, owner, postedTimestamp, item, price, buyNow, loosePrice);
    }

    /**
     * Deserializes an AuctionItem from a Map.
     * @param map Map to deserialize from
     * @return Deserialized AuctionItem
     */
    @NotNull
    public static AuctionProduct deserialize(@NotNull Map<String, Object> map) {
        return new AuctionProduct(
                UUID.fromString((String) map.get("uuid")),
                UUID.fromString((String) map.get("owner")),
                (long) map.get("posted"),
                (ItemStack) map.get("item"),
                (Price) map.get("price"),
                (boolean) map.get("buyNow"),
                (boolean) map.get("loose")
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionProduct that = (AuctionProduct) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "AuctionItem{" +
                "uuid=" + uuid +
                ", owner=" + getOwner() +
                ", postedTimestmap=" + postedTimestamp +
                ", buyNow=" + buyNow +
                ", loosePrice=" + loosePrice +
                ", item=" + item +
                ", price=" + price +
                '}';
    }
}
