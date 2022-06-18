package us.teaminceptus.novaconomy.api.events.player;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;

/**
 * Called after a Player purchases something
 */
public class PlayerPurchaseProductEvent extends PlayerEconomyEvent {

    private final BusinessProduct product;

    /**
     * Constructs a PlayerPurchaseProductEvent.
     * @param p Player that purchased product
     * @param product BusinessProduct used
     */
    public PlayerPurchaseProductEvent(@NotNull Player p, @NotNull BusinessProduct product) {
        super(p, product.getPrice().getAmount(), product.getEconomy());

        this.product = product;
    }

    /**
     * Fetches the BusinessProduct involved in this event.
     * @return Product player bought
     */
    @NotNull
    public BusinessProduct getProduct() {
        return product;
    }
}
