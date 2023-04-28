package us.teaminceptus.novaconomy.api.events.market.player;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.events.market.MarketEvent;

/**
 * Represents a Market Event that involves a player.
 */
public class PlayerMarketEvent extends MarketEvent {

    private final OfflinePlayer player;

    /**
     * Constructs a PlayerMarketEvent.
     * @param player Player Involved
     */
    public PlayerMarketEvent(@Nullable OfflinePlayer player) {
        this.player = player;
    }

    /**
     * Gets the player involved in this event.
     * @return Player Involved
     */
    @Nullable
    public OfflinePlayer getPlayer() {
        return player;
    }
}
