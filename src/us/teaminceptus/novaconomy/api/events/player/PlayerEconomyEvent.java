package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import us.teaminceptus.novaconomy.api.economy.Economy;

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

	public double getAmount() {
		return this.amount;
	}

	public Economy getEconomy() {
		return this.econ;
	}
}