package us.teaminceptus.novaconomy.api.events.business;

import org.bukkit.event.Cancellable;

import us.teaminceptus.novaconomy.api.business.Business;

/**
 * Called when a Business is advertised
 */
public class BusinessAdvertiseEvent extends BusinessEvent implements Cancellable {

    private boolean isCancelled;

    /**
     * Contructs a BusinessAdvertiseEvent.
     * @param b Business to use
     */
    public BusinessAdvertiseEvent(Business b) {
        super(b);
        this.isCancelled = false;
    }

    /**
     * Whether the business should be advertised.
     * @return true if the business should be advertised
     */
    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    /**
     * Sets whether the business should be advertised.
     * @param cancel true if cancelled, else false
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

}