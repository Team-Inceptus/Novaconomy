package us.teaminceptus.novaconomy.api.business;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility Class for Business Statistics
 */
public final class BusinessStatistics implements ConfigurationSerializable {

    /**
     * Represents a Transaction for a Business
     */
    public static final class Transaction implements ConfigurationSerializable {
        private final OfflinePlayer buyer;
        private final BusinessProduct product;
        private final long timestamp;

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param product Product that was bought
         * @param timestamp Time that the Transaction was made
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable BusinessProduct product, long timestamp) {
            this.buyer = buyer;
            this.product = product;
            this.timestamp = timestamp;
        }

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param product Product that was bought
         * @param timestamp Date Object of the time that the Transaction was made
         * @throws NullPointerException if Date is null
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable BusinessProduct product, @NotNull Date timestamp) throws NullPointerException{
            this(buyer, product, timestamp.getTime());
        }

        /**
         * Returns the buyer of the transaction
         * @return Buyer of the transaction, may be null
         */
        @Nullable
        public OfflinePlayer getBuyer() {
            return buyer;
        }

        /**
         * Returns the product of the transaction
         * @return Product of the transaction, may be null
         */
        @Nullable
        public BusinessProduct getProduct() {
            return product;
        }

        /**
         * Returns the timestamp of the transaction
         * @return Timestamp of the transaction
         */
        @NotNull
        public Date getTimestamp() {
            return new Date(timestamp);
        }

        @Override
        public Map<String, Object> serialize() {
            return new HashMap<String, Object>() {{
                put("buyer", buyer);
                put("item", product == null ? null : product.getItem());
                put("economy", product == null ? null : product.getEconomy().getUniqueId().toString());
                put("amount", product == null ? null : product.getAmount());
                put("business", product == null ? null : product.getBusiness().getUniqueId().toString());
                put("timestamp", timestamp);
            }};
        }

        /**
         * Deserializes a Map into a Transaction.
         * @param serial Serialization from {@link #serialize()}
         * @return Deserialized Transaction, or null if serial is null
         * @throws IllegalArgumentException if an argument is missing/malformed
         */
        @Nullable
        public static Transaction deserialize(@NotNull Map<String, Object> serial) throws IllegalArgumentException {
            if (serial == null) return null;

            try {
                return new Transaction(
                        (OfflinePlayer) serial.get("buyer"),
                        new BusinessProduct(
                                (ItemStack) serial.get("item"),
                                new Price(
                                        Economy.getEconomy(UUID.fromString((String) serial.get("economy"))),
                                        (double) serial.get("amount")
                                ),
                                Business.getById(UUID.fromString((String) serial.get("business")))
                        ),
                        (long) serial.get("timestamp")
                );
            } catch (NullPointerException | ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private final UUID businessId;

    private Transaction lastTransaction;

    BusinessStatistics(UUID businessId) {
        this.businessId = businessId;
    }

    BusinessStatistics(Business business) {
        this.businessId = business.getUniqueId();
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
           put("business_id", businessId.toString());
        }};
    }

    /**
     * Fetches the Business ID for this BusinessStatistics.
     * @return Business ID
     */
    @NotNull
    public UUID getBusinessId() {
        return businessId;
    }

    /**
     * Deserializes a Map into a BusinessStatistics.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized BusinessStatistics, or null if serial is null
     * @throws IllegalArgumentException if an argument is missing/malformed
     */
    @Nullable
    public static BusinessStatistics deserialize(@NotNull Map<String, Object> serial) throws IllegalArgumentException {
        if (serial == null) return null;
        try {
            return new BusinessStatistics(UUID.fromString((String) serial.get("business_id")));
        } catch (NullPointerException | ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
