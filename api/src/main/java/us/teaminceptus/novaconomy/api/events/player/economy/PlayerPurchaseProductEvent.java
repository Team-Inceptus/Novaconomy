package us.teaminceptus.novaconomy.api.events.player.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;

/**
 * Called after a Player purchases something
 */
public class PlayerPurchaseProductEvent extends PlayerEconomyEvent {

    private final BusinessProduct product;

    private final BusinessStatistics.Transaction transaction;

    private final int amount;

    /**
     * Constructs a PlayerPurchaseProductEvent.
     * @param p Player that purchased product
     * @param product BusinessProduct used
     * @param transaction The Transaction of the Purchase
     * @param amount Amount of product purchased
     */
    public PlayerPurchaseProductEvent(@NotNull Player p, @NotNull BusinessProduct product, @Nullable BusinessStatistics.Transaction transaction, int amount) {
        super(p, product.getPrice().getAmount(), product.getEconomy());

        this.product = product;
        this.amount = amount;
        this.transaction = transaction;
    }

    /**
     * Fetches the BusinessProduct involved in this event.
     * @return Product player bought
     */
    @NotNull
    public BusinessProduct getProduct() {
        return product;
    }

    /**
     * Fetches the amount of the product purchased.
     * @return Amount of product purchased
     */
    public int getPurchasedAmount() {
        return amount;
    }

    /**
     * Fetches the transaction involved in this event.
     * @return Transaction of the purchase
     */
    @Nullable
    public BusinessStatistics.Transaction getTransaction() {
        return transaction;
    }
}
