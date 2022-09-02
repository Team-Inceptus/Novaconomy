package us.teaminceptus.novaconomy.api.business;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.events.business.BusinessCreateEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Represents a Novaconomy Business
 */
public final class Business implements ConfigurationSerializable, Serializable {

    /**
     * Represents the Business File Suffix
     */
    public static final String BUSINESS_FILE_SUFFIX = ".nbusiness";

    private final UUID id;

    private String name;

    private final OfflinePlayer owner;

    private final File file;

    private Material icon;

    private final long creationDate;

    private Location home = null;

    final BusinessStatistics stats;

    private final List<Product> products = new ArrayList<>();

    private final Map<Settings.Business, Boolean> settings = new HashMap<>();

    private final List<ItemStack> resources = new ArrayList<>();

    private final List<String> keywords = new ArrayList<>();

    private Business(UUID uid, String name, Material icon, OfflinePlayer owner, Collection<Product> products, Collection<ItemStack> resources,
                     BusinessStatistics stats, Map<Settings.Business, Boolean> settings, long creationDate, List<String> keywords,
                     boolean save) {
        this.id = uid;
        this.name = name;
        this.icon = icon;
        this.owner = owner;

        this.file = new File(NovaConfig.getBusinessesFolder(), id + BUSINESS_FILE_SUFFIX);

        this.creationDate = creationDate;
        this.stats = stats == null ? new BusinessStatistics(id) : stats;

        if (products != null) products.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getPrice().getEconomy() != null)
                .forEach(p -> { try { this.products.add(new BusinessProduct(p, this)); } catch (IllegalArgumentException ignored) {/* Economy was removed */} });
        if (resources != null) this.resources.addAll(resources);

        this.settings.putAll(settings);
        this.keywords.addAll(keywords);

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
        if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");

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
        return this.owner.getUniqueId().equals(p.getUniqueId());
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
        resources.forEach(i -> map.put(index.getAndIncrement() + "", i));

        List<Product> p = new ArrayList<>();
        products.forEach(pr -> p.add(new Product(pr.getItem(), pr.getPrice())));

        Map<String, Boolean> settings = this.settings
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().name().toLowerCase(), Map.Entry::getValue));

        return new HashMap<String, Object>() {{
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

        AtomicInteger amount = new AtomicInteger();
        res.values().stream().map(ItemStack::getAmount).forEach(amount::addAndGet);
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
     * Removes a Collection of Resources from this Business.
     * @param resources Resources to Remove
     * @return this Business, for chaining
     */
    @NotNull
    public Business removeResource(@Nullable Collection<? extends ItemStack> resources) {
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
    public List<ItemStack> getLeftoverStock() {
        return getResources().stream().filter(i -> !isProduct(i)).collect(Collectors.toList());
    }

    /**
     * Fetches a Setting Value for this Business.
     * @param setting Setting to check
     * @return Setting Value, or false if setting is null
     */
    public boolean getSetting(@Nullable Settings.Business setting) {
        if (setting == null) return false;
        return this.settings.getOrDefault(setting, setting.getDefaultValue());
    }

    /**
     * Fetches a list of all ratings for this Business.
     * @return List of Ratings
     */
    @NotNull
    public List<Rating> getRatings() {
        List<Rating> ratings = new ArrayList<>();

        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (owner.getUniqueId().equals(p.getUniqueId())) continue;
            NovaPlayer np = new NovaPlayer(p);
            if (!np.hasRating(this)) continue;

            ratings.add(np.getRating(this));
        }

        ratings = ratings.stream().filter(Objects::nonNull).collect(Collectors.toList());

        return ratings;
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
    public Business setSetting(@NotNull Settings.Business setting, boolean value) {
        this.settings.put(setting, value);
        saveBusiness();
        return this;
    }

    /**
     * Deserializes a Map into a Business.
     * @param serial Serialization from {@link #serialize()}
     * @deprecated Bukkit Serialization is no longer used; this method exists only for the purposes of converting legacy businesses
     * @return Deserialized Business
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    @Nullable
    public static Business deserialize(@Nullable Map<String, Object> serial) {
        if (serial == null) return null;

        Map<String, ItemStack> res = (Map<String, ItemStack>) serial.get("resources");
        List<ItemStack> resources = new ArrayList<>(res.values());
        Location home = serial.containsKey("home") ? (Location) serial.get("home") : null;

        Map<Settings.Business, Boolean> settings = serial.containsKey("settings") ? ((Map<String, Boolean>) serial.get("settings"))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> Settings.Business.valueOf(e.getKey().toUpperCase()), Map.Entry::getValue)) : null;

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
                settings, creationDate, keywords,false
        );
        b.setHome(home, false);

        return b;
    }

    /**
     * Saves this Business to the Businesses File.
     */
    public void saveBusiness() {
        try {
            FileOutputStream fs = new FileOutputStream(this.file.getAbsoluteFile());
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(fs));

            writeBusiness(os);
            os.close();
        } catch (IOException e) {
            NovaConfig.print(e);
        }
    }

    private void writeBusiness(ObjectOutputStream os) throws IOException {
        os.writeObject(this.id);
        os.writeObject(this.name);
        os.writeObject(this.icon.name());
        os.writeObject(this.owner.getUniqueId());
        os.writeObject(this.settings);
        os.writeLong(this.creationDate);

        Map<String, Object> home = this.home == null ? new HashMap<>(0) : this.home.serialize();
        os.writeObject(home);

        List<Map<String, Object>> res = new ArrayList<>();
        for (ItemStack i : resources) {
            Map<String, Object> m = new HashMap<>(i.serialize());
            m.put("meta", i.getItemMeta().serialize());
            res.add(m);
        }
        os.writeObject(res);

        List<Map<String, Object>> prods = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> m = new HashMap<>(p.serialize());

            Map<String, Object> item = new HashMap<>(p.getItem().serialize());
            item.put("meta", p.getItem().getItemMeta().serialize());
            m.put("item", item);
            prods.add(m);
        }
        os.writeObject(prods);

        os.writeObject(this.keywords);

        stats.writeStats(os);
    }

    @SuppressWarnings("unchecked")
    private static Business readBusiness(ObjectInputStream os) throws IOException, ReflectiveOperationException {
        UUID id = (UUID) os.readObject();
        String name = (String) os.readObject();
        Material icon = Material.valueOf((String) os.readObject());
        OfflinePlayer owner = Bukkit.getOfflinePlayer((UUID) os.readObject());
        Map<Settings.Business, Boolean> settings = (Map<Settings.Business, Boolean>) os.readObject();
        long creationDate = os.readLong();

        Map<String, Object> homeMap = (Map<String, Object>) os.readObject();
        Location home = homeMap.isEmpty() ? null : Location.deserialize(homeMap);

        List<Map<String, Object>> res = (List<Map<String, Object>>) os.readObject();
        List<ItemStack> resources = new ArrayList<>();
        for (Map<String, Object> m : res) {
            Map<String, Object> sItem = new HashMap<>(m);

            ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) m.get("type")));
            DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
            Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
            deserialize.setAccessible(true);

            sItem.put("meta", deserialize.invoke(null, m.get("meta")));
            ItemStack i = ItemStack.deserialize(m);
            resources.add(i);
        }

        List<Map<String, Object>> prods = (List<Map<String, Object>>) os.readObject();
        List<Product> products = new ArrayList<>();

        for (Map<String, Object> m : prods) {
            Map<String, Object> sItem = new HashMap<>(m);
            Map<String, Object> item = new HashMap<>((Map<String, Object>) m.get("item"));

            ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) item.get("type")));
            DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
            Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
            deserialize.setAccessible(true);
            
            item.put("meta", deserialize.invoke(null, item.get("meta")));
            sItem.put("item", ItemStack.deserialize(item));
            products.add(Product.deserialize(sItem));
        }

        List<String> keywords = (List<String>) os.readObject();

        BusinessStatistics stats = BusinessStatistics.readStats(os);
        os.close();

        Business b = new Business(id, name, icon, owner, products, resources, stats, settings, creationDate, keywords, false);
        b.setHome(home, false);
        return b;
    }

    /**
     * Fetches all registered Businesses.
     * @return All Registered Businesses
     * @throws IllegalStateException if a business file is invalid
     */
    @NotNull
    public static List<Business> getBusinesses() throws IllegalStateException {
        List<Business> businesses = new ArrayList<>();
        for (File f : NovaConfig.getBusinessesFolder().listFiles()) {
            if (f.isDirectory()) continue;
            if (!f.getName().endsWith(BUSINESS_FILE_SUFFIX)) continue;

            Business b;

            try {
                FileInputStream fs = new FileInputStream(f.getAbsolutePath());
                ObjectInputStream os = new ObjectInputStream(new BufferedInputStream(fs));

                b = readBusiness(os);
                os.close();
            } catch (OptionalDataException e) {
                NovaConfig.print(e);
                continue;
            } catch (IOException | ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }

            businesses.add(b);
        }

        return businesses;
    }

    /**
     * Fetches a Business by its unique ID.
     * @param uid Business ID
     * @return Found business, or null if not found / ID is null
     * @throws IllegalStateException if the business file is malformed
     */
    @Nullable
    public static Business getById(@Nullable UUID uid) throws IllegalStateException {
        if (uid == null) return null;

        File f = new File(NovaConfig.getBusinessesFolder(), uid + BUSINESS_FILE_SUFFIX);
        if (!f.exists()) return null;

        Business b = null;

        try {
            FileInputStream fs = new FileInputStream(f.getAbsolutePath());
            ObjectInputStream os = new ObjectInputStream(new BufferedInputStream(fs));

            b = readBusiness(os);
        } catch (IOException | ReflectiveOperationException e) {
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
        return getBusinesses().stream().filter(b -> b.isOwner(p)).findFirst().orElse(null);
    }

    /**
     * Deletes a Business.
     * @param b Business to remove
     */
    public static void remove(@Nullable Business b) {
        if (b == null) return;
        b.file.delete();
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
        return Objects.hash(id, owner);
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
     * Checks whether this Business exists.
     * @param p Owner of Business
     * @return true if business exists, else false
     */
    public static boolean exists(@Nullable OfflinePlayer p) {
        if (p == null) return false;
        return getByOwner(p) != null;
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
     * Fetches the Business's Creation Date.
     * @return Business Creation Date, or May 17, 2009 (MC's Birth Date) if not set
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
     */
    public void addKeywords(@Nullable Collection<String> keywords) {
        if (keywords == null) return;
        this.keywords.addAll(keywords);
        saveBusiness();
    }

    /**
     * Removes keywords from the Business.
     * @param keywords Keywords to remove
     */
    public void removeKeywords(@Nullable String... keywords) {
        if (keywords == null) return;
        removeKeywords(keywords);
    }

    /**
     * Removes a collection of keywords from the Business.
     * @param keywords Keywords to remove
     */
    public void removeKeywords(@Nullable Collection<String> keywords) {
        if (keywords == null) return;
        this.keywords.removeAll(keywords);
        saveBusiness();
    }

    /**
     * Represents a Business Builder
     */
    public static final class Builder {

        String name;
        OfflinePlayer owner;
        Material icon;
        List<String> keywords = new ArrayList<>();
        Map<Settings.Business, Boolean> settings = new HashMap<>();

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
        public Builder setSetting(@NotNull Settings.Business setting, boolean value) {
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
         * @throws UnsupportedOperationException if Business exists
         */
        @NotNull
        public Business build() throws IllegalArgumentException, UnsupportedOperationException {
            if (owner == null) throw new IllegalArgumentException("Owner cannot be null");
            Validate.notNull(name, "Name cannot be null");
            if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");
            Validate.notNull(icon, "Icon cannot be null");

            for (Settings.Business setting : Settings.Business.values()) settings.putIfAbsent(setting, setting.getDefaultValue());

            Business b = new Business(UUID.nameUUIDFromBytes(name.getBytes()), name, icon, owner,
                    null, null, null, settings, System.currentTimeMillis(), keywords,false);

            if (Business.exists(name)) throw new UnsupportedOperationException("Business already exists");

            b.saveBusiness();

            BusinessCreateEvent event = new BusinessCreateEvent(b);
            Bukkit.getPluginManager().callEvent(event);
            return b;
        }

    }

}
