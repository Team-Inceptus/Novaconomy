package us.teaminceptus.novaconomy.api.corporation;

import us.teaminceptus.novaconomy.api.Language;

/**
 * Represents an achievement that a Corporation can earn.
 */
public enum CorporationAchievement {

    /**
     * The achievement for reaching 5, 15, 35, and 50 corporation children.
     */
    MONOPOLY(4, "constants.corporation.achievement.monopoly", 3500),

    
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
