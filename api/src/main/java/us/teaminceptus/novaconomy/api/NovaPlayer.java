package us.teaminceptus.novaconomy.api;

import java.io.File;
import java.io.IOException;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.teaminceptus.novaconomy.api.economy.Economy;

/**
 * Class representing a Player used in the Plugin
 *
 */
public final class NovaPlayer {

    private final OfflinePlayer player;

    private final File playerFile;
    private final FileConfiguration playerConfig;

    /**
     * Creates a new Player.
     * @param p Player to use
     * @throws IllegalArgumentException if p is null
     */
    public NovaPlayer(OfflinePlayer p) throws IllegalArgumentException {
        if (p == null) throw new IllegalArgumentException("player is null");
        this.player = p;

        if (!(NovaConfig.getPlayerDirectory().exists())) {
            NovaConfig.getPlayerDirectory().mkdir();
        }

        this.playerFile = new File(NovaConfig.getPlayerDirectory(), p.getUniqueId() + ".yml");

        if (!(this.playerFile.exists())) {
            try {
                this.playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        reloadValues();
    }

    /**
     * Fetch the Configuration file this player's information is stored in
     * @return ({@link FileConfiguration} representing player config
     */
    public FileConfiguration getPlayerConfig() {
        return this.playerConfig;
    }

    /**
     * Fetch the player this class belongs to
     * @return OfflinePlayer of this object
     */
    public OfflinePlayer getPlayer() {
        return this.player;
    }

    /**
     * Fetch the online player this class belongs to
     * @return Player if online, else null
     */
    public Player getOnlinePlayer() {
        if (this.player.isOnline()) return this.player.getPlayer();
        else return null;
    }

    /**
     * Fetches the balance of this player
     * @param econ Economy to use
     * @return Player Balance
     * @throws IllegalArgumentException if economy is null
     */
    public double getBalance(Economy econ) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        return this.playerConfig.getConfigurationSection("economies").getConfigurationSection(econ.getName().toLowerCase()).getDouble("balance");
    }

    /**
     * Sets the balance of this player
     * @param econ Economy to use
     * @param newBal Amount to set
     * @throws IllegalArgumentException if economy is null
     */
    public void setBalance(Economy econ, double newBal) throws IllegalArgumentException {
        if (newBal < 0) throw new IllegalArgumentException("Balance cannot be negative");
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        this.playerConfig.getConfigurationSection("economies").getConfigurationSection(econ.getName().toLowerCase()).set("balance", newBal);

        try {
            this.playerConfig.save(this.playerFile);
        } catch (IOException e) {
            NovaConfig.getPlugin().getLogger().info("Error saving player file");
            e.printStackTrace();
        }
    }

    /**
     * Adds to the balance of this player
     * @param econ Economy to use
     * @param add Amount to add
     * @throws IllegalArgumentException if economy is null
     */
    public void add(Economy econ, double add) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        setBalance(econ, getBalance(econ) + add);
    }

    /**
     * Removes from the balance of this player
     * @param econ Economy to use
     * @param remove Amount to remove
     * @throws IllegalArgumentException if economy is null
     */
    public void remove(Economy econ, double remove) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        setBalance(econ, getBalance(econ) - remove);
    }

    private void reloadValues() {
        OfflinePlayer p = player;

        // General Info
        if (!(playerConfig.isString("name"))) {
            playerConfig.set("name", p.getName());
        }

        if (!(playerConfig.isBoolean("op"))) {
            playerConfig.set("op", p.isOp());
        }

        // Economies
        if (!(playerConfig.isConfigurationSection("economies"))) {
            playerConfig.createSection("economies");
        }

        ConfigurationSection economies = playerConfig.getConfigurationSection("economies");

        if (Economy.getEconomies().size() > 0)
            for (Economy e : Economy.getEconomies()) {
                if (!(economies.isConfigurationSection(e.getName().toLowerCase()))) {
                    economies.createSection(e.getName().toLowerCase());
                }

                ConfigurationSection econ = economies.getConfigurationSection(e.getName().toLowerCase());

                if (!(econ.isDouble("balance"))) {
                    econ.set("balance", 0D);
                }
            }
    }

}