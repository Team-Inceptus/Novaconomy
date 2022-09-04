package us.teaminceptus.novaconomy.api.player;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;

import java.util.*;

/**
 * Represents NovaPlayer Statistics
 */
public final class PlayerStatistics implements ConfigurationSerializable {

    private final OfflinePlayer player;

    double highestBalance;

    Economy highestBalanceEconomy = null;

    private int productsPurchased;

    double totalWithdrawn;

    double moneyAdded;

    int totalBountiesCreated;

    int totalBountiesHad;

    int totalSharesPurchased;

    double totalMoneySpent;

    private final List<BusinessStatistics.Transaction> transactionHistory = new ArrayList<>();

    PlayerStatistics(OfflinePlayer player) {
        this.player = player;
    }

    /**
     * Deserializes a Map into a PlayerStatistics object.
     * @param serial Map to deserialize
     * @return PlayerStatistics object
     * @throws IllegalArgumentException if map is missing keys or malformed
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static PlayerStatistics deserialize(@NotNull Map<String, Object> serial) throws IllegalArgumentException {
        PlayerStatistics stats = new PlayerStatistics(Bukkit.getOfflinePlayer(UUID.fromString(serial.get("player").toString())));

        try {
            stats.highestBalance = (double) serial.getOrDefault("highest_balance", 0);
            stats.highestBalanceEconomy = serial.containsKey("highest_balance_economy") ? Economy.getEconomy(UUID.fromString(serial.get("highest_balance_economy").toString())) : null;
            stats.productsPurchased = (int) serial.getOrDefault("products_purchased", 0);
            stats.moneyAdded = (double) serial.getOrDefault("money_added", 0);
            stats.totalWithdrawn = (double) serial.getOrDefault("total_withdrawn", 0);
            stats.totalBountiesCreated = (int) serial.getOrDefault("total_bounties_created", 0);
            stats.totalBountiesHad = (int) serial.getOrDefault("total_bounties_had", 0);

            Object purchased = serial.getOrDefault("total_shares_purchased", 0);
            stats.totalSharesPurchased = purchased instanceof Double ? ((Double) purchased).intValue() : (int) purchased;
            stats.totalMoneySpent = (double) serial.getOrDefault("total_money_spent", 0);

            stats.transactionHistory.addAll((List<BusinessStatistics.Transaction>) serial.get("transaction_history"));
        } catch (ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }

        return stats;
    }

    /**
     * Fetches the player that these statistics belong to
     * @return Player that owns these Statistics
     */
    @NotNull
    public OfflinePlayer getPlayer() {
        return player;
    }

    /**
     * Fetches the total amount of Products Purchased.
     * @return Total amount of Products Purchased
     */
    public int getProductsPurchased() {
        return productsPurchased;
    }

    /**
     * Fetches the total amount of money added to this Player's account.
     * @return Total Money Added
     */
    public double getTotalMoneyAdded() {
        return moneyAdded;
    }

    /**
     * Fetches the total amount of money this player has spent.
     * @return Total Money Spent
     */
    public double getTotalMoneySpent() {
        return totalMoneySpent;
    }

    /**
     * Creates a Price representation of the highest balance this Player has ever had.
     * @return Price representation of the highest balance this Player has ever had
     */
    @Nullable
    public Price getHighestBalance() {
        if (highestBalanceEconomy == null || highestBalance <= 0) return null;
        return new Price(highestBalanceEconomy, highestBalance);
    }

    /**
     * Fetches the total amount of money withdrawn from the Bank.
     * @return Total Amount this Player has withdrawn from the Bank
     */
    public double getTotalWithdrawn() {
        return totalWithdrawn;
    }

    /**
     * Fetches the total amount of bounties this Player has created.
     * @return Total amount of bounties this Player has created
     */
    public int getTotalBountiesCreated() {
        return totalBountiesCreated;
    }

    /**
     * Fetches the total amount of bounties this Player has been the target of.
     * @return Total amount of bounties this Player has been the target of
     */
    public int getTotalBountiesTargeted() {
        return totalBountiesHad;
    }

    /**
     * Sets the amount of Products Purchased.
     * @param productsPurchased Amount of Products Purchased
     */
    public void setProductsPurchased(int productsPurchased) {
        this.productsPurchased = productsPurchased;
    }

    /**
     * Fetches the total amount of Market Shares purchased.
     * @return Total amount of Market Shares purchased
     */
    public int getTotalSharesPurchased() {
        return totalSharesPurchased;
    }

    /**
     * Fetches an immutable version of the last 10 transactions this Player has made.
     * @return Last 10 transactions this Player has made
     */
    @NotNull
    public List<BusinessStatistics.Transaction> getTransactionHistory() {
        return ImmutableList.copyOf(transactionHistory);
    }

    /**
     * Sets the current transaction history for this Player.
     * @param history Transaction history to set
     */
    public void setTransactionHistory(@NotNull List<BusinessStatistics.Transaction> history) {
        if (history == null) return;
        this.transactionHistory.clear();
        this.transactionHistory.addAll(history.subList(0, Math.min(history.size(), 10)));
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("player", player.getUniqueId().toString());
            put("highest_balance", highestBalance);
            put("products_purchased", productsPurchased);
            put("money_added", moneyAdded);
            put("total_withdrawn", totalWithdrawn);
            put("total_bounties_created", totalBountiesCreated);
            put("total_bounties_had", totalBountiesHad);
            put("transaction_history", transactionHistory);
            put("total_shares_purchased", totalSharesPurchased);
            put("total_money_spent", totalMoneySpent);

            if (highestBalanceEconomy != null) put("highest_balance_economy", highestBalanceEconomy.getUniqueId().toString());
        }};
    }
}
