package us.teaminceptus.novaconomy.api.events.market.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a Player's accessibility to the Novaconomy Market changes.
 */
public class PlayerMembershipChangeEvent extends PlayerMarketEvent implements Cancellable {

    private final boolean oldStatus;
    private boolean newStatus;

    private boolean cancelled = false;

    /**
     * Constructs a PlayerMarketEvent.
     * @param player Player Involved
     * @param oldStatus Old Player Membership Status
     */
    public PlayerMembershipChangeEvent(@Nullable OfflinePlayer player, boolean oldStatus, boolean newStatus) {
        super(player);

        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    /**
     * Gets the old status of the player's membership.
     * @return Old Player Membership Status
     */
    public boolean getOldStatus() {
        return oldStatus;
    }

    /**
     * Gets the new status of the player's membership.
     * @return Player Membership Status
     */
    public boolean getNewStatus() {
        return newStatus;
    }

    /**
     * Sets the new status of the player's membership.
     * @param newStatus Player Membership Status
     */
    public void setNewStatus(boolean newStatus) {
        this.newStatus = newStatus;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
