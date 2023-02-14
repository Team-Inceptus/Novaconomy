package us.teaminceptus.novaconomy.api.events.corporation;

import org.bukkit.event.Cancellable;

import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Called before a Corporation's Experience <strong>Naturally</strong> Changes
 */
public class CorporationExperienceChangeEvent extends CorporationEvent implements Cancellable {

    private final double oldExperience;
    private double newExperience;
    private boolean cancelled;

    /**
     * Constructs a CorporationExperienceChangeEvent.
     * @param corporation The Corporation whose Experience is changing
     * @param oldExperience The Corporation's Experience before the change
     * @param newExperience The Corporation's Experience after the change
     */
    public CorporationExperienceChangeEvent(Corporation corporation, double oldExperience, double newExperience) {
        super(corporation);
        this.oldExperience = oldExperience;
        this.newExperience = newExperience;
    }

    /**
     * Gets the Corporation's Experience before the change.
     * @return Corporation's Experience before the change
     */
    public double getOldExperience() {
        return oldExperience;
    }

    /**
     * Gets the Corporation's Experience after the change.
     * @return Corporation's Experience after the change
     */
    public double getNewExperience() {
        return newExperience;
    }

    /**
     * Sets the Corporation's Experience after the change.
     * @param newExperience Corporation's Experience after the change
     */
    public void setNewExperience(double newExperience) {
        this.newExperience = newExperience;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
