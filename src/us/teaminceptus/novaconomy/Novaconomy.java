package us.teaminceptus.novaconomy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.InterestEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerPayEvent;

/**
 * Class representing this Plugin
 * @see NovaConfig
 */
public class Novaconomy extends JavaPlugin implements NovaConfig {
	
	private static File playerDir;
	private static FileConfiguration economiesFile;
	
	private static FileConfiguration CONFIG;
	private static final ConfigurationSection INTEREST = CONFIG.getConfigurationSection("Interest");
	private static final ConfigurationSection NATURAL_CAUSES = CONFIG.getConfigurationSection("NaturalCauses");
	
	/**
	 * Send a Message as this Plugin
	 * @param sender Sender to send to
	 * @param message Message to send
	 */
	public static void sendPluginMessage(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.YELLOW + "[" + ChatColor.GOLD + "Novaconomy" + ChatColor.YELLOW + "] " + ChatColor.DARK_AQUA + message);
	}
	
	/**
	 * Send an Error as this Plugin
	 * @param sender Sender to send to
	 * @param error Erro to send
	 */
	public static void sendError(CommandSender sender, String error) {
		sendPluginMessage(sender, ChatColor.RED + error);
	}
	
	private class Events implements Listener {

		protected Events(Novaconomy plugin) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}

		static Random r = new Random();
		
		@EventHandler
		public void moneyIncrease(EntityDamageByEntityEvent e) {
			if (e.isCancelled()) return;
			if (!(e.getDamager() instanceof Player p)) return;
			if (!(e.getEntity() instanceof LivingEntity en)) return;
			if (en.getHealth() - e.getFinalDamage() > 0) return;

			NovaPlayer np = new NovaPlayer(p);

			for (Economy econ : Economy.getNaturalEconomies()) {
				double increase = en.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / r.nextInt(2) + 1.5;
				double previousBal = np.getBalance(econ);
				
				PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);

				Bukkit.getPluginManager().callEvent(event);
				if (!(event.isCancelled())) {
					np.add(econ, increase);
				}
			}
		}
	}
	
	private class Commands implements TabExecutor {

		protected Novaconomy plugin;

		protected Commands(Novaconomy plugin) {
			this.plugin = plugin;
			for (String name : plugin.getDescription().getCommands().keySet()) {
				plugin.getCommand(name).setExecutor(this);
				plugin.getCommand(name).setTabCompleter(this);
			}
		}

		public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
			List<String> suggestions = new ArrayList<>();

			switch (cmd.getName()) {
				case "ehelp": {
					suggestions.addAll(plugin.getDescription().getCommands().keySet());
					return suggestions;
				}
				case "convert": {
					switch (args.length) {
						case 1: {
							for (Economy econ : Economy.getEconomies()) suggestions.add(econ.getName());
							return suggestions;
						}
						case 2: {
							for (Economy econ : Economy.getEconomies()) {
								if (econ.getName().equals(args[0])) continue;
								suggestions.add(econ.getName());
							}
							return suggestions;
						}
					}
					return suggestions;
				}
				case "economy": {
					switch (args.length) {
						case 1: {
							suggestions.addAll(Arrays.asList("info"));
							
							if (sender.hasPermission("novaconomy.economy"))
							suggestions.addAll(Arrays.asList("create", "delete", "addbal", "removebal"));
							return suggestions;
						}
						case 2: {
							if (!(args[0].equalsIgnoreCase("create"))) {
								for (Economy econ : Economy.getEconomies()) suggestions.add(econ.getName());
							}
							return suggestions;
						}
						case 3: {
							if (args[0].equalsIgnoreCase("addbal") || args[0].equalsIgnoreCase("removebal")) {
								for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
							} else if (args[0].equalsIgnoreCase("create")) {
								suggestions.addAll(Arrays.asList("$", "%", "Q", "L", "P", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b"));
							}
							
							return suggestions;
						}
						case 4: {
							if (args[0].equalsIgnoreCase("create")) {
								for (Material m : Material.values()) suggestions.add("minecraft:" + m.name().toLowerCase());
							}
							return suggestions;
						}
						case 6: {
							if (args[0].equalsIgnoreCase("create")) {
								suggestions.addAll(Arrays.asList("true", "false"));
							}

							return suggestions;
						}
						
					}
					return suggestions;
				}
				case "pay": {
					switch (args.length) {
						case 1: {
							for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
							return suggestions;
						}
						case 2: {
							for (Economy econ : Economy.getEconomies()) suggestions.add(econ.getName());
							return suggestions;
						}
					}
					
					return suggestions;
				}
			}
			
			return suggestions;
		}
		
		
		
		public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
			switch (cmd.getName()) {
				case "ehelp": {
					List<String> commandInfo = new ArrayList<>();
					for (String name : plugin.getDescription().getCommands().keySet()) {
						PluginCommand pcmd = plugin.getCommand(name);
						
						if (pcmd.getPermission() != null && !(sender.hasPermission(pcmd.getPermission()))) continue;

						if (sender.isOp()) {
							commandInfo.add(ChatColor.GOLD + pcmd.getUsage() + ChatColor.WHITE + " - " + ChatColor.GREEN + pcmd.getDescription() + ChatColor.WHITE + " | " + ChatColor.BLUE + (pcmd.getPermission() == null ? "No Permissions" : pcmd.getPermission()));
						} else {
							commandInfo.add(ChatColor.GOLD + pcmd.getUsage() + ChatColor.WHITE + " - " + ChatColor.GREEN + pcmd.getDescription());
						}
					}

					String msg = ChatColor.GOLD + "" + ChatColor.UNDERLINE + "Commands\n\n" + String.join("\n", commandInfo.toArray(new String[]{}));

					sender.sendMessage(msg);
					break;
				}
				case "balance": {
					if (!(sender instanceof Player p)) return false;
					if (Economy.getEconomies().size() < 1) {
						p.sendMessage(ChatColor.RED + "There are no economies! Ask an Admin to create one!");
						return true;
					}
					NovaPlayer np = new NovaPlayer(p);
					
					List<String> balanceInfo = new ArrayList<>();

					for (Economy econ : Economy.getEconomies()) {
						balanceInfo.add(ChatColor.GOLD + econ.getName() + ChatColor.AQUA + " - " + ChatColor.GREEN + econ.getSymbol() + Double.toString(Math.floor(np.getBalance(econ) * 100) / 100));
					}

					p.sendMessage(ChatColor.GOLD + "" + ChatColor.UNDERLINE + "Balances\n" + String.join("\n", balanceInfo.toArray(new String[]{})));
					break;
				}
				case "novaconomyreload": {
					sender.sendMessage(ChatColor.GOLD + "Reloading...");
					plugin.reloadConfig();
					plugin.saveConfig();
					sender.sendMessage(ChatColor.GOLD + "Reloaded Config! You need to restart/reload your server to update the JAR file.");
				}
				case "convert": {
					if (!(sender instanceof Player p)) return false;
					if (Economy.getEconomies().size() < 1) {
						p.sendMessage(ChatColor.RED + "There are no economies! Ask an Admin to create one!");
						return true;
					}
					NovaPlayer np = new NovaPlayer(p);
					try {
						if (args.length < 1) {
							Novaconomy.sendError(p, "Please provide an economy to transfer from.");
							return false;
						}
		
						if (Economy.getEconomy(args[0]) == null) {
							Novaconomy.sendError(p, "Please provide a valid economy to transfer from.");
							return false;
						}
		
						Economy from = Economy.getEconomy(args[0]);
		
						if (args.length < 2) {
							Novaconomy.sendError(p, "Please provide an economy to transfer to.");
							return false;
						}
		
						if (Economy.getEconomy(args[1]) == null) {
							Novaconomy.sendError(p, "Please provide a valid economy to transfer to.");
							return false;
						}
		
						Economy to = Economy.getEconomy(args[1]);
		
						if (args.length < 3) {
							Novaconomy.sendError(p, "Please provide an amount to transfer.");
							return false;
						}

						double amount = Double.parseDouble(args[2]);

						if (amount <= 0) {
							Novaconomy.sendError(p, "Please provide a valid amount to transfer.");
							return false;
						}

						if (np.getBalance(from) < amount) {
							Novaconomy.sendError(p, "You do not have enough money to convert!");
							return false;
						}	

						double toBal = from.convertAmount(to, amount);

						np.remove(from, amount);
						np.add(to, toBal);
						p.sendMessage(ChatColor.GREEN + "Successfully converted " + ChatColor.GOLD + from.getSymbol() + Double.toString(amount) + ChatColor.GREEN + " to " + ChatColor.GOLD + to.getSymbol() + Double.toString(Math.floor(toBal * 100) / 100) + ChatColor.GREEN + "!");
					} catch (NumberFormatException e) {
						Novaconomy.sendError(p, "Please provide a valid amount to transfer.");
						return false;
					}
					break;
				}
				case "pay": {
					if (!(sender instanceof Player p)) return false;
					if (Economy.getEconomies().size() < 1) {
						p.sendMessage(ChatColor.RED + "There are no economies! Ask an Admin to create one!");
						return true;
					}
					NovaPlayer np = new NovaPlayer(p);
					try {

						if (args.length < 1) {
							Novaconomy.sendError(p, "Please provide a valid player.");
							return false;
						}
		
						if (Bukkit.getPlayer(args[0]) == null) {
							Novaconomy.sendError(p, "This player does not exist or is not online.");
							return false;
						}
		
						Player target = Bukkit.getPlayer(args[0]);
						NovaPlayer nt = new NovaPlayer(target);
						
						if (args.length < 2) {
							Novaconomy.sendError(p, "Please provide an economy.");
							return false;
						}

						if (Economy.getEconomy(args[1]) == null) {
							Novaconomy.sendError(p, "Please provide a valid economy.");
							return false;
						}

						Economy econ = Economy.getEconomy(args[1]);
						
						if (args.length < 3) {
							Novaconomy.sendError(p, "Please provide an amount to pay.");
							return false;
						}

						double amount = Double.parseDouble(args[2]);
						
						if (np.getBalance(econ) < amount) {
							Novaconomy.sendError(p, "You do not have enough money to pay this player!");
							return false;
						}

						PlayerPayEvent e = new PlayerPayEvent(p, target, econ, amount, nt.getBalance(econ), nt.getBalance(econ) + amount);

						Bukkit.getPluginManager().callEvent(e);

						if (!(e.isCancelled())) {
							np.remove(econ, amount);
							nt.add(econ, amount);

							target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD + "+" + Double.toString(Math.floor(e.getAmount() * 100) / 100) + ChatColor.GREEN + " from " + ChatColor.GOLD + e.getPayer().getName()));
							target.sendMessage(ChatColor.GREEN + "Received " + ChatColor.GOLD + econ.getSymbol() + Double.toString(Math.floor(e.getAmount() * 100) / 100) + ChatColor.GREEN + " from " + ChatColor.GOLD + e.getPayer().getName());
							target.playSound(target, Sound.ENTITY_ARROW_HIT_PLAYER, 3F, 2F);
						}
					} catch (NumberFormatException e) {
						Novaconomy.sendError(p, "Please provide a valid amount to pay.");
						return false;
					}
					break;
				}
				case "economy": {
					if (args.length < 1) {
						Novaconomy.sendError(sender, "Please provide valid arguments.");
						return false;
					}

					switch (args[0].toLowerCase()) {
						case "create": {
							if (!(sender.hasPermission("novaconomy.economy.create"))) {
								sender.sendMessage(ChatColor.RED + "You do not have access to this argument.");
								return true;
							}
							try {
								if (args.length < 2) {
									Novaconomy.sendError(sender, "Please provide a valid name.");
									return false;
								}
		
								for (Economy econ : Economy.getEconomies()) 
									if (econ.getName().equals(args[1])) {
										Novaconomy.sendError(sender, "This Economy already exists.");
										return false;
									}
		
								String name = args[1];
		
								if (args.length < 3) {
									Novaconomy.sendError(sender, "Please provide a valid symbol.");
									return false;
								}
		
								if (args[2].length() > 1) {
									Novaconomy.sendError(sender, "Please provide a valid symbol. Must be one character long.");
									return false;
								}
		
								char symbol = args[2].charAt(0);
		
								if (args.length < 4) {
									Novaconomy.sendError(sender, "Please provide a valid icon material.");
									return false;
								}

								Material icon = Material.valueOf(args[3].replaceAll("minecraft:", "").toUpperCase());

								if (args.length < 5) {
									Novaconomy.sendError(sender, "Please provide a valid conversion scale.");
									return false;
								}

								double scale = Double.parseDouble(args[4]);
								
								boolean naturalIncrease = true;
								
								if (!(args.length < 6)) {
									if (!(args[5].equalsIgnoreCase("true")) && !(args[5].equalsIgnoreCase("false"))) {
										Novaconomy.sendError(sender, "Please provide true or false for Natural Increase");
										return false;
									}

									naturalIncrease = Boolean.parseBoolean(args[5]);
								}
								
									Economy.builder().setName(name).setSymbol(symbol).setIcon(icon).setIncreaseNaturally(naturalIncrease).setConversionScale(scale).build();
								
								sender.sendMessage(ChatColor.GREEN + "Economy successfully created!");
							} catch (IllegalArgumentException e) {
								Novaconomy.sendError(sender, "Please provide a valid icon material.");
								return false;
							}
							break;
						}
						case "delete": {
							if (!(sender.hasPermission("novaconomy.economy.delete"))) {
								sender.sendMessage(ChatColor.RED + "You do not have access to this argument.");
								return true;
							}
							if (args.length < 2) {
								Novaconomy.sendError(sender, "Please provide an economy to delete.");
								return false;
							}

							if (Economy.getEconomy(args[1]) == null) {
								Novaconomy.sendError(sender, "This economy does not exist.");
								return false;
							}

							Economy econ = Economy.getEconomy(args[1]);
							String name = econ.getName();
							
							sender.sendMessage(ChatColor.GREEN + "Deleting " + name + "...");
							Economy.removeEconomy(econ);
							sender.sendMessage(ChatColor.GREEN + "Successfully deleted " + name + "!");
							break;
						}
						case "info": {
							if (!(sender.hasPermission("novaconomy.economy.info"))) {
								sender.sendMessage(ChatColor.RED + "You do not have access to this argument.");
								return true;
							}
							if (args.length < 2) {
								Novaconomy.sendError(sender, "Please provide an economy to query about.");
								return false;
							}

							if (Economy.getEconomy(args[1]) == null) {
								Novaconomy.sendError(sender, "This economy does not exist.");
								return false;
							}

							Economy econ = Economy.getEconomy(args[1]);

							String[] components = {
								ChatColor.GOLD + "" + ChatColor.UNDERLINE + econ.getName() + " Information",
								ChatColor.DARK_AQUA + "Increase Naturally: " + ChatColor.DARK_GREEN + Boolean.toString(econ.hasNaturalIncrease()),
								ChatColor.DARK_AQUA + "Symbol: " + ChatColor.DARK_GREEN + econ.getSymbol(),
								ChatColor.DARK_AQUA + "Conversion Scale: " + ChatColor.DARK_GREEN + "x" + Double.toString(Math.floor(econ.getConversionScale() * 100) / 100),
							};

							sender.sendMessage(String.join("\n", components));
							break;
						}
						case "addbal": {
							if (!(sender.hasPermission("novaconomy.economy.addbalance"))) {
								sender.sendMessage(ChatColor.RED + "You do not have access to this argument.");
								return true;
							}
							try {
								if (args.length < 2) {
									Novaconomy.sendError(sender, "Please provide a valid economy.");
									return false;
								}
		
								if (Economy.getEconomy(args[1]) == null) {
									Novaconomy.sendError(sender, "This economy does not exist.");
									return false;
								}
		
								Economy econ = Economy.getEconomy(args[1]);
		
								if (args.length < 3) {
									Novaconomy.sendError(sender, "Please provide a valid player.");
									return false;
								}
		
								if (Bukkit.getPlayer(args[2]) == null) {
									Novaconomy.sendError(sender, "This player does not exist or is not online.");
									return false;
								}
		
								Player target = Bukkit.getPlayer(args[2]);
								NovaPlayer nt = new NovaPlayer(target);
		
								if (args.length < 4) {
									Novaconomy.sendError(sender, "Please provide an amount to add.");
									return false;
								}

								double add = Double.parseDouble(args[3]);

								if (add <= 0) {
									Novaconomy.sendError(sender, "Please provide a valid amount to add.");
									return false;
								}

								nt.add(econ, add);
								sender.sendMessage(ChatColor.GREEN + "Successfully added " + ChatColor.GOLD + econ.getSymbol() + args[3] + ChatColor.GREEN + " to " + ChatColor.GOLD + target.getName() + "'s " + ChatColor.GREEN + "balance.");
							} catch (NumberFormatException e) {
								Novaconomy.sendError(sender, "Please provide a valid amount to add.");
							}
							break;
						}
						case "removebal": {	
							if (!(sender.hasPermission("novaconomy.economy.removebalance"))) {
								sender.sendMessage(ChatColor.RED + "You do not have access to this argument.");
								return true;
							}
							try {
								if (args.length < 2) {
									Novaconomy.sendError(sender, "Please provide a valid economy.");
									return false;
								}
		
								if (Economy.getEconomy(args[1]) == null) {
									Novaconomy.sendError(sender, "This economy does not exist.");
									return false;
								}
		
								Economy econ = Economy.getEconomy(args[1]);
		
								if (args.length < 3) {
									Novaconomy.sendError(sender, "Please provide a valid player.");
									return false;
								}
		
								if (Bukkit.getPlayer(args[2]) == null) {
									Novaconomy.sendError(sender, "This player does not exist or is not online.");
									return false;
								}
		
								Player target = Bukkit.getPlayer(args[2]);
								NovaPlayer nt = new NovaPlayer(target);
		
								if (args.length < 4) {
									Novaconomy.sendError(sender, "Please provide an amount to remove.");
									return false;
								}

								double remove = Double.parseDouble(args[3]);

								if (remove <= 0) {
									Novaconomy.sendError(sender, "Please provide a valid amount to remove.");
									return false;
								}

								nt.remove(econ, remove);
								sender.sendMessage(ChatColor.GREEN + "Successfully removed " + ChatColor.GOLD + econ.getSymbol() + args[3] + ChatColor.GREEN + " from " + ChatColor.GOLD + target.getName() + "'s " + ChatColor.GREEN + "balance.");
							} catch (NumberFormatException e) {
								Novaconomy.sendError(sender, "Please provide a valid amount to remove.");
								return false;
							}
							break;
						}
						case "interest": {
							if (args.length < 1) {
								Novaconomy.sendError(sender, "Please provide valid arguments.");
								return false;
							}
							
							switch (args[0].toLowerCase()) {
								case "enable": {
									
									break;
								}
								default: {
									Novaconomy.sendError(sender, "Please provide valid arugments.");
									return false;
								}
							}
							break;
						}
						default: {
							Novaconomy.sendError(sender, "Please provide valid arguments.");
							return false;
						}
					}
					
					break;
				}
				default: {
					return true;
				}
			}

			return true;
		}


	}
	
	private static BukkitRunnable INTEREST_RUNNABLE = new BukkitRunnable() {
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
	
	/**
	 * Fetch the Configuration Wrapper
	 * @return {@link NovaConfig} configuration
	 */
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
		NATURAL_CAUSES.set("FishingIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setMiningChance(int chance) {
		NATURAL_CAUSES.set("MiningChanceIncrease", chance);
		saveConfig();
	}

	@Override
	public void setFarmingChance(int chance) {
		NATURAL_CAUSES.set("FarmingIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setFarmingIncrease(boolean increase) {
		NATURAL_CAUSES.set("FarmingIncrease", increase);
		saveConfig();
	}

	@Override
	public void setMiningIncrease(boolean increase) {
		NATURAL_CAUSES.set("MiningIncrease", increase);
		saveConfig();
	}

	@Override
	public void setKillIncrease(boolean increase) {
		NATURAL_CAUSES.set("KillIncrease", increase);
	}

	@Override
	public void setDeathDecrease(boolean decrease) {
		NATURAL_CAUSES.set("DeathDecrease", decrease);
		saveConfig();
	}

}