package us.teaminceptus.novaconomy.api.events.corporation;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Called before a Corporation bans a member.
 */
public class CorporationBanEvent extends CorporationEvent implements Cancellable {

    private final Business banned;
    private boolean cancelled;

    public CorporationBanEvent(@NotNull Corporation corporation, @NotNull Business banned) {
        super(corporation);
        if (banned == null) throw new IllegalArgumentException("banned cannot be null");
        this.banned = banned;
    }

    /**
     * Gets the business that was banned.
     * @return The business that was banned.
     */
    @NotNull
    public Business getBanned() {
        return banned;
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
