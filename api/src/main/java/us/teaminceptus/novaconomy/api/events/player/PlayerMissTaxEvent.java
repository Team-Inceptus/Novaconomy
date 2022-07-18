package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called when a Player does not have enough to pay their automatic tax
 */
public class PlayerMissTaxEvent extends Event {

    private final double needed;
    private final Economy econ;

    private final OfflinePlayer p;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new PlayerMissTaxEvent
     * @param p OfflinePlayer involved
     * @param needed Amount needed to be taken
     * @param econ Economy related to the amount
     */
    public PlayerMissTaxEvent(OfflinePlayer p, double needed, Economy econ) {
        this.p = p;
        this.needed = needed;
        this.econ = econ;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Fetches the amount that would have been taken if the Player had enough
     * @return Amount needed
     */
    public double getAmountTaken() {
        return this.needed;
    }

    /**
     * Fetches the Economy involved in this event
     * @return Economy involved
     */
    @NotNull
    public Economy getEconomy() {
        return this.econ;
    }

    /**
     * Fetches the OfflinePlayer involved in this event
     * @return OfflinePlayer involved
     */
    @NotNull
    public OfflinePlayer getPlayer() { return this.p; }

    /**
     * Fetches the Event handlers
     * @return Event handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
