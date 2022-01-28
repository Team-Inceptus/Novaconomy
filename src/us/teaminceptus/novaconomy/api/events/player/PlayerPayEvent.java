package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;

import us.teaminceptus.novaconomy.api.economy.Economy;

public class PlayerPayEvent extends PlayerChangeBalanceEvent {

	private Player payer;

	public PlayerPayEvent(Player target, Player payer, Economy econ, double paid, double previousBal, double newBal) {
		super(target, econ, paid, previousBal, newBal, false);
		this.payer = payer;
	}

	public Player getPayer() {
		return this.payer;
	}

}