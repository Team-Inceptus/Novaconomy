package us.teaminceptus.novaconomy.api.events.market;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all market events.
 */
public abstract class MarketEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Creates a new MarketEvent.
     */
    protected MarketEvent() {
        this( false);
    }

    /**
     * Creates a new MarketEvent.
     * @param async Whether the event is asynchronous.
     */
    protected MarketEvent(boolean async) {
        super(async);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fetches the HandlerList for this event.
     * @return HandlerList
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
