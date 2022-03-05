package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;

import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called when a Player pays another player
 */
public class PlayerPayEvent extends PlayerChangeBalanceEvent {

	private Player payer;

	public PlayerPayEvent(Player target, Player payer, Economy econ, double paid, double previousBal, double newBal) {
		super(target, econ, paid, previousBal, newBal, false);
		this.payer = payer;
	}
	
	/**
	 * Get the person that is paying the target ({@link PlayerChangeBalanceEvent#getPlayer()})
	 * @return Player that is paying 
	 */
	public Player getPayer() {
		return this.payer;
	}

}