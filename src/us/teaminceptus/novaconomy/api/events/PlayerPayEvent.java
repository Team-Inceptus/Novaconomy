package us.teaminceptus.novaconomy.api.events;

import org.bukkit.entity.Player;

public class PlayerPayEvent extends PlayerChangeBalanceEvent {

	private boolean isCancelled;
	private double previousBal;
	private double newBal;
	private Player payer;

	public PlayerPayEvent(Player target, Player payer, Economy econ, double paid, double previousBal, double newBal) {
		super(target, econ, paid, previousBal, newBal, false);
		this.payer = payer;
		this.isCancelled = false;
		this.previousBal = previousBal;
		this.newBal = newBal;
	}

	public Player getPayer() {
		return this.payer;
	}

}