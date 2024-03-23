package us.teaminceptus.novaconomy.abstraction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.ModifierReader;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationInvite;
import us.teaminceptus.novaconomy.api.corporation.CorporationPermission;
import us.teaminceptus.novaconomy.api.corporation.CorporationRank;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.MarketCategory;
import us.teaminceptus.novaconomy.api.events.CommandTaxEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessAdvertiseEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessTeleportHomeEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessViewEvent;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationTeleportHeadquartersEvent;
import us.teaminceptus.novaconomy.api.player.Bounty;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.settings.SettingDescription;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.util.NovaSound;
import us.teaminceptus.novaconomy.util.NovaUtil;
import us.teaminceptus.novaconomy.util.inventory.Generator;
import us.teaminceptus.novaconomy.util.inventory.InventorySelector;
import us.teaminceptus.novaconomy.util.inventory.Items;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.bukkit.ChatColor.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.r;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.w;
import static us.teaminceptus.novaconomy.messages.MessageHandler.*;
import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;
import static us.teaminceptus.novaconomy.util.NovaUtil.capitalize;
import static us.teaminceptus.novaconomy.util.inventory.Generator.*;
import static us.teaminceptus.novaconomy.util.inventory.Items.*;

@SuppressWarnings("unchecked")
public interface CommandWrapper {

    String ERROR_PERMISSION = "error.permission";

    String ERROR_PERMISSION_ARGUMENT = "error.permission.argument";
    String SETTING_TAG = "setting";

    default void loadCommands() {
    }

    String BUSINESS_TAG = "business";
    String AMOUNT_TAG = "amount";
    String ECON_TAG = "economy";
    String PRODUCT_TAG = "product";
    String PRICE_TAG = "price";
    String CORPORATION_TAG = "corporation";
    String TYPE_TAG = "type";

    Map<String, List<String>> COMMANDS = ImmutableMap.<String, List<String>>builder()
            .put("ehelp", asList("nhelp", "novahelp", "econhelp", "economyhelp"))
            .put(ECON_TAG, asList("econ", "novaecon", "novaconomy", "necon"))
            .put("balance", asList("bal", "novabal", "nbal"))
            .put("nconvert", asList("nconv", "convert", "conv"))
            .put("exchange", asList("convertgui", "convgui", "exch"))
            .put("pay", asList("givemoney", "novapay", "econpay", "givebal"))
            .put("novaconomyreload", asList("novareload", "nreload", "econreload"))
            .put(BUSINESS_TAG, asList("nbusiness", "b", "nb"))
            .put("nbank", asList("bank", "globalbank", "gbank"))
            .put("createcheck", asList("nc", "check", "novacheck", "ncheck"))
            .put("balanceleaderboard", asList("bleaderboard", "nleaderboard", "bl", "nl", "novaleaderboard", "balboard", "novaboard"))
            .put("nbounty", asList("novabounty"))
            .put("taxevent", asList("customtax"))
            .put("nsettings", asList("novasettings"))
            .put("nrate", asList("novarate", "ratebusiness"))
            .put("npstatistics", asList("stats", "pstats", "pstatistics", "playerstats", "playerstatistics", "nstats", "nstatistics"))
            .put("novaconfig", asList("novaconomyconfig", "nconfig", "nconf"))
            .put("businessleaderboard", asList("bleaderboard", "bboard", "businessl", "bl", "businessboard"))
            .put(CORPORATION_TAG, asList("corp", "ncorp", "c"))
            .put("corporationchat", asList("corpchat", "cc", "ncc", "corporationc", "corpc", "cchat"))
            .put("market", asList("novamarket", "novam", "m"))
            .put("corporationleaderboard", asList("corpleaderboard", "cleaderboard", "corpboard", "cboard"))
            .put("nauctionhouse", asList("novaah", "ah", "auctionhouse", "auctions"))
            .put("nlanguage", asList("novalang", "nlang"))
            .build();

    Map<String, String> COMMAND_PERMISSION = ImmutableMap.<String, String>builder()
            .put(ECON_TAG, "novaconomy.economy")
            .put("balance", "novaconomy.user.balance")
            .put("nconvert", "novaconomy.user.convert")
            .put("exchange", "novaconomy.user.convert")
            .put("pay", "novaconomy.user.pay")
            .put("novaconomyreload", "novaconomy.admin.config")
            .put(BUSINESS_TAG, "novaconomy.user.business")
            .put("createcheck", "novaconomy.user.check")
            .put("balanceleaderboard", "novaconomy.user.leaderboard")
            .put("nbounty", "novaconomy.user.bounty")
            .put("taxevent", "novaconomy.admin.tax_event")
            .put("nsettings", "novaconomy.user.settings")
            .put("nrate", "novaconomy.user.rate")
            .put("npstatistics", "novaconomy.user.stats")
            .put("novaconfig", "novaconomy.admin.config")
            .put("businessleaderboard", "novaconomy.user.leaderboard")
            .put(CORPORATION_TAG, "novaconomy.user.corporation")
            .put("corporationchat", "novaconomy.user.corporation")
            .put("market", "novaconomy.user.market")
            .put("corporationleaderboard", "novaconomy.user.leaderboard")
            .put("nauctionhouse", "novaconomy.user.auction_house")
            .put("nlanguage", "novaconomy.user.language")
            .build();

    Map<String, String> COMMAND_DESCRIPTION = ImmutableMap.<String, String>builder()
            .put("ehelp", "Economy help")
            .put(ECON_TAG, "Manage economies or their balances")
            .put("balance", "Access your balances from all economies")
            .put("nconvert", "Convert one balance in an economy to another balance")
            .put("exchange", "Convert one balance in an economy to another balance (with a GUI)")
            .put("pay", "Pay another user")
            .put("novaconomyreload", "Reload Novaconomy Configuration")
            .put(BUSINESS_TAG, "Manage your Novaconomy Business")
            .put("nbank", "Interact with the Global Novaconomy Bank")
            .put("createcheck", "Create a Novaconomy Check redeemable for a certain amount of money")
            .put("balanceleaderboard", "View the top 15 balances in all or certain economies")
            .put("nbounty", "Manage your Novaconomy Bounties")
            .put("taxevent", "Call a Custom Tax Event from the configuration")
            .put("nsettings", "Manage your Novaconomy Settings")
            .put("nrate", "Rate a Novaconomy Business")
            .put("npstatistics", "View your Novaconomy Statistics")
            .put("novaconfig", "View or edit the Novaconomy Configuration")
            .put("businessleaderboard", "View the top 10 businesses in various categories")
            .put(CORPORATION_TAG, "Manage your Novaconomy Corporation")
            .put("corporationchat", "Chat with your Novaconomy Corporation")
            .put("market", "View and Manage the Novaconomy Market")
            .put("corporationleaderboard", "View the top 10 corporations in various categories")
            .put("nauctionhouse", "View the Novaconomy Auction House")
            .put("nlanguage", "Change your Novaconomy Language")
            .build();

    Map<String, String> COMMAND_USAGE = ImmutableMap.<String, String>builder()
            .put("ehelp", "/ehelp")
            .put(ECON_TAG, "/economy <create|delete|addbal|removebal|info> <args...>")
            .put("balance", "/balance")
            .put("nconvert", "/convert <econ-from> <econ-to> <amount>")
            .put("exchange", "/exchange <amount>")
            .put("pay", "/pay <player> <economy> <amount>")
            .put("novaconomyreload", "/novareload")
            .put(BUSINESS_TAG, "/business <create|delete|edit|stock|...> <args...>")
            .put("overridelanguages", "/overridelanguages")
            .put("createcheck", "/createcheck <economy> <amount>")
            .put("balanceleaderboard", "/balanceleaderboard [<economy>]")
            .put("nbounty", "/nbounty <owned|create|delete|self> <args...>")
            .put("taxevent", "/taxevent <event> [<self>]")
            .put("nsettings", "/nsettings [<business|personal>]")
            .put("nrate", "/nrate <business> [<comment>]")
            .put("npstatistics", "/npstatistics")
            .put("novaconfig", "/novaconfig <naturalcauses|reload|rl|...> <args...>")
            .put("businessleaderboard", "/businessleaderboard")
            .put(CORPORATION_TAG, "/nc <create|delete|edit|...> <args...>")
            .put("corporationchat", "/cc <message>")
            .put("market", "/market <open|sell|...>")
            .put("corporationleaderboard", "/corporationleaderboard")
            .put("nauctionhouse", "/ah [open|search|add|...]")
            .put("nlanguage", "/nlang")
            .build();

    // Command Methods

    default void help(CommandSender sender) {
        List<String> commandInfo = new ArrayList<>();
        for (String name : COMMANDS.keySet()) {
            PluginCommand pcmd = Bukkit.getPluginCommand(name);

            if (!sender.isOp() && COMMAND_PERMISSION.get(name) != null && !(sender.hasPermission(COMMAND_PERMISSION.get(name))))
                continue;

            if (sender.isOp())
                commandInfo.add(GOLD + "/" + pcmd.getName() + WHITE + " - " + GREEN + COMMAND_DESCRIPTION.get(name) + WHITE + " | " + BLUE + (COMMAND_PERMISSION.get(name) == null ? "No Permissions" : COMMAND_PERMISSION.get(name)));
            else
                commandInfo.add(GOLD + "/" + pcmd.getName() + WHITE + " - " + GREEN + COMMAND_DESCRIPTION.get(name));
        }

        String msg = get(sender, "constants.commands") + "\n\n" + String.join("\n", commandInfo.toArray(new String[]{}));
        messages.sendRaw(sender, msg);
    }

    default void balance(Player p) {
        if (!p.hasPermission("novaconomy.user.balance")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        messages.sendRaw(p, GREEN + get(p, "constants.loading"));
        p.openInventory(getBalancesGUI(p, SortingType.ECONOMY_NAME_ASCENDING).get(0));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        messages.send(sender, "command.reload.reloading");
        reloadFiles();
        messages.send(sender, "command.reload.success");
    }

    static void reloadFiles() {
        Plugin plugin = NovaConfig.getPlugin();

        plugin.reloadConfig();
        NovaConfig.loadConfig();
        NovaConfig.reloadRunnables();
        NovaConfig.loadFunctionalityFile();

        ModifierReader.LOADED_MODIFIERS.clear();

        try {
            Method loadFiles = NovaConfig.getPlugin().getClass().getDeclaredMethod("loadFiles");
            loadFiles.setAccessible(true);
            loadFiles.invoke(NovaConfig.getPlugin());
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }

        Economy.reloadEconomies();
        Corporation.reloadCorporations();
        Business.reloadBusinesses();
    }

    default void convert(Player p, Economy from, Economy to, double amount) {
        if (!p.hasPermission("novaconomy.user.convert")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (to.equals(from)) {
            messages.sendMessage(p, "error.economy.transfer_same");
            return;
        }

        if (!from.isConvertable()) {
            messages.sendError(p, "error.economy.transfer_not_convertable", from.getName());
            return;
        }

        if (!to.isConvertable()) {
            messages.sendError(p, "error.economy.transfer_not_convertable", to.getName());
            return;
        }

        NovaPlayer np = new NovaPlayer(p);

        if (amount <= 0) {
            messages.sendMessage(p, "error.economy.transfer_amount");
            return;
        }

        double max = NovaConfig.getConfiguration().getMaxConvertAmount(from);
        if (max >= 0 && amount > max) {
            messages.sendMessage(p, "error.economy.transfer_max", format("%,.2f", max) + from.getSymbol(), format("%,.2f", amount) + from.getSymbol());
            return;
        }

        if (!np.canAfford(from, amount, NovaConfig.getConfiguration().getWhenNegativeAllowConvertBalances())) {
            messages.sendMessage(p, "error.economy.invalid_amount", RED + get(p, "constants.convert"));
            return;
        }

        double toBal = from.convertAmount(to, amount);

        np.remove(from, amount);
        np.add(to, toBal);
        messages.sendMessage(p, "success.economy.convert", format("%,.2f", amount) + from.getSymbol(), format("%,.2f", Math.floor(toBal * 100) / 100) + to.getSymbol());
    }

    default void exchange(Player p, double amount) {
        if (!p.hasPermission("novaconomy.user.convert")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        List<Economy> economies = Economy.getEconomies()
                .stream()
                .filter(Economy::isConvertable)
                .sorted(Comparator.comparing(Economy::getName))
                .collect(Collectors.toList());

        if (economies.size() < 2) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        if (amount <= 0) {
            messages.sendMessage(p, "error.argument.amount");
            return;
        }

        double max = NovaConfig.loadFunctionalityFile().getDouble("MaxConvertAmount");
        if (max >= 0 && amount > max) {
            messages.sendMessage(p, "error.economy.transfer_max", format("%,.2f", max), format("%,.2f", amount));
            return;
        }

        NovaInventory inv = genGUI(36, get(p, "constants.economy.exchange"));
        inv.setCancelled();

        Economy e1 = economies.get(0);
        Economy e2 = economies.get(1);

        inv.setItem(12, builder(e1.getIcon(),
                meta -> meta.setLore(Collections.singletonList(YELLOW + String.valueOf(amount) + e1.getSymbol())),
                nbt -> {
                    nbt.setID("exchange:1");
                    nbt.set(ECON_TAG, e1.getUniqueId());
                    nbt.set(AMOUNT_TAG, amount);
                })
        );
        inv.setItem(13, Items.ARROW);
        inv.setItem(14, builder(e2.getIcon(),
                meta -> meta.setLore(Collections.singletonList(YELLOW + String.valueOf(e1.convertAmount(e2, amount)) + e2.getSymbol())),
                nbt -> {
                    nbt.setID("exchange:2");
                    nbt.set(ECON_TAG, e2.getUniqueId());
                    nbt.set(AMOUNT_TAG, Math.floor(e1.convertAmount(e2, amount) * 100) / 100);
                })
        );

        inv.setItem(30, yes("exchange"));
        inv.setItem(32, CANCEL);

        p.openInventory(inv);
    }

    default void createEconomy(CommandSender sender, String name, char symbol, Material icon, double scale, boolean naturalIncrease, boolean clickableReward) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        for (Economy econ : Economy.getEconomies()) {
            if (econ.getName().equalsIgnoreCase(name)) {
                messages.sendMessage(sender, "error.economy.exists");
                return;
            }

            if (econ.getSymbol() == symbol) {
                messages.sendMessage(sender, "error.economy.symbol_exists");
                return;
            }
        }

        if (scale <= 0) {
            messages.sendMessage(sender, "error.argument.scale");
            return;
        }

        try {
            Economy.builder().setName(name).setSymbol(symbol).setIcon(icon).setIncreaseNaturally(naturalIncrease).setConversionScale(scale).setClickableReward(clickableReward).build();
        } catch (UnsupportedOperationException e) {
            messages.sendMessage(sender, "error.economy.exists");
            return;
        }
        messages.sendMessage(sender, "success.economy.create");
    }

    default void economyInfo(CommandSender sender, Economy econ) {
        if (!(sender.hasPermission("novaconomy.economy.info"))) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        String[] components = {
                format(sender, get(sender, "constants.economy.info"), econ.getName()),
                format(sender, get(sender, "constants.economy.natural_increase"), econ.hasNaturalIncrease()),
                format(sender, get(sender, "constants.economy.symbol"), econ.getSymbol()),
                format(sender, get(sender, "constants.economy.scale"), Math.floor(econ.getConversionScale() * 100) / 100),
                format(sender, get(sender, "constants.economy.custom_model_data"), format("%,d", econ.getCustomModelData())),
                format(sender, get(sender, "constants.economy.clickable"), econ.hasClickableReward()),
                format(sender, get(sender, "constants.economy.taxable"), econ.hasTax()),
        };
        messages.sendRaw(sender, String.join("\n", components));
    }

    default void addBalance(CommandSender sender, Economy econ, Player target, double add) {
        if (!sender.hasPermission("novaconomy.economy.addbalance")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (add < 0) {
            messages.sendMessage(sender, "error.argument.amount");
            return;
        }

        nt.add(econ, add);
        messages.sendMessage(sender, "success.economy.addbalance", format("%,.2f", add), econ.getSymbol(), target.getName());
    }

    default void removeBalance(CommandSender sender, Economy econ, Player target, double remove) {
        if (!sender.hasPermission("novaconomy.economy.removebalance")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (remove < 0) {
            messages.sendMessage(sender, "error.argument.amount");
            return;
        }

        nt.remove(econ, remove);
        messages.sendMessage(sender, "success.economy.removebalance", format("%,.2f", remove), econ.getSymbol(), target.getName());
    }

    default void setBalance(CommandSender sender, Economy econ, Player target, double balance) {
        if (!sender.hasPermission("novaconomy.economy.setbalance")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (econ == null) {
            messages.sendMessage(sender, "error.argument.economy");
            return;
        }

        if (target == null) {
            messages.sendMessage(sender, "error.argument.player");
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (balance < 0 && !NovaConfig.getConfiguration().isNegativeBalancesEnabled()) {
            messages.sendMessage(sender, "error.argument.amount");
            return;
        }

        if (balance < NovaConfig.getConfiguration().getMaxNegativeBalance()) {
            messages.sendError(sender, "error.economy.min_balance", format("%,.2f", NovaConfig.getConfiguration().getMaxNegativeBalance()) + econ.getSymbol());
            return;
        }

        nt.setBalance(econ, balance);
        messages.sendMessage(sender, "success.economy.setbalance", target.getName(), econ.getName(), format("%,.2f", balance) + econ.getSymbol());
    }

    default void interest(CommandSender sender, boolean enabled) {
        if (!sender.hasPermission("novaconomy.economy.interest")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaConfig.getConfiguration().setInterestEnabled(enabled);
        String key = "success.economy." + (enabled ? "enable" : "disable") + "_interest";
        messages.sendMessage(sender, key);
    }

    default void balanceLeaderboard(Player p, Economy econ) {
        if (!p.hasPermission("novaconomy.user.leaderboard")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        boolean economy = econ != null;

        Function<NovaPlayer, Double> func = economy ? np -> np.getBalance(econ) : NovaPlayer::getTotalBalance;
        List<NovaPlayer> players = new ArrayList<>(Arrays.stream(Bukkit.getOfflinePlayers())
                .map(NovaPlayer::new)
                .filter(np -> np.getSetting(Settings.Personal.PUBLIC_BALANCE))
                .sorted(Comparator.comparing(func).reversed())
                .collect(Collectors.toList()))
                .subList(0, Math.min(Bukkit.getOfflinePlayers().length, 15));

        NovaInventory inv = genGUI(54, get(p, "constants.balance_leaderboard"));
        inv.setCancelled();

        ItemStack type = builder(Material.PAPER,
                meta -> {
                    if (economy) meta.setDisplayName(AQUA + econ.getName());
                    else meta.setDisplayName(AQUA + get(p, "constants.all_economies"));
                },
                nbt -> {
                    nbt.setID("economy:wheel:leaderboard");
                    nbt.set(ECON_TAG, economy ? econ.getName() : "all");
                }
        );

        if (economy) {
            type.setType(econ.getIconType());
            modelData(type, econ.getCustomModelData());
        }
        inv.setItem(13, type);

        for (int i = 0; i < players.size(); i++) {
            int index = i == 0 ? 22 : i + 27;
            if (index > 34) index = index + 2;
            int level = i + 1;
            ChatColor color = new ChatColor[]{GOLD, GRAY, YELLOW, AQUA}[Math.min(i, 3)];

            NovaPlayer np = players.get(i);
            Player op = np.getOnlinePlayer();
            inv.setItem(index, Items.builder(createPlayerHead(np.getPlayer()),
                    meta -> {
                        meta.setDisplayName(color + "#" + level + " - " + (op != null && op.getDisplayName() != null ? op.getDisplayName() : np.getPlayer().getName()));
                        meta.setLore(Collections.singletonList(GOLD + format("%,.2f", economy ? np.getBalance(econ) : np.getTotalBalance()) + (economy ? econ.getSymbol() : "")));
                    })
            );
        }

        if (r.nextBoolean() && NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            Business rand = Business.randomAdvertisingBusiness();
            if (rand != null) {
                BusinessAdvertiseEvent event = new BusinessAdvertiseEvent(rand);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled())
                    inv.setItem(18, builder(rand.getPublicIcon(),
                            nbt -> {
                                nbt.setID("business:click:advertising_external");
                                nbt.set(BUSINESS_TAG, rand.getUniqueId());
                            })
                    );
            }
        }

        p.openInventory(inv);
        NovaSound.BLOCK_NOTE_BLOCK_PLING.play(p, 1F, 1F);
    }

    default void createCheck(Player p, Economy econ, double amount, boolean take) {
        if ((take && !p.hasPermission("novaconomy.user.check")) || (!take && !p.hasPermission("novaconomy.economy.check"))) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (amount < 1) {
            messages.sendMessage(p, "error.argument.amount");
            return;
        }

        NovaPlayer nt = new NovaPlayer(p);
        if (take && !nt.canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowCreateChecks())) {
            messages.sendMessage(p, "error.economy.invalid_amount", get(p, "constants.purchase"));
            return;
        }

        p.getInventory().addItem(Generator.createCheck(econ, amount));
        if (take) nt.remove(econ, amount);

        messages.sendMessage(p, "success.economy.check", String.valueOf(amount), String.valueOf(econ.getSymbol()));
    }

    default void removeEconomy(CommandSender sender, Economy econ) {
        if (!(sender.hasPermission("novaconomy.economy.delete"))) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        String name = econ.getName();

        messages.sendMessage(sender, "command.economy.delete.deleting", name);
        Economy.removeEconomy(econ);
        messages.sendMessage(sender, "success.economy.delete", name);
    }

    double[] PAY_AMOUNTS = {0.5, 1, 10, 100, 1000, 10000, 100000};

    default void pay(Player p, Player target, Economy economy, double amount) {
        if (!p.hasPermission("novaconomy.user.pay")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (target.equals(p)) {
            messages.sendMessage(p, "error.economy.pay_self");
            return;
        }

        Economy econ = economy == null ? Economy.getEconomies()
                .stream()
                .min(Comparator.comparing(Economy::getName))
                .orElse(null)
                : economy;

        if (econ == null) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        NovaInventory inv = genGUI(54, get(p, "constants.pay_player"));
        inv.setCancelled();

        inv.setItem(10, Items.builder(createPlayerHead(p),
                meta -> {
                    meta.setDisplayName(AQUA + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()));
                    meta.setLore(Collections.singletonList(GOLD + format("%,.2f", np.getBalance(econ)) + econ.getSymbol()));
                }));
        inv.setItem(16, Items.builder(createPlayerHead(target),
                meta -> meta.setDisplayName(AQUA + (target.getDisplayName() == null ? target.getName() : target.getDisplayName()))
        ));

        inv.setItem(12, Items.ARROW);
        inv.setItem(13, economyWheel("pay", econ, p));
        inv.setItem(14, Items.ARROW);

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 7; j++) {
                boolean add = i == 0;
                double pAmount = PAY_AMOUNTS[j];
                inv.setItem(19 + (i * 9) + j, builder(add ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE,
                        meta -> meta.setDisplayName((add ? GREEN + "+" : RED + "-") + format("%,.2f", pAmount)),
                        nbt -> {
                            nbt.setID("pay:amount");
                            nbt.set("add", add);
                            nbt.set(AMOUNT_TAG, pAmount);
                        })
                );
            }

        inv.setItem(40, builder(econ.getIcon().clone(),
                meta -> meta.setDisplayName(GOLD + format("%,.2f", amount) + econ.getSymbol()),
                nbt -> {
                    nbt.set(AMOUNT_TAG, amount);
                    nbt.set(ECON_TAG, econ.getUniqueId());
                }
        ));

        inv.setItem(48, builder(CONFIRM,
                nbt -> {
                    nbt.setID("pay:confirm");
                    nbt.set("target", target.getUniqueId());
                })
        );
        inv.setItem(50, CANCEL);

        p.openInventory(inv);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void deleteBusiness(Player p, boolean confirm) {
        Business b = Business.byOwner(p);
        if (b == null) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (confirm) Business.remove(b);
        else messages.sendMessage(p, "constants.confirm_command", "/business delete confirm");
    }

    default void removeBusiness(CommandSender sender, Business b, boolean confirm) {
        if (!sender.hasPermission("novaconomy.admin.delete_business")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        if (confirm) {
            Business.remove(b);
            messages.sendMessage(sender, "success.business.delete");
        } else
            messages.sendMessage(sender, "constants.confirm_command", "/business remove <business> confirm");
    }

    default void businessInfo(Player p) {
        Business b = Business.byOwner(p);
        if (b == null) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }
        p.openInventory(generateBusinessData(b, p, false, SortingType.PRODUCT_NAME_ASCENDING).get(0));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p, 1F, 0.5F);
    }

    default void businessQuery(Player p, Business b) {
        if (!p.hasPermission("novaconomy.user.business.query")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }
        boolean notOwner = !b.isOwner(p);

        p.openInventory(generateBusinessData(b, p, notOwner, SortingType.PRODUCT_NAME_ASCENDING).get(0));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p, 1F, 0.5F);

        if (notOwner) {
            b.getStatistics().addView();
            b.saveBusiness();

            BusinessViewEvent event = new BusinessViewEvent(p, b);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    default void addProduct(Player p, double price) {
        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        PlayerInventory pInv = p.getInventory();
        Business b = Business.byOwner(p);

        if (b == null) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (pInv.getItemInHand() == null || pInv.getItemInHand().getType() == Material.AIR) {
            messages.sendMessage(p, "error.argument.item");
            return;
        }

        ItemStack pr = pInv.getItemInHand().clone();
        pr.setAmount(1);

        if (b.isProduct(pr)) {
            messages.sendMessage(p, "error.business.exists_product");
            return;
        }

        Economy econ = Economy.first();

        NovaInventory inv = genGUI(36, pr.hasItemMeta() && pr.getItemMeta().hasDisplayName() ? pr.getItemMeta().getDisplayName() : capitalize(pr.getType().name().replace('_', ' ')));
        inv.setCancelled();

        inv.setAttribute("item", pr);
        inv.setItem(22, economyWheel("add_product", p));

        inv.setItem(13, builder(pr,
                meta -> meta.setLore(Collections.singletonList(format(p, get(p, "constants.price"), price, econ.getSymbol()))),
                nbt -> nbt.set(PRICE_TAG, price)
        ));

        inv.setItem(23, builder(CONFIRM,
                nbt -> {
                    nbt.setID("business:add_product");
                    nbt.set(PRICE_TAG, price);
                    nbt.set(ECON_TAG, econ.getUniqueId());
                }
        ));

        p.openInventory(inv);
    }

    default void createBusiness(Player p, String name, Material icon) {
        if (!p.hasPermission("novaconomy.user.business.create")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (Business.exists(name)) {
            messages.sendMessage(p, "error.business.exists_name");
            return;
        }

        if (Business.exists(p)) {
            messages.sendMessage(p, "error.business.exists");
            return;
        }

        try {
            Business.builder().setOwner(p).setName(name).setIcon(icon).build();
            messages.sendMessage(p, "success.business.create", name);
        } catch (IllegalArgumentException e) {
            messages.sendMessage(p, "error.argument");
        }
    }

    default void addResource(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        if (!p.hasPermission("novaconomy.user.business.resources")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        Business b = Business.byOwner(p);
        if (b == null) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        NovaInventory inv = w.createInventory("return_items", get(p, "constants.business.add_stock"), 54);
        inv.setAttribute("player", p);
        inv.setAttribute("added", false);
        inv.setAttribute("ignore_ids", ImmutableList.of("business:add_resource"));

        inv.setItem(49, builder(CONFIRM,
                meta -> {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);

                    List<String> lore = new ArrayList<>();
                    for (int i = 1; i < 4; i++) lore.add(get(p, "constants.business.add_resource." + i));

                    meta.setLore(asList(ChatPaginator.wordWrap(String.join("\n\n", lore), 30)));
                }, nbt -> nbt.setID("business:add_resource"))
        );

        p.openInventory(inv);
    }

    default void removeProduct(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        Business b = Business.byOwner(p);
        if (b == null) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (b.getProducts().isEmpty()) {
            messages.sendMessage(p, "error.business.no_products");
            return;
        }

        List<NovaInventory> bData = generateBusinessData(b, p, false, SortingType.PRODUCT_NAME_ASCENDING);
        List<ItemStack> productItems = Arrays.stream(bData.stream().map(Inventory::getContents).flatMap(Arrays::stream).toArray(ItemStack[]::new))
                .filter(Objects::nonNull)
                .map(NBTWrapper::of)
                .filter(NBTWrapper::isProduct)
                .map(nbt -> {
                    nbt.setID("product:remove");
                    return nbt.getItem();
                })
                .collect(Collectors.toList());

        List<NovaInventory> invs = new ArrayList<>();

        int limit = (productItems.size() / GUI_SPACE) + 1;
        for (int i = 0; i < limit; i++) {
            final int fI = i;

            NovaInventory inv = genGUI(54, get(p, "constants.business.remove_product"));
            inv.setCancelled();

            if (limit > 1) {
                if (i > 0)
                    inv.setItem(46,
                        NBTWrapper.builder(
                                Items.prev("stored"),
                                nbt -> nbt.set("page", fI)
                        ));

                if (i < (limit - 1))
                    inv.setItem(52,
                            NBTWrapper.builder(
                                    Items.next("stored"),
                                    nbt -> nbt.set("page", fI)
                            ));
            }

            productItems.subList(i * GUI_SPACE, Math.min((i + 1) * GUI_SPACE, productItems.size())).forEach(inv::addItem);

            invs.add(inv);
        }

        invs.forEach(inv -> inv.setAttribute("invs", invs));

        p.openInventory(invs.get(0));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p, 1F, 0.5F);
    }

    default void bankBalances(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        messages.sendRaw(p, BLUE + get(p, "constants.loading"));

        p.openInventory(getBankBalanceGUI(SortingType.ECONOMY_NAME_ASCENDING, p).get(0));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void bankDeposit(Player p, double amount, Economy econ) {
        NovaPlayer np = new NovaPlayer(p);
        if (!np.canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowPayBanks())) {
            messages.sendMessage(p, "error.economy.invalid_amount", get(p, "constants.bank.deposit"));
            return;
        }

        if (amount < NovaConfig.getConfiguration().getMinimumPayment(econ)) {
            messages.sendMessage(p, "error.bank.minimum_payment", format("%,.2f", NovaConfig.getConfiguration().getMinimumPayment(econ)) + econ.getSymbol(), format("%,.2f", amount) + econ.getSymbol());
            return;
        }

        np.deposit(econ, amount);
        messages.sendMessage(p, "success.bank.deposit", amount + String.valueOf(econ.getSymbol()), econ.getName());
    }

    default void businessHome(Player p, boolean set) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (!p.hasPermission("novaconomy.user.business.home")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        Business b = Business.byOwner(p);
        if (set) {
            Location loc = p.getLocation();
            b.setHome(loc);
            messages.sendMessage(p, "success.business.set_home", GOLD + String.valueOf(loc.getBlockX()) + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else {
            if (!b.hasHome()) {
                messages.sendMessage(p, "error.business.no_home");
                return;
            }

            if (b.getHome().distanceSquared(p.getLocation()) < 16) {
                messages.sendMessage(p, "error.business.too_close_home");
                return;
            }

            messages.sendRaw(p, DARK_AQUA + get(p, "constants.teleporting"));

            BusinessTeleportHomeEvent event = new BusinessTeleportHomeEvent(p, b);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                scheduler.teleport(p, event.getLocation());
                NovaSound.ENTITY_ENDERMAN_TELEPORT.play(p, 1F, 1F);
            }
        }
    }

    default void bankWithdraw(Player p, double amount, Economy econ) {
        if (amount > NovaConfig.getConfiguration().getMaxWithdrawAmount(econ)) {
            messages.sendMessage(p, "error.bank.maximum_withdraw", format("%,.2f", NovaConfig.getConfiguration().getMaxWithdrawAmount(econ)) + econ.getSymbol(), format("%,.2f", amount) + econ.getSymbol());
            return;
        }

        if (amount > Bank.getBalance(econ)) {
            messages.sendMessage(p, "error.bank.maximum_withdraw", format("%,.2f", Bank.getBalance(econ)) + econ.getSymbol(), format("%,.2f", amount) + econ.getSymbol());
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        long time = (np.getLastBankWithdraw().getTimestamp() - System.currentTimeMillis()) + 86400000;
        long timeSecs = Math.floorDiv(time, 1000L);
        final String timeS;

        if (timeSecs < 60) timeS = timeSecs + " " + get(p, "constants.time.second");
        else if (timeSecs >= 60 && timeSecs < 3600)
            timeS = format("%,d", Math.floorDiv(timeSecs, 60L)) + get(p, "constants.time.minute");
        else
            timeS = format("%,d", Math.floorDiv(timeSecs, 3600L)) + get(p, "constants.time.hour");

        if (time > 0) {
            messages.sendMessage(p, "error.bank.withdraw_time", timeS);
            return;
        }

        np.withdraw(econ, amount);
        messages.sendMessage(p, "success.bank.withdraw", amount + String.valueOf(econ.getSymbol()), econ.getName());
    }

    default void createBounty(Player p, OfflinePlayer target, Economy econ, double amount) {
        if (!p.hasPermission("novaconomy.user.bounty.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!NovaConfig.getConfiguration().hasBounties()) {
            messages.sendMessage(p, "error.bounty.disabled");
            return;
        }

        if (target.equals(p)) {
            messages.sendMessage(p, "error.bounty.self");
            return;
        }

        if (amount <= 0) {
            messages.sendMessage(p, "error.argument.amount");
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        if (!np.canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowCreateBounties())) {
            messages.sendMessage(p, "error.economy.invalid_amount", get(p, "constants.place_bounty"));
            return;
        }

        try {
            Bounty.builder().setOwner(np).setAmount(amount).setTarget(target).setEconomy(econ).build();
            np.remove(econ, amount);
            messages.sendMessage(p, "success.bounty.create", target.getName());

            if (target.isOnline() && NovaConfig.getConfiguration().hasNotifications())
                messages.sendMessage(target.getPlayer(), "notification.bounty", p.getDisplayName() == null ? p.getName() : p.getDisplayName(), format("%,.2f", amount) + econ.getSymbol());
        } catch (UnsupportedOperationException e) {
            messages.sendMessage(p, "error.bounty.exists", target.isOnline() && target.getPlayer().getDisplayName() == null ? target.getName() : target.getPlayer().getDisplayName());
        }
    }

    default void deleteBounty(Player p, OfflinePlayer target) {
        if (!p.hasPermission("novaconomy.user.bounty.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!NovaConfig.getConfiguration().hasBounties()) {
            messages.sendMessage(p, "error.bounty.disabled");
            return;
        }

        if (target.equals(p)) {
            messages.sendMessage(p, "error.bounty.self");
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        Map<String, Object> data = np.getPlayerData();
        String key = "bounties." + target.getUniqueId();

        if (!data.containsKey(key)) {
            messages.sendMessage(p, "error.bounty.inexistent");
            return;
        }

        Bounty b = (Bounty) data.get(key);
        np.add(b.getEconomy(), b.getAmount());
        data.put(key, null);

        messages.sendMessage(p, "success.bounty.delete", target.getName());
    }

    default void listBounties(Player p, boolean owned) {
        if (!p.hasPermission("novaconomy.user.bounty.list")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!NovaConfig.getConfiguration().hasBounties()) {
            messages.sendMessage(p, "error.bounty.disabled");
            return;
        }

        NovaPlayer np = new NovaPlayer(p);

        if (owned && np.getPlayerData().keySet().stream().noneMatch(k -> k.startsWith("bounties"))) {
            messages.sendMessage(p, "error.bounty.none");
            return;
        }

        if (!owned && np.getSelfBounties().isEmpty()) {
            messages.sendMessage(p, "error.bounty.none.self");
            return;
        }

        NovaInventory inv = genGUI(36, owned ? get(p, "constants.bounty.all") : get(p, "constants.bounty.self"));
        inv.setCancelled();

        for (int i = 10; i < 12; i++) inv.setItem(i, GUI_BACKGROUND);
        for (int i = 15; i < 17; i++) inv.setItem(i, GUI_BACKGROUND);

        ItemStack head = Items.builder(createPlayerHead(p),
            meta -> {
                meta.setDisplayName(AQUA + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()));
                if (owned)
                    meta.setLore(Collections.singletonList(format(p, get(p, "constants.bounty.amount"), np.getOwnedBounties().size())));
            }
        );
        inv.setItem(4, head);

        IntUnaryOperator fIndex = i -> i > 2 ? i + 16 : i + 12;

        if (owned) {
            List<Map.Entry<OfflinePlayer, Bounty>> bounties = np.getTopBounties(10);
            for (int i = 0; i < bounties.size(); i++) {
                Map.Entry<OfflinePlayer, Bounty> bounty = bounties.get(i);
                int index = fIndex.applyAsInt(i);

                OfflinePlayer target = bounty.getKey();
                Bounty b = bounty.getValue();

                ItemStack bHead = createPlayerHead(target);
                SkullMeta bMeta = (SkullMeta) bHead.getItemMeta();
                bMeta.setOwner(target.getName());
                bMeta.setDisplayName(AQUA + (target.isOnline() && target.getPlayer().getDisplayName() == null ? target.getPlayer().getDisplayName() : target.getName()));
                bMeta.setLore(Collections.singletonList(YELLOW + format("%,.2f", b.getAmount()) + b.getEconomy().getSymbol()));
                bHead.setItemMeta(bMeta);
                inv.setItem(index, bHead);
            }
        } else {
            List<Bounty> bounties = np.getTopSelfBounties(10);
            for (int i = 0; i < bounties.size(); i++) {
                int index = fIndex.applyAsInt(i);

                Bounty b = bounties.get(i);

                ItemStack bMap = new ItemStack(Material.MAP);
                ItemMeta bMeta = bMap.getItemMeta();
                bMeta.setDisplayName(YELLOW + format("%,.2f", b.getAmount()) + b.getEconomy().getSymbol());
                bMap.setItemMeta(bMeta);

                inv.setItem(index, bMap);
            }
        }

        p.openInventory(inv);
        NovaSound.BLOCK_NOTE_BLOCK_PLING.play(p, 1F, 1F);
    }

    default void callEvent(CommandSender sender, String event, boolean self) {
        if (!sender.hasPermission("novaconomy.admin.tax_event")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!NovaConfig.getConfiguration().hasCustomTaxes()) {
            messages.sendMessage(sender, "error.tax.custom_disabled");
            return;
        }

        Optional<NovaConfig.CustomTaxEvent> customO = NovaConfig.getConfiguration().getAllCustomEvents()
                .stream()
                .filter(e -> e.getIdentifier().equals(event))
                .findFirst();

        if (!customO.isPresent()) {
            messages.sendMessage(sender, "error.tax.custom_inexistent");
            return;
        }

        CommandTaxEvent eventC = new CommandTaxEvent(customO.get());
        Bukkit.getPluginManager().callEvent(eventC);
        if (eventC.isCancelled()) return;

        NovaConfig.CustomTaxEvent custom = eventC.getEvent();

        if (!sender.hasPermission(custom.getPermission())) {
            messages.sendMessage(sender, "error.permission.tax_event");
            return;
        }

        List<UUID> players = (custom.isOnline() ? new ArrayList<>(Bukkit.getOnlinePlayers()) : asList(Bukkit.getOfflinePlayers()))
                .stream()
                .filter(p -> !NovaConfig.getConfiguration().isIgnoredTax(p, custom))
                .map(OfflinePlayer::getUniqueId)
                .collect(Collectors.toList());

        if (sender instanceof Player) {
            UUID uid = ((Player) sender).getUniqueId();
            if (self) {
                if (!players.contains(uid)) players.add(uid);
            } else players.remove(uid);
        }

        List<Price> prices = custom.getPrices();
        boolean deposit = custom.isDepositing();

        for (UUID uid : players)
            NovaUtil.sync(() -> {
                OfflinePlayer p = Bukkit.getOfflinePlayer(uid);
                NovaPlayer np = new NovaPlayer(p);
                prices.forEach(np::remove);
                if (deposit) prices.forEach(Bank::addBalance);

                if (p.isOnline())
                    messages.sendRaw(p.getPlayer(), translateAlternateColorCodes('&', custom.getMessage()));
            });

        messages.sendMessage(sender, "success.tax.custom_event", custom.getName());
    }

    default void settings(Player p, String section) {
        final NovaInventory settings;
        NovaPlayer np = new NovaPlayer(p);

        if (section == null) {
            settings = genGUI(27, get(p, "constants.settings.select"));

            ItemStack personal = builder(createPlayerHead(p),
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.settings.player"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID(SETTING_TAG);
                        nbt.set(SETTING_TAG, "personal");
                    }
            );

            ItemStack language = builder(OAK_SIGN,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.settings.language"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID(SETTING_TAG);
                        nbt.set(SETTING_TAG, "language");
                    }
            );

            ItemStack business = builder(Material.BOOK,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.settings.business"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID(SETTING_TAG);
                        nbt.set(SETTING_TAG, BUSINESS_TAG);
                    });

            ItemStack corporation = builder(Material.IRON_BLOCK,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.settings.corporation"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID(SETTING_TAG);
                        nbt.set(SETTING_TAG, CORPORATION_TAG);
                    });

            settings.addItem(personal, language, business, corporation);
        } else {
            BiFunction<Settings.NovaSetting<?>, Object, ItemStack> func = (sett, valueO) -> {
                Object value = valueO == null ? sett.getDefaultValue() : valueO;
                ItemStack item = value instanceof Boolean ? ((Boolean) value ? LIME_WOOL : RED_WOOL) : CYAN_WOOL;

                return builder(item,
                        meta -> {
                            String sValue;
                            if (value instanceof Boolean)
                                sValue = (Boolean) value ? GREEN + get(p, "constants.on") : RED + get(p, "constants.off");
                            else sValue = AQUA + value.toString().toUpperCase();

                            meta.setDisplayName(YELLOW + sett.getDisplayName() + ": " + sValue);
                            if (value instanceof Boolean && (Boolean) value) {
                                meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            }

                            if (sett.getDescription() != null) {
                                SettingDescription desc = sett.getDescription();
                                List<String> lore = new ArrayList<>();
                                lore.add(" ");
                                lore.addAll(Arrays.stream(ChatPaginator.wordWrap(get(desc.value()), 30)).map(s -> GRAY + s).collect(Collectors.toList()));
                                meta.setLore(lore);
                            }
                        }, nbt -> {
                            nbt.setID("setting_toggle");
                            nbt.set("display", sett.getDisplayName());
                            nbt.set("section", section);
                            nbt.set(SETTING_TAG, sett.name());
                            nbt.set("type", sett.getType());
                            nbt.set("value", value.toString());
                        }
                );
            };

            switch (section.toLowerCase()) {
                case "personal": {
                    settings = genGUI(36, get(p, "constants.settings.player"));

                    for (Settings.Personal sett : Settings.Personal.values()) {
                        boolean value = np.getSetting(sett);
                        settings.addItem(func.apply(sett, value));
                    }
                    break;
                }
                case "language": {
                    settings = Generator.generateLanguageSettings(p);
                    break;
                }
                case BUSINESS_TAG: {
                    if (!Business.exists(p)) {
                        messages.sendMessage(p, "error.business.not_an_owner");
                        return;
                    }

                    Business b = Business.byOwner(p);
                    settings = genGUI(36, get(p, "constants.settings.business"));

                    for (Settings.Business<?> sett : Settings.Business.values()) {
                        Object value = b.getSetting(sett);
                        settings.addItem(func.apply(sett, value));
                    }
                    break;
                }
                case CORPORATION_TAG: {
                    if (!Corporation.exists(p)) {
                        messages.sendError(p, "error.corporation.none");
                        return;
                    }
                    Corporation c = Corporation.byOwner(p);

                    settings = genGUI(36, get(p, "constants.settings.corporation"));

                    for (Settings.Corporation<?> sett : Settings.Corporation.values()) {
                        Object value = c.getSetting(sett);
                        settings.addItem(func.apply(sett, value));
                    }
                    break;
                }
                default: {
                    messages.sendMessage(p, "error.settings.section_inexistent");
                    return;
                }
            }

            settings.setItem(31, builder(BACK, nbt -> nbt.setID("back:settings")));
        }

        settings.setCancelled();

        p.openInventory(settings);
        NovaSound.BLOCK_ANVIL_USE.play(p, 1F, 1.5F);
    }

    default void businessStatistics(Player p, Business b) {
        if (b == null) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        NovaInventory stats = genGUI(45, get(p, "constants.business.statistics"));
        stats.setCancelled();

        BusinessStatistics statistics = b.getStatistics();

        boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(p);
        stats.setItem(12, Items.builder(createPlayerHead(anonymous ? null : b.getOwner()),
                meta -> {
                    meta.setDisplayName(anonymous ? AQUA + get(p, "constants.business.anonymous") : format(p, get(p, "constants.owner"), b.getOwner().getName()));
                    if (b.isOwner(p) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
                        meta.setLore(Collections.singletonList(YELLOW + get(p, "constants.business.hidden")));
                }
        ));

        stats.setItem(14, Items.builder(Material.EGG,
                meta -> {
                    meta.setDisplayName(YELLOW + format(p, get(p, "constants.business.stats.created"), NovaUtil.formatTimeAgo(p, b.getCreationDate().getTime())));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                })
        );

        stats.setItem(20, Items.builder(Material.EMERALD,
                meta -> {
                    meta.setDisplayName(YELLOW + String.valueOf(UNDERLINE) + get(p, "constants.business.stats.global"));
                    meta.setLore(asList(
                            "",
                            format(p, get(p, "constants.stats.global.sold"), format("%,d", statistics.getTotalSales())),
                            format(p, get(p, "constants.business.stats.global.resources"), format("%,d", statistics.getTotalResources())),
                            format(p, get(p, "constants.business.stats.global.ratings"), format("%,d", b.getRatings().size()))
                    ));
                })
        );

        final ItemStack latest;
        if (statistics.hasLatestTransaction()) {
            BusinessStatistics.Transaction latestT = statistics.getLastTransaction();
            Product pr = latestT.getProduct();
            ItemStack prI = pr.getItem();
            OfflinePlayer buyer = latestT.getBuyer();

            latest = Items.builder(createPlayerHead(buyer),
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.business.stats.global.latest"));

                        String display = prI.hasItemMeta() && prI.getItemMeta().hasDisplayName() ? prI.getItemMeta().getDisplayName() : capitalize(prI.getType().name().replace('_', ' '));
                        meta.setLore(asList(
                                AQUA + String.valueOf(UNDERLINE) + (buyer.isOnline() && buyer.getPlayer().getDisplayName() != null ? buyer.getPlayer().getDisplayName() : buyer.getName()),
                                " ",
                                WHITE + display + " (" + prI.getAmount() + ")" + GOLD + " | " + BLUE + format("%,.2f", pr.getAmount() * prI.getAmount()) + pr.getEconomy().getSymbol(),
                                DARK_AQUA + NovaUtil.formatTimeAgo(p, latestT.getTimestamp().getTime())
                        ));
                    }
            );
        } else
            latest = Items.builder(Material.PAPER,
                    meta -> meta.setDisplayName(RESET + get(p, "constants.business.no_transactions"))
            );

        stats.setItem(21, latest);
        stats.setItem(22, Items.builder(Material.matchMaterial("SPYGLASS") == null ? Material.COMPASS : Material.matchMaterial("SPYGLASS"),
                meta -> {
                    meta.setDisplayName(format(p, get(p, "constants.views"), format("%,d", b.getStatistics().getViews())));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                })
        );

        Map<Product, Integer> productSales = statistics.getProductSales();

        stats.setItem(23, Items.builder(Material.GOLD_INGOT,
                meta -> {
                    meta.setDisplayName(YELLOW + get(p, "constants.stats.global.total_made"));

                    List<String> lore = new ArrayList<>();

                    List<Economy> econs = Economy.getEconomies()
                            .stream()
                            .sorted(Collections.reverseOrder(Comparator.comparing(Economy::getName)))
                            .collect(Collectors.toList());

                    Map<Economy, Double> totals = new HashMap<>();

                    AtomicInteger i = new AtomicInteger();
                    for (Economy econ : econs) {
                        if (i.get() > 5) {
                            i.set(-1);
                            break;
                        }

                        double total = 0;
                        for (Product pr : productSales.keySet().stream().filter(pr -> econ.equals(pr.getEconomy())).collect(Collectors.toSet()))
                            total += pr.getAmount() * productSales.get(pr);

                        if (total == 0) continue;
                        totals.put(econ, total);
                        i.incrementAndGet();
                    }

                    List<Map.Entry<Economy, Double>> sortedTotals = totals
                            .entrySet()
                            .stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .collect(Collectors.toList());

                    boolean switcher = false;
                    for (Map.Entry<Economy, Double> entry : sortedTotals) {
                        Economy econ = entry.getKey();
                        double total = entry.getValue();

                        lore.add((switcher ? AQUA : BLUE) + format("%,.2f", total) + econ.getSymbol());
                        switcher = !switcher;
                    }

                    if (i.get() == -1) lore.add(WHITE + "...");
                    meta.setLore(lore);
                })
        );

        final ItemStack top;

        if (!productSales.isEmpty()) {
            List<Map.Entry<Product, Integer>> topProd = productSales
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toList());

            top = Items.builder(Material.DIAMOND,
                    meta -> {
                        meta.setDisplayName(YELLOW + String.valueOf(UNDERLINE) + get(p, "constants.business.stats.global.top"));

                        List<String> pLore = new ArrayList<>();
                        pLore.add(" ");

                        for (int j = 0; j < Math.min(5, topProd.size()); j++) {
                            Map.Entry<Product, Integer> entry = topProd.get(j);
                            Product pr = entry.getKey();
                            int sales = entry.getValue();
                            int num = j + 1;

                            ItemStack item = pr.getItem();
                            String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : capitalize(item.getType().name().replace('_', ' '));

                            pLore.add(YELLOW + "#" + num + ") " + RESET + display + GOLD + " - " + BLUE + format("%,.2f", pr.getAmount()) + pr.getEconomy().getSymbol() + GOLD + " | " + AQUA + format("%,d", sales));
                        }

                        meta.setLore(pLore);
                    });
        } else
            top = Items.builder(Material.PAPER,
                    meta -> meta.setDisplayName(RESET + get(p, "constants.business.no_products"))
            );

        stats.setItem(24, top);

        stats.setItem(40, builder(BACK,
                nbt -> {
                    nbt.setID("business:click");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                }
        ));

        p.openInventory(stats);
    }

    default void rate(Player p, Business b, String comment) {
        if (!p.hasPermission("novaconomy.user.rate")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (b.isOwner(p)) {
            messages.sendMessage(p, "error.business.rate_self");
            return;
        }

        NovaPlayer np = new NovaPlayer(p);

        long time = (np.getLastRating(b).getTime() - System.currentTimeMillis()) + 86400000;
        long timeSecs = Math.floorDiv(time, 1000L);
        final String timeS;

        if (timeSecs < 60)
            timeS = timeSecs + " " + get(p, "constants.time.second");
        else if (timeSecs >= 60 && timeSecs < 3600)
            timeS = format("%,d", Math.floorDiv(timeSecs, 60L)) + get(p, "constants.time.minute");
        else
            timeS = format("%,d", Math.floorDiv(timeSecs, 3600L)) + get(p, "constants.time.hour");

        if (time > 0) {
            messages.send(p, "error.business.rate_time", timeS, b.getName());
            return;
        }

        NovaInventory rate = genGUI(36, format(p, get(p, "constants.rating"), b.getName()));
        rate.setCancelled();

        rate.setItem(13, builder(RATING_MATS[2],
                meta -> meta.setDisplayName(YELLOW + "3⭐"),
                nbt -> {
                    nbt.setID("business:rating");
                    nbt.set("rating", 2);
                }
        ));

        rate.setItem(14, Items.builder(Material.SIGN,
                meta -> meta.setDisplayName(YELLOW + "\"" + (comment.isEmpty() ? get(p, "constants.no_comment") : comment) + "\"")
        ));

        rate.setItem(21, builder(yes("business_rate"),
                meta -> meta.setDisplayName(get(p, "constants.confirm")),
                nbt -> {
                    nbt.setID("yes:business_rate");
                    nbt.set("rating", 2);
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                    nbt.set("comment", comment);
                }
        ));

        rate.setItem(23, CANCEL);

        p.openInventory(rate);
    }

    default void businessRating(Player p, OfflinePlayer target) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (target.equals(p)) {
            messages.sendMessage(p, "error.business.rate_self");
            return;
        }

        Business b = Business.byOwner(p);

        Optional<Rating> r = b.getRatings()
                .stream()
                .filter(ra -> ra.isOwner(target) && !new NovaPlayer(ra.getOwner()).getSetting(Settings.Personal.ANONYMOUS_RATING))
                .findFirst();

        if (!r.isPresent()) {
            messages.sendMessage(p, "error.business.no_rating");
            return;
        }

        Rating rating = r.get();
        NovaInventory pr = genGUI(27, target.getName() + " - \"" + b.getName() + "\"");
        pr.setCancelled();

        pr.setItem(12, Items.builder(createPlayerHead(target),
                meta -> {
                    meta.setDisplayName(YELLOW + target.getName());
                    meta.setLore(Collections.singletonList(AQUA + NovaUtil.formatTimeAgo(p, rating.getTimestamp().getTime())));
                }
        ));

        pr.setItem(14, Items.builder(RATING_MATS[rating.getRatingLevel() - 1],
                meta -> {
                    meta.setDisplayName(YELLOW + String.valueOf(rating.getRatingLevel()) + "⭐");
                    meta.setLore(Collections.singletonList(YELLOW + "\"" + (rating.getComment().isEmpty() ? get(p, "constants.no_comment") : rating.getComment()) + "\""));
                }
        ));

        pr.setItem(22, builder(BACK,
                nbt -> {
                    nbt.setID("business:all_ratings");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                }
        ));

        p.openInventory(pr);
    }

    default void discoverBusinesses(Player p, String... keywords) {
        if (!p.hasPermission("novaconomy.user.business.discover")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!Business.exists()) {
            messages.sendMessage(p, "error.business.none");
            return;
        }

        NovaInventory discover = generateBusinessDiscovery(p, SortingType.BUSINESS_NAME_ASCENDING, keywords);

        if (discover == null) {
            messages.sendMessage(p, "error.business.none_keywords");
            return;
        }

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        p.openInventory(discover);
    }

    default void editPrice(Player p, double newPrice, Economy econ) {
        if (newPrice <= 0) {
            messages.sendMessage(p, "error.argument.amount");
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);
        List<NovaInventory> bData = generateBusinessData(b, p, false, SortingType.PRODUCT_NAME_ASCENDING);
        List<ItemStack> productItems = Arrays.stream(bData.stream().map(Inventory::getContents).flatMap(Arrays::stream).toArray(ItemStack[]::new))
                .filter(Objects::nonNull)
                .map(NBTWrapper::of)
                .filter(NBTWrapper::isProduct)
                .map(nbt -> {
                    Product product = nbt.getProduct(PRODUCT_TAG);

                    nbt.setID("product:edit_price");
                    nbt.set(PRICE_TAG, newPrice);
                    nbt.set(ECON_TAG, product.getEconomy().getUniqueId());
                    return nbt.getItem();
                })
                .collect(Collectors.toList());

        List<NovaInventory> invs = new ArrayList<>();

        int limit = (productItems.size() / GUI_SPACE) + 1;
        for (int i = 0; i < limit; i++) {
            final int fI = i;

            NovaInventory inv = genGUI(54, get(p, "constants.business.select_product"));
            inv.setCancelled();

            if (limit > 1) {
                if (i > 0)
                    inv.setItem(46,
                        NBTWrapper.builder(
                                Items.prev("stored"),
                                nbt -> nbt.set("page", fI)
                        ));

                if (i < (limit - 1))
                    inv.setItem(52,
                            NBTWrapper.builder(
                                    Items.next("stored"),
                                    nbt -> nbt.set("page", fI)
                            ));
            }

            productItems.subList(i * GUI_SPACE, Math.min((i + 1) * GUI_SPACE, productItems.size())).forEach(inv::addItem);

            invs.add(inv);
        }

        invs.forEach(inv -> inv.setAttribute("invs", invs));

        p.openInventory(invs.get(0));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void setBusinessName(Player p, String name) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (name == null || name.isEmpty()) {
            messages.sendMessage(p, "error.argument.name");
            return;
        }

        if (name.length() > Business.MAX_NAME_LENGTH) {
            messages.sendError(p, "error.business.name_length");
            return;
        }

        Business b = Business.byOwner(p);
        Business other = Business.byName(name);
        if (other != null && !other.equals(b)) {
            messages.sendError(p, "error.business.exists_name");
            return;
        }

        b.setName(name);
        messages.sendMessage(p, "success.business.set_name", name);
    }

    default void setBusinessIcon(Player p, Material icon) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (!w.isItem(icon)) {
            messages.sendMessage(p, "error.argument.icon");
            return;
        }

        Business b = Business.byOwner(p);
        b.setIcon(icon);
        messages.sendMessage(p, "success.business.set_icon", capitalize(icon.name().replace("_", " ")));
    }

    default void setEconomyModel(CommandSender sender, Economy econ, int data) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        econ.setCustomModelData(data);
        messages.sendMessage(sender, "success.economy.set_model_data", econ.getName(), data);
    }

    default void setEconomyIcon(CommandSender sender, Economy econ, Material icon) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!w.isItem(icon)) {
            messages.sendMessage(sender, "error.argument.icon");
            return;
        }

        econ.setIcon(icon);
        messages.sendMessage(sender, "success.economy.set_icon", econ.getName(), capitalize(icon.name().replace("_", " ")));
    }

    default void setEconomyScale(CommandSender sender, Economy econ, double scale) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        econ.setConversionScale(scale);
        messages.sendMessage(sender, "success.economy.set_scale", econ.getName(), scale);
    }

    default void setEconomyNatural(CommandSender sender, Economy econ, boolean naturalIncrease) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        econ.setIncreaseNaturally(naturalIncrease);
        messages.sendMessage(sender, "success.economy." + (naturalIncrease ? "enable" : "disable") + "_natural");
    }

    default void playerStatistics(Player p, OfflinePlayer target) {
        Player op = target.getPlayer();
        if (!p.hasPermission("novaconomy.user.stats")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        NovaPlayer np = new NovaPlayer(target);
        PlayerStatistics stats = np.getStatistics();

        NovaInventory inv = genGUI(36, get(p, "constants.player_statistics"));
        inv.setCancelled();

        inv.setItem(4, builder(createPlayerHead(target),
                meta -> {
                    meta.setDisplayName(LIGHT_PURPLE + get(p, "constants.player_statistics"));
                    meta.setLore(Collections.singletonList(YELLOW + (op == null ? target.getName() : op.getDisplayName())));
                }, NBTWrapper::removeID
        ));

        if (!np.getSetting(Settings.Personal.PUBLIC_STATISTICS) && !p.equals(target)) {
            inv.setItem(13, Items.builder(Material.BARRIER,
                    meta -> meta.setDisplayName(RED + get(p, "constants.player_statistics.hidden"))
            ));

            p.openInventory(inv);
            NovaSound.BLOCK_ANVIL_USE.play(p, 1F, 1.5F);
            return;
        }

        inv.setItem(10, 12, 14, 16, 22, LOADING);
        p.openInventory(inv);
        NovaSound.BLOCK_ANVIL_USE.play(p, 1F, 1.5F);

        NovaUtil.async(() -> {
            inv.setItem(10, Items.builder(Material.EMERALD_BLOCK,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.player_statistics.highest_balance"));
                        String s = stats.getHighestBalance() == null ? format("%,.2f", np.getTotalBalance()) : stats.getHighestBalance().toString();

                        meta.setLore(Collections.singletonList(GOLD + s));
                    }
            ));

            inv.setItem(12, Items.builder(Material.DIAMOND_CHESTPLATE,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.player_statistics.business"));
                        meta.setLore(asList(
                                GOLD + format(p, get(p, "constants.player_statistics.business.products_purchased"), format("%,d", stats.getProductsPurchased())),
                                AQUA + format(p, get(p, "constants.player_statistics.business.money_spent"), format("%,.2f", stats.getTotalMoneySpent()))
                        ));
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    })
            );

            inv.setItem(14, Items.builder(Material.GOLD_INGOT,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.player_statistics.bank"));
                        meta.setLore(asList(
                                format(p, get(p, "constants.player_statistics.bank.total_withdrawn"), format("%,.2f", stats.getTotalWithdrawn()))
                        ));
                    })
            );

            Material bountyM = Material.BOW;
            try {
                bountyM = Material.valueOf("TARGET");
            } catch (IllegalArgumentException ignored) {
            }

            inv.setItem(16, Items.builder(bountyM,
                    meta -> {
                        meta.setDisplayName(YELLOW + get(p, "constants.player_statistics.bounty"));
                        meta.setLore(asList(
                                RED + format(p, get(p, "constants.player_statistics.bounty.created"), format("%,d", stats.getTotalBountiesCreated())),
                                DARK_RED + format(p, get(p, "constants.player_statistics.bounty.had"), format("%,d", stats.getTotalBountiesTargeted()))
                        ));
                    }
            ));

            if (np.getSetting(Settings.Personal.PUBLIC_TRANSACTION_HISTORY) || p.equals(target))
                inv.setItem(22, Items.builder(Material.BOOK,
                        meta -> {
                            meta.setDisplayName(YELLOW + get(p, "constants.player_statistics.history"));

                            List<String> lore = new ArrayList<>();
                            List<BusinessStatistics.Transaction> transactions = stats.getTransactionHistory()
                                    .stream()
                                    .sorted(Collections.reverseOrder(Comparator.comparing(BusinessStatistics.Transaction::getTimestamp)))
                                    .collect(Collectors.toList());

                            for (BusinessStatistics.Transaction t : transactions) {
                                Product pr = t.getProduct();
                                ItemStack prItem = pr.getItem();
                                ItemMeta prMeta = prItem.getItemMeta();

                                String display = prMeta.hasDisplayName() ? prMeta.getDisplayName() : capitalize(prItem.getType().name().replace("_", " "));
                                lore.add((prMeta.hasEnchants() ? AQUA : WHITE) + display + " (" + prItem.getAmount() + ")"
                                        + GOLD + " - "
                                        + BLUE + pr.getPrice()
                                        + GOLD + " @ "
                                        + GREEN + (t.getBusiness() == null ? get(p, "constants.unknown") : t.getBusiness().getName())
                                        + GOLD + " | "
                                        + DARK_AQUA + NovaUtil.formatTimeAgo(p, t.getTimestamp().getTime()));
                            }

                            meta.setLore(lore);
                        }
                ));
            else
                inv.setItem(22, null);
        });
    }

    default void businessRecover(Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);
        if (b.getLeftoverStock().isEmpty()) {
            messages.sendMessage(p, "error.business.no_leftover_stock");
            return;
        }

        if (p.getInventory().firstEmpty() == -1) {
            messages.sendMessage(p, "error.player.full_inventory");
            return;
        }

        boolean overflow = false;

        List<ItemStack> items = new ArrayList<>();

        Iterator<ItemStack> it = b.getLeftoverStock().iterator();
        while (it.hasNext()) {
            if (p.getInventory().firstEmpty() == -1) {
                overflow = true;
                break;
            }

            ItemStack item = it.next();
            items.add(item);
            it.remove();
        }
        if (!b.getLeftoverStock().isEmpty()) overflow = true;

        b.removeResource(items);
        p.getInventory().addItem(items.toArray(new ItemStack[0]));

        messages.sendMessage(p, "success.business.recover");

        if (overflow) messages.send(p, "constants.business.stock_overflow");
    }

    default void listKeywords(Player p) {
        if (!p.hasPermission("novaconomy.user.business.keywords")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);

        if (b.getKeywords().isEmpty()) {
            messages.sendMessage(p, "error.business.no_keywords");
            return;
        }

        List<String> msgs = new ArrayList<>();
        msgs.add(DARK_PURPLE + get(p, "constants.business.keywords"));
        for (String keyword : b.getKeywords()) msgs.add(BLUE + "- " + DARK_AQUA + keyword);

        messages.sendRaw(p, msgs);
    }

    default void addKeywords(Player p, String... keywords) {
        if (!p.hasPermission("novaconomy.user.business.keywords")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);

        if (keywords == null) {
            messages.sendMessage(p, "error.argument.keywords");
            return;
        }

        if (b.hasAnyKeywords(keywords)) {
            messages.sendMessage(p, "error.business.keywords_already_added");
            return;
        }

        if (b.getKeywords().size() + keywords.length > 10) {
            messages.sendMessage(p, "error.business.too_many_keywords");
            return;
        }

        b.addKeywords(keywords);
        messages.sendMessage(p, "success.business.add_keywords", keywords.length);
    }

    default void removeKeywords(Player p, String... keywords) {
        if (!p.hasPermission("novaconomy.user.business.keywords")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);

        if (keywords == null) {
            messages.sendMessage(p, "error.argument.keywords");
            return;
        }

        if (!b.hasAllKeywords(keywords)) {
            messages.sendMessage(p, "error.business.keywords_not_added");
            return;
        }

        b.removeKeywords(keywords);
        messages.sendMessage(p, "success.business.remove_keywords", keywords.length);
    }

    default void businessAdvertising(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (!NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            messages.sendMessage(p, "error.business.advertising_disabled");
            return;
        }

        Business b = Business.byOwner(p);
        NovaInventory inv = genGUI(27, get(p, "constants.business.advertising"));
        inv.setCancelled();

        inv.setItem(4, Items.builder(createPlayerHead(p),
                meta -> meta.setDisplayName(DARK_PURPLE + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()))
        ));

        double advertisingBalance = b.getAdvertisingBalance();

        inv.setItem(12, Items.builder(Material.GOLD_INGOT,
                meta -> {
                    meta.setDisplayName(YELLOW + get(p, "constants.business.advertising_balance"));
                    meta.setLore(Collections.singletonList(GOLD + format("%,.2f", advertisingBalance)));
                }
        ));

        double adTotal = Math.max(Math.floor(Business.getBusinesses().stream().mapToDouble(Business::getAdvertisingBalance).sum()), 1);
        inv.setItem(14, Items.builder(Material.PAPER,
                meta -> {
                    meta.setDisplayName(YELLOW + get(p, "constants.other_info"));
                    meta.setLore(asList(
                            GREEN + format(p, get(p, "constants.business.advertising_chance"), GOLD + format("%,.2f", advertisingBalance < NovaConfig.getConfiguration().getBusinessAdvertisingReward() ? 0.0D : (advertisingBalance * 100) / adTotal) + "%")
                    ));
                }
        ));

        inv.setItem(22, builder(BACK,
                nbt -> {
                    nbt.setID("business:click");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                })
        );

        p.openInventory(inv);
    }

    double[] ADVERTISING_AMOUNTS = {
            1, 10, 50, 100, 500, 1000, 5000, 10000, 100000
    };

    default void businessAdvertisingChange(Player p, boolean deposit) {
        if (Economy.getEconomies().isEmpty()) {
            messages.sendMessage(p, "error.economy.none");
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        if (!NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            messages.sendMessage(p, "error.business.advertising_disabled");
            return;
        }

        Business b = Business.byOwner(p);

        NovaInventory inv = genGUI(45, get(p, "constants.business.advertising_" + (deposit ? "deposit" : "withdraw")));
        inv.setCancelled();

        for (int j = 0; j < 2; j++)
            for (int i = 0; i < ADVERTISING_AMOUNTS.length; i++) {
                double am = ADVERTISING_AMOUNTS[i];
                boolean add = j == 0;

                ItemStack change = builder(add ? LIME_STAINED_GLASS_PANE : RED_STAINED_GLASS_PANE,
                        meta -> meta.setDisplayName((add ? GREEN + "+" : RED + "-") + format("%,.0f", am)),
                        nbt -> {
                            nbt.setID("business:change_advertising");
                            nbt.set(AMOUNT_TAG, am);
                            nbt.set(BUSINESS_TAG, b.getUniqueId());
                            nbt.set("add", add);
                        }
                );

                inv.setItem((j * 9) + i + 9, change);
            }

        inv.setItem(31, economyWheel("change_advertising", p));

        inv.setItem(39, builder(CONFIRM,
                nbt -> {
                    nbt.setID("yes:" + (deposit ? "deposit" : "withdraw") + "_advertising");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                    nbt.set(AMOUNT_TAG, 0D);
                }
        ));

        Economy first = Economy.first();
        inv.setItem(40, Items.builder(Material.GOLD_INGOT,
                meta -> meta.setDisplayName(GOLD + "0" + first.getSymbol())
        ));

        inv.setItem(41, CANCEL);

        p.openInventory(inv);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void setEconomyName(CommandSender sender, Economy econ, String name) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (name == null || name.isEmpty()) {
            messages.sendMessage(sender, "error.argument.name");
            return;
        }

        Economy other = Economy.byName(name);
        if (other != null && !other.equals(econ)) {
            messages.sendMessage(sender, "error.economy.exists");
            return;
        }

        String old = econ.getName();
        econ.setName(name);
        messages.sendMessage(sender, "success.economy.set_name", old, name);
    }

    default void listBlacklist(Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);
        List<Business> blacklist = b.getBlacklist();

        if (blacklist.isEmpty()) {
            messages.sendMessage(p, "error.business.no_blacklist");
            return;
        }

        List<String> msgs = new ArrayList<>();
        msgs.add(LIGHT_PURPLE + get(p, "constants.business.blacklist"));
        msgs.add(" ");
        for (Business other : blacklist) {
            if (msgs.size() > 15) {
                msgs.add(WHITE + "...");
                break;
            }
            msgs.add(GOLD + "- " + YELLOW + other.getName());
        }

        messages.sendRaw(p, msgs);
    }

    default void addBlacklist(Player p, Business business) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);
        if (b.isBlacklisted(business)) {
            messages.sendMessage(p, "error.business.exists_blacklist");
            return;
        }

        b.blacklist(business);
        messages.sendMessage(p, "success.business.add_blacklist", business.getName());
    }

    default void removeBlacklist(Player p, Business business) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);
        if (!b.isBlacklisted(business)) {
            messages.sendMessage(p, "error.business.not_blacklisted");
            return;
        }

        b.unblacklist(business);
        messages.sendMessage(p, "success.business.remove_blacklist", business.getName());
    }

    default void allBusinessRatings(Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);

        if (b.getRatings().isEmpty()) {
            messages.sendMessage(p, "error.business.no_ratings");
            return;
        }

        p.openInventory(getRatingsGUI(p, b).get(0));
    }

    default void setEconomyRewardable(CommandSender sender, Economy econ, boolean rewardable) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        econ.setHasClickableReward(rewardable);
        messages.sendMessage(sender, "success.economy." + (rewardable ? "enable" : "disable") + "_reward", econ.getName());
    }

    // Configuration Management Commands

    default void configNaturalCauses(CommandSender sender, String option, String value) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = NovaConfig.loadConfig();

        switch (option.toLowerCase()) {
            case "enchant_bonus": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "EnchantBonus", config.get("NaturalCauses.EnchantBonus"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.EnchantBonus", b);
                messages.sendMessage(sender, "success.config.set", "EnchantBonus", b);
                break;
            }
            case "max_increase": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "MaxIncrease", config.get("NaturalCauses.MaxIncrease"));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < -1) {
                        messages.sendMessage(sender, "error.argument.amount");
                        return;
                    }

                    config.set("NaturalCauses.MaxIncrease", i);
                    messages.sendMessage(sender, "success.config.set", "MaxIncrease", i);
                } catch (NumberFormatException e) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            }
            case "kill_increase": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "KillIncrease", config.get("NaturalCauses.KillIncrease"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.KillIncrease", b);
                messages.sendMessage(sender, "success.config.set", "KillIncrease", b);
                break;
            }
            case "kill_increase_chance": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "KillIncreaseChance", config.get("NaturalCauses.KillIncreaseChance"));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        messages.sendMessage(sender, "error.argument.amount");
                        return;
                    }

                    config.set("NaturalCauses.KillIncreaseChance", i);
                    messages.sendMessage(sender, "success.config.set", "KillIncreaseChance", i);
                } catch (NumberFormatException e) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            }
            case "kill_increase_indirect": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "KillIncreaseIndirect", config.get("NaturalCauses.KillIncreaseIndirect"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.KillIncreaseIndirect", b);
                messages.sendMessage(sender, "success.config.set", "KillIncreaseIndirect", b);
                break;
            }
            case "fishing_increase": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "FishingIncrease", config.get("NaturalCauses.FishingIncrease"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.FishingIncrease", b);
                messages.sendMessage(sender, "success.config.set", "FishingIncrease", b);
                break;
            }
            case "fishing_increase_chance": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "FishingIncreaseChance", config.get("NaturalCauses.FishingIncreaseChance"));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        messages.sendMessage(sender, "error.argument.amount");
                        return;
                    }

                    config.set("NaturalCauses.FishingIncreaseChance", i);
                    messages.sendMessage(sender, "success.config.set", "FishingIncreaseChance", i);
                } catch (NumberFormatException e) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            }
            case "farming_increase": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "FarmingIncrease", config.get("NaturalCauses.FarmingIncrease"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.FarmingIncrease", b);
                messages.sendMessage(sender, "success.config.set", "FarmingIncrease", b);
                break;
            }
            case "farming_increase_chance": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "FarmingIncreaseChance", config.get("NaturalCauses.FarmingIncreaseChance"));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        messages.sendMessage(sender, "error.argument.amount");
                        return;
                    }

                    config.set("NaturalCauses.FarmingIncreaseChance", i);
                    messages.sendMessage(sender, "success.config.set", "FarmingIncreaseChance", i);
                } catch (NumberFormatException e) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            }
            case "mining_increase": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "MiningIncrease", config.get("NaturalCauses.MiningIncrease"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.MiningIncrease", b);
                messages.sendMessage(sender, "success.config.set", "MiningIncrease", b);
                break;
            }
            case "mining_increase_chance": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "MiningIncreaseChance", config.get("NaturalCauses.MiningIncreaseChance"));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        messages.sendMessage(sender, "error.argument.amount");
                        return;
                    }

                    config.set("NaturalCauses.MiningIncreaseChance", i);
                    messages.sendMessage(sender, "success.config.set", "MiningIncreaseChance", i);
                } catch (NumberFormatException e) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            }
            case "death_decrease": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "DeathDecrease", config.get("NaturalCauses.DeathDecrease"));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    messages.sendMessage(sender, "error.argument.bool");
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.DeathDecrease", b);
                messages.sendMessage(sender, "success.config.set", "DeathDecrease", b);
                break;
            }
            case "death_divider": {
                if (value == null) {
                    messages.sendMessage(sender, "success.config.print_value", "DeathDivider", config.get("NaturalCauses.DeathDivider"));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 1) {
                        messages.sendMessage(sender, "error.argument.amount");
                        return;
                    }

                    config.set("NaturalCauses.DeathDivider", i);
                    messages.sendMessage(sender, "success.config.set", "DeathDivider", i);
                } catch (NumberFormatException e) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            }
            default: {
                messages.sendMessage(sender, "error.argument.config");
                return;
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            NovaConfig.print(e);
        }
        reloadFiles();
    }

    default void addCausesModifier(CommandSender sender, String type, String key, String... values) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = NovaConfig.loadConfig();
        ConfigurationSection modConfig = config.getConfigurationSection("NaturalCauses.Modifiers");

        List<Map.Entry<Economy, Double>> mods = null;
        double divider = -1;

        if (type.equalsIgnoreCase("death")) divider = 0;
        else mods = new ArrayList<>();

        Iterator<String> it = ImmutableList.copyOf(values).iterator();
        while (it.hasNext()) {
            String v = it.next();
            if (v == null || v.isEmpty()) {
                it.remove();
                continue;
            }

            v = v.replace(" ", "");
            if (type.equalsIgnoreCase("death")) try {
                divider = Double.parseDouble(v);
                if (divider <= 0) {
                    messages.sendMessage(sender, "error.argument.amount");
                    return;
                }
                break;
            } catch (NumberFormatException e) {
                messages.sendMessage(sender, "error.argument.amount");
                return;
            }
            else {
                Map.Entry<Economy, Double> mod = ModifierReader.readString(v);
                if (mod == null) {
                    messages.sendMessage(sender, "error.argument.modifier");
                    return;
                }

                mods.add(mod);
            }
        }

        if ((divider == -1 && mods.isEmpty()) || (mods == null && divider == -1)) {
            messages.sendMessage(sender, "error.argument.amount");
            return;
        }

        Object value = divider == -1 ?
                mods.size() == 1 ? ModifierReader.toModString(mods.get(0)) : ModifierReader.toModList(mods) :
                divider;

        String entityName = key.toLowerCase().replace("minecraft:", "").toUpperCase();

        switch (type.toLowerCase()) {
            case "mining": {
                if (Material.matchMaterial(key) == null) {
                    messages.sendMessage(sender, "error.argument.block");
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!m.isBlock()) {
                    messages.sendMessage(sender, "error.argument.block");
                    return;
                }

                String modKey = "Mining." + m.name().toLowerCase();

                List<String> newValue = new ArrayList<>();
                if (modConfig.isList(modKey)) newValue.addAll(modConfig.getStringList(modKey));
                else newValue.add(modConfig.getString(modKey));

                if (value instanceof String) newValue.add((String) value);
                else newValue.addAll((List<String>) value);

                modConfig.set(modKey, newValue);
                break;
            }
            case "killing": {
                try {
                    EntityType t = EntityType.valueOf(entityName);

                    if (!LivingEntity.class.isAssignableFrom(t.getEntityClass()) || t == EntityType.PLAYER) {
                        messages.sendMessage(sender, "error.argument.entity");
                        return;
                    }

                    String modKey = "Killing." + t.name().toLowerCase();

                    List<String> newValue = new ArrayList<>();
                    if (modConfig.isList(modKey)) newValue.addAll(modConfig.getStringList(modKey));
                    else newValue.add(modConfig.getString(modKey));

                    if (value instanceof String) newValue.add((String) value);
                    else newValue.addAll((List<String>) value);

                    modConfig.set(modKey, newValue);
                } catch (IllegalArgumentException e) {
                    messages.sendMessage(sender, "error.argument.entity");
                    return;
                }
                break;
            }
            case "fishing": {
                final Enum<?> choice;

                EntityType etype = null;
                try {
                    etype = EntityType.valueOf(entityName);

                    if (!LivingEntity.class.isAssignableFrom(etype.getEntityClass()) || etype == EntityType.PLAYER || !etype.isAlive()) {
                        messages.sendMessage(sender, "error.argument.entity");
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                }

                if (Material.matchMaterial(key) == null && etype == null) {
                    messages.sendMessage(sender, "error.argument.item_entity");
                    return;
                }

                Material m = null;
                if (etype == null) {
                    m = Material.matchMaterial(key);
                    if (!w.isItem(m)) {
                        messages.sendMessage(sender, "error.argument.item");
                        return;
                    }
                }

                choice = etype == null ? m : etype;

                String modKey = "Fishing." + choice.name().toLowerCase();

                List<String> newValue = new ArrayList<>();
                if (modConfig.isList(modKey)) newValue.addAll(modConfig.getStringList(modKey));
                else newValue.add(modConfig.getString(modKey));

                if (value instanceof String) newValue.add((String) value);
                else newValue.addAll((List<String>) value);

                modConfig.set(modKey, newValue);
                break;
            }
            case "farming": {
                if (Material.matchMaterial(key) == null) {
                    messages.sendMessage(sender, "error.argument.crop");
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!w.isCrop(m)) {
                    messages.sendMessage(sender, "error.argument.crop");
                    return;
                }

                String modKey = "Farming." + m.name().toLowerCase();

                List<String> newValue = new ArrayList<>();
                if (modConfig.isList(modKey)) newValue.addAll(modConfig.getStringList(modKey));
                else newValue.add(modConfig.getString(modKey));

                if (value instanceof String) newValue.add((String) value);
                else newValue.addAll((List<String>) value);

                modConfig.set(modKey, newValue);
                break;
            }
            case "death": {
                try {
                    EntityDamageEvent.DamageCause c = EntityDamageEvent.DamageCause.valueOf(key.replace("minecraft:", "").toUpperCase());
                    modConfig.set("Death." + c.name().toLowerCase(), value);
                } catch (IllegalArgumentException e) {
                    messages.sendMessage(sender, "error.argument.cause");
                    return;
                }
                break;
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            NovaConfig.print(e);
        }
        reloadFiles();

        messages.sendMessage(sender, "success.config.add_modifier", type, key);
    }

    default void removeCausesModifier(CommandSender sender, String type, String key) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = NovaConfig.loadConfig();
        ConfigurationSection modConfig = config.getConfigurationSection("NaturalCauses.Modifiers");

        switch (type.toLowerCase()) {
            case "mining": {
                if (Material.matchMaterial(key) == null) {
                    messages.sendMessage(sender, "error.argument.block");
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!m.isBlock() || m == Material.AIR) {
                    messages.sendMessage(sender, "error.argument.block");
                    return;
                }

                if (!modConfig.isSet("Mining." + m.name().toLowerCase())) {
                    messages.sendMessage(sender, "error.config.modifier_inexistent");
                    return;
                }

                modConfig.set("Mining." + m.name().toLowerCase(), null);
                break;
            }
            case "killing": {
                try {
                    EntityType t = EntityType.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!LivingEntity.class.isAssignableFrom(t.getEntityClass()) || t == EntityType.PLAYER) {
                        messages.sendMessage(sender, "error.argument.entity");
                        return;
                    }

                    if (!modConfig.isSet("Killing." + t.name().toLowerCase())) {
                        messages.sendMessage(sender, "error.config.modifier_inexistent");
                        return;
                    }

                    modConfig.set("Killing." + t.name().toLowerCase(), null);
                } catch (IllegalArgumentException e) {
                    messages.sendMessage(sender, "error.argument.entity");
                    return;
                }
                break;
            }
            case "fishing": {
                final Enum<?> choice;

                EntityType etype = null;
                try {
                    etype = EntityType.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!LivingEntity.class.isAssignableFrom(etype.getEntityClass()) || etype == EntityType.PLAYER || !etype.isAlive()) {
                        messages.sendMessage(sender, "error.argument.entity");
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                }

                if (Material.matchMaterial(key) == null && etype == null) {
                    messages.sendMessage(sender, "error.argument.item_entity");
                    return;
                }

                Material m = null;
                if (etype == null) {
                    m = Material.matchMaterial(key);
                    if (!w.isItem(m)) {
                        messages.sendMessage(sender, "error.argument.item");
                        return;
                    }
                }

                choice = etype == null ? m : etype;

                if (!modConfig.isSet("Fishing." + choice.name().toLowerCase())) {
                    messages.sendMessage(sender, "error.config.modifier_inexistent");
                    return;
                }

                modConfig.set("Fishing." + choice.name().toLowerCase(), null);
                break;
            }
            case "farming": {
                if (Material.matchMaterial(key) == null) {
                    messages.sendMessage(sender, "error.argument.crop");
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!w.isCrop(m)) {
                    messages.sendMessage(sender, "error.argument.crop");
                    return;
                }

                if (!modConfig.isSet("Farming." + m.name().toLowerCase())) {
                    messages.sendMessage(sender, "error.config.modifier_inexistent");
                    return;
                }

                modConfig.set("Farming." + m.name().toLowerCase(), null);
                break;
            }
            case "death": {
                try {
                    EntityDamageEvent.DamageCause c = EntityDamageEvent.DamageCause.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!modConfig.isSet("Death." + c.name().toLowerCase())) {
                        messages.sendMessage(sender, "error.config.modifier_inexistent");
                        return;
                    }

                    modConfig.set("Death." + c.name().toLowerCase(), null);
                } catch (IllegalArgumentException e) {
                    messages.sendMessage(sender, "error.argument.cause");
                    return;
                }
                break;
            }
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            NovaConfig.print(e);
        }
        reloadFiles();

        messages.sendMessage(sender, "success.config.remove_modifier", type + "." + key);
    }

    default void viewCausesModifier(CommandSender sender, String type, String key) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        FileConfiguration config = NovaConfig.loadConfig();
        ConfigurationSection modConfig = config.getConfigurationSection("NaturalCauses.Modifiers");

        switch (type.toLowerCase()) {
            case "mining": {
                if (Material.matchMaterial(key) == null) {
                    messages.sendMessage(sender, "error.argument.block");
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!m.isBlock() || m == Material.AIR) {
                    messages.sendMessage(sender, "error.argument.block");
                    return;
                }

                if (!modConfig.isSet("Mining." + m.name().toLowerCase())) {
                    messages.sendMessage(sender, "error.config.modifier_inexistent");
                    return;
                }

                messages.sendMessage(sender, "success.config.view_modifier", type + "." + key, modConfig.get("Mining." + m.name().toLowerCase()));
                break;
            }
            case "killing": {
                try {
                    EntityType t = EntityType.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!LivingEntity.class.isAssignableFrom(t.getEntityClass()) || t == EntityType.PLAYER) {
                        messages.sendMessage(sender, "error.argument.entity");
                        return;
                    }

                    if (!modConfig.isSet("Killing." + t.name().toLowerCase())) {
                        messages.sendMessage(sender, "error.config.modifier_inexistent");
                        return;
                    }

                    messages.sendMessage(sender, "success.config.view_modifier", type + "." + key, modConfig.get("Killing." + t.name().toLowerCase()));
                } catch (IllegalArgumentException e) {
                    messages.sendMessage(sender, "error.argument.entity");
                    return;
                }
                break;
            }
            case "fishing": {
                final Enum<?> choice;

                EntityType etype = null;
                try {
                    etype = EntityType.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!LivingEntity.class.isAssignableFrom(etype.getEntityClass()) || etype == EntityType.PLAYER || !etype.isAlive()) {
                        messages.sendMessage(sender, "error.argument.entity");
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                }

                if (Material.matchMaterial(key) == null && etype == null) {
                    messages.sendMessage(sender, "error.argument.item_entity");
                    return;
                }

                Material m = null;
                if (etype == null) {
                    m = Material.matchMaterial(key);
                    if (!w.isItem(m)) {
                        messages.sendMessage(sender, "error.argument.item");
                        return;
                    }
                }

                choice = etype == null ? m : etype;

                if (!modConfig.isSet("Fishing." + choice.name().toLowerCase())) {
                    messages.sendMessage(sender, "error.config.modifier_inexistent");
                    return;
                }

                messages.sendMessage(sender, "success.config.view_modifier", type + "." + key, modConfig.get("Fishing." + choice.name().toLowerCase()));
                break;
            }
            case "farming": {
                if (Material.matchMaterial(key) == null) {
                    messages.sendMessage(sender, "error.argument.crop");
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!w.isCrop(m)) {
                    messages.sendMessage(sender, "error.argument.crop");
                    return;
                }

                if (!modConfig.isSet("Farming." + m.name().toLowerCase())) {
                    messages.sendMessage(sender, "error.config.modifier_inexistent");
                    return;
                }

                messages.sendMessage(sender, "success.config.view_modifier", type + "." + key, modConfig.get("Farming." + m.name().toLowerCase()));
                break;
            }
            case "death": {
                try {
                    EntityDamageEvent.DamageCause c = EntityDamageEvent.DamageCause.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!modConfig.isSet("Death." + c.name().toLowerCase())) {
                        messages.sendMessage(sender, "error.config.modifier_inexistent");
                        return;
                    }

                    messages.sendMessage(sender, "success.config.view_modifier", type + "." + key, modConfig.get("Death." + c.name().toLowerCase()));
                } catch (IllegalArgumentException e) {
                    messages.sendMessage(sender, "error.argument.cause");
                    return;
                }
                break;
            }
        }
    }

    default void setDefaultEconomy(CommandSender sender, Economy econ) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        if (econ == null) {
            messages.sendMessage(sender, "error.argument.economy");
            return;
        }

        File funcFile = NovaConfig.getFunctionalityFile();
        FileConfiguration func = NovaConfig.loadFunctionalityFile();

        func.set("VaultEconomy", econ == null ? -1 : econ.getName());
        try {
            func.save(funcFile);
        } catch (IOException e) {
            NovaConfig.print(e);
        }
        NovaConfig.getConfiguration().reloadHooks();
        reloadFiles();

        if (econ != null) messages.sendMessage(sender, "success.config.set", "VaultEconomy", econ.getName());
        else messages.sendMessage(sender, "success.config.reset_default_economy");
    }

    List<String> BL_CATEGORIES = asList(
            "ratings",
            "resources",
            "revenue"
    );

    Map<String, Comparator<Business>> BL_COMPARATORS = ImmutableMap.<String, Comparator<Business>>builder()
            .put("ratings", Collections.reverseOrder(Comparator.comparingDouble(Business::getAverageRating))
                    .thenComparing(Collections.reverseOrder(Comparator.comparingInt(b -> b.getRatings().size())))
                    .thenComparing(Business::getName))
            .put("resources", Collections.reverseOrder(Comparator.comparingInt(Business::getTotalResources))
                    .thenComparing(Business::getName))
            .put("revenue", Collections.reverseOrder(Comparator.comparingDouble(Business::getTotalRevenue)
                    .thenComparing(Business::getName)))

            .build();

    Map<String, Function<Business, List<String>>> BL_DESC = ImmutableMap.<String, Function<Business, List<String>>>builder()
            .put("ratings", b -> asList(
                    GOLD + format("%,.1f", b.getAverageRating()) + "⭐",
                    GREEN + format("%,d", b.getRatings().size()) + " " + get("constants.business.ratings")
            ))
            .put("resources", b -> asList(
                    GOLD + format("%,d", b.getTotalResources())
            ))
            .put("revenue", b -> asList(
                    DARK_GREEN + format("%,.2f", b.getTotalRevenue())
            ))
            .build();

    Map<String, Material> BL_ICONS = ImmutableMap.<String, Material>builder()
            .put("ratings", Material.DIAMOND)
            .put("resources", Material.CHEST)
            .put("revenue", Material.GOLD_INGOT)
            .build();

    default void businessLeaderboard(Player p, String category) {
        if (!p.hasPermission("novaconomy.user.leaderboard")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (!Business.exists()) {
            messages.sendMessage(p, "error.business.none");
            return;
        }

        NovaInventory inv = genGUI(54, get(p, "constants.business.leaderboard"));
        inv.setCancelled();

        for (int i = 30; i < 33; i++) inv.setItem(i, LOADING);
        for (int i = 37; i < 44; i++) inv.setItem(i, LOADING);

        p.openInventory(inv);
        scheduler.async(() -> {
            inv.setItem(13, builder(BL_ICONS.get(category),
                    meta -> {
                        meta.setDisplayName(GOLD + get(p, "constants.leaderboard." + category));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID("business:leaderboard_category");
                        nbt.set("category", category);
                    }
            ));

            List<Business> sorted = Business.getBusinesses()
                    .stream()
                    .sorted(BL_COMPARATORS.get(category))
                    .collect(Collectors.toList());

            if (category.equalsIgnoreCase("ratings"))
                sorted = sorted.stream()
                        .filter(b -> !b.getRatings().isEmpty())
                        .collect(Collectors.toList());

            Map<Integer, ItemStack> items = new HashMap<>();
            for (int i = 0; i < 10; i++) {
                int index = 30 + i;
                if (i >= 3) index = 34 + i;

                if (i >= sorted.size()) {
                    items.put(index, null);
                    continue;
                }

                Business b = sorted.get(i);

                ItemStack icon = builder(b.getPublicIcon(),
                        meta -> meta.setLore(BL_DESC.get(category).apply(b)),
                        nbt -> {
                            nbt.setID("business:click");
                            nbt.set(BUSINESS_TAG, b.getUniqueId());
                        }
                );

                items.put(index, icon);
            }

            items.forEach(inv::setItem);

            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        });
    }

    default void basicConfig(CommandSender sender, String key, Object value) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            messages.sendMessage(sender, ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        config.set(key, value);
        try {
            config.save(configFile);
        } catch (IOException e) {
            NovaConfig.print(e);
        }
        reloadFiles();

        messages.sendMessage(sender, "success.config.set", key, value);
    }

    default void corporationInfo(Player p) {
        if (!Corporation.existsByMember(p)) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        Corporation corp = Corporation.byMember(p);
        p.openInventory(generateCorporationData(corp, p, SortingType.BUSINESS_NAME_ASCENDING));

        if (!corp.isOwner(p)) corp.addView();
    }

    default void queryCorporation(Player p, Corporation corp) {
        if (!p.hasPermission("novaconomy.user.corporation.query")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (corp == null) {
            messages.sendError(p, "error.argument.corporation");
            return;
        }

        p.openInventory(generateCorporationData(corp, p, SortingType.BUSINESS_NAME_ASCENDING));
        if (!corp.isOwner(p)) corp.addView();
    }

    default void createCorporation(Player p, String name, Material icon) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (Corporation.exists(p)) {
            messages.sendError(p, "error.corporation.exists");
            return;
        }

        if (Corporation.existsByMember(p)) {
            messages.sendError(p, "error.corporation.exists.member");
            return;
        }

        if (name.length() > Corporation.MAX_NAME_LENGTH) {
            messages.sendError(p, "error.corporation.name.too_long", YELLOW + String.valueOf(Corporation.MAX_NAME_LENGTH) + RED);
            return;
        }

        try {
            Corporation.builder().setName(name).setOwner(p).setIcon(icon).build();
        } catch (UnsupportedOperationException e) {
            messages.sendError(p, "error.corporation.exists.name");
            return;
        }

        messages.sendSuccess(p, "success.corporation.create", name);
    }

    default void deleteCorporation(Player p, boolean confirm) {
        if (!Corporation.exists(p)) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        if (confirm) {
            corp.delete();
            messages.sendSuccess(p, "success.corporation.delete");
        } else messages.sendError(p, "error.corporation.confirm_delete");
    }

    default void setCorporationDescription(Player p, String desc) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (desc == null) {
            messages.sendError(p, "error.argument");
            return;
        }

        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.EDIT_DETAILS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (desc.length() > Corporation.MAX_DESCRIPTION_LENGTH) {
            messages.sendError(p, "error.corporation.description_too_long", YELLOW + String.valueOf(Corporation.MAX_DESCRIPTION_LENGTH) + RED);
            return;
        }

        c.setDescription(desc);
        messages.sendSuccess(p, "success.corporation.description");
    }

    default void setCorporationIcon(Player p, Material icon) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.EDIT_DETAILS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (!w.isItem(icon)) {
            messages.sendError(p, "error.argument.icon");
            return;
        }

        c.setIcon(icon);
        messages.sendSuccess(p, "success.corporation.icon", GOLD + icon.name());
    }

    default void setCorporationHeadquarters(Player p) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.EDIT_DETAILS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (c.getLevel() < 3) {
            messages.sendError(p, "error.corporation.too_low_level");
            return;
        }

        Location l = p.getLocation();
        c.setHeadquarters(l);
        messages.sendSuccess(p, "success.corporation.headquarters",
                GOLD + String.valueOf(l.getBlockX()),
                GOLD + String.valueOf(l.getBlockY()),
                GOLD + String.valueOf(l.getBlockZ())
        );
    }

    default void setCorporationName(Player p, String name) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.EDIT_DETAILS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (name == null || name.isEmpty() || name.length() > Corporation.MAX_NAME_LENGTH) {
            messages.sendError(p, "error.argument.name");
            return;
        }

        c.setName(name);
        messages.sendSuccess(p, "success.corporation.name", name);
    }

    default void corporationAchievements(Player p) {
        if (!Corporation.exists(p)) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        p.openInventory(Generator.generateCorporationAchievements(corp, p));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationLeveling(Player p) {
        if (!Corporation.exists(p)) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        p.openInventory(Generator.generateCorporationLeveling(corp, corp.getLevel(), p));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationStatistics(Player p) {
        if (!Corporation.exists(p)) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        p.openInventory(Generator.generateCorporationStatistics(corp, p));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void inviteBusiness(Player p, Business b) {
        if (!Corporation.exists(p)) {
            messages.sendError(p, "error.corporation.none");
            return;
        }

        if (b.getParentCorporation() != null) {
            messages.sendError(p, "error.corporation.invite.business");
            return;
        }

        Corporation c = Corporation.byMember(p);

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.INVITE_MEMBERS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (c.isBanned(b)) {
            messages.sendError(p, "error.corporation.banned.target");
            return;
        }

        if (c.getSetting(Settings.Corporation.JOIN_TYPE) != Corporation.JoinType.INVITE_ONLY) {
            messages.sendError(p, "error.corporation.invite_only");
            return;
        }

        if (c.getInvited().contains(b)) {
            messages.sendError(p, "error.corporation.invite.already_invited");
            return;
        }

        if (c.getChildren().size() >= c.getMaxChildren()) {
            messages.sendError(p, "error.corporation.max_children");
            return;
        }

        c.inviteBusiness(b);
        messages.sendSuccess(p, "success.corporation.invite.business", GOLD + b.getName());
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void setCorporationExperience(@NotNull CommandSender sender, Corporation c, double exp) {
        if (!sender.hasPermission("novaconomy.admin.corporation.manage_experience")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (exp < 1) {
            messages.sendError(sender, "error.argument.experience_too_low");
            return;
        }

        if (Corporation.toLevel(exp) > Corporation.MAX_LEVEL) {
            messages.sendError(sender, "error.argument.experience_too_high");
            return;
        }

        c.setExperience(exp);
        messages.sendSuccess(sender, "success.corporation.level_experience",
                GOLD + String.valueOf(c.getLevel()),
                GOLD + format("%,.0f", c.getExperience())
        );
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void acceptCorporationInvite(Player p, Corporation from) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.none");
            return;
        }

        Business b = Business.byOwner(p);

        if (from.isBanned(b)) {
            messages.sendError(p, "error.corporation.banned");
            return;
        }

        CorporationInvite invite = b.getInvites()
                .stream()
                .filter(i -> i.getFrom().equals(from))
                .findFirst()
                .orElse(null);

        if (invite == null) {
            messages.sendError(p, "error.corporation.invite.none");
            return;
        }

        if (from.getChildren().size() >= from.getMaxChildren()) {
            messages.sendError(p, "error.corporation.max_children");
            return;
        }

        try {
            invite.accept();
        } catch (IllegalStateException ignored) {
            messages.sendError(p, "error.corporation.accept_invite");
            return;
        }

        messages.sendSuccess(p, "success.corporation.invite.accepted", GOLD + from.getName());
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void declineCorporationInvite(Player p, Corporation from) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.none");
            return;
        }

        Business b = Business.byOwner(p);
        CorporationInvite invite = b.getInvites()
                .stream()
                .filter(i -> i.getFrom().equals(from))
                .findFirst()
                .orElse(null);

        if (invite == null) {
            messages.sendError(p, "error.corporation.invite.none");
            return;
        }

        try {
            invite.decline();
        } catch (IllegalStateException ignored) {
            messages.sendError(p, "error.corporation.decline_invite");
            return;
        }

        messages.sendSuccess(p, "success.corporation.invite.declined", GOLD + from.getName());
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void joinCorporation(@NotNull Player p, Corporation c) {
        if (!p.hasPermission("novaconomy.user.business.join_corporation")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.none");
            return;
        }

        Business b = Business.byOwner(p);

        if (b.getParentCorporation() != null) {
            messages.sendError(p, "error.business.in_corporation");
            return;
        }

        if (c.getChildren().size() >= c.getMaxChildren()) {
            messages.sendError(p, "error.corporation.max_children");
            return;
        }

        if (c.getSetting(Settings.Corporation.JOIN_TYPE) != Corporation.JoinType.PUBLIC) {
            messages.sendError(p, "error.corporation.public_only");
            return;
        }

        c.addChild(b);
        c.broadcastMessage(GREEN + format(p, get(p, "plugin.prefix") + get(p, "broadcast.corporation.join"), AQUA + b.getName()));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void leaveCorporation(@NotNull Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.none");
            return;
        }

        Business b = Business.byOwner(p);

        if (b.getParentCorporation() == null) {
            messages.sendError(p, "error.business.not_in_corporation");
            return;
        }

        Corporation c = b.getParentCorporation();

        if (c.getOwner().equals(p)) {
            messages.sendError(p, "error.corporation.owner_leave");
            return;
        }

        b.leaveParentCorporation();
        c.broadcastMessage(GREEN + format(p, get(p, "plugin.prefix") + get(p, "broadcast.corporation.leave"), AQUA + b.getName()));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationHeadquarters(@NotNull Player p) {
        if (!Corporation.existsByMember(p)) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        Corporation c = Corporation.byMember(p);

        if (c.getHeadquarters() == null) {
            messages.sendError(p, "error.corporation.no_hq");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.TELEPORT_TO_HEADQUARTERS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        CorporationTeleportHeadquartersEvent event = new CorporationTeleportHeadquartersEvent(p, c);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            scheduler.teleport(p, event.getLocation());
            messages.sendRaw(p, AQUA + get(p, "constants.teleporting"));
            NovaSound.ENTITY_ENDERMAN_TELEPORT.playSuccess(p);
        }
    }

    default void corporationChat(@NotNull Player p, String message) {
        if (!Corporation.existsByMember(p)) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        Corporation c = Corporation.byMember(p);

        if (!c.getSetting(Settings.Corporation.CHAT)) {
            messages.sendError(p, "error.corporation.chat_disabled");
            return;
        }

        for (Player m : c.getMembers().stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).collect(Collectors.toList()))
            messages.sendRaw(m, (GOLD + "[" + c.getName() + "] " +
                    GRAY + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()) + DARK_GRAY + " > " +
                    WHITE + translateAlternateColorCodes('&', message)
            ));
    }

    // Market Commands

    default void openMarket(@NotNull Player p, @NotNull Economy econ) {
        if (!p.hasPermission("novaconomy.user.market")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!NovaConfig.getMarket().isMarketEnabled()) {
            messages.sendError(p, "error.market.disabled");
            return;
        }

        Economy econ0;
        if (econ == null)
            econ0 = Economy.getEconomies()
                    .stream()
                    .sorted(Economy::compareTo)
                    .collect(Collectors.toList())
                    .get(0);
        else econ0 = econ;

        NovaPlayer np = new NovaPlayer(p);

        NovaInventory inv;
        if (!np.hasMarketAccess()) {
            inv = genGUI(27, get(p, "constants.market.buy_access"));
            inv.setCancelled();
            for (int i = 0; i < 7; i++) inv.setItem(10 + i, GUI_BACKGROUND);

            inv.setItem(12, economyWheel("market_access", econ0, p));

            inv.setItem(14, NBTWrapper.builder(Material.DIAMOND_BLOCK,
                    meta -> {
                        meta.setDisplayName(GREEN + get(p, "constants.market.buy_access"));
                        meta.setLore(asList(
                                GOLD + format(p, get(p, "constants.price"), format("%,.2f", NovaConfig.getMarket().getMarketMembershipCost(econ0)), String.valueOf(econ0.getSymbol()))
                        ));
                    }, nbt -> {
                        nbt.setID("market:buy_access");
                        nbt.set(ECON_TAG, econ0.getUniqueId());
                    })
            );
        } else
            inv = Generator.generateMarket(p, MarketCategory.MINERALS, SortingType.MATERIAL_TYPE_ASCENDING, econ0, 0);

        p.openInventory(inv);
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void setMarketAccess(@NotNull CommandSender sender, @NotNull OfflinePlayer target, boolean access) {
        if (!sender.hasPermission("novaconomy.admin.market.manage_membership")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);
        nt.setMarketAccess(access);

        messages.sendSuccess(sender, "success.market." + (access ? "enable" : "disable") + "_access", GOLD + target.getName());
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void openSellMarket(@NotNull Player p) {
        if (!p.hasPermission("novaconomy.user.market")) {
            messages.sendMessage(p, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (!NovaConfig.getMarket().isMarketEnabled()) {
            messages.sendError(p, "error.market.disabled");
            return;
        }

        NovaInventory inv = w.createInventory("", get(p, "constants.market.sell_items"), 54);

        inv.setItem(48, builder(Items.NEXT,
                meta -> meta.setDisplayName(BLUE + get(p, "constants.market.sell_items")),
                nbt -> nbt.setID("market:sell_items")
        ));

        inv.setItem(50, economyWheel(p));

        p.openInventory(inv);
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void setMarketPrice(CommandSender sender, Material material, double price) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (material == null) {
            messages.send(sender, "error.argument.item");
            return;
        }

        if (!NovaConfig.getMarket().getAllSold().contains(material)) {
            messages.sendError(sender, "error.market.not_sold", material.name());
            return;
        }

        NovaConfig.getMarket().setPriceOverrides(material, price);
        messages.sendSuccess(sender, "success.market.set_price", GOLD + material.name() + GREEN, GOLD + format("%,.2f", price) + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketRestockEnabled(CommandSender sender, boolean enabled) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaConfig.getMarket().setMarketRestockEnabled(enabled);
        messages.sendSuccess(sender, "success.market." + (enabled ? "enable" : "disable") + "_restock");
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketRestockInterval(CommandSender sender, long interval) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (interval < 1) {
            messages.send(sender, "error.argument.integer");
            return;
        }

        NovaConfig.getMarket().setMarketRestockInterval(interval);
        messages.sendSuccess(sender, "success.market.restock_interval", GOLD + format("%,d", interval) + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketRestockAmount(CommandSender sender, long amount) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (amount < 1) {
            messages.send(sender, "error.argument.amount");
            return;
        }

        NovaConfig.getMarket().setMarketRestockAmount(amount);
        messages.sendSuccess(sender, "success.market.restock_amount", GOLD + format("%,d", amount) + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketMaxPurchases(CommandSender sender, long maxPurchases) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaConfig.getMarket().setMaxPurchases(maxPurchases);
        messages.sendSuccess(sender, "success.market.max_purchases", GOLD + format("%,d", maxPurchases) + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketDepositEnabled(CommandSender sender, boolean enabled) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaConfig.getMarket().setDepositEnabled(enabled);
        messages.sendSuccess(sender, "success.market." + (enabled ? "enable" : "disable") + "_deposit");
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketMembershipEnabled(CommandSender sender, boolean enabled) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaConfig.getMarket().setMarketMembershipEnabled(enabled);
        messages.sendSuccess(sender, "success.market." + (enabled ? "enable" : "disable") + "_membership");
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketMembershipCost(CommandSender sender, double cost) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (cost < 0) {
            messages.send(sender, "error.argument.amount");
            return;
        }

        NovaConfig.getMarket().setMarketMembershipCost(cost);
        messages.sendSuccess(sender, "success.market.membership_cost", GOLD + format("%,.2f", cost) + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketSellPercentage(CommandSender sender, double percentage) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (percentage < 0 || percentage > 100) {
            messages.send(sender, "error.argument.amount");
            return;
        }

        NovaConfig.getMarket().setSellPercentage(percentage);
        messages.sendSuccess(sender, "success.market.sell_percentage", GOLD + format("%,.2f", percentage) + "%" + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketEnabled(CommandSender sender, boolean enabled) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        NovaConfig.getMarket().setMarketEnabled(enabled);
        messages.sendSuccess(sender, "success.market." + (enabled ? "enable" : "disable"));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    List<String> CL_CATEGORIES = asList(
            "ratings",
            "resources",
            "revenue",
            "members"
    );

    Map<String, Comparator<Corporation>> CL_COMPARATORS = ImmutableMap.<String, Comparator<Corporation>>builder()
            .put("ratings", Collections.reverseOrder(Comparator.comparingDouble(Corporation::getAverageRating))
                    .thenComparing(Collections.reverseOrder(Comparator.comparingInt(c -> c.getAllRatings().size())))
                    .thenComparing(Corporation::getName))
            .put("resources", Collections.reverseOrder(Comparator.comparingInt(Corporation::getTotalResources))
                    .thenComparing(Corporation::getName))
            .put("revenue", Collections.reverseOrder(Comparator.comparingDouble(Corporation::getTotalRevenue))
                    .thenComparing(Corporation::getName))
            .put("members", Collections.reverseOrder(Comparator.comparingDouble(Corporation::getMemberCount))
                    .thenComparing(Corporation::getName))
            .build();

    Map<String, Function<Corporation, List<String>>> CL_DESC = ImmutableMap.<String, Function<Corporation, List<String>>>builder()
            .put("ratings", c -> asList(
                    GOLD + format("%,.1f", c.getAverageRating()) + "⭐",
                    GREEN + format("%,d", c.getAllRatings().size()) + " " + get("constants.corporation.ratings")
            ))
            .put("resources", c -> asList(
                    GOLD + format("%,d", c.getTotalResources())
            ))
            .put("revenue", c -> asList(
                    DARK_GREEN + format("%,.2f", c.getTotalRevenue())
            ))
            .put("members", c -> asList(
                    AQUA + format("%,d", c.getMembers().size())
            ))
            .build();

    Map<String, Material> CL_ICONS = ImmutableMap.<String, Material>builder()
            .put("ratings", Material.DIAMOND)
            .put("resources", Material.CHEST)
            .put("revenue", Material.GOLD_INGOT)
            .put("members", Material.ARMOR_STAND)
            .build();

    default void corporationLeaderboard(Player p, String category) {
        if (!p.hasPermission("novaconomy.user.leaderboard")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (!Corporation.exists()) {
            messages.sendMessage(p, "error.corporation.none_exists");
            return;
        }

        NovaInventory inv = genGUI(54, get(p, "constants.corporation.leaderboard"));
        inv.setCancelled();

        for (int i = 30; i < 33; i++) inv.setItem(i, LOADING);
        for (int i = 37; i < 44; i++) inv.setItem(i, LOADING);

        p.openInventory(inv);

        scheduler.async(() -> {
            inv.setItem(13, builder(CL_ICONS.get(category),
                    meta -> {
                        meta.setDisplayName(GOLD + get(p, "constants.leaderboard." + category));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID("corporation:leaderboard_category");
                        nbt.set("category", category);
                    }
            ));

            List<Corporation> sorted = Corporation.getCorporations()
                    .stream()
                    .sorted(CL_COMPARATORS.get(category))
                    .collect(Collectors.toList());

            if (category.equalsIgnoreCase("ratings"))
                sorted = sorted.stream()
                        .filter(c -> !c.getAllRatings().isEmpty())
                        .collect(Collectors.toList());

            Map<Integer, ItemStack> items = new HashMap<>();
            for (int i = 0; i < 10; i++) {
                int index = 30 + i;
                if (i >= 3) index = 34 + i;

                if (i >= sorted.size()) {
                    items.put(index, null);
                    continue;
                }

                Corporation c = sorted.get(i);

                ItemStack icon = builder(c.getPublicIcon(),
                        meta -> meta.setLore(CL_DESC.get(category).apply(c)),
                        nbt -> {
                            nbt.setID("corporation:click");
                            nbt.set(CORPORATION_TAG, c.getUniqueId());
                        }
                );

                items.put(index, icon);
            }

            items.forEach(inv::setItem);

            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        });
    }

    default void businessSupplyChests(Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);
        p.openInventory(Generator.generateBusinessSupplyChests(b, SortingType.BLOCK_LOCATION_ASCENDING, p).get(0));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void addBusinessSupplyChest(Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business bus = Business.byOwner(p);
        Block b = p.getTargetBlock((HashSet<Material>) null, 5);

        if (!(b.getState() instanceof Chest)) {
            messages.sendError(p, "error.business.not_supply_chest");
            return;
        }

        Chest chest = (Chest) b.getState();

        if (bus.getSupplyChests().stream().anyMatch(c -> {
            AtomicBoolean bool = new AtomicBoolean(false);

            bool.compareAndSet(false, c.getLocation().equals(chest.getLocation()));

            if (c.getInventory().getHolder() instanceof DoubleChest) {
                DoubleChest dc = (DoubleChest) c.getInventory().getHolder();

                bool.compareAndSet(false, chest.getInventory().getHolder().equals(dc.getLeftSide()) || chest.getInventory().getHolder().equals(dc.getRightSide()));
            }

            return bool.get();
        })) {
            messages.sendError(p, "error.business.already_supply_chest");
            return;
        }

        if (Business.isSupplyClaimed(b.getLocation())) {
            messages.sendError(p, "error.business.supply_already_claimed");
            return;
        }

        NovaInventory inv = InventorySelector.confirm(p, cInv -> {
            bus.addSupplyChest(b.getLocation());
            messages.sendSuccess(p, "success.business.add_supply_chest",
                    BLUE + String.valueOf(b.getX()) + GREEN,
                    BLUE + String.valueOf(b.getY()) + GREEN,
                    BLUE + String.valueOf(b.getZ()) + GREEN
            );
            p.closeInventory();
        });

        inv.setItem(13, Items.builder(Material.CHEST,
                meta -> meta.setDisplayName(BLUE + b.getWorld().getName() + GOLD + " | " + YELLOW + b.getX() + ", " + b.getY() + ", " + b.getZ())
        ));
        p.openInventory(inv);
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void businessSupply(Player p) {
        if (!Business.exists(p)) {
            messages.sendMessage(p, "error.business.not_an_owner");
            return;
        }

        Business b = Business.byOwner(p);

        if (b.getSupplyChests().isEmpty()) {
            messages.sendError(p, "error.business.no_supply_chests");
            return;
        }

        b.supply();
        messages.sendSuccess(p, "success.business.supply");
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void setEconomyConvertable(CommandSender sender, Economy econ, boolean convertable) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        if (econ == null) {
            messages.sendError(sender, "error.economy.none");
            return;
        }

        econ.setConvertable(convertable);
        messages.sendSuccess(sender, "success.economy." + (convertable ? "enable" : "disable") + "_convertable", GOLD + econ.getName() + GREEN);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void setMarketStock(CommandSender sender, Collection<Material> materials, long amount) {
        if (!sender.hasPermission("novaconomy.admin.market.manage")) {
            messages.sendMessage(sender, ERROR_PERMISSION_ARGUMENT);
            return;
        }

        List<Material> materials0 = materials.stream()
                .filter(m -> NovaConfig.getMarket().getAllSold().contains(m))
                .collect(Collectors.toList());

        if (materials0.isEmpty()) {
            messages.sendError(sender, "error.argument.item");
            return;
        }

        NovaConfig.getMarket().setStock(materials0, amount);

        if (materials0.size() == 1) {
            Material m = materials0.get(0);
            messages.sendSuccess(sender, "success.market.set_stock", GOLD + capitalize(m.name()), DARK_AQUA + format("%,d", amount));
        } else
            messages.sendSuccess(sender, "success.market.set_stock.multiple", GOLD + String.valueOf(materials.size()) + GREEN, DARK_AQUA + format("%,d", amount));

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(sender);
    }

    default void auctionHouse(Player p, @Nullable String searchQuery) {
        if (!p.hasPermission("novaconomy.user.auction_house")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        NovaInventory inv = Generator.generateAuctionHouse(p, SortingType.PRODUCT_NAME_ASCENDING, "").get(0);
        p.openInventory(inv);
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void addAuctionItem(Player p, double amount) {
        if (!p.hasPermission("novaconomy.user.auction_house")) {
            messages.sendMessage(p, ERROR_PERMISSION);
            return;
        }

        if (Economy.getEconomies().isEmpty()) {
            messages.sendError(p, "error.economy.none");
            return;
        }

        if (amount <= 0) {
            messages.sendError(p, "error.argument.amount");
            return;
        }

        if (p.getInventory().getItemInHand() == null || p.getInventory().getItemInHand().getType() == Material.AIR) {
            messages.sendMessage(p, "error.argument.item");
            return;
        }

        ItemStack item = p.getInventory().getItemInHand().clone();
        NovaInventory inv = genGUI(36, get(p, "constants.auction_house.add_item"));
        inv.setCancelled();

        Economy econ = Economy.first();
        inv.setAttribute("item", item);
        inv.setItem(13, builder(item,
                meta -> meta.setLore(
                        Collections.singletonList(GOLD + format(p, get(p, "constants.price"), format("%,.2f", amount), econ.getSymbol()))
                ),
                nbt -> nbt.set(PRICE_TAG, amount)
        ));

        inv.setItem(21, economyWheel("add_product", p));

        inv.setItem(23, button(get(p, "constants.sorting_types.auction.buy_now"), false));
        inv.setItem(24, button(get(p, "constants.loose_price"), false));

        inv.setItem(31, builder(CONFIRM, nbt -> {
            nbt.setID("auction:add_item");
            nbt.set(PRICE_TAG, amount);
        }));

        p.openInventory(inv);
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void setCorporationRank(Player p, Business target, CorporationRank rank) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank pRank = c.getRank(p);

        if (!pRank.hasPermission(CorporationPermission.CHANGE_USER_RANKS) || rank.getPriority() <= pRank.getPriority()) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (!c.getChildren().contains(target)) {
            messages.sendError(p, "error.corporation.not_member");
            return;
        }

        if (c.getOwner().equals(target.getOwner())) {
            messages.sendError(p, "error.corporation.edit_owner_permissions");
            return;
        }

        c.setRank(target, rank);
        messages.sendSuccess(p, "success.corporation.set_rank", GOLD + target.getName() + GREEN, GOLD + rank.getName() + GREEN);
    }

    default void createCorporationRank(Player p, String name, int priority, String prefix, Material icon) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        if (!CorporationRank.VALID_NAME.matcher(name).matches() || name.length() > CorporationRank.MAX_NAME_LENGTH) {
            messages.sendError(p, "error.argument.name");
            return;
        }

        if (!CorporationRank.VALID_PREFIX.matcher(prefix).matches() || prefix.length() > CorporationRank.MAX_PREFIX_LENGTH) {
            messages.sendError(p, "error.argument.prefix");
            return;
        }

        CorporationRank pRank = c.getRank(p);
        if (!pRank.hasPermission(CorporationPermission.CREATE_RANKS) || priority <= pRank.getPriority()) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        Set<CorporationRank> ranks = c.getRanks();

        if (ranks.size() >= c.getMaxRanks()) {
            messages.sendError(p, "error.corporation.max_ranks");
            return;
        }

        if (ranks.stream().anyMatch(r -> r.getName().equalsIgnoreCase(name))) {
            messages.sendError(p, "error.corporation.rank_exists.name");
            return;
        }

        if (ranks.stream().anyMatch(r -> r.getPriority() == priority)) {
            messages.sendError(p, "error.corporation.rank_exists.priority");
            return;
        }

        CorporationRank.builder()
                .setCorporation(c)
                .setName(name)
                .setPriority(priority)
                .setPrefix(prefix)
                .setIcon(icon)
                .build();

        messages.sendSuccess(p, "success.corporation.create_rank", GOLD + name + GREEN);
    }

    default void deleteCorporationRank(Player p, CorporationRank rank, boolean confirm) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank pRank = c.getRank(p);
        if (!pRank.hasPermission(CorporationPermission.MANAGE_RANKS) || rank.getPriority() <= pRank.getPriority()) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (rank.getIdentifier().equals(CorporationRank.OWNER_RANK) || rank.getIdentifier().equals(CorporationRank.DEFAULT_RANK)) {
            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
            return;
        }

        if (confirm) {
            rank.delete();
            messages.sendSuccess(p, "success.corporation.delete_rank", GOLD + rank.getName() + GREEN);
        } else
            messages.sendError(p, "error.corporation.confirm_delete_rank", GOLD + rank.getName() + RED);
    }

    default void editCorporationRank(Player p, CorporationRank rank) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank pRank = c.getRank(p);
        if (!pRank.hasPermission(CorporationPermission.MANAGE_RANKS) || rank.getPriority() <= pRank.getPriority()) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        p.openInventory(Generator.generateCorporationRankEditor(p, rank));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void openCorporationRanks(Player p) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        p.openInventory(Generator.generateCorporationRanks(p, c));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
    }

    default void broadcastCorporationMessage(Player p, String message) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.BROADCAST_MESSAGES)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        c.broadcastMessage(message);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationBan(Player p, Business target) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.BAN_MEMBERS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (c.isBanned(target)) {
            messages.sendError(p, "error.corporation.banned.target");
            return;
        }

        if (c.getOwner().equals(target.getOwner())) {
            messages.sendError(p, "error.corporation.owner_leave");
            return;
        }

        c.ban(target);
        if (target.getOwner().isOnline())
            messages.sendNotification(target.getOwner(), "notification.corporation.ban", GOLD + c.getName() + RED);

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationUnban(Player p, Business target) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.BAN_MEMBERS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (!c.isBanned(target)) {
            messages.sendError(p, "error.corporation.not_banned");
            return;
        }

        c.unban(target);
        if (target.getOwner().isOnline())
            messages.sendNotification(target.getOwner(), "notification.corporation.unban", GOLD + c.getName() + AQUA);

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationKick(Player p, Business target) {
        Corporation c = Corporation.byMember(p);
        if (c == null) {
            messages.sendError(p, "error.corporation.none.member");
            return;
        }

        CorporationRank rank = c.getRank(p);
        if (!rank.hasPermission(CorporationPermission.KICK_MEMBERS)) {
            messages.sendError(p, "error.permission.corporation");
            return;
        }

        if (!c.getChildren().contains(target)) {
            messages.sendError(p, "error.corporation.not_member");
            return;
        }

        if (c.getOwner().equals(target.getOwner())) {
            messages.sendError(p, "error.corporation.owner_leave");
            return;
        }

        c.removeChild(target);
        if (target.getOwner().isOnline())
            messages.sendNotification(target.getOwner(), "notification.corporation.kick", GOLD + c.getName() + RED);

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

}
