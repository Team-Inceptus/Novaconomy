package us.teaminceptus.novaconomy.api.business;

import com.google.common.collect.ImmutableMap;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a Product owned by a Business
 */
public final class BusinessProduct extends Product {

    private static final long serialVersionUID = 7186744343709160963L;

    private Business business;

    /**
     * Constructs a Business Product.
     * @param item Item to use
     * @param price Price to use
     * @param business Business to use
     * @throws IllegalArgumentException if item, price, or business is null
     */
    public BusinessProduct(@NotNull ItemStack item, @NotNull Price price, @NotNull Business business) throws IllegalArgumentException {
        super(item, price);
        if (business == null) throw new IllegalArgumentException("Business cannot be null");
        this.business = business;
    }

    /**
     * Constructs a Business Product.
     * @param item Item to use
     * @param econ Economy Price is using
     * @param amount Amount Price is selling at
     * @param business Business to use
     * @throws IllegalArgumentException if item, price, or economy is null / amount is less than or greater than 0
     */
    public BusinessProduct(@NotNull ItemStack item, @NotNull Economy econ, double amount, @NotNull Business business) throws IllegalArgumentException{
        this(item, new Price(econ, amount), business);
    }

    /**
     * Constructs a Business Product from a Product and Business.
     * @param pr Product to use
     * @param business Business to use
     * @throws IllegalArgumentException if product or business is null
     */
    public BusinessProduct(@NotNull Product pr, @NotNull Business business) throws IllegalArgumentException {
        super(pr == null ? null : pr.getItem(), pr == null ? null : pr.getPrice());
        if (business == null) throw new IllegalArgumentException("Business cannot be null");
        this.business = business;
    }

    /**
     * Fetches the BusinessProduct's Business.
     * @return Business that owns this BusinessProduct
     */
    @NotNull
    public Business getBusiness() {
        return business;
    }

    /**
     * Sets the BusinessProduct's Business.
     * @param business Business to set
     */
    public void setBusiness(@NotNull Business business) {
        if (business == null) throw new IllegalArgumentException("Business cannot be null");
        this.business = business;
    }

    @Override
    public BusinessProduct setPrice(@NotNull Price price) throws IllegalArgumentException {
        super.setPrice(price);
        this.business.saveBusiness();
        return this;
    }

    @Override
    public BusinessProduct setItem(@NotNull ItemStack item) throws IllegalArgumentException {
        super.setItem(item);
        this.business.saveBusiness();
        return this;
    }

    @Override
    public String toString() {
        return "BusinessProduct{" +
                "business=" + business +
                ", item=" + item +
                ", price=" + price +
                '}';
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .putAll(super.serialize())
                .put("business", business.getUniqueId().toString())
                .build();
    }

    /**
     * Deserializes a Map into a BusinessProduct.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized BusinessProduct
     * @throws IllegalArgumentException if a argument is missing or
     */
    @Nullable
    public static BusinessProduct deserialize(@Nullable Map<String, Object> serial) throws IllegalArgumentException {
        if (serial == null) return null;

        try {
            return new BusinessProduct(Product.deserialize(serial), Business.byId(UUID.fromString((String) serial.get("business") )));
        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
