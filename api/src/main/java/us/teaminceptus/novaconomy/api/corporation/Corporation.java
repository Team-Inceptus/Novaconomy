package us.teaminceptus.novaconomy.api.corporation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.market.StockHolder;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationCreateEvent;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Novaconomy Corporation
 */
@SuppressWarnings("unchecked")
public final class Corporation implements StockHolder {

    // Constants

    /**
     * The maximum length of a corporation name
     */
    public static final int MAX_NAME_LENGTH = 32;

    // Class

    private final UUID id;
    private final long creationDate;
    private final OfflinePlayer owner;

    private final File folder;

    private String name;
    private Material icon;
    private Location headquarters = null;

    private double experience = 0.0;
    private int stockLimit;

    private final Set<Business> children = new HashSet<>();
    private final Map<CorporationAchievement, Integer> achievements = new HashMap<>();
    private final List<ItemStack> resources = new ArrayList<>();
    private final Map<UUID, CorporationPermission[]> businessPermissions = new HashMap<>();

    private Corporation(@NotNull UUID id, long creationDate, OfflinePlayer owner) {
        this.id = id;
        this.creationDate = creationDate;
        this.owner = owner;

        this.folder = new File(NovaConfig.getCorporationsFolder(), id.toString());
    }

    /**
     * Fetches the ID of this Corporation.
     * @return Corporation ID
     */
    @NotNull
    public UUID getUniqueId() { return id; }

    /**
     * Fetches the folder that this corporation's data is stored in.
     * @return Corporation Folder
     */
    @NotNull
    public File getFolder() { return folder; }

    /**
     * Fetches the date that this Corporation was created.
     * @return Corporation Creation Date
     */ 
    @NotNull
    public Date getCreationDate() {
        return new Date(creationDate);
    }

    /**
     * Fetches the owner of this Corporation.
     * @return Corporation Owner
     */
    @NotNull
    public OfflinePlayer getOwner() {
        return owner;
    }

    // Info

    /**
     * Fetches an immutable set of the Businesses this Corporation is responsible for.
     * @return Business Children
     */
    @NotNull
    public Set<Business> getChildren() {
        return ImmutableSet.copyOf(children);
    }

    /**
     * Fetches an immutable map of the Business Owners this Corporation is responsible for.
     * @return Business Owners
     */
    @NotNull
    public Set<OfflinePlayer> getMembers() {
        return ImmutableSet.copyOf(
            children.stream()
                    .map(Business::getOwner)
                    .collect(Collectors.toList())
        );
    }

    /**
     * Adds a Business to this Corporation's children.
     * @param b Business to add
     * @throws IllegalArgumentException if the Business already has a parent corporation, or is null
     */
    public void addChild(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null");
        if (b.getParentCorporation() != null) throw new IllegalArgumentException("Business already has a parent corporation");
        if (children.contains(b)) throw new IllegalArgumentException("Business is already a child of this corporation");

        children.add(b);
        saveCorporation();
    }

    /**
     * Removes a Business from this Corporation's children.
     * @param b Business to remove
     * @throws IllegalArgumentException if the Business is not a child of this Corporation, is null, or Business matches owner's business
     */
    public void removeChild(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null");
        if (!b.getParentCorporation().equals(this)) throw new IllegalArgumentException("Business is not a child of this corporation");
        if (b.getOwner().equals(owner)) throw new IllegalArgumentException("Cannot remove a business owned by the corporation owner");

        children.remove(b);
        saveCorporation();
    }

    /**
     * Fetches the name of this Corporation.
     * @return Corporation Name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this Corporation.
     * @param name New Corporation Name
     * @throws IllegalArgumentException if name is too long according to {@link #MAX_NAME_LENGTH} or is null
     */
    public void setName(@NotNull String name) throws IllegalArgumentException {
        if (name == null) throw new IllegalArgumentException("Name cannot be null!");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Corporation name cannot be longer than 32 characters.");

        this.name = name;
        saveCorporation();
    }

    /**
     * Fetches the amount of experience this Corporation has.
     * @return Corporation Experience
     */
    public double getExperience() {
        return experience;
    }

    /**
     * Sets the amount of experience this Corporation has.
     * @param experience New Corporation Experience
     * @throws IllegalArgumentException if experience is negative
     */
    public void setExperience(double experience) throws IllegalArgumentException {
        if (experience < 0) throw new IllegalArgumentException("Corporation experience cannot be negative!");
        this.experience = experience;

        saveCorporation();
    }

    /**
     * Adds experience to this Corporation.
     * @param add Experience to add
     * @throws IllegalArgumentException if result is negative
     */
    public void addExperience(double add) throws IllegalArgumentException {
        setExperience(experience + add);
    }

    /**
     * Removes experience from this Corporation.
     * @param remove Experience to remove
     * @throws IllegalArgumentException if result is negative
     */
    public void removeExperience(double remove) throws IllegalArgumentException {
        setExperience(experience - remove);
    }

    /**
     * Fetches this Corporation's Level.
     * @return Corporation Level
     */
    public int getLevel() {
        return toLevel(experience);
    }

    /**
     * Sets this corporation's level, setting the experience to the minimum required.
     * @param level New Corporation Level
     * @throws IllegalArgumentException if level is not postiive
     */
    public void setLevel(int level) throws IllegalArgumentException {
        if (level < 1) throw new IllegalArgumentException("Corporation level must be postiive!");
        setExperience(toExperience(level));
    }

    /**
     * Fetches this Corporation's Icon.
     * @return Corporation Icon
     */
    @NotNull
    public Material getIcon() {
        return icon;
    }

    /**
     * Sets this Corporation's Icon.
     * @param icon New Corporation Icon
     * @throws IllegalArgumentException if icon is null
     */
    public void setIcon(@NotNull Material icon) throws IllegalArgumentException {
        if (icon == null) throw new IllegalArgumentException("Corporation Icon cannot be null!");
        this.icon = icon;
        saveCorporation();
    }

    /**
     * Fetches this Corporation's Icon as an ItemStack.
     * @return Corporation Icon
     */
    public ItemStack getPublicIcon() {
        ItemStack icon = new ItemStack(this.icon);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        icon.setItemMeta(meta);

        return icon;
    }

    /**
     * Fetches the Location of this Corporation's Headquarters.
     * @return Corporation Headquarters
     */
    @Nullable
    public Location getHeadquarters() {
        return headquarters;
    }

    /**
     * Sets the Location of this Corporation's Headquarters.
     * @param headquarters New Corporation Headquarters
     */
    public void setHeadquarters(@Nullable Location headquarters) {
        this.headquarters = headquarters;
        saveCorporation();
    }

    /**
     * Fetches an immutable version of all of the Corporation's Achievements to their level.
     * @return Corporation Achievements
     */
    @NotNull
    public Map<CorporationAchievement, Integer> getAchievements() {
        return ImmutableMap.copyOf(achievements);
    }

    /**
     * Fetches an Corporation Achievement's level.
     * @param achievement Achievement to fetch
     * @return Achievement Level
     */
    @NotNull
    public int getAchievementLevel(@NotNull CorporationAchievement achievement) {
        if (achievement == null) throw new IllegalArgumentException("Corporation Achievement cannot be null!");
        
        return achievements.getOrDefault(achievement, 0);
    }

    /**
     * Awards an Achievement to this Corporation, increasing its level by 1 and awarding the necessary experience.
     * @param achievement Achievement to award
     * @throws IllegalArgumentException if achievement is null, or achievement is already at max level
     */
    @NotNull
    public void awardAchievement(@NotNull CorporationAchievement achievement) throws IllegalArgumentException {
        if (achievement == null) throw new IllegalArgumentException("Corporation Achievement cannot be null!");
        int newLevel = getAchievementLevel(achievement) + 1;

        if (newLevel > achievement.getMaxLevel()) throw new IllegalArgumentException("Achievement is already at max level! (Max: " + achievement.getMaxLevel() + ")");
        
        achievements.put(achievement, newLevel);
        experience += achievement.getExperienceReward() * newLevel;

        saveCorporation();
    }

    /**
     * Fetches the Corporation's Statistic.
     * @return Corporation Statistics
     */
    @NotNull
    public CorporationStatistics getStatistics() {
        return new CorporationStatistics(this);
    }

    /**
     * Deletes this Corporation.
     */
    public void delete() { removeCorporation(this); }

    /**
     * Fetches an immutable copy of all of the shared resources this Corporation has.
     * @return Corporation Resources
     */
    public List<ItemStack> getResources() {
        return ImmutableList.copyOf(resources);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Corporation that = (Corporation) o;
        return creationDate == that.creationDate && id.equals(that.id) && owner.equals(that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creationDate, owner.getUniqueId());
    }

    @Override
    public String toString() {
        return "Corporation{" +
                "id=" + id +
                ", creationDate=" + creationDate +
                ", owner=" + owner +
                ", name='" + name + '\'' +
                '}';
    }

    // Stock Impl

    @Override
    public double getStockPrice() {
        return Math.max(
            getStatistics().getTotalProfit() / getStockLimit(),
            0.01
        );
    }

    @Override
    public int getStockLimit() {
        return stockLimit;
    }

    @Override
    public void setStockLimit(int limit) {
        this.stockLimit = limit;
        saveCorporation();
    }

    // Static Methods

    private static final Set<Corporation> CORPORATION_CACHE = new HashSet<>();

    /**
     * Fetches an immutable set of all of the corporations that exist.
     * @return All Corporations
     */
    @NotNull
    public static Set<Corporation> getCorporations() {
        if (!CORPORATION_CACHE.isEmpty()) return ImmutableSet.copyOf(CORPORATION_CACHE);
        Set<Corporation> corporations = new HashSet<>();

        for (File folder : NovaConfig.getCorporationsFolder().listFiles()) {
            if (folder == null) continue;
            if (!folder.isDirectory()) continue;

            Corporation c;

            try {
                c = readCorporation(folder.getAbsoluteFile());
            } catch (OptionalDataException e) {
                NovaConfig.print(e);
                continue;
            } catch (IOException | ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }

            corporations.add(c);
        }

        CORPORATION_CACHE.addAll(corporations);
        return ImmutableSet.copyOf(CORPORATION_CACHE);
    }

    /**
     * Fetches a Corporation by its name.
     * @param name Corporation Name
     * @return Corporation found, or null if not found
     */
    @Nullable
    public static Corporation byName(@Nullable String name) {
        if (name == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches a Corporation by its ID.
     * @param id Corporation ID
     * @return Corporation found, or null if not found
     */
    @Nullable
    public static Corporation byId(@Nullable UUID id) {
        if (id == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getUniqueId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches a Corporation by its owner.
     * @param owner Corporation Owner
     * @return Corporation found, or null if not found
     */
    @Nullable
    public static Corporation byOwner(@Nullable OfflinePlayer owner) {
        if (owner == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getOwner().equals(owner))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches a Corporation by a member.
     * @param member Corporation Member
     * @return Corporation found, or null if not found
     */
    @Nullable
    public static Corporation byMember(@Nullable OfflinePlayer member) {
        if (member == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getMembers().contains(member) || c.getOwner().equals(member))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fetches a Corporation by a member.
     * @param child Corporation Member as a Business
     * @return Corporation found, or null if not found
     */
    @Nullable
    public static Corporation byMember(@Nullable Business child) {
        if (child == null) return null;
        return getCorporations().stream()
                .filter(c -> c.getChildren().contains(child))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether a Corporation exists with the given name.
     * @param name Corporation Name
     * @return true if exists, false otherwise
     */
    public static boolean exists(@Nullable String name) {
        if (name == null) return false;
        return getCorporations().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    /**
     * Whether a Corporation exists with the given ID.
     * @param id Corporation ID
     * @return true if exists, false otherwise
     */
    public static boolean exists(@Nullable UUID id) {
        if (id == null) return false;
        return getCorporations().stream()
                .anyMatch(c -> c.getUniqueId().equals(id));
    }

    /**
     * Whether a Corporation exists with the given owner.
     * @param owner Corporation Owner
     * @return true if exists, false otherwise
     */
    public static boolean exists(@Nullable OfflinePlayer owner) {
        if (owner == null) return false;
        return getCorporations().stream()
                .anyMatch(c -> c.getOwner().equals(owner));
    }

    /**
     * Whether the given member is in a Corporation.
     * @param member Corporation Member
     * @return true if member is in a corporation, false otherwise
     */
    public static boolean existsByMember(@Nullable OfflinePlayer member) {
        if (member == null) return false;
        if (exists(member)) return true;
        
        return getCorporations().stream()
                .anyMatch(c -> c.getMembers().contains(member));
    }

    /**
     * Deletes a Corporation.
     * @param c Corporation to delete
     */
    public static void removeCorporation(@NotNull Corporation c) {
        if (c == null) throw new IllegalArgumentException("Corporation cannot be null!");
        CORPORATION_CACHE.clear();

        for (File f : c.folder.listFiles()) {
            if (f == null) continue;
            f.delete();
        }

        c.folder.delete();
    }

    /**
     * Converts the experience of a Corporation to a level.
     * @param level Level to convert to
     * @return Minimum Experience required for the specified level
     * @throws IllegalArgumentException if level is not positive
     */
    public static double toExperience(int level) throws IllegalArgumentException {
        if (level < 1) throw new IllegalArgumentException("Level must be positive!");
        if (level == 1) return 0;

        double level0 = level - 1;
        double num = Math.floor(Math.pow(2, level0 - 1) * 10000 * level0);
        double rem = num % 1000;

        return rem >= 1000 / 2D ? num - rem + 1000 : num - rem;
    }

    /**
     * Converts the experience of a Corporation to a level.
     * @param experience Experience to convert to
     * @return Level conversion at the specified experience
     * @throws IllegalArgumentException if experience is not positive
     */
    public static int toLevel(double experience) throws IllegalArgumentException {
        if (experience < 0) throw new IllegalArgumentException("Experience must be positive!");
        if (experience < 40000) return 1;

        int level = 1;
        while (toExperience(level) < experience) level++;

        return level;
    }

    // Builder

    /**
     * Creates a new Corporation Builder.
     * @return New Corporation Builder
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class used to create Corporations
     */
    public static final class Builder {

        String name;
        OfflinePlayer owner;
        Material icon = Material.STONE;
        Location headquarters;

        private Builder() {}

        /**
         * Sets the name of the Corporation.
         * @param name Corporation Name
         * @return this class, for chaining
         * @throws IllegalArgumentException if name is null or longer than {@link #MAX_NAME_LENGTH}
         */
        public Builder setName(@NotNull String name) throws IllegalArgumentException {
            if (name == null) throw new IllegalArgumentException("Corporation Name cannot be null!");
            if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Corporation Name is too long!");

            this.name = name;
            return this;
        }

        /**
         * Sets the owner of the Corporation.
         * @param owner Corporation Owner
         * @return this class, for chaining
         * @throws IllegalArgumentException if owner is null
         */
        public Builder setOwner(@NotNull OfflinePlayer owner) throws IllegalArgumentException {
            if (owner == null) throw new IllegalArgumentException("Corporation Owner cannot be null!");
            this.owner = owner;
            return this;
        }

        /**
         * Sets the icon of the Corporation.
         * @param icon Corporation Icon
         * @return this class, for chaining
         * @throws IllegalArgumentException if icon is null
         */
        public Builder setIcon(@NotNull Material icon) throws IllegalArgumentException {
            if (icon == null) throw new IllegalArgumentException("Corporation Icon cannot be null!");
            this.icon = icon;
            return this;
        }

        /**
         * Sets the headquarters of the Corporation.
         * @param headquarters Corporation Headquarters
         * @return this class, for chaining
         */
        public Builder setHeadquarters(@Nullable Location headquarters) {
            this.headquarters = headquarters;
            return this;
        }

        /**
         * Builds the Corporation.
         * @return New Corporation
         * @throws IllegalStateException if one or more arguments is null
         * @throws UnsupportedOperationException if corporation with name already exists
         */
        @NotNull
        public Corporation build() throws IllegalStateException, UnsupportedOperationException {
            if (name == null) throw new IllegalStateException("Corporation Name cannot be null!");
            if (owner == null) throw new IllegalStateException("Corporation Owner cannot be null!");
            if (icon == null) throw new IllegalStateException("Corporation Icon cannot be null!");
            if (Corporation.exists(name)) throw new UnsupportedOperationException("Corporation with name already exists!");

            CORPORATION_CACHE.clear();
            UUID uid = UUID.randomUUID();

            Corporation c = new Corporation(uid, System.currentTimeMillis(), owner);
            c.name = name;
            c.icon = icon;
            c.headquarters = headquarters;

            if (Business.exists(owner)) c.children.add(Business.getByOwner(owner));

            c.saveCorporation();

            CorporationCreateEvent event = new CorporationCreateEvent(c);
            Bukkit.getPluginManager().callEvent(event);
            return c;
        }
    }

    // Reading & Writing

    /**
     * <p>Saves this Corporation to its Corporation file.</p>
     * <p>This method is called automatically.</p>
     */
    public void saveCorporation() {
        if (!folder.exists()) folder.mkdir();
        CORPORATION_CACHE.clear();

        try {
            writeCorporation();
        } catch (IOException e) {
            NovaConfig.print(e);
        }
    }

    private void writeCorporation() throws IOException {
        File info = new File(folder, "info.dat");
        if (!info.exists()) info.createNewFile();

        ObjectOutputStream infoOs = new ObjectOutputStream(Files.newOutputStream(info.toPath()));
        infoOs.writeObject(this.id);
        infoOs.writeLong(this.creationDate);
        infoOs.writeObject(this.owner.getUniqueId());
        infoOs.close();

        File dataF = new File(folder, "data.yml");
        if (!dataF.exists()) dataF.createNewFile();

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataF);
        data.set("name", this.name);
        data.set("experience", this.experience);
        data.set("icon", this.icon.name());
        data.set("headquarters", this.headquarters);
        data.set("achievements", this.achievements
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().name().toLowerCase(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        data.set("share_limit", this.stockLimit);
        data.save(dataF);

        File childrenF = new File(folder, "children.yml");
        if (!childrenF.exists()) childrenF.createNewFile();

        FileConfiguration children = YamlConfiguration.loadConfiguration(childrenF);
        children.set("children", this.children
                .stream()
                .map(Business::getUniqueId)
                .map(UUID::toString)
                .collect(Collectors.toList())
        );

        if (!children.isConfigurationSection("permissions")) children.createSection("permissions");
        for (Map.Entry<UUID, CorporationPermission[]> entry : this.businessPermissions.entrySet())
            children.set("permissions." + entry.getKey().toString(), Arrays.stream(entry.getValue())
                    .map(CorporationPermission::getId)
                    .collect(Collectors.toList()));

        children.save(childrenF);

        // Global Resources

        File sourcesF = new File(folder, "resources");
        if (!sourcesF.exists()) sourcesF.mkdir();

        Set<Material> mats = this.resources.stream().map(ItemStack::getType).collect(Collectors.toSet());
        for (Material mat : mats) {
            List<ItemStack> added = this.resources.stream().filter(i -> i.getType() == mat).collect(Collectors.toList());

            File matF = new File(sourcesF, mat.name().toLowerCase() + ".dat");
            if (!matF.exists()) matF.createNewFile();

            FileOutputStream matFs = new FileOutputStream(matF);
            ObjectOutputStream matOs = new ObjectOutputStream(new BufferedOutputStream(matFs));

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
    }

    @NotNull
    private static Corporation readCorporation(File folder) throws IOException, IllegalStateException, ReflectiveOperationException {
        File info = new File(folder, "info.dat");
        if (!info.exists()) throw new IllegalStateException("Could not find: info.dat");

        ObjectInputStream infoIs = new ObjectInputStream(Files.newInputStream(info.toPath()));
        UUID id = (UUID) infoIs.readObject();
        long creationDate = infoIs.readLong();
        OfflinePlayer owner = Bukkit.getOfflinePlayer((UUID) infoIs.readObject());
        infoIs.close();

        Corporation c = new Corporation(id, creationDate, owner);

        File dataF = new File(folder, "data.yml");
        if (!dataF.exists()) throw new IllegalStateException("Could not find: data.yml");

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataF);
        c.name = data.getString("name");
        c.experience = data.getDouble("experience");
        c.icon = Material.valueOf(data.getString("icon"));
        c.headquarters = (Location) data.get("headquarters");
        c.stockLimit = data.getInt("share_limit");

        c.achievements.putAll(data.getConfigurationSection("achievements").getValues(false)
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(CorporationAchievement.valueOf(e.getKey().toUpperCase()), (Integer) e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        File childrenF = new File(folder, "children.yml");
        if (!childrenF.exists()) throw new IllegalStateException("Could not find: children.yml");

        FileConfiguration children = YamlConfiguration.loadConfiguration(childrenF);
        c.children.addAll(children.getStringList("children")
                .stream()
                .map(UUID::fromString)
                .map(Business::getById)
                .collect(Collectors.toList()));

        for (Map.Entry<String, Object> entry : children.getConfigurationSection("permissions").getValues(false).entrySet()) {
            UUID bid = UUID.fromString(entry.getKey());
            CorporationPermission[] bPermissions = ((List<String>) entry.getValue())
                    .stream()
                    .map(CorporationPermission::byId)
                    .toArray(CorporationPermission[]::new);

            c.businessPermissions.put(bid, bPermissions);
        }

        // Global Resources
        File sourcesF = new File(folder, "resources");
        if (!sourcesF.exists()) sourcesF.mkdir();

        List<ItemStack> resources = new ArrayList<>();
        for (File matF : sourcesF.listFiles()) {
            FileInputStream matFs = new FileInputStream(matF);
            ObjectInputStream matOs = new ObjectInputStream(new BufferedInputStream(matFs));

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
                Map<String, Object> sItem = new HashMap<>(m);

                ItemMeta base = Bukkit.getItemFactory().getItemMeta(Material.valueOf((String) m.get("type")));
                DelegateDeserialization deserialization = base.getClass().getAnnotation(DelegateDeserialization.class);
                Method deserialize = deserialization.value().getDeclaredMethod("deserialize", Map.class);
                deserialize.setAccessible(true);

                Map<String, Object> sMeta = (Map<String, Object>) sItem.getOrDefault("meta", base.serialize());
                if (sMeta == null) sMeta = base.serialize();

                ItemMeta meta = (ItemMeta) deserialize.invoke(null, sMeta);
                sItem.put("meta", meta);
                ItemStack i = ItemStack.deserialize(m);
                i.setItemMeta(meta);

                resources.add(i);
            }

        }
        c.resources.addAll(resources);

        return c;
    }
}
