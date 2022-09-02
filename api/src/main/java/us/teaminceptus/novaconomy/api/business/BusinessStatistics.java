package us.teaminceptus.novaconomy.api.business;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility Class for Business Statistics
 */
public final class BusinessStatistics implements ConfigurationSerializable, Serializable {

    /**
     * Represents a Transaction for a Business
     */
    public static final class Transaction implements ConfigurationSerializable, Externalizable {
        private UUID buyer;
        private Product product;
        private long timestamp;

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
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Product product) {
            this(buyer, product, System.currentTimeMillis());
        }

        /**
         * Constructs a Transaction.
         * @param buyer Player who bought the Product
         * @param product Product that was bought
         * @param timestamp Time that the Transaction was made
         * @throws IllegalArgumentException if timestamp is negative
         */
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Product product, long timestamp) throws IllegalArgumentException {
            if (timestamp < 0) throw new IllegalArgumentException("Timestamp cannot be negative");
            this.buyer = buyer == null ? null : buyer.getUniqueId();
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
        public Transaction(@Nullable OfflinePlayer buyer, @Nullable Product product, @NotNull Date timestamp) throws NullPointerException {
            this(buyer, product, timestamp.getTime());
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

        @Override
        public Map<String, Object> serialize() {
            return new HashMap<String, Object>() {{
                put("buyer", buyer.toString());
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

            Object buyerO = serial.get("buyer");
            OfflinePlayer buyer = buyerO instanceof OfflinePlayer ? (OfflinePlayer) buyerO : Bukkit.getOfflinePlayer(UUID.fromString((String) buyerO));

            try {
                return new Transaction(
                        buyer,
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

        // Serialization

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(buyer);

            Map<String, Object> item = new HashMap<>(product.getItem().serialize());
            item.put("meta", product.getItem().getItemMeta().serialize());

            out.writeObject(item);
            out.writeObject(product.getPrice());
            out.writeLong(timestamp);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            this.buyer = (UUID) in.readObject();

            Map<String, Object> item = new HashMap<>((Map<String, Object>) in.readObject());

            ItemStack prItem = null;

            try {
                ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) item.get("type")));
                DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
                Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
                deserialize.setAccessible(true);

                ItemMeta meta = (ItemMeta) deserialize.invoke(null, item.get("meta"));
                item.put("meta", meta);
                prItem = ItemStack.deserialize(item);
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
            }

            this.product = new Product(prItem, (Price) in.readObject());

            this.timestamp = in.readLong();
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

    void writeStats(ObjectOutputStream os) throws IOException {
        os.writeObject(businessId);
        os.writeObject(lastTransaction);
        os.writeInt(totalSales);
        os.writeInt(totalResources);

        Map<Map<String, Object>, Integer> prods = new HashMap<>();
        for (Map.Entry<Product, Integer> entry : productSales.entrySet()) {
            Product p = entry.getKey();
            Map<String, Object> m = new HashMap<>(p.serialize());

            Map<String, Object> item = new HashMap<>(p.getItem().serialize());
            item.put("meta", p.getItem().getItemMeta().serialize());
            m.put("item", item);

            prods.put(m, entry.getValue());
        }
        os.writeObject(prods);
    }

    @SuppressWarnings("unchecked")
    static BusinessStatistics readStats(ObjectInputStream is) throws IOException, ClassNotFoundException {
        BusinessStatistics stats = new BusinessStatistics((UUID) is.readObject());
        stats.lastTransaction = (Transaction) is.readObject();
        stats.totalSales = is.readInt();
        stats.totalResources = is.readInt();

        try {
            Map<Map<String, Object>, Integer> prods = (Map<Map<String, Object>, Integer>) is.readObject();
            for (Map.Entry<Map<String, Object>, Integer> entry : prods.entrySet()) {
                Map<String, Object> m = entry.getKey();

                Map<String, Object> item = new HashMap<>((Map<String, Object>) m.get("item"));

                ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) item.get("type")));
                DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
                Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);

                deserialize.setAccessible(true);
                item.put("meta", deserialize.invoke(null, item.get("meta")));

                ItemStack prItem = ItemStack.deserialize(item);

                stats.productSales.put(new Product(prItem, (Price) m.get("price")), entry.getValue());
            }
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }

        return stats;
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
    @Deprecated
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
     * @deprecated Bukkit Serialization is no longer used
     * @return Deserialized BusinessStatistics, or null if serial is null
     * @throws IllegalArgumentException if an argument is malformed
     * @throws NullPointerException if ID is missing
     */
    @Deprecated
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
