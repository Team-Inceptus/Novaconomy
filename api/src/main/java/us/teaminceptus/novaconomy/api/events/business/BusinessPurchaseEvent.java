package us.teaminceptus.novaconomy.api.events.business;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;

/**
 * Called when a Product is Purchased
 */
public class BusinessPurchaseEvent extends BusinessEvent {

    private final BusinessStatistics.Transaction transaction;

    /**
     * Constructs a BusinessPurchaseEvent.
     * @param t Transaction that was made
     */
    public BusinessPurchaseEvent(BusinessStatistics.Transaction t) {
        super(t.getProduct().getBusiness());

        this.transaction = t;
    }

    /**
     * Returns the Transaction that was made.
     * @return Transaction that was made
     */
    @NotNull
    public BusinessStatistics.Transaction getTransaction() {
        return transaction;
    }
}
