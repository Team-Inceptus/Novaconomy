package us.teaminceptus.novaconomy.api.corporation;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
     * The maximum number of ranks a Corporation can have.
     */
    public static final int MAX_RANK_COUNT = 10;

    private final UUID identifier;
    private final UUID corporation;

    private String name;
    private int priority;

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
        if (name == null) throw new IllegalArgumentException("Name cannot be null.");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Name cannot be longer than " + MAX_NAME_LENGTH + " characters.");

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
        if (priority < 0) throw new IllegalArgumentException("Priority cannot be less than 0.");
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
        return permissions.contains(permission);
    }

    /**
     * Checks if this rank has all of the permissions.
     * @param permissions Permissions to check
     * @return true if this rank has all of the permissions, false otherwise
     */
    public boolean hasPermissions(@NotNull Iterable<CorporationPermission> permissions) {
        if (permissions == null) throw new IllegalArgumentException("Permissions cannot be null.");
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
        permissions.add(permission);
        save();
    }

    /**
     * Adds permissions to this rank.
     * @param permissions Permissions to Add
     */
    public void addPermissions(@NotNull Iterable<CorporationPermission> permissions) {
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
        permissions.remove(permission);
        save();
    }

    /**
     * Removes permissions from this rank.
     * @param permissions Permissions to Remove
     */
    public void removePermissions(@NotNull Iterable<CorporationPermission> permissions) {
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
        permissions.clear();
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
        rank.permissions.addAll(((List<String>) map.get("permissions")).stream()
                .map(CorporationPermission::valueOf)
                .collect(Collectors.toList())
        );
        return rank;
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

        String name;
        int priority;

        final Set<CorporationPermission> permissions = new HashSet<>();

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
         * @param permissions The permissions.
         * @return this builder, for chaining
         */
        public Builder setPermissions(@NotNull Iterable<CorporationPermission> permissions) {
            this.permissions.clear();
            this.permissions.addAll(ImmutableSet.copyOf(permissions));
            return this;
        }

        /**
         * Builds the rank.
         * @return The rank.
         */
        @NotNull
        public CorporationRank build() {
            CorporationRank rank = new CorporationRank(UUID.randomUUID(), corporation.getUniqueId());

            rank.name = name;
            rank.priority = priority;
            rank.permissions.addAll(permissions);
            rank.save();

            return rank;
        }

    }

}
