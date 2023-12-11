package us.teaminceptus.novaconomy.api.events.business;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

import java.util.HashSet;
import java.util.Set;

/**
 * Called before a Business receives an automatic supply from a container, adding the items to its resources.
 */
public class BusinessSupplyEvent extends BusinessEvent implements Cancellable {

    private final Location location;
    private final Set<Chest> containers;
    private final Set<ItemStack> items = new HashSet<>();
    private boolean isCancelled;

    /**
     * Creates a new BusinessSupplyEvent.
     * @param b Business Supplied
     * @param containers Array of Containers Supplied From
     */
    public BusinessSupplyEvent(@NotNull Business b, @NotNull Iterable<? extends ItemStack> items, @NotNull Chest... containers) {
        this(b, items, ImmutableSet.copyOf(containers));
    }

    /**
     * Creates a new BusinessSupplyEvent.
     * @param b Business Supplied
     * @param containers Iterable of Containers Supplied From
     */
    public BusinessSupplyEvent(@NotNull Business b, @NotNull Iterable<? extends ItemStack> items, @NotNull Iterable<? extends Chest> containers) {
        super(b);

        this.items.addAll(ImmutableSet.copyOf(items));
        this.containers = ImmutableSet.copyOf(containers);
        this.location = containers.iterator().next().getLocation();
    }

    /**
     * Fetches the Location of the container involved in this event.
     * @return Location of Container
     */
    @NotNull
    public Location getLocation() {
        return location;
    }

    /**
     * Fetches an immutable set of the Chest Containers that will have the items taken from.
     * @return Chest Containers
     */
    @NotNull
    public Set<Chest> getContainers() {
        return containers;
    }

    /**
     * Fetches a mutable set of the ItemStacks that will be added to the Business's resources.
     * @return Mutable Set of Items
     */
    @NotNull
    public Set<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    /**
     * Supported intervals for the Supply Event to take place
     */
    public enum Interval {

        /**
         * Every 10 Minutes
         */
        TEN_MINUTES(600 * 20),

        /**
         * Every 20 Minutes / Every MC Day
         */
        TWENTY_MINUTES(1200 * 20),

        /**
         * Every 30 Minutes
         */
        THIRTY_MINUTES(1800 * 20),

        /**
         * Every Hour
         */
        HOUR(3600 * 20),

        /**
         * Every 2 Hours
         */
        TWO_HOURS(2 * 3600 * 20),

        /**
         * Every 4 Hours
         */
        FOUR_HOURS(4 * 3600 * 20),

        /**
         * Every 12 Hours
         */
        TWELVE_HOURS(12 * 3600 * 20),

        /**
         * Every Day (24 hours)
         */
        DAY(3600 * 24 * 20),

        ;

        private final long ticks;

        Interval(long ticks) { this.ticks = ticks; }

        /**
         * Fetches the amount of ticks in between each supply event for this Interval.
         * @return Ticks
         */
        public long getTicks() {return ticks;}
    }
}
