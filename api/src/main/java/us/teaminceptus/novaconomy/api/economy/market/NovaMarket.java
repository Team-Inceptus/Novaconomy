package us.teaminceptus.novaconomy.api.economy.market;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Represents the Novaconomy Market
 * @since 1.7.1
 */
public interface NovaMarket {

    /**
     * Fetches the uninflated base price of a Material on the Market.
     * @param m Material to fetch
     * @return Base Price of the Material
     * @throws IllegalArgumentException if the Material is not on the Market
     */
    double getBasePrice(@NotNull Material m) throws IllegalArgumentException;

    /**
     * Fetches the uninflated base price of a Material on the Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param scale Economy Conversion Scale fto use
     * @return Base Price of the Material for the Economy
     * @throws IllegalArgumentException if the Material is not on the Market
     */
    default double getBasePrice(@NotNull Material m, double scale) throws IllegalArgumentException {
        return getBasePrice(m) * scale;
    }

    /**
     * Fetches the uninflated base price of a Material on the Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param econ Economy to use
     * @return Base Price of the Material for the Economy
     * @throws IllegalArgumentException if the Material is not on the Market
     */
    default double getBasePrice(@NotNull Material m, @Nullable Economy econ) throws IllegalArgumentException {
        return getBasePrice(m) * (econ == null ? 1 : econ.getConversionScale());
    }

    /**
     * Fetches the price of a Material on the Market.
     * @param m Material to fetch
     * @return Base Price of the Material
     */
    double getPrice(@NotNull Material m);

    /**
     * Fetches the price of a Material on the Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param econ Economy to use
     * @return Price of the Material for the Economy
     */
    default double getPrice(@NotNull Material m, @Nullable Economy econ) {
        return getPrice(m) * (econ == null ? 1 : econ.getConversionScale());
    }

    /**
     * Fetches the price of a Material on the Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param scale Scale to use
     * @return Price of the Material for the Economy
     */
    default double getPrice(@NotNull Material m, double scale) {
        return getPrice(m) * scale;
    }

    /**
     * Fetches the amount of stock of a Material on the Market that is available to purchase.
     * @param m Material to fetch
     * @return Stock of the Material
     */
    long getStock(@NotNull Material m);

    /**
     * Buys a Material from the Market. This method does not add the material to their inventory, only makes a receipt.
     * @param buyer Player buying the Material
     * @param m Material to buy
     * @param amount Amount of the Material to buy
     * @param econ Economy to buy with
     * @return Receipt of the Transaction
     * @throws IllegalArgumentException if amount is not positive, not enough stock, or player does not have enough money
     */
    @NotNull
    Receipt buy(@NotNull OfflinePlayer buyer, @NotNull Material m, int amount, @NotNull Economy econ) throws IllegalArgumentException;

    // Configuration

    /**
     * Whether Novaconomy Market features are currently enabled.
     * @return true if enabled, else false
     */
    boolean isMarketEnabled();

    /**
     * Sets whether Novaconomy Market features are currently enabled.
     * @param enabled true if enabled, else false
     */
    void setMarketEnabled(boolean enabled);

    /**
     * Whether the Novaconomy Market automatically restocks itself.
     * @return true if enabled, else false
     */
    boolean isMarketRestockEnabled();

    /**
     * Sets whether the Novaconomy Market automatically restocks itself.
     * @param enabled true if enabled, else false
     */
    void setMarketRestockEnabled(boolean enabled);

    /**
     * Fetches the interval at which the Novaconomy Market automatically restocks itself.
     * @return Restock interval
     */
    long getMarketRestockInterval();

    /**
     * Sets the interval at which the Novaconomy Market automatically restocks itself.
     * @param interval Restock interval
     */
    void setMarketRestockInterval(long interval);

    /**
     * Fetches the base amount of how much the Novaconomy Market restocks.
     * @return Base Restock Amount
     */
    long getMarketRestockAmount();

    /**
     * Sets the base amount of how much the Novaconomy Market restocks.
     * @param amount Base Restock Amount
     */
    void setMarketRestockAmount(long amount);

    /**
     * <p>Whether the Market's restock amount is influenced by how much money is currently in the Novaconomy Bank.</p>
     * <strong>No actual money will be taken or given to the Bank.</strong>
     * @return true if enabled, else false
     */
    boolean hasMarketBankInfluence();

    /**
     * Sets whether the Market's restock amount is influenced by how much money is currently in the Novaconomy Bank.
     * @param enabled true if enabled, else false
     * @see #hasMarketBankInfluence()
     */
    void setMarketBankInfluence(boolean enabled);

    /**
     * Fetches an unmodifiable configuration-set base price of all of the Materials.
     * @return Map of Material to Base Price
     */
    @NotNull
    Map<Material, Double> getBasePriceOverrides();

    /**
     * Sets the configuration-set base price of all of the Materials.
     * @param overrides Map of Material to Base Price
     * @throws IllegalArgumentException if map is null, materials are null, or prices are negative
     */
    void setBasePriceOverrides(@NotNull Map<Material, Double> overrides) throws IllegalArgumentException;

    /**
     * Sets the configuration-set base price of a Material.
     * @param m Material to set
     * @param price Base Price
     * @throws IllegalArgumentException if material is null or price is negative
     */
    void setBasePriceOverride(@NotNull Material m, double price) throws IllegalArgumentException;
}
