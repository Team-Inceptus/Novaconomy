package us.teaminceptus.novaconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class Commands implements CommandExecutor {

	protected Novaconomy plugin;

	protected Commands(Novaconomy plugin) {
		this.plugin = plugin;
			for (String name : plugin.getDescription().getCommands().keySet()) {
			plugin.getCommand(name).setExecutor(this);
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		switch (cmd.getName()) {
			case "ehelp": {
				String generalMsg = ChatColor.GOLD + "" + ChatColor.UNDERLINE + "Novaconomy General Commands\n\n" +
				ChatColor.YELLOW + "/bal - " + ChatColor.GREEN + "Check your Balance";
				if (sender.isOp()) {

				} else {

				}
				break;
			}
			case "balance": {
				
				break;
			}
			case "novaconomyreload": {
				sender.sendMessage(ChatColor.GOLD + "Reloading...");
				plugin.saveConfig();
				plugin.reloadConfig();
				plugin.saveConfig();
				sender.sendMessage(ChatColor.GOLD + "Reloaded Config! You need to restart/reload your server to update the JAR file.");
			}
			default: {
				return true;
			}
		}

		return false;
	}


}