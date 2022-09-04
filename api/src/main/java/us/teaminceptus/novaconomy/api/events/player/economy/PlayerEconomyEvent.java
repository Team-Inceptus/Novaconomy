package us.teaminceptus.novaconomy.api.events.player.economy;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * An Economy-Related Event involving a Player
 *
 */
public abstract class PlayerEconomyEvent extends PlayerEvent {

    private final double amount;
    private final Economy econ;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Represents an Economy-Related Event
     * @param who Player involved
     * @param amount Amount involved
     * @param econ Economy involved
     */
    public PlayerEconomyEvent(Player who, double amount, @Nullable Economy econ) {
        super(who);
        this.amount = amount;
        this.econ = econ;
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
    @NotNull
    public static HandlerList getHanderList() { return HANDLERS; }

    /**
     * Fetch the amount involved in this event
     * @return amount used
     */
    public double getAmount() {
        return this.amount;
    }

    /**
     * Fetch the Economy involved in this event
     * @return {@link Economy} involved
     */
    @Nullable
    public Economy getEconomy() {
        return this.econ;
    }
}