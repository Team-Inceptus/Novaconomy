package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Called after a Player withdraws from the Global Bank
 */
public class PlayerWithdrawEvent extends PlayerEconomyEvent {

    private final long timestamp;

    /**
     * Constructs a new PlayerWithdrawEvent
     * @param who       Player involved
     * @param amount    Amount withdrawn
     * @param econ      Economy involved
     * @param timestamp Timestamp of the withdraw
     */
    public PlayerWithdrawEvent(Player who, double amount, Economy econ, long timestamp) {
        super(who, amount, econ);
        this.timestamp = timestamp;
    }

    /**
     * Fetches the system milliseconds timestamp of the withdraw
     * @return System Milliseconds
     */
    @NotNull
    public long getTimestamp() {
        return timestamp;
    }
}
