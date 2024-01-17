package us.teaminceptus.novaconomy.api.corporation;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a permission in a Corporation.
 */
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
    BROADCAST_MESSAGES,

    /**
     * Permission to invite businesses to the Corporation.
     */
    INVITE_MEMBERS,

    /**
     * Permission to kick members from the Corporation.
     */
    KICK_MEMBERS,

    /**
     * Permission to ban and unban members from the Corporation.
     */
    BAN_MEMBERS,

    /**
     * Permission to edit and delete the ranks of the Corporation, up to (exclusively) the rank of the user.
     */
    MANAGE_RANKS,

    /**
     * Permission to create new ranks in the Corporation, with its priority under the rank of the user.
     */
    CREATE_RANKS,

    /**
     * Permission to change the ranks of specific users in the Corporation, up to (exclusively) the rank of the user.
     */
    CHANGE_USER_RANKS,

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

    /**
     * Gets an immutable copy of all default Corporation permissions.
     * @return all default Corporation permissions
     */
    @NotNull
    public static Set<CorporationPermission> getDefaultPermissions() {
        return ImmutableSet.copyOf(Arrays.stream(values())
                .filter(CorporationPermission::isDefaultPermission)
                .collect(Collectors.toSet()));
    }
}
