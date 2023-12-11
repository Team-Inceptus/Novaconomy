package us.teaminceptus.novaconomy.api.business;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationInvite;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.business.BusinessCreateEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessSupplyEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Represents a Novaconomy Business
 */
@SuppressWarnings("unchecked")
public final class Business implements ConfigurationSerializable {

    // Constants

    /**
     * The maximum length of a Business Name.
     */
    public static final int MAX_NAME_LENGTH = 48;

    /**
     * The maximum number of keywords a Business can have.
     */
    public static final int MAX_KEYWORDS = 12;

    /**
     * The maximum length of a business keyword.
     */
    public static final int MAX_KETWORD_LENGTH = 16;

    // Usage

    private static final SecureRandom r = new SecureRandom();

    private final UUID id;
    private final OfflinePlayer owner;
    private final File folder;
    private final long creationDate;

    private String name;
    private Material icon;
    private Location home = null;
    private double advertisingBalance;
    private int customModelData;

    final BusinessStatistics stats;

    private final List<Product> products = new ArrayList<>();
    private final Map<Settings.Business<?>, Object> settings = new HashMap<>();
    private final List<ItemStack> resources = new ArrayList<>();
    private final List<String> keywords = new ArrayList<>();
    private final List<UUID> blacklist = new ArrayList<>();
    private final List<Location> supplyChests = new ArrayList<>();

    private Business(UUID uid, @NotNull String name, Material icon, OfflinePlayer owner, Collection<Product> products, Collection<ItemStack> resources,
                     BusinessStatistics stats, Map<Settings.Business<?>, Object> settings, long creationDate, List<String> keywords,
                     double advertisingBalance, List<UUID> blacklist) {
        this.id = uid;
        this.name = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        this.icon = icon;
        this.owner = owner;

        this.folder = new File(NovaConfig.getBusinessesFolder(), this.id.toString());

        this.creationDate = creationDate;
        this.stats = stats == null ? new BusinessStatistics(id) : stats;

        if (products != null) products.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getPrice().getEconomy() != null)
                .forEach(p -> { try { this.products.add(new BusinessProduct(p, this)); } catch (IllegalArgumentException ignored) {/* Economy was removed */} });
        if (resources != null) this.resources.addAll(resources);

        this.settings.putAll(settings);
        this.keywords.addAll(keywords);
        this.advertisingBalance = advertisingBalance;
        this.blacklist.addAll(blacklist);
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
     * Fetches the Business's Home Location.
     * @return Business Home Location, or null if not found
     */
    @Nullable
    public Location getHome() { return this.home; }

    /**
     * Whether this Busienss has a home set.
     * @return true if home set, else false
     */
    public boolean hasHome() {
        return this.home != null;
    }

    /**
     * Sets the Business's Home Location.
     * @param home Business Home Location
     */
    public void setHome(@Nullable Location home) {
        setHome(home, true);
    }

    /**
     * Sets the icon of this Business.
     * @param icon Business Icon
     * @throws IllegalArgumentException if icon is null
     */
    public void setIcon(@NotNull Material icon) throws IllegalArgumentException {
        Validate.notNull(icon, "Icon cannot be null");
        this.icon = icon;
        saveBusiness();
    }

    /**
     * Sets the name of this Business.
     * @param name Business Name
     * @throws IllegalArgumentException if name is null or empty
     */
    public void setName(@NotNull String name) throws IllegalArgumentException {
        if (name == null) throw new IllegalArgumentException("Name cannot be null");
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Name cannot be empty");

        this.name = name;
        saveBusiness();
    }

    private void setHome(Location home, boolean save) {
        this.home = home;
        if (save) saveBusiness();
    }

    /**
     * Whether this Player is the owner of this Business.
     * @param p Player to check
     * @return true if owner, else false
     */
    public boolean isOwner(@Nullable OfflinePlayer p) {
        if (p == null) return false;
        return owner.equals(p);
    }

    /**
     * Utility method to check if any Businesses currently exist.
     * @return true if any Businesses exist, else false
     */
    public static boolean exists() {
        File bFolder = NovaConfig.getBusinessesFolder();
        if (!bFolder.exists()) {
            bFolder.mkdir();
            return false;
        }

        return bFolder.listFiles((dir, name) -> new File(dir, name).isDirectory()).length > 0;
    }

    /**
     * Fetches the List of Products this Business is selling.
     * @return Products to add
     */
    @NotNull
    public List<BusinessProduct> getProducts() {
        this.products.removeIf(p -> p.getPrice().getEconomy() == null);
        return this.products.stream().map(p -> new BusinessProduct(p, this)).collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public Map<String, Object> serialize() {
        Map<String, ItemStack> map = new HashMap<>();
        AtomicInteger index = new AtomicInteger();
        resources.forEach(i -> map.put(String.valueOf(index.getAndIncrement()), i));

        List<Product> p = new ArrayList<>();
        products.forEach(pr -> p.add(new Product(pr.getItem(), pr.getPrice())));

        Map<String, Object> settings = this.settings
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().name().toLowerCase(), Map.Entry::getValue));

        return new HashMap<String, Object>() {
            private static final long serialVersionUID = -1783086253880529119L;

            {
            put("id", id.toString());
            put("name", name);
            put("owner", owner.toString());
            put("creation_date", creationDate);
            put("icon", icon.name());
            put("resources", map);
            put("products", p);
            put("stats", stats);
            put("settings", settings);

            if (home != null) put("home", home);
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
     * Returns an immutable version of Business's Resources available for selling via Products.
     * @return List of Resources used for selling with Products
     */
    @NotNull
    public List<ItemStack> getResources() {
        return ImmutableList.copyOf(this.resources);
    }

    /**
     * Adds a Collection of Resources available for purchase from this Business.
     * @param resources Items to add
     * @return this Business, fo rchaining
     */
    @NotNull
    public Business addResource(@Nullable Iterable<? extends ItemStack> resources) {
        if (resources == null) return this;

        List<ItemStack> resources0 = ImmutableList.copyOf(resources);
        List<ItemStack> items = new ArrayList<>(resources0);
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

        AtomicInteger amount = new AtomicInteger();
        resources0
            .stream()
            .map(ItemStack::getAmount)
            .forEach(amount::addAndGet);
        this.stats.totalResources += amount.get();

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
     * Fetches the total stock amount of this item.
     * @param item Item to test
     * @return Total stock amount of this item, or 0 if item is null or not found
     */
    public int getTotalStock(@Nullable ItemStack item) {
        if (item == null) return 0;

        AtomicInteger count = new AtomicInteger();
        List<ItemStack> items = new ArrayList<>(resources).stream().filter(i -> i.isSimilar(item)).collect(Collectors.toList());

        items.forEach(i -> count.addAndGet(i.getAmount()));

        return count.get();
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
     * Removes an Array of ItemStacks from this Business's Products.
     * @param items Products to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeProduct(@Nullable ItemStack... items) {
        if (items == null) return this;
        if (items.length < 1) return this;

        for (ItemStack item : items) this.products.removeIf(p -> isProduct(item));
        saveBusiness();

        return this;
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
     * Removes an iterable of Resources from this Business.
     * @param resources Resources to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeResource(@Nullable Iterable<? extends ItemStack> resources) {
        if (resources == null) return this;

        List<ItemStack> newR = new ArrayList<>();
        for (ItemStack item : resources)
            for (int i = 0; i < item.getAmount(); i++) {
                ItemStack clone = item.clone();
                clone.setAmount(1);
                newR.add(clone);
            }

        newR.forEach(i -> {
            Iterator<ItemStack> it = this.resources.iterator();
            while (it.hasNext()) {
                ItemStack item = it.next();
                if (item.isSimilar(i)) {
                    if (item.getAmount() > i.getAmount()) item.setAmount(item.getAmount() - i.getAmount());
                    else it.remove();
                    break;
                }
            }
        });

        saveBusiness();
        return this;
    }

    /**
     * Removes an iterable of Products from this Business.
     * @param products Products to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeProduct(@Nullable Iterable<? extends BusinessProduct> products) {
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
        AtomicBoolean state = new AtomicBoolean();
        for (Product p : this.products) {
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
        AtomicBoolean state = new AtomicBoolean();
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
        for (Product p : this.products) {
            ItemStack pr = p.getItem();
            if (!pr.hasItemMeta() && !item.hasItemMeta() && item.getType() == pr.getType()) return new BusinessProduct(p, this);
            if (pr.isSimilar(item)) return new BusinessProduct(p, this);
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
     * <p>Fetches any extra stock that are no longer products in the Business.</p>
     * <p>This method can be used to fetch stock of products that sold on deleted economies.</p>
     * @return Extra Stock
     */
    @NotNull
    public List<ItemStack> getLeftoverStock() {
        return getResources().stream().filter(i -> !isProduct(i)).collect(Collectors.toList());
    }

    /**
     * Fetches a Setting Value for this Business.
     * @param setting Setting to check
     * @return Setting Value, or false if setting is null
     */
    public <T> T getSetting(@Nullable Settings.Business<T> setting) {
        if (setting == null) return null;
        return (T) this.settings.getOrDefault(setting, setting.getDefaultValue());
    }

    /**
     * Fetches an immutable version of all of the settings on this business.
     * @return Immutable Map of Settings
     */
    @NotNull
    public Map<Settings.Business<?>, Object> getSettings() {
        Map<Settings.Business<?>, Object> dupl = new HashMap<>(this.settings);
        for (Settings.Business<?> value : Settings.Business.values()) dupl.putIfAbsent(value, value.getDefaultValue());

        return ImmutableMap.copyOf(dupl);
    }

    /**
     * Fetches a list of all ratings for this Business.
     * @return List of Ratings
     */
    @NotNull
    public List<Rating> getRatings() {
        List<Rating> ratings = new ArrayList<>();

        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (owner.equals(p)) continue;
            NovaPlayer np = new NovaPlayer(p);
            if (!np.hasRating(this)) continue;

            ratings.add(np.getRating(this));
        }

        ratings = ratings.stream().filter(Objects::nonNull).collect(Collectors.toList());

        return ImmutableList.copyOf(ratings);
    }

    /**
     * Fetches the average rating level for this Business.
     * @return Average Rating Level
     */
    public double getAverageRating() {
        AtomicDouble average = new AtomicDouble();
        getRatings().forEach(r -> average.addAndGet(r.getRatingLevel()));
        return average.get() / getRatings().size();
    }

    /**
     * Sets a Setting Value for this Business.
     * @param setting Setting to set
     * @param value Value of the new setting
     * @return this Business, for chaining
     */
    @NotNull
    public <T> Business setSetting(@NotNull Settings.Business<T> setting, T value) {
        this.settings.put(setting, value);
        saveBusiness();
        return this;
    }

    /**
     * Fetches a list of all Businesses that are prohibited from associating with this Business, such as with Advertising.
     * @return List of Blacklisted Businesses
     */
    @NotNull
    public List<Business> getBlacklist() {
        return ImmutableList.copyOf(blacklist.stream()
                .map(Business::byId)
                .collect(Collectors.toList()));
    }

    /**
     * Adds a Business to this Business's blacklist.
     * @param business Business to add
     * @return this Business, for chaining
     * @throws IllegalArgumentException if business is this business
     */
    @NotNull
    public Business blacklist(@NotNull Business business) throws IllegalArgumentException {
        if (this.equals(business)) throw new IllegalArgumentException("Cannot blacklist self");
        if (this.blacklist.contains(business.id)) return this;
        if (business.blacklist.contains(this.id)) return this;

        this.blacklist.add(business.id);
        saveBusiness();
        return this;
    }

    /**
     * Checks whether a Business is blacklisted from associating with this Business.
     * @param business Business to check
     * @return true if blacklisted, else false
     */
    public boolean isBlacklisted(@NotNull Business business) {
        if (this.equals(business)) return false;
        return this.blacklist.contains(business.id) || business.blacklist.contains(this.id);
    }

    /**
     * Removes a Business from this Business's blacklist.
     * @param business Business to remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business unblacklist(@NotNull Business business) {
        if (this.equals(business)) return this;

        if (this.blacklist.contains(business.id)) {
            this.blacklist.remove(business.id);
            saveBusiness();
        } else if (business.blacklist.contains(this.id)) {
            business.blacklist.remove(this.id);
            business.saveBusiness();
        }
        return this;
    }

    /**
     * Fetches the custom model data ID for this Business.
     * @return Custom Model Data
     */
    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * Sets the custom model data ID for this Business.
     * @param customModelData Custom Model Data
     */
    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        saveBusiness();
    }

    /**
     * Fetches an immutable set of all of the supply chests for this Business.
     * @return Immutable Set of Supply Chest States
     */
    @NotNull
    public Set<Chest> getSupplyChests() {
        Set<Location> chests = new HashSet<>();
        if (supplyChests.isEmpty()) return ImmutableSet.of();

        Iterator<Location> it = supplyChests.iterator();
        while (it.hasNext()) {
            Location loc = it.next();

            if (loc.getBlock() == null || !(loc.getBlock().getState() instanceof Chest)) {
                it.remove();
                continue;
            }

            chests.add(loc);
        }

        return ImmutableSet.copyOf(
                chests.stream()
                        .map(Location::getBlock)
                        .map(Block::getState)
                        .map(Chest.class::cast)
                        .collect(Collectors.toSet())
        );
    }

    /**
     * Adds a supply chest to this Business.
     * @param chest Chest Block to add
     */
    @NotNull
    public void addSupplyChest(@NotNull Chest chest) {
        if (chest == null) throw new IllegalArgumentException("Chest cannot be null");
        if (chest.getBlock() == null) throw new IllegalArgumentException("Chest must be placed");

        supplyChests.add(chest.getLocation());
        saveBusiness();
    }

    /**
     * Adds a supply chest to this Business.
     * @param loc Location of the chest
     */
    @NotNull
    public void addSupplyChest(@NotNull Location loc) {
        if (loc == null) throw new IllegalArgumentException("Location cannot be null");
        if (loc.getWorld() == null) throw new IllegalArgumentException("Location must be in a world");
        if (!(loc.getBlock().getState() instanceof Chest)) throw new IllegalArgumentException("Location must be a chest or double chest");

        supplyChests.add(loc.clone());
        saveBusiness();
    }

    /**
     * Removes a supply chest from this Business.
     * @param loc Location of the chest
     */
    @NotNull
    public void removeSupplyChest(@Nullable Location loc) {
        if (loc == null) return;

        supplyChests.remove(loc);
        saveBusiness();
    }

    /**
     * Adds the resources from all of the supply chests to the Business's inventory, calling {@link BusinessSupplyEvent}. This method will silently fail if the event is cancelled.
     */
    public void supply() {
        List<Map.Entry<ItemStack, Chest>> items = new ArrayList<>();
        Set<Chest> supply = getSupplyChests();
        if (supply.isEmpty()) return;

        for (Chest chest : supply) {
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR || !isProduct(item)) continue;
                items.add(new AbstractMap.SimpleEntry<>(item, chest));
            }
        }

        BusinessSupplyEvent event = new BusinessSupplyEvent(this, items.stream().map(Map.Entry::getKey).collect(Collectors.toSet()), supply);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            for (ItemStack item : event.getItems()) {
                if (item == null || item.getType() == Material.AIR || !isProduct(item)) continue;
                addResource(item);

                if (items.stream().anyMatch(e -> e.getKey().equals(item)))
                    items.stream().filter(e -> e.getKey().equals(item))
                            .forEach(e -> e.getValue().getInventory().removeItem(item));
            }
        }
    }

    /**
     * Deserializes a Map into a Business.
     * @param serial Serialization from {@link #serialize()}
     * @deprecated Entire Bukkit Serialization is no longer used; this method exists only for the purposes of converting legacy businesses
     * @return Deserialized Business
     */
    @Deprecated
    @Nullable
    public static Business deserialize(@Nullable Map<String, Object> serial) {
        if (serial == null) return null;

        Map<String, ItemStack> res = (Map<String, ItemStack>) serial.get("resources");
        List<ItemStack> resources = new ArrayList<>(res.values());
        Location home = serial.containsKey("home") ? (Location) serial.get("home") : null;

        Map<Settings.Business<?>, Object> settings = serial.containsKey("settings") ? ((Map<String, Object>) serial.get("settings"))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> Settings.Business.valueOf(e.getKey()), Map.Entry::getValue)) : null;

        long creationDate = serial.containsKey("creation_date") ? (long) serial.get("creation_date") : 1242518400000L;

        String name = (String) serial.get("name");
        UUID uid = serial.containsKey("id") ? UUID.fromString((String) serial.get("id")) : UUID.nameUUIDFromBytes(name.getBytes());
        List<String> keywords = (List<String>) serial.getOrDefault("keywords", new ArrayList<>());

        Object ownerO = serial.get("owner");
        OfflinePlayer owner = ownerO instanceof OfflinePlayer ? (OfflinePlayer) ownerO : Bukkit.getOfflinePlayer(UUID.fromString(serial.get("owner").toString()));

        Business b = new Business(
                uid,
                name,
                Material.valueOf((String) serial.get("icon")),
                owner,
                (List<Product>) serial.get("products"), resources,
                (BusinessStatistics) serial.getOrDefault("stats", null),
                settings, creationDate, keywords, 0, new ArrayList<>()
        );
        b.setHome(home, false);

        return b;
    }

    /**
     * Fetches the total amount of money this Business has earned, accounting for conversion scales.
     * @return Total Money Earned by this Business
     */
    public double getTotalRevenue() {
        double revenue = 0;
        for (Map.Entry<Product, Integer> entry : stats.getProductSales().entrySet()) {
            Product p = entry.getKey();
            double amount = p.getPrice().getAmount() * entry.getValue();
            if (p.getEconomy() != null) amount *= p.getEconomy().getConversionScale();

            revenue += amount;
        }

        return revenue;
    }

    /**
     * Fetches the folder this Business is stored in, if {@link NovaConfig#isDatabaseEnabled()} is false.
     * @return Business Folder
     */
    @NotNull
    public File getBusinessFolder() {
        return folder;
    }

    private static void checkTable() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS businesses (" +
                "id CHAR(36) NOT NULL," +
                "owner CHAR(36) NOT NULL," +
                "creation_date BIGINT NOT NULL," +
                "name VARCHAR(" + MAX_NAME_LENGTH + ") NOT NULL," +
                "icon VARCHAR(192) NOT NULL," +
                "home BLOB(65535)," +
                "products LONGBLOB NOT NULL," +
                "resources LONGBLOB NOT NULL," +
                "settings BLOB(65535) NOT NULL," +
                "stats MEDIUMBLOB NOT NULL," +
                "keywords BLOB(65535) NOT NULL," +
                "adbalance DOUBLE NOT NULL," +
                "blacklist BLOB(65535) NOT NULL," +
                "PRIMARY KEY (id))"
        );

        DatabaseMetaData md = db.getMetaData();
        ResultSet rs = null;

        try {
            if (!(rs = md.getColumns(null, null, "businesses", "custom_model_data")).next())
                db.createStatement().execute("ALTER TABLE businesses ADD custom_model_data INT NOT NULL");

            if (!(rs = md.getColumns(null, null, "businesses", "supply_chests")).next())
                db.createStatement().execute("ALTER TABLE businesses ADD supply_chests LONGBLOB NOT NULL");
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
     * Saves this Business in {@link #getBusinessFolder()}.
     */
    public void saveBusiness() {
        try {
            BUSINESS_CACHE.clear();
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                writeDB();
            } else {
                if (!folder.exists()) folder.mkdir();
                writeFile();
            }
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private void writeDB() throws IOException, SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        String sql;

        try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM businesses WHERE id = \"" + this.id + "\"")) {
            if (rs.next())
                sql = "UPDATE businesses SET " +
                        "id = ?, " +
                        "owner = ?, " +
                        "creation_date = ?, " +
                        "name = ?, " +
                        "icon = ?, " +
                        "home = ?, " +
                        "products = ?, " +
                        "resources = ?, " +
                        "settings = ?, " +
                        "stats = ?, " +
                        "keywords = ?, " +
                        "adbalance = ?, " +
                        "blacklist = ?, " +
                        "custom_model_data = ?, " +
                        "supply_chests = ? " +
                        "WHERE id = \"" + this.id + "\"";
            else
                sql = "INSERT INTO businesses VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        PreparedStatement ps = db.prepareStatement(sql);
        ps.setString(1, this.id.toString());
        ps.setString(2, this.owner.getUniqueId().toString());
        ps.setLong(3, this.creationDate);
        ps.setString(4, this.name);
        ps.setString(5, this.icon.name());

        if (this.home == null) ps.setBytes(6, null);
        else {
            ByteArrayOutputStream homeOs = new ByteArrayOutputStream();
            BukkitObjectOutputStream homeBos = new BukkitObjectOutputStream(homeOs);
            homeBos.writeObject(this.home);
            homeBos.close();
            ps.setBytes(6, homeOs.toByteArray());
        }

        ByteArrayOutputStream pOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream pBos = new BukkitObjectOutputStream(new BufferedOutputStream(pOs));

        List<Map<String, Object>> prods = new ArrayList<>();
        for (Product p : this.products) {
            Map<String, Object> m = new HashMap<>(p.serialize());
            Map<String, Object> item = new HashMap<>(p.getItem().serialize());

            if (p.getItem().hasItemMeta()) item.put("meta", p.getItem().getItemMeta().serialize());

            m.put("item", item);
            prods.add(m);
        }

        pBos.writeObject(prods);
        pBos.close();
        ps.setBytes(7, pOs.toByteArray());

        ByteArrayOutputStream resourcesOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream resourcesBos = new BukkitObjectOutputStream(resourcesOs);
        resourcesBos.writeObject(this.resources);
        resourcesBos.close();
        ps.setBytes(8, resourcesOs.toByteArray());

        ByteArrayOutputStream settingsOs = new ByteArrayOutputStream();
        ObjectOutputStream settingsBos = new ObjectOutputStream(settingsOs);
        settingsBos.writeObject(this.settings);
        settingsBos.close();
        ps.setBytes(9, settingsOs.toByteArray());

        ByteArrayOutputStream statsOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream statsBos = new BukkitObjectOutputStream(statsOs);
        statsBos.writeObject(this.stats == null ? new BusinessStatistics(this) : this.stats);
        statsBos.close();
        ps.setBytes(10, statsOs.toByteArray());

        byte[] keywords = String.join(",", this.keywords).getBytes(StandardCharsets.UTF_8);
        ps.setBytes(11, keywords);
        ps.setDouble(12, this.advertisingBalance);

        ByteArrayOutputStream blacklistOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream blacklistBos = new BukkitObjectOutputStream(blacklistOs);
        blacklistBos.writeObject(this.blacklist);
        blacklistBos.close();
        ps.setBytes(13, blacklistOs.toByteArray());

        ps.setInt(14, this.customModelData);

        ByteArrayOutputStream supplyChestsOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream supplyChestsBos = new BukkitObjectOutputStream(supplyChestsOs);
        supplyChestsBos.writeObject(this.supplyChests);
        supplyChestsBos.close();
        ps.setBytes(15, supplyChestsOs.toByteArray());

        ps.executeUpdate();
        ps.close();
    }

    private static Business readDB(ResultSet rs) throws IOException, ReflectiveOperationException, SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("owner")));
        long creationDate = rs.getLong("creation_date");
        String name = rs.getString("name");
        Material icon = Material.valueOf(rs.getString("icon"));

        Location home = null;
        if (rs.getBytes("home") != null) {
            ByteArrayInputStream homeIs = new ByteArrayInputStream(rs.getBytes("home"));
            BukkitObjectInputStream homeBis = new BukkitObjectInputStream(homeIs);
            home = (Location) homeBis.readObject();
            homeBis.close();
        }

        List<Product> products = new ArrayList<>();
        ByteArrayInputStream pIs = new ByteArrayInputStream(rs.getBytes("products"));
        BukkitObjectInputStream pBis = new BukkitObjectInputStream(new BufferedInputStream(pIs));

        List<Map<String, Object>> prods = (List<Map<String, Object>>) pBis.readObject();
        pBis.close();
        for (Map<String, Object> m : prods) {
            if (m == null) continue;

            Map<String, Object> sProduct = new HashMap<>(m);
            Map<String, Object> sItem = new HashMap<>((Map<String, Object>) m.get("item"));

            ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) sItem.get("type")));
            if (base == null) continue;

            DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
            Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
            deserialize.setAccessible(true);

            Map<String, Object> sMeta = (Map<String, Object>) sItem.getOrDefault("meta", base.serialize());
            sItem.put("meta", sMeta == null ? base : deserialize.invoke(null, sMeta));
            ItemStack item = ItemStack.deserialize(sItem);

            sProduct.put("item", item);
            products.add(Product.deserialize(sProduct));
        }

        ByteArrayInputStream resourcesIs = new ByteArrayInputStream(rs.getBytes("resources"));
        BukkitObjectInputStream resourcesBis = new BukkitObjectInputStream(resourcesIs);
        List<ItemStack> resources = (List<ItemStack>) resourcesBis.readObject();
        resourcesBis.close();

        ByteArrayInputStream settingsIs = new ByteArrayInputStream(rs.getBytes("settings"));
        ObjectInputStream settingsBis = new ObjectInputStream(settingsIs);
        Map<Settings.Business<?>, Object> settings = (Map<Settings.Business<?>, Object>) settingsBis.readObject();
        settingsBis.close();

        ByteArrayInputStream statsIs = new ByteArrayInputStream(rs.getBytes("stats"));
        BukkitObjectInputStream statsBis = new BukkitObjectInputStream(statsIs);
        BusinessStatistics stats = (BusinessStatistics) statsBis.readObject();
        statsBis.close();

        List<String> keywords = Arrays.asList(new String(rs.getBytes("keywords")).split(","));
        double adBal = rs.getDouble("adbalance");

        ByteArrayInputStream blacklistIs = new ByteArrayInputStream(rs.getBytes("blacklist"));
        BukkitObjectInputStream blacklistBis = new BukkitObjectInputStream(blacklistIs);
        List<UUID> blacklist = (List<UUID>) blacklistBis.readObject();
        blacklistBis.close();

        ByteArrayInputStream supplyChestsIs = new ByteArrayInputStream(rs.getBytes("supply_chests"));
        BukkitObjectInputStream supplyChestsBis = new BukkitObjectInputStream(supplyChestsIs);
        List<Location> supplyChests = (List<Location>) supplyChestsBis.readObject();
        supplyChestsBis.close();

        Business b = new Business(id, name, icon, owner, products, resources, stats, settings, creationDate, keywords, adBal, blacklist);
        b.setHome(home, false);

        b.customModelData = rs.getInt("custom_model_data");
        b.supplyChests.addAll(supplyChests);
        return b;
    }

    private void writeFile() throws IOException {
        File info = new File(folder, "information.dat");
        if (!info.exists()) info.createNewFile();

        // Permanent Information

        FileOutputStream infoFs = new FileOutputStream(info);
        ObjectOutputStream infoOs = new ObjectOutputStream(infoFs);
        infoOs.writeObject(this.id);
        infoOs.writeLong(this.creationDate);
        infoOs.writeObject(this.owner.getUniqueId());
        infoOs.close();

        // Statistics

        File stats = new File(folder, "stats.yml");
        if (!stats.exists()) stats.createNewFile();

        FileConfiguration sConfig = YamlConfiguration.loadConfiguration(stats);
        sConfig.set("stats", this.stats);
        sConfig.save(stats);

        // Settings

        File settings = new File(folder, "settings.yml");
        if (!settings.exists()) settings.createNewFile();

        FileConfiguration seConfig = YamlConfiguration.loadConfiguration(settings);
        if (!seConfig.isConfigurationSection("settings")) seConfig.createSection("settings");
        ConfigurationSection settingsSection = seConfig.getConfigurationSection("settings");

        this.settings.entrySet()
                .stream()
                .map(e -> {
                    Object value = e.getValue();
                    if (value instanceof Enum<?>) value = value.toString();

                    return new AbstractMap.SimpleEntry<>(e.getKey().name().toLowerCase(), value);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach(settingsSection::set);

        seConfig.save(settings);

        // Products

        File products = new File(folder, "products.dat");
        if (!products.exists()) products.createNewFile();

        FileOutputStream pFs = new FileOutputStream(products);
        BukkitObjectOutputStream pOs = new BukkitObjectOutputStream(new BufferedOutputStream(pFs));

        List<Map<String, Object>> prods = new ArrayList<>();
        for (Product p : this.products) {
            Map<String, Object> m = new HashMap<>(p.serialize());

            Map<String, Object> item = new HashMap<>(p.getItem().serialize());

            if (p.getItem().hasItemMeta()) item.put("meta", p.getItem().getItemMeta().serialize());

            m.put("item", item);
            prods.add(m);
        }

        pOs.writeObject(prods);
        pOs.close();

        // Resources

        File sourcesF = new File(folder, "resources");
        if (!sourcesF.exists()) sourcesF.mkdir();

        Set<Material> mats = this.products.stream().map(Product::getItem).map(ItemStack::getType).collect(Collectors.toSet());
        for (Material mat : mats) {
            List<ItemStack> added = this.resources.stream().filter(i -> i.getType() == mat).collect(Collectors.toList());

            File matF = new File(sourcesF, mat.name().toLowerCase() + ".dat");
            if (!matF.exists()) matF.createNewFile();

            FileOutputStream matFs = new FileOutputStream(matF);
            BukkitObjectOutputStream matOs = new BukkitObjectOutputStream(new BufferedOutputStream(matFs));

            matOs.writeInt(added.stream().filter(i -> !i.hasItemMeta()).mapToInt(ItemStack::getAmount).sum());

            List<Map<String, Object>> res = new ArrayList<>();
            for (ItemStack i : added.stream().filter(ItemStack::hasItemMeta).collect(Collectors.toList())) {
                Map<String, Object> m = new HashMap<>(i.serialize());
                if (i.hasItemMeta()) m.put("meta", i.getItemMeta().serialize());

                res.add(m);
            }
            matOs.writeObject(res);
            matOs.close();
        }

        // Supply Chests

        File supply = new File(folder, "supply.yml");
        if (!supply.exists()) supply.createNewFile();
        
        FileConfiguration suConfig = YamlConfiguration.loadConfiguration(supply);
        suConfig.set("supply_chests", this.supplyChests);
        suConfig.save(supply);

        // Other Information

        File other = new File(folder, "other.yml");
        if (!other.exists()) other.createNewFile();

        FileConfiguration oConfig = YamlConfiguration.loadConfiguration(other);
        oConfig.set("name", this.name);
        oConfig.set("icon", this.icon.name());
        oConfig.set("keywords", this.keywords);
        oConfig.set("home", this.home);
        oConfig.set("adverising_balance", this.advertisingBalance);
        oConfig.set("custom_model_data", this.customModelData);

        List<String> blacklisted = this.blacklist.stream().map(UUID::toString).collect(Collectors.toList());
        oConfig.set("blacklist", blacklisted);

        oConfig.save(other);
    }

    private static Business readFile(File folder) throws IOException, ReflectiveOperationException {
        File info = new File(folder, "information.dat");
        if (!info.exists()) throw new IllegalStateException("Business information file does not exist!");

        // Info

        FileInputStream infoFs = new FileInputStream(info);
        ObjectInputStream infoOs = new ObjectInputStream(infoFs);
        UUID id = (UUID) infoOs.readObject();
        long creationDate = infoOs.readLong();
        OfflinePlayer owner = Bukkit.getOfflinePlayer((UUID) infoOs.readObject());
        infoOs.close();

        // Other

        File other = new File(folder, "other.yml");
        if (!other.exists()) throw new IllegalStateException("Business \"other.yml\" file does not exist!");

        FileConfiguration oConfig = YamlConfiguration.loadConfiguration(other);
        String name = oConfig.getString("name");
        Material icon = Material.valueOf(oConfig.getString("icon"));
        List<String> keywords = oConfig.getStringList("keywords");
        Location home = (Location) oConfig.get("home");
        double advertisingBalance = oConfig.getDouble("adverising_balance", 0);
        List<UUID> blacklist = oConfig.getStringList("blacklist").stream().map(UUID::fromString).collect(Collectors.toList());

        // Products

        List<Product> product = new ArrayList<>();
        File products = new File(folder, "products.dat");
        if (products.exists()) {
            FileInputStream pFs = new FileInputStream(products);
            BukkitObjectInputStream pOs = new BukkitObjectInputStream(new BufferedInputStream(pFs));

            List<Map<String, Object>> prods = (List<Map<String, Object>>) pOs.readObject();
            pOs.close();

            for (Map<String, Object> m : prods) {
                if (m == null) continue;

                Map<String, Object> sProduct = new HashMap<>(m);
                Map<String, Object> sItem = new HashMap<>((Map<String, Object>) m.get("item"));

                ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) sItem.get("type")));
                if (base == null) continue;

                DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
                Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
                deserialize.setAccessible(true);

                Map<String, Object> sMeta = (Map<String, Object>) sItem.getOrDefault("meta", base.serialize());
                sItem.put("meta", sMeta == null ? base : deserialize.invoke(null, sMeta));
                ItemStack item = ItemStack.deserialize(sItem);

                sProduct.put("item", item);
                product.add(Product.deserialize(sProduct));
            }
        }

        // Statistics

        BusinessStatistics stats = null;
        File statsFile = new File(folder, "stats.yml");
        if (statsFile.exists()) {
            FileConfiguration sConfig = YamlConfiguration.loadConfiguration(statsFile);
            stats = (BusinessStatistics) sConfig.get("stats");
        }

        // Settings

        Map<Settings.Business<?>, Object> settings = new HashMap<>();
        File settingsF = new File(folder, "settings.yml");

        if (settingsF.exists()) {
            FileConfiguration seConfig = YamlConfiguration.loadConfiguration(settingsF);
            if (seConfig.isConfigurationSection("settings"))
                settings.putAll(seConfig.getConfigurationSection("settings").getValues(false)
                        .entrySet()
                        .stream()
                        .map(e -> {
                            Settings.Business<?> key = Arrays.stream(Settings.Business.values())
                                    .filter(sett -> sett.name().equalsIgnoreCase(e.getKey()))
                                    .findFirst()
                                    .orElseThrow(IllegalStateException::new);

                            return new AbstractMap.SimpleEntry<>(key, key.parseValue(e.getValue().toString()));
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        // Resources

        File sourcesF = new File(folder, "resources");
        if (!sourcesF.exists()) sourcesF.mkdir();

        List<ItemStack> resources = new ArrayList<>();
        for (File matF : sourcesF.listFiles()) {
            FileInputStream matFs = new FileInputStream(matF);
            BukkitObjectInputStream matOs = new BukkitObjectInputStream(new BufferedInputStream(matFs));

            int baseCount = matOs.readInt();
            Material baseM = Material.valueOf(matF.getName().replace(".dat", "").toUpperCase());

            while (baseCount > 0) {
                int amount = Math.min(baseCount, baseM.getMaxStackSize());
                ItemStack i = new ItemStack(baseM, amount);
                resources.add(i);

                baseCount -= amount;
            }

            List<Map<String, Object>> res = (List<Map<String, Object>>) matOs.readObject();
            for (Map<String, Object> m : res) {
                if (m == null) continue;
                Map<String, Object> sItem = new HashMap<>(m);

                ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) m.get("type")));
                if (base == null) continue;

                DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
                Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
                deserialize.setAccessible(true);

                Map<String, Object> sMeta = (Map<String, Object>) sItem.getOrDefault("meta", base.serialize());
                sItem.put("meta", sMeta == null ? base : deserialize.invoke(null, sMeta));
                ItemStack i = ItemStack.deserialize(sItem);

                resources.add(i);
            }

            matOs.close();
        }

        // Supply Chests
        List<Location> supplyChests = new ArrayList<>();
        File supply = new File(folder, "supply.yml");
        if (supply.exists()) {
            FileConfiguration sConfig = YamlConfiguration.loadConfiguration(supply);
            supplyChests.addAll((List<Location>) sConfig.get("supply_chests"));
        }

        Business b = new Business(id, name, icon, owner, product, resources, stats, settings, creationDate, keywords, advertisingBalance, blacklist);
        b.setHome(home, false);

        b.customModelData = oConfig.getInt("custom_model_data");
        b.supplyChests.addAll(supplyChests);
        return b;
    }

    private static final Set<Business> BUSINESS_CACHE = new HashSet<>();

    /**
     * Reloads the Business Cache.
     */
    public static void reloadBusinesses() {
        BUSINESS_CACHE.clear();
        getBusinesses();
    }

    /**
     * Fetches an immutable version of all registered Businesses.
     * @return All Registered Businesses
     * @throws IllegalStateException if a business file is invalid
     */
    @NotNull
    public static Set<Business> getBusinesses() throws IllegalStateException {
        if (!BUSINESS_CACHE.isEmpty()) return ImmutableSet.copyOf(BUSINESS_CACHE);

        Set<Business> businesses = new HashSet<>();
        if (NovaConfig.getConfiguration().isDatabaseEnabled())
            try {
                checkTable();
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                PreparedStatement ps = db.prepareStatement("SELECT *, COUNT(*) FROM businesses GROUP BY id");
                ResultSet rs = ps.executeQuery();

                while (rs.next()) businesses.add(readDB(rs));

                rs.close();
                ps.close();
            } catch (Exception e) {
                NovaConfig.print(e);
            }
        else {
            List<File> files = NovaConfig.getBusinessesFolder().listFiles() == null ? new ArrayList<>() : Arrays.asList(NovaConfig.getBusinessesFolder().listFiles());
            for (File f : files) {
                if (!f.isDirectory()) continue;

                Business b;

                try {
                    b = readFile(f.getAbsoluteFile());
                } catch (OptionalDataException e) {
                    NovaConfig.print(e);
                    continue;
                } catch (IOException | ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }

                businesses.add(b);
            }
        }

        BUSINESS_CACHE.addAll(businesses);

        return ImmutableSet.copyOf(BUSINESS_CACHE);
    }

    /**
     * Fetches a Business by its unique ID.
     * @param uid Business ID
     * @return Found business, or null if not found / ID is null
     * @throws IllegalStateException if the business file is malformed
     */
    @Nullable
    public static Business byId(@Nullable UUID uid) throws IllegalStateException {
        if (uid == null) return null;

        Business b = null;

        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                PreparedStatement ps = db.prepareStatement("SELECT * FROM businesses WHERE id = ?");
                ps.setString(1, uid.toString());

                ResultSet rs = ps.executeQuery();
                if (rs.next()) b = readDB(rs);

                rs.close();
                ps.close();
            } else {
                File f = new File(NovaConfig.getBusinessesFolder(), uid.toString());
                if (!f.exists()) return null;
                b = readFile(f);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return b;
    }

    /**
     * Fetches a Business by its name.
     * @param name Business Name
     * @return Found business, or null if not found
     */
    @Nullable
    public static Business byName(@Nullable String name) {
        if (name == null) return null;
        return getBusinesses().stream().filter(b -> b.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /**
     * Fetches a Business by its Owner.
     * @param p Owner of Business
     * @return Found business, or null if not found / Player is null
     */
    @Nullable
    public static Business byOwner(@Nullable OfflinePlayer p) {
        if (p == null) return null;
        return getBusinesses().stream().filter(b -> b.isOwner(p)).findFirst().orElse(null);
    }

    /**
     * Deletes a Business.
     * @param b Business to remove
     */
    public static void remove(@Nullable Business b) {
        if (b == null) return;

        BUSINESS_CACHE.clear();

        if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
            Connection db = NovaConfig.getConfiguration().getDatabaseConnection();
            try {
                PreparedStatement ps = db.prepareStatement("DELETE FROM businesses WHERE id = ?");
                ps.setString(1, b.id.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                NovaConfig.print(e);
            }
        } else {
            for (File f : b.folder.listFiles()) {
                if (f == null) continue;
                f.delete();
            }

            b.folder.delete();
        }
    }

    /**
     * Checks whether this Business exists.
     * @param name Business name
     * @return true if business exists, else false
     */
    public static boolean exists(@Nullable String name) {
        if (name == null) return false;
        return byName(name) != null;
    }

    /**
     * Fetches if any of the Businesses have claimed this Supply Chest.
     * @param loc Supply Chest Location
     * @return true if claimed, else false
     */
    public static boolean isSupplyClaimed(@Nullable Location loc) {
        if (loc == null) return false;

        return getBusinesses().stream().anyMatch(b -> b.getSupplyChests().stream().anyMatch(c -> c.getLocation().equals(loc)));
    }

    /**
     * Fetches the Business's Advertising Balance.
     * @return Advertising Balance
     */
    public double getAdvertisingBalance() {
        return advertisingBalance;
    }

    /**
     * Sets the Business's Advertising Balance.
     * @param advertisingBalance Advertising Balance
     * @return this business, for chaining
     */
    @NotNull
    public Business setAdvertisingBalance(double advertisingBalance) {
        this.advertisingBalance = advertisingBalance;
        saveBusiness();
        return this;
    }

    /**
     * Adds to the Business's Advertising Balance.
     * @param amount Amount to add
     * @return this business, for chaining
     */
    @NotNull
    public Business addAdvertisingBalance(double amount) {
        return setAdvertisingBalance(advertisingBalance + amount);
    }

    /**
     * Adds to the Business's Advertising Balance.
     * @param price Price to add
     * @return this business, for chaining
     */
    @NotNull
    public Business addAdvertisingBalance(@Nullable Price price) {
        if (price == null) return this;
        return addAdvertisingBalance(price.getAmount() * price.getEconomy().getConversionScale());
    }

    /**
     * Adds to the Business's Advertising Balance.
     * @param amount Amount to add
     * @param econ Economy to use with the Conversion Scale
     * @return this business, for chaining
     */
    @NotNull
    public Business addAdvertisingBalance(double amount, @Nullable Economy econ) {
        return addAdvertisingBalance(amount * (econ == null ? 1 : econ.getConversionScale()));
    }

    /**
     * Removes from the Business's Advertising Balance.
     * @param amount Amount to remove
     * @return this business, for chaining
     */
    @NotNull
    public Business removeAdvertisingBalance(double amount) {
        return setAdvertisingBalance(advertisingBalance - amount);
    }

    /**
     * Removes from the Business's Advertising Balance.
     * @param amount Amount to remove
     * @param econ Economy to use with the Conversion Scale
     * @return this business, for chaining
     */
    @NotNull
    public Business removeAdvertisingBalance(double amount, @Nullable Economy econ) {
        return removeAdvertisingBalance(amount * (econ == null ? 1 : econ.getConversionScale()));
    }

    /**
     * Removes from the Business's Advertising Balance.
     * @param p Price to remove
     * @return this business, for chaining
     */
    @NotNull
    public Business removeAdvertisingBalance(@Nullable Price p) {
        if (p == null) return this;
        return removeAdvertisingBalance(p.getAmount() * p.getEconomy().getConversionScale());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Business business = (Business) o;
        return id.equals(business.id);
    }

    @Override
    public String toString() {
        return "Business{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", owner=" + owner +
                ", icon=" + icon +
                ", creationDate=" + new Date(creationDate) +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, owner);
    }

    /**
     * Checks whether this Business exists.
     * @param uid Business ID
     * @return true if business exists, else false
     */
    public static boolean exists(@Nullable UUID uid) {
        if (uid == null) return false;
        return byId(uid) != null;
    }

    /**
     * Checks whether this Business exists.
     * @param p Owner of Business
     * @return true if business exists, else false
     */
    public static boolean exists(@Nullable OfflinePlayer p) {
        if (p == null) return false;
        return byOwner(p) != null;
    }

    /**
     * Fetches the Business's Statistics.
     * @return Business Statistics
     */
    @NotNull
    public BusinessStatistics getStatistics() {
        return this.stats;
    }

    /**
     * Shortcut method to {@link BusinessStatistics#getProductSales()}.
     * @return Product Sales
     */
    @NotNull
    public Map<Product, Integer> getProductSales() {
        return this.stats.getProductSales();
    }

    /**
     * Shortcut method to {@link BusinessStatistics#getPurchaseCount(Product)}.
     * @param p Product to check
     * @return Purchase Count, or 0 if none
     */
    public int getPurchaseCount(@NotNull Product p) {
        return this.stats.getPurchaseCount(p);
    }

    /**
     * Fetches the Business's Creation Date.
     * @return Business Creation Date, or May 17, 2009 (MC's Birth Date) if not set / created before this was tracked
     */
    @NotNull
    public Date getCreationDate() {
        return new Date(creationDate);
    }

    /**
     * Fetches an immutable version of the Business's Keywords.
     * @return Business Keywords
     */
    @NotNull
    public List<String> getKeywords() {
        return ImmutableList.copyOf(keywords);
    }

    /**
     * Adds keywords to the Business.
     * @param keywords Keywords to add
     */
    public void addKeywords(@Nullable String... keywords) {
        if (keywords == null) return;
        addKeywords(Arrays.asList(keywords));
    }

    /**
     * Adds a collection of keywords to the Business.
     * @param keywords Keywords to add
     * @throws IllegalArgumentException if keywords are too long or the business has exceeded the amount of keywords
     */
    public void addKeywords(@Nullable Iterable<String> keywords) throws IllegalArgumentException {
        if (keywords == null) return;
        List<String> added = new ArrayList<>();
        keywords.forEach(added::add);

        if (this.keywords.size() + added.size() > MAX_KEYWORDS) throw new IllegalArgumentException("Business \"" + name + "\" has exceeded the maximum amount of keywords! Max is " + MAX_KEYWORDS);

        added.forEach(s -> {
            if (s == null) return;
            if (s.length() > MAX_KETWORD_LENGTH) throw new IllegalArgumentException("Keyword \"" + s + "\" is too long! Max length is " + MAX_KETWORD_LENGTH);

            this.keywords.add(s);
        });

        saveBusiness();
    }

    /**
     * Removes keywords from the Business.
     * @param keywords Keywords to remove
     */
    public void removeKeywords(@Nullable String... keywords) {
        if (keywords == null) return;
        removeKeywords(Arrays.asList(keywords));
    }

    /**
     * Removes a collection of keywords from the Business.
     * @param keywords Keywords to remove
     */
    public void removeKeywords(@Nullable Iterable<String> keywords) {
        if (keywords == null) return;
        keywords.forEach(this.keywords::remove);
        saveBusiness();
    }

    /**
     * Whether this Business contains all of the keywords in this array.
     * @param keywords Keywords to check
     * @return true if all keywords are present, else false
     */
    public boolean hasAllKeywords(@Nullable String... keywords) {
        if (keywords == null) return false;
        return hasAllKeywords(Arrays.asList(keywords));
    }

    /**
     * Whether this Business contains all of the keywords in this collection.
     * @param keywords Keywords to check
     * @return true if all keywords are present, else false
     */
    public boolean hasAllKeywords(@Nullable Iterable<String> keywords) {
        if (keywords == null) return false;
        try {
            return new HashSet<>(this.keywords).containsAll(ImmutableList.copyOf(keywords));
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Whether this Business contains any of the keywords in this array.
     * @param keywords Keywords to check
     * @return true if any keywords are present, else false
     */
    public boolean hasAnyKeywords(@Nullable String... keywords) {
        if (keywords == null) return false;
        return hasAnyKeywords(Arrays.asList(keywords));
    }

    /**
     * Whether this Business contains any of the keywords in this collection.
     * @param keywords Keywords to check
     * @return true if any keywords are present, else false
     */
    public boolean hasAnyKeywords(@Nullable Iterable<String> keywords) {
        if (keywords == null) return false;
        Iterator<String> it = keywords.iterator();
        try {
            return this.keywords.stream().anyMatch(s -> it.hasNext() && it.next().equalsIgnoreCase(s));
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Creates an integer representation of the place this Businesses is in, according to its Advertising Balance.
     * @return Place in Advertising Balance
     */
    public int getAdvertisingBalancePlace() {
        int place = 1;
        for (Business b : getBusinesses()) if (b.getAdvertisingBalance() > getAdvertisingBalance()) place++;

        return place;
    }

    /**
     * Utility method for fetching {@link BusinessStatistics#getTotalResources()}.
     * @return Total Resources ever collected by this Business
     */
    public int getTotalResources() {
        return getStatistics().getTotalResources();
    }

    /**
     * Creates a Public Icon for this Business.
     * @return Generated Public Icon
     */
    @NotNull
    public ItemStack getPublicIcon() {
        boolean pOwner = getSetting(Settings.Business.PUBLIC_OWNER);
        boolean pRating = getSetting(Settings.Business.PUBLIC_RATING);

        ItemStack icon = new ItemStack(getIcon().getType());
        ItemMeta hMeta = icon.getItemMeta();
        hMeta.setDisplayName(ChatColor.GOLD + this.name);
        StringBuilder rB = new StringBuilder();
        for (int j = 0; j < Math.round(getAverageRating()); j++) rB.append("⭐");

        hMeta.setLore(Arrays.asList(
                pOwner ? String.format(Language.getCurrentMessage("constants.owner"), owner.getName()) : Language.getCurrentMessage("constants.business.anonymous"),
                ChatColor.AQUA + keywords.toString().replaceAll("[\\[\\]]", ""),
                pRating ? ChatColor.YELLOW + rB.toString() : "")
        );

        hMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        hMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        try {
            Method m = hMeta.getClass().getDeclaredMethod("setCustomModelData", Integer.class);
            m.setAccessible(true);
            m.invoke(hMeta, this.customModelData);
        } catch (NoSuchMethodException ignored) {}
        catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }

        icon.setItemMeta(hMeta);

        return icon;
    }

    /**
     * Fetches the corporation that this Business belongs to.
     * @return Parent Corporation, or null if none
     */
    @Nullable
    public Corporation getParentCorporation() {
        return Corporation.getCorporations()
                .stream()
                .filter(c -> c.getChildren().contains(this) || owner.equals(c.getOwner()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Makes this Business leave its parent Corporation.
     * @throws IllegalStateException if this Business is not in a Corporation
     */
    public void leaveParentCorporation() throws IllegalStateException {
        Corporation parent = getParentCorporation();
        if (parent == null) throw new IllegalStateException("This business is not in a corporation!");

        parent.removeChild(this);
    }

    /**
     * Fetches the date this Business joined its parent Corporation.
     * @return Date joined, or null if not in a Corporation
     */
    @Nullable
    public Date getCorporationJoinDate() {
        if (getParentCorporation() == null) return null;
        return getParentCorporation().getJoinDate(this);
    }

    /**
     * Fetches an immutable set of all of the Corporations this Business has been invited to.
     * @return Set of Corporation Invites
     */
    @NotNull
    public Set<CorporationInvite> getInvites() {
        if (getParentCorporation() != null) return ImmutableSet.of();

        return ImmutableSet.copyOf(Corporation.getCorporations()
                .stream()
                .filter(c -> c.isInvited(this))
                .map(c -> c.getInvite(this))
                .collect(Collectors.toList())
        );
    }

    // Static Util

    /**
     * <p>Fetches a random business based on its advertising balance.</p>
     * <p>Will return null if {@link NovaConfig#isAdvertisingEnabled()} is false.</p>
     * @return Random Business
     */
    @Nullable
    public static Business randomAdvertisingBusiness() {
        if (!NovaConfig.getConfiguration().isAdvertisingEnabled()) return null;

        List<Business> businesses = getBusinesses().stream().filter(b -> b.getAdvertisingBalance() > NovaConfig.getConfiguration().getBusinessAdvertisingReward()).collect(Collectors.toList());
        if (businesses.isEmpty()) return null;

        Map<Business, Double> minRange = new HashMap<>();
        Map<Business, Double> maxRange = new HashMap<>();

        AtomicReference<Double> total = new AtomicReference<>(0D);

        businesses.forEach(b -> {
            if (b.getAdvertisingBalance() <= 0) return;
            double bal = Math.floor(b.getAdvertisingBalance());

            minRange.put(b, total.get());
            maxRange.put(b, total.get() + bal);

            total.updateAndGet(v -> v + bal + 1);
        });

        double rand = Math.floor(r.nextDouble() * total.get());

        Business b = null;

        for (Business business : businesses)
            if (rand >= minRange.get(business) && rand <= maxRange.get(business)) {
                b = business;
                break;
            }

        return b;
    }

    /**
     * Represents a Business Builder
     */
    public static final class Builder {

        String name;
        OfflinePlayer owner;
        Material icon;
        List<String> keywords = new ArrayList<>();
        Map<Settings.Business<?>, Object> settings = new HashMap<>();

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
         * Sets a setting for the Business.
         * @param setting Setting to set
         * @param value Value to set
         * @return this builder, for chaining
         */
        public <T> Builder setSetting(@NotNull Settings.Business<T> setting, T value) {
            if (setting == null) return this;
            this.settings.put(setting, value);
            return this;
        }

        /**
         * Sets the Business's Keywords.
         * @param keywords Keywords to set
         * @return this builder, for chaining
         */
        public Builder setKeywords(@Nullable Collection<String> keywords) {
            if (keywords == null) return this;
            this.keywords = new ArrayList<>(keywords);
            return this;
        }

        /**
         * Sets the Business's Keywords.
         * @param keywords Keywords to set
         * @return this builder, for chaining
         */
        public Builder setKeywords(@Nullable String... keywords) {
            if (keywords == null) return this;
            return setKeywords(Arrays.asList(keywords));
        }

        /**
         * Adds an array of keywords to the Business.
         * @param keywords Keywords to add
         * @return this builder, for chaining
         */
        public Builder addKeywords(@Nullable String... keywords) {
            if (keywords == null) return this;
            return addKeywords(Arrays.asList(keywords));
        }

        /**
         * Adds a collection of Keywords to the Business.
         * @param keywords Keywords to add
         * @return this builder, for chaining
         */
        public Builder addKeywords(@Nullable Collection<String> keywords) {
            if (keywords == null) return this;
            this.keywords.addAll(keywords);
            return this;
        }

        /**
         * Builds a Novaconomy Business.
         * @return Built Novaconomy Business
         * @throws IllegalArgumentException if a part is missing or is null
         * @throws UnsupportedOperationException if Business exists via name or owner
         */
        @NotNull
        public Business build() throws IllegalArgumentException, UnsupportedOperationException {
            if (owner == null) throw new IllegalArgumentException("Owner cannot be null");
            Validate.notNull(name, "Name cannot be null");
            if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");
            Validate.notNull(icon, "Icon cannot be null");

            if (Business.exists(name)) throw new UnsupportedOperationException("Business already exists");
            if (Business.exists(owner)) throw new UnsupportedOperationException("Owner already has a business");

            BUSINESS_CACHE.clear();

            for (Settings.Business<?> setting : Settings.Business.values()) settings.putIfAbsent(setting, setting.getDefaultValue());

            Business b = new Business(UUID.nameUUIDFromBytes(name.getBytes()), name, icon, owner,
                    null, null, null, settings, System.currentTimeMillis(), keywords, 0, new ArrayList<>());

            b.saveBusiness();

            if (Corporation.exists(owner)) {
                Corporation c = Corporation.byOwner(owner);
                c.addChild(b);
            }

            BusinessCreateEvent event = new BusinessCreateEvent(b);
            Bukkit.getPluginManager().callEvent(event);
            return b;
        }

    }

}
