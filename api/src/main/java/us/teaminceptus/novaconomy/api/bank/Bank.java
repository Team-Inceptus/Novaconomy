package us.teaminceptus.novaconomy.api.bank;

import com.google.common.collect.ImmutableMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.Price;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the Global Bank in Novaconomy that holds tax money in the server.
 */
public final class Bank {

    private static final Map<Economy, Double> bankBalances = new HashMap<>();

    private Bank() { throw new UnsupportedOperationException("Do not instantiate!"); }

    static {
        read();
    }

    /**
     * Clears the Bank's cached values and reads the newly fresh values.
     */
    public static void reloadBank() {
        bankBalances.clear();
        read();
    }

    private static void read() {
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                readDB();
            } else
                readFile();
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private static void checkTable() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        db.createStatement().execute("CREATE TABLE IF NOT EXISTS bank (" +
                "economy CHAR(36) NOT NULL, " +
                "amount DOUBLE NOT NULL, " +
                "PRIMARY KEY (economy))");
    }

    private static void readDB() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();
        ResultSet rs = db.createStatement().executeQuery("SELECT * FROM bank");

        while (rs.next()) {
            Economy econ = Economy.byId(UUID.fromString(rs.getString("economy")));
            double amount = rs.getDouble("amount");

            bankBalances.put(econ, amount);
        }

        rs.close();
    }

    private static void readFile() {
        FileConfiguration global = NovaConfig.getGlobalStorage();
        if (!global.isConfigurationSection("Bank")) return;

        for (Economy econ : Economy.getEconomies())
            bankBalances.put(econ, global.getDouble("Bank." + econ.getName(), 0));
    }

    private static void write() {
        try {
            if (NovaConfig.getConfiguration().isDatabaseEnabled()) {
                checkTable();
                writeDB();
            } else
                writeFile();
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private static void writeDB() throws SQLException {
        Connection db = NovaConfig.getConfiguration().getDatabaseConnection();

        for (Economy econ : Economy.getEconomies()) {
            String sql;

            try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM bank WHERE economy = \"" + econ.getUniqueId() + "\"")) {
                if (rs.next())
                    sql = "UPDATE bank SET " +
                            "economy = ?, " +
                            "amount = ? " +
                            "WHERE economy = \"" + econ.getUniqueId() + "\"";
                else
                    sql = "INSERT INTO bank VALUES (?, ?)";
            }

            PreparedStatement ps = db.prepareStatement(sql);
            ps.setString(1, econ.getUniqueId().toString());
            ps.setDouble(2, bankBalances.getOrDefault(econ, 0.0D));

            ps.executeUpdate();
            ps.close();
        }
    }

    private static void writeFile() throws IOException {
        File globalF = new File(NovaConfig.getDataFolder(), "global.yml");
        if (!globalF.exists()) globalF.createNewFile();

        FileConfiguration global = NovaConfig.getGlobalStorage();
        if (!global.isConfigurationSection("Bank")) global.createSection("Bank");

        for (Economy econ : Economy.getEconomies())
            global.set("Bank." + econ.getName(), bankBalances.getOrDefault(econ, 0.0D));

        global.save(globalF);
    }

    /**
     * Fetches all of the balances in the Bank.
     * @return Map of all of the balances in the Bank to their values.
     */
    @NotNull
    public static Map<Economy, Double> getBalances() {
        return ImmutableMap.copyOf(bankBalances);
    }

    /**
     * Sets the Bank Balance of this Economy.
     * @param econ Economy to set the balance of.
     * @param amount Amount to set the balance to.
     * @throws IllegalArgumentException if econ is null
     */
    public static void setBalance(@NotNull Economy econ, double amount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");

        bankBalances.put(econ, amount);
        write();
    }

    /**
     * Sets a set of balances in the Bank.
     * @param amounts Map of Economies to their amounts.
     */
    public static void setBalances(@NotNull Map<Economy, Double> amounts) {
        if (amounts == null) throw new IllegalArgumentException("Amounts cannot be null");

        amounts.forEach((econ, amount) -> {
            if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
            if (amount == null || amount < 0) throw new IllegalArgumentException("Amount cannot be null or negative");

            bankBalances.put(econ, amount);
        });

        write();
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
        return getBalances().getOrDefault(econ, 0.0D);
    }

}
