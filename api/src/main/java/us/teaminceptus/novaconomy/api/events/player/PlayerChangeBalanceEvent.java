package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called when a player's balance changes
 *
 */
public class PlayerChangeBalanceEvent extends PlayerEconomyEvent implements Cancellable {

    private boolean isCancelled;
    private final boolean isNatural;
    private final double newBal;
    private final double previousBal;

    /**
     * Called when a Player's Balance Changes
     * @param target Target involved in this event
     * @param econ Economy involved
     * @param amount Amount added
     * @param previousBal Previous Balance
     * @param newBal New Balance (previousBal + amount = newBal is NOT being checked)
     * @param isNatural If this addition was natural
     * @see PlayerEconomyEvent
     */
    public PlayerChangeBalanceEvent(Player target, Economy econ, double amount, double previousBal, double newBal, boolean isNatural) {
        super(target, amount, econ);
        this.newBal = newBal;
        this.isNatural = isNatural;
        this.previousBal = previousBal;
        this.isCancelled = false;
    }

    /**
     * Whether or not this increase was caused by a natural event (i.e. fishing increase)
     * @return true if natural, else false
     */
    public boolean isNatural() {
        return this.isNatural;
    }

    /**
     * Fetch the previous balance of this player
     * @return Previous Balance
     */
    public double getPreviousBalance() {
        return this.previousBal;
    }

    /**
     * Fetch the new balance of this player
     * @return New Balance
     */
    public double getNewBalance() {
        return this.newBal;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }
}