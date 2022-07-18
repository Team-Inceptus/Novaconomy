package us.teaminceptus.novaconomy.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.player.PlayerDepositEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerWithdrawlEvent;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.File;
import java.io.IOException;

/**
 * Class representing a Player used in the Plugin
 *
 */
public final class NovaPlayer {

    private static final String LBW = "last_bank_withdrawl";
    private static final String LBD = "last_bank_deposit";
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

        if (!(pFile.exists())) try {
            pFile.createNewFile();
        } catch (IOException e) {
            NovaConfig.getLogger().severe(e.getMessage());
        }

        pConfig = YamlConfiguration.loadConfiguration(pFile);

        reloadValues();
    }

    /**
     * Fetch the Configuration file this player's information is stored in
     * @return {@link FileConfiguration} representing player config
     */
    @NotNull
    public FileConfiguration getPlayerConfig() {
        return pConfig;
    }

    /**
     * Fetch the player this class belongs to
     * @return OfflinePlayer of this object
     */
    @NotNull
    public OfflinePlayer getPlayer() {
        return p;
    }

    /**
     * Fetch the online player this class belongs to
     * @return Player if online, else null
     */
    @Nullable
    public Player getOnlinePlayer() {
        if (p.isOnline()) return p.getPlayer();
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
        return pConfig.getConfigurationSection("economies").getConfigurationSection(econ.getName().toLowerCase()).getDouble("balance");
    }

    /**
     * Sets the balance of this player
     * @param econ Economy to use
     * @param newBal Amount to set
     * @throws IllegalArgumentException if economy is null
     */
    public void setBalance(@NotNull Economy econ, double newBal) throws IllegalArgumentException {
        if (newBal < 0) throw new IllegalArgumentException("Balance cannot be negative");
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        pConfig.set("economies." + econ.getName().toLowerCase() + ".balance", newBal);
    }
    
    private void save() {
        try {
            pConfig.save(pFile);
        } catch (IOException e) {
            NovaConfig.getLogger().info("Error saving player file");
            NovaConfig.getLogger().severe(e.getMessage());
        }
    }

    /**
     * Adds to the balance of this player
     * @param econ Economy to use
     * @param add Amount to add
     * @throws IllegalArgumentException if economy is null
     */
    public void add(@NotNull Economy econ, double add) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

        setBalance(econ, getBalance(econ) + add);
    }

    /**
     * Removes from the balance of this player
     * @param econ Economy to use
     * @param remove Amount to remove
     * @throws IllegalArgumentException if economy is null
     */
    public void remove(@NotNull Economy econ, double remove) throws IllegalArgumentException {
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
     * Fetches an Event Representation of the last bank withdrawl of this player
     * @return {@link PlayerWithdrawlEvent} representing last withdrawl; Player may be null if {@link #isOnline()} returns false
     */
    @NotNull
    public PlayerWithdrawlEvent getLastBankWithdrawl() {
        return new PlayerWithdrawlEvent(getOnlinePlayer(), pConfig.getDouble(LBW + ".amount"), Economy.getEconomy(pConfig.getString(LBW + ".economy")), pConfig.getLong(LBW + ".timestamp"));
    }

    /**
     * Fetches an Event Representation of the last bank deposit of this player
     * @return {@link PlayerDepositEvent} representing last deposit; Player may be null if {@link #isOnline()} returns false
     */
    @NotNull
    public PlayerDepositEvent getLastBankDeposit() {
        return new PlayerDepositEvent(getOnlinePlayer(), pConfig.getDouble(LBD + ".amount"), Economy.getEconomy(pConfig.getString(LBD + ".economy")), pConfig.getLong(LBD + ".timestamp"));
    }

    /**
     * Withdrawls an amount from the global bank
     * @param econ Economy to withdrawl from
     * @param amount Amount to withdrawl
     * @throws IllegalArgumentException if economy is null, or amount is negative or greater than the current bank balance
     * @throws UnsupportedOperationException if amount is greater than the max withdrawl or already withdrawn in the last 24 hours
     */
    public void withdrawl(@NotNull Economy econ, double amount) throws IllegalArgumentException, UnsupportedOperationException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount < 0 || amount > Bank.getBalance(econ)) throw new IllegalArgumentException("Amount cannot be negative or greater than current bank balance");

        if (!NovaConfig.getConfiguration().canBypassWithdrawl(p) && NovaConfig.getConfiguration().getMaxWithdrawlAmount(econ) < amount) throw new UnsupportedOperationException("Amount exceeds max withdrawl amount");
        if (System.currentTimeMillis() - 86400000 < pConfig.getLong(LBW + ".timestamp")) throw new UnsupportedOperationException("Last withdrawl was less than 24 hours ago");

        Bank.removeBalance(econ, amount);
        add(econ, amount);

        pConfig.set(LBW + ".amount", amount);
        pConfig.set(LBW + ".economy", econ.getName());
        pConfig.set(LBW + ".timestamp", System.currentTimeMillis());
        save();

        PlayerWithdrawlEvent event = new PlayerWithdrawlEvent(getOnlinePlayer(), amount, econ, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Deposits an amount to the global bank
     * @param econ Economy to deposit to
     * @param firstAmount Amount to deposit
     * @throws IllegalArgumentException if economy is null, or amount is negative
     */
    public void deposit(@NotNull Economy econ, double firstAmount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (firstAmount < 0) throw new IllegalArgumentException("Amount cannot be negative");

        PlayerDepositEvent event = new PlayerDepositEvent(getOnlinePlayer(), firstAmount, econ, System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(event);
        double amount = event.getAmount();

        Bank.addBalance(econ, amount);
        remove(econ, amount);

        pConfig.set(LBW + ".amount", amount);
        pConfig.set(LBW + ".economy", econ.getName());
        pConfig.set(LBW + ".timestamp", System.currentTimeMillis());
        save();
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

        if (!pConfig.isConfigurationSection(LBW)) pConfig.createSection(LBW);
        if (!pConfig.isLong(LBW + ".timestamp")) pConfig.set(LBW + ".timestamp", 0);
        if (!pConfig.isDouble(LBW + ".amount")) pConfig.set(LBW + ".amount", 0);
        if (!pConfig.isString(LBW + ".economy")) pConfig.set(LBW + ".economy", "");

        if (!pConfig.isConfigurationSection(LBD)) pConfig.createSection(LBD);
        if (!pConfig.isLong(LBD + ".timestamp")) pConfig.set(LBD + ".timestamp", 0);
        if (!pConfig.isDouble(LBD + ".amount")) pConfig.set(LBD + ".amount", 0);
        if (!pConfig.isString(LBD + ".economy")) pConfig.set(LBD + ".economy", "");

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