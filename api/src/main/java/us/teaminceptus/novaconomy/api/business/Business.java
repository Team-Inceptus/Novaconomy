package us.teaminceptus.novaconomy.api.business;

import org.apache.commons.lang.Validate;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Represents a Novaconomy Business
 */
public final class Business {

    private final UUID id;

    private final String name;

    private final OfflinePlayer owner;

    private Business(String name, OfflinePlayer owner) {
        this.id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        this.name = name;
        this.owner = owner;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fetches the Business's Owner.
     * @return Business Owner
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return this.owner;
    }

    /**
     * Fetches the Business's Name.
     * @return Business Name
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Fetches the Business's Unique ID.
     * @return Business ID
     */
    @NotNull
    public UUID getUniqueId() {
        return this.id;
    }


    /**
     * Represents a Business Builder
     */
    public static final class Builder {

        String name;
        OfflinePlayer owner;

        private Builder() {}

        /**
         * Sets the Business Name.
         * @param name Name of Business
         * @return this builder, for chaining
         */
        public Builder setName(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the Business's Owner.
         * @param owner Owner of Business
         * @return this builder, for chaining
         */
        public Builder setOwner(@Nullable OfflinePlayer owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Builds a Novaconomy Business.
         * @return Built Novaconomy Business
         * @throws IllegalArgumentException if a part is missing or null
         */
        @NotNull
        public Business build() throws IllegalArgumentException {
            Validate.notNull(owner, "Owner cannot be null");
            Validate.notNull(name, "Name cannot be null");

            return new Business(name, owner);
        }

    }

}
