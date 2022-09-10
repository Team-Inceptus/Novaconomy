package us.teaminceptus.novaconomy.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Configuration used for API
 */
public interface NovaConfig  {

    /**
     * Fetches this Configuration.
     * @return this configuration
     */
    static NovaConfig getConfiguration() {
        return (NovaConfig) getPlugin();
    }

    /**
     * Fetches the file of the main config.yml.
     * @return Configuration File
     */
    static File getConfigFile() { return new File(getDataFolder(), "config.yml"); }

    /**
     * Fetches the folder that all Businesses are stored.
     * @return Businesses Folder
     * @since 1.6.0
     */
    @NotNull
    static File getBusinessesFolder() { return new File(getDataFolder(), "businesses"); }

    /**
     * Prints a Throwable in the Plugin's Namespace and Format.
     * @param t Throwable to print
     * @since 1.6.0
     */
    static void print(@NotNull Throwable t) {
        getLogger().severe(t.getClass().getSimpleName());
        getLogger().severe("-----------");
        getLogger().severe(t.getMessage());
        for (StackTraceElement element : t.getStackTrace()) getLogger().severe(element.toString());
    }

    /**
     * Whether Notifications is turned on inside of the configuration.
     * @return true if notifications, else false
     */
    boolean hasNotifications();

    /**
     * Fetch how often Interest is Applied.
     * @return how often interest is applied, in ticks
     */
    long getInterestTicks();

    /**
     * Fetches the plugin instance.
     * @return Plugin Instance
     */
    static Plugin getPlugin() { return Bukkit.getPluginManager().getPlugin("Novaconomy"); }

    /**
     * Fetch the Player Directory
     * @return Player Directory
     */
    static File getPlayerDirectory() { return new File(getDataFolder(), "players"); }

    /**
     * Fetches the Data Folder of this Plugin.
     * @return Data Folder
     */
    static File getDataFolder() { return getPlugin().getDataFolder(); }

    /**
     * Fetch the Economies File
     * @deprecated Economies are no longer stored in a single file
     * @return Economies File
     */
    @Deprecated
    static FileConfiguration getEconomiesConfig() { return null; }

    /**
     * Fetches the {@link File} related to ecnomies.yml.
     * @deprecated Economies are no longer stored in a single file
     * @return File related to economies.yml
     */
    @Deprecated
    static File getEconomiesFile() { return new File(getDataFolder(), "economies.yml"); }

    /**
     * Fetches the folder that all Economies are stored in.
     * @return Economies Folder
     */
    static File getEconomiesFolder() { return new File(getDataFolder(), "economies"); }

    /**
     * Reloads the Interest and Taxes Runnables with new values from the configuration.
     */
    static void reloadRunnables() {
        Plugin plugin = getPlugin();
        Class<?> clazz = plugin.getClass();
        try {
            Method m = clazz.getDeclaredMethod("updateRunnables");
            m.setAccessible(true);
            m.invoke(null);
        } catch (Exception e) {
            print(e);
        }
    }

    /**
     * Fetches the File that businesses.yml belongs to.
     * @deprecated Businesses are now stored in individual files
     * @return businesses.yml File
     */
    @Deprecated
    static File getBusinessFile() {
        return new File(getDataFolder(), "businesses.yml");
    }

    /**
     * Fetches the Plugin's Logger.
     * @return Plugin Logger
     */
    static Logger getLogger() {
        return getPlugin().getLogger();
    }

    /**
     * Loads the businesses.yml file.
     * @deprecated Businesses are now stored in individual files
     * @return Businesses File
     */
    @Deprecated
    static FileConfiguration loadBusinesses() {
        return null;
    }

    /**
     * Fetches the Functionality Configuration File.
     * @return Functionality File
     */
    static File getFunctionalityFile() {
        return new File(getDataFolder(), "functionality.yml");
    }

    /**
     * Loads the Functionality Configuration.
     * @return Loaded Functionality Configuration
     */
    static FileConfiguration loadFunctionalityFile() {
        if (!getFunctionalityFile().exists()) getPlugin().saveResource("functionality.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(getFunctionalityFile());

        if (!config.isSet("CommandVersion")) config.set("CommandVersion", "auto");

        if (!config.isDouble("MaxConvertAmount")) config.set("MaxConvertAmount", -1);
        if (!config.isConfigurationSection("EconomyMaxConvertAmounts")) config.createSection("EconomyMaxConvertAmounts");

        try { config.save(getFunctionalityFile()); } catch (IOException e) { getPlugin().getLogger().severe(e.getMessage()); }

        return config;
    }

    /**
     * Loads the global storage configuration.
     * @return Global Storage Configuration
     */
    static FileConfiguration getGlobalStorage() { return YamlConfiguration.loadConfiguration(new File(getDataFolder(), "global.yml")); }

    /**
     * Loads the configuration's values.
     * @return Loaded FileConfiguration
     */
    static FileConfiguration loadConfig() {
        // Config Checks
        Plugin p = getPlugin();
        File f = getConfigFile();
        if (!f.exists()) p.saveDefaultConfig();

        FileConfiguration config = YamlConfiguration.loadConfiguration(f);

        if (!config.isBoolean("Notifications")) config.set("Notifications", true);
        if (!config.isString("Language")) config.set("Language", "en");

        // Natural Causes
        if (!config.isConfigurationSection("NaturalCauses")) config.createSection("NaturalCauses");
        ConfigurationSection nc = config.getConfigurationSection("NaturalCauses");

        if (!nc.isList("Ignore")) nc.set("Ignore", new ArrayList<>());
        if (!nc.isDouble("MaxIncrease") && !nc.isInt("MaxIncrease")) nc.set("MaxIncrease", 1000.0D);
        if (!nc.isBoolean("EnchantBonus")) nc.set("EnchantBonus", true);

        if (!nc.isBoolean("KillIncrease")) nc.set("KillIncrease", true);
        if (!nc.isInt("KillIncreaseChance")) nc.set("KillIncreaseChance", 100);
        if (!nc.isBoolean("KillIncreaseIndirect")) nc.set("KillIncreaseIndirect", true);

        if (!nc.isBoolean("FishingIncrease")) nc.set("FishingIncrease", true);
        if (!nc.isInt("FishingIncreaseChance")) nc.set("FishingIncreaseChance", 70);

        if (!nc.isBoolean("MiningIncrease")) nc.set("MiningIncrease", true);
        if (!nc.isInt("MiningIncreaseChance")) nc.set("MiningIncreaseChance", 30);

        if (!nc.isBoolean("FarmingIncrease")) nc.set("FarmingIncrease", true);
        if (!nc.isInt("FarmingIncreaseChance")) nc.set("FarmingIncreaseChance", 40);

        if (!nc.isBoolean("DeathDecrease")) nc.set("DeathDecrease", true);
        if (!nc.isDouble("DeathDivider") && !(nc.isInt("DeathDivider"))) nc.set("DeathDivider", 2);

        if (!nc.isConfigurationSection("Modifiers")) nc.createSection("Modifiers");
        if (!nc.isConfigurationSection("Modifiers.Killing")) nc.createSection("Modifiers.Killing");
        if (!nc.isConfigurationSection("Modifiers.Fishing")) nc.createSection("Modifiers.Fishing");
        if (!nc.isConfigurationSection("Modifiers.Mining")) nc.createSection("Modifiers.Mining");
        if (!nc.isConfigurationSection("Modifiers.Farming")) nc.createSection("Modifiers.Farming");
        if (!nc.isConfigurationSection("Modifiers.Death")) nc.createSection("Modifiers.Death");

        // Interest
        if (!config.isConfigurationSection("Interest")) config.createSection("Interest");
        if (!config.isBoolean("Interest.Enabled")) config.set("Interest.Enabled", true);
        if (!config.isInt("Interest.IntervalTicks") && !(config.isLong("Interest.IntervalTicks"))) config.set("Interest.IntervalTicks", 1728000);
        if (!config.isDouble("Interest.ValueMultiplier") && !(config.isInt("Interest.ValueMultiplier"))) config.set("Interest.ValueMultiplier", 1.03D);
        
        // Taxes
        if (!config.isConfigurationSection("Taxes")) config.createSection("Taxes");
        ConfigurationSection taxes = config.getConfigurationSection("Taxes");

        if (!taxes.isList("Ignore")) taxes.set("Ignore", Arrays.asList("OPS"));
        if (!taxes.isBoolean("Online")) taxes.set("Online", false);

        if (!taxes.isConfigurationSection("MaxWithdraw")) taxes.createSection("MaxWithdraw");
        if (!taxes.isInt("MaxWithdraw.Global")) taxes.set("MaxWithdraw.Global", 100);
        if (!taxes.isList("MaxWithdraw.Bypass")) taxes.set("MaxWithdraw.Bypass", Arrays.asList("OPS"));
        for (Economy econ : Economy.getEconomies()) {
            String id = "MaxWithdraw." + econ.getName();
            if (taxes.isSet(id) && !taxes.isDouble(id)) taxes.set(id, null);
        }

        if (!taxes.isConfigurationSection("Automatic")) taxes.createSection("Automatic");
        if (!taxes.isLong("Automatic.Interval") && !taxes.isInt("Automatic.Interval")) taxes.set("Automatic.Interval", 1728000);
        if (!taxes.isBoolean("Automatic.Enabled")) taxes.set("Automatic.Enabled", false);
        if (!taxes.isList("Automatic.Ignore")) taxes.set("Automatic.Ignore", new ArrayList<>());

        if (!taxes.isConfigurationSection("Minimums")) taxes.createSection("Minimums");
        if (!taxes.isDouble("Minimums.Global") && !taxes.isInt("Minimums.Global")) taxes.set("Minimums.Global", 0.0D);
        for (Economy econ : Economy.getEconomies()) {
            String id = "Minimums." + econ.getName();
            if (taxes.isSet(id) && !taxes.isDouble(id) && !taxes.isInt(id)) taxes.set(id, null);
        }

        if (!taxes.isConfigurationSection("Events")) taxes.createSection("Events");

        if (!config.isConfigurationSection("Bounties")) config.createSection("Bounties");
        if (!config.isBoolean("Bounties.Enabled")) config.set("Bounties.Enabled", true);
        if (!config.isBoolean("Bounties.Broadcast")) config.set("Bounties.Broadcast", true);

        if (!config.isConfigurationSection("Business")) config.createSection("Business");

        if (!config.isConfigurationSection("Business.Advertising")) config.createSection("Business.Advertising");
        if (!config.isBoolean("Business.Advertising.Enabled")) config.set("Business.Advertising.Enabled", true);
        if (!config.isDouble("Business.Advertising.ClickReward") && !config.isInt("Business.Advertising.ClickReward")) config.set("Business.Advertising.ClickReward", 5D);

        try { config.save(f); } catch (IOException e) { getPlugin().getLogger().severe(e.getMessage()); }

        return config;
    }

    /**
     * Represents a Custom Tax Event in the configuration
     */
    final class CustomTaxEvent {

        private final String identifier;
        private String name;
        private String permission;
        private boolean online;
        private List<Price> prices;
        private boolean usingIgnore;
        private String message;
        private boolean depositing;

        private List<String> ignore;

        /**
         * Constructs a CustomTaxEvent.
         * @param identifier The identifier of the event as the key in the configuration.
         * @param name The name of the event.
         * @param prices Prices that will be deducted from the player.
         * @param permission The permission required to call this event.
         * @param message The message that will be sent to the player.
         * @param usingIgnore Whether the event is including the default ignore list.
         * @param ignore The list of Permissions, Players or Groups that are not affected.
         * @param online Whether the event is only called when the player is online.
         * @param deposit Whether the event will deposit into the global bank
         */
        public CustomTaxEvent(String identifier, String name, List<Price> prices, String permission, String message, boolean usingIgnore, List<String> ignore, boolean online, boolean deposit) {
            this.identifier = identifier;
            this.name = name;
            this.prices = prices;
            this.permission = permission;
            this.usingIgnore = usingIgnore;
            this.ignore = ignore;
            this.online = online;
            this.message = message;
            this.depositing = deposit;
        }

        /**
         * Fetches the identifier of the event stored in the configuration.
         * @return Event ID
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         * Gets the name of the event.
         * @return The name of the event.
         */
        @NotNull
        public String getName() {
            return name;
        }

        /**
         * Gets the permission required to call this event.
         * @return The permission required to call this event.
         */
        @NotNull
        public String getPermission() {
            return permission;
        }

        /**
         * Whether the Player has to be online to be affected.
         * @return true if online mode, else false
         */
        public boolean isOnline() {
            return online;
        }

        /**
         * Gets a list of Prices that the player will be deducted from.
         * @return Prices that the player will pay
         */
        @NotNull
        public List<Price> getPrices() {
            return prices;
        }

        /**
         * Whether this Event is including the default event list in {@link #getIgnoring()}.
         * @return true if using ignore, else false
         */
        public boolean isUsingIgnore() {
            return usingIgnore;
        }

        /**
         * Gets all of the Permissions, Players or Groups that are not affected.
         * @return A list of Permissions, Players or Groups that are not affected.
         */
        @NotNull
        public List<String> getIgnoring() {
            return ignore;
        }

        /**
         * Fetches the message sent to the player.
         * @return The message sent to the player.
         */
        @NotNull
        public String getMessage() {
            return message;
        }

        /**
         * Sets the message sent to the player.
         * @param message Message sent
         * @throws IllegalArgumentException if message is null
         */
        public void setMessage(@NotNull String message) throws IllegalArgumentException {
            if (message == null) throw new IllegalArgumentException("Message cannot be null");
            this.message = message;
        }

        /**
         * Sets the name of this event.
         * @param name Name of event
         * @throws IllegalArgumentException if name is null
         */
        public void setName(@NotNull String name) throws IllegalArgumentException {
            if (name == null) throw new IllegalArgumentException("Name cannot be null");
            this.name = name;
        }

        /**
         * Sets the permission required to call this event.
         * @param permission Permission required to call this event.
         * @throws IllegalArgumentException if permission is null
         */
        public void setPermission(String permission) throws IllegalArgumentException {
            if (permission == null) throw new IllegalArgumentException("Permission cannot be null");
            this.permission = permission;
        }

        /**
         * Sets whether the Player has to be online to be affected.
         * @param online true if online mode, else false
         */
        public void setOnline(boolean online) {
            this.online = online;
        }

        /**
         * Sets the list of Prices that the player will be deducted from.
         * @param prices Prices that the player will pay
         * @throws IllegalArgumentException if price collection is null
         */
        public void setPrices(@NotNull Collection<? extends Price> prices) throws IllegalArgumentException {
            if (prices == null) throw new IllegalArgumentException("Prices cannot be null");
            this.prices = new ArrayList<>(prices);
        }

        /**
         * Sets whether this Event is including the default event list in {@link #getIgnoring()}.
         * @param usingIgnore true if using ignore, else false
         */
        public void setUsingIgnore(boolean usingIgnore) {
            this.usingIgnore = usingIgnore;
        }

        /**
         * Sets the list of Permissions, Players or Groups that are not affected.
         * @param ignore A Collection of Permissions, Players or Groups that are not affected.
         * @throws IllegalArgumentException if ignore collection is null
         */
        public void setIgnore(@NotNull Collection<? extends String> ignore) throws IllegalArgumentException {
            if (ignore == null) throw new IllegalArgumentException("Ignore List cannot be null");
            this.ignore = new ArrayList<>(ignore);
        }

        /**
         * Whether this Event is depositing into the global bank.
         * @return true if depositing, else false
         */
        public boolean isDepositing() {
            return depositing;
        }

        /**
         * Sets whether this Event is depositing into the global bank.
         * @param deposit true if depositing, else false
         */
        public void setDepositing(boolean deposit) {
            this.depositing = deposit;
        }
    }

    // Impl

    /**
     * Fetches the current language set.
     * @return Langauge set
     */
    String getLanguage();

    /**
     * Fetch if Interest is Enabled
     * @return true if enabled, else false
     */
    boolean isInterestEnabled();

    /**
     * Fetch if Mining Increase is enabled.
     * @return true if enabled, else false
     */
    boolean hasMiningIncrease();

    /**
     * Fetch if Fishing Increase is enabled.
     * @return true if enabled, else false
     */
    boolean hasFishingIncrease();

    /**
     * Fetch if Killing Increase is enabled.
     * @return true if enabled, else false
     */
    boolean hasKillIncrease();

    /**
     * Fetch if Death Decrease is enabled.
     * @return true if enabled, else false
     */
    boolean hasDeathDecrease();

    /**
     * Fetch is Farming Increase is enabled.
     * @return true if enabled, else false
     */
    boolean hasFarmingIncrease();

    /**
     * Fetch the Interest Multiplier
     * @return Multiplier applied when using interest
     */
    double getInterestMultiplier();

    /**
     * Sets the interest multiplier
     * @param multiplier New Interest Multiplier
     */
    void setInterestMultiplier(double multiplier);

    /**
     * Fetch the mining chance of increase
     * @return Chance of mining increase
     */
    int getMiningChance();

    /**
     * Fetch the fishing chance of increase
     * @return Chance of fishing increase
     */
    int getFishingChance();

    /**
     * Fetch the killing chance of increase
     * @return Chance of killing increase
     */
    int getKillChance();

    /**
     * Fetch the farming chance of increase
     * @return Chance of farming increase
     */
    int getFarmingChance();

    /**
     * Sets the chance of killing something increasing your money.
     * @param chance New Chance
     */
    void setKillChance(int chance);

    /**
     * Sets the chance of successfully fishing something to increase your money.
     * @param chance New Chance
     */
    void setFishingChance(int chance);

    /**
     * Sets the chacne of mining something to increase your money.
     * @param chance New Chance
     */
    void setMiningChance(int chance);

    /**
     * Sets the chance of farming something to increase your money.
     * @param chance New Chance
     */
    void setFarmingChance(int chance);

    /**
     * Sets whether or not farming should increase money.
     * @param increase Whether farming should increase money
     */
    void setFarmingIncrease(boolean increase);

    /**
     * Sets whether or not mining should increase money.
     * @param increase Whether mining should increase money
     */
    void setMiningIncrease(boolean increase);

    /**
     * Sets whether or not killing something should increase money.
     * @param increase Whether killing something should increase money
     */
    void setKillIncrease(boolean increase);

    /**
     * Sets whether or not dying should decrease money.
     * @param decrease Whether dying should decrease money
     */
    void setDeathDecrease(boolean decrease);

    /**
     * Fetches the Divider used when removing money in a DeathDecrease.
     * @return Divider Used
     * @see NovaConfig#setDeathDivider(double)
     */
    double getDeathDivider();

    /**
     * Sets the divider of the Death Event.
     * <p>
     * When a Player dies, the plugin will remove the balance divided by this value.
     * @param divider Divider to use
     */
    void setDeathDivider(double divider);

    /**
     * Sets whether or not interest is enabled.
     * @param enabled true if enabled, else false
     */
    void setInterestEnabled(boolean enabled);

    /**
     * Fetches the maximum amount of money a player can convert FROM.
     * <br><br>
     * This value does not control how much money a player will receive after converting.
     * @param econ Economy to test against
     * @return Maximum amount
     */
    double getMaxConvertAmount(Economy econ);

    /**
     * Reloads API Hooks (Placeholders, Vault, etc.).
     */
    void reloadHooks();

    /**
     * Fetches the maxmimum amount of money a player can withdraw from the global bank on a daily basis.
     * @param econ Economy to test against
     * @return Maximum amount
     */
    double getMaxWithdrawAmount(Economy econ);

    /**
     * Whether this OfflinePlayer can bypass the max withdraw amount.
     * @param p Player to test against
     * @return true if they can, else false
     */
    boolean canBypassWithdraw(OfflinePlayer p);

    /**
     * Whether this NovaPlayer can bypass the max withdraw amount.
     * @param np NovaPlayer to test against
     * @return true if they can, else false
     */
    default boolean canBypassWithdraw(NovaPlayer np) {
        return canBypassWithdraw(np.getPlayer());
    }

    /**
     * Whether this OfflinePlayer does not automatically pay taxes.
     * @param p Player to test against
     * @return true if they can, else false
     */
    boolean canIgnoreTaxes(OfflinePlayer p);

    /**
     * Whether this NovaPlayer does not automatically pay taxes.
     * @param np NovaPlayer to test against
     * @return true if they can, else false
     */
    default boolean canIgnoreTaxes(NovaPlayer np) {
        return canIgnoreTaxes(np.getPlayer());
    }

    /**
     * Whether automatic tax payments are enabled.
     * @return true if enabled, else false
     */
    boolean hasAutomaticTaxes();

    /**
     * Fetches how often, in ticks, taxes should be automatically withdrawed from the player's balance.
     * @return Withdraw interval
     */
    long getTaxesTicks();

    /**
     * Fetches the Minimum amount of money a player must be taxed.
     * @param econ Economy to test against
     * @return Minimum amount
     */
    double getMinimumPayment(Economy econ);

    /**
     * Whether Players have to be online for taxes to be paid.
     * @return true if they must be online, else false
     */
    boolean hasOnlineTaxes();

    /**
     * Sets whether Players have to be online for taxes to be paid.
     * @param enabled true if they must be online, else false
     */
    void setOnlineTaxes(boolean enabled);

    /**
     * Whether custom tax events are enabled.
     * @return true if enabled, else false
     */
    boolean hasCustomTaxes();

    /**
     * Sets whether custom tax events are enabled.
     * @param enabled true if enabled, else false
     */
    void setCustomTaxes(boolean enabled);

    /**
     * Fetches the maximum amount that can be gained from a natural cause.
     * @return Maximum Amount
     */
    double getMaxIncrease();

    /**
     * Sets the maximum amount that can be gained from a natural cause.
     * @param max Maximum Amount
     */
    void setMaxIncrease(double max);

    /**
     * Whether Enchantments amplify natural causes.
     * @return true if enabled, else false
     */
    boolean hasEnchantBonus();

    /**
     * Sets whether Enchantments amplify natural causes.
     * @param enabled true if enabled, else false
     */
    void setEnchantBonus(boolean enabled);

    /**
     * Whether Bounties are enabled.
     * @return true if enabled, else false
     */
    boolean hasBounties();

    /**
     * Sets whether Bounties are enabled.
     * @param enabled true if enabled, else false
     */
    void setBountiesEnabled(boolean enabled);

    /**
     * Whether Bounties are being broadcated.
     * @return true if broadcasted, else false
     */
    boolean isBroadcastingBounties();

    /**
     * Sets whether Bounties are being broadcasted.
     * @param broadcast true if broadcasted, else false
     */
    void setBroadcastingBounties(boolean broadcast);

    /**
     * Fetches all of the Custom Tax Events in the configuration.
     * @return Custom Tax Events
     */
    @NotNull
    Set<CustomTaxEvent> getAllCustomEvents();

    /**
     * Tests if this Player is ignored
     * @param p Player to test against
     * @param event Optional CustomTaxEvent to test against
     * @return true if they are ignored, else false
     */
    boolean isIgnoredTax(@NotNull OfflinePlayer p, @Nullable CustomTaxEvent event);

    /**
     * Tests if this Player is ignored
     * @param p Player to test against
     * @return true if they are ignored, else false
     */
    default boolean isIgnoredTax(OfflinePlayer p) {
        return isIgnoredTax(p, null);
    }

    /**
     * Whether the Stock Market is currently enabled.
     * @return true if enabled, else false
     */
    boolean isMarketEnabled();

    /**
     * Sets whether the Stock Market is currently enabled.
     * @param enabled true if enabled, else false
     */
    void setMarketEnabled(boolean enabled);

    /**
     * Fetches the Market Tax percentage.
     * @return Market Tax percentage
     */
    double getMarketTax();

    /**
     * Sets the Market Tax percentage.
     * @param tax Market Tax percentage
     * @throws IllegalArgumentException if tax is 0 or less
     */
    void setMarketTax(double tax) throws IllegalArgumentException;

    /**
     * Fetches whether business advertising is enabled.
     * @return true if enabled, else false
     */
    boolean isAdvertisingEnabled();

    /**
     * Sets whether business advertising is enabled.
     * @param enabled true if enabled, else false
     */
    void setAdvertisingEnabled(boolean enabled);

    /**
     * Fetches the advertising reward for clicking on a business's icon.
     * @return Advertising reward
     */
    double getBusinessAdvertisingReward();

    /**
     * Sets the advertising reward for clicking on a business's icon.
     * @param reward Advertising reward
     */
    void setBusinessAdvertisingReward(double reward);

    /**
     * Sets the current language used by the plugin.
     * @param language Language to use
     * @throws IllegalArgumentException if language is null
     */
    void setLanguage(@NotNull Language language) throws IllegalArgumentException;

}