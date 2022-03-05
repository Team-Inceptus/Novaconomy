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
	private boolean isNatural;
	private double newBal;
	private double previousBal;

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