package us.teaminceptus.novaconomy.api.corporation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Represents a Permission a Business can have in a Corporation.
 */
public enum CorporationPermission {

    /**
     * Permission to add resources to the Corporation's Global Supply.
     */
    ADD_RESOURCE,

    /**
     * Permission to deposit resources from the Corporation's Global Supply into own Business Resources.
     */
    DEPOSIT_RESOURCE,


    ;

    private final String id;
    private final boolean defaultState;

    CorporationPermission() {
        this(true);
    }

    CorporationPermission(boolean defaultState) {
        this.id = name().toLowerCase();
        this.defaultState = defaultState;
    }

    /**
     * Fetches this Permission's ID.
     * @return Permission Id
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Fetches this Permission's default state.
     * @return true if given by default, false otherwise
     */
    public boolean getDefaultState() {
        return defaultState;
    }

    /**
     * Fetches a Permission by its ID.
     * @param id Permission ID
     * @return CorporationPermission found, or null if not found
     */
    @Nullable
    public static CorporationPermission byId(@Nullable String id) {
        if (id == null) return null;

        return Arrays.stream(values())
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
