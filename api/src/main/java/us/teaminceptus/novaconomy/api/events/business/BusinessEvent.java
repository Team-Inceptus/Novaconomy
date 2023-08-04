package us.teaminceptus.novaconomy.api.events.business;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

/**
 * Represents an Event with a Business involved
 */
public abstract class BusinessEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Business b;

    /**
     * Constructs a BusinessEvent.
     * @param b Business involved
     */
    public BusinessEvent(Business b) {
        this.b = b;
    }

    /**
     * Constructs a BusinessEvent.
     * @param b Business involved
     * @param async Whether the event is asynchronous or not
     */
    public BusinessEvent(Business b, boolean async) {
        super(async);
        this.b = b;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fetches the Event Handlers.
     * @return Event Handlers
     * @see HandlerList
     */
    public static HandlerList getHandlerList() { return HANDLERS; }

    /**
     * Fetches the BusinessEvent's Business
     * @return Business involved in this event
     */
    @NotNull
    public Business getBusiness() {
        return b;
    }
}
