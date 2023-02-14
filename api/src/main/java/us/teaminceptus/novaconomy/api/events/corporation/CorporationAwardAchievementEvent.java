package us.teaminceptus.novaconomy.api.events.corporation;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationAchievement;

/**
 * Called before a Corporation earns an achievement.
 */
public class CorporationAwardAchievementEvent extends CorporationExperienceChangeEvent {

    private final CorporationAchievement achievement;
    private final int oldLevel;
    private int newLevel;

    /**
     * Constructs a CorporationAwardAchievementEvent.
     * @param c The Corporation earning the achievement
     * @param achievement The achievement being earned
     * @param oldLevel The Corporation's level before earning the achievement
     * @param newLevel The Corporation's level after earning the achievement
     */
    public CorporationAwardAchievementEvent(@NotNull Corporation c, @NotNull CorporationAchievement achievement, int oldLevel, int newLevel) {
        super(c, c.getExperience(), c.getExperience() + (achievement.getExperienceReward() * newLevel) );

        this.achievement = achievement;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    /**
     * Gets the achievement being earned.
     * @return Achievement being earned
     */
    @NotNull
    public CorporationAchievement getAchievement() {
        return achievement;
    }

    /**
     * Gets the Corporation's level before earning the achievement.
     * @return Corporation's level before earning the achievement
     */
    public int getOldLevel() {
        return oldLevel;
    }

    /**
     * Gets the Corporation's level after earning the achievement.
     * @return Corporation's level after earning the achievement
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * Sets the Corporation's level after earning the achievement.
     * @param newLevel Corporation's level after earning the achievement
     * @throws IllegalArgumentException if newLevel is greater than the achievement's max level, or is negative
     */
    public void setNewLevel(int newLevel) throws IllegalArgumentException {
        if (newLevel < 0 || newLevel > achievement.getMaxLevel()) throw new IllegalArgumentException("New Level cannot be negative or greater than: " + achievement.getMaxLevel());
        this.newLevel = newLevel;
    }

    /**
     * @deprecated Use {@link #setNewLevel(int)}.
     */
    @Override
    @Deprecated
    public final void setNewExperience(double newExperience) {
        throw new UnsupportedOperationException("Cannot set experience directly. Use setNewLevel instead.");
    }
}
