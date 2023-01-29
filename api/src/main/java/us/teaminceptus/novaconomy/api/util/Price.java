package us.teaminceptus.novaconomy.api.util;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility Class for creating a Price with an Economy
 */
public final class Price implements ConfigurationSerializable, Comparable<Price>, Externalizable {

    private static final long serialVersionUID = 9211350596474508468L;

    private Economy econ;
    private double amount;

    // Serialization

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(amount);
        out.writeObject(econ == null ? "" : econ.getUniqueId().toString());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.amount = in.readDouble();
        String s = (String) in.readObject();
        this.econ = s.isEmpty() ? null : Economy.getEconomy(UUID.fromString(s));
    }

    // Other

    /**
     * Constructs a Price with an amount of 1.
     */
    public Price() {
        this(1);
    }

    /**
     * Constructs a Price with a null economy.
     * @param amount Amount of money
     */
    public Price(double amount) {
        this(null, amount);
    }

    /**
     * Constructs a Price.
     * @param econ Economy to use
     * @param amount Price Amount
     * @throws IllegalArgumentException if amount is not positive
     */
    public Price(@Nullable Economy econ, double amount) throws IllegalArgumentException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        this.econ = econ;
        this.amount = amount;
    }

    /**
     * Constructs a Price from a Map Entry.
     * @param entry Map Entry
     */
    public Price(@NotNull Map.Entry<Economy, Double> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     * Fetches the Price amount.
     * @return Price Amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the current Price amount.
     * @param amount Price Amount
     * @return this Price, for chaining
     * @throws IllegalArgumentException if amount is not positive
     */
    @NotNull
    public Price setAmount(double amount) throws IllegalArgumentException {
       if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
       this.amount = amount;
       return this;
    }

    /**
     * Fetches the Price's Economy.
     * @return Price's Economy
     */
    @Nullable
    public Economy getEconomy() {
        return this.econ;
    }

    /**
     * Sets the current Price's Economy.
     * @param econ Price Economy
     * @return this Price, for chaining
     */
    @NotNull
    public Price setEconomy(@Nullable Economy econ) {
        this.econ = econ;
        return this;
    }

    /**
     * Converts this Price to another Price with Economy.
     * @param econ Economy to use
     * @return Converted Price with amount converted to Economy
     * @throws IllegalArgumentException if economy is null
     */
    @NotNull
    public Price convertTo(@NotNull Economy econ) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        return new Price(econ, econ.convertAmount(this.econ, this.amount));
    }

    /**
     * Adds an amount to this Price's amount.
     * @param amount Amount to add
     * @return this Price, for chaining
     */
    @NotNull
    public Price add(double amount) {
        this.amount += amount;
        return this;
    }

    /**
     * Adds a Price's amount to this Price's amount.
     * @param price Price to add
     * @return this Price, for chaining
     */
    @NotNull
    public Price add(@Nullable Price price) {
        if (price == null) return this;
        return add(price.amount);
    }

    /**
     * Removes a Price's amount from this Price's amount.
     * @param price Price to remove
     * @return this Price, for chaining
     */
    @NotNull
    public Price remove(@Nullable Price price) {
        if (price == null) return this;
        return remove(price.amount);
    }

    /**
     * Fetches the Price's Symbol.
     * @return Price Symbol
     */
    public char getPriceSymbol() { return this.econ.getSymbol(); }

    /**
     * Removes this amount from this Price's current amount.
     * @param amount Amount to remove
     * @return this Price, for chaining
     */
    @NotNull
    public Price remove(double amount) {
        this.amount -= amount;
        return this;
    }

    /**
     * Multiplies {@linkplain #getAmount() the amount} by the economy's {@linkplain Economy#getConversionScale() conversion scale}.
     * @return Real Amount, factoring in the economy's conversion scale
     */
    public double getRealAmount() {
        return amount * econ.getConversionScale();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Price price = (Price) o;
        return Double.compare(price.amount, amount) == 0 && econ.equals(price.econ);
    }

    @Override
    public int hashCode() {
        return Objects.hash(econ, amount);
    }

    @Override
    public String toString() {
        return String.format("%,.2f", amount) + econ.getSymbol();
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("amount", amount);
            if (econ != null) put("economy", econ.getName().toLowerCase());
        }};
    }

    /**
     * Deserializes a Map into a Price.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized Price
     * @throws IllegalArgumentException if an argument is malformed or missing
     */
    @Nullable
    public static Price deserialize(@Nullable Map<String, Object> serial) throws IllegalArgumentException {
        if (serial == null) return null;

        Economy econ = serial.containsKey("economy") ? Economy.getEconomy((String) serial.get("economy")) : null;

        try {
            return new Price(econ, (double) serial.get("amount"));
        } catch (ClassCastException | NullPointerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int compareTo(@NotNull Price r) {
        return Double.compare(amount, r.amount);
    }
}
