package us.teaminceptus.novaconomy.api.auction;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an item in the Novaconomy Auction House.
 */
public final class AuctionItem implements Serializable {

    private static final long serialVersionUID = 6553572638722360815L;

    /**
     * The duration of an auction in milliseconds.
     */
    public static final long AUCTION_DURATION = 1000 * 60 * 60 * 24 * 7; // 7 days

    /**
     * The duration of a buy now item in milliseconds.
     */
    public static final long BUY_NOW_DURATION = 1000 * 60 * 60 * 24 * 5; // 3 days

    private final UUID uuid;
    private final OfflinePlayer owner;
    private final long postedTimestmap;
    private final ItemStack item;
    private final Price price;
    private final boolean buyNow;
    private final boolean loosePrice;

    AuctionItem(UUID uuid, OfflinePlayer owner, long postedTimestmap, ItemStack item, Price price, boolean buyNow, boolean loosePrice) {
        this.uuid = uuid;
        this.owner = owner;
        this.postedTimestmap = postedTimestmap;
        this.item = item;
        this.price = price;
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
        return owner;
    }

    /**
     * Gets the price of the auction item.
     * @return The price of the auction item.
     */
    @NotNull
    public Price getPrice() {
        return price;
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
     * Gets the item being sold.
     * @return The item being sold.
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Gets the timestamp of when the auction item was posted.
     * @return Timestamp of Posted Time.
     */
    @NotNull
    public Date getPostedTimestmap() {
        return new Date(postedTimestmap);
    }

    /**
     * Gets whether the current Auction Item has expired.
     * @return true if the auction item has expired, false otherwise.
     */
    public boolean isExpired() {
        long millis = System.currentTimeMillis() - postedTimestmap;
        return buyNow ? millis > BUY_NOW_DURATION : millis > AUCTION_DURATION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionItem that = (AuctionItem) o;
        return postedTimestmap == that.postedTimestmap && buyNow == that.buyNow && loosePrice == that.loosePrice && Objects.equals(uuid, that.uuid) && Objects.equals(owner, that.owner) && Objects.equals(item, that.item) && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
