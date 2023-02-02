package us.teaminceptus.novaconomy.api.events.corporation;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.corporation.Corporation;

/**
 * Called when a Corporation has been created
 */
public class CorporationCreateEvent extends CorporationEvent {

    /**
     * Constructs a CorporationCreateEvent.
     * @param c Corporation Created
     */
    public CorporationCreateEvent(@NotNull Corporation c) {
        super(c);
    }

}
