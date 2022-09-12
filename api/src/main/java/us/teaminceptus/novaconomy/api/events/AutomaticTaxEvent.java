package us.teaminceptus.novaconomy.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Called when Automatic Tax Happens
 */
public class AutomaticTaxEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;

    private final Map<NovaPlayer, Map<Economy, Double>> previousBalances;
    private final Map<NovaPlayer, Map<Economy, Double>> newBalances;
    private final Map<NovaPlayer, Map<Economy, Double>> taxAmount;

    /**
     * Constructs an AutomaticTaxEvent.
     * @param previousBalances A Map of Players to the economies affected and their previous balances
     * @param taxAmount A Map of Players to the economies affected and the amount removed
     */
    public AutomaticTaxEvent(Map<NovaPlayer, Map<Economy, Double>> previousBalances, Map<NovaPlayer, Map<Economy, Double>> taxAmount) {
        this.previousBalances = previousBalances;
        this.taxAmount = taxAmount;

        Map<NovaPlayer, Map<Economy, Double>> newBalances = new HashMap<>();

        for (Map.Entry<NovaPlayer, Map<Economy, Double>> entry : previousBalances.entrySet()) {
            Map<Economy, Double> balEntry = new HashMap<>();
            for (Map.Entry<Economy, Double> econEntry : entry.getValue().entrySet()) balEntry.put(econEntry.getKey(), econEntry.getValue() - taxAmount.get(entry.getKey()).get(econEntry.getKey()));

            newBalances.put(entry.getKey(), balEntry);
        }

        this.newBalances = newBalances;
    }

    /**
     * Whether the Tax Event should not happen.
     * @return true if tax event is cancelled, else false
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether the Tax Event should not happen.
     * @param b true if tax event is cancelled, else false
     */
    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    /**
     * Fetches all of the previous balances for all players.
     * @return A Map of Players to their previous balances.
     */
    @NotNull
    public Map<NovaPlayer, Map<Economy, Double>> getAllPreviousBalances() {
        return previousBalances;
    }

    /**
     * Fetches all of the new balances for all players.
     * @return A Map of Players to their new balances.
     */
    @NotNull
    public Map<NovaPlayer, Map<Economy, Double>> getAllNewBalances() {
        return newBalances;
    }

    /**
     * Fetches all of the tax amounts for all players.
     * @return A Map of Players to their tax amounts.
     */
    @NotNull
    public Map<NovaPlayer, Map<Economy, Double>> getAllTaxAmount() {
        return taxAmount;
    }

    /**
     * Fetches a NovaPlayer's previous balance.
     * @param np The NovaPlayer to fetch the balance for.
     * @return A Map of Economies to their amounts for their previous balance, or null if the NovaPlayer is not in the event.
     * @throws IllegalArgumentException if NovaPlayer is null
     */
    @Nullable
    public Map<Economy, Double> getPreviousBalance(@NotNull NovaPlayer np) throws IllegalArgumentException {
        if (np == null) throw new IllegalArgumentException("NovaPlayer cannot be null");
        return previousBalances.get(np);
    }

    /**
     * Fetches a NovaPlayer's new balance.
     * @param np The NovaPlayer to fetch the balance for.
     * @return A Map of Economies to their amounts for their new balance, or null if the NovaPlayer is not in the event.
     * @throws IllegalArgumentException if NovaPlayer is null
     */
    @Nullable
    public Map<Economy, Double> getNewBalance(@NotNull NovaPlayer np) throws IllegalArgumentException {
        if (np == null) throw new IllegalArgumentException("NovaPlayer cannot be null");
        return newBalances.get(np);
    }

    /**
     * Fetches a NovaPlayer's tax amount.
     * @param np The NovaPlayer to fetch the tax amount for.
     * @return A Map of Economies to their amounts for their tax amount, or null if the NovaPlayer is not in the event.
     * @throws IllegalArgumentException if NovaPlayer is null
     */
    @Nullable
    public Map<Economy, Double> getTaxAmount(@NotNull NovaPlayer np) throws IllegalArgumentException {
        if (np == null) throw new IllegalArgumentException("NovaPlayer cannot be null");
        return taxAmount.get(np);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fetches the Event's HandlerList.
     * @return The Event's HandlerList.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
