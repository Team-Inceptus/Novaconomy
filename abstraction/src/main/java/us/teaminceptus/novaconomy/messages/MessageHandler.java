package us.teaminceptus.novaconomy.messages;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.r;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.w;

public interface MessageHandler {

    MessageHandler messages = getMessageHandler();

    default void sendNotification(OfflinePlayer p, String key, Object... args) {
        if (!p.isOnline()) return;

        NovaPlayer np = new NovaPlayer(p);
        if (np.hasNotifications())
            sendMessage(p.getPlayer(), key, args);
    }

    default void sendRawNotification(OfflinePlayer p, String message) {
        if (!p.isOnline()) return;
        NovaPlayer np = new NovaPlayer(p);

        if (np.hasNotifications())
            sendRawMessage(p.getPlayer(), message);
    }

    void send(CommandSender sender, String key, Object... args);

    void sendMessage(CommandSender sender, String key, Object... args);

    void sendError(CommandSender sender, String key, Object... args);

    void sendSuccess(CommandSender sender, String key, Object... args);

    void sendRaw(CommandSender sender, String message);

    default void sendRaw(CommandSender sender, Iterable<String> messages) {
        sendRaw(sender, ImmutableList.copyOf(messages).toArray(new String[0]));
    }

    void sendRaw(CommandSender sender, String... messages);

    void sendRawMessage(CommandSender sender, String message);

    // Static Maps

    static <T> Supplier<String> any(Supplier<Iterable<T>> iterable, Function<T, String> toString, String def) {
        return () -> ImmutableList.copyOf(iterable.get())
                .stream()
                .map(toString)
                .findAny()
                .orElse(def);
    }

    static <T> Supplier<String> any(Supplier<Iterable<T>> iterable, Function<T, String> toString, String def, Predicate<T> filter) {
        return () -> ImmutableList.copyOf(iterable.get())
                .stream()
                .filter(filter)
                .map(toString)
                .findAny()
                .orElse(def);
    }

    static <T> Supplier<String> any(Supplier<T[]> array, Function<T, String> toString) {
        return () -> {
            List<String> l = Arrays.stream(array.get())
                    .map(toString)
                    .collect(Collectors.toList());

            return l.get(r.nextInt(l.size()));
        };
    }

    static <T> Supplier<String> any(Supplier<T[]> array, Function<T, String> toString, Predicate<T> filter) {
        return () -> {
            List<String> l = Arrays.stream(array.get())
                    .filter(filter)
                    .map(toString)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            return l.get(r.nextInt(l.size()));
        };
    }

    ChatColor[] EXAMPLE_COLORS = new ChatColor[] {
            ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.BLUE, ChatColor.AQUA, ChatColor.DARK_AQUA
    };

    Map<String, Supplier<String>> ERROR_EXAMPLES = ImmutableMap.<String, Supplier<?>>builder()
            .put("error.argument.amount", r::nextInt)
            .put("error.argument.block", any(Material::values, Material::toString, Material::isBlock))
            .put("error.argument.bool", r::nextBoolean)
            .put("error.argument.business", any(Business::getBusinesses, Business::getName, "PopShop"))
            .put("error.argument.player", any(Bukkit::getOnlinePlayers, Player::getName, "GamerCoder"))
            .put("error.argument.cause", any(EntityDamageEvent.DamageCause::values, Enum::toString))
            .put("error.argument.corporation", any(Corporation::getCorporations, Corporation::getName, "Team Inceptus"))
            .put("error.argument.crop", any(Material::values, Material::toString, w::isCrop))
            .put("error.argument.economy", any(Economy::getEconomies, Economy::getName, "Coins"))
            .put("error.argument.entity", any(EntityType::values, Enum::toString, EntityType::isSpawnable))
            .put("error.argument.icon", any(Material::values, Material::toString, w::isItem))
            .put("error.argument.integer", r::nextInt)
            .put("error.argument.item", any(Material::values, Material::toString, w::isItem))
            .put("error.argument.scale", () -> r.nextInt(20) / 10.0)
            .put("error.argument.symbol", () -> "'" + (char) (r.nextInt(0x7E - 0x21) + 0x21) + "'")

            .build()
            .entrySet()
            .stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), (Supplier<String>) () -> e.getValue().get().toString()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Util

    static String prefix() {
        return get("plugin.prefix");
    }

    static String prefix(ServerOperator sender) {
        return get(sender, "plugin.prefix");
    }

    static String get(String key) {
        return Language.getCurrentMessage(key);
    }

    static String get(ServerOperator sender, String key) {
        if (!(sender instanceof Player)) return get(key);
        Player p = (Player) sender;
        NovaPlayer np = new NovaPlayer(p);

        return np.getLanguage().getMessage(key);
    }

    static String format(String format, Object... args) {
        if (args.length == 0) return format;

        return String.format(Language.getCurrentLocale(), format, args)
                .replace("\u00a0", " "); // Replace non-breaking space with regular space
    }

    static String format(ServerOperator sender, String format, Object... args) {
        if (!(sender instanceof Player)) return format(format, args);

        Player p = (Player) sender;
        NovaPlayer np = new NovaPlayer(p);

        return String.format(np.getLanguage().getLocale(), format, args)
                .replace("\u00a0", " "); // Replace non-breaking space with regular space
    }

    // Fetching

    static MessageHandler getMessageHandler() {
        Plugin plugin = NovaConfig.getPlugin();

        switch (LoadedMessageType.find()) {
            default: return new BukkitMessageHandler(plugin);
            case SPIGOT: return new SpigotMessageHandler(plugin);
            case ADVENTURE: {
                try {
                    Class<? extends MessageHandler> adventure = Class.forName("us.teaminceptus.novaconomy.messages.AdventureMessageHandler")
                            .asSubclass(MessageHandler.class);

                    Constructor<? extends MessageHandler> constructor = adventure.getDeclaredConstructor(Plugin.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(plugin);
                } catch (ReflectiveOperationException e) {
                    NovaConfig.print(e);
                    return new BukkitMessageHandler(plugin);
                }
            }
        }
    }

    enum LoadedMessageType {
        BUKKIT,
        SPIGOT,
        ADVENTURE

        ;

        static LoadedMessageType find() {
            String type = NovaConfig.loadFunctionalityFile().getString("MessageHandler", "auto").toLowerCase();

            switch (type) {
                case "bukkit": return BUKKIT;
                case "spigot": return SPIGOT;
                case "adventure": return ADVENTURE;
                default: return findAuto();
            }
        }

        static LoadedMessageType findAuto() {
            try {
                Class.forName("net.kyori.adventure.text.Component");
                return ADVENTURE;
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("org.bukkit.entity.Player$Spigot");
                    return SPIGOT;
                } catch (ClassNotFoundException e2) {
                    return BUKKIT;
                }
            }
        }
    }
}
