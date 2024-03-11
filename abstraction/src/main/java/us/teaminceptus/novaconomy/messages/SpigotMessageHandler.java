package us.teaminceptus.novaconomy.messages;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.r;
import static us.teaminceptus.novaconomy.messages.MessageHandler.format;
import static us.teaminceptus.novaconomy.messages.MessageHandler.get;

class SpigotMessageHandler implements MessageHandler {

    private static final Player.Spigot CONSOLE_SPIGOT = new Player.Spigot() {
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent component) {
            Bukkit.getConsoleSender().sendMessage(component.toLegacyText());
        }
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent... components) {
            Bukkit.getConsoleSender().sendMessage(TextComponent.toLegacyText(components));
        }
    };

    private final Plugin plugin;

    SpigotMessageHandler(Plugin plugin) {
        this.plugin = plugin;

        plugin.getLogger().info("Loaded Spigot MessageHandler");
    }

    private static boolean advancedText(CommandSender sender) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            NovaPlayer np = new NovaPlayer(p);
            return np.getSetting(Settings.Personal.ADVANCED_TEXT);
        }

        return true;
    }

    private TextComponent prefix(CommandSender sender) {
        TextComponent prefix = new TextComponent(MessageHandler.prefix());
        if (!advancedText(sender)) return prefix;

        TextComponent hoverText = new TextComponent("Novaconomy v" + plugin.getDescription().getVersion());
        hoverText.setColor(ChatColor.GOLD);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{hoverText});
        prefix.setHoverEvent(hover);

        ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Team-Inceptus/Novaconomy");
        prefix.setClickEvent(click);

        return prefix;
    }

    private static Player.Spigot spigot(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return CONSOLE_SPIGOT;

        return ((Player) sender).spigot();
    }

    private static void map(CommandSender sender, List<BaseComponent> components, String key) {
        if (!advancedText(sender)) return;

        if (MessageHandler.ERROR_EXAMPLES.containsKey(key)) {
            BaseComponent[] example = TextComponent.fromLegacyText(EXAMPLE_COLORS[r.nextInt(EXAMPLE_COLORS.length)] + format(sender, get(sender, "constants.example"), ChatColor.GOLD + MessageHandler.ERROR_EXAMPLES.get(key).get()));
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, example);

            components.forEach(c -> c.setHoverEvent(hover));
        }
    }

    @Override
    public void send(CommandSender sender, String key, Object... args) {
        List<BaseComponent> message = new ArrayList<>(Arrays.asList(TextComponent.fromLegacyText(format(sender, get(sender, key), args))));
        map(sender, message, key);

        sendComponents(sender, message);
    }

    @Override
    public void sendMessage(CommandSender sender, String key, Object... args) {
        List<BaseComponent> message = new ArrayList<>(Arrays.asList(TextComponent.fromLegacyText(format(sender, get(sender, key), args))));
        map(sender, message, key);

        message.add(0, prefix(sender));

        sendComponents(sender, message);
    }

    @Override
    public void sendError(CommandSender sender, String key, Object... args) {
        List<BaseComponent> message = new ArrayList<>(Arrays.asList(TextComponent.fromLegacyText(ChatColor.RED + format(sender, get(sender, key), args))));
        map(sender, message, key);

        message.add(0, prefix(sender));

        sendComponents(sender, message);
    }

    @Override
    public void sendSuccess(CommandSender sender, String key, Object... args) {
        List<BaseComponent> message = new ArrayList<>(Arrays.asList(TextComponent.fromLegacyText(ChatColor.GREEN + format(sender, get(sender, key), args))));
        map(sender, message, key);

        message.add(0, prefix(sender));

        sendComponents(sender, message);
    }

    @Override
    public void sendRaw(CommandSender sender, String message) {
        sendComponents(sender, Arrays.asList(TextComponent.fromLegacyText(message)));
    }

    @Override
    public void sendRaw(CommandSender sender, String[] messages) {
        sendComponents(sender, Arrays.asList(messages)
                .stream()
                .map(TextComponent::fromLegacyText)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList())
        );
    }

    @Override
    public void sendRawMessage(CommandSender sender, String message) {
        List<BaseComponent> message0 = new ArrayList<>(Arrays.asList(TextComponent.fromLegacyText(message)));
        message0.add(0, prefix(sender));

        sendComponents(sender, message0);
    }

    private void sendComponents(CommandSender sender, Collection<BaseComponent> message) {
        spigot(sender).sendMessage(message.toArray(new BaseComponent[0]));
    }
}
