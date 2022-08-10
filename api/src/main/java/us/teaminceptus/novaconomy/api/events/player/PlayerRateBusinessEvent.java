package us.teaminceptus.novaconomy.api.events.player;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.Rating;

/**
 * Called before a Player rates a business
 */
public class PlayerRateBusinessEvent extends PlayerEvent implements Cancellable {

    private final Business business;

    private Rating rating;

    private boolean cancelled;

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a PlayerRateBusinessEvent.
     * @param p         Player rating the business
     * @param business  Business being rated
     * @param rating    Rating being given
     * @throws IllegalArgumentException if business or rating is null
     */
    public PlayerRateBusinessEvent(Player p, @NotNull Business business, @NotNull Rating rating) throws IllegalArgumentException {
        super(p);
        Preconditions.checkNotNull(business, "Business cannot be null");
        Preconditions.checkNotNull(rating, "Rating cannot be null");

        this.business = business;
        this.rating = rating;
        this.cancelled = false;
    }

    /**
     * Fetches the Business involved in this event.
     * @return Business involved
     */
    @NotNull
    public Business getBusiness() {
        return this.business;
    }

    /**
     * Fetches the Rating being given.
     * @return Rating being given
     */
    @NotNull
    public Rating getRating() {
        return rating;
    }

    /**
     * Sets the Rating being given.
     * @param rating Rating being given
     * @throws IllegalArgumentException if rating is null
     */
    public void setRating(@NotNull Rating rating) throws IllegalArgumentException {
        Preconditions.checkNotNull(rating, "Rating cannot be null");
        this.rating = rating;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fecthes the Event Handlers.
     * @return Event Handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
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
