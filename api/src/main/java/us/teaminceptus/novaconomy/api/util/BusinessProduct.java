package us.teaminceptus.novaconomy.api.util;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.business.Business;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a Product owned by a Business
 */
public final class BusinessProduct extends Product {

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
        Validate.notNull(business, "Business cannot be null");
        this.business = business;
    }

    /**
     * Constructs a Business Product from a Product and Business.
     * @param pr Product to use
     * @param business Business to use
     * @throws IllegalArgumentException if product or business is null
     */
    public BusinessProduct(@NotNull Product pr, @NotNull Business business) throws IllegalArgumentException {
        super(pr == null ? null : pr.getItem(), pr == null ? null : pr.getPrice());
        Validate.notNull(business, "Business cannot be null");
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
        Validate.notNull(business, "Business cannot be null");
        this.business = business;
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>(super.serialize()) {{
            put("business", business.getUniqueId().toString());
        }};
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
            return new BusinessProduct(Product.deserialize(serial), Business.getById(UUID.fromString((String) serial.get("business") )));
        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
