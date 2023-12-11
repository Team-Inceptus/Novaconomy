package us.teaminceptus.novaconomy.api.events.corporation;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Called before a Business is unbanned from a Corporation.
 */
public class CorporationUnbanEvent extends CorporationEvent implements Cancellable {

    private final Business unbanned;
    private boolean cancelled;

    public CorporationUnbanEvent(@NotNull Corporation corporation, @NotNull Business unbanned) {
        super(corporation);
        if (unbanned == null) throw new IllegalArgumentException("unbanned cannot be null");
        this.unbanned = unbanned;
    }

    /**
     * Gets the business that was banned.
     * @return The business that was banned.
     */
    @NotNull
    public Business getUnbanned() {
        return unbanned;
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