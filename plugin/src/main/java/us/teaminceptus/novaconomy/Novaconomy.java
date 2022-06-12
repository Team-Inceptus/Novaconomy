package us.teaminceptus.novaconomy;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.apache.commons.lang.Validate;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.InterestEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing this Plugin
 * @see NovaConfig
 */
public class Novaconomy extends JavaPlugin implements NovaConfig {
	
	private static File playerDir;
	private static FileConfiguration economiesFile;
	
	private static FileConfiguration config;
	private static ConfigurationSection interest;
	private static ConfigurationSection ncauses;

	private static String prefix;

	public static String get(String key) {
		String lang = NovaConfig.getConfiguration().getLanguage();
		return Language.getById(lang).getMessage(key);
	}

	public static String getMessage(String key) { return prefix + get(key); }

	static Random r = new Random();

	private static void updateInterest() {
		Novaconomy plugin = getPlugin(Novaconomy.class);

		config = plugin.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		if (INTEREST_RUNNABLE.getTaskId() != -1) INTEREST_RUNNABLE.cancel();

		INTEREST_RUNNABLE = new BukkitRunnable() {
			public void run() {
				if (!(NovaConfig.getConfiguration().isInterestEnabled())) cancel();

				Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
				Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();

				for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
					NovaPlayer np = new NovaPlayer(p);

					Map<Economy, Double> previousBal = new HashMap<>();
					Map<Economy, Double> amount = new HashMap<>();
					for (Economy econ : Economy.getInterestEconomies()) {
						double balance = np.getBalance(econ);
						double add = (balance * (NovaConfig.getConfiguration().getInterestMultiplier() - 1)) / econ.getConversionScale();

						previousBal.put(econ, balance);
						amount.put(econ, add);
					}

					previousBals.put(np, previousBal);
					amounts.put(np, amount);
				}

				InterestEvent event = new InterestEvent(previousBals, amounts);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					for (NovaPlayer np : previousBals.keySet()) {
						int i = 0;
						for (Economy econ : previousBals.get(np).keySet()) {
							np.add(econ, amounts.get(np).get(econ));
							i++;
						}

						if (np.getPlayer().isOnline() && NovaConfig.getConfiguration().hasNotifications()) {
							np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.interest"), i + "", (i == 1 ? get("constants.economy") : get("constants.economies"))));
						}
					}
				}
			}

		};

		new BukkitRunnable() {
			public void run() {
				INTEREST_RUNNABLE.runTaskTimer(plugin, plugin.getIntervalTicks(), plugin.getIntervalTicks());
			}
		}.runTask(plugin);

	}

	private class Events implements Listener {
		
		private Novaconomy plugin;
		
		protected Events(Novaconomy plugin) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
			this.plugin = plugin;
		}

		@EventHandler
		public void claimCheck(PlayerInteractEvent e) {
			if (e.getItem() == null) return;
			if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
			Player p = e.getPlayer();
			Wrapper wrapper = getWrapper();
			if (!(e.getItem().getType() == Material.PAPER)) return;
			ItemStack item = e.getItem();

			try {
				String value = wrapper.getNBTString(item, "economy");
				Validate.notNull(value);
				double amount = Double.parseDouble(wrapper.getNBTString(item, "amount"));
				Validate.notNull(value);
			} catch (IllegalArgumentException | NullPointerException ignored) {}
		}
		
		@EventHandler
		public void moneyIncrease(EntityDamageByEntityEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasKillIncrease())) return;
			if (!(e.getDamager() instanceof Player)) return;
			Player p = (Player) e.getDamager();
			if (!(e.getEntity() instanceof LivingEntity)) return;
			LivingEntity en = (LivingEntity) e.getEntity();
			if (en.getHealth() - e.getFinalDamage() > 0) return;

			update(p, en.getMaxHealth());
		}
		
		@EventHandler
		public void moneyIncrease(BlockBreakEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasMiningIncrease())) return;
			if (e.getBlock().getDrops().size() < 1) return;
			
			Block b = e.getBlock();
			Player p = e.getPlayer();
			
			if (!(ores.contains(b.getType()))) return;

			update(p, e.getExpToDrop());
		}
		
		@EventHandler
		public void moneyIncrease(PlayerFishEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasFishingIncrease())) return;
			
			if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
			
			Player p = e.getPlayer();
			update(p, e.getExpToDrop());
		}

		private void update(Player p, double amount) {
			NovaPlayer np = new NovaPlayer(p);
			List<String> added = new ArrayList<>();

			for (Economy econ : Economy.getNaturalEconomies()) {
				double divider = r.nextInt(2) + 1;
				double increase = ((amount + r.nextInt(8) + 1) / divider) / econ.getConversionScale();

				double previousBal = np.getBalance(econ);

				PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);

				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					np.add(econ, increase);

					String message = ChatColor.GREEN + "+" + (Math.floor(increase * 100) / 100 + econ.getSymbol() + "").replace("D", "") + ", ";
					added.add(message);
				}

			}

			if (plugin.hasNotifications()) getWrapper().sendActionbar(p, String.join("\n", added.toArray(new String[0])));
		}
		
		@EventHandler
		public void moneyDecrease(PlayerDeathEvent e) {
			if (!(plugin.hasDeathDecrease())) return;
			
			Player p = e.getEntity();
			NovaPlayer np = new NovaPlayer(p);
			
			List<String> lost = new ArrayList<>();
			
			lost.add(ChatColor.RED + "You Lost:");
			
			for (Economy econ : Economy.getEconomies()) {
				double amount = np.getBalance(econ) / getDeathDivider();
				np.remove(econ, amount);
				
				lost.add(ChatColor.DARK_RED + "- " + ChatColor.RED + econ.getSymbol() + Math.floor(amount * 100) / 100);
			}
			
			if (plugin.hasNotifications()) p.sendMessage(String.join("\n", lost.toArray(new String[0])));
		}

	}

	private static final Set<Material> ores = new HashSet<Material>() {{
		addAll(Arrays.stream(Material.values()).filter(m -> m.name().endsWith("ORE") || m.name().equalsIgnoreCase("ANCIENT_DEBRIS")).collect(Collectors.toSet()));
	}};

	private static CommandWrapper getCommandWrapper() {
		try {
			final int wrapperVersion;
			String dec;
			String k = "CommandVersion";

			if (funcConfig.isInt(k)) {
				int i = funcConfig.getInt(k, 3);
				dec = i > 2 || i < 1 ? "auto" : i + "";
			} else
				dec = !funcConfig.getString(k, "auto").equalsIgnoreCase("auto") ? "auto" : funcConfig.getString(k, "auto");

			if (dec.equalsIgnoreCase("auto")) wrapperVersion = getWrapper().getCommandVersion();
			else wrapperVersion = Integer.parseInt(dec);

			return (CommandWrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".CommandWrapperV" + wrapperVersion).getConstructor(Plugin.class).newInstance(JavaPlugin.getPlugin(Novaconomy.class));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getServerVersion() {
		return  Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
	}

	private static Wrapper getWrapper() {
		try {
			return (Wrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".Wrapper" + getServerVersion()).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static BukkitRunnable INTEREST_RUNNABLE = new BukkitRunnable() {
		public void run() {
			if (!(NovaConfig.getConfiguration().isInterestEnabled())) cancel();
			
			Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
			Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();
			
			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
				NovaPlayer np = new NovaPlayer(p);
				
				Map<Economy, Double> previousBal = new HashMap<>();
				Map<Economy, Double> amount = new HashMap<>();
				for (Economy econ : Economy.getInterestEconomies()) {
					double balance = np.getBalance(econ);
					double add = (balance * (NovaConfig.getConfiguration().getInterestMultiplier() - 1)) / econ.getConversionScale();
					
					previousBal.put(econ, balance);
					amount.put(econ, add);
				}
				
				previousBals.put(np, previousBal);
				amounts.put(np, amount);
			}
			
			InterestEvent event = new InterestEvent(previousBals, amounts);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				for (NovaPlayer np : previousBals.keySet()) {
					int i = 0;
					for (Economy econ : previousBals.get(np).keySet()) {
						np.add(econ, amounts.get(np).get(econ));
						i++;
					}
					
					if (np.getPlayer().isOnline() && NovaConfig.getConfiguration().hasNotifications()) {
						np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.interest"), i + "", i == 1 ? get("constants.economy") : get("constants.economies")));
					}
				}
			}
		}
		
	};

	private static FileConfiguration funcConfig;

	private static final List<Class<? extends ConfigurationSerializable>> SERIALIZABLE = new ArrayList<Class<? extends ConfigurationSerializable>>() {{
		add(Economy.class);
		add(Business.class);
		add(Price.class);
		add(Product.class);
	}};

	public void onEnable() {
		saveDefaultConfig();
		saveConfig();

		funcConfig = NovaConfig.loadFunctionalityFile();

		for (Language l : Language.values()) {
			File f = new File(getDataFolder(), "novaconomy" + ( l.getIdentifier().length() == 0 ? "" : "_" + l.getIdentifier() ) + ".properties");

			if (!(f.exists())) {
				saveResource("novaconomy" + (l.getIdentifier().length() == 0 ? "" : "_" + l.getIdentifier()) + ".properties", false);
			}

			getLogger().info("Loaded Language " + l.name() + "...");
		}

		SERIALIZABLE.forEach(ConfigurationSerialization::registerClass);

		playerDir = new File(getDataFolder(), "players");
		File economyFile = new File(getDataFolder(), "economies.yml");

		try {
			if (!(economyFile.exists())) economyFile.createNewFile();
			if (!(playerDir.exists())) playerDir.mkdir();
		} catch (IOException e) {
			getLogger().info("Error loading files & folders");
			e.printStackTrace();
		}

		economiesFile = YamlConfiguration.loadConfiguration(economyFile);
		config = this.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		prefix = get("plugin.prefix");

		getLogger().info("Loaded Files...");

		getCommandWrapper();
		new Events(this);

		reloadValues();
		
		INTEREST_RUNNABLE.runTaskTimer(this, getIntervalTicks(), getIntervalTicks());

		getLogger().info("Loaded Core Functionality...");

		new UpdateChecker(this, UpdateCheckSource.SPIGOT, "100503")
				.setDownloadLink("https://www.spigotmc.org/resources/novaconomy.100503/")
				.setNotifyOpsOnJoin(true)
				.setChangelogLink("https://github.com/Team-Inceptus/Novaconomy/releases/")
				.setUserAgent("Java 8 Novaconomy User Agent")
				.checkEveryXHours(1)
				.checkNow();

		Metrics metrics = new Metrics(this, PLUGIN_ID);

		saveConfig();
		getLogger().info("Successfully loaded Novaconomy");
	}

	private static final int PLUGIN_ID = 15322;

	private void reloadValues() {
		NovaConfig.loadConfig();
	}
	
	public static File getPlayerDirectory() {
		return playerDir;
	}

	public static FileConfiguration getEconomiesFile() {
		return economiesFile;
	}
	
	@Override
	public long getIntervalTicks() { return interest.getLong("IntervalTicks"); }

	@Override
	public boolean isInterestEnabled() { return interest.getBoolean("Enabled"); }

	@Override
	public void setInterestEnabled(boolean enabled) { interest.set("Enabled", enabled); saveConfig(); }

	@Override
	public boolean hasMiningIncrease() { return ncauses.getBoolean("MiningIncrease"); }

	@Override
	public boolean hasFishingIncrease() { return ncauses.getBoolean("FishingIncrease"); }

	@Override
	public boolean hasKillIncrease() { return ncauses.getBoolean("KillIncrease"); }

	@Override
	public String getLanguage() { return config.getString("Language"); }


	@Override
	public boolean hasDeathDecrease() { return ncauses.getBoolean("DeathDecrease"); }

	@Override
	public boolean hasFarmingIncrease() { return ncauses.getBoolean("FarmingIncrease"); }

	@Override
	public double getInterestMultiplier() { return interest.getDouble("ValueMultiplier"); }

	@Override
	public void setInterestMultiplier(double multiplier) {
		interest.set("ValueMultiplier", multiplier);
		saveConfig();
	}

	@Override
	public int getMiningChance() {
		return ncauses.getInt("MiningIncreaseChance");
	}

	@Override
	public int getFishingChance() {
		return ncauses.getInt("FishingIncreaseChance");
	}

	@Override
	public int getKillChance() {
		return ncauses.getInt("KillIncreaseChance");
	}

	@Override
	public int getFarmingChance() {
		return ncauses.getInt("FarmingIncreaseChance");
	}

	@Override
	public void setKillChance(int chance) {
		ncauses.set("KillIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setFishingChance(int chance) {
		ncauses.set("FishingIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setMiningChance(int chance) {
		ncauses.set("MiningChanceIncrease", chance);
		saveConfig();
	}

	@Override
	public void setFarmingChance(int chance) {
		ncauses.set("FarmingIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setFarmingIncrease(boolean increase) {
		ncauses.set("FarmingIncrease", increase);
		saveConfig();
	}

	@Override
	public void setMiningIncrease(boolean increase) {
		ncauses.set("MiningIncrease", increase);
		saveConfig();
	}

	@Override
	public void setKillIncrease(boolean increase) {
		ncauses.set("KillIncrease", increase);
	}

	@Override
	public void setDeathDecrease(boolean decrease) {
		ncauses.set("DeathDecrease", decrease);
		saveConfig();
	}

	@Override
	public boolean hasNotifications() {
		return config.getBoolean("Notifications");
	}

	@Override
	public void setDeathDivider(double divider) {
		ncauses.set("DeathDivider", divider);
		saveConfig();
	}

	@Override
	public double getDeathDivider() {
		return ncauses.getDouble("DeathDivider");
	}

}