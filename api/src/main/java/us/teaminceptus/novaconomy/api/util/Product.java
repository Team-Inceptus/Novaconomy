package us.teaminceptus.novaconomy.api.util;

import com.google.common.collect.Maps;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Business Product
 */
public class Product implements ConfigurationSerializable {

    private ItemStack item;

    private Price price;

    /**
     * Constructs a Business Product.
     * @param item Item to sell
     * @param price Price to sell at
     * @throws IllegalArgumentException if item or price is null
     */
    public Product(@NotNull ItemStack item, @NotNull Price price) throws IllegalArgumentException {
        Validate.notNull(item, "Item cannot be null");
        Validate.notNull(price, "Price cannot be null");
        Validate.notNull(price.getEconomy(), "Price Economy cannot be null");

        this.item = item;
        this.price = price;
    }

    /**
     * Constructs a Business Product.
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
        Validate.notNull(item, "Item cannot be null");
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
    @NotNull
    public Economy getEconomy() {
        return price.getEconomy();
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
        return new HashMap<String, Object>() {{
            put("price", price);
            put("item", item);
        }};
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
}
