package us.teaminceptus.novaconomy.api.events.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.auction.AuctionProduct;
import us.teaminceptus.novaconomy.api.auction.Bid;

/**
 * Called when a player bids on an item in the auction house
 */
public class PlayerBidEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Bid bid;
    private final AuctionProduct bidded;

    /**
     * Constructs a new PlayerBidEvent.
     * @param bidder The player who bidded on the item
     * @param bidded The item that was bidded
     */
    public PlayerBidEvent(@NotNull Player bidder, @NotNull Bid bid, @NotNull AuctionProduct bidded) {
        super(bidder);
        this.bid = bid;
        this.bidded = bidded;
    }

    /**
     * Gets the item that was purchased.
     * @return The item that was purchased
     */
    @NotNull
    public AuctionProduct getBidded() {
        return bidded;
    }

    /**
     * Gets the bid on the item.
     * @return The bid on the item
     */
    @NotNull
    public Bid getBid() {
        return bid;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
