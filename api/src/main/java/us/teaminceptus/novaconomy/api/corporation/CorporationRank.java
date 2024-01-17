package us.teaminceptus.novaconomy.api.corporation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a rank in a Corporation.
 */
public final class CorporationRank implements Serializable, ConfigurationSerializable {

    private static final long serialVersionUID = 5906646942872358140L;

    /**
     * The maximum length of a rank's name.
     */
    public static final int MAX_NAME_LENGTH = 36;

    /**
     * The pattern a rank's name must match.
     */
    public static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9_\\-+#]*$");

    /**
     * The maximum length of a rank's prefix.
     */
    public static final int MAX_PREFIX_LENGTH = 6;

    /**
     * The pattern a rank's prefix must match.
     */
    public static final Pattern VALID_PREFIX = Pattern.compile("^[a-zA-Z0-9_\\-+|/#\\\\]*$");

    /**
     * The maximum number of ranks a Corporation can have (including the default and owner ranks).
     */
    public static final int MAX_RANK_COUNT = 12;

    /**
     * The maximum (lowest) priority a rank can have.
     */
    public static final int MAX_PRIORITY = Integer.MAX_VALUE - 1;

    /**
     * The minimum (highest) priority a rank can have.
     */
    public static final int MIN_PRIORITY = 1;

    /**
     * The UUID for the {@linkplain #ownerRank(Corporation) owner rank} in a corporation.
     */
    public static final UUID OWNER_RANK = UUID.nameUUIDFromBytes("Corporation:OWNER".getBytes());

    /**
     * The UUID for the {@linkplain #defaultRank(Corporation) default rank} in a corporation.
     */
    public static final UUID DEFAULT_RANK = UUID.nameUUIDFromBytes("Corporation:DEFAULT".getBytes());

    private final UUID identifier;
    private final UUID corporation;

    private String name;
    private int priority;
    private String prefix = "M";
    private Material icon = Material.STONE;

    private final Set<CorporationPermission> permissions = new HashSet<>();

    private CorporationRank(UUID identifier, UUID corporation) {
        this.identifier = identifier;
        this.corporation = corporation;
    }

    /**
     * Gets the identifier for this rank.
     * @return The identifier.
     */
    @NotNull
    public UUID getIdentifier() {
        return identifier;
    }

    /**
     * Gets the corporation this rank belongs to.
     * @return The corporation.
     */
    @NotNull
    public Corporation getCorporation() {
        return Corporation.byId(corporation);
    }

    /**
     * Gets the name of this rank.
     * @return The name.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this rank.
     * @param name The name.
     * @throws IllegalArgumentException if the name is null or longer than {@link #MAX_NAME_LENGTH}
     */
    public void setName(@NotNull String name) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name cannot be null.");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Name cannot be longer than " + MAX_NAME_LENGTH + " characters.");
        if (!VALID_NAME.matcher(name).matches()) throw new IllegalArgumentException("Name must match " + VALID_NAME.pattern());

        checkImmutable();

        this.name = name;
        save();
    }

    /**
     * Gets the priority of this rank. A rank priority of 0 is the highest priority.
     * @return The priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority of this rank. A rank priority of 0 is the highest priority.
     * @param priority The priority.
     * @throws IllegalArgumentException if the priority is less than 0
     */
    public void setPriority(int priority) throws IllegalArgumentException {
        if (priority < MIN_PRIORITY) throw new IllegalArgumentException("Priority cannot be less (higher) than " + MIN_PRIORITY + ".");
        if (priority > MAX_PRIORITY) throw new IllegalArgumentException("Priority cannot be greater (lower) than " + MAX_PRIORITY + ".");
        checkImmutable();

        this.priority = priority;
    }

    /**
     * Gets an immutable copy of the permissions this rank has.
     * @return Rank Permissions
     */
    @NotNull
    public Set<CorporationPermission> getPermissions() {
        return ImmutableSet.copyOf(permissions);
    }

    /**
     * Checks if this rank has a permission.
     * @param permission Permission to check
     * @return true if this rank has the permission, false otherwise
     */
    public boolean hasPermission(@NotNull CorporationPermission permission) {
        if (permission == null) throw new IllegalArgumentException("Permission cannot be null.");
        if (identifier.equals(OWNER_RANK)) return true;

        return permissions.contains(permission);
    }

    /**
     * Checks if this rank has all of the permissions.
     * @param permissions Permissions to check
     * @return true if this rank has all of the permissions, false otherwise
     */
    public boolean hasPermissions(@NotNull Iterable<CorporationPermission> permissions) {
        if (permissions == null) throw new IllegalArgumentException("Permissions cannot be null.");
        if (identifier.equals(OWNER_RANK)) return true;

        return this.permissions.containsAll(ImmutableSet.copyOf(permissions));
    }

    /**
     * Checks if this rank has all of the permissions.
     * @param permissions Permissions to check
     * @return true if this rank has all of the permissions, false otherwise
     */
    public boolean hasPermissions(@NotNull CorporationPermission... permissions) {
        return hasPermissions(ImmutableSet.copyOf(permissions));
    }

    /**
     * Adds a permission to this rank.
     * @param permission Permission to Add
     */
    public void addPermission(@NotNull CorporationPermission permission) {
        checkImmutable();
        permissions.add(permission);
        save();
    }

    /**
     * Adds permissions to this rank.
     * @param permissions Permissions to Add
     */
    public void addPermissions(@NotNull Iterable<CorporationPermission> permissions) {
        checkImmutable();
        this.permissions.addAll(ImmutableSet.copyOf(permissions));
        save();
    }

    /**
     * Adds permissions to this rank.
     * @param permissions Permissions to Add
     */
    public void addPermissions(@NotNull CorporationPermission... permissions) {
        addPermissions(ImmutableSet.copyOf(permissions));
    }

    /**
     * Removes a permission from this rank.
     * @param permission Permission to Remove
     */
    public void removePermission(@NotNull CorporationPermission permission) {
        checkImmutable();
        permissions.remove(permission);
        save();
    }

    /**
     * Removes permissions from this rank.
     * @param permissions Permissions to Remove
     */
    public void removePermissions(@NotNull Iterable<CorporationPermission> permissions) {
        checkImmutable();
        this.permissions.removeAll(ImmutableSet.copyOf(permissions));
        save();
    }

    /**
     * Removes permissions from this rank.
     * @param permissions Permissions to Remove
     */
    public void removePermissions(@NotNull CorporationPermission... permissions) {
        removePermissions(ImmutableSet.copyOf(permissions));
    }

    /**
     * Clears all permissions from this rank.
     */
    public void clearPermissions() {
        checkImmutable();
        permissions.clear();
        save();
    }

    /**
     * Gets the prefix of this rank.
     * @return The prefix.
     */
    @NotNull
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix of this rank.
     * @param prefix The prefix.
     * @throws IllegalArgumentException if the prefix is null
     */
    public void setPrefix(@NotNull String prefix) throws IllegalArgumentException {
        if (prefix == null || prefix.isEmpty()) throw new IllegalArgumentException("Prefix cannot be null.");
        if (prefix.length() > MAX_PREFIX_LENGTH) throw new IllegalArgumentException("Prefix cannot be longer than " + MAX_PREFIX_LENGTH + " characters.");
        if (!VALID_PREFIX.matcher(prefix).matches()) throw new IllegalArgumentException("Prefix must match " + VALID_PREFIX.pattern());

        checkImmutable();

        this.prefix = prefix;
        save();
    }

    /**
     * Sets the prefix of this rank.
     * @param prefix The prefix.
     */
    public void setPrefix(char prefix) {
        checkImmutable();

        this.prefix = String.valueOf(prefix);
        save();
    }

    /**
     * Gets the icon of this rank.
     * @return The icon.
     */
    @NotNull
    public Material getIcon() {
        return icon;
    }

    /**
     * Sets the icon of this rank.
     * @param icon The icon.
     * @throws IllegalArgumentException if the icon is null
     */
    public void setIcon(@NotNull Material icon) throws IllegalArgumentException {
        if (icon == null) throw new IllegalArgumentException("Icon cannot be null.");
        checkImmutable();

        this.icon = icon;
        save();
    }

    /**
     * Saves this rank. This method is called automatically.
     */
    public void save() {
        Corporation c = getCorporation();
        c.ranks.put(identifier, this);
        c.saveCorporation();
    }

    /**
     * Deletes this rank from the Corporation it belongs to.
     */
    public void delete() {
        Corporation c = getCorporation();
        c.deleteRank(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CorporationRank that = (CorporationRank) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(corporation, that.corporation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, corporation);
    }

    @Override
    public String toString() {
        return "CorporationRank{" +
                "identifier=" + identifier +
                ", corporation=" + corporation +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", prefix='" + prefix + '\'' +
                ", icon=" + icon +
                ", permissions=" + permissions +
                '}';
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .put("id", identifier.toString())
                .put("corporation", corporation.toString())
                .put("name", name)
                .put("priority", priority)
                .put("prefix", prefix)
                .put("icon", icon.name())
                .put("permissions", permissions.stream()
                        .map(CorporationPermission::name)
                        .collect(Collectors.toList())
                )
                .build();
    }

    /**
     * Deserializes a CorporationRank from a map.
     * @param map Map to Deserialize from {@link #serialize()}
     * @return The deserialized rank.
     */
    @SuppressWarnings("unchecked")
    public static CorporationRank deserialize(@NotNull Map<String, Object> map) {
        CorporationRank rank = new CorporationRank(UUID.fromString((String) map.get("id")), UUID.fromString((String) map.get("corporation")));

        rank.name = map.get("name").toString();
        rank.priority = (int) map.get("priority");
        rank.prefix = map.getOrDefault("prefix", "M").toString();
        rank.icon = Material.matchMaterial(map.getOrDefault("icon", Material.STONE.name()).toString());

        rank.permissions.addAll(((List<String>) map.get("permissions")).stream()
                .map(CorporationPermission::valueOf)
                .collect(Collectors.toList())
        );
        return rank;
    }

    /**
     * Gets or creates the owner rank for a corporation.
     * @param c The corporation.
     * @return The owner rank.
     */
    public static CorporationRank ownerRank(@NotNull Corporation c) {
        return c.getRanks().stream()
                .filter(r -> r.identifier.equals(OWNER_RANK))
                .findFirst()
                .orElse(builder()
                        .setIdentifier(OWNER_RANK)
                        .setCorporation(c)
                        .setName("Owner")
                        .setPrefix("CEO")
                        .setIcon(Material.EMERALD)
                        .setPriority(0)
                        .setPermissions(CorporationPermission.values())
                        .build0()
                );
    }

    /**
     * Gets or Creates a default rank for a corporation.
     * @param c The corporation.
     * @return The default rank.
     */
    @NotNull
    public static CorporationRank defaultRank(@NotNull Corporation c) {
        return c.getRanks().stream()
                .filter(r -> r.identifier.equals(DEFAULT_RANK))
                .findFirst()
                .orElse(builder()
                        .setIdentifier(DEFAULT_RANK)
                        .setCorporation(c)
                        .setName("Member")
                        .setPrefix('M')
                        .setIcon(Material.STONE)
                        .setPriority(Integer.MAX_VALUE)
                        .build0()
                );
    }

    /**
     * Creates a new CorporationRank builder.
     * @return The new builder.
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Represents a CorporationRank builder.
     */
    public static final class Builder {

        Corporation corporation;

        UUID identifier = UUID.randomUUID();
        String name;
        int priority = 1;
        String prefix = "M";
        Material icon = Material.STONE;

        final Set<CorporationPermission> permissions = new HashSet<>(CorporationPermission.getDefaultPermissions());

        private Builder setIdentifier(UUID identifier) {
            this.identifier = identifier;
            return this;
        }

        /**
         * Sets the corporation this rank belongs to.
         * @param corporation The corporation.
         * @return this builder, for chaining
         */
        public Builder setCorporation(@NotNull Corporation corporation) {
            this.corporation = corporation;
            return this;
        }

        /**
         * Sets the name of this rank.
         * @param name The name.
         * @return this builder, for chaining
         */
        public Builder setName(@NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the priority of this rank.
         * @param priority The priority.
         * @return this builder, for chaining
         */
        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the prefix of this rank.
         * @param prefix The prefix.
         * @return this builder, for chaining
         */
        public Builder setPrefix(@NotNull String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the prefix of this rank.
         * @param prefix The prefix.
         * @return this builder, for chaining
         */
        public Builder setPrefix(char prefix) {
            this.prefix = String.valueOf(prefix);
            return this;
        }

        /**
         * Adds a permission to this rank.
         * @param permission The permission.
         * @return this builder, for chaining
         */
        public Builder addPermission(@NotNull CorporationPermission permission) {
            this.permissions.add(permission);
            return this;
        }

        /**
         * Removes a permission from this rank.
         * @param permission The permission.
         * @return this builder, for chaining
         */
        public Builder removePermission(@NotNull CorporationPermission permission) {
            this.permissions.remove(permission);
            return this;
        }

        /**
         * Clears all permissions from this rank.
         * @return this builder, for chaining
         */
        public Builder clearPermissions() {
            this.permissions.clear();
            return this;
        }

        /**
         * Sets the permissions of this rank.
         * @param permissions Iterable of Permissions
         * @return this builder, for chaining
         */
        public Builder setPermissions(@NotNull Iterable<CorporationPermission> permissions) {
            this.permissions.clear();
            this.permissions.addAll(ImmutableSet.copyOf(permissions));
            return this;
        }

        /**
         * Sets the permissions of this rank.
         * @param permissions Array of Permissions
         * @return this builder, for chaining
         */
        public Builder setPermissions(@NotNull CorporationPermission... permissions) {
            this.permissions.clear();
            this.permissions.addAll(ImmutableSet.copyOf(permissions));
            return this;
        }

        /**
         * Sets the icon for this rank.
         * @param icon The icon.
         * @return this builder, for chaining
         */
        public Builder setIcon(@NotNull Material icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Builds the rank.
         * @return The rank.
         * @throws IllegalArgumentException if field errors occur
         * @throws IllegalStateException if the corporation has reached its rank limit
         */
        @NotNull
        public CorporationRank build() throws IllegalArgumentException, IllegalStateException {
            if (priority < MIN_PRIORITY) throw new IllegalArgumentException("Priority cannot be less (higher) than " + MIN_PRIORITY + ".");
            if (priority > MAX_PRIORITY) throw new IllegalArgumentException("Priority cannot be greater (lower) than " + MAX_PRIORITY + ".");

            if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name cannot be null.");
            if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Name cannot be longer than " + MAX_NAME_LENGTH + " characters.");
            if (!VALID_NAME.matcher(name).matches()) throw new IllegalArgumentException("Name must match " + VALID_NAME);

            if (prefix == null || prefix.isEmpty()) throw new IllegalArgumentException("Prefix cannot be null.");
            if (prefix.length() > MAX_PREFIX_LENGTH) throw new IllegalArgumentException("Prefix cannot be longer than " + MAX_PREFIX_LENGTH + " characters.");
            if (!VALID_PREFIX.matcher(prefix).matches()) throw new IllegalArgumentException("Prefix must match " + VALID_PREFIX);

            if (icon == null) throw new IllegalArgumentException("Icon cannot be null.");

            if (corporation.getRanks().size() >= corporation.getMaxRanks()) throw new IllegalStateException("Corporation has reached its rank limit of '" + corporation.getMaxRanks() + "'");

            CorporationRank rank = build0();
            rank.save();
            return rank;
        }

        private CorporationRank build0() {
            CorporationRank rank = new CorporationRank(identifier, corporation.getUniqueId());

            rank.name = name;
            rank.priority = priority;
            rank.prefix = prefix;
            rank.icon = icon;
            rank.permissions.addAll(permissions);

            return rank;
        }

    }

    private void checkImmutable() {
        if (immutable(this)) throw new UnsupportedOperationException("Cannot modify immutable rank.");
    }

    private static boolean immutable(CorporationRank rank) {
        return rank.identifier.equals(OWNER_RANK);
    }
}
