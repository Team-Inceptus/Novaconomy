package us.teaminceptus.novaconomy.api.economy.market;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents an immutable custom Market Item on the Novaconomy Market
 */
public final class MarketItem implements Comparable<MarketItem> {

    private final Material item;
    private final MarketCategory category;
    private final double price;

    /**
     * Constructs a new Market Item.
     * @param item Material of the Market Item to be sold
     * @param category Category of the Market Item
     * @param price Price of the Market Item
     */
    public MarketItem(@NotNull Material item, @NotNull MarketCategory category, double price) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null!");
        if (category == null) throw new IllegalArgumentException("Category cannot be null!");
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative!");

        this.item = item;
        this.category = category;
        this.price = price;
    }

    /**
     * Gets the Material of this Market Item.
     * @return Market Item Material
     */
    @NotNull
    public Material getItem() {
        return item;
    }

    /**
     * Gets the Market Category of this Market Item.
     * @return Market Item Category
     */
    @NotNull
    public MarketCategory getCategory() {
        return category;
    }

    /**
     * Gets the price of this Market Item.
     * @return Market Item Price
     */
    public double getPrice() {
        return price;
    }

    @Override
    public int compareTo(@NotNull MarketItem o) {
        return Double.compare(price, o.price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketItem that = (MarketItem) o;
        return Double.compare(price, that.price) == 0 && item == that.item && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, category, price);
    }

    @Override
    public String toString() {
        return "MarketItem{" +
                "item=" + item +
                ", category=" + category +
                ", price=" + price +
                '}';
    }
}
