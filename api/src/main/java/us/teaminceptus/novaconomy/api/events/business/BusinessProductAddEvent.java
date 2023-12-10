package us.teaminceptus.novaconomy.api.events.business;

import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;

/**
 * Called before a Business Product is created
 */
public class BusinessProductAddEvent extends BusinessEvent implements Cancellable {

    private BusinessProduct product;

    private boolean isCancelled;

    /**
     * Constructs a BusinessProductAddEvent.
     * @param pr Product used
     * @throws NullPointerException if Product is null
     */
    public BusinessProductAddEvent(@NotNull BusinessProduct pr) throws NullPointerException {
        super(pr.getBusiness());
        this.product = pr;
        this.isCancelled = false;
    }

    /**
     * Fetches the BusinessProduct involved in this event.
     * @return Product added
     */
    @NotNull
    public BusinessProduct getProduct() {
        return product;
    }

    /**
     * Fetches if the product shouldn't be created.
     * @return true if not created, else false
     */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Sets if the product shouldn't be created.
     * @param cancelled true if not created, else false
     */
    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    /**
     * Sets the Product to be added.
     * @param product Product that will be added
     */
    public void setProduct(@NotNull BusinessProduct product) {
        if (product == null) return;
        this.product = product;
    }
}
