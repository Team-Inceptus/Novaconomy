package us.teaminceptus.novaconomy.api.events.business;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;

import java.util.List;

/**
 * Called when a Player attempts to stock a Business
 */
public class BusinessStockEvent extends BusinessEvent {

    private final Player p;
    private final List<ItemStack> extra;
    private final List<ItemStack> added;


    public BusinessStockEvent(@NotNull Business business, Player p, List<ItemStack> extra, List<ItemStack> added) {
        super(business);
        this.p = p;
        this.extra = extra;
        this.added = added;
    }

    /**
     * Get the player involved in this BusinessStockEvent.
     * @return Player involved
     */
    @NotNull
    public Player getPlayer() {
        return p;
    }

    /**
     * Fetches the extra items that were not added.
     * @return List of items that weren't added as stock
     */
    @NotNull
    public List<ItemStack> getExtra() {
        return extra;
    }

    /**
     * Fetches the items that were added.
     * @return List of items that were added as stock.
     */
    @NotNull
    public List<ItemStack> getAdded() {
        return added;
    }
}
