package us.teaminceptus.novaconomy.api.business;

import com.google.common.base.Preconditions;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a Business Rating
 */
public final class Rating implements ConfigurationSerializable, Comparable<Rating> {

    private final OfflinePlayer owner;

    private final UUID businessId;

    private final int level;

    private final String comment;

    /**
     * Constructs a Rating.
     * @param owner Owner of the Rating
     * @param businessId ID of the Business that the Rating is for
     * @param level Level of the Rating
     * @param comment Optional Comment on the rating
     * @throws IllegalArgumentException if owner / business is null, or level is negative / greater than 5
     */
    public Rating(@NotNull OfflinePlayer owner, @NotNull UUID businessId, int level, String comment) throws IllegalArgumentException {
        Preconditions.checkNotNull(owner, "Owner cannot be null");
        Preconditions.checkNotNull(businessId, "Business cannot be null");
        Preconditions.checkArgument(level >= 0 && level < 5, "Level must be between 0 and 5");

        this.owner = owner;
        this.businessId = businessId;
        this.level = level;
        this.comment = comment;
    }

    /**
     * Constructs a Rating.
     * @param owner Owner of the Rating
     * @param business Business that the Rating is for
     * @param level Level of the Rating
     * @param comment Optional comment on the Rating
     * @throws IllegalArgumentException if owner / business is null, or level is negative / greater than 5
     */
    public Rating(@NotNull OfflinePlayer owner, @NotNull Business business, int level, String comment) throws IllegalArgumentException {
        this(owner, business.getUniqueId(), level, comment);
    }

    /**
     * Constructs a Rating.
     * @param owner Owner of the Rating
     * @param businessId ID of the Business that the Rating is for
     * @param level Level of the Rating
     * @throws IllegalArgumentException if owner / business is null, or level is negative / greater than 5
     */
    public Rating(@NotNull OfflinePlayer owner, @NotNull UUID businessId, int level) throws IllegalArgumentException {
        this(owner, businessId, level, "");
    }

    /**
     * Constructs a Rating.
     * @param owner Owner of the Rating
     * @param business Business that the Rating is for
     * @param level Level of the Rating
     * @throws IllegalArgumentException if owner / business is null, or level is negative / greater than 5
     */
    public Rating(@NotNull OfflinePlayer owner, @NotNull Business business, int level) throws IllegalArgumentException {
        this(owner, business.getUniqueId(), level);
    }

    /**
     * Fetches the owner of this Rating.
     * @return Owner of the Rating
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return owner;
    }

    /**
     * Fetches the Business ID that this Rating is for.
     * @return Business that this rating is targeting
     */
    @NotNull
    public UUID getBusinessId() {
        return businessId;
    }

    /**
     * Fetches the rating level that this Rating is at.
     * @return Rating level
     */
    public int getRatingLevel() {
        return level;
    }

    /**
     * Fetches the comment on this Rating.
     * @return Comment on the Rating, may be empty
     */
    @NotNull
    public String getComment() {
        return comment;
    }

    @Override
    public int compareTo(@NotNull Rating r) {
        return Integer.compare(level, r.level);
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("owner", owner);
            put("business", businessId.toString());
            put("level", level);
            put("comment", comment);
        }};
    }

    /**
     * Deserializes a Map into a Rating.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized Rating, or null if serial is null
     * @throws IllegalArgumentException if an argument is missing/malformed
     */
    public static Rating deserialize(@NotNull Map<String, Object> serial) throws IllegalArgumentException {
        try {
            return new Rating(
                    (OfflinePlayer) serial.get("owner"),
                    UUID.fromString((String) serial.get("business")),
                    (int) serial.get("level"),
                    (String) serial.get("comment"));
        } catch (NullPointerException | ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
