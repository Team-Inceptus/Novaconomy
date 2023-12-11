package us.teaminceptus.novaconomy.api.player;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a Bounty placeable on a player
 */
public final class Bounty implements ConfigurationSerializable, Comparable<Bounty> {

    private final OfflinePlayer owner;

    private final Economy econ;

    private double amount;

    private final OfflinePlayer target;

    private Bounty(OfflinePlayer owner, Economy econ, double amount, OfflinePlayer target) {
        this.owner = owner;
        this.econ = econ;
        this.amount = amount;
        this.target = target;
    }

    /**
     * Creates a new Bounty Builder.
     * @return Bounty Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fetches the Owner of this Bounty.
     * @return Bounty Owner
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return owner;
    }

    /**
     * Whether this Player is the owner of this Bounty.
     * @param p Player to check
     * @return true if owner, else false
     */
    public boolean isOwner(@Nullable OfflinePlayer p) {
        if (p == null) return false;
        return p.equals(owner);
    }

    /**
     * Fetches the Economy this Bounty has been placed in.
     * @return Bounty Economy
     */
    @NotNull
    public Economy getEconomy() { return econ; }

    /**
     * Fetches the Target of this Bounty.
     * @return Bounty Target
     */
    @NotNull
    public OfflinePlayer getTarget() {
        return target;
    }

    /**
     * Fetches the Amount of this Bounty.
     * @return Bounty Amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the Amount of this Bounty.
     * @param amount Amount to set
     */
    public void setAmount(double amount) {
        this.amount = amount;
        save();
    }

    @Override
    public Map<String, Object> serialize() {
        return ImmutableMap.<String, Object>builder()
                .put("owner", owner.getUniqueId().toString())
                .put("target", target.getUniqueId().toString())
                .put("amount", amount)
                .put("economy", econ.getUniqueId().toString())
                .build();
    }

    private void save() {
        String key = "bounties." + target.getUniqueId();
        NovaPlayer owner = new NovaPlayer(this.owner);
        Map<String, Object> data = owner.getPlayerData();

        data.put(key, this);
    }

    /**
     * Deserializes a Map into a Bounty.
     * @param serialize Serialization from {@link #serialize()}
     * @return Deserialized Bounty, or null if not found
     * @throws IllegalArgumentException if an object is missing or malformed
     */
    @Nullable
    public static Bounty deserialize(@Nullable Map<String, Object> serialize) throws IllegalArgumentException {
        if (serialize == null) return null;

        try {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString((String) serialize.get("owner")));
            Economy econ = Economy.byId(UUID.fromString((String) serialize.get("economy")));
            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString((String) serialize.get("target")));
            return new Bounty(owner, econ, (double) serialize.get("amount"), target);
        } catch (ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int compareTo(@NotNull Bounty o) {
        return Double.compare(this.amount, o.amount);
    }

    /**
     * Represents a builder class for a Bounty
     */
    public static final class Builder {

        NovaPlayer owner;
        Economy econ;
        double amount;
        OfflinePlayer target;

        Builder() {
            amount = 100;
        }

        /**
         * Sets the Owner of this Bounty.
         * @param owner Bounty Owner
         * @return this builder, for chaining
         */
        public Builder setOwner(@NotNull OfflinePlayer owner) {
            if (owner == null) return this;
            this.owner = new NovaPlayer(owner);
            return this;
        }

        /**
         * Sets the Owner of this Bounty.
         * @param owner Bounty Owner
         * @return this builder, for chaining
         */
        public Builder setOwner(@NotNull NovaPlayer owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the Economy of this Bounty.
         * @param econ Economy of this Bounty
         * @return this builder, for chaining
         */
        public Builder setEconomy(@NotNull Economy econ) {
            this.econ = econ;
            return this;
        }

        /**
         * Sets the Amount of this Bounty.
         * @param amount Amount of this Bounty
         * @return this builder, for chaining
         */
        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Sets the Target of this Bounty.
         * @param target Target of this Bounty
         * @return this builder, for chaining
         */
        public Builder setTarget(@NotNull OfflinePlayer target) {
            this.target = target;
            return this;
        }

        /**
         * Builds the Bounty
         * @return Bounty Built
         * @throws UnsupportedOperationException if a bounty with this target already exists
         * @throws IllegalArgumentException if amount is not positive, or owner/economy/target are null
         */
        public Bounty build() throws UnsupportedOperationException, IllegalArgumentException {
            if (owner == null) throw new IllegalArgumentException("Owner cannot be null");
            if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
            if (target == null) throw new IllegalArgumentException("Target cannot be null");
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

            String key = "bounties." + target.getUniqueId();
            Map<String, Object> data = owner.getPlayerData();

            if (data.containsKey(key)) throw new UnsupportedOperationException("Bounty already exists");
            Bounty b = new Bounty(owner.getPlayer(), econ, amount, target);

            owner.stats.totalBountiesCreated++;
            data.put(key, b);
            owner.save();

            NovaPlayer targetN = new NovaPlayer(target);
            targetN.stats.totalBountiesHad++;
            targetN.save();

            return b;
        }

    }

}
