package us.teaminceptus.novaconomy.api;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

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

}