package us.teaminceptus.novaconomy.api;

public interface NovaConfig {
	
	long getIntervalTicks();

	File getPlayerDirectory();

	void saveEconomiesFile();

	FileConfiguration getEconomiesFile();

	boolean isInterestEnabled();

	boolean hasMiningIncrease();

	boolean hasFishingIncrease();

	boolean hasKillIncrease();

	boolean hasDeathDecrease();

	boolean hasFarmingIncrease();

	double getInterestMultiplier();

	void setInterestMultiplier(double multiplier);
	
	int getMiningChance();

	int getFishingChance();

	int getKillChance();

	int getFarmingChance();

	void setKillChance(int chance);

	void setFishingChance(int chance);

	void setMiningChance(int chance);

	void setFarmingChance(int chance);	
	 
}