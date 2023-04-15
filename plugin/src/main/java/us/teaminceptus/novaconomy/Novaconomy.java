package us.teaminceptus.novaconomy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
				dec = i > 2 || i < 1 ? "auto" : String.valueOf(i);
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

	private static BukkitRunnable PING_DB_RUNNABLE = new BukkitRunnable() {
		@Override
		public void run() {
			try {
				if (!NovaConfig.getConfiguration().isDatabaseEnabled()) {
					cancel();
					return;
				}

				if (db == null) {
					NovaConfig.getLogger().severe("Database has been disconnected!");
					Bukkit.getPluginManager().disablePlugin(NovaConfig.getPlugin());
					cancel();
					return;
				}

				db.createStatement().execute("SELECT 1");
			} catch (SQLException e) {
				NovaConfig.getLogger().severe("Failed to Ping Database:");
				NovaConfig.print(e);
			}
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

	static Connection db;

	private void connectDB(boolean validate) {
		boolean invalid = false;

		String host = config.getString("Database.Host");
		int port = config.getInt("Database.Port", 3306);
		String database = config.getString("Database.Database");
		String username = config.getString("Database.Username");
		String password = config.getString("Database.Password");
		String service = config.getString("Database.Service", "mysql");

		if (validate) {
			if (host == null) {
				getLogger().severe("Database Host is not set!");
				invalid = true;
			}
			if (port == 0 || port > 65535) {
				getLogger().severe("Database Port is not set or invalid!");
				invalid = true;
			}
			if (database == null) {
				getLogger().severe("Database Database is not set!");
				invalid = true;
			}
			if (username == null) {
				getLogger().severe("Database Username is not set!");
				invalid = true;
			}
			if (password == null) {
				getLogger().severe("Database Password is not set!");
				invalid = true;
			}
			if (service == null) {
				getLogger().severe("Database Service is not set!");
				invalid = true;
			}
		}

		if (invalid && validate)
			throw new RuntimeException("Invalid Database Configuration");

		String url = null;

		try {
			switch (service) {
				case "oracle": {
					Class.forName("oracle.jdbc.driver.OracleDriver");
					url = "jdbc:oracle:thin@//" + host + ":" + port + "/" + database;
					break;
				}
				case "postgresql": {
					Class.forName("org.postgresql.Driver");
					url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
					break;
				}
				case "mysql": {
					Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
					url = "jdbc:mysql://" + host + ":" + port + "/" + database;
					break;
				}
				case "sqlite": {
					Class.forName("org.sqlite.JDBC");
					url = "jdbc:sqlite:" + database;
					break;
				}
				default:
					if (validate) throw new RuntimeException("Unsupported Database Service \"" + service + "\"");
			}
		} catch (ReflectiveOperationException e) {
			if (validate) throw new RuntimeException(e);
		}

		try {
			db = DriverManager.getConnection(url, username, password);
			if (db == null) {
				getLogger().severe("Failed to connect to database!");
				throw new RuntimeException("Failed to connect to database");
			}
		} catch (SQLException e) {
			if (validate) {
				getLogger().severe("Failed to connect to database!");
				throw new RuntimeException(e);
			}
		}
	}

	private void disconnectDB() {
		try {
			if (db != null && !db.isClosed()) {
				db.close();
				db = null;
			}
		} catch (SQLException e) {
			NovaConfig.print(e);
		}
	}

	private void loadFiles() {
		loadLegacyEconomies();
		loadLegacyBusinesses();

		if (isDatabaseEnabled()) {
			if (db != null) {
				convertToDatabase();
				return;
			}

			getLogger().info("Database found! Connecting...");

			connectDB(true);
			PING_DB_RUNNABLE.runTaskTimerAsynchronously(this, 60 * 20, 60 * 20);

			getLogger().info("Connection Successful!");
			convertToDatabase();
		} else {
			getLogger().info("Database is disabled! Loading Files...");
			convertToFiles();

			File economiesDir = new File(getDataFolder(), "economies");
			if (!economiesDir.exists()) economiesDir.mkdir();

			File businessesDir = new File(getDataFolder(), "businesses");
			if (!businessesDir.exists()) businessesDir.mkdir();

			File corporationsDir = new File(getDataFolder(), "corporations");
			if (!corporationsDir.exists()) corporationsDir.mkdir();

			playerDir = new File(getDataFolder(), "players");
			marketFile = new File(getDataFolder(), "market.dat");
			if (!marketFile.exists()) try { marketFile.createNewFile(); } catch (IOException e) { NovaConfig.print(e); }
		}

		loadMarket();
		File globalF = new File(getDataFolder(), "global.yml");
		if (!globalF.exists()) saveResource("global.yml", false);

		FileConfiguration global = YamlConfiguration.loadConfiguration(globalF);
		for (Economy econ : Economy.getEconomies()) if (!global.isSet("Bank." + econ.getName())) global.set("Bank." + econ.getName(), 0);
		try { global.save(globalF); } catch (IOException e) { getLogger().severe(e.getMessage()); for (StackTraceElement s : e.getStackTrace()) getLogger().severe(s.toString()); }

		NovaConfig.loadConfig();
	}

	private void convertToDatabase() {
		File businessesDir = NovaConfig.getBusinessesFolder();
		File economiesDir = NovaConfig.getEconomiesFolder();
		File corporationDir = NovaConfig.getCorporationsFolder();
		File playerDir = NovaConfig.getPlayerDirectory();
		File marketFile = NovaConfig.getMarketFile();

		if (!NovaConfig.getConfiguration().isDatabaseConversionEnabled()) return;

		try {
			if (economiesDir.exists()) {
				getLogger().warning("Converting Economies to Database Storage...");

				for (File file : economiesDir.listFiles()) {
					if (file.isDirectory()) continue;

					FileConfiguration config = YamlConfiguration.loadConfiguration(file);
					String id = file.getName().split("\\.")[0];

					Economy e = (Economy) config.get(id);
					if (e == null) continue;
					e.saveEconomy();
				}

				deleteDir(economiesDir);
				Economy.getEconomies();
				getLogger().info("Economy Migration Complete!");
			}

			if (businessesDir.exists()) {
				getLogger().warning("Converting Businesses to Database Storage...");

				for (File file : businessesDir.listFiles()) {
					if (!file.isDirectory()) continue;

					Method read = Business.class.getDeclaredMethod("readFile", File.class);
					read.setAccessible(true);
					Business b = (Business) read.invoke(null, file);

					if (b == null) continue;
					b.saveBusiness();
				}

				deleteDir(businessesDir);
				Business.getBusinesses();
				getLogger().info("Business Migration Complete!");
			}

			if (corporationDir.exists()) {
				getLogger().warning("Converting Corporations to Database Storage...");

				for (File file : corporationDir.listFiles()) {
					if (!file.isDirectory()) continue;

					Method read = Corporation.class.getDeclaredMethod("readFile", File.class);
					read.setAccessible(true);
					Corporation c = (Corporation) read.invoke(null, file);

					if (c == null) continue;
					c.saveCorporation();
				}

				deleteDir(corporationDir);
				Corporation.getCorporations();
				getLogger().info("Corporation Migration Complete!");
			}

			if (playerDir.exists()) {
				getLogger().warning("Converting Players to Database Storage...");

				Method checkTable = NovaPlayer.class.getDeclaredMethod("checkTable");
				checkTable.setAccessible(true);
				checkTable.invoke(null);

				for (File file : playerDir.listFiles()) {
					if (file.isDirectory()) continue;
					if (!file.getName().endsWith(".yml")) continue;
					UUID id = UUID.fromString(file.getName().split("\\.")[0]);

					FileConfiguration config = YamlConfiguration.loadConfiguration(file);
					Map<String, Object> data = toMap(config.getValues(true));

					PlayerStatistics stats = (PlayerStatistics) data.get("stats");
					data.remove("stats");

					ByteArrayOutputStream os = new ByteArrayOutputStream();
					BukkitObjectOutputStream bOs = new BukkitObjectOutputStream(os);
					bOs.writeObject(data);
					bOs.close();

					ByteArrayOutputStream statsOs = new ByteArrayOutputStream();
					BukkitObjectOutputStream statsBOs = new BukkitObjectOutputStream(statsOs);
					statsBOs.writeObject(stats);
					statsBOs.close();

					String sql;
					try (ResultSet rs = db.createStatement().executeQuery("SELECT * FROM players WHERE id = \"" + id + "\"")) {
						if (rs.next())
							sql = "UPDATE players SET " +
									"id = ?, " +
									"data = ?, " +
									"stats = ? " +
									"WHERE id = \"" + id + "\"";
						else
							sql = "INSERT INTO players VALUES (?, ?, ?)";
					}
					PreparedStatement ps = db.prepareStatement(sql);

					ps.setString(1, id.toString());
					ps.setBytes(2, os.toByteArray());
					ps.setBytes(3, statsOs.toByteArray());
					ps.executeUpdate();
				}

				deleteDir(playerDir);

				getLogger().info("Player Migration Complete!");
			}

			if (marketFile.exists()) {
				getLogger().warning("Converting Market to Database Storage...");

				readMarketFile();
				writeMarketDB();
				marketFile.delete();

				getLogger().info("Market Migration Complete!");
			}
		} catch (Exception e) {
			NovaConfig.print(e);
		}
	}

	private void convertToFiles() {
		if (!NovaConfig.getConfiguration().isDatabaseConversionEnabled()) return;
		connectDB(false);

		try {
			if (db == null) return;
			DatabaseMetaData meta = db.getMetaData();

			try (ResultSet economies = meta.getTables(null, null, "economies", null)) {
				if (!NovaConfig.getEconomiesFolder().exists() && economies.first()) {
					NovaConfig.getEconomiesFolder().mkdir();
					getLogger().warning("Converting Economies to File Storage...");

					PreparedStatement ps = db.prepareStatement("SELECT * FROM economies");
					ResultSet result = ps.executeQuery();

					while (result.next()) {
						Method read = Economy.class.getDeclaredMethod("readDB", ResultSet.class);
						read.setAccessible(true);

						Economy e = (Economy) read.invoke(null, result);
						if (e == null) continue;
						e.saveEconomy();
					}

					Economy.getEconomies();
					getLogger().info("Economy Migration Complete!");
				}
			}

			try (ResultSet businesses = meta.getTables(null, null, "businesses", null)) {
				if (!NovaConfig.getBusinessesFolder().exists() && businesses.first()) {
					NovaConfig.getBusinessesFolder().mkdir();
					getLogger().warning("Converting Businesses to File Storage...");

					PreparedStatement ps = db.prepareStatement("SELECT * FROM businesses");
					ResultSet result = ps.executeQuery();

					while (result.next()) {
						Method read = Business.class.getDeclaredMethod("readDB", ResultSet.class);
						read.setAccessible(true);

						Business b = (Business) read.invoke(null, result);
						if (b == null) continue;
						b.saveBusiness();
					}

					Business.getBusinesses();
					getLogger().info("Business Migration Complete!");
				}
			}

			try (ResultSet corporations = meta.getTables(null, null, "corporations", null)) {
				if (!NovaConfig.getCorporationsFolder().exists() && corporations.first()) {
					NovaConfig.getCorporationsFolder().mkdir();
					getLogger().warning("Converting Corporations to File Storage...");

					PreparedStatement ps = db.prepareStatement("SELECT * FROM corporations");
					ResultSet rs = ps.executeQuery();

					while (rs.next()) {
						Method read = Corporation.class.getDeclaredMethod("readDB", ResultSet.class);
						read.setAccessible(true);

						Corporation c = (Corporation) read.invoke(null, rs);
						if (c == null) continue;
						c.saveCorporation();
					}

					Corporation.getCorporations();
					getLogger().info("Corporation Migration Complete!");
				}
			}

			try (ResultSet players = meta.getTables(null, null, "players", null)) {
				if (!NovaConfig.getPlayerDirectory().exists() && players.first()) {
					NovaConfig.getPlayerDirectory().mkdir();
					getLogger().warning("Converting Players to File Storage...");

					PreparedStatement ps = db.prepareStatement("SELECT * FROM players");
					ResultSet rs = ps.executeQuery();

					while (rs.next()) {
						File pFile = new File(NovaConfig.getPlayerDirectory(), rs.getString("id") + ".yml");
						if (!pFile.exists()) pFile.createNewFile();

						FileConfiguration config = YamlConfiguration.loadConfiguration(pFile);

						ByteArrayInputStream is = new ByteArrayInputStream(rs.getBytes("data"));
						BukkitObjectInputStream bIs = new BukkitObjectInputStream(is);
						Map<String, Object> pConfig = (Map<String, Object>) bIs.readObject();
						for (Map.Entry<String, Object> entry : pConfig.entrySet())
							config.set(entry.getKey(), entry.getValue());

						bIs.close();

						ByteArrayInputStream statsIs = new ByteArrayInputStream(rs.getBytes("stats"));
						BukkitObjectInputStream statsBIs = new BukkitObjectInputStream(statsIs);
						config.set("stats", statsBIs.readObject());
						statsBIs.close();

						config.save(pFile);
					}

					getLogger().info("Player Migration Complete!");
				}
			}

			try (ResultSet market = meta.getTables(null, null, "market", null)) {
				if (!NovaConfig.getMarketFile().exists() && market.first()) {
					getLogger().warning("Converting Market to File Storage...");
					readMarketDB();
					writeMarketFile();
					getLogger().info("Market Migration Complete!");
				}
			}
		} catch (Exception e) {
			NovaConfig.print(e);
		}

		disconnectDB();
	}

	private static void deleteDir(File dir) {
		if (dir.isDirectory())
			for (File child : dir.listFiles()) deleteDir(child);

		dir.delete();
	}

	private static Map<String, Object> toMap(Map<String, Object> serial) {
		for (Map.Entry<String, Object> entry : serial.entrySet())
			if (entry.getValue() instanceof ConfigurationSection)
				serial.put(entry.getKey(), toMap(((ConfigurationSection) entry.getValue()).getValues(true)));

		return serial;
	}

	/**
	 * Called when the Plugin enables
	 */
	@Override
	public void onEnable() {
		saveDefaultConfig();
		funcConfig = NovaConfig.loadFunctionalityFile();

		config = getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		loadFiles();

		getLogger().info("Loaded Files...");

		SERIALIZABLE.forEach(ConfigurationSerialization::registerClass);
		getLogger().info("Initialized Serializables...");

		prefix = get("plugin.prefix");

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
				.setSupportLink("https://discord.gg/WVFNWEvuqX")
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
		metrics.addCustomChart(new SimplePie("command_version", () -> String.valueOf(w.getCommandVersion())));
		metrics.addCustomChart(new SingleLineChart("economy_count", () -> Economy.getEconomies().size()));
		metrics.addCustomChart(new SingleLineChart("business_count", () -> Business.getBusinesses().size()));
		metrics.addCustomChart(new SingleLineChart("corporation_count", () -> Corporation.getCorporations().size()));
		metrics.addCustomChart(new SingleLineChart("bounty_count", () -> {
			AtomicInteger count = new AtomicInteger();
			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) count.addAndGet(new NovaPlayer(p).getOwnedBounties().size());
			return count.get();
		}));
		metrics.addCustomChart(new SimplePie("mysql_enabled", () -> String.valueOf(NovaConfig.getConfiguration().isDatabaseEnabled())));

		getLogger().info("Loaded Dependencies...");

		loadAddons();
		getLogger().info("Loaded Optional Hooks...");

		saveConfig();
		getLogger().info("Successfully loaded Novaconomy");
	}

	@Override
	public void onDisable() {
		writeMarket();

		SERIALIZABLE.forEach(ConfigurationSerialization::unregisterClass);
		for (Player p : Bukkit.getOnlinePlayers()) w.removePacketInjector(p);

		if (db != null) {
			disconnectDB();
			getLogger().info("Closed Database Connection...");
		}

		getLogger().info("Successfully disabled Novcaonomy");
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
	public boolean isDatabaseEnabled() {
		return config.getBoolean("Database.Enabled", false);
	}

	@Override
	public void setDatabaseEnabled(boolean enabled) {
		config.set("Database.Enabled", enabled);
		saveConfig();
	}

	@Override
	public Connection getDatabaseConnection() {
		return db;
	}

	@Override
	public boolean isDatabaseConversionEnabled() {
		return config.getBoolean("Database.Convert", true);
	}

	@Override
	public void setDatabaseConversionEnabled(boolean enabled) {
		config.set("Database.Convert", enabled);
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

	@Override
	public boolean isMarketEnabled() {
		return config.getBoolean("Market.Enabled", true);
	}

	@Override
	public void setMarketEnabled(boolean enabled) {
		config.set("Market.Enabled", enabled);
		saveConfig();
	}

	@Override
	public boolean isMarketRestockEnabled() {
		return config.getBoolean("Market.Restock.Enabled", true);
	}

	@Override
	public void setMarketRestockEnabled(boolean enabled) {
		config.set("Market.Restock.Enabled", enabled);
		saveConfig();
	}

	@Override
	public long getMarketRestockInterval() {
		return config.getLong("Market.Restock.IntervalTicks", 1728000);
	}

	@Override
	public void setMarketRestockInterval(long interval) {
		config.set("Market.Restock.IntervalTicks", interval);
		saveConfig();
	}

	@Override
	public long getMarketRestockAmount() {
		return config.getLong("Market.Restock.Base", 1000);
	}

	@Override
	public void setMarketRestockAmount(long amount) {
		config.set("Market.Restock.Base", amount);
		saveConfig();
	}

	@Override
	public boolean hasMarketBankInfluence() {
		return config.getBoolean("Market.Restock.BankInfluence", true);
	}

	@Override
	public void setMarketBankInfluence(boolean enabled) {
		config.set("Market.Restock.BankInfluence", enabled);
		saveConfig();
	}

	// TODO Create Base Prices
	static final Map<String, Double> basePrices = ImmutableMap.<String, Double>builder()
		.build();

    static final List<Receipt> purchases = new ArrayList<>();
	static final Map<Material, Long> stock = new HashMap<>();

	private void loadMarket() {
		readMarket();

		basePrices.keySet().stream().filter(m -> Material.matchMaterial(m) != null).map(Material::matchMaterial)
				.forEach(m -> stock.putIfAbsent(m, getMarketRestockAmount()));
	}

	private void writeMarket() {
		try {
			if (isDatabaseEnabled())
				writeMarketDB();
			else
				writeMarketFile();
		} catch (Exception e) {
			NovaConfig.print(e);
		} 
	}

	private void readMarket() {
		try {
			if (isDatabaseEnabled())
				readMarketDB();
			else
				readMarketFile();
		} catch (Exception e) {
			NovaConfig.print(e);
		} 
	}

	private static void writeMarketDB() throws SQLException, IOException {
		db.createStatement().execute("CREATE TABLE IF NOT EXISTS market (" +
				"material VARCHAR(255) NOT NULL, " +
				"purchases LONGBLOB NOT NULL, " +
				"stock BIGINT NOT NULL, " +
				"PRIMARY KEY (material))"
		);

		for (Material m : basePrices.keySet().stream().map(Material::matchMaterial).filter(Objects::nonNull).collect(Collectors.toList())) {
			String sql;
			PreparedStatement has = db.prepareStatement("SELECT * FROM market WHERE material = ?");
			has.setString(1, m.name());

			if (has.execute())
				sql = "UPDATE market SET material = ?, purchases = ?, stock = ? WHERE material = \"" + m.name() + "\"";
			else 
				sql = "INSERT INTO market VALUES (?, ?, ?)";
			
			PreparedStatement ps = db.prepareStatement(sql);
			ps.setString(1, m.name());

			ByteArrayOutputStream pOs = new ByteArrayOutputStream();
			BukkitObjectOutputStream pBos = new BukkitObjectOutputStream(pOs);
			pBos.writeObject(purchases.stream()
					.filter(r -> r.getPurchased() == m)
					.collect(Collectors.toList())
			);
			pBos.close();

			ps.setBytes(2, pOs.toByteArray());
			ps.setLong(3, stock.get(m));
			
			ps.executeUpdate();
			ps.close();
		}
		
	}

	private static void readMarketDB() throws SQLException, IOException, ClassNotFoundException {
		for (Material m : basePrices.keySet().stream().map(Material::matchMaterial).filter(Objects::nonNull).collect(Collectors.toList())) {
			PreparedStatement ps = db.prepareStatement("SELECT * FROM market WHERE material = ?");
			ps.setString(1, m.name());

			ResultSet rs = ps.executeQuery();

			if (!rs.next()) continue;
			
			ByteArrayInputStream pIs = new ByteArrayInputStream(rs.getBytes("purchases"));
			BukkitObjectInputStream pBis = new BukkitObjectInputStream(pIs);
			purchases.addAll((List<Receipt>) pBis.readObject());
			pBis.close();

			stock.put(m, rs.getLong("stock"));

			rs.close();
		}
	}

    private static void writeMarketFile() throws IOException {
        File marketFile = new File(NovaConfig.getDataFolder(), "market.dat");
		if (!marketFile.exists()) marketFile.createNewFile();

		FileOutputStream fos = new FileOutputStream(marketFile);
		BukkitObjectOutputStream bos = new BukkitObjectOutputStream(fos);
		bos.writeObject(purchases);
		bos.writeObject(stock);
		bos.close();
    }

    private static void readMarketFile() throws IOException, ClassNotFoundException {
		File marketFile = new File(NovaConfig.getDataFolder(), "market.dat");
		if (!marketFile.exists()) return;

		try {
			FileInputStream fis = new FileInputStream(marketFile);
			BukkitObjectInputStream bis = new BukkitObjectInputStream(fis);
			purchases.addAll((List<Receipt>) bis.readObject());
			stock.putAll((Map<Material, Long>) bis.readObject());
			bis.close();
		} catch (EOFException ignored) {} // File is empty
	}

	@Override
	public double getBasePrice(@NotNull Material m) throws IllegalArgumentException {
		if (m == null) throw new IllegalArgumentException("Material cannot be null");
		if (getBasePriceOverrides().containsKey(m)) return getBasePriceOverrides().get(m);
		
		double base = basePrices.getOrDefault(m.name().toLowerCase(), -1D);
		if (base == -1) throw new IllegalArgumentException("Material not sold on market");

		return base;
	}

    @Override
    public double getPrice(@NotNull Material m) throws IllegalArgumentException {
		double base = basePrices.getOrDefault(m.name().toLowerCase(), -1D);
		if (base == -1) throw new IllegalArgumentException("Material not sold on market");

		long purchasesLastRestock = purchases.stream()
				.filter(r -> r.getPurchased() == m)
				.filter(r -> r.getTimestamp().getTime() > System.currentTimeMillis() - (getMarketRestockInterval() * 500))
				.count();

		long stock = Novaconomy.stock.get(m);
		double istock = stock + purchasesLastRestock;
		
		return base * (1 + (1 - stock / istock) + (purchasesLastRestock / istock));
	}

	@Override
	public long getStock(@NotNull Material m) {
		return stock.get(m);
	}

    @Override
    public @NotNull Receipt buy(@NotNull OfflinePlayer buyer, @NotNull Material m, int amount, @NotNull Economy econ) throws IllegalArgumentException {
    	if (buyer == null) throw new IllegalArgumentException("Buyer cannot be null");
        if (m == null) throw new IllegalArgumentException("Material cannot be null");
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
		if (stock.get(m) < amount) throw new IllegalArgumentException("Insufficient stock");
	
	    NovaPlayer np = new NovaPlayer(buyer);

        double price = getPrice(m, econ) * amount;
        if (price <= 0) throw new IllegalArgumentException("Price must be positive");

        if (np.getBalance(econ) < price) throw new IllegalArgumentException("Insufficient funds");
        np.remove(econ, price);

	    Receipt r = new Receipt(m, price / amount, amount, buyer);
	    purchases.add(r);
	    stock.put(m, stock.get(m) - amount);

	    writeMarket();
	    return r;
    }

	@Override
	public Map<Material, Double> getBasePriceOverrides() {
		return config.getConfigurationSection("Market.BasePriceOverride").getValues(false)
				.entrySet().stream()
				.map(e -> {
					if (Material.matchMaterial(e.getKey()) == null) throw new IllegalArgumentException("Invalid Material '" + e.getKey() + "'");
					double d = Double.parseDouble(e.getValue().toString());
					if (d < 0) throw new IllegalArgumentException("Base price for '" + e.getKey() + "' must be positive");
					
					return new AbstractMap.SimpleEntry<>(Material.matchMaterial(e.getKey()), d);
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public void setBasePriceOverrides(@NotNull Map<Material, Double> overrides) throws IllegalArgumentException {
		if (overrides == null) throw new IllegalArgumentException("Overrides cannot be null");
		
		for (Material m : overrides.keySet()) {
			if (m == null) throw new IllegalArgumentException("Overrides cannot contain null material");
			if (overrides.get(m) == null) throw new IllegalArgumentException("Price cannot be null for '" + m + "'");
			if (overrides.get(m) < 0) throw new IllegalArgumentException("Price must be positive for '" + m + "'");

			config.set("Market.BasePriceOverride." + m.name(), overrides.get(m));
		}
		saveConfig();
	}

	@Override
	public void setBasePriceOverride(@NotNull Material m, double price) throws IllegalArgumentException {
		if (m == null) throw new IllegalArgumentException("Material cannot be null");
		if (price < 0) throw new IllegalArgumentException("Price must be positive for '" + m + "'");

		config.set("Market.BasePriceOverride." + m.name(), price);
		saveConfig();
	}

}