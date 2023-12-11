package us.teaminceptus.novaconomy.api.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an Economy
 */
public final class Economy implements ConfigurationSerializable, Comparable<Economy> {

    // Constants

    /**
     * The maximum length of an Economy name
     */
    public static final int MAX_NAME_LENGTH = 32;

    /**
     * The maximum amount of a conversion scale
     */
    public static final double MAX_CONVERSION_SCALE = 999;

    private static final String IGNORE_TAXES = "Taxes.Automatic.Ignore";

    private final char symbol;
    private final File file;
    private String name;
    private ItemStack icon;
    private boolean hasNaturalIncrease;
    private double conversionScale;
    private final UUID uid;
    private boolean interestEnabled;
    private int customModelData;
    private boolean clickableReward;
    private boolean isConvertable;

    private Economy(UUID id, String name, ItemStack icon, char symbol, boolean naturalIncrease, double conversionScale, boolean clickable) {
        this.symbol = symbol;
        this.file = new File(NovaConfig.getEconomiesFolder(), id.toString() + ".yml");
        this.name = name;
        this.icon = icon;
        this.hasNaturalIncrease = naturalIncrease;
        this.conversionScale = conversionScale;
        this.uid = id;
        this.clickableReward = clickable;
    }

    // Other

    /**
     * Whether this Economy supports interest
     * @return true if allows, else false
     */
    public boolean hasInterest() {
        return this.interestEnabled;
    }

    /**
     * Fetch a set of all economies supporting interest
     * @return Set of Interest Economies
     */
    @NotNull
    public static Set<Economy> getInterestEconomies() { return getEconomies().stream().filter(Economy::hasInterest).collect(Collectors.toSet()); }

    /**
     * Fetches a set of Economies that have {@link #hasTax()} to true.
     * @return Set of Taxable Economies
     */
    @NotNull
    public static Set<Economy> getTaxableEconomies() { return getEconomies().stream().filter(Economy::hasTax).collect(Collectors.toSet()); }

    /**
     * Fetches a set of Economies that have {@link #hasClickableReward()} to true.
     * @return Set of Economies that can be rewarded on click
     */
    @NotNull
    public static Set<Economy> getClickableRewardEconomies() { return getEconomies().stream().filter(Economy::hasClickableReward).collect(Collectors.toSet()); }

    /**
     * Whether this Economy is rewarded when a business is clicked on when advertising.
     * @return true if rewarded, else false
     */
    public boolean hasClickableReward() {
        return clickableReward;
    }

    /**
     * Set the interest acception state of this economy
     * @param interest Whether to allow interest
     */
    public void setInterest(boolean interest) {
        this.interestEnabled = interest;
        saveEconomy();
    }

    /**
     * Sets the custom model data integer of this economy
     * @param customModelData Custom Model Data
     */
    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        saveEconomy();
    }

    /**
     * Sets the economy's name.
     * @param name Name of the economy
     */
    public void setName(@NotNull String name) {
        if (name == null) throw new NullPointerException("Name cannot be null");
        if (name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Name cannot be longer than " + MAX_NAME_LENGTH + " characters");

        this.name = name;
        saveEconomy();
    }

    /**
     * Sets the icon of this economy.
     * @param icon Icon of the economy
     * @throws IllegalArgumentException if icon is null
     */
    public void setIcon(@NotNull ItemStack icon) throws IllegalArgumentException {
        if (icon == null) throw new IllegalArgumentException("Icon cannot be null");
        this.icon = icon;
        saveEconomy();
    }

    /**
     * Sets the icon of this economy.
     * @param m Icon of the economy
     * @throws IllegalArgumentException if icon is null
     */
    public void setIcon(@NotNull Material m) throws IllegalArgumentException {
        if (m == null) throw new IllegalArgumentException("Icon cannot be null");

        ItemStack item = new ItemStack(m);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + this.name);

        try {
            Method setModelData = meta.getClass().getDeclaredMethod("setCustomModelData", Integer.class);
            setModelData.setAccessible(true);
            setModelData.invoke(meta, this.customModelData);
        } catch (NoSuchMethodException ignored) {}
        catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }
        item.setItemMeta(meta);

        this.icon = item;
        saveEconomy();
    }

    /**
     * Sets whether this economy naturally increases.
     * @param increase Whether to increase naturally
     */
    public void setIncreaseNaturally(boolean increase) {
        this.hasNaturalIncrease = increase;
        saveEconomy();
    }

    /**
     * Sets the conversion scale of this Economy.
     * @param scale Conversion Scale
     * @throws IllegalArgumentException if scale is less than 0
     */
    public void setConversionScale(double scale) throws IllegalArgumentException {
        if (scale <= 0) throw new IllegalArgumentException("Conversion Scale must be greater than 0");
        if (scale > MAX_CONVERSION_SCALE) throw new IllegalArgumentException("Conversion Scale cannot be greater than " + MAX_CONVERSION_SCALE);

        this.conversionScale = scale;
        saveEconomy();
    }

    /**
     * Sets whether this economy is rewarded by clicking on a business icon when advertising.
     * @param clickable true if clickable, else false
     */
    public void setHasClickableReward(boolean clickable) {
        this.clickableReward = clickable;
        saveEconomy();
    }

    /**
     * Whether this Economy is taxable.
     * <br><br>
     * This value is not serialized with the Economy and is changable from config.yml.
     * @return true if taxable, else false
     */
    public boolean hasTax() {
        return !NovaConfig.loadConfig().getStringList(IGNORE_TAXES).contains(this.name);
    }

    /**
     * Sets whether this Economy is taxable.
     * <br><br>
     * This value is not serialized with the Economy and is changable from config.yml.
     * @param tax Whether to allow tax
     * @return true if successful, else false
     */
    public boolean setTax(boolean tax) {
        FileConfiguration config = NovaConfig.loadConfig();
        List<String> ignore = config.getStringList(IGNORE_TAXES);
        boolean b = tax ? ignore.remove(this.name) : ignore.add(this.name);
        config.set(IGNORE_TAXES, ignore);
        try { config.save(NovaConfig.getConfigFile()); } catch (Exception ignored) {}

        return b;
    }

    /**
     * Remove an Economy from the Plugin
     * @param name Name of the Economy
     * @see Economy#removeEconomy(Economy)
     */
    public static void removeEconomy(@NotNull String name) { removeEconomy(byName(name)); }

    /**
     * Remove an Economy from the Plugin
     * @param econ Economy to remove
     * @throws IllegalArgumentException if economy is null
     */
    public static void removeEconomy(@Nullable Economy econ) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        ECONOMY_CACHE.clear();

        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            NovaPlayer np = new NovaPlayer(p);
            Map<String, Object> data = np.getPlayerData();

            data.keySet().stream()
                    .filter(key -> key.startsWith("economy." + econ.getName()))
                    .forEach(data::remove);
        }

        if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
            Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

            try {
                PreparedStatement ps = db.prepareStatement("DELETE FROM economies WHERE id = ?");
                ps.setString(1, econ.getUniqueId().toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                NovaConfig.print(e);
            }
        } else {
            econ.file.delete();
        }

        NovaConfig.getConfiguration().reloadHooks();
    }

    /**
     * Fetch an Economy 
     * @param name Name of the Economy
     * @return Found Economy, or null if none found
     * @throws IllegalArgumentException if name is null
     */
    @Nullable
    public static Economy byName(@NotNull String name) throws IllegalArgumentException {
        if (name == null) throw new IllegalArgumentException("Name cannot be null");

        return Economy.getEconomies().stream()
                .filter(e -> e.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether this Economy exists.
     * @param name Name of the Economy
     * @return true if economy exists, else false
     */
    public static boolean exists(@Nullable String name) {
        if (name == null) return false;
        return byName(name) != null;
    }

    /**
     * Whetehr an economy with this UUID exists.
     * @param uid UUID of the Economy
     * @return true if economy exists, else false
     */
    public static boolean exists(@Nullable UUID uid) {
        if (uid == null) return false;
        return byId(uid) != null;
    }

    /**
     * Whether an Economy with this symbol exists.
     * @param c Symbol of the economy
     * @return true if economy exists, else false`
     */
    public static boolean exists(char c) {
        return bySymbol(c) != null;
    }

    /**
     * Whether this economy will naturally increase (not the same as Interest)
     * <p>
     * An economy increasing naturally means that it increases from NaturalCauses (i.e. Mining, Fishing). Specific events can be turned off in the configuration.
     * <p>
     * Death Decrease applies to ALL Economies, so turn it off globally if you don't want that.
     * @return true if naturally increases, else false
     */
    public boolean hasNaturalIncrease() { return this.hasNaturalIncrease; }

    /**
     * Return the scale of which this economy will be converted to a different economy
     * <p>
     * An economy with a conversion scale of {@code 1} and another with a conversion scale of {@code 0.5} would have a 2:1 ratio, meaning that 100 in the first economy would be 200 in the second economy.
     * @return conversion scale of this economy
     */
    public double getConversionScale() { return this.conversionScale; }

    private static final Set<Economy> ECONOMY_CACHE = new HashSet<>();

    /**
     * Reloads the Economy Cache.
     */
    public static void reloadEconomies() {
        ECONOMY_CACHE.clear();
        getEconomies();
    }

    /**
     * Fetch a set of all economies registered on the Plugin
     * @return Set of Registered Economies
     */
    @NotNull
    public static Set<Economy> getEconomies() {
        if (!ECONOMY_CACHE.isEmpty()) return ECONOMY_CACHE;
        Set<Economy> economies = new HashSet<>();

        if (NovaConfig.getConfiguration().isDatabaseEnabled())
            try {
                checkTable();
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                PreparedStatement ps = db.prepareStatement("SELECT *, COUNT(*) FROM economies GROUP BY id");
                ResultSet rs = ps.executeQuery();

                while (rs.next()) economies.add(readDB(rs));

                rs.close();
                ps.close();
            } catch (Exception e) {
                NovaConfig.print(e);
            }
        else {
            List<File> files = NovaConfig.getEconomiesFolder().listFiles() == null ? new ArrayList<>() : Arrays.asList(NovaConfig.getEconomiesFolder().listFiles());
            for (File f : files) {
                if (f == null) continue;
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                economies.add(byId(id));
            }
        }

        ECONOMY_CACHE.addAll(economies);
        return ECONOMY_CACHE;
    }

    /**
     * Fetch a set of economies that increase naturally
     * @return Set of Economies that increase naturally
     */
    public static Set<Economy> getNaturalEconomies() { return getEconomies().stream().filter(Economy::hasNaturalIncrease).collect(Collectors.toSet()); }

    /**
     * Return the ConfigurationSection that this Economy is stored in
     * @deprecated Economies are no longer stored in an individual file
     * @return {@link ConfigurationSection} of this economy
     */
    @Deprecated
    public ConfigurationSection getEconomySection() { return null; }

    /**
     * Fetch the name of this economy
     * @return Name of this economy
     */
    @NotNull
    public String getName() { return this.name; }

    /**
     * Fetches the economy's symbol.
     * @return Symbol of Economy
     */
    public char getSymbol() {
        return this.symbol;
    }

    /**
     * Get the Icon of this Economy
     * @return Icon of this Economy
     */
    @NotNull
    public ItemStack getIcon() {
        return this.icon;
    }

    /**
     * Gets the Economy Icon's Type.
     * @return Icon Material Type
     */
    @NotNull
    public Material getIconType() { return this.icon.getType(); }

    /**
     * Convert this economy to another economy
     * @param to The New Economy to convert to
     * @param fromAmount How much amount is to be converted
     * @return Converted amount in the other economy's form
     * @throws IllegalArgumentException if to or from is null, economies are identical, or either are not convertable
     * @see Economy#convertAmount(Economy, Economy, double)
     */
    public double convertAmount(@NotNull Economy to, double fromAmount) throws IllegalArgumentException {
        return convertAmount(this, to, fromAmount);
    }

    /**
     * Get the Economy's unique identifier.
     * @return Unique Identifier
     */
    @NotNull
    public UUID getUniqueId() {
        return this.uid;
    }



    /**
     * Convert one economy's balance to another
     * @param from The Economy to convert from
     * @param to The Economy to convert to
     * @param fromAmount How much amount is to be converted
     * @return Converted amount in the other economy's form
     * @throws IllegalArgumentException if to or from is null, economies are identical, or either is not convertable
     */
    public static double convertAmount(@NotNull Economy from, @NotNull Economy to, double fromAmount) throws IllegalArgumentException {
        if (from == null) throw new IllegalArgumentException("Economy from is null");
        if (to == null) throw new IllegalArgumentException("Economy to is null");
        if (from == to) throw new IllegalArgumentException("Economies are identical");
        if (!from.isConvertable) throw new IllegalArgumentException("Economy from is not convertable");
        if (!to.isConvertable) throw new IllegalArgumentException("Economy to is not convertable");

        double scale = from.getConversionScale() / to.getConversionScale();

        return fromAmount * scale;
    }

    /**
     * Attempts to lookup an Economy by its unique id.
     * @param uid UUID to find
     * @return Economy found, or null if not found or if UUID is null
     */
    @Nullable
    public static Economy byId(@Nullable UUID uid) {
        if (uid == null) return null;

        Economy econ = null;

        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

                PreparedStatement ps = db.prepareStatement("SELECT * FROM economies WHERE id = ?");
                ps.setString(1, uid.toString());

                ResultSet rs = ps.executeQuery();
                if (rs.next()) econ = readDB(rs);

                rs.close();
                ps.close();
            } else {
                File f = new File(NovaConfig.getEconomiesFolder(), uid + ".yml");
                if (!f.exists()) return null;

                FileConfiguration config = YamlConfiguration.loadConfiguration(f);
                econ = (Economy) config.get(uid.toString());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return econ;
    }

    /**
     * Fetches the custom model data integer for this Economy.
     * @return Custom Model Data Integer
     */
    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * Fetches whether an Economy can be converted to another Economy.
     * @return true if convertable, else false
     */
    public boolean isConvertable() {
        return isConvertable;
    }

    /**
     * Sets whether an Economy can be converted to another Economy.
     * @param convertable true if convertable, else false
     */
    public void setConvertable(boolean convertable) {
        this.isConvertable = convertable;
        saveEconomy();
    }

    /**
     * Fetches an Economy by its unique symbol.
     * @param symbol Symbol to find
     * @return Economy found, or null if not found
     */
    @Nullable
    public static Economy bySymbol(char symbol) {
        for (Economy econ : Economy.getEconomies()) if (econ.getSymbol() == symbol) return econ;
        return null;
    }

    /**
     * Gets the first Economy in the list of Economies.
     * @return First Economy
     * @throws IllegalStateException if no economies are registered
     */
    public static Economy first() throws IllegalStateException {
        if (getEconomies().isEmpty()) throw new IllegalStateException("No Economies are registered");

        return getEconomies()
                .stream()
                .sorted(Economy::compareTo)
                .collect(Collectors.toList())
                .get(0);
    }

    /**
     * Fetch a Builder used for creating economies
     * @return {@link Builder} Class
     */
    public static Economy.Builder builder() {
        return new Economy.Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Economy economy = (Economy) o;
        return economy.name.equals(this.name) || this.uid.equals(economy.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }

    @Override
    public int compareTo(@NotNull Economy o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return "Economy{" +
                "id=" + uid +
                ", symbol=" + symbol +
                ", name='" + name + '\'' +
                ", icon=" + icon.getType().name() +
                ", hasNaturalIncrease=" + hasNaturalIncrease +
                ", conversionScale=" + conversionScale +
                ", interestEnabled=" + interestEnabled +
                ", customModelData=" + customModelData +
                ", clickableReward=" + clickableReward +
                '}';
    }

    // Serialization

    private static void checkTable() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS economies (" +
                "id CHAR(36) NOT NULL," +
                "name VARCHAR(" + MAX_NAME_LENGTH + ") NOT NULL," +
                "symbol CHAR(1) NOT NULL," +
                "icon BLOB(65535) NOT NULL," +
                "natural_increase BOOL NOT NULL," +
                "conversion_scale DOUBLE NOT NULL," +
                "interest BOOL NOT NULL," +
                "custom_model_data INT NOT NULL," +
                "clickable_reward BOOL NOT NULL," +
                "convertable BOOL NOT NULL DEFAULT TRUE," +
                "PRIMARY KEY (id))"
        );

        DatabaseMetaData md = db.getMetaData();
        ResultSet rs = null;

        try {
            if (!(rs = md.getColumns(null, null, "economies", "convertable")).next())
                db.createStatement().execute("ALTER TABLE economies ADD convertable BOOL NOT NULL DEFAULT TRUE");
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
     * Saves this Economy to its file.
     */
    public void saveEconomy() {
        try {
            ECONOMY_CACHE.clear();

            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                writeDB();
            } else {
                if (!NovaConfig.getEconomiesFolder().exists()) NovaConfig.getEconomiesFolder().mkdir();
                if (!this.file.exists()) this.file.createNewFile();

                FileConfiguration config = YamlConfiguration.loadConfiguration(this.file);
                config.set(this.uid.toString(), this);
                config.set("last_saved_timestamp", System.currentTimeMillis());

                config.save(this.file);
            }
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private void writeDB() throws IOException, SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        String sql;

        try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM economies WHERE id = \"" + this.uid +"\"")) {
            if (rs.next())
                sql = "UPDATE economies SET " +
                        "id = ?, " +
                        "name = ?, " +
                        "symbol = ?, " +
                        "icon = ?, " +
                        "natural_increase = ?, " +
                        "conversion_scale = ?, " +
                        "interest = ?, " +
                        "custom_model_data = ?, " +
                        "clickable_reward = ?, " +
                        "convertable = ? " +
                        "WHERE id = \"" + this.uid + "\"";
            else
                sql = "INSERT INTO economies VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        PreparedStatement ps = db.prepareStatement(sql);
    
        ps.setString(1, this.uid.toString());
        ps.setString(2, this.name);
        ps.setString(3, String.valueOf(this.symbol));

        ByteArrayOutputStream iconOs = new ByteArrayOutputStream();
        BukkitObjectOutputStream iconBos = new BukkitObjectOutputStream(iconOs);
        iconBos.writeObject(this.icon);
        iconBos.close();

        ps.setBytes(4, iconOs.toByteArray());

        ps.setBoolean(5, this.hasNaturalIncrease);
        ps.setDouble(6, this.conversionScale);
        ps.setBoolean(7, this.interestEnabled);
        ps.setInt(8, this.customModelData);
        ps.setBoolean(9, this.clickableReward);
        ps.setBoolean(10, this.isConvertable);

        ps.executeUpdate();
        ps.close();
    }

    private static Economy readDB(ResultSet rs) throws ClassNotFoundException, IOException, SQLException {
        UUID uid = UUID.fromString(rs.getString("id"));
        String name = rs.getString("name");
        char symbol = rs.getString("symbol").charAt(0);

        ByteArrayInputStream iconIs = new ByteArrayInputStream(rs.getBytes("icon"));
        BukkitObjectInputStream iconBis = new BukkitObjectInputStream(iconIs);
        ItemStack icon = (ItemStack) iconBis.readObject();
        iconBis.close();

        boolean hasNaturalIncrease = rs.getBoolean("natural_increase");
        double conversionScale = rs.getDouble("conversion_scale");
        boolean interestEnabled = rs.getBoolean("interest");
        int customModelData = rs.getInt("custom_model_data");
        boolean clickableReward = rs.getBoolean("clickable_reward");
        boolean isConvertable = rs.getBoolean("convertable");

        Economy en = new Economy(uid, name, icon, symbol, hasNaturalIncrease,  conversionScale, clickableReward);
        en.interestEnabled = interestEnabled;
        en.customModelData = customModelData;
        en.isConvertable = isConvertable;

        return en;
    }

    /**
     * Serialization inside a YML File
     * @return Serialized Economy
     */
    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> serial = new HashMap<>();
        serial.put("id", this.uid.toString());
        serial.put("name", this.name);
        serial.put("icon", this.icon);
        serial.put("symbol", this.symbol);
        serial.put("increase-naturally", this.hasNaturalIncrease);
        serial.put("conversion-scale", this.conversionScale);
        serial.put("interest", this.interestEnabled);
        serial.put("custom-model-data", this.customModelData);
        serial.put("clickable", this.clickableReward);
        serial.put("convertable", this.isConvertable);
        return serial;
    }

    /**
     * Serialization from a YML File
     * <p>
     * This method is only meant to take maps from {@link Economy#serialize()}. **Using Maps outside of this may break the plugin**.
     * @param serial Map of serialization
     * @throws NullPointerException if a necessary part is missing
     * @return Economy Class
     * @see Economy#serialize()
     */
    public static Economy deserialize(@Nullable Map<String, Object> serial) throws NullPointerException {
        if (serial == null) return null;

        String name = (String) serial.get("name");
        UUID id = UUID.fromString(serial.getOrDefault("id", UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString()).toString());

        Economy econ = new Economy(
                id,
                name,
                (ItemStack) serial.get("icon"),
                serial.get("symbol").toString().charAt(0),
                (boolean) serial.getOrDefault("increase-naturally", true),
                (double) serial.getOrDefault("conversion-scale", 1),
                (boolean) serial.getOrDefault("clickable", true)
        );

        econ.interestEnabled = (boolean) serial.getOrDefault("interest", true);
        econ.customModelData = (int) serial.getOrDefault("custom-model-data", 0);
        econ.isConvertable = (boolean) serial.getOrDefault("convertable", true);

        return econ;
    }

    /**
     * Class used for Creating Economies
     * @see Economy#builder()
     */
    public static final class Builder {
        char symbol;
        String name;
        ItemStack icon;
        boolean increaseNaturally;
        double conversionScale;
        int customModelData;
        boolean clickableReward;

        private Builder() {
            this.icon = new ItemStack(Material.GOLD_INGOT);
            this.symbol = '$';
            this.increaseNaturally = true;
            this.conversionScale = 1;
            this.clickableReward = true;
        }

        /**
         * Set the Symbol of this Economy
         * @param symbol Symbol of this economy
         * @return Builder Class, for chaining
         */
        public Builder setSymbol(char symbol) {
            this.symbol = symbol;
            return this;
        }

        /**
         * Set the name of this Economy
         * @param name Name of the Economy
         * @return Builder Class, for chaining
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the Icon of this Economy
         * @param iconM Icon Material
         * @return Builder Class, for chaining
         * @see Builder#setIcon(ItemStack)
         */
        public Builder setIcon(Material iconM) {
            ItemStack icon = new ItemStack(iconM);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + this.name);

            int modelData = Economy.getEconomies().size() + 1;

            try {
                Method m = meta.getClass().getDeclaredMethod("setCustomModelData", Integer.class);
                m.setAccessible(true);
                m.invoke(meta, modelData);

                this.customModelData = modelData;
            } catch (NoSuchMethodException ignored) {}
            catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
            }

            icon.setItemMeta(meta);
            this.icon = icon;

            return this;
        }

        /**
         * Set the Icon of this Economy
         * @param stack {@link ItemStack} for this economy
         * @return Builder Class, for chaining
         */
        public Builder setIcon(ItemStack stack) {
            this.icon = stack;
            return this;
        }

        /**
         * Set whether or not this economy increases naturally (i.e. Increases from Mining)
         * @param increaseNaturally Whether this economy should increase from Natural Causes
         * @return Builder Class, for chaining
         */
        public Builder setIncreaseNaturally(boolean increaseNaturally) {
            this.increaseNaturally = increaseNaturally;
            return this;
        }

        /**
         * Set the Conversion Scale (used to convert the value to other economies)
         * @param scale Scale of this economy
         * @return Builder Class, for chaining
         * @throws IllegalArgumentException if scale is not positive
         */
        public Builder setConversionScale(double scale) throws IllegalArgumentException {
            if (scale <= 0) throw new IllegalArgumentException("Scale must be positive");
            this.conversionScale = scale;
            return this;
        }

        /**
         * Sets whether this Economy can be usedas a reward from host businesses when advertising.
         * @param clickableReward true if clickable reward, else false
         * @return Builder Class, for chaining
         */
        public Builder setClickableReward(boolean clickableReward) {
            this.clickableReward = clickableReward;
            return this;
        }

        /**
         * Sets the custom model data integer for this Economy.
         * @param modelData Custom Model Data Integer
         * @return Builder Class, for chaining
         */
        public Builder setCustomModelData(int modelData) {
            this.customModelData = modelData;
            return this;
        }

        /**
         * Builds this economy
         * @return Created Economy
         * @throws IllegalArgumentException if name is null (icon's default is Gold Ingot)
         * @throws UnsupportedOperationException if already exists or symbol is taken
         */
        public Economy build() throws IllegalArgumentException, UnsupportedOperationException {
            if (this.name == null) throw new IllegalArgumentException("Name cannot be null");
            ECONOMY_CACHE.clear();

            UUID id = UUID.nameUUIDFromBytes(this.name.getBytes(StandardCharsets.UTF_8));
            File file = new File(NovaConfig.getEconomiesFolder(), id + ".yml");

            if (file.exists()) throw new UnsupportedOperationException("Economy already exists");

            for (Economy econ : Economy.getEconomies()) if (econ.getSymbol() == this.symbol) throw new UnsupportedOperationException("Symbol is taken");

            Economy econ = new Economy(id, this.name, this.icon, this.symbol, this.increaseNaturally, this.conversionScale, this.clickableReward);
            econ.customModelData = customModelData;
            econ.isConvertable = true;

            econ.saveEconomy();

            NovaConfig.getConfiguration().reloadHooks();
            return econ;
        }
    }
}