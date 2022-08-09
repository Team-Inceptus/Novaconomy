package us.teaminceptus.novaconomy.api.events.business;

import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;

/**
 * Called when a Business's Statistics Change
 */
public class BusinessStatisticChangeEvent extends BusinessEvent {

    private final BusinessStatistics before;
    private final BusinessStatistics after;

    /**
     * Constructs a BusinessStatisticChangeEvent.
     * @param before The BusinessStatistics Object before the change
     * @param after The BusinessStatistics Object after the change
     */
    public BusinessStatisticChangeEvent(BusinessStatistics before, BusinessStatistics after) {
        super(Business.getById(before.getBusinessId()));

        this.before = before;
        this.after = after;
    }

    /**
     * Fetches the BusinessStatistics Object before the change.
     * @return The BusinessStatistics Object before the statistical change
     */
    @Nullable
    public BusinessStatistics getBefore() {
        return before;
    }

    /**
     * Fetches the BusinessStatistics Object after the change.
     * @return The BusinessStatistics Object after the statistical change
     */
    @Nullable
    public BusinessStatistics getAfter() {
        return after;
    }
}
