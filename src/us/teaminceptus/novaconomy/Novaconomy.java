package us.teaminceptus.novaconomy;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Novaconomy extends JavaPlugin {

	private static File playerDir;
	private static FileConfiguration economiesFile;

	public void onEnable() {
		saveDefaultConfig();
		saveConfig();
		playerDir = new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder().getPath() + "/players"); 	
		File economyFile = new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder(), "economies.yml");
		economiesFile = YamlConfiguration.loadConfiguration(economyFile);

		new Commands(this);
		new Events(this);

		// Config Checks
		FileConfiguration config = getConfig();

		// Natural Causes
		if (!(config.isConfigurationSection("NaturalCauses"))) {
			config.createSection("NaturalCauses");
		}

		ConfigurationSection naturalC = config.getConfigurationSection("NaturalCauses");

		if (!(naturalC.isBoolean("KillIncrease"))) {
			naturalC.set("KillIncrease", true);
		}

		if (!(naturalC.isBoolean("FishingIncrease"))) {
			naturalC.set("FishingIncrease", true);
		}

		if (!(naturalC.isInt("FishingIncreaseChance"))) {
			naturalC.set("FishingIncreaseChance", 70);
		}

		if (!(naturalC.isBoolean("MiningIncrease"))) {
			naturalC.set("MiningIncrease", true);
		}

		if (!(naturalC.isInt("MiningIncreaseChance"))) {
			naturalC.set("MiningIncreaseChance", 30);
		}

		if (!(naturalC.isBoolean("FarmingIncrease"))) {
			naturalC.set("FarmingIncrease", true);
		}

		if (!(naturalC.isInt("FarmingIncreaseChance"))) {
			naturalC.set("FarmingIncreaseChance", 40);
		}

		if (!(naturalC.isBoolean("DeathDecrease"))) {
			naturalC.set("DeathDecrease", true);
		}

		// Interest
		if (!(config.isConfigurationSection("Interest"))) {
			config.createSection("Interest");
		}

		ConfigurationSection interest = config.getConfigurationSection("Interest");

		if (!(interest.isBoolean("Enabled"))) {
			interest.set("Enabled", true);
		}

		if (!(interest.isLong("IntervalTicks"))) {
			interest.set("IntervalTicks", 1728000);
		}

		if (!(interest.isDouble("ValueMultiplier"))) {
			interest.set("ValueMultiplier", 1.02D);
		}

		saveConfig();
	}

	public static final File getPlayerDirectory() {
		return playerDir;
	}

	public static final FileConfiguration getEconomiesFile() {
		return economiesFile;
	}

	public static final void saveEconomiesFile() {
		File economyFile = new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder(), "economies.yml");
		
		try {
			getEconomiesFile().save(economyFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}