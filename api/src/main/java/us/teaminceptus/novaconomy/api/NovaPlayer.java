package us.teaminceptus.novaconomy.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.File;
import java.io.IOException;

/**
 * Class representing a Player used in the Plugin
 *
 */
public final class NovaPlayer {

    private final OfflinePlayer p;

    private final File pFile;
    private final FileConfiguration pConfig;

    /**
     * Creates a new Player.
     * @param p Player to use
     * @throws IllegalArgumentException if p is null
     */
    public NovaPlayer(OfflinePlayer p) throws IllegalArgumentException {
        if (p == null) throw new IllegalArgumentException("player is null");
        this.p = p;

        if (!(NovaConfig.getPlayerDirectory().exists())) NovaConfig.getPlayerDirectory().mkdir();

        this.pFile = new File(NovaConfig.getPlayerDirectory(), p.getUniqueId() + ".yml");

        if (!(this.pFile.exists())) try {
            this.pFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.pConfig = YamlConfiguration.loadConfiguration(pFile);

        reloadValues();
    }

    /**
     * Fetch the Configuration file this player's information is stored in
     * @return ({@link FileConfiguration} representing player config
     */
    public FileConfiguration getPlayerConfig() {
        return this.pConfig;
    }

    /**
     * Fetch the player this class belongs to
     * @return OfflinePlayer of this object
     */
    public OfflinePlayer getPlayer() {
        return this.p;
    }

    /**
     * Fetch the online player this class belongs to
     * @return Player if online, else null
     */
    public Player getOnlinePlayer() {
        if (this.p.isOnline()) return this.p.getPlayer();
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
        return this.pConfig.getConfigurationSection("economies").getConfigurationSection(econ.getName().toLowerCase()).getDouble("balance");
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

        this.pConfig.set("economies." + econ.getName().toLowerCase() + ".balance", newBal);

        try {
            this.pConfig.save(this.pFile);
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

    /**
     * Whether this Nova Player can afford a Product.
     * @param p Product to buy
     * @return true if can afford, else false
     */
    public boolean canAfford(@Nullable Product p) {
        if (p == null) return false;
        return getBalance(p.getEconomy()) >= p.getPrice().getAmount();
    }

    /**
     * Whether this Nova Player is online.
     * @return true if online, else false
     */
    public boolean isOnline() {
        return p.isOnline();
    }

    private void reloadValues() {
        OfflinePlayer p = this.p;

        // General Info
        if (!pConfig.isString("name")) pConfig.set("name", p.getName());
        if (!pConfig.isBoolean("op")) pConfig.set("op", p.isOp());

        // Economies
        if (!(pConfig.isConfigurationSection("economies"))) pConfig.createSection("economies");

        ConfigurationSection economies = pConfig.getConfigurationSection("economies");

        if (Economy.getEconomies().size() > 0)
            for (Economy e : Economy.getEconomies()) {
                String path = e.getName().toLowerCase();
                if (!economies.isConfigurationSection(path)) economies.createSection(path);
                ConfigurationSection econ = economies.getConfigurationSection(path);

                if (!econ.isDouble("balance")) econ.set("balance", 0D);
            }
    }
}