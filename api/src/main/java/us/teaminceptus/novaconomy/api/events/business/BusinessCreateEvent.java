package us.teaminceptus.novaconomy.api.events.business;

import us.teaminceptus.novaconomy.api.business.Business;

/**
 * Called when a Business is created
 */
public class BusinessCreateEvent extends BusinessEvent {

    /**
     * Constructs a BusinessCreateEvent.
     * @param b Business created
     */
    public BusinessCreateEvent(Business b) {
        super(b);
    }
}
