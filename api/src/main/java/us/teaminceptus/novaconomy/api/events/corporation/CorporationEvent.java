package us.teaminceptus.novaconomy.api.events.corporation;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Represents an event involving a Corporation
 */
public abstract class CorporationEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * The corporation involved in this event.
     */
    protected final Corporation corporation;

    CorporationEvent(@NotNull Corporation c) {
        this.corporation = c;
    }

    /**
     * Fetches the Corporation involved in this event.
     * @return Corporation Involved
     */
    @NotNull
    public Corporation getCorporation() {
        return corporation;
    }

    /**
     * Gets the handlers for this event.
     * @return Event HandlerList
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
