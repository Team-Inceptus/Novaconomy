package us.teaminceptus.novaconomy.api.events.corporation;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Called before a Business is kicked from a Corporation. This is also called if the Business leaves on its own.
 */
public class CorporationKickEvent extends CorporationEvent implements Cancellable {

    private final Business kicked;
    private boolean cancelled;

    public CorporationKickEvent(@NotNull Corporation corporation, @NotNull Business kicked) {
        super(corporation);
        if (kicked == null) throw new IllegalArgumentException("kicked cannot be null");
        this.kicked = kicked;
    }

    /**
     * Gets the business that was kicked.
     * @return The business that was kicked.
     */
    @NotNull
    public Business getKicked() {
        return kicked;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
