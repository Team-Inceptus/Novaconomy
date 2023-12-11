package us.teaminceptus.novaconomy.api.events.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.auction.AuctionProduct;

/**
 * Called when a player purchases a buy now item from the auction house
 */
public class PlayerPurchaseAuctionItemEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AuctionProduct purchased;

    /**
     * Gets the item that was purchased.
     * @param purchaser The player who purchased the item
     * @param purchased The item that was purchased
     */
    public PlayerPurchaseAuctionItemEvent(@NotNull Player purchaser, @NotNull AuctionProduct purchased) {
        super(purchaser);
        this.purchased = purchased;
    }

    /**
     * Gets the item that was purchased.
     * @return The item that was purchased
     */
    @NotNull
    public AuctionProduct getPurchased() {
        return purchased;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
