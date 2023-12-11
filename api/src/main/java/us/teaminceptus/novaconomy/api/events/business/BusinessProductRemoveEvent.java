package us.teaminceptus.novaconomy.api.events.business;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;

/**
 * Called when a Business Product is removed
 */
public class BusinessProductRemoveEvent extends BusinessEvent {

    private final BusinessProduct product;

    /**
     * Constructs a BusinessProductRemoveEvent.
     * @param b Product Removed
     */
    public BusinessProductRemoveEvent(BusinessProduct b) {
        super(b.getBusiness());
        this.product = b;
    }

    /**
     * Fetches the Product removed.
     * @return Product Removed
     */
    @NotNull
    public BusinessProduct getProduct() {
        return product;
    }
}
