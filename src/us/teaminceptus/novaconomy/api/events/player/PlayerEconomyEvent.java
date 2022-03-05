package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * An Economy-Related Event involving a Player
 *
 */
public abstract class PlayerEconomyEvent extends PlayerEvent {

	private final double amount;
	private final Economy econ;
	private static final HandlerList HANDLERS = new HandlerList();

	public PlayerEconomyEvent(Player who, double amount, Economy econ) {
		super(who);
		this.amount = amount;
		this.econ = econ;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHanderList() {
		return HANDLERS;
	}
	
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
	public Economy getEconomy() {
		return this.econ;
	}
}