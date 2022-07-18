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
     * @param replace if should replace files (defaults to false)
     */
    static void reloadLanguages(boolean replace) {
        for (Language l : Language.values()) {
            String fName = "novaconomy" + (l.getIdentifier().length() == 0 ? "" : "_" + l.getIdentifier()) + ".properties";
            File f = new File(getDataFolder(), fName);

            if (replace && f.exists()) f.delete();

            if (!f.exists()) getPlugin().saveResource(fName, false);
        }
    }
    /**
     * Reloads Novaconomy's Language Files.
     */
    static void reloadLanguages() {
        reloadLanguages(false);
    }

    /**
     * Whether or not Notifications is turned on inside of the configuration.
     * @return true if notifications, else false
     */
    boolean hasNotifications();

    /**
     * Fetch how often Interest is Applied.
     * @return how often interest is applied, in ticks
     */
    long getIntervalTicks();

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
     * Reloads the Interest Runnable with new values from the configuration.
     */
    static void reloadInterest() {
        Plugin plugin = getPlugin();
        Class<?> clazz = plugin.getClass();
        try {
            Method m = clazz.getDeclaredMethod("updateInterest");
            m.setAccessible(true);
            m.invoke(null);
        } catch (Exception e) {
            getPlugin().getLogger().severe(e.getMessage());
        }
    }

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
     * Loads the configuration's values.
     */
    static void loadConfig() {
        // Config Checks
        FileConfiguration config = getPlugin().getConfig();

        if (!config.isBoolean("Notifications")) config.set("Notifications", true);
        if (!config.isString("Language")) config.set("Language", "en");

        // Natural Causes
        if (!config.isConfigurationSection("NaturalCauses")) config.createSection("NaturalCauses");
        ConfigurationSection nc = config.getConfigurationSection("NaturalCauses");

        if (!nc.isBoolean("KillIncrease")) nc.set("KillIncrease", true);
        if (!nc.isInt("KillIncreaseChance")) nc.set("KillIncreaseChance", 100);

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

        if (!taxes.isList("Ignore")) config.set("Ignore", Arrays.asList("OPS"));

        if (!taxes.isConfigurationSection("MaxWithdrawl")) taxes.createSection("MaxWithdrawl");
        if (!taxes.isInt("MaxWithdraw.Global")) taxes.set("MaxWithdraw.Global", 100);
        if (!taxes.isList("MaxWithdraw.Bypass")) taxes.set("MaxWithdraw.Bypass", Arrays.asList("OPS"));
        for (Economy econ : Economy.getEconomies()) {
            String id = "MaxWithdraw." + econ.getName();
            if (taxes.isSet(id) && !taxes.isDouble(id)) taxes.set(id, null);
        }

        if (!taxes.isConfigurationSection("Automatic")) taxes.createSection("Automatic");
        if (!taxes.isLong("Automatic.Interval")) taxes.set("Automatic.Interval", 1728000);
        if (!taxes.isBoolean("Automatic.Enabled")) taxes.set("Automatic.Enabled", false);

        if (!taxes.isConfigurationSection("Minimums")) taxes.createSection("Minimums");
        if (!taxes.isDouble("Minimums.Global")) taxes.set("Minimums.Global", 0.0D);
        for (Economy econ : Economy.getEconomies()) {
            String id = "Minimums." + econ.getName();
            if (taxes.isSet(id) && !taxes.isDouble(id)) taxes.set(id, null);
        }

        getPlugin().saveConfig();
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
     * Fetches the maxmimum amount of money a player can withdrawl from the global bank on a daily basis.
     * @param econ Economy to test against
     * @return Maximum amount
     */
    double getMaxWithdrawlAmount(Economy econ);

    /**
     * Whether this OfflinePlayer can bypass the max withdrawl amount.
     * @param p Player to test against
     * @return true if they can, else false
     */
    boolean canBypassWithdrawl(OfflinePlayer p);

    /**
     * Whether this NovaPlayer can bypass the max withdrawl amount.
     * @param np NovaPlayer to test against
     * @return true if they can, else false
     */
    default boolean canBypassWithdrawl(NovaPlayer np) {
        return canBypassWithdrawl(np.getPlayer());
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
     * Fetches how often, in ticks, taxes should be automatically withdrawled from the player's balance.
     * @return Withdrawl interval
     */
    long getTaxesInterval();

    /**
     * Fetches the Minimum amount of money a player must be taxed.
     * @param econ Economy to test against
     * @return Minimum amount
     */
    double getMinimumPayment(Economy econ);

}