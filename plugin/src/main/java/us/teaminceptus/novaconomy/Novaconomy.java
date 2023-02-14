package us.teaminceptus.novaconomy;

import com.google.common.collect.ImmutableList;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationInvite;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.NovaMarket;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;
import us.teaminceptus.novaconomy.api.events.AutomaticTaxEvent;
import us.teaminceptus.novaconomy.api.events.InterestEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerMissTaxEvent;
import us.teaminceptus.novaconomy.api.player.Bounty;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.essentialsx.EssentialsListener;
import us.teaminceptus.novaconomy.placeholderapi.Placeholders;
import us.teaminceptus.novaconomy.treasury.TreasuryRegistry;
import us.teaminceptus.novaconomy.util.NovaUtil;
import us.teaminceptus.novaconomy.vault.VaultRegistry;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.*;
import static us.teaminceptus.novaconomy.util.NovaUtil.format;

/**
 * Class representing this Plugin
 * @see NovaConfig
 * @see NovaMarket
 */
@SuppressWarnings("unchecked")
public final class Novaconomy extends JavaPlugin implements NovaConfig, NovaMarket {

	/**
	 * Main Novaconomy Constructor
	 * <strong>DO NOT INSTANTIATE THIS WAY</strong>
	 */
	public Novaconomy() { /* Constructor should only be called by Bukkit Plugin Class Loader */}

	static File playerDir;
	static FileConfiguration economiesFile;
	
	static FileConfiguration config;
	static ConfigurationSection interest;
	static ConfigurationSection ncauses;

	static String prefix;

    static File marketFile;


	/**
	 * Performs an API request to turn an OfflinePlayer's name to an OfflinePlayer object.
	 * @param name OfflinePlayer Name
	 * @return OfflinePlayer Object
	 */
	public static OfflinePlayer getPlayer(String name) {
		return Wrapper.getPlayer(name);
	}

	public static boolean isIgnored(Player p, String s) {
		AtomicBoolean state = new AtomicBoolean();

		FileConfiguration config = NovaConfig.getPlugin().getConfig();
		List<String> ignore = config.getStringList("NaturalCauses.Ignore");

		state.set(ignore.stream().anyMatch(s::equalsIgnoreCase));
		state.compareAndSet(false, ignore.stream().anyMatch(p.getName()::equals));

		Set<PermissionAttachmentInfo> infos = p.getEffectivePermissions();
		infos.forEach(perm -> state.compareAndSet(false, ignore.stream().anyMatch(perm.getPermission()::equals)));

		if (hasVault()) state.compareAndSet(false, VaultChat.isInGroup(ignore, p));

		return state.get();
	}

	static CommandWrapper getCommandWrapper() {
		try {
			if (w.getCommandVersion() == 0)
				return (CommandWrapper) Class.forName(CommandWrapper.class.getPackage().getName() + ".TestCommandWrapper").getConstructor(Plugin.class).newInstance(NovaConfig.getPlugin());

			final int wrapperVersion;

			String dec;
			String k = "CommandVersion";

			if (funcConfig.isInt(k)) {
				int i = funcConfig.getInt(k, 3);
				dec = i > 2 || i < 1 ? "auto" : i + "";
			} else
				dec = !funcConfig.getString(k, "auto").equalsIgnoreCase("auto") ? "auto" : funcConfig.getString(k, "auto");

			int tempV;
			try {
				if (dec.equalsIgnoreCase("auto")) tempV = w.getCommandVersion();
				else tempV = Integer.parseInt(dec);
			} catch (IllegalArgumentException e) {
				tempV = w.getCommandVersion();
			}

			wrapperVersion = tempV;
			return (CommandWrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".CommandWrapperV" + wrapperVersion).getConstructor(Plugin.class).newInstance(NovaConfig.getPlugin());
		} catch (InvocationTargetException e) {
			NovaConfig.print(e.getTargetException());
			return null;
		} catch (Exception e) {
			NovaConfig.print(e);
			return null;
		}
	}

	private static void runInterest() {
		if (!NovaConfig.getConfiguration().isInterestEnabled()) return;
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
		if (!event.isCancelled()) for (NovaPlayer np : previousBals.keySet()) {
			int i = 0;
			for (Economy econ : previousBals.get(np).keySet()) {
				np.add(econ, amounts.get(np).get(econ));
				i++;
			}

			if (np.isOnline() && np.hasNotifications())
				np.getOnlinePlayer().sendMessage(format(getMessage("notification.interest"), i + " ", i == 1 ? get("constants.economy") : get("constants.economies")));
		}
	}
	
	private static BukkitRunnable INTEREST_RUNNABLE = new BukkitRunnable() {
		@Override
		public void run() {
			if (!NovaConfig.getConfiguration().isInterestEnabled()) { cancel(); return; }
			runInterest();
		}
	};

	private static void runTaxes() {
		Novaconomy plugin = getPlugin(Novaconomy.class);
		if (!plugin.hasAutomaticTaxes()) return;
		Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
		Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();

		Collection<? extends OfflinePlayer> players = plugin.hasOnlineTaxes() ? Bukkit.getOnlinePlayers() : Arrays.asList(Bukkit.getOfflinePlayers());

		for (OfflinePlayer p : players) {
			if (plugin.canIgnoreTaxes(p)) continue;
			NovaPlayer np = new NovaPlayer(p);

			Map<Economy, Double> previousBal = new HashMap<>();
			Map<Economy, Double> amount = new HashMap<>();

			for (Economy econ : Economy.getTaxableEconomies()) {
				previousBal.put(econ, np.getBalance(econ));
				double amountD = plugin.getMinimumPayment(econ);
				if (amountD > 0) amount.put(econ, amountD);
			}

			previousBals.put(np, previousBal);
			if (amount.size() > 0) amounts.put(np, amount);
		}

		if (amounts.size() < 1) return;
		AutomaticTaxEvent event = new AutomaticTaxEvent(previousBals, amounts);
		Bukkit.getPluginManager().callEvent(event);

		Map<NovaPlayer, Map<Economy, Double>> missedMap = new HashMap<>();
		if (!event.isCancelled()) {
			for (NovaPlayer np : previousBals.keySet()) {
				missedMap.put(np, new HashMap<>());
				for (Economy econ : previousBals.get(np).keySet()) {
					double amount = amounts.get(np).get(econ);

					if (np.getBalance(econ) < amount) {
						Map<Economy, Double> newMap = new HashMap<>(missedMap.get(np));
						newMap.put(econ, amount);
						missedMap.put(np, newMap);
						np.deposit(econ, np.getBalance(econ));

						PlayerMissTaxEvent event2 = new PlayerMissTaxEvent(np.getPlayer(), amount - np.getBalance(econ), econ);
						Bukkit.getPluginManager().callEvent(event2);
					} else np.deposit(econ, amount);

				}
			}
			sendTaxNotifications(previousBals.keySet(), missedMap);
		}
	}

	private static void sendTaxNotifications(Collection<NovaPlayer> players, Map<NovaPlayer, Map<Economy, Double>> missedMap) {
		for (NovaPlayer np : players) {
			if (!np.getPlayer().isOnline()) continue;
			if (!np.hasNotifications()) continue;
			int j = missedMap.get(np).size();
			int i = Economy.getTaxableEconomies().size() - j;
			if (j > 0)
				np.getOnlinePlayer().sendMessage(format(getMessage("notification.tax.missed"), j + " ", j == 1 ? get("constants.economy") : get("constants.economies")));

			if (i > 0)
				np.getOnlinePlayer().sendMessage(format(getMessage("notification.tax"), i + " ", i == 1 ? get("constants.economy") : get("constants.economies")));
		}
	}

	private static BukkitRunnable TAXES_RUNNABLE = new BukkitRunnable() {
		@Override
		public void run() {
			if (!NovaConfig.getConfiguration().hasAutomaticTaxes()) { cancel(); return; }
			runTaxes();
		}
	};

	private static FileConfiguration funcConfig;

	static final List<Class<? extends ConfigurationSerializable>> SERIALIZABLE = ImmutableList.<Class<? extends ConfigurationSerializable>>builder()
			.add(Economy.class)
			.add(Business.class)
			.add(Price.class)
			.add(Product.class)
			.add(BusinessProduct.class)
			.add(Bounty.class)
			.add(BusinessStatistics.class)
			.add(BusinessStatistics.Transaction.class)
			.add(Rating.class)
			.add(PlayerStatistics.class)
			.add(CorporationInvite.class)
			.build();

	private void loadAddons() {
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			getLogger().info("Placeholder API Found! Hooking...");
			new Placeholders(this);
			getLogger().info("Hooked into Placeholder API!");
		}

		if (hasVault()) {
			getLogger().info("Vault Found! Hooking...");
			VaultRegistry.reloadVault();
		}

		if (Bukkit.getPluginManager().getPlugin("Treasury") != null) {
			getLogger().info("Treasury Found! Hooking...");
			new TreasuryRegistry(this);
		}

        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            getLogger().info("Essentials Found! Hooking...");
            new EssentialsListener(this);
        }
	}

	/**
	 * Called when the Plugin enables
	 */
	@Override
	public void onEnable() {
		File economiesDir = new File(getDataFolder(), "economies");
		if (!economiesDir.exists()) economiesDir.mkdir();
		loadLegacyEconomies();

		File businessesDir = new File(getDataFolder(), "businesses");
		if (!businessesDir.exists()) businessesDir.mkdir();
		loadLegacyBusinesses();

		funcConfig = NovaConfig.loadFunctionalityFile();
		playerDir = new File(getDataFolder(), "players");
		config = NovaConfig.loadConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		File globalF = new File(getDataFolder(), "global.yml");
		if (!globalF.exists()) saveResource("global.yml", false);

		FileConfiguration global = YamlConfiguration.loadConfiguration(globalF);
		for (Economy econ : Economy.getEconomies()) if (!global.isSet("Bank." + econ.getName())) global.set("Bank." + econ.getName(), 0);
		try { global.save(globalF); } catch (IOException e) { getLogger().severe(e.getMessage()); for (StackTraceElement s : e.getStackTrace()) getLogger().severe(s.toString()); }

		getLogger().info("Loaded Languages & Configuration...");
		SERIALIZABLE.forEach(ConfigurationSerialization::registerClass);

		prefix = get("plugin.prefix");

        marketFile = new File(getDataFolder(), "market.dat");
        if (!marketFile.exists())
			try { marketFile.createNewFile(); } catch (IOException e) { NovaConfig.print(e); }

		getLogger().info("Loaded Files...");

		if (getCommandWrapper() == null) {
			getLogger().severe(format("Command Wrapper not found for version \"%s\" Disabling...", Bukkit.getBukkitVersion()));
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		new Events(this);
		new GUIManager(this);

		// Load Cache
		Business.getBusinesses();
		Corporation.getCorporations();
		Economy.getEconomies();

		INTEREST_RUNNABLE.runTaskTimer(this, getInterestTicks(), getInterestTicks());
		TAXES_RUNNABLE.runTaskTimer(this, getTaxesTicks(), getTaxesTicks());

		for (Player p : Bukkit.getOnlinePlayers()) w.addPacketInjector(p);

		getLogger().info("Loaded Core Functionality...");

		if (w.getCommandVersion() == 0) {
			getLogger().info("Finished Loading Test Plugin!");
			return;
		}

		// Update Checker
		new UpdateChecker(this, UpdateCheckSource.SPIGOT, "100503")
				.setDownloadLink("https://www.spigotmc.org/resources/novaconomy.100503/")
				.setNotifyOpsOnJoin(true)
				.setChangelogLink("https://github.com/Team-Inceptus/Novaconomy/releases/")
				.setUserAgent("Java 8 Novaconomy User Agent")
				.setColoredConsoleOutput(true)
				.setDonationLink("https://www.patreon.com/teaminceptus")
				.setNotifyRequesters(true)
				.checkEveryXHours(1)
				.checkNow();

		// bStats
		Metrics metrics = new Metrics(this, PLUGIN_ID);

		metrics.addCustomChart(new SimplePie("used_language", () -> Language.getById(this.getLanguage()).name()));
		metrics.addCustomChart(new SimplePie("command_version", () -> w.getCommandVersion() + ""));
		metrics.addCustomChart(new SingleLineChart("economy_count", () -> Economy.getEconomies().size()));
		metrics.addCustomChart(new SingleLineChart("business_count", () -> Business.getBusinesses().size()));
		metrics.addCustomChart(new SingleLineChart("bounty_count", () -> {
			AtomicInteger count = new AtomicInteger();
			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) count.addAndGet(new NovaPlayer(p).getOwnedBounties().size());
			return count.get();
		}));

		getLogger().info("Loaded Dependencies...");

		loadAddons();
		getLogger().info("Loaded Optional Hooks...");

		saveConfig();
		getLogger().info("Successfully loaded Novaconomy");
	}

	@Override
	public void onDisable() {
		SERIALIZABLE.forEach(ConfigurationSerialization::unregisterClass);
		for (Player p : Bukkit.getOnlinePlayers()) w.removePacketInjector(p);
	}

	private void loadLegacyBusinesses() {
		File businesses = new File(getDataFolder(), "businesses.yml");
		if (businesses.exists()) {
			getLogger().warning("Businesses are now stored in individual files. Automatically migrating...");

			FileConfiguration bConfig = YamlConfiguration.loadConfiguration(businesses);
			bConfig.getValues(false).forEach((k, v) -> {
				if (!(v instanceof Business)) return;
				Business b = (Business) v;
				b.saveBusiness();
			});

			businesses.delete();

			getLogger().info("Migration complete!");
		}
	}

	private void loadLegacyEconomies() {
		File economies = new File(getDataFolder(), "economies.yml");

		if (economies.exists()) {
			getLogger().warning("Economies are now stored in individual files. Automatically migrating...");

			FileConfiguration eConfig = YamlConfiguration.loadConfiguration(economies);
			eConfig.getKeys(false).forEach(k -> {
				ConfigurationSection sec = eConfig.getConfigurationSection(k);
				if (sec == null) return;

				Economy econ = (Economy) sec.get("economy");
				if (econ == null) return;

				econ.saveEconomy();
			});

			NovaUtil.sync(() -> {
				economies.delete();
				getLogger().info("Migration complete!");
			});
		}
	}

	static final int PLUGIN_ID = 15322;

	/**
	 * Whether the server is currently running on a legacy platform (1.8-1.12) and Command Version 1 is active.
	 * <br><br>
	 * Setting the command version to 1 in functionality.yml will not change this value.
	 * @return true if legacy server, else false
	 */
	public static boolean isLegacy() { return w.isLegacy(); }

	/**
	 * Fetches the Directory of all Player data.
	 * @return the player directory
	 */
	public static File getPlayerDirectory() {
		return playerDir;
	}

	/**
	 * Fetches the Economy Configuration File.
	 * @return the economy configuration file
	 */
	public static FileConfiguration getEconomiesFile() {
		return economiesFile;
	}
	
	@Override
	public long getInterestTicks() { return interest.getLong("IntervalTicks"); }

	@Override
	public boolean isInterestEnabled() { return interest.getBoolean("Enabled"); }

	@Override
	public void setInterestEnabled(boolean enabled) { interest.set("Enabled", enabled); saveConfig(); }

	@Override
	public double getMaxConvertAmount(Economy econ) {
		if (funcConfig.getConfigurationSection("EconomyMaxConvertAmounts").contains(econ.getName())) return funcConfig.getDouble("EconomyMaxConvertAmounts." + econ.getName());
		return funcConfig.getDouble("MaxConvertAmount", -1);
	}

	@Override
	public void reloadHooks() {
		if (hasVault()) VaultRegistry.reloadVault();
	}

	private boolean isIncludedIn(List<String> list, OfflinePlayer p) {
		if (list == null || list.isEmpty()) return false;
		AtomicBoolean b = new AtomicBoolean();

		for (String s : list) {
			Pattern patt = Pattern.compile(s);
			if (patt.matcher(p.getName()).matches() || (s.equalsIgnoreCase("OPS") && p.isOp()) || (s.equalsIgnoreCase("NONOPS") && !p.isOp())) {
				b.set(true);
				break;
			}
		}

		if (p.isOnline()) {
			Player op = p.getPlayer();
			for (String s : list) {
				if (b.get()) break;
				Pattern patt = Pattern.compile(s);
				for (PermissionAttachmentInfo info : op.getEffectivePermissions())
					if (patt.matcher(info.getPermission()).matches()) {
						b.set(true);
						break;
					}
			}
			if (hasVault() && VaultChat.isInGroup(list, op)) b.set(true);
		}

		return b.get();
	}

	static boolean hasVault() {
		return Bukkit.getPluginManager().getPlugin("Vault") != null;
	}

	@Override
	public double getMaxWithdrawAmount(Economy econ) {
		ConfigurationSection sec = config.getConfigurationSection("Taxes.MaxWithdraw");
		return sec.contains(econ.getName()) ? sec.getDouble(econ.getName()) : sec.getDouble("Global", 100);
	}

	@Override
	public boolean canBypassWithdraw(OfflinePlayer p) {
		return isIncludedIn(config.getStringList("Taxes.MaxWithdraw.Bypass"), p);
	}

	@Override
	public boolean canIgnoreTaxes(OfflinePlayer p) {
		return isIncludedIn(config.getStringList("Taxes.Ignore"), p);
	}

	@Override
	public boolean hasAutomaticTaxes() {
		return config.getBoolean("Taxes.Automatic.Enabled", false);
	}

	@Override
	public long getTaxesTicks() {
		return config.getLong("Taxes.Automatic.Interval");
	}

	@Override
	public double getMinimumPayment(Economy econ) {
		return config.getDouble("Taxes.Minimums." + econ.getName(), config.getDouble("Taxes.Minimums.Global", 0));
	}

	@Override
	public boolean hasOnlineTaxes() {
		return config.getBoolean("Taxes.Online", false);
	}

	@Override
	public void setOnlineTaxes(boolean enabled) {
		config.set("Taxes.Online", enabled);
		saveConfig();
	}

	@Override
	public boolean hasCustomTaxes() {
		return config.getBoolean("Taxes.Events.Enabled", false);
	}

	@Override
	public void setCustomTaxes(boolean enabled) {
		config.set("Taxes.Events.Enabled", enabled);
		saveConfig();
	}

	@Override
	public double getMaxIncrease() {
		return config.getDouble("NaturalCauses.MaxIncrease", -1) <= 0 ? Double.MAX_VALUE : config.getDouble("NaturalCauses.MaxIncrease", Double.MAX_VALUE);
	}

	@Override
	public void setMaxIncrease(double max) {
		config.set("NaturalCauses.MaxIncrease", max);
		saveConfig();
	}

	@Override
	public boolean hasEnchantBonus() {
		return ncauses.getBoolean("EnchantBonus", true);
	}

	@Override
	public void setEnchantBonus(boolean enabled) {
		config.set("NaturalCauses.EnchantBonus", enabled);
		saveConfig();
	}

	@Override
	public boolean hasBounties() {
		return config.getBoolean("Bounties.Enabled", true);
	}

	@Override
	public void setBountiesEnabled(boolean enabled) {
		config.set("Bounties.Enabled", enabled);
		saveConfig();
	}

	@Override
	public boolean isBroadcastingBounties() {
		return config.getBoolean("Bounties.Broadcast", true);
	}

	@Override
	public void setBroadcastingBounties(boolean broadcast) {
		config.set("Bounties.Broadcast", broadcast);
		saveConfig();
	}

	@Override
	public Set<CustomTaxEvent> getAllCustomEvents() {
		Set<CustomTaxEvent> events = new HashSet<>();

		config.getConfigurationSection("Taxes.Events").getValues(false).forEach((k, v) -> {
			if (!(v instanceof ConfigurationSection)) return;
			ConfigurationSection sec = (ConfigurationSection) v;

			String name = sec.getString("name", k);
			String perm = sec.getString("permission", "novaconomy.admin.tax.call");
			String msg = sec.getString("message", "");
			boolean ignore = sec.getBoolean("using_ignore", true);
			boolean online = sec.getBoolean("online", false);
			List<String> ignored = sec.getStringList("ignore");
			boolean deposit = sec.getBoolean("deposit", true);

			List<Price> prices = new ArrayList<>();
			String amount = sec.get("amount").toString();
			if (amount.contains("[") && amount.contains("]")) {
				amount = amount.replaceAll("[\\[\\]]", "").replace(" ", "");
				String[] amounts = amount.split(",");

				for (String s : amounts) prices.add(new Price(ModifierReader.readString(s)));
			} else prices.add(new Price(ModifierReader.readString(amount)));

			events.add(new CustomTaxEvent(k, name, prices, perm, msg, ignore, ignored, online, deposit));
		});

		return events;
	}

	@Override
	public boolean isIgnoredTax(@NotNull OfflinePlayer p, @Nullable NovaConfig.CustomTaxEvent event) {
		AtomicBoolean state = new AtomicBoolean();

		FileConfiguration config = NovaConfig.getPlugin().getConfig();
		List<String> ignore = config.getStringList("Taxes.Ignore");

		state.compareAndSet(false, ignore.contains("OPS") && p.isOp());
		state.compareAndSet(false, ignore.contains("NONOPS") && !p.isOp());

		if (p.isOnline()) {
			Player op = p.getPlayer();

			Set<PermissionAttachmentInfo> infos = op.getEffectivePermissions();
			infos.forEach(perm -> state.compareAndSet(false, ignore.stream().anyMatch(perm.getPermission()::equals)));

			if (hasVault()) state.compareAndSet(false, VaultChat.isInGroup(ignore, op));
		}

		if (event != null) {
			if (event.isUsingIgnore()) state.compareAndSet(false, ignore.stream().anyMatch(p.getName()::equals));
			state.compareAndSet(false, event.getIgnoring().stream().anyMatch(p.getName()::equals));

			if (p.isOnline()) {
				Player op = p.getPlayer();
				if (hasVault()) state.compareAndSet(false, VaultChat.isInGroup(event.getIgnoring(), op));
			}
		}

		return state.get();
	}

	@Override
	public boolean isMarketEnabled() {
		return config.getBoolean("Business.Market.Enabled", true);
	}

	@Override
	public void setMarketEnabled(boolean enabled) {
		config.set("Business.Market.Enabled", enabled);
		saveConfig();
	}

	@Override
	public double getMarketTax() {
		return config.getDouble("Business.Market.MarketTax", 0.05);
	}

	@Override
	public void setMarketTax(double tax) throws IllegalArgumentException {
		if (tax <= 0) throw new IllegalArgumentException("Tax must be greater than 0");
		config.set("Business.Market.MarketTax", tax);
		saveConfig();
	}

	@Override
	public boolean isAdvertisingEnabled() {
		return config.getBoolean("Business.Advertising.Enabled", true);
	}

	@Override
	public void setAdvertisingEnabled(boolean enabled) {
		config.set("Business.Advertising.Enabled", enabled);
		saveConfig();
	}

	@Override
	public double getBusinessAdvertisingReward() {
		return config.getDouble("Business.Advertising.ClickReward", 5.0D);
	}

	@Override
	public void setBusinessAdvertisingReward(double reward) {
		config.set("Business.Advertising.ClickReward", reward);
		saveConfig();
	}

	@Override
	public void setLanguage(@NotNull Language language) throws IllegalArgumentException {
		if (language == null) throw new IllegalArgumentException("Language cannot be null");
		config.set("Language", language.getIdentifier());
		saveConfig();
	}

	@Override
	public boolean hasProductIncrease() {
		return config.getBoolean("Corporations.ExperienceIncrease.ProductIncrease", true);
	}

	@Override
	public void setProductIncrease(boolean enabled) {
		config.set("Corporations.ExperienceIncrease.ProductIncrease", enabled);
		saveConfig();
	}

	@Override
	public double getProductIncreaseModifier() {
		return config.getDouble("Corporations.ExperienceIncrease.ProductIncreaseModifier", 1);
	}

	@Override
	public void setProductIncreaseModifier(double modifier) {
		config.set("Corporations.ExperienceIncrease.ProductIncreaseModifier", modifier);
		saveConfig();
	}

	@Override
	public boolean hasMiningIncrease() { return ncauses.getBoolean("MiningIncrease", true); }

	@Override
	public boolean hasFishingIncrease() { return ncauses.getBoolean("FishingIncrease", true); }

	@Override
	public boolean hasKillIncrease() { return ncauses.getBoolean("KillIncrease", true); }

	@Override
	public boolean hasIndirectKillIncrease() { return ncauses.getBoolean("KillIncreaseIndirect", true); }

	@Override
	public String getLanguage() { return config.getString("Language", "en"); }

	@Override
	public boolean hasDeathDecrease() { return ncauses.getBoolean("DeathDecrease", true); }

	@Override
	public boolean hasFarmingIncrease() { return ncauses.getBoolean("FarmingIncrease", true); }

	@Override
	public double getInterestMultiplier() { return interest.getDouble("ValueMultiplier", 1.03D); }

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
		saveConfig();
	}

	@Override
	public void setDeathDecrease(boolean decrease) {
		ncauses.set("DeathDecrease", decrease);
		saveConfig();
	}

	@Override
	public boolean hasNotifications() {
		return config.getBoolean("Notifications", true);
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

	@SuppressWarnings("unused")
	private static void updateRunnables() {
		Novaconomy plugin = getPlugin(Novaconomy.class);

		config = plugin.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		try { if (INTEREST_RUNNABLE.getTaskId() != -1) INTEREST_RUNNABLE.cancel(); } catch (IllegalStateException ignored) {}
		try { if (TAXES_RUNNABLE.getTaskId() != -1) TAXES_RUNNABLE.cancel(); } catch (IllegalStateException ignored) {}

		INTEREST_RUNNABLE = new BukkitRunnable() {
			@Override
			public void run() {
				if (!(NovaConfig.getConfiguration().isInterestEnabled())) { cancel(); return; }
				runInterest();
			}
		};

		TAXES_RUNNABLE = new BukkitRunnable() {
			@Override
			public void run() {
				if (!(NovaConfig.getConfiguration().hasAutomaticTaxes())) { cancel(); return; }
				runTaxes();
			}
		};

		NovaUtil.sync(() -> {
			INTEREST_RUNNABLE.runTaskTimer(plugin, plugin.getInterestTicks(), plugin.getInterestTicks());
			TAXES_RUNNABLE.runTaskTimer(plugin, plugin.getTaxesTicks(), plugin.getTaxesTicks());
		});
	}

    // Market Impl

    static final Map<Material, Double> prices = new HashMap<>();
    static final Map<Material, Integer> purchaseCount = new HashMap<>();

    private void readMarket() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(marketFile.toPath()));
        prices.putAll((Map<Material, Double>) ois.readObject());
        purchaseCount.putAll((Map<Material, Integer>) ois.readObject());
        ois.close();
    }

    private void writeMarket() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(marketFile.toPath()));
        oos.writeObject(prices);
        oos.writeObject(purchaseCount);
        oos.close();
    }

    private void writeMarketWithCatch() {
        try {
            writeMarket();
        } catch (IOException e) {
            NovaConfig.print(e);    
        }
    }

    private void readMarketWithCatch() {
        try {
            readMarket();
        } catch (IOException | ClassNotFoundException e) {
            NovaConfig.print(e);
        }
    }

    @Override
    public double getPrice(@NotNull Material m) {
//        if (prices.isEmpty()) readMarketWithCatch();
//        return prices.getOrDefault(m, 1.0);
		throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public @NotNull Receipt buy(@NotNull OfflinePlayer buyer, @NotNull Material m, int amount, @NotNull Economy econ) throws IllegalArgumentException {
//        if (buyer == null) throw new IllegalArgumentException("Buyer cannot be null");
//        if (m == null) throw new IllegalArgumentException("Material cannot be null");
//        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
//        NovaPlayer np = new NovaPlayer(buyer);
//
//        double price = getPrice(m, econ) * amount;
//        if (price <= 0) throw new IllegalArgumentException("Price must be positive");
//
//        if (np.getBalance(econ) < price) throw new IllegalArgumentException("Insufficient funds");
//        np.remove(econ, price);
//        purchaseCount.put(m, purchaseCount.getOrDefault(m, 0) + amount);
//        writeMarketWithCatch();
//
//		return new Receipt(m, price, buyer);
		throw new UnsupportedOperationException("Not implemented yet");
    }

}