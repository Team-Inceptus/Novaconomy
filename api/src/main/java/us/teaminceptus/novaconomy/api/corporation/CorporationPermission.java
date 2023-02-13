package us.teaminceptus.novaconomy.api.corporation;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Represents a Permission a Business can have in a Corporation.
 */
public enum CorporationPermission {

    /**
     * Permission to invite other Businesses to the Corporation.
     */
    INVITE_BUSINESS(Material.PAPER),

    /**
     * Permission to teleport to the Corporation's Headquarters.
     */
    TELEPORT_HEADQUARTERS(Material.ENDER_PEARL),

    ;

    private final Material icon;
    private final String id;
    private final boolean defaultState;

    CorporationPermission(Material icon) {
        this(icon, true);
    }

    CorporationPermission(Material icon, boolean defaultState) {
        this.id = name().toLowerCase();
        this.icon = icon;
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
     * Fetches this Permission's icon.
     * @return Material icon
     */
    @NotNull
    public Material getIcon() {
        return icon;
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
