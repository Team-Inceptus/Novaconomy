package us.teaminceptus.novaconomy.api.events.corporation;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Called before a Corporation is deleted
 */
public class CorporationDeleteEvent extends CorporationEvent {

    /**
     * Constructs a new CorporationDeleteEvent.
     * @param corporation Corporation to delete
     */
    public CorporationDeleteEvent(@NotNull Corporation corporation) {
        super(corporation);
    }

}
