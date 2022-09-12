package us.teaminceptus.novaconomy.api.bank;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Global Bank in Novaconomy that holds tax money in the server.
 */
public final class Bank {

    private static final FileConfiguration GLOBAL;
    private static final File GLOBAL_FILE;
    private static final ConfigurationSection BANK_SECTION;

    private Bank() { throw new UnsupportedOperationException("Do not instantiate!"); }

    static {
        GLOBAL_FILE = new File(NovaConfig.getDataFolder(), "global.yml");
        if (!GLOBAL_FILE.exists()) NovaConfig.getPlugin().saveResource("global.yml", false);
        GLOBAL = YamlConfiguration.loadConfiguration(GLOBAL_FILE);

        if (!GLOBAL.isConfigurationSection("Bank")) GLOBAL.createSection("Bank");
        BANK_SECTION = GLOBAL.getConfigurationSection("Bank");

        for (Economy econ : Economy.getEconomies()) if (!BANK_SECTION.isSet(econ.getName())) BANK_SECTION.set(econ.getName(), 0);
    }

    /**
     * Fetches all of the balances in the Bank.
     * @return Map of all of the balances in the Bank to their values.
     */
    @NotNull
    public static Map<Economy, Double> getBalances() {
        Map<Economy, Double> bal = new HashMap<>();
        BANK_SECTION.getValues(false).forEach((k, v) -> bal.put(Economy.getEconomy(k), Double.parseDouble(v.toString())));
        return bal;
    }

    private static void save() {
        try { GLOBAL.save(GLOBAL_FILE); } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    /**
     * Sets the Bank Balance of this Economy.
     * @param econ Economy to set the balance of.
     * @param amount Amount to set the balance to.
     * @throws IllegalArgumentException if econ is null
     */
    public static void setBalance(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        Preconditions.checkNotNull(econ, "Economy cannot be null");
        BANK_SECTION.set(econ.getName(), amount);
        save();
    }

    /**
     * Adds a balance to the Bank.
     * @param econ Economy to add the balance to.
     * @param amount Amount to add to the balance.
     * @throws IllegalArgumentException if economy is null
     */
    public static void addBalance(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        setBalance(econ, getBalance(econ) + amount);
    }

    /**
     * Adds a Price to the Bank.
     * @param price Price to add
     * @throws IllegalArgumentException if price is null
     */
    public static void addBalance(@NotNull Price price) throws IllegalArgumentException {
        if (price == null) throw new IllegalArgumentException("Price cannot be null");
        addBalance(price.getEconomy(), price.getAmount());
    }

    /**
     * Removes a balance from the Bank.
     * @param econ Economy to remove the balance from.
     * @param amount Amount to remove from the balance.
     * @throws IllegalArgumentException if economy is null
     */
    public static void removeBalance(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        setBalance(econ, getBalance(econ) - amount);
    }

    /**
     * Removes a Price from the Bank.
     * @param price Price to remove
     * @throws IllegalArgumentException if price is null
     */
    public static void removeBalance(@NotNull Price price) throws IllegalArgumentException {
        if (price == null) throw new IllegalArgumentException("Price cannot be null");
        removeBalance(price.getEconomy(), price.getAmount());
    }

    /**
     * Fetches the Bank Balance of this Economy.
     * @param econ Economy to fetch the balance of
     * @return Bank Balance, or 0 if economy is null
     */
    public static double getBalance(@NotNull Economy econ)  {
        if (econ == null) return 0;
        return getBalances().get(econ);
    }

}
