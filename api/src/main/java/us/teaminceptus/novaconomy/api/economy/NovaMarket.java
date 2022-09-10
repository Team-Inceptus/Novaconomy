package us.teaminceptus.novaconomy.api.economy;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.io.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Novaconomy Stock Market
 * @deprecated Draft API
 */
@Deprecated
@SuppressWarnings("unchecked")
public final class NovaMarket implements Serializable {

    private static final SecureRandom r = new SecureRandom();

    private static final File MARKET_FILE;

    private static final Map<Material, Long> SHARE_AMOUNT = new HashMap<>();
    private static final Map<Material, Double> SHARE_PRICE = new HashMap<>();

    private NovaMarket() { throw new UnsupportedOperationException("Do not instantiate!"); }

    private static void save() {
        try {
            FileOutputStream fs = new FileOutputStream(MARKET_FILE);
            ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(fs));

            os.writeObject(SHARE_AMOUNT);
            os.writeObject(SHARE_PRICE);

            os.close();
        } catch (IOException e) {
            NovaConfig.print(e);
        }
    }

    static {
        MARKET_FILE = new File(NovaConfig.getDataFolder(), "novamarket.dat");

        try {
            if (MARKET_FILE.exists()) {
                FileInputStream fs = new FileInputStream(MARKET_FILE);
                ObjectInputStream os = new ObjectInputStream(new BufferedInputStream(fs));

                Map<Material, Long> shareAmount = (Map<Material, Long>) os.readObject();
                Map<Material, Double> sharePrice = (Map<Material, Double>) os.readObject();

                SHARE_AMOUNT.putAll(shareAmount);
                SHARE_PRICE.putAll(sharePrice);

                os.close();
            } else MARKET_FILE.createNewFile();
        } catch (IOException | ClassNotFoundException e) {
            NovaConfig.print(e);
        }
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

}
