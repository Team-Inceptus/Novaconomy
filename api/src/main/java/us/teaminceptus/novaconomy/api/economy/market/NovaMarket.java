package us.teaminceptus.novaconomy.api.economy.market;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Represents the Novaconomy Market
 * @deprecated Draft API
 */
@Deprecated
public interface NovaMarket {

    /**
     * Fetches the price of a Material on the Stock Market.
     * @param m Material to fetch
     * @return Base Price of the Material
     */
    double getPrice(@NotNull Material m);

    /**
     * Fetches the price of a Material on the Stock Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param econ Economy to use
     * @return Price of the Material for the Economy
     */
    default double getPrice(@NotNull Material m, @Nullable Economy econ) {
        return getPrice(m) * econ.getConversionScale();
    }

    /**
     * Fetches the price of a Material on the Stock Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param scale Scale to use
     * @return Price of the Material for the Economy
     */
    default double getPrice(@NotNull Material m, double scale) {
        return getPrice(m) * scale;
    }

    /**
     * Buys a Material from the Market. This method does not add the material to their inventory, only makes a receipt.
     * @param buyer Player buying the Material
     * @param m Material to buy
     * @param amount Amount of the Material to buy
     * @param econ Economy to buy with
     * @return Receipt of the Transaction
     * @throws IllegalArgumentException if amount is not positive, or player does not have enough money
     */
    @NotNull
    Receipt buy(@NotNull OfflinePlayer buyer, @NotNull Material m, int amount, @NotNull Economy econ) throws IllegalArgumentException;

}
