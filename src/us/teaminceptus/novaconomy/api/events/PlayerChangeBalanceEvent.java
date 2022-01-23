package us.teaminceptus.novaconomy.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import us.teaminceptus.novaconomy.api.economy.Economy;

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

	public boolean isNatural() {
		return this.isNatural;
	}

	public double getPreviousBalance() {
		return this.previousBal;
	}

	public double getNewBalance() {
		return this.newBal;
	}

	public boolean isCancelled() {
		return this.isCancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
}