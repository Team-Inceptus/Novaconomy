package us.teaminceptus.novaconomy.api.corporation;

import us.teaminceptus.novaconomy.api.Language;

/**
 * Represents an achievement that a Corporation can earn.
 */
public enum CorporationAchievement {

    /**
     * The achievement for reaching 5, 15, 35, and 50 corporation children.
     */
    MONOPOLY(4, "constants.corporation.achievement.monopoly", 5500),

    /**
     * The achievement for reaching a collective 10, 50K, 150K, 500K, 1M, and 5M Business and Corporation Views.
     */
    ADVERTISER(6, "constants.corporation.achievement.advertiser", 7800),
    
    /**
     * The achievement for recahing a collective 5K, 15K, 100K, 350K, and 750K Business sales.
     */
    SELLER(5, "constants.corporation.achievement.seller", 10500),

    /**
     * The achievement for reaching a collective 1M, 5M, 10M, 25M, 50M, and 100M in profit.
     */
    SUPER_SELLLER(5, "constants.corporation.achievement.super_seller", 30750),
    
    ;

    private final int maxLevel;
    private final String key;
    private final double experienceReward;

    CorporationAchievement(int maxLevel, String key, double expReward) {
        this.maxLevel = maxLevel;
        this.key = key;
        this.experienceReward = expReward;
    }

    /**
     * Fetches the maximum level of this Corporation Achievement.
     * @return Maximum Level
     */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Fetches the display name fo this Corporation Achievement.
     * @return Display Name
     */
    public String getDisplayName() {
        return Language.getCurrentLanguage().getMessage(key);
    }

    /**
     * <p>Fetches the base experience reward for achieving this Corporation Achievement.</p>
     * <p>This will be multiplied by the level unlocked, when applicable.</p>
     * @return Experience Reward
     */
    public double getExperienceReward() {
        return experienceReward;
    }
}
