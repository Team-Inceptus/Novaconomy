package us.teaminceptus.novaconomy.api.events.business;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

/**
 * Called before a Player teleports to a Business's Home.
 */
public class BusinessTeleportHomeEvent extends BusinessEvent implements Cancellable {

    private Location location;
    private final Player player;
    private boolean cancelled;

    /**
     * Constructs a new BusinessTeleportHomeEvent.
     * @param p Player
     * @param business Business
     */
    public BusinessTeleportHomeEvent(@NotNull Player p, @NotNull Business business) {
        super(business);

        this.player = p;
        this.location = business.getHome();
    }

    /**
     * Fetches the Player teleporting to the Business's Home.
     * @return Player Teleporting
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Fetches the Location the Player is teleporting to.
     * @return Location
     */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the Location the Player is teleporting to.
     * @param location Location
     */
    public void setLocation(@NotNull Location location) {
        if (location.getWorld() == null) throw new IllegalArgumentException("Location must have a World!");
        this.location = location;
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
