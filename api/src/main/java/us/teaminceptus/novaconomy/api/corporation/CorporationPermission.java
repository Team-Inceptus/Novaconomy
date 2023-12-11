package us.teaminceptus.novaconomy.api.corporation;

/**
 * Represents a permission in a Corporation.
 * @deprecated Draft API
 */
@Deprecated
public enum CorporationPermission {

    /**
     * Permission to edit the name and description of the Corporation.
     */
    EDIT_DETAILS,

    /**
     * Permission to edit the settings of the Corporation.
     */
    EDIT_SETTINGS,

    /**
     * Permission to teleport to the Corporation's headquarters.
     */
    TELEPORT_TO_HEADQUARTERS(true),

    /**
     * Permission to broadcast messages to all members of the Corporation.
     */
    BROADCAST_MESSAGES(true),

    /**
     * Permission to invite businesses to the Corporation.
     */
    INVITE_MEMBERS,

    /**
     * Permission to kick members from the Corporation.
     */
    KICK_MEMBERS,

    /**
     * Permission to ban members from the Corporation.
     */
    BAN_MEMBERS,

    ;

    private final boolean defaultPermission;

    CorporationPermission() {
        this(false);
    }

    CorporationPermission(boolean defaultPermission) {
        this.defaultPermission = defaultPermission;
    }

    /**
     * Returns whether or not this permission is enabled by default.
     * @return true if enabled by default, false otherwise
     */
    public boolean isDefaultPermission() {
        return defaultPermission;
    }
}
