package us.teaminceptus.novaconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

class Commands implements TabExecutor {

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
						suggestions.addAll(Arrays.asList("create", "delete", "info", "addbal", "removebal"));
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
							suggestions.addAll("$", "%", "₼", "ƒ", "¥", "₡", "₱", "£", "Q", "L", "P", "₭", "₦", "₽", "฿", "₫", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b");
						}
						
						return suggestions;
					}
					case 4: {
						if (args[0].equalsIgnoreCase("create")) {
							for (Material m : Material.values()) suggestions.add("minecraft:" + m.name().toLowerCase());
						}
						return suggestions;
					}
					case 5: {
						if (args[0].equalsIgnoreCase("create")) {
							suggestions.addAll(Arrays.asList("true", "false"));
						}

						return suggestions;
					}
				}
			}
		}
		
		return suggestions;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		switch (cmd.getName()) {
			case "ehelp": {
				List<String> commandInfo = new ArrayList<>();
				for (String name : plugin.getDescription().getCommands().keySet()) {
					PluginCommand cmd = plugin.getCommand(name);

					if (sender.isOp()) {
						commandInfo.add(ChatColor.GOLD + cmd.getUsage() + ChatColor.WHITE + " - " + ChatColor.GREEN + cmd.getDescription() + ChatColor.WHITE + " | " + ChatColor.BLUE + cmd.getPermission());
					} else {
						commandInfo.add(ChatColor.GOLD + cmd.getUsage() + ChatColor.WHITE + " - " + ChatColor.GREEN + cmd.getDescription());
					}
				}

				String msg = ChatColor.GOLD + "" + ChatColor.UNDERLINE + "Commands\n" + String.join("\n", commandInfo.toArray(new String[]{}));

				sender.sendMessage(msg);
				break;
			}
			case "balance": {
				if (!(sender instanceof Player p)) return false;
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
						Novaconomy.sendError(p, "Please provide an amount to pay.");
						return false;
					}

					double amount = Double.parseDouble(args[1]);

					if (args.length < 3) {
						Novaconomy.sendError(p, "Please provide an economy.");
						return false;
					}

					if (Economy.getEconomy(args[2]) == null) {
						Novaconomy.sendError(p, "Please provide a valid economy.");
						return false;
					}

					Economy econ = Economy.getEconomy(args[2]);

					if (np.getBalance(econ) < amount) {
						Novaconomy.sendError(p, "You do not have enough money to pay this player!");
						return false;
					}

					PlayerPayEvent event = new PlayerPayEvent(p, target, econ, amount, nt.getBalance(econ), nt.getBalance(econ) + amount);

					Bukkit.getPluginManager().callEvent(event);

					if (!(event.isCancelled())) {
						np.remove(amount);
						nt.add(amount);

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

							Material icon = Material.matchMaterial(args[3].toUpperCase().replaceAll("minecraft:", ""));

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
							
								Economy.builder().setName(name).setSymbol(symbol).setIcon(icon).setNaturallyIncrease(naturalIncrease).setConversionScale(scale).build();
							
							sender.sendMessage(ChatColor.GREEN + "Economy successfully created!");
						} catch (IllegalArgumentException e) {
							Novaconomy.sendError(sender, "Please provide a valid icon material.");
							return false;
						}
						break;
					}
					case "delete": {
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

							np.add(econ, add);
							sender.sendMessage(ChatColor.GREEN + "Successfully added " + ChatColor.GOLD + econ.getSymbol() + args[3] + ChatColor.GREEN + " to " + ChatColor.GOLD + target.getName() + "'s " + ChatColor.GREEN + " balance.");
						} catch (NumberFormatException e) {
							Novaconomy.sendError(sender, "Please provide a valid amount to add.");
						}
						break;
					}
					case "removebal": {	
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

							np.remove(econ, remove);
							sender.sendMessage(ChatColor.GREEN + "Successfully removed " + ChatColor.GOLD + econ.getSymbol() + args[3] + ChatColor.GREEN + " from " + ChatColor.GOLD + target.getName() + "'s " + ChatColor.GREEN + " balance.");
						} catch (NumberFormatException e) {
							Novaconomy.sendError(sender, "Please provide a valid amount to remove.");
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

		return false;
	}


}