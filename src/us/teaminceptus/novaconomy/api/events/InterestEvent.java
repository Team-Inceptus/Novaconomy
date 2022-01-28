package us.teaminceptus.novaconomy.api.events;

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

	public Map<NovaPlayer, Map<Economy, Double>> getAllPreviousBalances() {
		return this.previousBalances;
	}

	public Map<Economy, Double> getPreviousBalance(NovaPlayer np) throws IllegalArgumentException {
		if (np == null) throw new IlleaglArgumentException("NovaPlayer cannot be null");
		return this.previousBalances.get(np);
	}

	public Map<NovaPlayer, Map<Economy, Double>> getAllNewBalances() {
		return this.newBalances;
	}

	public Map<Economy, Double> getNewBalance(NovaPlayer np) throws IllegalArgumentException {
		if (np == null) throw new IlleaglArgumentException("NovaPlayer cannot be null");
		return this.newBalances.get(np);
	}

	public Map<NovaPlayer, Map<Economy, Double>> getAllInterestAmounts() {
		return this.interestAmount;
	}

	public Map<Economy, Double> getInterestAmount(NovaPlayer np) throws IllegalArgumentException {
		if (np == null) throw new IlleaglArgumentException("NovaPlayer cannot be null");
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