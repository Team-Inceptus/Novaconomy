package us.teaminceptus.novaconomy.api.util;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

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
}
