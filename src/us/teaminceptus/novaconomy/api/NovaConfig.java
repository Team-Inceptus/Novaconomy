package us.teaminceptus.novaconomy.api;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

import us.teaminceptus.novaconomy.Novaconomy;

public interface NovaConfig {
	
	long getIntervalTicks();

	static File getPlayerDirectory() {
		return Novaconomy.getPlayerDirectory();
	}

	static void saveEconomiesFile() {
		Novaconomy.saveEconomiesFile();
	}

	static FileConfiguration getEconomiesFile() {
		return Novaconomy.getEconomiesFile();
	}

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
	
	void setFarmingIncrease(boolean increase);
	
	void setMiningIncrease(boolean increase);
	
	void setKillIncrease(boolean increase);
	
	void setDeathDecrease(boolean decrease);
	 
}