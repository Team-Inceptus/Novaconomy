package us.teaminceptus.novaconomy.api.events.market;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Called before the market is restocked. Normally called Asyncronously.
 */
public class AsyncMarketRestockEvent extends MarketEvent implements Cancellable {

    private final Map<Material, Long> oldStock;
    private final Map<Material, Long> newStock;

    private boolean cancelled = false;

    /**
     * Creates a new AsyncMarketRestockEvent.
     * @param oldStock Map of Material to Old Stock Amount
     * @param newStock Map of Material to New Stock Amount
     */
    public AsyncMarketRestockEvent(@NotNull Map<Material, Long> oldStock, @NotNull Map<Material, Long> newStock) {
        super(true);

        this.oldStock = ImmutableMap.copyOf(oldStock);
        this.newStock = new HashMap<>(newStock);
    }

    /**
     * Fetches how much stock there was before the restock.
     * @return Map of Old Stock Material to Amount
     */
    @NotNull
    public Map<Material, Long> getOldStock() {
        return oldStock;
    }

    /**
     * Fetches how much stock there was before the restock.
     * @param m Material to fetch
     * @return Old Stock Amount
     */
    public long getOldStock(@NotNull Material m) {
        return oldStock.get(m);
    }

    /**
     * Fetches how much stock there will be after the restock.
     * @return Map of New Stock Material to Amount
     */
    @NotNull
    public Map<Material, Long> getNewStock() {
        return newStock;
    }

    /**
     * Fetches how much stock there will be after the restock.
     * @param m Material to fetch
     * @return New Stock Amount
     */
    public long getNewStock(@NotNull Material m) {
        return newStock.get(m);
    }

    /**
     * Sets how much stock there will be after the restock.
     * @param m Material to set
     * @param newStock New Stock Amount
     */
    public void setNewStock(Material m, long newStock) {
        this.newStock.put(m, newStock);
    }

    /**
     * Sets how much stock there will be after the restock.
     * @param newStock New Stock Map
     */
    public void setNewStock(@NotNull Map<Material, Long> newStock) {
        this.newStock.clear();
        this.newStock.putAll(newStock);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
