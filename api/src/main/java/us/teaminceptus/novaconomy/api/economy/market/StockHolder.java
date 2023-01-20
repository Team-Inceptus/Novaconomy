package us.teaminceptus.novaconomy.api.economy.market;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Represents something that can be bought and sold on the Stock Market using {@link Stock}s
 */
public interface StockHolder {

    /**
     * Fetches the current price of this StockHolder.
     * @return Stock's Current Price, with no modification from a conversion scale
     */
    double getStockPrice();

    /**
     * Fetches the current price of this StockHolder.
     * @param economy The economy to use for conversion
     * @return Stock's Current Price, with a conversion scale applied
     */
    default double getStockPrice(@NotNull Economy economy) {
        return getStockPrice() * economy.getConversionScale();
    }

    /**
     * Fetches the limit of how many shares can be bought of this StockHolder.
     * @return Stock Purchase Limit
     */
    int getStockLimit();

    /**
     * Sets the limit of how many shares can be bought of this StockHolder.
     * @param limit Stock Purchase Limit
     */
    void setStockLimit(int limit);

    /**
     * Whether stocks can be bought from this StockHolder.
     * @return true if stocks can be purchased, false otherwise
     */
    default boolean canPurchase() {
        return getStockLimit() > 0;
    }

}
