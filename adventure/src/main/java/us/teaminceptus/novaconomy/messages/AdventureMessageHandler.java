package us.teaminceptus.novaconomy.messages;

import com.google.common.collect.ImmutableSet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Product;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.r;
import static us.teaminceptus.novaconomy.messages.MessageHandler.format;
import static us.teaminceptus.novaconomy.messages.MessageHandler.get;

class AdventureMessageHandler implements MessageHandler {

    private final Plugin plugin;

    AdventureMessageHandler(Plugin plugin) {
        this.plugin = plugin;

        plugin.getLogger().info("Loaded Adventure MessageHandler");
    }

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer
            .legacySection()
            .toBuilder()
            .extractUrls(Style.style()
                    .decorate(TextDecoration.UNDERLINED)
                    .build()
            )
            .build();

    private static final ClickCallback.Options LIFETIME = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .build();

    private static final Supplier<ImmutableSet<TextReplacementConfig>> TEXT_REPLACERS = () -> ImmutableSet.<TextReplacementConfig>builder()
            // Players Online
            .addAll(Bukkit.getOnlinePlayers()
                    .stream()
                    .map(p -> {
                        String name = p.getName();
                        String displayName = PlainTextComponentSerializer.plainText().serialize(p.displayName());
                        return TextReplacementConfig.builder()
                                .match(Pattern.compile(name + "|" + displayName, Pattern.LITERAL))
                                .replacement(Component.text(name)
                                        .hoverEvent(HoverEvent.showEntity(Key.key("minecraft:player"), p.getUniqueId()))
                                        .clickEvent(ClickEvent.callback(audience -> {
                                            if (audience instanceof Player)
                                                ((Player) audience).performCommand("playerstats " + name);
                                        }, LIFETIME))
                                )
                                .build();
                    })
                    .collect(Collectors.toList())
            )

            // Businesses
            .addAll(Business.getBusinesses()
                    .stream()
                    .map(b -> {
                        String name = b.getName();
                        return TextReplacementConfig.builder()
                                .matchLiteral(name)
                                .replacement(Component.text(name)
                                        .hoverEvent(HoverEvent.showText(
                                                Component.text(get("constants.click_to_open") + " '" + name + "'").color(TextColor.color(0xFFD500))
                                        ))
                                        .clickEvent(ClickEvent.callback(audience -> {
                                            if (audience instanceof Player)
                                                ((Player) audience).performCommand("business query " + name);
                                        }, LIFETIME))
                                )
                                .build();
                    })
                    .collect(Collectors.toList())
            )

            // Corporations
            .addAll(Corporation.getCorporations()
                    .stream()
                    .map(c -> {
                        String name = c.getName();
                        return TextReplacementConfig.builder()
                                .matchLiteral(name)
                                .replacement(Component.text(name)
                                        .hoverEvent(HoverEvent.showText(
                                                Component.text(get("constants.click_to_open") + " '" + name + "'").color(TextColor.color(0xFFD500))
                                        ))
                                        .clickEvent(ClickEvent.callback(audience -> {
                                            if (audience instanceof Player)
                                                ((Player) audience).performCommand("corporation query " + name);
                                        }, LIFETIME))
                                )
                                .build();
                    })
                    .collect(Collectors.toList())
            )
            .build();

    private static boolean advancedText(CommandSender sender) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            NovaPlayer np = new NovaPlayer(p);
            return np.getSetting(Settings.Personal.ADVANCED_TEXT);
        }

        return true;
    }

    private static Component fromLegacy(CommandSender sender, String legacy) {
        Component base = LEGACY_SERIALIZER.deserialize(legacy);
        if (!advancedText(sender)) return base;

        for (TextReplacementConfig replacer : TEXT_REPLACERS.get()) {
            base = base.replaceText(replacer);
        }

        return base;
    }

    private Component prefix(CommandSender sender) {
        Component prefix = fromLegacy(sender, MessageHandler.prefix());
        if (!advancedText(sender)) return prefix;

        return prefix
                .hoverEvent(HoverEvent.showText(
                        Component.text("Novaconomy v" + plugin.getDescription().getVersion()).color(TextColor.color(0xFFD700))
                ))
                .clickEvent(ClickEvent.openUrl("https://github.com/Team-Inceptus/Novaconomy"));
    }

    @SuppressWarnings("PatternValidation")
    private static Component map(CommandSender sender, Component component, String key, Object[] args) {
        if (!advancedText(sender)) return component;

        if (MessageHandler.ERROR_EXAMPLES.containsKey(key)) {
            Component text = fromLegacy(sender, EXAMPLE_COLORS[r.nextInt(EXAMPLE_COLORS.length)] + format(sender, get(sender, "constants.example"), ChatColor.GOLD + MessageHandler.ERROR_EXAMPLES.get(key).get()));
            component = component.hoverEvent(HoverEvent.showText(text));
        }

        switch (key) {
            // Notifications
            case "notification.business.purchase": {
                Player p = (Player) sender;
                Business b = Business.byOwner(p);
                String productName = args[1].toString();

                Product product = b.getProducts().stream()
                        .filter(bp -> {
                            ItemStack item = bp.getItem();
                            return item.getType().name().equalsIgnoreCase(productName.replace(' ', '_')) || (item.getItemMeta().hasDisplayName() && item.getItemMeta().displayName().toString().contains(productName));
                        })
                        .findFirst()
                        .orElse(null);
                if (product == null) break;

                Key item = Key.key("minecraft", product.getItem().getType().name().toLowerCase());
                String tag = NBTWrapper.of(product.getItem()).getFullTag();

                component = component
                        .hoverEvent(HoverEvent.showItem(item, product.getItem().getAmount(), BinaryTagHolder.binaryTagHolder(tag)))
                        .clickEvent(ClickEvent.callback(audience -> {
                            if (audience instanceof Player)
                                ((Player) audience).performCommand("business");
                        }, LIFETIME));
                break;
            }

            // Success Messages
            case "success.business.create": {
                component = component
                        .hoverEvent(HoverEvent.showText(
                                Component.text(get(sender, "constants.click_to_open")).color(TextColor.color(0xFFD500))
                        ))
                        .clickEvent(ClickEvent.callback(audience -> {
                            if (audience instanceof Player)
                                ((Player) audience).performCommand("business");
                        }, LIFETIME));
                break;
            }
            case "success.corporation.create": {
                component = component
                        .hoverEvent(HoverEvent.showText(
                                Component.text(get(sender, "constants.click_to_open")).color(TextColor.color(0xFFD500))
                        ))
                        .clickEvent(ClickEvent.callback(audience -> {
                            if (audience instanceof Player)
                                ((Player) audience).performCommand("corporation");
                        }, LIFETIME));
                break;
            }
        }

        return component;
    }

    @Override
    public void send(CommandSender sender, String key, Object... args) {
        Component message = map(sender, fromLegacy(sender, format(sender, get(sender, key), args)), key, args);

        sender.sendMessage(message);
    }

    @Override
    public void sendMessage(CommandSender sender, String key, Object... args) {
        Component message = map(sender, fromLegacy(sender, format(sender, get(sender, key), args)), key, args);
        sendComponents(sender, prefix(sender), message);
    }

    @Override
    public void sendError(CommandSender sender, String key, Object... args) {
        Component message = map(sender, fromLegacy(sender, ChatColor.RED + format(sender, get(sender, key), args)), key, args);
        sendComponents(sender, prefix(sender), message);
    }

    @Override
    public void sendSuccess(CommandSender sender, String key, Object... args) {
        Component message = map(sender, fromLegacy(sender, ChatColor.GREEN + format(sender, get(sender, key), args)), key, args);
        sendComponents(sender, prefix(sender), message);
    }

    @Override
    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(fromLegacy(sender, message));
    }

    @Override
    public void sendRaw(CommandSender sender, String[] messages) {
        sendComponents(sender, Arrays.stream(messages)
                .map(m -> fromLegacy(sender, m))
                .toArray(Component[]::new));
    }

    @Override
    public void sendRawMessage(CommandSender sender, String message) {
        sendComponents(sender, prefix(sender), fromLegacy(sender, message));
    }

    private void sendComponents(CommandSender sender, Component... components) {
        sender.sendMessage(Component.empty().children(Arrays.asList(components)));
    }
}
