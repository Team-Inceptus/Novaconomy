package us.teaminceptus.novaconomy.api.corporation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationAwardAchievementEvent;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationCreateEvent;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationDeleteEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static us.teaminceptus.novaconomy.api.corporation.CorporationAchievement.MONOPOLY;

/**
 * Represents a Novaconomy Corporation
 */
@SuppressWarnings("unchecked")
public final class Corporation {

    // Constants

    /**
     * The maximum length of a corporation name
     */
    public static final int MAX_NAME_LENGTH = 32;

    /**
     * The maximum length of a corporation description
     */
    public static final int MAX_DESCRIPTION_LENGTH = 256;

    /**
     * The maximum level a of a corporation
     */
    public static final int MAX_LEVEL = 100;

    /**
     * The maximum amount of pending invites this Corporation can have
     */
    public static final int MAX_INVITES = 10;

    // Class

    private final UUID id;
    private final long creationDate;
    private final OfflinePlayer owner;

    private final File folder;

    private String name;
    private String description = "";
    private Material icon;
    private Location headquarters = null;

    private double experience = 0.0;
    long views = 0;
    private int customModelData;

    private final Set<Business> children = new HashSet<>();
    private final Map<UUID, Long> childrenJoinDates = new HashMap<>();
    private final Map<CorporationAchievement, Integer> achievements = new HashMap<>();
    private final Map<Settings.Corporation<?>, Object> settings = new HashMap<>();

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

    /**
     * Whether the player is the owner of this Corporation.
     * @param owner Player to check
     * @return true if player owns corporation, false otherwise
     */
    public boolean isOwner(@Nullable OfflinePlayer owner) {
        return owner != null && owner.equals(this.owner);
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
     * Fetches the maximum amount of Children this Corporation can hold.
     * @return Maximum Children
     */
    public int getMaxChildren() {
        return ((getLevel() - 1) * 5) + 10;
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
     * @throws IllegalStateException if the Corporation already has the maximum amount of children
     */
    public void addChild(@NotNull Business b) throws IllegalArgumentException, IllegalStateException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null");
        if (b.getParentCorporation() != null) throw new IllegalArgumentException("Business already has a parent corporation");
        if (children.contains(b)) throw new IllegalArgumentException("Business is already a child of this corporation");

        int newSize = children.size() + 1;
        if (newSize > getMaxChildren()) throw new IllegalStateException("Cannot add a business to a corporation with too many children");

        if ((newSize >= 5 && !(getAchievementLevel(MONOPOLY) >= 1))
            || (newSize >= 15 && !(getAchievementLevel(MONOPOLY) >= 2))
            || (newSize >= 35 && !(getAchievementLevel(MONOPOLY) >= 3))
            || (newSize >= 50 && !(getAchievementLevel(MONOPOLY) >= 4))
        ) awardAchievement(MONOPOLY);

        children.add(b);
        childrenJoinDates.put(b.getUniqueId(), System.currentTimeMillis());

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
     * Fetches the Date a Business joined this Corporation.
     * @param b Business to fetch join date for
     * @return Business Join Date
     * @throws IllegalArgumentException if the Business is not a child of this Corporation, or is null
     */
    @NotNull
    public Date getJoinDate(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null");
        if (!children.contains(b)) throw new IllegalArgumentException("Business is not a child of this corporation");

        return new Date(childrenJoinDates.get(b.getUniqueId()));
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
     * Fetches the description of this Corporation.
     * @return Corporation Description
     */
    @NotNull
    public String getDescription() {
        if (description == null) description = "";
        return description;
    }

    /**
     * Sets the description of this Corporation.
     * @param description New Corporation Description
     * @throws IllegalArgumentException if description is too long according to {@link #MAX_DESCRIPTION_LENGTH} or is null
     */
    @NotNull
    public void setDescription(@NotNull String description) throws IllegalArgumentException {
        if (description == null) throw new IllegalArgumentException("Description cannot be null!");
        if (description.length() > MAX_DESCRIPTION_LENGTH) throw new IllegalArgumentException("Corporation description cannot be longer than 256 characters.");
        
        this.description = description;
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
        if (toLevel(experience) > MAX_LEVEL) throw new IllegalArgumentException("Corporation level cannot be higher than " + MAX_LEVEL + "!");

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
        if (level > MAX_LEVEL) throw new IllegalArgumentException("Corporation level cannot be greater than " + MAX_LEVEL + "!");
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

        try {
            Method m = meta.getClass().getDeclaredMethod("setCustomModelData", Integer.class);
            m.setAccessible(true);
            m.invoke(meta, this.customModelData);
        } catch (NoSuchMethodException ignored) {}
        catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }

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
     * @throws IllegalStateException if Corporation is less than 5
     */
    public void setHeadquarters(@Nullable Location headquarters) throws IllegalStateException {
        if (getLevel() < 3) throw new IllegalStateException("Corporation must be level 5 to set headquarters!");

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

        CorporationAwardAchievementEvent event = new CorporationAwardAchievementEvent(this, achievement, getAchievementLevel(achievement), newLevel);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;

        achievements.put(achievement, event.getNewLevel());
        experience += achievement.getExperienceReward() * event.getNewLevel();

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
     * Fetches how many views this Corporation has received.
     * @return Corporation Views
     */
    public long getViews() {
        return views;
    }

    /**
     * Adds a specific amount of views to this Corporation.
     * @param count Views to add
     */
    public void addView(long count) {
        views += count;
        saveCorporation();
    }

    /**
     * Adds a single view to this Corporation.
     */
    public void addView() {
        addView(1);
    }

    /**
     * Fetches the profit modifier when purchasing products from a Corporation's Child.
     * @return Profit Modifier
     */
    public double getProfitModifier() {
        if (getLevel() < 5) return 1.0D;

        return Math.min(( Math.floor( (double) getLevel() / 5) * 0.1D) + 1, 2.0D);
    }

    /**
     * Fetches the value of a Corporation's setting.
     * @param <T> Setting Type
     * @param setting Setting to fetch
     * @return Setting Value, or null if not found
     */
    @Nullable
    public <T> T getSetting(@NotNull Settings.Corporation<T> setting) {
        if (setting.getType().isPrimitive()) return getSetting(setting, setting.getDefaultValue());

        return getSetting(setting, null);
    }

    /**
     * Fetches the value of a Corporation's setting.
     * @param <T> Setting Type
     * @param setting Setting to fetch
     * @param def Default value to return if setting is not found
     * @return Setting Value, or default value if not found
     */
    @Nullable
    public <T> T getSetting(@NotNull Settings.Corporation<T> setting, @Nullable T def) {
        if (setting == null) return null;
        return (T) settings.getOrDefault(setting, def);
    }

    /**
     * Sets the value of a Corporation's setting.
     * @param <T> Setting Type
     * @param setting Setting to set
     * @param value New value of setting
     * @throws IllegalArgumentException if setting or value is null, or value is not a valid value for this setting
     */
    public <T> void setSetting(@NotNull Settings.Corporation<T> setting, @NotNull T value) throws IllegalArgumentException {
        if (setting == null) throw new IllegalArgumentException("Setting cannot be null!");
        if (value == null) throw new IllegalArgumentException("Value cannot be null!");
        if (!setting.getPossibleValues().contains(value)) throw new IllegalArgumentException("Value is not a valid value for this setting!");

        settings.put(setting, value);
        saveCorporation();
    }

    private final Map<UUID, Long> invited = new HashMap<>();

    /**
     * Fetches an immutable set of all of the businesses invited to this Corporation.
     * @return Businesses invited to this Corporation
     * @throws IllegalStateException if Corporation is not invite only
     */
    @NotNull
    public Set<Business> getInvited() throws IllegalStateException {
        if (getSetting(Settings.Corporation.JOIN_TYPE) != JoinType.INVITE_ONLY) throw new IllegalStateException("Corporation is not invite only!");

        return ImmutableSet.copyOf(invited.keySet().stream()
                .map(Business::byId)
                .collect(Collectors.toSet()));
    }

    /**
     * Whether this Business has been invited to this Corporation.
     * @param b Business to check
     * @return true if Business has been invited, false otherwise
     * @throws IllegalArgumentException if business is null
     */
    public boolean isInvited(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null!");
        if (getSetting(Settings.Corporation.JOIN_TYPE) != JoinType.INVITE_ONLY) return false;

        return invited.keySet().stream()
                .anyMatch(id -> id.equals(b.getUniqueId()));
    }

    /**
     * Removes an invite to this Corporation.
     * @param b Business to remove invite from
     * @throws IllegalArgumentException if business is null, or business is not invited to this Corporation
     */
    public void removeInvite(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null!");
        if (!isInvited(b)) throw new IllegalArgumentException("Business is not invited to this Corporation!");

        invited.remove(b.getUniqueId());
        saveCorporation();
    }

    /**
     * Fetches this Businesses' CorporationInvite.
     * @param b Business to fetch invite for
     * @return CorporationInvite, or null if Business is not invited
     * @throws IllegalArgumentException if business is null
     */
    @Nullable
    public CorporationInvite getInvite(@NotNull Business b) throws IllegalArgumentException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null!");
        if (!isInvited(b)) return null;

        return new CorporationInvite(this, b, new Date(invited.get(b.getUniqueId())));
    }

    /**
     * Invites a Business to this Corporation.
     * @param b Business to invite
     * @return CorporationInvite Object
     * @throws IllegalArgumentException if business is null, business is already apart of this Corporation, or has already been invited
     * @throws IllegalStateException if Corporation is not invite only or invite count is above {@link #MAX_INVITES}
     */
    @NotNull
    public CorporationInvite inviteBusiness(@NotNull Business b) throws IllegalArgumentException, IllegalStateException {
        if (b == null) throw new IllegalArgumentException("Business cannot be null!");
        if (children.contains(b)) throw new IllegalArgumentException("Business is already apart of this Corporation!");
        if (isInvited(b)) throw new IllegalArgumentException("Business is already invited to this Corporation");
        if (getSetting(Settings.Corporation.JOIN_TYPE) != JoinType.INVITE_ONLY) throw new IllegalStateException("Corporation is not invite only!");
        if (invited.size() >= MAX_INVITES) throw new IllegalStateException("Corporation has reached maximum invite count of \"" + MAX_INVITES + "\"!");

        Date d = new Date();
        invited.put(b.getUniqueId(), d.getTime());
        saveCorporation();

        if (b.getOwner().isOnline()) {
            Player owner = b.getOwner().getPlayer();
            owner.sendMessage(ChatColor.YELLOW + String.format(Language.getCurrentLocale(), Language.getCurrentMessage("constants.corporation.invite"), ChatColor.AQUA + getName()));
        }

        return new CorporationInvite(this, b, d);
    }

    /**
     * Broadcasts a Message to all memebrs of the Corporation.
     * @param message Message to broadcast
     * @throws IllegalArgumentException if message is null
     */
    public void broadcastMessage(@NotNull String message) throws IllegalArgumentException {
        if (message == null) throw new IllegalArgumentException("Message cannot be null!");

        String[] sent = new String[] {
                ChatColor.GOLD + "-------------- " + ChatColor.DARK_BLUE + name + " --------------",
                ChatColor.translateAlternateColorCodes('&', message),
                ChatColor.GOLD + "----------------------------"
        };

        Stream.concat(Stream.of(owner), getMembers().stream())
                .filter(OfflinePlayer::isOnline)
                .forEach(p -> p.getPlayer().sendMessage(sent));
    }

    /**
     * Fetches all of the Ratings in all of the Businesses in this Corporation.
     * @return Ratings in this Corporation
     */
    @NotNull
    public List<Rating> getAllRatings() {
        return ImmutableList.copyOf(children.stream().flatMap(b -> b.getRatings().stream()).collect(Collectors.toList()));
    }

    /**
     * Fetches the average for the average rating in all of the Businesses in this Corporation.
     * @return Average Rating of this Corporation
     */
    public double getAverageRating() {
        List<Business> rated = children.stream()
                .filter(b -> !b.getRatings().isEmpty())
                .collect(Collectors.toList());

        return rated.stream().mapToDouble(Business::getAverageRating).sum() / rated.size();
    }

    /**
     * Fetches the total revenue of all of the Businesses in this Corporation.
     * @return Total Revenue of this Corporation
     */
    public double getTotalRevenue() {
        return children.stream().mapToDouble(Business::getTotalRevenue).sum();
    }

    /**
     * Fetches the total resource amount of all of the Businesses in this Corporation.
     * @return Total Resource Amount of this Corporation
     */
    public int getTotalResources() {
        return children.stream().mapToInt(Business::getTotalResources).sum();
    }

    /**
     * Utliity method for {@link #getMembers() getMembers().size()}.
     * @return Member Count
     */
    public int getMemberCount() {
        return children.size();
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
                ", creationDate=" + new Date(creationDate) +
                ", owner=" + owner +
                ", name='" + name + '\'' +
                '}';
    }

    // Static Methods

    private static final Set<Corporation> CORPORATION_CACHE = new HashSet<>();

    /**
     * Reloads the Corporation Cache.
     */
    public static void reloadCorporations() {
        CORPORATION_CACHE.clear();
        getCorporations();
    }

    /**
     * Fetches an immutable set of all of the corporations that exist.
     * @return All Corporations
     */
    @NotNull
    public static Set<Corporation> getCorporations() {
        if (!CORPORATION_CACHE.isEmpty()) return ImmutableSet.copyOf(CORPORATION_CACHE);
        Set<Corporation> corporations = new HashSet<>();

        if (NovaConfig.getConfiguration().isDatabaseEnabled())
            try {
                checkTable();
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                PreparedStatement ps = db.prepareStatement("SELECT *, COUNT(*) FROM corporations GROUP BY id");
                ResultSet rs = ps.executeQuery();

                while (rs.next())
                    corporations.add(readDB(rs));

                rs.close();
                ps.close();
            } catch (Exception e) {
                NovaConfig.print(e);
            }
        else {
            List<File> files = NovaConfig.getCorporationsFolder().listFiles() == null ? new ArrayList<>() : Arrays.asList(NovaConfig.getCorporationsFolder().listFiles());
            for (File folder : files) {
                if (folder == null) continue;
                if (!folder.isDirectory()) continue;

                Corporation c;

                try {
                    c = readFile(folder.getAbsoluteFile());
                } catch (OptionalDataException e) {
                    NovaConfig.print(e);
                    continue;
                } catch (IOException | ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }

                corporations.add(c);
            }
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

        Corporation c = null;

        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();
                PreparedStatement ps = db.prepareStatement("SELECT * FROM corporations WHERE id = ?");
                ps.setString(1, id.toString());

                ResultSet rs = ps.executeQuery();
                if (rs.next()) c = readDB(rs);

                rs.close();
                ps.close();
            } else {
                File f = new File(NovaConfig.getCorporationsFolder(), id.toString());
                if (!f.exists()) return null;
                c = readFile(f);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return c;
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
                .filter(c -> c.isOwner(owner))
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
                .filter(c -> c.getMembers().contains(member) || c.isOwner(member))
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
                .anyMatch(c -> c.isOwner(owner));
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
     * Whether any Corporations exist.
     * @return true if any Corporations exist, false otherwise
     */
    public static boolean exists() {
        return !getCorporations().isEmpty();
    }

    /**
     * Deletes a Corporation.
     * @param c Corporation to delete
     */
    public static void removeCorporation(@NotNull Corporation c) {
        if (c == null) throw new IllegalArgumentException("Corporation cannot be null!");

        CorporationDeleteEvent event = new CorporationDeleteEvent(c);
        Bukkit.getPluginManager().callEvent(event);

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
        double num = Math.floor(Math.pow(1.5, level0 - 1) * 5000 * level0);
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

    // Static Classes

    /**
     * Represents the joining type of a Corporation.
     */
    public enum JoinType {

        /**
         * Represents a Corporation that is public and joinable by anyone.
         */
        PUBLIC, 

        /**
         * Represents a Corporation that can only be joined by invitation.
         */
        INVITE_ONLY,

        /**
         * Represents a Corporation that is private and cannot be joined by anyone.
         */
        PRIVATE

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

            if (Business.exists(owner)) c.children.add(Business.byOwner(owner));

            c.saveCorporation();

            CorporationCreateEvent event = new CorporationCreateEvent(c);
            Bukkit.getPluginManager().callEvent(event);
            return c;
        }
    }

    // Reading & Writing

    private static void checkTable() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS corporations (" +
                "id CHAR(36) NOT NUll," +
                "owner CHAR(36) NOT NULL," +
                "creation_date BIGINT NOT NULL," +
                "name VARCHAR(" + MAX_NAME_LENGTH + ") NOT NULL," +
                "description VARCHAR(" + MAX_DESCRIPTION_LENGTH + ") NOT NULL," +
                "icon VARCHAR(128) NOT NULL," +
                "hq BLOB(65535)," +
                "experience DOUBLE NOT NULL," +
                "children BLOB(65535) NOT NULL," +
                "children_joindates BLOB(65535) NOT NULL," +
                "achievements BLOB(65535) NOT NULL," +
                "settings BLOB(65535) NOT NULL," +
                "invited BLOB(65535) NOT NULL," +
                "PRIMARY KEY (id))"
        );

        DatabaseMetaData md = db.getMetaData();
        ResultSet rs = null;

        try {
            if (!(rs = md.getColumns(null, null, "corporations", "custom_model_data")).next())
                db.createStatement().execute("ALTER TABLE corporations ADD COLUMN custom_model_dat INT NOT NULL");
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
     * <p>Saves this Corporation to its Corporation file.</p>
     * <p>This method is called automatically.</p>
     */
    public void saveCorporation() {
        CORPORATION_CACHE.clear();

        try {
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

    private void writeDB() throws SQLException, IOException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        String sql;

        try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM corporations WHERE id = \"" + this.id + "\"")) {
            if (rs.next())
                sql = "UPDATE corporations SET " +
                        "id = ?, " +
                        "owner = ?, " +
                        "creation_date = ?, " +
                        "name = ?, " +
                        "description = ?, " +
                        "icon = ?, " +
                        "hq = ?, " +
                        "experience = ?, " +
                        "children = ?, " +
                        "children_joindates = ?, " +
                        "achievements = ?, " +
                        "settings = ?, " +
                        "invited = ?, " +
                        "custom_model_data = ? " +
                        "WHERE id = \"" + this.id + "\"";
            else
                sql = "INSERT INTO corporations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        PreparedStatement ps = db.prepareStatement(sql);

        ps.setString(1, this.id.toString());
        ps.setString(2, this.owner.getUniqueId().toString());
        ps.setLong(3, this.creationDate);
        ps.setString(4, this.name);
        ps.setString(5, this.description);

        ps.setString(6, this.icon.name());

        ByteArrayOutputStream hqOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream hqBos = new BukkitObjectOutputStream(hqOs);
        hqBos.writeObject(this.headquarters);
        hqBos.close();
        ps.setBytes(7, hqOs.toByteArray());
        ps.setDouble(8, this.experience);

        ByteArrayOutputStream childrenOs = new ByteArrayOutputStream();
        ObjectOutputStream childrenBos = new ObjectOutputStream(childrenOs);
        childrenBos.writeObject(this.children.stream().filter(Objects::nonNull).map(Business::getUniqueId).collect(Collectors.toList()));
        childrenBos.close();
        ps.setBytes(9, childrenOs.toByteArray());

        ByteArrayOutputStream childrenJoinDatesOs = new ByteArrayOutputStream();
        ObjectOutputStream childrenJoinDatesBos = new ObjectOutputStream(childrenJoinDatesOs);
        childrenJoinDatesBos.writeObject(this.childrenJoinDates);
        childrenJoinDatesBos.close();
        ps.setBytes(10, childrenJoinDatesOs.toByteArray());

        ByteArrayOutputStream achievementsOs = new ByteArrayOutputStream();
        ObjectOutputStream achievementsBos = new ObjectOutputStream(achievementsOs);
        achievementsBos.writeObject(this.achievements);
        achievementsBos.close();
        ps.setBytes(11, achievementsOs.toByteArray());

        ByteArrayOutputStream settingsOs = new ByteArrayOutputStream();
        ObjectOutputStream settingsBos = new ObjectOutputStream(settingsOs);
        settingsBos.writeObject(this.settings.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().name(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        settingsBos.close();
        ps.setBytes(12, settingsOs.toByteArray());

        ByteArrayOutputStream invitedOs = new ByteArrayOutputStream();
        ObjectOutputStream invitedBos = new ObjectOutputStream(invitedOs);
        invitedBos.writeObject(this.invited);
        invitedBos.close();
        ps.setBytes(13, invitedOs.toByteArray());

        ps.setInt(14, this.customModelData);

        ps.executeUpdate();
        ps.close();
    }

    private static Corporation readDB(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        UUID id = UUID.fromString(rs.getString("id"));
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("owner")));
        long creationDate = rs.getLong("creation_date");

        Corporation c = new Corporation(id, creationDate, owner);
        c.name = rs.getString("name");
        c.description = rs.getString("description");
        c.icon = Material.valueOf(rs.getString("icon"));

        ByteArrayInputStream hqIs = new ByteArrayInputStream(rs.getBytes("hq"));
        BukkitObjectInputStream hqBis = new BukkitObjectInputStream(hqIs);
        c.headquarters = (Location) hqBis.readObject();
        hqBis.close();

        c.experience = rs.getDouble("experience");

        ByteArrayInputStream childrenIs = new ByteArrayInputStream(rs.getBytes("children"));
        ObjectInputStream childrenBis = new ObjectInputStream(childrenIs);
        c.children.addAll(
                ((List<UUID>) childrenBis.readObject()).stream().map(Business::byId).collect(Collectors.toList())
        );
        childrenBis.close();

        ByteArrayInputStream childrenJoinDatesIs = new ByteArrayInputStream(rs.getBytes("children_joindates"));
        ObjectInputStream childrenJoinDatesBis = new ObjectInputStream(childrenJoinDatesIs);
        c.childrenJoinDates.putAll((Map<UUID, Long>) childrenJoinDatesBis.readObject());
        childrenJoinDatesBis.close();

        ByteArrayInputStream achievementsIs = new ByteArrayInputStream(rs.getBytes("achievements"));
        ObjectInputStream achievementsBis = new ObjectInputStream(achievementsIs);
        c.achievements.putAll((Map<CorporationAchievement, Integer>) achievementsBis.readObject());
        achievementsBis.close();

        ByteArrayInputStream settingsIs = new ByteArrayInputStream(rs.getBytes("settings"));
        ObjectInputStream settingsBis = new ObjectInputStream(settingsIs);
        c.settings.putAll(
                ((Map<String, Object>) settingsBis.readObject()).entrySet().stream()
                        .map(e -> new AbstractMap.SimpleEntry<>(Settings.Corporation.valueOf(e.getKey()), e.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        settingsBis.close();

        ByteArrayInputStream invitedIs = new ByteArrayInputStream(rs.getBytes("invited"));
        ObjectInputStream invitedBis = new ObjectInputStream(invitedIs);
        c.invited.putAll((Map<UUID, Long>) invitedBis.readObject());
        invitedBis.close();

        c.customModelData = rs.getInt("custom_model_data");

        return c;
    }

    private void writeFile() throws IOException {
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
        data.set("description", this.description);
        data.set("experience", this.experience);
        data.set("icon", this.icon.name());
        data.set("headquarters", this.headquarters);
        data.set("achievements", this.achievements
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().name().toLowerCase(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        if (!data.isConfigurationSection("stats")) data.createSection("stats");
        data.set("stats.views", this.views);
        data.set("custom_model_data", this.customModelData);

        data.save(dataF);

        File childrenF = new File(folder, "children.yml");
        if (!childrenF.exists()) childrenF.createNewFile();

        FileConfiguration children = YamlConfiguration.loadConfiguration(childrenF);
        children.set("children", this.children
                .stream()
                .filter(Objects::nonNull)
                .map(Business::getUniqueId)
                .map(UUID::toString)
                .collect(Collectors.toList())
        );

        if (!children.isConfigurationSection("join_dates")) children.createSection("join_dates");
        for (Map.Entry<UUID, Long> entry : this.childrenJoinDates.entrySet())
            children.set("join_dates." + entry.getKey().toString(), entry.getValue());

        children.set("invited", this.invited.entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        children.save(childrenF);

        File settingsF = new File(folder, "settings.yml");
        if (!settingsF.exists()) settingsF.createNewFile();

        FileConfiguration settings = YamlConfiguration.loadConfiguration(settingsF);
        if (!settings.isConfigurationSection("settings")) settings.createSection("settings");
        ConfigurationSection settingsSection = settings.getConfigurationSection("settings");

        this.settings.entrySet()
                .stream()
                .map(e -> {
                    Object value = e.getValue();
                    if (value instanceof Enum<?>) value = value.toString();

                    return new AbstractMap.SimpleEntry<>(e.getKey().name().toLowerCase(), value);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach(settingsSection::set);

        settings.save(settingsF);
    }

    @NotNull
    private static Corporation readFile(File folder) throws IOException, IllegalStateException, ReflectiveOperationException {
        File info = new File(folder, "info.dat");
        if (!info.exists()) throw new IllegalStateException("Could not find: info.dat");

        ObjectInputStream infoIs = new ObjectInputStream(Files.newInputStream(info.toPath()));
        UUID id = (UUID) infoIs.readObject();
        long creationDate = infoIs.readLong();
        OfflinePlayer owner = Bukkit.getOfflinePlayer((UUID) infoIs.readObject());
        infoIs.close();

        Corporation c = new Corporation(id, creationDate, owner);

        File dataF = new File(folder, "data.yml");
        if (!dataF.exists()) dataF.createNewFile();

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataF);
        c.name = data.getString("name");
        c.description = data.getString("description", "");
        c.experience = data.getDouble("experience");
        c.icon = Material.valueOf(data.getString("icon"));
        c.headquarters = (Location) data.get("headquarters");

        c.achievements.putAll(data.getConfigurationSection("achievements").getValues(false)
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(CorporationAchievement.valueOf(e.getKey().toUpperCase()), (Integer) e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        c.views = data.getInt("stats.views");
        c.customModelData = data.getInt("custom_model_data");

        File childrenF = new File(folder, "children.yml");
        if (!childrenF.exists()) childrenF.createNewFile();

        FileConfiguration children = YamlConfiguration.loadConfiguration(childrenF);
        c.children.addAll(children.getStringList("children")
                .stream()
                .map(UUID::fromString)
                .map(Business::byId)
                .collect(Collectors.toList()));

        if (children.isConfigurationSection("join_dates"))
            for (Map.Entry<String, Object> entry : children.getConfigurationSection("join_dates").getValues(false).entrySet()) {
                UUID bid = UUID.fromString(entry.getKey());
                long joinDate = (long) entry.getValue();

                c.childrenJoinDates.put(bid, joinDate);
            }

        if (children.isConfigurationSection("invited"))
            for (Map.Entry<String, Object> entry : children.getConfigurationSection("invited").getValues(false).entrySet()) {
                UUID bid = UUID.fromString(entry.getKey());
                long inviteDate = (long) entry.getValue();

                c.invited.put(bid, inviteDate);
            }

        File settingsF = new File(folder, "settings.yml");
        if (!settingsF.exists()) settingsF.createNewFile();

        FileConfiguration settings = YamlConfiguration.loadConfiguration(settingsF);
        if (settings.isConfigurationSection("settings"))
            c.settings.putAll(settings.getConfigurationSection("settings").getValues(false)
                    .entrySet()
                    .stream()
                    .map(e -> {
                        Settings.Corporation<?> key = Arrays.stream(Settings.Corporation.values())
                                .filter(sett -> sett.name().equalsIgnoreCase(e.getKey()))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new);

                        return new AbstractMap.SimpleEntry<>(key, key.parseValue(e.getValue().toString()));
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        return c;
    }
}
