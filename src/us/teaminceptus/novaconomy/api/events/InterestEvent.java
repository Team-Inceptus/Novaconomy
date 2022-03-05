package us.teaminceptus.novaconomy.api.events;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called when Interest happens
 */
public class InterestEvent extends Event implements Cancellable {

	private final Map<NovaPlayer, Map<Economy, Double>> previousBalances;
	private final Map<NovaPlayer, Map<Economy, Double>> newBalances;
	private final Map<NovaPlayer, Map<Economy, Double>> interestAmount;
	
	private boolean isCancelled;
	private static final HandlerList HANDLERS = new HandlerList();

	public InterestEvent(Map<NovaPlayer, Map<Economy, Double>> previousBalances, Map<NovaPlayer, Map<Economy, Double>> interestAmount) {
		this.previousBalances = previousBalances;
		this.interestAmount = interestAmount;

		Map<NovaPlayer, Map<Economy, Double>> newBalances = new HashMap<>();

		for (Map.Entry<NovaPlayer, Map<Economy, Double>> entry : previousBalances.entrySet()) {
			Map<Economy, Double> balEntry = new HashMap<>();

			for (Map.Entry<Economy, Double> econEntry : entry.getValue().entrySet()) {
				balEntry.put(econEntry.getKey(), econEntry.getValue() + interestAmount.get(entry.getKey()).get(econEntry.getKey()));
			}

			newBalances.put(entry.getKey(), balEntry);
		}

		this.newBalances = newBalances;
	}
	
	/**
	 * Fetch a Map of what players were affected to their previous balances.
	 * @return Map of Previous Balances
	 */
	public Map<NovaPlayer, Map<Economy, Double>> getAllPreviousBalances() {
		return this.previousBalances;
	}
	
	/**
	 * Utility Method to get the values affected from this player and their previous balances
	 * @param np Player to use
	 * @return Map of Economies to their Previous Balances
	 * @throws IllegalArgumentException if player is null
	 */
	public Map<Economy, Double> getPreviousBalance(NovaPlayer np) throws IllegalArgumentException {
		if (np == null) throw new IllegalArgumentException("NovaPlayer cannot be null");
		return this.previousBalances.get(np);
	}
	
	/**
	 * Fetch a Map of what players were affected to their new balances.
	 * @return Map of New Balances
	 */
	public Map<NovaPlayer, Map<Economy, Double>> getAllNewBalances() {
		return this.newBalances;
	}
	
	/**
	 * Utility Method to get the values affected from this player and their new balances
	 * @param np Player to use
	 * @return Map of Economies to their New Balances
	 * @throws IllegalArgumentException if player is null
	 */
	public Map<Economy, Double> getNewBalance(NovaPlayer np) throws IllegalArgumentException {
		if (np == null) throw new IllegalArgumentException("NovaPlayer cannot be null");
		return this.newBalances.get(np);
	}
	
	/**
	 * Fetch a Map of what players were affected to how much was added to each balance
	 * @return Map of Interest Amount
	 */
	public Map<NovaPlayer, Map<Economy, Double>> getAllInterestAmounts() {
		return this.interestAmount;
	}
	
	/**
	 * Utility Method to get the values affected from this player and the amount added to their balance
	 * @param np Player to use
	 * @return Map of Economies to their additions
	 * @throws IllegalArgumentException if player is null
	 */
	public Map<Economy, Double> getInterestAmount(NovaPlayer np) throws IllegalArgumentException {
		if (np == null) throw new IllegalArgumentException("NovaPlayer cannot be null");
		return this.interestAmount.get(np);
	}

	public boolean isCancelled() {
		return this.isCancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}
	
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHanderList() {
		return HANDLERS;
	}
}