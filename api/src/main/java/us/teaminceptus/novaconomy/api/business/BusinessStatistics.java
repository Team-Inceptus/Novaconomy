package us.teaminceptus.novaconomy.api.business;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
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
        private UUID buyer;
        private Product product;
        private long timestamp;
        private UUID business;

        /**
         * Constructs a Transaction with no buyer or product.
         */
        public Transaction() {
            this(null, null);
        }

        /**
         * Constructs a Transaction with a timestamp equaling {@link System#currentTimeMillis()}.
         * @param buyer Buyer of the Product
         * @param product Product bought
         * @param business Business that the transaction took place in
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Business business, @Nullable Product product) {
            this(buyer, business, product, System.currentTimeMillis());
        }

        /**
         * Constructs a Transaction with a timestamp equaling {@link System#currentTimeMillis()}.
         * @param buyer Buyer of the Product
         * @param product Business Product bought
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable BusinessProduct product) {
            this(buyer, product == null ? null : product.getBusiness(), product == null ? null : new Product(product), System.currentTimeMillis());
        }

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param business Business that the transaction took place in
         * @param product Product that was bought
         * @param timestamp Time that the Transaction was made
         * @throws IllegalArgumentException if timestamp is negative
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Business business, @Nullable Product product, long timestamp) throws IllegalArgumentException {
            if (timestamp < 0) throw new IllegalArgumentException("Timestamp cannot be negative");
            this.buyer = buyer == null ? null : buyer.getUniqueId();
            this.product = product;
            this.timestamp = timestamp;
            this.business = business == null ? null : business.getUniqueId();
        }

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param product Business Product that was bought
         * @param timestamp Time that the Transaction was made
         * @throws IllegalArgumentException if timestamp is negative
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable BusinessProduct product, long timestamp) throws IllegalArgumentException {
            this(buyer, product == null ? null : product.getBusiness(), product == null ? null : new Product(product), timestamp);
        }

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param business Business that the transaction took place in
         * @param product Product that was bought
         * @param timestamp Date Object of the time that the Transaction was made
         * @throws NullPointerException if Date is null
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Business business, @Nullable Product product, @NotNull Date timestamp) throws NullPointerException {
            this(buyer, business, product, timestamp.getTime());
        }

        /**
         * Returns the buyer of the transaction
         * @return Buyer of the transaction, may be null
         */
        @Nullable
        public OfflinePlayer getBuyer() {
            return Bukkit.getOfflinePlayer(buyer);
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

        /**
         * Sets the buyer of this transaction.
         * @param buyer Buyer to set
         */
        public void setBuyer(@Nullable OfflinePlayer buyer) {
            this.buyer = buyer == null ? null : buyer.getUniqueId();
        }

        /**
         * Sets the product of this transaction.
         * @param product Product to set
         */
        public void setProduct(@Nullable Product product) {
            this.product = product;
        }

        /**
         * Sets the timestamp of this transaction.
         * @param timestamp Timestamp to set
         * @throws IllegalArgumentException if timestamp is negative
         */
        public void setTimestamp(long timestamp) throws IllegalArgumentException {
            if (timestamp < 0) throw new IllegalArgumentException("Timestamp cannot be negative");
            this.timestamp = timestamp;
        }

        /**
         * Sets the timestamp of this transaction.
         * @param timestamp Date Object of the timestamp to set
         * @throws NullPointerException if Date is null
         */
        public void setTimestamp(@NotNull Date timestamp) throws NullPointerException {
            Preconditions.checkNotNull(timestamp, "Timestamp cannot be null");
            setTimestamp(timestamp.getTime());
        }

        /**
         * Returns the Business that the transaction took place in.
         * @return Business that the transaction took place in.
         */
        @Nullable
        public Business getBusiness() {
            return Business.byId(business);
        }

        /**
         * Sets the business of this transaction.
         * @param business Business to set
         */
        public void setBusiness(@NotNull Business business) {
            this.business = business.getUniqueId();
        }

        @Override
        public Map<String, Object> serialize() {
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                    .put("buyer", buyer.toString())
                    .put("timestamp", timestamp);

            if (product != null) builder.put("item", product.getItem());
            if (product != null && product.getEconomy() != null) builder.put("economy", product.getEconomy().getUniqueId().toString());
            if (product != null) builder.put("amount", product.getAmount());
            if (business != null) builder.put("business", business.toString());

            return builder.build();
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

            Object buyerO = serial.get("buyer");
            OfflinePlayer buyer = buyerO == null ? null : buyerO instanceof OfflinePlayer ? (OfflinePlayer) buyerO : Bukkit.getOfflinePlayer(UUID.fromString((String) buyerO));

            Object businessO = serial.get("business");
            UUID business = businessO == null ? null : UUID.fromString(businessO.toString());

            Transaction t = new Transaction(buyer, null,
                    new Product(
                            (ItemStack) serial.get("item"),
                            new Price(
                                    serial.containsKey("economy") ? Economy.byId(UUID.fromString((String) serial.get("economy"))) : null,
                                    (double) serial.get("amount")
                            )
                    ), num
            );

            t.business = business;
            return t;
        }
    }

    private final UUID businessId;

    private Transaction lastTransaction;

    private int totalSales = 0;

    int totalResources = 0;

    private int views = 0;

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
        return lastTransaction != null && lastTransaction.getBuyer() != null && lastTransaction.getProduct() != null && lastTransaction.getBusiness() != null;
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
     * Fetches the total amount of times this Business was viewed.
     * @return Total Views
     */
    public int getViews() {
        return views;
    }

    /**
     * Sets the total amount of views for this Business.
     * @param views Total Views
     */
    public void setViews(int views) {
        this.views = views;
    }

    /**
     * Adds to the total amount of views for this Business.
     * @param views Views to add
     */
    public void addView(int views) {
        this.views += views;
    }

    /**
     * Adds one view to the total amount of views for this Business.
     */
    public void addView() {
        addView(1);
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
     * Fetches how much of a Product was bought from this Business.
     * @param product Product to fetch
     * @return Amount of Product bought, or 0 if none
     */
    public int getPurchaseCount(@NotNull Product product) {
        return productSales.getOrDefault(product, 0);
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
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("business_id", businessId.toString())
                .put("total_sales", totalSales)
                .put("total_resources", totalResources)
                .put("product_sales", productSales)
                .put("views", views);

        if (lastTransaction != null) builder.put("last_transaction", lastTransaction);
        return builder.build();
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
        Map<String, Object> data = serial.containsKey("data") ? (Map<String, Object>) serial.get("data") : serial;

        s.totalResources = (int) data.getOrDefault("total_resources", 0);
        s.totalSales = (int) data.getOrDefault("total_sales", 0);
        s.lastTransaction = (Transaction) data.get("last_transaction");
        s.productSales.putAll((Map<Product, Integer>) data.getOrDefault("product_sales", new HashMap<>()));
        s.views = (int) data.getOrDefault("views", 0);

        return s;
    }
}
