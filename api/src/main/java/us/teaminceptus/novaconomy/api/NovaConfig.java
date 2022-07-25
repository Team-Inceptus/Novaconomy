package us.teaminceptus.novaconomy.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
     * Reloads Novaconomy's Language Files.
     * @deprecated Languages are no longer stored in the plugin folder
     * @param replace if should replace files (defaults to false)
     */
    @Deprecated
    static void reloadLanguages(boolean replace) {
    }
    /**
     * Reloads Novaconomy's Language Files.
     * @deprecated Languages are no longer stored in the plugin folder
     */
    @Deprecated
    static void reloadLanguages() {
        reloadLanguages(false);
    }

    /**
     * Fetches the file of the main config.yml.
     * @return Configuration File
     */
    static File getConfigFile() { return new File(getDataFolder(), "config.yml"); }

    /**
     * Whether or not Notifications is turned on inside of the configuration.
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
     * @return Economies File
     */
    static FileConfiguration getEconomiesConfig() { return YamlConfiguration.loadConfiguration(getEconomiesFile()); }

    /**
     * Fetches the {@link File} related to ecnomies.yml.
     * @return File related to economies.yml
     */
    static File getEconomiesFile() { return new File(getDataFolder(), "economies.yml"); }

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
            getPlugin().getLogger().severe(e.getMessage());
        }
    }

    /**
     * Fetches the File that businesses.yml belongs to.
     * @return businesses.yml File
     */
    static File getBusinessFile() {
        File f = new File(getDataFolder(), "businesses.yml");
        if (!f.exists()) getPlugin().saveResource("businesses.yml", false);

        return f;
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
     * @return Businesses File
     */
    static FileConfiguration loadBusinesses() {
        return YamlConfiguration.loadConfiguration(getBusinessFile());
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

        if (!config.isConfigurationSection("Bounties")) config.createSection("Bounties");
        if (!config.isBoolean("Bounties.Enabled")) config.set("Bounties.Enabled", true);
        if (!config.isBoolean("Bounties.Broadcast")) config.set("Bounties.Broadcast", true);

        try { config.save(f); } catch (IOException e) { getPlugin().getLogger().severe(e.getMessage()); }

        return config;
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
     * @param increase Whether or not farming should increase money
     */
    void setFarmingIncrease(boolean increase);

    /**
     * Sets whether or not mining should increase money.
     * @param increase Whether or not mining should increase money
     */
    void setMiningIncrease(boolean increase);

    /**
     * Sets whether or not killing something should increase money.
     * @param increase Whether or not killing something should increase money
     */
    void setKillIncrease(boolean increase);

    /**
     * Sets whether or not dying should decrease money.
     * @param decrease Whether or not dying should decrease money
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
    boolean areBountiesEnabled();

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

}