package us.teaminceptus.novaconomy.api.business;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.events.business.BusinessCreateEvent;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a Novaconomy Business
 */
public final class Business implements ConfigurationSerializable {

    private final UUID id;

    private final String name;

    private final OfflinePlayer owner;

    private final Material icon;

    private final List<BusinessProduct> products = new ArrayList<>();

    private final List<ItemStack> resources = new ArrayList<>();

    private Business(String name, Material icon, OfflinePlayer owner, Collection<Product> products, Collection<ItemStack> resources, boolean save) {
        this.id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        this.name = name;
        this.icon = icon;
        this.owner = owner;
        if (products != null) products.forEach(p -> this.products.add(new BusinessProduct(p, this)));
        if (resources != null) this.resources.addAll(resources);
        if (save) saveBusiness();
    }

    /**
     * Fetches the Business Builder.
     * @return Business Builder
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Fetches the Business's Owner.
     * @return Business Owner
     */
    @NotNull
    public OfflinePlayer getOwner() { return this.owner; }

    /**
     * Fetches the Business's Name.
     * @return Business Name
     */
    @NotNull
    public String getName() { return this.name; }

    /**
     * Fetches the Business's Unique ID.
     * @return Business ID
     */
    @NotNull
    public UUID getUniqueId() { return this.id; }

    /**
     * Fetches the List of Products this Business is selling.
     * @return Products to add
     */
    @NotNull
    public List<BusinessProduct> getProducts() { return this.products; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, ItemStack> map = new HashMap<>();
        AtomicInteger index = new AtomicInteger(0);
        resources.forEach(i -> map.put(index.getAndIncrement() + "", i));

        List<Product> p = new ArrayList<>();
        products.forEach(pr -> p.add(new Product(pr.getItem(), pr.getPrice())));

        return new HashMap<String, Object>() {{
            put("name", name);
            put("owner", owner);
            put("icon", icon.name());
            put("resources", map);
            put("products", p);
        }};
    }

    /**
     * Adds Resources available for purchase from this Business.
     * @param items Items to add
     * @return this Business, for chaining
     */
    @NotNull
    public Business addResource(@Nullable ItemStack... items) {
        if (items == null) return this;
        return addResource(Arrays.asList(items));
    }

    /**
     * Returns the Business's Resources available for selling via Products.
     * @return List of Resources used for selling with Products
     */
    @NotNull
    public List<ItemStack> getResources() {
        return this.resources;
    }

    /**
     * Adds a Collection of Resources available for purchase from this Business.
     * @param resources Items to add
     * @return this Business, fo rchaining
     */
    @NotNull
    public Business addResource(@Nullable Collection<? extends ItemStack> resources) {
        if (resources == null) return this;

        List<ItemStack> items = new ArrayList<>(resources);
        Map<Integer, ItemStack> res = new HashMap<>();
        AtomicInteger rIndex = new AtomicInteger();
        items.forEach(i -> {
            if (this.resources.contains(i)) {
                int index = this.resources.indexOf(i);
                if (this.resources.get(index).getAmount() < this.resources.get(index).getMaxStackSize()) {
                    ItemStack clone = i.clone();
                    clone.setAmount(i.getAmount() + 1);
                    res.put(rIndex.get(), clone);
                } else res.put(rIndex.get(), i);
            } else res.put(rIndex.get(), i);
            rIndex.getAndIncrement();
        });

        this.resources.addAll(res.values());
        saveBusiness();
        return this;
    }

    /**
     * Whether this item is in stock.
     * @param item Item to test
     * @return true if item is in stock, else false
     */
    public boolean isInStock(@Nullable ItemStack item) {
        if (item == null) return false;
        for (ItemStack i : resources) if (i.isSimilar(item)) return true;

        return false;
    }

    /**
     * Whether a Resource matching this Material is in stock.
     * @param m Material to use
     * @return true if material matches an item in stock, else false
     */
    public boolean isInStock(@Nullable Material m) {
        if (m == null) return false;
        for (ItemStack i : resources) if (i.getType() == m) return true;

        return false;
    }

    /**
     * Removes an Array of Products from this Business.
     * @param products Business Products to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeProduct(@Nullable BusinessProduct... products) {
        if (products == null) return this;
        if (products.length < 1) return this;
        return removeProduct(Arrays.asList(products));
    }

    /**
     * Removes an Array of Resources from this Business.
     * @param resources Resources to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeResource(@Nullable ItemStack... resources) {
        if (resources == null) return this;
        if (resources.length < 1) return this;
        return removeResource(Arrays.asList(resources));
    }

    /**
     * Removes a Collection of Resources from this Business.
     * @param resources Resources to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeResource(@Nullable Collection<? extends ItemStack> resources) {
        if (resources == null) return this;
        resources.forEach(i -> {
            if (this.resources.contains(i)) {
                int index = this.resources.indexOf(i);
                if (this.resources.get(index).getAmount() > this.resources.get(index).getMaxStackSize()) {
                    ItemStack clone = i.clone();
                    clone.setAmount(clone.getAmount() - 1);
                    this.resources.set(index, clone);
                } else this.resources.remove(i);
            }
        });
        saveBusiness();
        return this;
    }

    /**
     * Removes a Collection of Products from this Business.
     * @param products Products to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeProduct(@Nullable Collection<? extends BusinessProduct> products) {
        if (products == null) return this;

        products.forEach(pr -> {
            if (isProduct(pr.getItem())) this.products.remove(getProduct(pr.getItem()));
        });
        return this;
    }

    /**
     * Whether this Item is a Registered Product.
     * @param i Item to test
     * @return true if item is product, else false
     */
    public boolean isProduct(@Nullable ItemStack i) {
        if (i == null) return false;
        ItemStack item = i.clone();
        AtomicBoolean state = new AtomicBoolean(false);
        for (BusinessProduct p : this.products) {
            ItemStack pr = p.getItem();

            if (!pr.hasItemMeta() && !item.hasItemMeta() && item.getType() == pr.getType()) { state.set(true); break; }
            if (pr.isSimilar(item)) { state.set(true); break; }
        }

        return state.get();
    }

    /**
     * Whether one of the products has this Material as its product.
     * @param m Material to test
     * @return true if product has material, else false
     */
    public boolean isProduct(@Nullable Material m) {
        if (m == null) return false;
        AtomicBoolean state = new AtomicBoolean(false);
        this.products.forEach(p -> {
            if (p.getItem().getType() == m) state.set(true);
        });

        return state.get();
    }

    /**
     * Fetches a BusinessProduct by its Item.
     * @param item Item to fetch
     * @return Business Product found, or null if not found or if item is null
     */
    @Nullable
    public BusinessProduct getProduct(@Nullable ItemStack item) {
        if (item == null) return null;
        if (!isProduct(item)) return null;
        for (BusinessProduct p : this.products) {
            ItemStack pr = p.getItem();
            if (!pr.hasItemMeta() && !item.hasItemMeta() && item.getType() == pr.getType()) return p;
            if (pr.isSimilar(item)) return p;
        }

        return null;
    }

    /**
     * Adds an Array of Products to this Business.
     * @param products Products to Add
     * @return this Business, for chaining
     */
    @NotNull
    public Business addProduct(@Nullable Product... products) {
        if (products == null) return this;
        if (products.length < 1) return this;
        return addProduct(Arrays.asList(products));
    }

    /**
     * Fetches this Business's Icon.
     * @return Business Icon
     */
    @NotNull
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        meta.setDisplayName(ChatColor.YELLOW + this.name);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Adds a Collection of Products to this Business.
     * @param products Products to Add
     * @return this Business, for chaining
     */
    @NotNull
    public Business addProduct(@Nullable Collection<? extends Product> products) {
        if (products == null) return this;
        products.forEach(p -> this.products.add(new BusinessProduct(p, this)));
        saveBusiness();
        return this;
    }

    /**
     * Deserializes a Map into a Business.
     * @param serial Serialization from {@link #serialize()}
     * @return Deserialized Business
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Business deserialize(@Nullable Map<String, Object> serial) {
        if (serial == null) return null;

        Map<String, ItemStack> res = (Map<String, ItemStack>) serial.get("resources");
        List<ItemStack> resources = new ArrayList<>(res.values());

        return new Business(
                (String) serial.get("name"),
                Material.valueOf((String) serial.get("icon")),
                (OfflinePlayer) serial.get("owner"),
                (List<Product>) serial.get("products"), resources, false
        );
    }

    /**
     * Saves this Business to the Businesses File.
     */
    public void saveBusiness() {
        FileConfiguration config = NovaConfig.loadBusinesses();
        config.set(this.id.toString(), this);

        try { config.save(NovaConfig.getBusinessFile()); } catch (IOException e) { NovaConfig.getLogger().severe(e.getMessage()); }
    }

    /**
     * Fetches all registered Businesses.
     * @return All Registered Businesses
     */
    @NotNull
    public static List<Business> getBusinesses() {
        List<Business> businesses = new ArrayList<>();
        FileConfiguration config = NovaConfig.loadBusinesses();

        config.getKeys(false).forEach(s -> businesses.add((Business) config.get(s)));

        return businesses;
    }

    /**
     * Fetches a Business by its unique ID.
     * @param uid Business ID
     * @return Found business, or null if not found / ID is null
     */
    @Nullable
    public static Business getById(@Nullable UUID uid) {
        if (uid == null) return null;
        return getBusinesses().stream().filter(b -> b.id.equals(uid)).findFirst().orElse(null);
    }

    /**
     * Fetches a Business by its name.
     * @param name Business Name
     * @return Found business, or null if not found
     */
    @Nullable
    public static Business getByName(@Nullable String name) {
        if (name == null) return null;
        return getById(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Fetches a Business by its Owner.
     * @param p Owner of Business
     * @return Found business, or null if not found / Player is null
     */
    @Nullable
    public static Business getByOwner(@Nullable OfflinePlayer p) {
        if (p == null) return null;
        return getBusinesses().stream().filter(b -> b.owner.getUniqueId().equals(p.getUniqueId())).findFirst().orElse(null);
    }

    /**
     * Deletes a Business.
     * @param b Business to remove
     */
    public static void remove(@Nullable Business b) {
        if (b == null) return;
        FileConfiguration config = NovaConfig.loadBusinesses();

        config.set(b.id.toString(), null);
        try { config.save(NovaConfig.getBusinessFile()); } catch (IOException e) { NovaConfig.getLogger().severe(e.getMessage()); }
    }

    /**
     * Checks whether this Business exists.
     * @param name Business name
     * @return true if business exists, else false
     */
    public static boolean exists(@Nullable String name) {
        if (name == null) return false;
        return getByName(name) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Business business = (Business) o;
        return id.equals(business.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, owner, icon);
    }

    /**
     * Checks whether this Business exists.
     * @param uid Business ID
     * @return true if business exists, else false
     */
    public static boolean exists(@Nullable UUID uid) {
        if (uid == null) return false;
        return getById(uid) != null;
    }



    /**
     * Represents a Business Builder
     */
    public static final class Builder {

        String name;
        OfflinePlayer owner;
        Material icon;

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
         * Sets the Business's Material Icon.
         * @param icon Material Icon
         * @return this builder, for chaining
         */
        public Builder setIcon(@Nullable Material icon) {
            this.icon = icon;
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
         * @throws IllegalArgumentException if a part is missing or is null
         * @throws UnsupportedOperationException if Business exists
         */
        @NotNull
        public Business build() throws IllegalArgumentException, UnsupportedOperationException {
            Validate.notNull(owner, "Owner cannot be null");
            Validate.notNull(name, "Name cannot be null");
            Validate.notNull(icon, "Icon cannot be null");

            Business b = new Business(name, icon, owner, null, null, false);

            if (Business.exists(name)) throw new UnsupportedOperationException("Business already exists");

            b.saveBusiness();

            BusinessCreateEvent event = new BusinessCreateEvent(b);
            Bukkit.getPluginManager().callEvent(event);
            return b;
        }

    }

}
