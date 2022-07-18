package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called before a Player deposits into the Global Bank
 */
public class PlayerDepositEvent extends PlayerEconomyEvent {

    private final long timestamp;
    private double amount;

    /**
     * Constructs a PlayerDepositEvent.
     *
     * @param who       Player involved
     * @param amount    Amount involved
     * @param econ      Economy involved
     * @param timestamp Timestamp of the deposit
     */
    public PlayerDepositEvent(Player who, double amount, Economy econ, long timestamp) {
        super(who, amount, econ);

        this.amount = amount;
        this.timestamp = timestamp;
    }

    /**
     * Sets the amount deposited into the global bank.
     * @param amount Amount deposited
     * @throws IllegalArgumentException if amount is negative
     */
    public void setAmount(double amount) throws IllegalArgumentException {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        this.amount = amount;
    }

    /**
     * Fetches the amount deposited into the global bank.
     * @return Amount deposited
     */
    @Override
    public double getAmount() {
        return amount;
    }

    /**
     * Fetches the system milliseconds timestamp of the deposit
     * @return System Milliseconds
     */
    public long getTimestamp() {
        return this.timestamp;
    }
}
