package us.teaminceptus.novaconomy;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.InterestEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerPayEvent;

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

	private class Events implements Listener {
		
		private Novaconomy plugin;
		
		protected Events(Novaconomy plugin) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
			this.plugin = plugin;
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

			NovaPlayer np = new NovaPlayer(p);
			
			List<BaseComponent> added = new ArrayList<>();

			int size = Economy.getNaturalEconomies().size();
			
			for (Economy econ : Economy.getNaturalEconomies()) {
				double divider = r.nextInt(2) + 1.5;
				double increase = (en.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / divider) / econ.getConversionScale();
				
				double previousBal = np.getBalance(econ);
				
				PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);

				Bukkit.getPluginManager().callEvent(event);
				if (!(event.isCancelled())) {
					np.add(econ, increase);
					
					TextComponent message = new TextComponent(ChatColor.GREEN + "+" + Math.floor(increase * 100) / 100 + econ.getSymbol() + (size == 1 ? "" : ", "));
					added.add(message);
				}
			}
			
			if (plugin.hasNotifications()) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, added.toArray(new BaseComponent[0]));
		}
		
		@EventHandler
		public void moneyIncrease(BlockBreakEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasMiningIncrease())) return;
			if (!(e.isDropItems())) return;
			
			Block b = e.getBlock();
			Player p = e.getPlayer();
			NovaPlayer np = new NovaPlayer(p);
			
			if (!(ores.contains(b.getType()))) return;
			
			List<BaseComponent> added = new ArrayList<>();
			
			for (Economy econ : Economy.getNaturalEconomies()) {
				double divider = r.nextInt(2) + 1.25;
				double increase = ((e.getExpToDrop() + r.nextInt(3) + 1) / divider) / econ.getConversionScale();
				
				double previousBal = np.getBalance(econ);
				
				PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);
				
				Bukkit.getPluginManager().callEvent(event);
				if (!(event.isCancelled())) {
					np.add(econ, increase);
					
					TextComponent message = new TextComponent(ChatColor.GREEN + "+" + Math.floor(increase * 100) / 100 + econ.getSymbol() + ", ");
					added.add(message);
				}
			}
			
			if (plugin.hasNotifications()) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, added.toArray(new BaseComponent[0]));
		}
		
		@EventHandler
		public void moneyIncrease(PlayerFishEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasFishingIncrease())) return;
			
			if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
			
			Player p = e.getPlayer();
			NovaPlayer np = new NovaPlayer(p);
			
			List<BaseComponent> added = new ArrayList<>();
			
			for (Economy econ : Economy.getNaturalEconomies()) {
				double divider = r.nextInt(2) + 1;
				double increase = ((e.getExpToDrop() + r.nextInt(8) + 1) / divider) / econ.getConversionScale();
				
				double previousBal = np.getBalance(econ);
				
				PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);
				
				Bukkit.getPluginManager().callEvent(event);
				if (!(event.isCancelled())) {
					np.add(econ, increase);
					
					TextComponent message = new TextComponent(ChatColor.GREEN + "+" + Math.floor(increase * 100) / 100 + econ.getSymbol() + ", ");
					added.add(message);
				}
				
			}
			
			if (plugin.hasNotifications()) p.spigot().sendMessage(ChatMessageType.ACTION_BAR, added.toArray(new BaseComponent[0]));
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
		addAll(Arrays.stream(Material.values()).filter(m -> m.name().endsWith("ORE")).collect(Collectors.toSet()));
	}};
	
	
	private static class Commands implements TabExecutor {

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
							
							if (sender.hasPermission("novaconomy.economy")) suggestions.addAll(Arrays.asList("create", "delete", "addbal", "removebal", "setbal"));
							return suggestions;
						}
						case 2: {
							if (!(args[0].equalsIgnoreCase("create"))) {
								for (Economy econ : Economy.getEconomies()) suggestions.add(econ.getName());
							}
							return suggestions;
						}
						case 3: {
							if (args[0].toLowerCase().contains("bal")) {
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

					String msg = get("constants.commands") + "\n\n" + String.join("\n", commandInfo.toArray(new String[]{}));

					sender.sendMessage(msg);
					break;
				}
				case "balance": {
					if (!(sender instanceof Player)) return false;
					Player p = (Player) sender;
					if (Economy.getEconomies().size() < 1) {
						p.sendMessage(getMessage("error.economy.none"));
						return true;
					}
					NovaPlayer np = new NovaPlayer(p);
					
					List<String> balanceInfo = new ArrayList<>();

					for (Economy econ : Economy.getEconomies()) {
						balanceInfo.add(ChatColor.GOLD + econ.getName() + ChatColor.AQUA + " - " + ChatColor.GREEN + econ.getSymbol() + Math.floor(np.getBalance(econ) * 100) / 100);
					}

					p.sendMessage(get("command.balance.balances") + "\n" + String.join("\n", balanceInfo.toArray(new String[]{})));
					break;
				}
				case "novaconomyreload": {
					sender.sendMessage(get("command.reload.reloading"));
					plugin.reloadConfig();
					plugin.reloadValues();
					Novaconomy.updateInterest();
					sender.sendMessage(get("command.reload.success"));
					break;
				}
				case "convert": {
					if (!(sender instanceof Player)) return false;
					Player p = (Player) sender;

					if (Economy.getEconomies().size() < 1) {
						p.sendMessage(getMessage("error.economy.none"));
						return true;
					}
					NovaPlayer np = new NovaPlayer(p);
					try {
						if (args.length < 1) {
							p.sendMessage(getMessage("error.economy.transfer_from"));
							return false;
						}
		
						if (Economy.getEconomy(args[0]) == null) {
							p.sendMessage(getMessage("error.economy.transfer_from"));
							return false;
						}
		
						Economy from = Economy.getEconomy(args[0]);
		
						if (args.length < 2) {
							p.sendMessage(getMessage("error.economy.transfer_to"));
							return false;
						}
		
						if (Economy.getEconomy(args[1]) == null) {
							p.sendMessage(getMessage("error.economy.transfer_to"));
							return false;
						}
		
						Economy to = Economy.getEconomy(args[1]);
		
						if (args.length < 3) {
							p.sendMessage(getMessage("error.economy.transfer_amount"));
							return false;
						}

						double amount = Double.parseDouble(args[2]);

						if (amount <= 0) {
							p.sendMessage(getMessage("error.economy.transfer_amount"));
							return false;
						}

						if (np.getBalance(from) < amount) {
							p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), ChatColor.RED + get("constants.convert")));
							return false;
						}

						double toBal = from.convertAmount(to, amount);

						np.remove(from, amount);
						np.add(to, toBal);
						p.sendMessage(String.format(getMessage("success.economy.convert"), (from.getSymbol() + "") + amount + "", (to.getSymbol() + "") + Math.floor(toBal * 100) / 100) + "");
					
					} catch (NumberFormatException e) {
						p.sendMessage(getMessage("error.economy.transfer_amount"));
						return false;
					} catch (IllegalArgumentException e) {
						p.sendMessage(getMessage("error.economy.transfer_same"));
						return false;
					}
					break;
				}
				case "pay": {
					if (!(sender instanceof Player)) return false;
					Player p = (Player) sender;
					if (Economy.getEconomies().size() < 1) {
						p.sendMessage(getMessage("error.economy.none"));
						return true;
					}
					NovaPlayer np = new NovaPlayer(p);
					try {

						if (args.length < 1) {
							p.sendMessage(getMessage("error.argument.player"));
							return false;
						}
		
						if (Bukkit.getPlayer(args[0]) == null) {
							p.sendMessage(getMessage("error.player.offline"));
							return false;
						}
		
						Player target = Bukkit.getPlayer(args[0]);
						NovaPlayer nt = new NovaPlayer(target);
						
						if (target.getUniqueId().equals(p.getUniqueId())) {
							p.sendMessage(getMessage("error.economy.pay_self"));
							return false;
						}
						
						if (args.length < 2) {
							p.sendMessage(getMessage("error.argument.economy"));
							return false;
						}

						if (Economy.getEconomy(args[1]) == null) {
							p.sendMessage(getMessage("error.argument.economy"));
							return false;
						}

						Economy econ = Economy.getEconomy(args[1]);
						
						if (args.length < 3) {
							p.sendMessage(getMessage("error.argument.pay_amount"));
							return false;
						}

						double amount = Double.parseDouble(args[2]);
						
						if (np.getBalance(econ) < amount) {
							p.sendMessage(getMessage("error.economy.invalid_amount_pay"));
							return false;
						}

						PlayerPayEvent e = new PlayerPayEvent(p, target, econ, amount, nt.getBalance(econ), nt.getBalance(econ) + amount);

						Bukkit.getPluginManager().callEvent(e);

						if (!(e.isCancelled())) {
							np.remove(econ, amount);
							nt.add(econ, amount);

							target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(String.format(getMessage("success.economy.receive_actionbar"), Math.floor(e.getAmount() * 100) / 100, e.getPayer().getName())));
							target.sendMessage(String.format(getMessage("success.economy.receive"), econ.getSymbol() + Math.floor(e.getAmount() * 100) / 100 + "", e.getPayer().getName() + ""));
							target.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 3F, 2F);
						}
					} catch (NumberFormatException e) {
						p.sendMessage(getMessage("error.argument.pay_amount"));
						return false;
					}
					break;
				}
				case "economy": {
					if (args.length < 1) {
						sender.sendMessage(getMessage("error.argument"));
						return false;
					}

					switch (args[0].toLowerCase()) {
						case "create": {
							if (!(sender.hasPermission("novaconomy.economy.create"))) {
								sender.sendMessage(getMessage("error.permission.argument"));
								return true;
							}
							try {
								if (args.length < 2) {
									sender.sendMessage(getMessage("error.argument.name"));
									return false;
								}
		
								for (Economy econ : Economy.getEconomies()) 
									if (econ.getName().equals(args[1])) {
										sender.sendMessage(getMessage("error.economy.exists"));
										return false;
									}
		
								String name = args[1];
		
								if (args.length < 3) {
									sender.sendMessage(getMessage("error.argument.symbol"));
									return false;
								}
		
								if (args[2].length() > 1) {
									sender.sendMessage(getMessage("error.argument.symbol"));
									return false;
								}
		
								char symbol = args[2].charAt(0);
		
								if (args.length < 4) {
									sender.sendMessage(getMessage("error.argument.icon"));
									return false;
								}

								Material icon = Material.valueOf(args[3].replaceAll("minecraft:", "").toUpperCase());

								if (args.length < 5) {
									sender.sendMessage(getMessage("error.argument.scale"));
									return false;
								}

								double scale = Double.parseDouble(args[4]);
								
								boolean naturalIncrease = true;
								
								if (!(args.length < 6)) {
									if (!(args[5].equalsIgnoreCase("true")) && !(args[5].equalsIgnoreCase("false"))) {
										sender.sendMessage(getMessage("error.argument.bool"));
										return false;
									}

									naturalIncrease = Boolean.parseBoolean(args[5]);
								}
								
								Economy.builder().setName(name).setSymbol(symbol).setIcon(icon).setIncreaseNaturally(naturalIncrease).setConversionScale(scale).build();
								
								sender.sendMessage(getMessage("success.economy.create"));
							} catch (IllegalArgumentException e) {
								sender.sendMessage(getMessage("error.argument.icon"));
								return false;
							}
							break;
						}
						case "delete": {
							if (!(sender.hasPermission("novaconomy.economy.delete"))) {
								sender.sendMessage(getMessage("error.permission.argument"));
								return true;
							}
							if (args.length < 2) {
								sender.sendMessage(getMessage("error.argument.economy"));
								return false;
							}

							if (Economy.getEconomy(args[1]) == null) {
								sender.sendMessage(getMessage("error.economy.inexistent"));
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
								sender.sendMessage(getMessage("error.permission.argument"));
								return true;
							}
							if (args.length < 2) {
								sender.sendMessage(getMessage("error.argument.economy"));
								return false;
							}

							if (Economy.getEconomy(args[1]) == null) {
								sender.sendMessage(getMessage("error.economy.inexistent"));
								return false;
							}

							Economy econ = Economy.getEconomy(args[1]);

							String[] components = {
								String.format(get("constants.economy.info"), econ.getName()),
								String.format(get("constants.economy.natural_increase"), econ.hasNaturalIncrease() + ""),
								String.format(get("constants.economy.symbol"), econ.getSymbol() + ""),
								String.format(get("constants.economy.scale"), Math.floor(econ.getConversionScale() * 100) / 100 + ""),
							};

							sender.sendMessage(String.join("\n", components));
							break;
						}
						case "addbal": {
							if (!(sender.hasPermission("novaconomy.economy.addbalance"))) {
								sender.sendMessage(getMessage("error.permission.argument"));
								return true;
							}
							try {
								if (args.length < 2) {
									sender.sendMessage(getMessage("error.argument.economy"));
									return false;
								}
		
								if (Economy.getEconomy(args[1]) == null) {
									sender.sendMessage(getMessage("error.economy.inexistent"));
									return false;
								}
		
								Economy econ = Economy.getEconomy(args[1]);
		
								if (args.length < 3) {
									sender.sendMessage(getMessage("error.argument.player"));
									return false;
								}
		
								if (Bukkit.getPlayer(args[2]) == null) {
									sender.sendMessage(getMessage("error.player.offline"));
									return false;
								}
		
								Player target = Bukkit.getPlayer(args[2]);
								NovaPlayer nt = new NovaPlayer(target);
		
								if (args.length < 4) {
									sender.sendMessage(getMessage("error.argument.amount"));
									return false;
								}

								double add = Double.parseDouble(args[3]);

								if (add <= 0) {
									sender.sendMessage(getMessage("error.argument.amount"));
									return false;
								}

								nt.add(econ, add);
								sender.sendMessage(String.format(getMessage("success.economy.addbalance"),  econ.getSymbol() + "", args[3], target.getName()));
							} catch (NumberFormatException e) {
								sender.sendMessage(getMessage("error.argument.amount"));
							}
							break;
						}
						case "removebal": {	
							if (!(sender.hasPermission("novaconomy.economy.removebalance"))) {
								sender.sendMessage(getMessage("error.permission.argument"));
								return true;
							}
							try {
								if (args.length < 2) {
									sender.sendMessage(getMessage("error.argument.economy"));
									return false;
								}
		
								if (Economy.getEconomy(args[1]) == null) {
									sender.sendMessage(getMessage("error.economy.inexistent"));
									return false;
								}
		
								Economy econ = Economy.getEconomy(args[1]);
		
								if (args.length < 3) {
									sender.sendMessage(getMessage("error.argument.player"));
									return false;
								}
		
								if (Bukkit.getPlayer(args[2]) == null) {
									sender.sendMessage(getMessage("error.player.offline"));
									return false;
								}
		
								Player target = Bukkit.getPlayer(args[2]);
								NovaPlayer nt = new NovaPlayer(target);
		
								if (args.length < 4) {
									sender.sendMessage(getMessage("error.argument.amount"));
									return false;
								}

								double remove = Double.parseDouble(args[3]);

								if (remove <= 0) {
									sender.sendMessage(getMessage("error.argument.amount"));
									return false;
								}

								nt.remove(econ, remove);
								sender.sendMessage(String.format(getMessage("success.economy.removebalance"), econ.getSymbol() + "", args[3], target.getName()));
							} catch (NumberFormatException e) {
								sender.sendMessage(getMessage("error.argument.amount"));
								return false;
							}
							break;
						}
						case "setbal": {
							if (!(sender.hasPermission("novaconomy.economy.addbalance")) && !(sender.hasPermission("novaconomy.economy.removebalance"))) {
								sender.sendMessage(getMessage("error.permission.argument"));
								return true;
							}
							try {
								if (args.length < 2) {
									sender.sendMessage(getMessage("error.argument.economy"));
									return false;
								}
		
								if (Economy.getEconomy(args[1]) == null) {
									sender.sendMessage(getMessage("error.economy.inexistent"));
									return false;
								}
		
								Economy econ = Economy.getEconomy(args[1]);
		
								if (args.length < 3) {
									sender.sendMessage(getMessage("error.argument.player"));
									return false;
								}
		
								if (Bukkit.getPlayer(args[2]) == null) {
									sender.sendMessage(getMessage("error.player.offline"));
									return false;
								}
		
								Player target = Bukkit.getPlayer(args[2]);
								NovaPlayer nt = new NovaPlayer(target);
		
								if (args.length < 4) {
									sender.sendMessage(getMessage("error.argument.amount"));
									return false;
								}

								double newBal = Double.parseDouble(args[3]);

								if (newBal < 0) {
									sender.sendMessage(getMessage("error.argument.amount"));
									return false;
								}

								nt.setBalance(econ, newBal);
								sender.sendMessage(String.format(getMessage("success.economy.setbalance"), target.getName(), econ.getName(), newBal + ""));
							} catch (NumberFormatException e) {
								sender.sendMessage(getMessage("error.argument.amount"));
								return false;
							}
							break;
						}
						case "interest": {
							if (args.length < 2) {
								sender.sendMessage(getMessage("error.argument"));
								return false;
							}
							
							switch (args[1].toLowerCase()) {
								case "enable": {
									NovaConfig.getConfiguration().setInterestEnabled(true);
									sender.sendMessage(getMessage("success.economy.enable_interest"));
									break;
								}
								case "disable": {
									NovaConfig.getConfiguration().setInterestEnabled(false);
									sender.sendMessage(getMessage("success.economy.disable_interest"));
									break;
								}
								default: {
									sender.sendMessage(getMessage("error.argument"));
									return false;
								}
							}
							break;
						}
						default: {
							sender.sendMessage(getMessage("error.argument"));
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
			if (!(event.isCancelled())) {
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
	
	public void onEnable() {
		saveDefaultConfig();
		saveConfig();

		for (Language l : Language.values()) {
			File f = new File(getDataFolder(), "novaconomy" + ( l.getIdentifier().length() == 0 ? "" : "_" + l.getIdentifier() ) + ".properties");

			if (!(f.exists())) {
				saveResource("novaconomy" + (l.getIdentifier().length() == 0 ? "" : "_" + l.getIdentifier()) + ".properties", false);
			}

			getLogger().info("Loaded Language " + l.name() + "...");
		}

		ConfigurationSerialization.registerClass(Economy.class);

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

		new Commands(this);
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
		// Config Checks
		FileConfiguration config = getConfig();
		
		if (!(config.isBoolean("Notifications"))) {
			config.set("Notifications", true);
		}

		if (!(config.isString("Language"))) {
			config.set("Language", "en");
		}
		
		// Natural Causes
		if (!(config.isConfigurationSection("NaturalCauses"))) {
			config.createSection("NaturalCauses");
		}

		ConfigurationSection naturalC = config.getConfigurationSection("NaturalCauses");

		if (!(naturalC.isBoolean("KillIncrease"))) {
			naturalC.set("KillIncrease", true);
		}
		
		if (!(naturalC.isInt("KillIncreaseChance"))) {
			naturalC.set("KillIncreaseChance", 100);
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
		
		if (!(naturalC.isDouble("DeathDivider")) && !(naturalC.isInt("DeathDivider"))) {
			naturalC.set("DeathDivider", 2);
		}

		// Interest
		if (!(config.isConfigurationSection("Interest"))) {
			config.createSection("Interest");
		}

		ConfigurationSection interest = config.getConfigurationSection("Interest");

		if (!(interest.isBoolean("Enabled"))) {
			interest.set("Enabled", true);
		}

		if (!(interest.isInt("IntervalTicks")) && !(interest.isLong("IntervalTicks"))) {
			interest.set("IntervalTicks", 1728000);
		}

		if (!(interest.isDouble("ValueMultiplier")) && !(interest.isInt("ValueMultiplier"))) {
			interest.set("ValueMultiplier", 1.03D);
		}
	}
	
	private static void updateInterest() {
		Novaconomy plugin = getPlugin(Novaconomy.class);
		
		config = plugin.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");
		
		if (!(INTEREST_RUNNABLE.isCancelled())) INTEREST_RUNNABLE.cancel();
		
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
				if (!(event.isCancelled())) {
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