package us.teaminceptus.novaconomy.api.events.market.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;

/**
 * Called before a Player purchases an item from the Market.
 */
public class PlayerMarketPurchaseEvent extends PlayerMarketEvent implements Cancellable {

    private final Receipt receipt;
    private boolean cancelled = false;

    /**
     * Constructs a PlayerMarketPurchaseEvent.
     * @param player Player Involved
     * @param receipt Receipt after Purchasing
     */
    public PlayerMarketPurchaseEvent(@Nullable OfflinePlayer player, @Nullable Receipt receipt) {
        super(player);

        this.receipt = receipt;
    }

    /**
     * Fetches the receipt involved in this event.
     * @return Receipt Involved
     */
    @Nullable
    public Receipt getReceipt() {
        return receipt;
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
