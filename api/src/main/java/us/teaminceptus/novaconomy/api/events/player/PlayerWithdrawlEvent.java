package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called after a Player withdrawls from the Global Bank
 */
public class PlayerWithdrawlEvent extends PlayerEconomyEvent {

    private final long timestamp;

    /**
     * Constructs a new PlayerWithdrawlEvent
     * @param who       Player involved
     * @param amount    Amount withdrawln
     * @param econ      Economy involved
     * @param timestamp Timestamp of the withdrawl
     */
    public PlayerWithdrawlEvent(Player who, double amount, Economy econ, long timestamp) {
        super(who, amount, econ);
        this.timestamp = timestamp;
    }

    /**
     * Fetches the system milliseconds timestamp of the withdrawl
     * @return System Milliseconds
     */
    @NotNull
    public long getTimestamp() {
        return timestamp;
    }
}
