package us.teaminceptus.novaconomy.api.economy;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an Economy
 */
public final class Economy implements ConfigurationSerializable {

    private static final String IGNORE_TAXES = "Taxes.Automatic.Ignore";
    private final char symbol;
    private final String section;
    private final String name;
    private final ItemStack icon;
    private final boolean hasNaturalIncrease;
    private final double conversionScale;

    private final UUID uid;

    private boolean interestEnabled;

    private int customModelData;

    private Economy(String section, String name, ItemStack icon, char symbol, boolean naturalIncrease, double conversionScale) {
        this.symbol = symbol;
        this.section = section;
        this.name = name;
        this.icon = icon;
        this.hasNaturalIncrease = naturalIncrease;
        this.conversionScale = conversionScale;
        this.uid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    // Implementation & Recommended Implementation

    /**
     * Serialization inside a YML File
     * @return Serialized Economy
     */
    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> serial = new HashMap<>();
        serial.put("section", this.section);
        serial.put("name", this.name);
        serial.put("icon", this.icon);
        serial.put("symbol", this.symbol);
        serial.put("increase-naturally", this.hasNaturalIncrease);
        serial.put("conversion-scale", this.conversionScale);
        serial.put("interest", this.interestEnabled);
        serial.put("custom-model-data", this.customModelData);
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
        Economy econ = new Economy(serial.get("section").toString(), serial.get("name").toString(), (ItemStack) serial.get("icon"), serial.get("symbol").toString().charAt(0), (boolean) serial.getOrDefault("increase-naturally", true), (double) serial.getOrDefault("conversion-scale", 1));

        econ.interestEnabled = (boolean) serial.getOrDefault("interest", true);
        econ.customModelData = (int) serial.getOrDefault("custom-model-data", 0);

        return econ;
    }

    // Other

    /**
     * Whether or not this Economy supports interest
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
    public static Set<Economy> getTaxableEconomies() { return getEconomies().stream().filter(Economy::hasTax).collect(Collectors.toSet()); }

    /**
     * Set the interest acception state of this economy
     * @param interest Whether or not to allow interest
     */
    public void setInterest(boolean interest) {
        this.interestEnabled = interest;
        saveFile();
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
     * Saves this Economy to its file.
     */
    public void saveFile() {
        FileConfiguration config = NovaConfig.getEconomiesConfig();
        ConfigurationSection section = config.getConfigurationSection(this.section);
        section.set("economy", this);
        section.set("last_saved_timestamp", System.currentTimeMillis());

        try {
            config.save(NovaConfig.getEconomiesFile());
        } catch (IOException e) {
            NovaConfig.getLogger().severe(e.getMessage());
        }
    }

    /**
     * Remove an Economy from the Plugin
     * @param name Name of the Economy
     * @see Economy#removeEconomy(Economy)
     */
    public static void removeEconomy(@NotNull String name) { removeEconomy(getEconomy(name)); }

    /**
     * Remove an Economy from the Plugin
     * @param econ Economy to remove
     * @throws IllegalArgumentException if economy is null
     */
    public static void removeEconomy(@Nullable Economy econ) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        FileConfiguration config = NovaConfig.getEconomiesConfig();

        config.set(econ.getEconomySection().getCurrentPath(), null);
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            NovaPlayer np = new NovaPlayer(p);
            FileConfiguration pConfig = np.getPlayerConfig();

            pConfig.set("economies." + econ.getName().toLowerCase(), null);

            try {
                pConfig.save(new File(NovaConfig.getPlugin().getDataFolder().getPath() + "/players", p.getUniqueId() + ".yml"));
            } catch (IOException e) {
                NovaConfig.getLogger().severe(e.getMessage());
            }
        }

        try {
            config.save(new File(NovaConfig.getPlugin().getDataFolder(), "economies.yml"));
        } catch (IOException e) {
            NovaConfig.getLogger().info("Error removing economy " + econ.getName());
            NovaConfig.getLogger().severe(e.getMessage());
        }
        NovaConfig.getConfiguration().reloadHooks();
    }

    /**
     * Fetch an Economy 
     * @param name Name of the Economy
     * @return Found Economy, or null if none found
     * @throws IllegalArgumentException if name is null
     */
    public static Economy getEconomy(@NotNull String name) throws IllegalArgumentException {
        Validate.notNull(name, "Name is null");
        ConfigurationSection section = NovaConfig.getEconomiesConfig().getConfigurationSection(name.toLowerCase());
        if (section != null) return (Economy) section.get("economy");
        else return null;
    }

    /**
     * Whether this Economy exists.
     * @param name Name of the Economy
     * @return true if economy exists, else false
     */
    public static boolean exists(@Nullable String name) {
        if (name == null) return false;
        return getEconomy(name) != null;
    }

    /**
     * Whetehr an economy with this UUID exists.
     * @param uid UUID of the Economy
     * @return true if economy exists, else false
     */
    public static boolean exists(@Nullable UUID uid) {
        if (uid == null) return false;
        return getEconomy(uid) != null;
    }

    /**
     * Whether an Economy with this symbol exists.
     * @param c Symbol of the economy
     * @return true if economy exists, else false
     */
    public static boolean exists(char c) {
        return getEconomy(c) != null;
    }

    /**
     * Whether or not this economy will naturally increase (not the same as Interest)
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

    /**
     * Fetch a set of all economies registered on the Plugin
     * @return Set of Registered Economies
     */
    public static Set<Economy> getEconomies() {
        Set<Economy> economies = new HashSet<>();

        final FileConfiguration config = NovaConfig.getEconomiesConfig();
        config.getKeys(false).forEach(key -> economies.add((Economy) config.getConfigurationSection(key).get("economy")));

        return economies;
    }

    /**
     * Fetch a set of economies that increase naturally
     * @return Set of Economies that increase naturally
     */
    public static Set<Economy> getNaturalEconomies() { return getEconomies().stream().filter(Economy::hasNaturalIncrease).collect(Collectors.toSet()); }

    /**
     * Return the ConfigurationSection that this Economy is stored in
     * @return {@link ConfigurationSection} of this economy
     */
    public ConfigurationSection getEconomySection() { return NovaConfig.getEconomiesConfig().getConfigurationSection(this.section); }

    /**
     * Fetch the name of this economy
     * @return Name of this economy
     */
    public String getName() { return this.name; }

    /**
     * Get the Icon of this Economy
     * @return Icon of this Economy
     */
    public ItemStack getIcon() {
        return this.icon;
    }

    /**
     * Gets the Economy Icon's Type.
     * @return Icon Material Type
     */
    public Material getIconType() { return this.icon.getType(); }

    /**
     * Convert this economy to another economy
     * @param to The New Economy to convert to
     * @param fromAmount How much amount is to be converted
     * @return Converted amount in the other economy's form
     * @throws IllegalArgumentException if to or from is null, or economies are identical
     * @see Economy#convertAmount(Economy, Economy, double)
     */
    public double convertAmount(Economy to, double fromAmount) throws IllegalArgumentException {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Economy economy = (Economy) o;
        return economy.getName().equals(this.getName()) || economy.getUniqueId().equals(this.getUniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, section, name, icon, hasNaturalIncrease, conversionScale, interestEnabled);
    }

    /**
     * Convert one economy's balance to another
     * @param from The Economy to convert from
     * @param to The Economy to convert to
     * @param fromAmount How much amount is to be converted
     * @return Converted amount in the other economy's form
     * @throws IllegalArgumentException if to or from is null, or economies are identical
     */
    public static double convertAmount(Economy from, Economy to, double fromAmount) throws IllegalArgumentException {
        if (from == null) throw new IllegalArgumentException("Economy from is null");
        if (to == null) throw new IllegalArgumentException("Economy to is null");
        if (from.getName().equals(to.getName())) throw new IllegalArgumentException("Economies are identical");
        double scale = from.getConversionScale() / to.getConversionScale();

        return fromAmount * scale;
    }

    /**
     * Attempts to lookup an Economy by its unique id.
     * @param uid UUID to find
     * @return Economy found, or null if not found or if UUID is null
     */
    @Nullable
    public static Economy getEconomy(@Nullable UUID uid) {
        if (uid == null) return null;

        for (Economy econ : Economy.getEconomies()) if (econ.getUniqueId().equals(uid)) return econ;
        return null;
    }

    /**
     * Fetches the custom model data integer for this Economy.
     * @return Custom Model Data Integer
     */
    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * Fetches an Economy by its unique symbol.
     * @param symbol Symbol to find
     * @return Economy found, or null if not found
     */
    @Nullable
    public static Economy getEconomy(char symbol) {
        for (Economy econ : Economy.getEconomies()) if (econ.getSymbol() == symbol) return econ;
        return null;
    }

    /**
     * Fetch a Builder used for creating economies
     * @return {@link Builder} Class
     */
    public static Economy.Builder builder() {
        return new Economy.Builder();
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
        int modelData;

        private Builder() {
            this.icon = new ItemStack(Material.GOLD_INGOT);
            this.symbol = '$';
            this.increaseNaturally = true;
            this.conversionScale = 1;
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

                this.modelData = modelData;
            } catch (NoSuchMethodException ignored) {}
            catch (ReflectiveOperationException e) {
                Bukkit.getLogger().severe(e.getClass().getSimpleName());
                Bukkit.getLogger().severe(e.getMessage());
                for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
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
         * @param increaseNaturally Whether or not this economy should increase from Natural Causes
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
         */
        public Builder setConversionScale(double scale) {
            this.conversionScale = scale;
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

            if (NovaConfig.getEconomiesConfig().getConfigurationSection(this.name.toLowerCase()) != null)
                throw new UnsupportedOperationException("Economy already exists");

            for (Economy econ : Economy.getEconomies()) if (econ.getSymbol() == this.symbol) throw new UnsupportedOperationException("Symbol is taken");

            FileConfiguration config = NovaConfig.getEconomiesConfig();
            ConfigurationSection es = config.createSection(this.name.toLowerCase());

            Economy econ = new Economy(es.getName(), this.name, this.icon, this.symbol, this.increaseNaturally, this.conversionScale);
            econ.customModelData = modelData;

            es.set("economy", econ);
            es.set("last_saved_timestamp", System.currentTimeMillis());

            try {
                config.save(NovaConfig.getEconomiesFile());
            } catch (IOException e) {
                NovaConfig.getLogger().severe(e.getMessage());
            }

            NovaConfig.getConfiguration().reloadHooks();
            return econ;
        }
    }

    @Override
    public String toString() {
        return "Economy{" +
                "symbol=" + symbol +
                ", section='" + section + '\'' +
                ", name='" + name + '\'' +
                ", icon=" + icon +
                ", hasNaturalIncrease=" + hasNaturalIncrease +
                ", conversionScale=" + conversionScale +
                ", interestEnabled=" + interestEnabled +
                '}';
    }

    /**
     * Fetches the economy's symbol.
     * @return Symbol of Economy
     */
    public char getSymbol() {
        return this.symbol;
    }
}