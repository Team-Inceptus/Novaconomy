package us.teaminceptus.novaconomy.api.business;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

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
        private final Product product;
        private final long timestamp;

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param product Product that was bought
         * @param timestamp Time that the Transaction was made
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Product product, long timestamp) {
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
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Product product, @NotNull Date timestamp) throws NullPointerException{
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
        public Product getProduct() {
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

            long num = serial.get("timestamp") instanceof Integer ? (int) serial.get("timestamp") : (long) serial.get("timestamp");

            try {
                return new Transaction(
                        (OfflinePlayer) serial.get("buyer"),
                        new Product(
                                (ItemStack) serial.get("item"),
                                new Price(
                                        Economy.getEconomy(UUID.fromString((String) serial.get("economy"))),
                                        (double) serial.get("amount")
                                )
                        ), num
                );
            } catch (NullPointerException | ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private final UUID businessId;

    private Transaction lastTransaction;

    private int totalSales = 0;

    int totalResources = 0;

    private final Map<Product, Integer> productSales = new HashMap<>();

    BusinessStatistics(UUID businessId) {
        this.businessId = businessId;
    }

    BusinessStatistics(Business business) {
        this.businessId = business.getUniqueId();
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
     * Fetches the last transaction for this Business.
     * @return Last Transaction, or null if there are none
     */
    @Nullable
    public Transaction getLastTransaction() {
        return lastTransaction;
    }

    /**
     * Whether a Transaction has been made for this Business.
     * @return true if a transaction was made, else false
     */
    public boolean hasLatestTransaction() {
        return lastTransaction != null;
    }

    /**
     * Gets the total amount of items purchased from this Business.
     * @return Total Sales
     */
    public int getTotalSales() {
        return totalSales;
    }

    /**
     * Fetches the total amount of stock ever added to this Business.
     * @return Total Resources
     */
    public int getTotalResources() {
        return totalResources;
    }

    /**
     * Fetches a Map of Products to how much was bought of it.
     * @return Product Sales
     */
    @NotNull
    public Map<Product, Integer> getProductSales() {
        return productSales;
    }

    /**
     * Sets the latest transaction for this Business.
     * @param last Transaction to set
     */
    public void setLastTransaction(@NotNull Transaction last) {
        this.lastTransaction = last;
    }

    /**
     * Sets the total amount of sales for this Business.
     * @param sales Total Sales
     */
    public void setTotalSales(int sales) {
        this.totalSales = sales;
    }

    @Override
    public Map<String, Object> serialize() {
        return new HashMap<String, Object>() {{
            put("business_id", businessId.toString());
            put("data", new HashMap<String, Object>() {{
                put("total_sales", totalSales);
                put("total_resources", totalResources);
                put("last_transaction", lastTransaction);
                put("product_sales", productSales);
            }});
        }};
    }

    /**
     * Deserializes a Map into a BusinessStatistics.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized BusinessStatistics, or null if serial is null
     * @throws IllegalArgumentException if an argument is malformed
     * @throws NullPointerException if ID is missing
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static BusinessStatistics deserialize(@NotNull Map<String, Object> serial) throws IllegalArgumentException, NullPointerException {
        if (serial == null) return null;

        BusinessStatistics s = new BusinessStatistics(UUID.fromString((String) serial.get("business_id")));

        try {
            Map<String, Object> data = (Map<String, Object>) serial.get("data");

            s.totalResources = (int) data.get("total_resources");
            s.totalSales = (int) data.get("total_sales");
            s.lastTransaction = (Transaction) data.get("last_transaction");
            s.productSales.putAll((Map<Product, Integer>) data.get("product_sales"));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(e);
        } catch (NullPointerException ignored) {}

        return s;
    }
}
