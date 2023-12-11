package us.teaminceptus.novaconomy.api.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Sellable Product
 */
public class Product implements ConfigurationSerializable, Comparable<Product>, Serializable {

    private static final long serialVersionUID = 5404582861266017032L;

    /**
     * Item to sell
     */
    protected ItemStack item;

    /**
     * Price to sell at
     */
    protected Price price;

    /**
     * Constructs a Product.
     * @param item Item to sell
     * @param price Price to sell at
     * @throws IllegalArgumentException if item or price is null
     */
    public Product(@NotNull ItemStack item, @NotNull Price price) throws IllegalArgumentException {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (price == null) throw new IllegalArgumentException("Price cannot be null");

        this.item = item;
        this.price = price;
    }

    /**
     * Constructs a Product from a Business Product.
     * @param product Business Product to construct from
     * @throws NullPointerException if product is null
     */
    public Product(@NotNull BusinessProduct product) throws NullPointerException {
        this(product.getItem(), product.getPrice());
    }

    /**
     * Constructs a Product.
     * @param item Item to sell
     * @param econ Economy to sell at
     * @param amount Price at selling
     * @throws IllegalArgumentException if item, price, or economy is null / amount is less than or greater than 0
     */
    public Product(@NotNull ItemStack item, @NotNull Economy econ, double amount) throws IllegalArgumentException {
        this(item, new Price(econ, amount));
    }

    /**
     * Fetches the Product's Item.
     * @return Product Item selling
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * Sets the Product's Selling Item.
     * @param item Item to sell
     * @return this Product, for chaining
     * @throws IllegalArgumentException if item is null
     */
    @NotNull
    public Product setItem(@NotNull ItemStack item) throws IllegalArgumentException {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        this.item = item;
        return this;
    }

    /**
     * Fetches the Product's Price.
     * @return Price Product is selling at
     */
    @NotNull
    public Price getPrice() {
        return price;
    }

    /**
     * Fetches the Product's Economy.
     * @return Economy Product is purchased with
     */
    @Nullable
    public Economy getEconomy() {
        return price.getEconomy();
    }

    /**
     * Fetches the Product's Price Amount.
     * @return Price Amount Product is selling at
     */
    @NotNull
    public double getAmount() {
        return price.getAmount();
    }

    /**
     * Converts this Product to an immutable Map Entry.
     * @return Immutable Map Entry
     */
    @NotNull
    public Map.Entry<ItemStack, Price> toEntry() {
        return Maps.immutableEntry(this.item, this.price);
    }

    /**
     * Sets the Proudct's selling price.
     * @param price Price Selling at
     * @return this Product, for chaining
     * @throws IllegalArgumentException if Price is null
     */
    @NotNull
    public Product setPrice(@NotNull Price price) throws IllegalArgumentException {
        Validate.notNull(price, "Price cannot be null");
        this.price = price;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .put("price", price)
                .put("item", item)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return item.equals(product.item) && price.equals(product.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, price);
    }

    @Override
    public String toString() {
        return "Product{" +
                "item=" + item +
                ", price=" + price +
                '}';
    }

    /**
     * Deserializes a Product into a Map.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized Product, or null if serial is null
     * @throws IllegalArgumentException if a part is missing or malformed
     */
    @Nullable
    public static Product deserialize(@Nullable Map<String, Object> serial) throws IllegalArgumentException {
        if (serial == null) return null;

        try {
            return new Product((ItemStack) serial.get("item"), (Price) serial.get("price"));
        } catch (ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int compareTo(@NotNull Product p) {
        return Double.compare(this.getAmount(), p.getAmount());
    }
}
