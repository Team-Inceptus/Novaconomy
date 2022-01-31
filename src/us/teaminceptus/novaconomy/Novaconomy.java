package us.teaminceptus.novaconomy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.InterestEvent;

public class Novaconomy extends JavaPlugin implements NovaConfig {
	
	private static File playerDir;
	private static FileConfiguration economiesFile;
	
	private static FileConfiguration CONFIG;
	private static final ConfigurationSection INTEREST = CONFIG.getConfigurationSection("Interest");
	private static final ConfigurationSection NATURAL_CAUSES = CONFIG.getConfigurationSection("NaturalCauses");

	public static void sendPluginMessage(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "Novaconomy" + ChatColor.YELLOW + "] " + ChatColor.DARK_AQUA + message);
	}

	public static void sendError(CommandSender sender, String error) {
		sendPluginMessage(sender, ChatColor.RED + error);
	}
	
	protected static BukkitRunnable INTEREST_RUNNABLE = new BukkitRunnable() {
		public void run() {
			if (!(Novaconomy.getConfiguration().isInterestEnabled())) cancel();
			
			Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
			Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();
			
			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
				NovaPlayer np = new NovaPlayer(p);
				
				Map<Economy, Double> previousBal = new HashMap<>();
				Map<Economy, Double> amount = new HashMap<>();
				for (Economy econ : Economy.getInterestEconomies()) {
					double balance = np.getBalance(econ);
					double add = balance * Novaconomy.getConfiguration().getInterestMultiplier();
					
					previousBal.put(econ, balance);
					amount.put(econ, add);
				}
				
				previousBals.put(np, previousBal);
				amounts.put(np, amount);
			}
			
			InterestEvent event = new InterestEvent(previousBals, amounts);
			Bukkit.getPluginManager().callEvent(event);
			if (!(event.isCancelled())) {
				for (NovaPlayer np : previousBals.keySet()) {
					int i = 0;
					for (Economy econ : previousBals.get(np).keySet()) {
						np.add(econ, amounts.get(np).get(econ));
						i++;
					}
					
					if (np.getPlayer().isOnline()) {
						np.getOnlinePlayer().sendMessage(ChatColor.GREEN + "You have gained interest from " + ChatColor.GOLD + Integer.toString(i) + ChatColor.GREEN + (i == 1 ? "economy" : "economies") + "!");
					}
				}
			}
			
			this.runTaskLater(JavaPlugin.getPlugin(Novaconomy.class), Novaconomy.getConfiguration().getIntervalTicks());
		}
		
	};
	
	public void onEnable() {
		playerDir = new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder().getPath() + "/players"); 	
		File economyFile = new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder(), "economies.yml");
		try {
			if (!(economyFile.exists())) economyFile.createNewFile();
			if (!(playerDir.exists())) playerDir.mkdir();
		} catch (IOException e) {
			getLogger().info("Error loading files & folders");
			e.printStackTrace();
		}
		economiesFile = YamlConfiguration.loadConfiguration(economyFile);
		CONFIG = this.getConfig();

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
		
		if (!(naturalC.isInt("KillIncreaseChance"))) {
			naturalC.set("KillIncreaseChance", "100");
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
			interest.set("ValueMultiplier", 1.03D);
		}
		
		INTEREST_RUNNABLE.runTask(this);
		saveConfig();
	}
	
	
	

	public static final NovaConfig getConfiguration() {
		return JavaPlugin.getPlugin(Novaconomy.class);
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
			economiesFile.save(economyFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public long getIntervalTicks() {
		return INTEREST.getLong("IntervalTicks");
	}

	@Override
	public boolean isInterestEnabled() {
		return INTEREST.getBoolean("Enabled");
	}

	@Override
	public boolean hasMiningIncrease() {
		return NATURAL_CAUSES.getBoolean("MiningIncrease");
	}

	@Override
	public boolean hasFishingIncrease() {
		return NATURAL_CAUSES.getBoolean("FishingIncrease");
	}

	@Override
	public boolean hasKillIncrease() {
		return NATURAL_CAUSES.getBoolean("KillIncrease");
	}

	@Override
	public boolean hasDeathDecrease() {
		return NATURAL_CAUSES.getBoolean("DeathDecrease");
	}

	@Override
	public boolean hasFarmingIncrease() {
		return NATURAL_CAUSES.getBoolean("FarmingIncrease");
	}

	@Override
	public double getInterestMultiplier() {
		return INTEREST.getDouble("ValueMultiplier");
	}

	@Override
	public void setInterestMultiplier(double multiplier) {
		INTEREST.set("ValueMultiplier", multiplier);
		saveConfig();
	}

	@Override
	public int getMiningChance() {
		return NATURAL_CAUSES.getInt("MiningIncreaseChance");
	}

	@Override
	public int getFishingChance() {
		return NATURAL_CAUSES.getInt("FishingIncreaseChance");
	}

	@Override
	public int getKillChance() {
		return NATURAL_CAUSES.getInt("KillIncreaseChance");
	}

	@Override
	public int getFarmingChance() {
		return NATURAL_CAUSES.getInt("FarmingIncreaseChance");
	}

	@Override
	public void setKillChance(int chance) {
		NATURAL_CAUSES.set("KillIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setFishingChance(int chance) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMiningChance(int chance) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFarmingChance(int chance) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFarmingIncrease(boolean increase) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMiningIncrease(boolean increase) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setKillIncrease(boolean increase) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDeathDecrease(boolean decrease) {
		// TODO Auto-generated method stub
		
	}

}