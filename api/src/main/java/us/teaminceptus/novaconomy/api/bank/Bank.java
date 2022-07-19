package us.teaminceptus.novaconomy.api.bank;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents the Global Bank in Novaconomy that holds tax money in the server.
 */
public final class Bank {

    private static final Logger LGR = NovaConfig.getLogger();
    private static final FileConfiguration global;
    private static final File globalF;
    private static final ConfigurationSection bankSection;

    private Bank() { throw new UnsupportedOperationException("Do not instantiate!"); }

    static {
        globalF = new File(NovaConfig.getDataFolder(), "global.yml");
        if (!globalF.exists()) NovaConfig.getPlugin().saveResource("global.yml", false);
        global = YamlConfiguration.loadConfiguration(globalF);

        if (!global.isConfigurationSection("Bank")) global.createSection("Bank");
        bankSection = global.getConfigurationSection("Bank");

        for (Economy econ : Economy.getEconomies()) if (!bankSection.isSet(econ.getName())) bankSection.set(econ.getName(), 0);
    }

    /**
     * Fetches all of the balances in the Bank.
     * @return Map of all of the balances in the Bank to their values.
     */
    @NotNull
    public static Map<Economy, Double> getBalances() {
        Map<Economy, Double> bal = new HashMap<>();
        bankSection.getValues(false).forEach((k, v) -> bal.put(Economy.getEconomy(k), Double.parseDouble(v.toString())));
        return bal;
    }

    private static void save() {
        try { global.save(globalF); } catch (Exception e) {
            LGR.severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) LGR.severe(s.toString());
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
        bankSection.set(econ.getName(), amount);
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
     * Removes a balance from the Bank.
     * @param econ Economy to remove the balance from.
     * @param amount Amount to remove from the balance.
     * @throws IllegalArgumentException if economy is null
     */
    public static void removeBalance(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        setBalance(econ, getBalance(econ) - amount);
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
