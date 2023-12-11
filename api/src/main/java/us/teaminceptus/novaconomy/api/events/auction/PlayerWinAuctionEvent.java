package us.teaminceptus.novaconomy.api.events.auction;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.auction.AuctionProduct;

/**
 * Called when a player wins a bid on an item from the auction house
 */
public class PlayerWinAuctionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final OfflinePlayer purchaser;
    private final AuctionProduct purchased;

    /**
     * Constructs a new PlayerWinAuctionEvent.
     * @param purchaser The player who purchased the item
     * @param purchased The item that was purchased
     */
    public PlayerWinAuctionEvent(@NotNull OfflinePlayer purchaser, @NotNull AuctionProduct purchased) {
        this.purchaser = purchaser;
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

    /**
     * Gets the player that won the bid.
     * @return The player that won the bid
     */
    @NotNull
    public OfflinePlayer getPurchaser() {
        return purchaser;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
