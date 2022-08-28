package us.teaminceptus.novaconomy.api.economy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.io.File;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Novaconomy Stock Market
 */
public final class NovaMarket {

    private static final SecureRandom r = new SecureRandom();

    private static final File GLOBAL_FILE;
    
    private static final FileConfiguration GLOBAL;
    private static final ConfigurationSection MARKET_SECTION;
    
    private static final Map<Material, Long> SHARE_AMOUNT = new HashMap<>();
    private static final Map<Material, Double> SHARE_PRICE = new HashMap<>();
    
    private NovaMarket() { throw new UnsupportedOperationException("Do not instantiate!"); }
    
    static {
        GLOBAL_FILE = new File(NovaConfig.getDataFolder(), "global.yml");
        if (!GLOBAL_FILE.exists()) NovaConfig.getPlugin().saveResource("global.yml", false);
        GLOBAL = YamlConfiguration.loadConfiguration(GLOBAL_FILE);

        if (!GLOBAL.isConfigurationSection("NovaMarket")) GLOBAL.createSection("NovaMarket");
        MARKET_SECTION = GLOBAL.getConfigurationSection("NovaMarket");

        if (!MARKET_SECTION.isConfigurationSection("Shares")) MARKET_SECTION.createSection("Shares");
        for (String k : MARKET_SECTION.getConfigurationSection("Shares").getKeys(false)) {
            Material mat = Material.getMaterial(k);
            if (mat == null) {
                MARKET_SECTION.set("Shares." + k, null);
                continue;
            }
            SHARE_AMOUNT.put(mat, MARKET_SECTION.getLong(k));
        }

        if (!MARKET_SECTION.isConfigurationSection("Prices")) MARKET_SECTION.createSection("Prices");
        for (String k : MARKET_SECTION.getConfigurationSection("Prices").getKeys(false)) {
            Material mat = Material.getMaterial(k);
            if (mat == null) {
                MARKET_SECTION.set("Prices." + k, null);
                continue;
            }
            SHARE_PRICE.put(mat, MARKET_SECTION.getDouble(k));
        }

        save();
    }

    /**
     * Fetches the total amount of shares of a Material.
     * @param mat Material to fetch the amount of shares of
     * @return Amount of shares of the Materia, or 0 if not found
     */
    public static long getTotalShares(@NotNull Material mat) {
        return SHARE_AMOUNT.getOrDefault(mat, 0L);
    }

    /**
     * Fetches the share price of a Material.
     * @param mat Material to fetch the share price of
     * @param econ Optional economy to input conversion scale (1 is used if null)
     * @return Share price of the Material, or 0 if not found
     */
    public static double getSharePrice(@NotNull Material mat, @Nullable Economy econ) {
        double scale = econ == null ? 1 : econ.getConversionScale();
        return getSharePrice(mat, scale);
    }

    /**
     * Fetches the share price of a Material.
     * @param mat Material to fetch the share price of
     * @param conversionScale Conversion Scale to convert the share price to
     * @return Share price of the Material, or 0 if not found
     * @throws IllegalArgumentException if scale is not positive
     */
    public static double getSharePrice(@NotNull Material mat, double conversionScale) throws IllegalArgumentException {
        if (conversionScale <= 0) throw new IllegalArgumentException("Conversion Scale must be positive");
        return SHARE_PRICE.getOrDefault(mat, 0D) / conversionScale;
    }

    /**
     * Sets the amount of shares of a Material.
     * @param mat Material to set the amount of shares of
     * @param amount Amount to set the amount of shares to
     * @throws IllegalArgumentException if material is negative
     */
    public static void setAmount(@NotNull Material mat, long amount) throws IllegalArgumentException {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
        SHARE_AMOUNT.put(mat, amount);
        SHARE_PRICE.put(mat, amount + (double) r.nextInt(4));
        save();
    }

    /**
     * Adds an amount of shares to a Material.
     * @param mat Material to add the amount of shares to
     * @param add Amount to add to the amount of shares
     * @throws IllegalArgumentException if result becomes negative
     */
    public static void addAmount(@NotNull Material mat, long add) throws IllegalArgumentException {
        setAmount(mat, getTotalShares(mat) + add);
    }

    /**
     * Removes an amount of shares from a Material.
     * @param mat Material to remove the amount of shares from
     * @param remove Amount to remove from the amount of shares
     * @throws IllegalArgumentException if result becomes negative
     */
    public static void removeAmount(@NotNull Material mat, long remove) throws IllegalArgumentException {
        addAmount(mat, -remove);
    }

    private static void save() {
        for (Map.Entry<Material, Long> e : SHARE_AMOUNT.entrySet())
            MARKET_SECTION.set("Shares." + e.getKey().name(), e.getValue());

        for (Map.Entry<Material, Double> e : SHARE_PRICE.entrySet())
            MARKET_SECTION.set("Prices." + e.getKey().name(), e.getValue());

        try { GLOBAL.save(GLOBAL_FILE); } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

}
