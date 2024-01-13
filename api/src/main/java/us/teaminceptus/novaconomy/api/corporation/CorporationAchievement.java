package us.teaminceptus.novaconomy.api.corporation;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.Language;

import java.util.function.BiFunction;

/**
 * Represents an achievement that a Corporation can earn.
 */
public enum CorporationAchievement {

    /**
     * The achievement for reaching 5, 15, 35, and 50 corporation children.
     */
    MONOPOLY(Material.GOLD_INGOT, 4, "constants.corporation.achievement.monopoly", 25500, (c, level) -> {
        int size = c.getChildren().size();

        switch (level) {
            case 0: return size / 5D;
            case 1: return size / 15D;
            case 2: return size / 35D;
            case 3: return size / 50D;
            default: return 1D;
        }
    }),

    /**
     * The achievement for reaching a collective 10K, 50K, 150K, 500K, 1M, and 5M Business and Corporation Views.
     */
    ADVERTISER(Material.PAPER,6, "constants.corporation.achievement.advertiser", 42500, (c, level) -> {
        long views = c.getStatistics().getTotalViews();

        switch (level) {
            case 0: return views / 10_000D;
            case 1: return views / 50_000D;
            case 2: return views / 150_000D;
            case 3: return views / 500_000D;
            case 4: return views / 1_000_000D;
            case 5: return views / 5_000_000D;
            default: return 1D;
        }
    }),
    
    /**
     * The achievement for reaching a collective 5K, 15K, 100K, 350K, 750K, 1.5M, 5M, 10M, 25M, 50M, and 100M Business sales.
     */
    SELLER(Material.EMERALD, 11, "constants.corporation.achievement.seller", 35000, (c, level) -> {
        long sales = c.getStatistics().getTotalSales();

        switch (level) {
            case 0: return sales / 5_000D;
            case 1: return sales / 15_000D;
            case 2: return sales / 100_000D;
            case 3: return sales / 350_000D;
            case 4: return sales / 750_000D;
            case 5: return sales / 1_500_000D;
            case 6: return sales / 5_000_000D;
            case 7: return sales / 10_000_000D;
            case 8: return sales / 25_000_000D;
            case 9: return sales / 50_000_000D;
            case 10: return sales / 100_000_000D;
            default: return 1D;
        }
    }),

    /**
     * The achievement for reaching a collective 100K, 450K, 1M, 2.5M, 7.5M, 15M, 40M, 75M, and 125M in total Business Profit.
     */
    BUSINESSMAN(Material.DIAMOND_BLOCK, 9, "constants.corporation.achievement.businessman", 155000, (c, level) -> {
        double profit = c.getStatistics().getTotalProfit();

        switch (level) {
            case 0: return profit / 100_000D;
            case 1: return profit / 450_000D;
            case 2: return profit / 1_000_000D;
            case 3: return profit / 2_500_000D;
            case 4: return profit / 7_500_000D;
            case 5: return profit / 15_000_000D;
            case 6: return profit / 40_000_000D;
            case 7: return profit / 75_000_000D;
            case 8: return profit / 125_000_000D;
            default: return 1D;
        }
    }),

    /**
     * The achievement for reaching a collective 5K, 20K, 75K, 130K, 285K, and 610K in total Corporation Resources.
     */
    STOCKPILER(Material.CHEST, 6, "constants.corporation.achievement.stockpiler", 137500, (c, level) -> {
        int resources = c.getTotalResources();

        switch (level) {
            case 0: return resources / 5_000D;
            case 1: return resources / 20_000D;
            case 2: return resources / 75_000D;
            case 3: return resources / 130_000D;
            case 4: return resources / 285_000D;
            case 5: return resources / 610_000D;
            default: return 1D;
        }
    }),

    /**
     * The achievement for reaching an average rating of 4 stars or more on all of this Corporation's Children when they have at least 5 members.
     */
    SUPER_QUALITY(Material.EMERALD, 1, "constants.corporation.achievement.super_quality", 100000, (c, level) -> {
        if (c.getChildren().size() < 5) return 0D;
        if (c.getAverageRating() > 4D) return 1D;

        return c.getAverageRating() / 4D;
    }),

    /**
     * The achievement for reaching a collective 50, 150, or 250 Products.
     */
    TOO_MANY(Material.IRON_SWORD, 3, "constants.corporation.achievement.too_many", 75000, (c, level) -> {
        int products = c.getAllProducts().size();

        switch (level) {
            case 0: return products / 50D;
            case 1: return products / 150D;
            case 2: return products / 250D;
            default: return 1D;
        }
    }),
    
    ;

    private final int maxLevel;
    private final String key;
    private final double experienceReward;
    private final Material icon;
    private final BiFunction<Corporation, Integer, Double> progress;

    CorporationAchievement(Material icon, int maxLevel, String key, double expReward, BiFunction<Corporation, Integer, Double> progress) {
        this.maxLevel = maxLevel;
        this.key = key;
        this.experienceReward = expReward;
        this.icon = icon;
        this.progress = progress;
    }

    /**
     * Fetches this CorporationAchievement's Material Icon.
     * @return Material Icon
     */
    @NotNull
    public Material getIcon() {
        return icon;
    }

    /**
     * Fetches the maximum level of this Corporation Achievement.
     * @return Maximum Level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Fetches the display name for this Corporation Achievement.
     * @return Localized Display Name
     */
    @NotNull
    public String getDisplayName() { return Language.getCurrentMessage(key); }

    /**
     * Fetches teh localized description for this Corporation Achievement.
     * @return Localized Description
     */
    @NotNull
    public String getDescription() { return Language.getCurrentMessage(key + ".desc"); }

    /**
     * <p>Fetches the base experience reward for achieving this Corporation Achievement.</p>
     * <p>This will be multiplied by the level unlocked, when applicable.</p>
     * @return Experience Reward
     */
    public double getExperienceReward() {
        return experienceReward;
    }

    /**
     * Fetches the current progress percentage of the Corporation towards the next level of this achievement.
     * @param c Corporation to check
     * @return Percentage Progress towards Achievement out of 100%
     * @throws IllegalArgumentException if Corporation is null
     */
    public double getProgress(@NotNull Corporation c) throws IllegalArgumentException {
        if (c == null) throw new IllegalArgumentException("Corporation cannot be null.");
        return progress.apply(c, c.getAchievementLevel(this)) * 100;
    }
}
