package us.teaminceptus.novaconomy.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;

/**
 * Called before a CommandTaxEvent is executed.
 */
public class CommandTaxEvent extends Event implements Cancellable {

    private boolean cancelled;

    private NovaConfig.CustomTaxEvent event;

    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a CommandTaxEvent.
     * @param event CustomTaxEvent involved
     */
    public CommandTaxEvent(@NotNull NovaConfig.CustomTaxEvent event) {
        if (event == null) throw new IllegalArgumentException("Event cannot be null");

        this.event = event;
        this.cancelled = false;
    }

    /**
     * Fetches the CustomTaxEvent involved in this event.
     * @return CustomTaxEvent
     */
    @NotNull
    public NovaConfig.CustomTaxEvent getEvent() {
        return event;
    }

    /**
     * Sets the CustomEvent that will be called.
     * @param event CustomEvent to call
     * @throws IllegalArgumentException if event is null
     */
    public void setEvent(@NotNull NovaConfig.CustomTaxEvent event) throws IllegalArgumentException {
        if (event == null) throw new IllegalArgumentException("CustomEvent cannot be null");
        this.event = event;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fetches the HandlerList for this event.
     * @return HandlerList for this event
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
