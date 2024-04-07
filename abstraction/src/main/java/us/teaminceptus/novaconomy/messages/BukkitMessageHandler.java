package us.teaminceptus.novaconomy.messages;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import static us.teaminceptus.novaconomy.messages.MessageHandler.*;

class BukkitMessageHandler implements MessageHandler {

    private final Plugin plugin;

    BukkitMessageHandler(Plugin plugin) {
        this.plugin = plugin;

        plugin.getLogger().info("Loaded Bukkit MessageHandler");
    }

    @Override
    public void send(CommandSender sender, String key, Object... args) {
        sendRaw(sender, format(sender, get(sender, key), args));
    }

    @Override
    public void sendMessage(CommandSender sender, String key, Object... args) {
        sendRawMessage(sender, format(sender, get(sender, key), args));
    }

    @Override
    public void sendError(CommandSender sender, String key, Object... args) {
        sendRawMessage(sender, ChatColor.RED + format(sender, get(sender, key), args));
    }

    @Override
    public void sendSuccess(CommandSender sender, String key, Object... args) {
        sendRawMessage(sender, ChatColor.GREEN + format(sender, get(sender, key), args));
    }

    @Override
    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    public void sendRaw(CommandSender sender, String[] messages) {
        sender.sendMessage(messages);
    }

    @Override
    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix() + message);
    }
}
