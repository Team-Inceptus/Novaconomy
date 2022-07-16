package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called when a Player does not have enough to pay their automatic tax
 */
public class PlayerMissTaxEvent extends PlayerEvent {

    private final double needed;
    private final Economy econ;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructs a new PlayerMissTaxEvent
     * @param player Player involved
     * @param needed Amount needed to be taken
     * @param econ Economy related to the amount
     */
    public PlayerMissTaxEvent(Player player, double needed, Economy econ) {
        super(player);
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
     * Fetches the Event handlers
     * @return Event handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
