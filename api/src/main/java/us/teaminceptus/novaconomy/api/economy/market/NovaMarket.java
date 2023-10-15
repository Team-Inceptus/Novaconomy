package us.teaminceptus.novaconomy.api.economy.market;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.market.player.PlayerMarketPurchaseEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

/**
 * Represents the Novaconomy Market
 * @since 1.7.1
 */
public interface NovaMarket {

    /**
     * Fetches the file that the Novaconomy Market information is stored in.
     * @return Market File
     */
    static File getMarketFile() {
        return new File(NovaConfig.getDataFolder(), "market.dat");
    }

    /**
     * Fetches the price of a Material on the Market.
     * @param m Material to fetch
     * @return Price of the Material
     * @throws IllegalArgumentException if the Material is not on the Market
     */
    double getPrice(@NotNull Material m) throws IllegalArgumentException;

    /**
     * Fetches the price of a Material on the Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param econ Economy to use
     * @return Price of the Material for the Economy
     * @throws IllegalArgumentException if the Material is not on the Market
     */
    default double getPrice(@NotNull Material m, @Nullable Economy econ) throws IllegalArgumentException {
        return getPrice(m, (econ == null ? 1 : econ.getConversionScale()));
    }

    /**
     * Fetches the price of a Material on the Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param m Material to fetch
     * @param scale Scale to use
     * @return Price of the Material for the Economy
     * @throws IllegalArgumentException if the Material is not on the Market
     */
    default double getPrice(@NotNull Material m, double scale) throws IllegalArgumentException {
        return getPrice(m) / scale;
    }

    /**
     * Fetches whether a material is currently sold on the Novaconomy Market.
     * @param m Material to check
     * @return true if sold, else false
     */
    default boolean isSold(@NotNull Material m) {
        try {
            getPrice(m);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Fetches an immutable set of all of the items sold on the Novaconomy Market.
     * @return Set of all sold items
     */
    @NotNull
    Set<Material> getAllSold();

    /**
     * Fetches the amount of stock of a Material on the Market that is available to purchase.
     * @param m Material to fetch
     * @return Stock of the Material
     */
    long getStock(@NotNull Material m);

    /**
     * Sets the amount of stock of a Material on the Market.
     * @param m Material to set
     * @param stock Stock to set
     * @throws IllegalArgumentException if stock is negative
     */
    void setStock(@NotNull Material m, long stock) throws IllegalArgumentException;

    /**
     * Sets the amount of stock for Materials on the Market.
     * @param materials Materials to set
     * @param stock Stock to set
     * @throws IllegalArgumentException if stock is negative
     */
    void setStock(@NotNull Iterable<Material> materials, long stock) throws IllegalArgumentException;

    /**
     * Adds stock to a Material on the Market.
     * @param m Material to add
     * @param add Stock to add
     */
    default void addStock(@NotNull Material m, long add) {
        setStock(m, getStock(m) + add);
    }

    /**
     * Removes stock from a Material on the Market.
     * @param m Material to remove
     * @param remove Stock to remove
     */
    default void removeStock(@NotNull Material m, long remove) {
        setStock(m, getStock(m) - remove);
    }

    /**
     * Buys a Material from the Market. This method does not add the material to their inventory, only makes a receipt.
     * @param buyer Player buying the Material
     * @param m Material to buy
     * @param amount Amount of the Material to buy
     * @param econ Economy to buy with
     * @return Receipt of the Transaction
     * @throws IllegalArgumentException if amount is not positive, not enough stock, or player does not have enough money
     * @throws CancellationException if {@link PlayerMarketPurchaseEvent} was cancelled
     */
    @NotNull
    Receipt buy(@NotNull OfflinePlayer buyer, @NotNull Material m, int amount, @NotNull Economy econ) throws IllegalArgumentException, CancellationException;

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
     * Fetches an unmodifiable configuration-set price of all of the Materials.
     * @return Map of Material to Price
     */
    @NotNull
    Map<Material, Double> getPriceOverrides();

    /**
     * Sets the configuration-set price of all of the Materials.
     * @param overrides Map of Material to Price
     * @throws IllegalArgumentException if map is null, materials are null, or prices are negative
     */
    void setPriceOverrides(@NotNull Map<Material, Double> overrides) throws IllegalArgumentException;

    /**
     * Sets the configuration-set price of a Material.
     * @param m Material to set
     * @param price Price
     * @throws IllegalArgumentException if material is null or price is negative
     */
    void setPriceOverrides(@NotNull Material m, double price) throws IllegalArgumentException;

    /**
     * Whether money made from the Novaconomy Market is deposited into the Novaconomy Bank.
     * @return true if enabled, else false
     */
    boolean isDepositEnabled();

    /**
     * Sets whether money made from the Novaconomy Market is deposited into the Novaconomy Bank.
     * @param enabled true if enabled, else false
     */
    void setDepositEnabled(boolean enabled);

    /**
     * Fetches the maximum amount of purchases a player can make in a single day (20 minutes).
     * @return Maximum amount of purchases, or -1 if unlimited
     */
    long getMaxPurchases();

    /**
     * Sets the maximum amount of purchases a player can make in a single day (20 minutes).
     * @param maxPurchases Maximum amount of Purchases (-1 for unlimited)
     */
    void setMaxPurchases(long maxPurchases);

    /**
     * <p>Whether the Market Membership feature is enabled.</p>
     * <p>If enabled, players will have to pay a {@linkplain #getMarketMembershipCost() one-time fee} to gain access to the Novaconomy Market.</p>
     * @return true if enabled, else false
     */
    boolean isMarketMembershipEnabled();

    /**
     * Sets whether the Market Membership feature is enabled.
     * @param enabled true if enabled, else false
     * @see #isMarketMembershipEnabled()
     */
    void setMarketMembershipEnabled(boolean enabled);

    /**
     * Fetches the one-time fee a player must pay to gain access to the Novaconomy Market. Default is {@code 10000.0}.
     * @return Market Membership Cost
     */
    double getMarketMembershipCost();

    /**
     * Fetches the one-time fee a player must pay to gain access to the Novaconomy Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param scale Scale to use
     * @return Market Membership Cost
     */
    default double getMarketMembershipCost(double scale) {
        return getMarketMembershipCost() / scale;
    }

    /**
     * Fetches the one-time fee a player must pay to gain access to the Novaconomy Market, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @param econ Economy to use
     * @return Market Membership Cost
     */
    default double getMarketMembershipCost(@Nullable Economy econ) {
        return getMarketMembershipCost(econ == null ? 1 : econ.getConversionScale());
    }

    /**
     * Sets the base one-time fee a player must pay to gain access to the Novaconomy Market.
     * @param cost Market Membership Cost
     */
    void setMarketMembershipCost(double cost);

    /**
     * Fetches an immutable copy of all of the purchases ever on the Novaconomy Market.
     * @return Set of Receipts
     */
    @NotNull
    Set<Receipt> getAllPurchases();

    /**
     * Fetches an immutable set of all of the {@linkplain Receipt#isRecent() recent purchases}.
     * @return Set of Receipts
     */
    @NotNull
    default Set<Receipt> getRecentPurchases() {
        return ImmutableSet.copyOf(getAllPurchases().stream()
                .filter(Receipt::isRecent)
                .collect(Collectors.toList()));
    }

    /**
     * Fetches the number that is multiplied by the {@linkplain #getPrice(Material) market price} to get the money back from selling the material of the same type.
     * @return Sell Percentage
     */
    double getSellPercentage();

    /**
     * Sets the sell percentage.
     * @param percentage Sell Percentage
     * @throws IllegalArgumentException if percentage is not positive
     * @see #getSellPercentage()
     */
    void setSellPercentage(double percentage) throws IllegalArgumentException;

    /**
     * Fetches whether items players sell on the Novaconomy Market will be added to that item's stock.
     * @return true if enabled, else false
     */
    boolean isSellStockEnabled();

    /**
     * Sets whether sell stock is enabled.
     * @param enabled true if enabled, else false
     * @see #isSellStockEnabled()
     */
    void setSellStockEnabled(boolean enabled);

    /**
     * Fetches the timestamp for when the last restock event occured.
     * @return Last Restock Timestamp, or null if hasn't restocked yet
     */
    @Nullable
    Date getLastRestockTimestamp();

    /**
     * Fetches an immutable list of all of the blacklisted materials that won't be sold on the Novaconomy Market.
     * @return List of Materials
     */
    @NotNull
    List<Material> getBlacklistedMaterials();

    /**
     * Adds a blacklisted material to the Novaconomy Market.
     * @param material Material to add
     */
    default void addBlacklistedMaterial(@NotNull Material material) {
        setBlacklistedMaterials(Iterables.concat(getBlacklistedMaterials(), Collections.singleton(material)));
    }

    /**
     * Removes a blacklisted material from the Novaconomy Market.
     * @param material Material to remove
     */
    default void removeBlacklistedMaterial(@NotNull Material material) {
        setBlacklistedMaterials(getBlacklistedMaterials()
                .stream()
                .filter(m -> m != material)
                .collect(Collectors.toList())
        );
    }

    /**
     * Sets the blacklisted materials that won't be sold on the Novaconomy Market.
     * @param materials Iterable of Materials
     */
    void setBlacklistedMaterials(@NotNull Iterable<Material> materials);

    /**
     * Sets the blacklisted materials that won't be sold on the Novaconomy Market.
     * @param materials Array of Materials
     */
    default void setBlacklistedMaterials(@NotNull Material... materials) {
         if (materials != null) setBlacklistedMaterials(Arrays.asList(materials));
    }

    /**
     * Fetches an immutable set of all of the items additionally added to the Novaconomy Market.
     * @return Set of Market Items
     */
    @NotNull
    Set<MarketItem> getCustomItems();

    /**
     * Sets the custom items that will be added to the Novaconomy Market.
     * @param items Map of Materials to their Price
     */
    void setCustomItems(@NotNull Iterable<MarketItem> items);

    /**
     * Adds a custom item to the Novaconomy Market.
     * @param material Material to add
     * @param price Price of the Material
     */
    default void addCustomItem(@NotNull Material material, @NotNull MarketCategory category, double price) {
        setCustomItems(ImmutableSet.<MarketItem>builder()
                .addAll(getCustomItems())
                .add(new MarketItem(material, category, price))
                .build()
        );
    }

    /**
     * Remove a custom item from the Novaconomy Market.
     * @param material Material to remove
     */
    void removeCustomItem(@NotNull Material material);

    /**
     * Fetches an immutable set of all economies allowed to be used when buying from the Novaconomy Market.
     * @return Set of Economies
     */
    @NotNull
    Set<Economy> getWhitelistedEconomies();

    /**
     * Sets the economies allowed to be used when buying from the Novaconomy Market.
     * @param economies Iterable of Economies
     */
    void setWhitelistedEconomies(@NotNull Iterable<Economy> economies);

    /**
     * Sets the economies disallowed to be used when buying from the Novaconomy Market.
     * @return Set of Economies
     */
    @NotNull
    Set<Economy> getBlacklistedEconomies();

    /**
     * Sets the economies disallowed to be used when buying from the Novaconomy Market.
     * @param economies Iterable of Economies
     */
    void setBlacklistedEconomies(@NotNull Iterable<Economy> economies);

}
