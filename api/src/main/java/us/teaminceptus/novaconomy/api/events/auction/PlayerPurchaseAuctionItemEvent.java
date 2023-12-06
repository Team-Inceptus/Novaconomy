package us.teaminceptus.novaconomy.api.events.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.auction.AuctionItem;

/**
 * Called when a player purchases an item from the auction house
 */
public class PlayerPurchaseAuctionItemEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AuctionItem purchased;

    /**
     * Gets the item that was purchased.
     * @param purchaser The player who purchased the item
     * @param purchased The item that was purchased
     */
    public PlayerPurchaseAuctionItemEvent(@NotNull Player purchaser, @NotNull AuctionItem purchased) {
        super(purchaser);
        this.purchased = purchased;
    }

    /**
     * Gets the item that was purchased.
     * @return The item that was purchased
     */
    @NotNull
    public AuctionItem getPurchased() {
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
