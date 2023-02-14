package us.teaminceptus.novaconomy.api.events.business;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

/**
 * Called when a Business is Viewed by a player that <strong>does not</strong> own the Business.
 */
public class BusinessViewEvent extends BusinessEvent {

    private final Player viewer;

    /**
     * Constructs a new BusinessViewEvent.
     * @param viewer Player viewing the business
     * @param business Business being viewed
     */
    public BusinessViewEvent(Player viewer, Business business) {
        super(business);
        this.viewer = viewer;
    }

    /**
     * Fetches the viewer of the business.
     * @return Player viewing the business.
     */
    @NotNull
    public Player getViewer() {
        return viewer;
    }
}
