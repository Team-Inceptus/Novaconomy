package us.teaminceptus.novaconomy.abstraction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.ChatPaginator;
import us.teaminceptus.novaconomy.ModifierReader;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.CommandTaxEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessAdvertiseEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessViewEvent;
import us.teaminceptus.novaconomy.api.player.Bounty;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.settings.SettingDescription;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.util.Generator;
import us.teaminceptus.novaconomy.util.Items;
import us.teaminceptus.novaconomy.util.NovaSound;
import us.teaminceptus.novaconomy.util.NovaUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.of;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.*;
import static us.teaminceptus.novaconomy.util.Generator.*;
import static us.teaminceptus.novaconomy.util.Items.*;

@SuppressWarnings("unchecked")
public interface CommandWrapper {

    String ERROR_PERMISSION = getMessage("error.permission");

    default void loadCommands() {}

    String BUSINESS_TAG = "business";
    String AMOUNT_TAG = "amount";
    String ECON_TAG = "economy";
    String PRODUCT_TAG = "product";
    String PRICE_TAG = "price";
    String CORPORATION_TAG = "corporation";
    String TYPE_TAG = "type";


    Map<String, List<String>> COMMANDS = new HashMap<String, List<String>>() {{
        put("ehelp", Arrays.asList("nhelp", "novahelp", "econhelp", "economyhelp"));
        put(ECON_TAG, Arrays.asList("econ", "novaecon", "novaconomy", "necon"));
        put("balance", Arrays.asList("bal", "novabal", "nbal"));
        put("convert", Arrays.asList("conv"));
        put("exchange", Arrays.asList("convertgui", "convgui", "exch"));
        put("pay", Arrays.asList("givemoney", "novapay", "econpay", "givebal"));
        put("novaconomyreload", Arrays.asList("novareload", "nreload", "econreload"));
        put(BUSINESS_TAG, Arrays.asList("nbusiness", "b", "nb"));
        put("nbank", Arrays.asList("bank", "globalbank", "gbank"));
        put("createcheck", Arrays.asList("nc", "check", "novacheck", "ncheck"));
        put("balanceleaderboard", Arrays.asList("bleaderboard", "nleaderboard", "bl", "nl", "novaleaderboard", "balboard", "novaboard"));
        put("bounty", Arrays.asList("novabounty", "nbounty"));
        put("taxevent", Arrays.asList("customtax"));
        put("settings", Arrays.asList("novasettings", "nsettings"));
        put("rate", Arrays.asList("nrate", "novarate", "ratebusiness"));
        put("statistics", Arrays.asList("stats", "pstats", "pstatistics", "playerstats", "playerstatistics", "nstats", "nstatistics"));
        put("novaconfig", Arrays.asList("novaconomyconfig", "nconfig", "nconf"));
        put("businessleaderboard", Arrays.asList("bleaderboard", "bboard", "businessl", "bl", "businessboard"));
        put(CORPORATION_TAG, Arrays.asList("corp", "ncorp", "c"));
    }};

    Map<String, String> COMMAND_PERMISSION = new HashMap<String, String>() {{
       put(ECON_TAG, "novaconomy.economy");
       put("balance", "novaconomy.user.balance");
       put("convert", "novaconomy.user.convert");
       put("exchange", "novaconomy.user.convert");
       put("pay", "novaconomy.user.pay");
       put("novaconomyreload", "novaconomy.admin.config");
       put(BUSINESS_TAG, "novaconomy.user.business");
       put("createcheck", "novaconomy.user.check");
       put("balanceleaderboard", "novaconomy.user.leaderboard");
       put("bounty", "novaconomy.user.bounty");
       put("taxevent", "novaconomy.admin.tax_event");
       put("settings", "novaconomy.user.settings");
       put("rate", "novaconomy.user.rate");
       put("statistics", "novaconomy.user.stats");
       put("novaconfig", "novaconomy.admin.config");
       put("businessleaderboard", "novaconomy.user.leaderboard");
       put(CORPORATION_TAG, "novaconomy.user.corporation");
    }};

    Map<String, String> COMMAND_DESCRIPTION = new HashMap<String, String>() {{
       put("ehelp", "Economy help");
       put(ECON_TAG, "Manage economies or their balances");
       put("balance", "Access your balances from all economies");
       put("convert", "Convert one balance in an economy to another balance");
       put("exchange", "Convert one balance in an economy to another balance (with a GUI)");
       put("pay", "Pay another user");
       put("novaconomyreload", "Reload Novaconomy Configuration");
       put(BUSINESS_TAG, "Manage your Novaconomy Business");
       put("nbank", "Interact with the Global Novaconomy Bank");
       put("createcheck", "Create a Novaconomy Check redeemable for a certain amount of money");
       put("balanceleaderboard", "View the top 15 balances in all or certain economies");
       put("bounty", "Manage your Novaconomy Bounties");
       put("taxevent", "Call a Custom Tax Event from the configuration");
       put("settings", "Manage your Novaconomy Settings");
       put("rate", "Rate a Novaconomy Business");
       put("statistics", "View your Novaconomy Statistics");
       put("novaconfig", "View or edit the Novaconomy Configuration");
       put("businessleaderboard", "View the top 10 businesses in various categories");
       put(CORPORATION_TAG, "Manage your Novaconomy Corporation");
    }};

    Map<String, String> COMMAND_USAGE = new HashMap<String, String>() {{
       put("ehelp", "/ehelp");
       put(ECON_TAG, "/economy <create|delete|addbal|removebal|info> <args...>");
       put("balance", "/balance");
       put("convert", "/convert <econ-from> <econ-to> <amount>");
       put("exchange", "/exchange <amount>");
       put("pay", "/pay <player> <economy> <amount>");
       put("novaconomyreload", "/novareload");
       put(BUSINESS_TAG, "/business <create|delete|edit|stock|...> <args...>");
       put("overridelanguages", "/overridelanguages");
       put("createcheck", "/createcheck <economy> <amount>");
       put("balanceleaderboard", "/balanceleaderboard [<economy>]");
       put("bounty", "/bounty <owned|create|delete|self> <args...>");
       put("taxevent", "/taxevent <event> [<self>]");
       put("settings", "/settings [<business|personal>]");
       put("rate", "/rate <business> [<comment>]");
       put("statistics", "/statistics");
       put("novaconfig", "/novaconfig <naturalcauses|reload|rl|...> <args...>");
       put("businessleaderboard", "/businessleaderboard");
       put(CORPORATION_TAG, "/nc <create|delete|edit|...> <args...>");
    }};

    static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Novaconomy");
    }

    // Command Methods

    default void help(CommandSender sender) {
        List<String> commandInfo = new ArrayList<>();
        for (String name : COMMANDS.keySet()) {
            PluginCommand pcmd = Bukkit.getPluginCommand(name);

            if (!sender.isOp() && COMMAND_PERMISSION.get(name) != null && !(sender.hasPermission(COMMAND_PERMISSION.get(name)))) continue;

            if (sender.isOp())
                commandInfo.add(ChatColor.GOLD + "/" + pcmd.getName() + ChatColor.WHITE + " - " + ChatColor.GREEN + COMMAND_DESCRIPTION.get(name) + ChatColor.WHITE + " | " + ChatColor.BLUE + (COMMAND_PERMISSION.get(name) == null ? "No Permissions" : COMMAND_PERMISSION.get(name)));
            else
                commandInfo.add(ChatColor.GOLD + "/" + pcmd.getName() + ChatColor.WHITE + " - " + ChatColor.GREEN + COMMAND_DESCRIPTION.get(name));
        }

        String msg = get("constants.commands") + "\n\n" + String.join("\n", commandInfo.toArray(new String[]{}));
        sender.sendMessage(msg);
    }

    default void balance(Player p) {
        if (!p.hasPermission("novaconomy.user.balance")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        p.sendMessage(ChatColor.GREEN + get("constants.loading"));
        p.openInventory(getBalancesGUI(p, SortingType.ECONOMY_NAME_ASCENDING).get(0));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        sender.sendMessage(get("command.reload.reloading"));
        reloadFiles();
        sender.sendMessage(get("command.reload.success"));
    }

    static void reloadFiles() {
        Plugin plugin = getPlugin();

        plugin.reloadConfig();
        NovaConfig.loadConfig();
        NovaConfig.reloadRunnables();
        NovaConfig.loadFunctionalityFile();
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "businesses.yml"));
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "global.yml"));

        Corporation.reloadCorporations();
        Business.reloadBusinesses();
    }

    default void convert(Player p, Economy from, Economy to, double amount) {
        if (!p.hasPermission("novaconomy.user.convert")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (to.equals(from)) {
            p.sendMessage(getMessage("error.economy.transfer_same"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);

        if (amount <= 0) {
            p.sendMessage(getMessage("error.economy.transfer_amount"));
            return;
        }

        double max = NovaConfig.getConfiguration().getMaxConvertAmount(from);
        if (max >= 0 && amount > max) {
            p.sendMessage(String.format(getMessage("error.economy.transfer_max"), String.format("%,.2f", max) + from.getSymbol(), String.format("%,.2f", amount) + from.getSymbol()));
            return;
        }

        if (np.getBalance(from) < amount) {
            p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), ChatColor.RED + get("constants.convert")));
            return;
        }

        double toBal = from.convertAmount(to, amount);

        np.remove(from, amount);
        np.add(to, toBal);
        p.sendMessage(String.format(getMessage("success.economy.convert"), String.format("%,.2f", amount) + from.getSymbol(), String.format("%,.2f", Math.floor(toBal * 100) / 100)) + to.getSymbol());
    }

    default void exchange(Player p, double amount) {
        if (!p.hasPermission("novaconomy.user.convert")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (Economy.getEconomies().size() < 2) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        if (amount <= 0) {
            p.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        double max = NovaConfig.loadFunctionalityFile().getDouble("MaxConvertAmount");
        if (max >= 0 && amount > max) {
            p.sendMessage(String.format(getMessage("error.economy.transfer_max"), String.format("%,.2f", max),   String.format("%,.2f", amount)));
            return;
        }

        NovaInventory inv = genGUI(36, get("constants.economy.exchange"));
        inv.setCancelled();

        List<Economy> economies = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());

        Economy e1 = economies.get(0);
        Economy e2 = economies.get(1);

        inv.setItem(12, builder(e1.getIcon(),
                meta -> meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + amount + "" + e1.getSymbol())),
                nbt -> {
                    nbt.setID("exchange:1");
                    nbt.set(ECON_TAG, e1.getUniqueId());
                    nbt.set(AMOUNT_TAG, amount);
                })
        );
        inv.setItem(13, Items.ARROW);
        inv.setItem(14, builder(e2.getIcon(),
                meta -> meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + e1.convertAmount(e2, amount) + "" + e2.getSymbol())),
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
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        for (Economy econ : Economy.getEconomies()) {
            if (econ.getName().equalsIgnoreCase(name)) {
                sender.sendMessage(getMessage("error.economy.exists"));
                return;
            }

            if (econ.getSymbol() == symbol) {
                sender.sendMessage(getMessage("error.economy.symbol_exists"));
                return;
            }
        }

        if (scale <= 0) {
            sender.sendMessage(getMessage("error.argument.scale"));
            return;
        }

        try {
            Economy.builder().setName(name).setSymbol(symbol).setIcon(icon).setIncreaseNaturally(naturalIncrease).setConversionScale(scale).setClickableReward(clickableReward).build();
        } catch (UnsupportedOperationException e) {
            sender.sendMessage(getMessage("error.economy.exists"));
            return;
        }
        sender.sendMessage(getMessage("success.economy.create"));
    }

    default void economyInfo(CommandSender sender, Economy econ) {
        if (!(sender.hasPermission("novaconomy.economy.info"))) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        String[] components = {
                String.format(get("constants.economy.info"), econ.getName()),
                String.format(get("constants.economy.natural_increase"), econ.hasNaturalIncrease()),
                String.format(get("constants.economy.symbol"), econ.getSymbol()),
                String.format(get("constants.economy.scale"), Math.floor(econ.getConversionScale() * 100) / 100),
                String.format(get("constants.economy.custom_model_data"), String.format("%,d", econ.getCustomModelData())),
                String.format(get("constants.economy.clickable"), econ.hasClickableReward()),
                String.format(get("constants.economy.taxable"), econ.hasTax()),
        };
        sender.sendMessage(String.join("\n", components));
    }

    default void addBalance(CommandSender sender, Economy econ, Player target, double add) {
        if (!sender.hasPermission("novaconomy.economy.addbalance")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (add < 0) {
            sender.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        nt.add(econ, add);
        sender.sendMessage(String.format(getMessage("success.economy.addbalance"),  String.format("%,.2f", add), econ.getSymbol(), target.getName()));
    }

    default void removeBalance(CommandSender sender, Economy econ, Player target, double remove) {
        if (!sender.hasPermission("novaconomy.economy.removebalance")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (remove < 0) {
            sender.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        nt.remove(econ, remove);
        sender.sendMessage(String.format(getMessage("success.economy.removebalance"),  String.format("%,.2f", remove), econ.getSymbol(), target.getName()));
    }

    default void setBalance(CommandSender sender, Economy econ, Player target, double balance) {
        if (!sender.hasPermission("novaconomy.economy.setbalance")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (balance <= 0) {
            sender.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        nt.setBalance(econ, balance);
        sender.sendMessage(String.format(getMessage("success.economy.setbalance"), target.getName(), econ.getName(), String.format("%,.2f", balance) + econ.getSymbol()));
    }

    default void interest(CommandSender sender, boolean enabled) {
        if (!sender.hasPermission("novaconomy.economy.interest")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaConfig.getConfiguration().setInterestEnabled(enabled);
        String key = "success.economy." + (enabled ? "enable" : "disable" ) + "_interest";
        sender.sendMessage(getMessage(key));
    }

    default void balanceLeaderboard(Player p, Economy econ) {
        if (!p.hasPermission("novaconomy.user.leaderboard")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
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

        NovaInventory inv = genGUI(54, get("constants.balance_leaderboard"));
        inv.setCancelled();

        ItemStack type = builder(Material.PAPER,
                meta -> { if (economy) meta.setDisplayName(ChatColor.AQUA + econ.getName()); else meta.setDisplayName(ChatColor.AQUA + get("constants.all_economies")); },
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
            ChatColor color = new ChatColor[]{ChatColor.GOLD, ChatColor.GRAY, ChatColor.YELLOW, ChatColor.AQUA}[Math.min(i, 3)];

            NovaPlayer np = players.get(i);
            Player op = np.getOnlinePlayer();
            inv.setItem(index, Items.builder(createPlayerHead(np.getPlayer()),
                    meta -> {
                        meta.setDisplayName(color + "#" + level + " - " + (op != null && op.getDisplayName() != null ? op.getDisplayName() : np.getPlayer().getName()));
                        meta.setLore(Collections.singletonList(ChatColor.GOLD + String.format("%,.2f", economy ? np.getBalance(econ) : np.getTotalBalance()) + (economy ? econ.getSymbol() : "")));
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
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (amount < 1) {
            p.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        NovaPlayer nt = new NovaPlayer(p);
        if (take && nt.getBalance(econ) < amount) {
            p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), get("constants.purchase")));
            return;
        }

        p.getInventory().addItem(Generator.createCheck(econ, amount));
        if (take) nt.remove(econ, amount);

        p.sendMessage(String.format(getMessage("success.economy.check"), amount + "", econ.getSymbol() + ""));
    }

    default void removeEconomy(CommandSender sender, Economy econ) {
        if (!(sender.hasPermission("novaconomy.economy.delete"))) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        String name = econ.getName();

        sender.sendMessage(String.format(getMessage("command.economy.delete.deleting"), name));
        Economy.removeEconomy(econ);
        sender.sendMessage(String.format(getMessage("success.economy.delete"), name));
    }

    double[] PAY_AMOUNTS = { 0.5, 1, 10, 100, 1000, 10000, 100000 };

    default void pay(Player p, Player target, Economy economy, double amount) {
        if (!p.hasPermission("novaconomy.user.pay")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(getMessage("error.economy.pay_self"));
            return;
        }

        Economy econ = economy == null ? Economy.getEconomies()
                .stream()
                .sorted(Comparator.comparing(Economy::getName))
                .findFirst().orElse(null)
                : economy;

        if (econ == null) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        NovaInventory inv = genGUI(54, get("constants.pay_player"));
        inv.setCancelled();

        inv.setItem(10, Items.builder(createPlayerHead(p),
                meta -> {
                    meta.setDisplayName(ChatColor.AQUA + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()));
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + String.format("%,.2f", np.getBalance(econ)) + econ.getSymbol()));
                }));
        inv.setItem(16, Items.builder(createPlayerHead(target),
                meta -> meta.setDisplayName(ChatColor.AQUA + (target.getDisplayName() == null ? target.getName() : target.getDisplayName()))
        ));

        inv.setItem(12, Items.ARROW);
        inv.setItem(14, Items.ARROW);

        inv.setItem(13, builder(econ.getIcon().clone(),
                nbt -> {
                    nbt.setID("economy:wheel:pay");
                    nbt.set(ECON_TAG, econ.getUniqueId());
                })
        );

        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 7; j++) {
                boolean add = i == 0;
                double pAmount = PAY_AMOUNTS[j];
                inv.setItem(19 + (i * 9) + j, builder(add ? Items.LIME_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE,
                        meta -> meta.setDisplayName((add ? ChatColor.GREEN + "+" : ChatColor.RED + "-") + String.format("%,.2f", pAmount)),
                        nbt -> {
                            nbt.setID("pay:amount");
                            nbt.set("add", add);
                            nbt.set(AMOUNT_TAG, pAmount);
                        })
                );
            }

        inv.setItem(40, builder(econ.getIcon().clone(),
                meta -> meta.setDisplayName(ChatColor.GOLD + String.format("%,.2f", amount) + econ.getSymbol()),
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
        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (confirm) Business.remove(b);
        else p.sendMessage(String.format(getMessage("constants.confirm_command"), "/business delete confirm"));
    }

    default void removeBusiness(CommandSender sender, Business b, boolean confirm) {
        if (!sender.hasPermission("novaconomy.admin.delete_business")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (confirm) {
            Business.remove(b);
            sender.sendMessage(getMessage("success.business.delete"));
        }
        else sender.sendMessage(String.format(getMessage("constants.confirm_command"), "/business remove <business> confirm"));
    }

    default void businessInfo(Player p) {
        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }
        p.openInventory(generateBusinessData(b, p, false, SortingType.PRODUCT_NAME_ASCENDING));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p, 1F, 0.5F);
    }

    default void businessQuery(Player p, Business b) {
        if (!p.hasPermission("novaconomy.user.business.query")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }
        boolean notOwner = !b.isOwner(p);

        p.openInventory(generateBusinessData(b, p, notOwner, SortingType.PRODUCT_NAME_ASCENDING));
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
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (b.getProducts().size() >= 28) {
            p.sendMessage(getMessage("error.business.too_many_products"));
            return;
        }

        if (p.getItemInHand() == null) {
            p.sendMessage(getMessage("error.argument.item"));
            return;
        }

        if (p.getItemInHand().getType() == Material.AIR) {
            p.sendMessage(getMessage("error.argument.item"));
            return;
        }

        ItemStack pr = p.getItemInHand().clone();
        pr.setAmount(1);

        if (b.isProduct(pr)) {
            p.sendMessage(getMessage("error.business.exists_product"));
            return;
        }

        ItemStack product = p.getItemInHand().clone();
        product.setAmount(1);

        Economy econ = Economy.getEconomies()
                .stream()
                .sorted(Economy::compareTo)
                .collect(Collectors.toList())
                .get(0);

        NovaInventory inv = genGUI(36, pr.hasItemMeta() && pr.getItemMeta().hasDisplayName() ? pr.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(pr.getType().name().replace('_', ' ')));
        inv.setCancelled();

        ItemStack economyWheel = builder(econ.getIconType(),
                meta -> meta.setDisplayName(ChatColor.GOLD + econ.getName()),
                nbt -> {
                    nbt.set(ECON_TAG, econ.getUniqueId());
                    nbt.setID("economy:wheel:add_product");
                });
        modelData(economyWheel, econ.getCustomModelData());
        inv.setItem(22, economyWheel);

        inv.setItem(13, builder(pr,
                meta -> meta.setLore(Collections.singletonList(String.format(get("constants.business.price"), price, econ.getSymbol()) )),
                nbt -> nbt.set(PRICE_TAG, price)
        ));

        inv.setItem(23, builder(CONFIRM,
                nbt -> {
                    nbt.setID("business:add_product");
                    nbt.set("item", product);
                    nbt.set(PRICE_TAG, price);
                    nbt.set(ECON_TAG, econ.getUniqueId());
                }
        ));

        p.openInventory(inv);
    }

    default void createBusiness(Player p, String name, Material icon) {
        if (!p.hasPermission("novaconomy.user.business.create")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        try {
            Business.builder().setOwner(p).setName(name).setIcon(icon).build();
            p.sendMessage(String.format(getMessage("success.business.create"), name));
        } catch (IllegalArgumentException e) {
            p.sendMessage(getMessage("error.argument"));
        } catch (UnsupportedOperationException e) {
            p.sendMessage(getMessage("error.business.exists_name"));
        }
    }

    default void addResource(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        if (!p.hasPermission("novaconomy.user.business.resources")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        NovaInventory inv = w.createInventory("return_items", get("constants.business.add_stock"), 54);
        inv.setAttribute("player", p);
        inv.setAttribute("added", false);
        inv.setAttribute("ignore_ids", ImmutableList.of("business:add_resource"));

        inv.setItem(49, builder(CONFIRM,
                meta -> {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);

                    List<String> lore = new ArrayList<>();
                    for (int i = 1; i < 4; i++) lore.add(get("constants.business.add_resource." + i));

                    meta.setLore(Arrays.asList(ChatPaginator.wordWrap(String.join("\n\n", lore), 30)));
                }, nbt -> nbt.setID("business:add_resource"))
        );

        p.openInventory(inv);
    }

    default void removeProduct(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (b.getProducts().size() < 1) {
            p.sendMessage(getMessage("error.business.no_products"));
            return;
        }

        NovaInventory inv = genGUI(54, get("constants.business.remove_product"));
        inv.setCancelled();

        NovaInventory bData = generateBusinessData(b, p, false, SortingType.PRODUCT_NAME_ASCENDING);

        Arrays.stream(bData.getContents())
                .filter(Objects::nonNull)
                .map(NBTWrapper::of)
                .filter(NBTWrapper::isProduct)
                .forEach(nbt -> {
                    nbt.setID("product:remove");
                    inv.addItem(nbt.getItem());
                });

        p.openInventory(inv);
    }

    default void bankBalances(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        p.sendMessage(ChatColor.BLUE + get("constants.loading"));

        p.openInventory(getBankBalanceGUI(SortingType.ECONOMY_NAME_ASCENDING).get(0));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void bankDeposit(Player p, double amount, Economy econ) {
        NovaPlayer np = new NovaPlayer(p);
        if (np.getBalance(econ) < amount) {
            p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), get("constants.bank.deposit")));
            return;
        }

        if (amount < NovaConfig.getConfiguration().getMinimumPayment(econ)) {
            p.sendMessage(String.format(getMessage("error.bank.minimum_payment"), String.format("%,.2f", NovaConfig.getConfiguration().getMinimumPayment(econ)) + econ.getSymbol(), String.format("%,.2f", amount) + econ.getSymbol()));
            return;
        }

        np.deposit(econ, amount);
        p.sendMessage(String.format(getMessage("success.bank.deposit"), amount + "" + econ.getSymbol(), econ.getName()));
    }

    default void businessHome(Player p, boolean set) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (!p.hasPermission("novaconomy.user.business.home")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (set) {
            Location loc = p.getLocation();
            b.setHome(loc);
            p.sendMessage(String.format(getMessage("success.business.set_home"), ChatColor.GOLD + "" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));
        } else {
            if (!b.hasHome()) {
                p.sendMessage(getMessage("error.business.no_home"));
                return;
            }

            if (b.getHome().distanceSquared(p.getLocation()) < 16) {
                p.sendMessage(getMessage("error.business.too_close_home"));
                return;
            }

            p.sendMessage(ChatColor.DARK_AQUA + get("constants.teleporting"));
            p.teleport(b.getHome());
            NovaSound.  ENTITY_ENDERMAN_TELEPORT.play(p, 1F, 1F);
        }
    }

    default void bankWithdraw(Player p, double amount, Economy econ) {
        if (amount > NovaConfig.getConfiguration().getMaxWithdrawAmount(econ)) {
            p.sendMessage(String.format(getMessage("error.bank.maximum_withdraw"), String.format("%,.2f", NovaConfig.getConfiguration().getMaxWithdrawAmount(econ)) + econ.getSymbol(), String.format("%,.2f", amount) + econ.getSymbol()));
            return;
        }

        if (amount > Bank.getBalance(econ)) {
            p.sendMessage(String.format(getMessage("error.bank.maximum_withdraw"), String.format("%,.2f", Bank.getBalance(econ)) + econ.getSymbol(), String.format("%,.2f", amount) + econ.getSymbol()));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        long time = (np.getLastBankWithdraw().getTimestamp() - System.currentTimeMillis()) + 86400000;
        long timeSecs = (long) Math.floor((double) time / 1000D);
        final String timeS;

        if (timeSecs < 60) timeS = timeSecs + " " + get("constants.time.second");
        else if (timeSecs >= 60 && timeSecs < 3600) timeS = ((long) Math.floor((double) timeSecs / 60D) + " ").replace("L", "") + get("constants.time.minute");
        else timeS = ((long) Math.floor((double) timeSecs / (60D * 60D)) + " ").replace("L", "") + get("constants.time.hour");

        if (time > 0) {
            p.sendMessage(String.format(getMessage("error.bank.withdraw_time"), timeS));
            return;
        }

        np.withdraw(econ, amount);
        p.sendMessage(String.format(getMessage("success.bank.withdraw"), amount + "" + econ.getSymbol(), econ.getName()));
    }

    default void createBounty(Player p, OfflinePlayer target, Economy econ, double amount) {
        if (!p.hasPermission("novaconomy.user.bounty.manage")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!NovaConfig.getConfiguration().hasBounties()) {
            p.sendMessage(getMessage("error.bounty.disabled"));
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(getMessage("error.bounty.self"));
            return;
        }

        if (amount <= 0) {
            p.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        if (np.getBalance(econ) < amount) {
            p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), get("constants.place_bounty")));
            return;
        }

        try {
            Bounty.builder().setOwner(np).setAmount(amount).setTarget(target).setEconomy(econ).build();
            np.remove(econ, amount);
            p.sendMessage(String.format(getMessage("success.bounty.create"), target.getName()));

            if (target.isOnline() && NovaConfig.getConfiguration().hasNotifications())
                target.getPlayer().sendMessage(String.format(getMessage("notification.bounty"), p.getDisplayName() == null ? p.getName() : p.getDisplayName(), String.format("%,.2f", amount) + econ.getSymbol()));
        } catch (UnsupportedOperationException e) {
            p.sendMessage(String.format(getMessage("error.bounty.exists"), target.isOnline() && target.getPlayer().getDisplayName() == null ? target.getName() : target.getPlayer().getDisplayName()));
        }
    }

    default void deleteBounty(Player p, OfflinePlayer target) {
        if (!p.hasPermission("novaconomy.user.bounty.manage")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!NovaConfig.getConfiguration().hasBounties()) {
            p.sendMessage(getMessage("error.bounty.disabled"));
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(getMessage("error.bounty.self"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        FileConfiguration config = np.getPlayerConfig();
        File f = np.getPlayerFile();
        String key = "bounties." + target.getUniqueId();

        if (!config.isSet(key)) {
            p.sendMessage(getMessage("error.bounty.inexistent"));
            return;
        }

        Bounty b = (Bounty) config.get(key);
        np.add(b.getEconomy(), b.getAmount());

        config.set(key, null);
        try { config.save(f); } catch (IOException e) { NovaConfig.print(e); }
        p.sendMessage(String.format(getMessage("success.bounty.delete"), target.getName()));
    }

    default void listBounties(Player p, boolean owned) {
        if (!p.hasPermission("novaconomy.user.bounty.list")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!NovaConfig.getConfiguration().hasBounties()) {
            p.sendMessage(getMessage("error.bounty.disabled"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        FileConfiguration config = np.getPlayerConfig();

        if (owned && !config.isSet("bounties")) {
            p.sendMessage(getMessage("error.bounty.none"));
            return;
        }

        if (!owned && np.getSelfBounties().isEmpty()) {
            p.sendMessage(getMessage("error.bounty.none.self"));
            return;
        }

        NovaInventory inv = genGUI(36, owned ? get("constants.bounty.all") : get("constants.bounty.self"));
        inv.setCancelled();

        for (int i = 10; i < 12; i++) inv.setItem(i, GUI_BACKGROUND);
        for (int i = 15; i < 17; i++) inv.setItem(i, GUI_BACKGROUND);

        ItemStack head = createPlayerHead(p);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()));
        if (owned) meta.setLore(Collections.singletonList(String.format(get("constants.bounty.amount"), np.getOwnedBounties().size())));
        head.setItemMeta(meta);
        inv.setItem(4, head);

        Function<Integer, Integer> fIndex = i -> i > 2 ? i + 16 : i + 12;

        if (owned) {
            List<Map.Entry<OfflinePlayer, Bounty>> bounties = np.getTopBounties(10);
            for (int i = 0; i < bounties.size(); i++) {
                Map.Entry<OfflinePlayer, Bounty> bounty = bounties.get(i);
                int index = fIndex.apply(i);

                OfflinePlayer target = bounty.getKey();
                Bounty b = bounty.getValue();

                ItemStack bHead = createPlayerHead(target);
                SkullMeta bMeta = (SkullMeta) bHead.getItemMeta();
                bMeta.setOwner(target.getName());
                bMeta.setDisplayName(ChatColor.AQUA + (target.isOnline() && target.getPlayer().getDisplayName() == null ? target.getPlayer().getDisplayName() : target.getName()));
                bMeta.setLore(Collections.singletonList(ChatColor.YELLOW + String.format("%,.2f", b.getAmount()) + b.getEconomy().getSymbol()));
                bHead.setItemMeta(bMeta);
                inv.setItem(index, bHead);
            }
        } else {
            List<Bounty> bounties = np.getTopSelfBounties(10);
            for (int i = 0; i < bounties.size(); i++) {
                int index = fIndex.apply(i);

                Bounty b = bounties.get(i);

                ItemStack bMap = new ItemStack(Material.MAP);
                ItemMeta bMeta = bMap.getItemMeta();
                bMeta.setDisplayName(ChatColor.YELLOW + String.format("%,.2f", b.getAmount()) + b.getEconomy().getSymbol());
                bMap.setItemMeta(bMeta);

                inv.setItem(index, bMap);
            }
        }

        p.openInventory(inv);
        NovaSound.BLOCK_NOTE_BLOCK_PLING.play(p, 1F, 1F);
    }

    default void callEvent(CommandSender sender, String event, boolean self) {
        if (!sender.hasPermission("novaconomy.admin.tax_event")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!NovaConfig.getConfiguration().hasCustomTaxes()) {
            sender.sendMessage(getMessage("error.tax.custom_disabled"));
            return;
        }

        Optional<NovaConfig.CustomTaxEvent> customO = NovaConfig.getConfiguration().getAllCustomEvents()
                .stream()
                .filter(e -> e.getIdentifier().equals(event))
                .findFirst();

        if (!customO.isPresent()) {
            sender.sendMessage(getMessage("error.tax.custom_inexistent"));
            return;
        }

        CommandTaxEvent eventC = new CommandTaxEvent(customO.get());
        Bukkit.getPluginManager().callEvent(eventC);
        if (eventC.isCancelled()) return;

        NovaConfig.CustomTaxEvent custom = eventC.getEvent();

        if (!sender.hasPermission(custom.getPermission())) {
            sender.sendMessage(getMessage("error.permission.tax_event"));
            return;
        }

        List<UUID> players = (custom.isOnline() ? new ArrayList<>(Bukkit.getOnlinePlayers()) : Arrays.asList(Bukkit.getOfflinePlayers()))
                .stream()
                .filter(p -> !NovaConfig.getConfiguration().isIgnoredTax(p, custom))
                .map(OfflinePlayer::getUniqueId)
                .collect(Collectors.toList());

        if (sender instanceof Player) {
            UUID uid = ((Player) sender).getUniqueId();
            if (self) { if (!players.contains(uid)) players.add(uid); }
            else players.remove(uid);
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
                    p.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', custom.getMessage()));
            });

        sender.sendMessage(String.format(getMessage("success.tax.custom_event"), custom.getName()));
    }

    default void settings(Player p, String section) {
        final NovaInventory settings;
        NovaPlayer np = new NovaPlayer(p);

        if (section == null) {
            settings = genGUI(27, get("constants.settings.select"));

            ItemStack personal = builder(createPlayerHead(p),
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + get("constants.settings.player"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID("setting");
                        nbt.set("setting", "personal");
                    }
            );

            ItemStack business = builder(Material.BOOK,
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + get("constants.settings.business"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID("setting");
                        nbt.set("setting", BUSINESS_TAG);
                    });

            ItemStack corporation = builder(Material.IRON_BLOCK,
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + get("constants.settings.corporation"));
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }, nbt -> {
                        nbt.setID("setting");
                        nbt.set("setting", CORPORATION_TAG);
                    });

            settings.addItem(personal, business, corporation);
        } else {
            ItemStack back = BACK.clone();
            NBTWrapper bNBT = of(back);

            BiFunction<Settings.NovaSetting<?>, Object, ItemStack> func = (sett, value) -> {
                ItemStack item = value instanceof Boolean ? ((Boolean) value ? LIME_WOOL : RED_WOOL) : CYAN_WOOL;
                return builder(item,
                    meta -> {
                        String sValue;
                        if (value instanceof Boolean)
                            sValue = (Boolean) value ? ChatColor.GREEN + get("constants.on") : ChatColor.RED + get("constants.off");
                        else sValue = ChatColor.AQUA + value.toString().toUpperCase();

                        meta.setDisplayName(ChatColor.YELLOW + sett.getDisplayName() + ": " + sValue);
                        if (value instanceof Boolean && (Boolean) value) {
                            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        }

                        if (sett.getDescription() != null) {
                            SettingDescription desc = sett.getDescription();
                            List<String> lore = new ArrayList<>();
                            lore.add(" ");
                            lore.addAll(Arrays.stream(ChatPaginator.wordWrap(get(desc.value()), 30)).map(s -> ChatColor.GRAY + s).collect(Collectors.toList()));
                            meta.setLore(lore);
                        }
                    }, nbt -> {
                        nbt.setID("setting_toggle");
                        nbt.set("display", sett.getDisplayName());
                        nbt.set("section", section);
                        nbt.set("setting", sett.name());
                        nbt.set("type", sett.getType());
                        nbt.set("value", value.toString());
                    }
                );
            };

            switch (section.toLowerCase()) {
                case "personal": {
                    settings = genGUI(36, "constants.settings.corporation");

                    for (Settings.Personal sett : Settings.Personal.values()) {
                        boolean value = np.getSetting(sett);
                        settings.addItem(func.apply(sett, value));
                    }
                    break;
                }
                case BUSINESS_TAG: {
                    if (!Business.exists(p)) {
                        p.sendMessage(getMessage("error.business.not_an_owner"));
                        return;
                    }

                    Business b = Business.getByOwner(p);
                    settings = genGUI(36, "constants.settings.business");

                    for (Settings.Business sett : Settings.Business.values()) {
                        boolean value = b.getSetting(sett);
                        settings.addItem(func.apply(sett, value));
                    }
                    break;
                }
                case CORPORATION_TAG: {
                    if (!Corporation.exists(p)) {
                        p.sendMessage(getError("error.corporation.none"));
                        return;
                    }
                    Corporation c = Corporation.byOwner(p);

                    settings = genGUI(36, "constants.settings.corporation");

                    for (Settings.Corporation<?> sett : Settings.Corporation.values()) {
                        Object value = c.getSetting(sett);
                        settings.addItem(func.apply(sett, value));
                    }
                    break;
                }
                default: {
                    p.sendMessage(getMessage("error.settings.section_inexistent"));
                    return;
                }
            }

            settings.setItem(31, bNBT.getItem());
        }

        settings.setCancelled();

        p.openInventory(settings);
        NovaSound.BLOCK_ANVIL_USE.play(p, 1F, 1.5F);
    }

    default void businessStatistics(Player p, Business b) {
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        NovaInventory stats = genGUI(45, get("constants.business.statistics"));
        stats.setCancelled();

        BusinessStatistics statistics = b.getStatistics();

        boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(p);
        stats.setItem(12, Items.builder(createPlayerHead(anonymous ? null : b.getOwner()),
                meta -> {
                    meta.setDisplayName(anonymous ? ChatColor.AQUA + get("constants.business.anonymous") : String.format(get("constants.owner"), b.getOwner().getName()));
                    if (b.isOwner(p) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
                        meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                }
        ));

        stats.setItem(14, Items.builder(Material.EGG,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + String.format(get("constants.business.stats.created"), NovaUtil.formatTimeAgo(b.getCreationDate().getTime())));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                })
        );

        stats.setItem(20, Items.builder(Material.EMERALD,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE + get("constants.business.stats.global"));
                    meta.setLore(Arrays.asList(
                            "",
                            String.format(get("constants.stats.global.sold"), String.format("%,d", statistics.getTotalSales())),
                            String.format(get("constants.business.stats.global.resources"), String.format("%,d", statistics.getTotalResources())),
                            String.format(get("constants.business.stats.global.ratings"), String.format("%,d", b.getRatings().size()))
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
                        meta.setDisplayName(ChatColor.YELLOW + get("constants.business.stats.global.latest"));

                        String display = prI.hasItemMeta() && prI.getItemMeta().hasDisplayName() ? prI.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(prI.getType().name().replace('_', ' '));
                        meta.setLore(Arrays.asList(
                                ChatColor.AQUA + "" + ChatColor.UNDERLINE + (buyer.isOnline() && buyer.getPlayer().getDisplayName() != null ? buyer.getPlayer().getDisplayName() : buyer.getName()),
                                " ",
                                ChatColor.WHITE + display + " (" + prI.getAmount() + ")" + ChatColor.GOLD + " | " + ChatColor.BLUE + String.format("%,.2f", pr.getAmount() * prI.getAmount()) + pr.getEconomy().getSymbol(),
                                ChatColor.DARK_AQUA + NovaUtil.formatTimeAgo(latestT.getTimestamp().getTime())
                        ));
                    }
            );
        } else
            latest = Items.builder(Material.PAPER,
                    meta -> meta.setDisplayName(ChatColor.RESET + get("constants.business.no_transactions"))
            );

        stats.setItem(21, latest);
        stats.setItem(22, Items.builder(Material.matchMaterial("SPYGLASS") == null ? Material.COMPASS : Material.matchMaterial("SPYGLASS"),
                meta -> {
                    meta.setDisplayName(String.format(get("constants.views"), String.format("%,d", b.getStatistics().getViews())));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                })
        );

        Map<Product, Integer> productSales = statistics.getProductSales();

        stats.setItem(23, Items.builder(Material.GOLD_INGOT,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.stats.global.total_made"));

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

                        lore.add((switcher ? ChatColor.AQUA : ChatColor.BLUE) + String.format("%,.2f", total) + econ.getSymbol());
                        switcher = !switcher;
                    }

                    if (i.get() == -1) lore.add(ChatColor.WHITE + "...");
                    meta.setLore(lore);
                })
        );

        final ItemStack top;

        if (productSales.size() > 0) {
            List<Map.Entry<Product, Integer>> topProd = productSales
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toList());

            top = Items.builder(Material.DIAMOND,
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE + get("constants.business.stats.global.top"));

                        List<String> pLore = new ArrayList<>();
                        pLore.add(" ");

                        for (int j = 0; j < Math.min(5, topProd.size()); j++) {
                            Map.Entry<Product, Integer> entry = topProd.get(j);
                            Product pr = entry.getKey();
                            int sales = entry.getValue();
                            int num = j + 1;

                            ItemStack item = pr.getItem();
                            String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(item.getType().name().replace('_', ' '));

                            pLore.add(ChatColor.YELLOW + "#" + num + ") " + ChatColor.RESET + display + ChatColor.GOLD + " - " + ChatColor.BLUE + String.format("%,.2f", pr.getAmount()) + pr.getEconomy().getSymbol() + ChatColor.GOLD + " | " + ChatColor.AQUA + String.format("%,d", sales));
                        }

                        meta.setLore(pLore);
                    });
        } else
            top = Items.builder(Material.PAPER,
                    meta -> meta.setDisplayName(ChatColor.RESET + get("constants.business.no_products"))
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
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (b.isOwner(p)) {
            p.sendMessage(getMessage("error.business.rate_self"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);

        long time = (np.getLastRating(b).getTime() - System.currentTimeMillis()) + 86400000;
        long timeSecs = (long) Math.floor((double) time / 1000D);
        final String timeS;

        if (timeSecs < 60) timeS = timeSecs + " " + get("constants.time.second");
        else if (timeSecs >= 60 && timeSecs < 3600) timeS = ((long) Math.floor((double) timeSecs / 60D) + " ").replace("L", "") + get("constants.time.minute");
        else timeS = ((long) Math.floor((double) timeSecs / (60D * 60D)) + " ").replace("L", "") + get("constants.time.hour");

        if (time > 0) {
            p.sendMessage(String.format(get("error.business.rate_time"), timeS, b.getName()));
            return;
        }

        NovaInventory rate = genGUI(36, String.format(get("constants.rating"), b.getName()));
        rate.setCancelled();

        rate.setItem(13, builder(getRatingMats()[2],
                meta -> meta.setDisplayName(ChatColor.YELLOW + "3⭐"),
                nbt -> {
                    nbt.setID("business:rating");
                    nbt.set("rating", 2);
                }
        ));

        rate.setItem(14, Items.builder(Material.SIGN,
                meta -> meta.setDisplayName(ChatColor.YELLOW + "\"" + (comment.isEmpty() ? get("constants.no_comment") : comment) + "\"")
        ));

        rate.setItem(21, builder(yes("business_rate"),
                meta -> meta.setDisplayName(get("constants.confirm")),
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
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(getMessage("error.business.rate_self"));
            return;
        }

        Business b = Business.getByOwner(p);

        Optional<Rating> r = b.getRatings().stream().filter(ra -> ra.isOwner(target) && !new NovaPlayer(ra.getOwner()).getSetting(Settings.Personal.ANONYMOUS_RATING)).findFirst();
        if (!r.isPresent()) {
            p.sendMessage(getMessage("error.business.no_rating"));
            return;
        }

        Rating rating = r.get();
        NovaInventory pr = genGUI(27, target.getName() + " - \"" + b.getName() + "\"");
        pr.setCancelled();

        pr.setItem(12, Items.builder(createPlayerHead(target),
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + target.getName());
                    meta.setLore(Collections.singletonList(ChatColor.AQUA + NovaUtil.formatTimeAgo(rating.getTimestamp().getTime())));
                }
        ));

        pr.setItem(14, Items.builder(getRatingMats()[rating.getRatingLevel() - 1],
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + "" + rating.getRatingLevel() + "⭐");
                    meta.setLore(Collections.singletonList(ChatColor.YELLOW + "\"" + (rating.getComment().isEmpty() ? get("constants.no_comment") : rating.getComment()) + "\""));
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
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!Business.exists()) {
            p.sendMessage(getMessage("error.business.none"));
            return;
        }

        NovaInventory discover = generateBusinessDiscovery(SortingType.BUSINESS_NAME_ASCENDING, keywords);

        if (discover == null) {
            p.sendMessage(getMessage("error.business.none_keywords"));
            return;
        }

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        p.openInventory(discover);
    }

    default void editPrice(Player p, double newPrice, Economy econ) {
        if (newPrice <= 0) {
            p.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        NovaInventory select = genGUI(54, get("constants.business.select_product"));
        select.setCancelled();

        NovaInventory bData = generateBusinessData(b, p, false, SortingType.PRODUCT_NAME_ASCENDING);

        Arrays.stream(bData.getContents())
                .filter(Objects::nonNull)
                .map(NBTWrapper::of)
                .filter(NBTWrapper::isProduct)
                .forEach(nbt -> {
                    Product product = nbt.getProduct(PRODUCT_TAG);

                    nbt.setID("product:edit_price");
                    nbt.set(PRICE_TAG, newPrice);
                    nbt.set(ECON_TAG, product.getEconomy().getUniqueId());

                    select.addItem(nbt.getItem());
                });

        p.openInventory(select);
    }

    default void setBusinessName(Player p, String name) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (name.isEmpty()) {
            p.sendMessage(getMessage("error.argument.empty"));
            return;
        }

        Business other = Business.getByName(name);
        if (other != null && !other.equals(b)) {
            p.sendMessage(getMessage("error.business.exists_name"));
            return;
        }

        b.setName(name);
        p.sendMessage(String.format(getMessage("success.business.set_name"), name));
    }

    default void setBusinessIcon(Player p, Material icon) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        b.setIcon(icon);
        p.sendMessage(String.format(getMessage("success.business.set_icon"), WordUtils.capitalizeFully(icon.name().replace("_", " "))));
    }

    default void setEconomyModel(CommandSender sender, Economy econ, int data) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        econ.setCustomModelData(data);
        sender.sendMessage(String.format(getMessage("success.economy.set_model_data"), econ.getName(), data));
    }

    default void setEconomyIcon(CommandSender sender, Economy econ, Material icon) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!w.isItem(icon)) {
            sender.sendMessage(getMessage("error.argument.icon"));
            return;
        }

        econ.setIcon(icon);
        sender.sendMessage(String.format(getMessage("success.economy.set_icon"), econ.getName(), WordUtils.capitalizeFully(icon.name().replace("_", " "))));
    }

    default void setEconomyScale(CommandSender sender, Economy econ, double scale) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        econ.setConversionScale(scale);
        sender.sendMessage(String.format(getMessage("success.economy.set_scale"), econ.getName(), scale));
    }

    default void setEconomyNatural(CommandSender sender, Economy econ, boolean naturalIncrease) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        econ.setIncreaseNaturally(naturalIncrease);
        sender.sendMessage(getMessage("success.economy." + (naturalIncrease ? "enable" : "disable") + "_natural"));
    }

    default void playerStatistics(Player p, OfflinePlayer target) {
        Player op = target.getPlayer();
        boolean online = op != null;
        if (!p.hasPermission("novaconomy.user.stats")) {
            op.sendMessage(ERROR_PERMISSION);
            return;
        }

        NovaPlayer np = new NovaPlayer(target);
        PlayerStatistics stats = np.getStatistics();

        NovaInventory inv = genGUI(36, get("constants.player_statistics"));
        inv.setCancelled();

        inv.setItem(4, builder(createPlayerHead(target),
                meta -> {
                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + get("constants.player_statistics"));
                    meta.setLore(Collections.singletonList(ChatColor.YELLOW + (online && op.getDisplayName() == null ? target.getName() : op.getDisplayName())));
                }, NBTWrapper::removeID
        ));

        inv.setItem(10, Items.builder(Material.EMERALD_BLOCK,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.highest_balance"));
                    String s = stats.getHighestBalance() == null ? String.format("%,.2f", np.getTotalBalance()) : stats.getHighestBalance().toString();

                    meta.setLore(Collections.singletonList(ChatColor.GOLD + s));
                }
        ));

        inv.setItem(12, Items.builder(Material.DIAMOND_CHESTPLATE,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.business"));
                    meta.setLore(Arrays.asList(
                            ChatColor.GOLD + String.format(get("constants.player_statistics.business.products_purchased"), String.format("%,d", stats.getProductsPurchased())),
                            ChatColor.AQUA + String.format(get("constants.player_statistics.business.money_spent"), String.format("%,.2f", stats.getTotalMoneySpent()))
                    ));
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                })
        );

        inv.setItem(14, Items.builder(Material.GOLD_INGOT,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.bank"));
                    meta.setLore(Arrays.asList(
                            String.format(get("constants.player_statistics.bank.total_withdrawn"), String.format("%,.2f", stats.getTotalWithdrawn()))
                    ));
                })
        );

        Material bountyM = Material.BOW;
        try {
            bountyM = Material.valueOf("TARGET");
        } catch (IllegalArgumentException ignored) {}

        inv.setItem(16, Items.builder(bountyM,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.bounty"));
                    meta.setLore(Arrays.asList(
                            ChatColor.RED + String.format(get("constants.player_statistics.bounty.created"), String.format("%,d", stats.getTotalBountiesCreated())),
                            ChatColor.DARK_RED + String.format(get("constants.player_statistics.bounty.had"), String.format("%,d", stats.getTotalBountiesTargeted()))
                    ));
                }
        ));

        inv.setItem(22, Items.builder(Material.BOOK,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.history"));

                    List<String> lore = new ArrayList<>();
                    List<BusinessStatistics.Transaction> transactions = stats.getTransactionHistory().stream().sorted(Collections.reverseOrder(Comparator.comparing(BusinessStatistics.Transaction::getTimestamp))).collect(Collectors.toList());

                    for (BusinessStatistics.Transaction t : transactions) {
                        Product pr = t.getProduct();
                        ItemStack prItem = pr.getItem();
                        String display = prItem.hasItemMeta() && prItem.getItemMeta().hasDisplayName() ? prItem.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(prItem.getType().name().replace("_", " "));
                        lore.add(ChatColor.WHITE + display + " (" + prItem.getAmount() + ")"
                                + ChatColor.GOLD + " - "
                                + ChatColor.BLUE + pr.getPrice()
                                + ChatColor.GOLD + " @ "
                                + ChatColor.AQUA + (t.getBusiness() == null ? get("constants.unknown") : t.getBusiness().getName())
                                + ChatColor.GOLD + " | "
                                + ChatColor.DARK_AQUA + NovaUtil.formatTimeAgo(t.getTimestamp().getTime()));
                    }

                    meta.setLore(lore);
                }
        ));

        op.openInventory(inv);
        NovaSound.BLOCK_ANVIL_USE.play(p, 1F, 1.5F);
    }

    default void businessRecover(Player p) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b.getLeftoverStock().isEmpty()) {
            p.sendMessage(getMessage("error.business.no_leftover_stock"));
            return;
        }

        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage(getMessage("error.player.full_inventory"));
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
        if (b.getLeftoverStock().size() > 0) overflow = true;

        b.removeResource(items);
        p.getInventory().addItem(items.toArray(new ItemStack[0]));

        p.sendMessage(getMessage("success.business.recover"));

        if (overflow) p.sendMessage(get("constants.business.stock_overflow"));
    }

    default void listKeywords(Player p) {
        if (!p.hasPermission("novaconomy.user.business.keywords")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);

        if (b.getKeywords().isEmpty()) {
            p.sendMessage(getMessage("error.business.no_keywords"));
            return;
        }

        List<String> msgs = new ArrayList<>();
        msgs.add(ChatColor.DARK_PURPLE + get("constants.business.keywords"));
        for (String keyword : b.getKeywords()) msgs.add(ChatColor.BLUE + "- " + ChatColor.DARK_AQUA + keyword);

        p.sendMessage(msgs.toArray(new String[0]));
    }

    default void addKeywords(Player p, String... keywords) {
        if (!p.hasPermission("novaconomy.user.business.keywords")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);

        if (keywords == null) {
            p.sendMessage(getMessage("error.argument.keywords"));
            return;
        }

        if (b.hasAnyKeywords(keywords)) {
            p.sendMessage(getMessage("error.business.keywords_already_added"));
            return;
        }

        if (b.getKeywords().size() + keywords.length > 10) {
            p.sendMessage(getMessage("error.business.too_many_keywords"));
            return;
        }

        b.addKeywords(keywords);
        p.sendMessage(String.format(getMessage("success.business.add_keywords"), keywords.length));
    }

    default void removeKeywords(Player p, String... keywords) {
        if (!p.hasPermission("novaconomy.user.business.keywords")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);

        if (keywords == null) {
            p.sendMessage(getMessage("error.argument.keywords"));
            return;
        }

        if (!b.hasAllKeywords(keywords)) {
            p.sendMessage(getMessage("error.business.keywords_not_added"));
            return;
        }

        b.removeKeywords(keywords);
        p.sendMessage(String.format(getMessage("success.business.remove_keywords"), keywords.length));
    }

    default void businessAdvertising(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (!NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            p.sendMessage(getMessage("error.business.advertising_disabled"));
            return;
        }

        Business b = Business.getByOwner(p);
        NovaInventory inv = genGUI(27, get("constants.business.advertising"));
        inv.setCancelled();

        inv.setItem(4, Items.builder(createPlayerHead(p),
                meta -> meta.setDisplayName(ChatColor.DARK_PURPLE + (p.getDisplayName() == null ? p.getName() : p.getDisplayName()))
        ));

        double advertisingBalance = b.getAdvertisingBalance();

        inv.setItem(12, Items.builder(Material.GOLD_INGOT,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.business.advertising_balance"));
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + String.format("%,.2f", advertisingBalance)));
                }
        ));

        double adTotal = Math.max(Math.floor(Business.getBusinesses().stream().mapToDouble(Business::getAdvertisingBalance).sum()), 1);
        inv.setItem(14, Items.builder(Material.PAPER,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get("constants.other_info"));
                    meta.setLore(Arrays.asList(
                            ChatColor.GREEN + String.format(get("constants.business.advertising_chance"), ChatColor.GOLD + String.format("%,.2f", advertisingBalance < NovaConfig.getConfiguration().getBusinessAdvertisingReward() ? 0.0D : (advertisingBalance * 100) / adTotal) + "%")
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
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (!NovaConfig.getConfiguration().isAdvertisingEnabled()) {
            p.sendMessage(getMessage("error.business.advertising_disabled"));
            return;
        }

        List<Economy> economies = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());
        if (economies.isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        Economy first = economies.stream().findFirst().get();
        Business b = Business.getByOwner(p);

        NovaInventory inv = genGUI(45, get("constants.business.advertising_" + (deposit ? "deposit" : "withdraw")));
        inv.setCancelled();

        for (int j = 0; j < 2; j++)
            for (int i = 0; i < ADVERTISING_AMOUNTS.length; i++) {
                double am = ADVERTISING_AMOUNTS[i];
                boolean add = j == 0;

                ItemStack change = builder(add ? LIME_STAINED_GLASS_PANE : RED_STAINED_GLASS_PANE,
                        meta -> meta.setDisplayName((add ? ChatColor.GREEN + "+" : ChatColor.RED + "-") + String.format("%,.0f", am)),
                        nbt -> {
                            nbt.setID("business:change_advertising");
                            nbt.set(AMOUNT_TAG, am);
                            nbt.set(BUSINESS_TAG, b.getUniqueId());
                            nbt.set("add", add);
                        }
                );

                inv.setItem((j * 9) + i + 9, change);
            }

        inv.setItem(31, builder(first.getIcon(),
                nbt -> {
                    nbt.setID("economy:wheel:change_advertising");
                    nbt.set(ECON_TAG, first.getUniqueId());
                }
        ));

        inv.setItem(39, builder(CONFIRM,
                nbt -> {
                    nbt.setID("yes:" + (deposit ? "deposit" : "withdraw") + "_advertising");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                    nbt.set(AMOUNT_TAG, 0D);
                }
        ));

        inv.setItem(40, Items.builder(Material.GOLD_INGOT,
                meta -> meta.setDisplayName(ChatColor.GOLD + "0" + first.getSymbol())
        ));

        inv.setItem(41, CANCEL);

        p.openInventory(inv);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void setEconomyName(CommandSender sender, Economy econ, String name) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        for (Economy other : Economy.getEconomies()) {
            if (other.equals(econ)) continue;

            if (other.getName().equalsIgnoreCase(name)) {
                sender.sendMessage(getMessage("error.economy.name_exists"));
                return;
            }
        }

        String old = econ.getName();
        econ.setName(name);
        sender.sendMessage(String.format(getMessage("success.economy.set_name"), old, name));
    }

    default void listBlacklist(Player p) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        List<Business> blacklist = b.getBlacklist();

        if (blacklist.isEmpty()) {
            p.sendMessage(getMessage("error.business.no_blacklist"));
            return;
        }

        List<String> msgs = new ArrayList<>();
        msgs.add(ChatColor.LIGHT_PURPLE + get("constants.business.blacklist"));
        msgs.add(" ");
        for (Business other : blacklist) {
            if (msgs.size() > 15) {
                msgs.add(ChatColor.WHITE + "...");
                break;
            }
            msgs.add(ChatColor.GOLD + "- " + ChatColor.YELLOW + other.getName());
        }

        p.sendMessage(msgs.toArray(new String[0]));
    }

    default void addBlacklist(Player p, Business business) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b.isBlacklisted(business)) {
            p.sendMessage(getMessage("error.business.exists_blacklist"));
            return;
        }

        b.blacklist(business);
        p.sendMessage(String.format(getMessage("success.business.add_blacklist"), business.getName()));
    }

    default void removeBlacklist(Player p, Business business) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (!b.isBlacklisted(business)) {
            p.sendMessage(getMessage("error.business.not_blacklisted"));
            return;
        }

        b.unblacklist(business);
        p.sendMessage(String.format(getMessage("success.business.remove_blacklist"), business.getName()));
    }

    default void allBusinessRatings(Player p) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);

        if (b.getRatings().isEmpty()) {
            p.sendMessage(getMessage("error.business.no_ratings"));
            return;
        }

        p.openInventory(getRatingsGUI(p, b).get(0));
    }

    default void setEconomyRewardable(CommandSender sender, Economy econ, boolean rewardable) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        econ.setHasClickableReward(rewardable);
        sender.sendMessage(String.format(getMessage("success.economy." + (rewardable ? "enable" : "disable") + "_reward"), econ.getName()));
    }

    // Configuration Management Commands

    default void configNaturalCauses(CommandSender sender, String option, String value) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = NovaConfig.loadConfig();

        switch (option.toLowerCase()) {
            case "enchant_bonus": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "EnchantBonus", config.get("NaturalCauses.EnchantBonus")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.EnchantBonus", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "EnchantBonus", b));
                break;
            }
            case "max_increase": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "MaxIncrease", config.get("NaturalCauses.MaxIncrease")));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < -1) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return;
                    }

                    config.set("NaturalCauses.MaxIncrease", i);
                    sender.sendMessage(String.format(getMessage("success.config.set"), "MaxIncrease", i));
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            }
            case "kill_increase": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "KillIncrease", config.get("NaturalCauses.KillIncrease")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.KillIncrease", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "KillIncrease", b));
                break;
            }
            case "kill_increase_chance": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "KillIncreaseChance", config.get("NaturalCauses.KillIncreaseChance")));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return;
                    }

                    config.set("NaturalCauses.KillIncreaseChance", i);
                    sender.sendMessage(String.format(getMessage("success.config.set"), "KillIncreaseChance", i));
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            }
            case "kill_increase_indirect": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "KillIncreaseIndirect", config.get("NaturalCauses.KillIncreaseIndirect")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.KillIncreaseIndirect", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "KillIncreaseIndirect", b));
                break;
            }
            case "fishing_increase": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "FishingIncrease", config.get("NaturalCauses.FishingIncrease")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.FishingIncrease", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "FishingIncrease", b));
                break;
            }
            case "fishing_increase_chance": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "FishingIncreaseChance", config.get("NaturalCauses.FishingIncreaseChance")));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return;
                    }

                    config.set("NaturalCauses.FishingIncreaseChance", i);
                    sender.sendMessage(String.format(getMessage("success.config.set"), "FishingIncreaseChance", i));
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            }
            case "farming_increase": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "FarmingIncrease", config.get("NaturalCauses.FarmingIncrease")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.FarmingIncrease", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "FarmingIncrease", b));
                break;
            }
            case "farming_increase_chance": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "FarmingIncreaseChance", config.get("NaturalCauses.FarmingIncreaseChance")));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return;
                    }

                    config.set("NaturalCauses.FarmingIncreaseChance", i);
                    sender.sendMessage(String.format(getMessage("success.config.set"), "FarmingIncreaseChance", i));
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            }
            case "mining_increase": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "MiningIncrease", config.get("NaturalCauses.MiningIncrease")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.MiningIncrease", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "MiningIncrease", b));
                break;
            }
            case "mining_increase_chance": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "MiningIncreaseChance", config.get("NaturalCauses.MiningIncreaseChance")));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 0 || i > 100) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return;
                    }

                    config.set("NaturalCauses.MiningIncreaseChance", i);
                    sender.sendMessage(String.format(getMessage("success.config.set"), "MiningIncreaseChance", i));
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            }
            case "death_decrease": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "DeathDecrease", config.get("NaturalCauses.DeathDecrease")));
                    return;
                }

                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    sender.sendMessage(getMessage("error.argument.bool"));
                    return;
                }

                boolean b = Boolean.parseBoolean(value);
                config.set("NaturalCauses.DeathDecrease", b);
                sender.sendMessage(String.format(getMessage("success.config.set"), "DeathDecrease", b));
                break;
            }
            case "death_divider": {
                if (value == null) {
                    sender.sendMessage(String.format(getMessage("success.config.print_value"), "DeathDivider", config.get("NaturalCauses.DeathDivider")));
                    return;
                }

                try {
                    int i = Integer.parseInt(value);
                    if (i < 1) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return;
                    }

                    config.set("NaturalCauses.DeathDivider", i);
                    sender.sendMessage(String.format(getMessage("success.config.set"), "DeathDivider", i));
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            }
            default: {
                sender.sendMessage(getMessage("error.argument.config"));
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
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = NovaConfig.loadConfig();
        ConfigurationSection modConfig = config.getConfigurationSection("NaturalCauses.Modifiers");

        List<Map.Entry<Economy, Double>> mods = null;
        double divider = -1;

        if (type.equalsIgnoreCase("death")) divider = 0; else mods = new ArrayList<>();

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
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return;
                }
                break;
            } catch (NumberFormatException e) {
                sender.sendMessage(getMessage("error.argument.amount"));
                return;
            }
            else {
                Map.Entry<Economy, Double> mod = ModifierReader.readString(v);
                if (mod == null) {
                    sender.sendMessage(getMessage("error.argument.modifier"));
                    return;
                }

                mods.add(mod);
            }
        }

        if ((divider == -1 && mods.isEmpty()) || (mods == null && divider == -1)) {
            sender.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        Object value = divider == -1 ?
                mods.size() == 1 ? ModifierReader.toModString(mods.get(0)) : ModifierReader.toModList(mods) :
                divider;

        String entityName = key.toLowerCase().replace("minecraft:", "").toUpperCase();

        switch (type.toLowerCase()) {
            case "mining": {
                if (Material.matchMaterial(key) == null) {
                    sender.sendMessage(getMessage("error.argument.block"));
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!m.isBlock()) {
                    sender.sendMessage(getMessage("error.argument.block"));
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
                        sender.sendMessage(getMessage("error.argument.entity"));
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
                    sender.sendMessage(getMessage("error.argument.entity"));
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
                        sender.sendMessage(getMessage("error.argument.entity"));
                        return;
                    }
                } catch (IllegalArgumentException ignored) {}

                if (Material.matchMaterial(key) == null && etype == null) {
                    sender.sendMessage(getMessage("error.argument.item_entity"));
                    return;
                }
                
                Material m = null;
                if (etype == null) {
                    m = Material.matchMaterial(key);
                    if (!w.isItem(m)) {
                        sender.sendMessage(getMessage("error.argument.item"));
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
                    sender.sendMessage(getMessage("error.argument.crop"));
                    return;
                }
                
                Material m = Material.matchMaterial(key);
                if (!w.isCrop(m)) {
                    sender.sendMessage(getMessage("error.argument.crop"));
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
                    sender.sendMessage(getMessage("error.argument.cause"));
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

        sender.sendMessage(String.format(getMessage("success.config.add_modifier"), type, key));
    }

    default void removeCausesModifier(CommandSender sender, String type, String key) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = NovaConfig.loadConfig();
        ConfigurationSection modConfig = config.getConfigurationSection("NaturalCauses.Modifiers");

        switch (type.toLowerCase()) {
            case "mining": {
                if (Material.matchMaterial(key) == null) {
                    sender.sendMessage(getMessage("error.argument.block"));
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!m.isBlock() || m == Material.AIR) {
                    sender.sendMessage(getMessage("error.argument.block"));
                    return;
                }

                if (!modConfig.isSet("Mining." + m.name().toLowerCase())) {
                    sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                    return;
                }

                modConfig.set("Mining." + m.name().toLowerCase(), null);
                break;
            }
            case "killing": {
                try {
                    EntityType t = EntityType.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!LivingEntity.class.isAssignableFrom(t.getEntityClass()) || t == EntityType.PLAYER) {
                        sender.sendMessage(getMessage("error.argument.entity"));
                        return;
                    }

                    if (!modConfig.isSet("Killing." + t.name().toLowerCase())) {
                        sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                        return;
                    }

                    modConfig.set("Killing." + t.name().toLowerCase(), null);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(getMessage("error.argument.entity"));
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
                        sender.sendMessage(getMessage("error.argument.entity"));
                        return;
                    }
                } catch (IllegalArgumentException ignored) {}

                if (Material.matchMaterial(key) == null && etype == null) {
                    sender.sendMessage(getMessage("error.argument.item_entity"));
                    return;
                }

                Material m = null;
                if (etype == null) {
                    m = Material.matchMaterial(key);
                    if (!w.isItem(m)) {
                        sender.sendMessage(getMessage("error.argument.item"));
                        return;
                    }
                }

                choice = etype == null ? m : etype;

                if (!modConfig.isSet("Fishing." + choice.name().toLowerCase())) {
                    sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                    return;
                }

                modConfig.set("Fishing." + choice.name().toLowerCase(), null);
                break;
            }
            case "farming": {
                if (Material.matchMaterial(key) == null) {
                    sender.sendMessage(getMessage("error.argument.crop"));
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!w.isCrop(m)) {
                    sender.sendMessage(getMessage("error.argument.crop"));
                    return;
                }

                if (!modConfig.isSet("Farming." + m.name().toLowerCase())) {
                    sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                    return;
                }

                modConfig.set("Farming." + m.name().toLowerCase(), null);
                break;
            }
            case "death": {
                try {
                    EntityDamageEvent.DamageCause c = EntityDamageEvent.DamageCause.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!modConfig.isSet("Death." + c.name().toLowerCase())) {
                        sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                        return;
                    }

                    modConfig.set("Death." + c.name().toLowerCase(), null);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(getMessage("error.argument.cause"));
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

        sender.sendMessage(String.format(getMessage("success.config.remove_modifier"), type + "." + key));
    }

    default void viewCausesModifier(CommandSender sender, String type, String key) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        FileConfiguration config = NovaConfig.loadConfig();
        ConfigurationSection modConfig = config.getConfigurationSection("NaturalCauses.Modifiers");

        switch (type.toLowerCase()) {
            case "mining": {
                if (Material.matchMaterial(key) == null) {
                    sender.sendMessage(getMessage("error.argument.block"));
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!m.isBlock() || m == Material.AIR) {
                    sender.sendMessage(getMessage("error.argument.block"));
                    return;
                }

                if (!modConfig.isSet("Mining." + m.name().toLowerCase())) {
                    sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                    return;
                }

                sender.sendMessage(String.format(getMessage("success.config.view_modifier"), type + "." + key, modConfig.get("Mining." + m.name().toLowerCase())));
                break;
            }
            case "killing": {
                try {
                    EntityType t = EntityType.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!LivingEntity.class.isAssignableFrom(t.getEntityClass()) || t == EntityType.PLAYER) {
                        sender.sendMessage(getMessage("error.argument.entity"));
                        return;
                    }

                    if (!modConfig.isSet("Killing." + t.name().toLowerCase())) {
                        sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                        return;
                    }

                    sender.sendMessage(String.format(getMessage("success.config.view_modifier"), type + "." + key, modConfig.get("Killing." + t.name().toLowerCase())));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(getMessage("error.argument.entity"));
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
                        sender.sendMessage(getMessage("error.argument.entity"));
                        return;
                    }
                } catch (IllegalArgumentException ignored) {}

                if (Material.matchMaterial(key) == null && etype == null) {
                    sender.sendMessage(getMessage("error.argument.item_entity"));
                    return;
                }

                Material m = null;
                if (etype == null) {
                    m = Material.matchMaterial(key);
                    if (!w.isItem(m)) {
                        sender.sendMessage(getMessage("error.argument.item"));
                        return;
                    }
                }

                choice = etype == null ? m : etype;

                if (!modConfig.isSet("Fishing." + choice.name().toLowerCase())) {
                    sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                    return;
                }

                sender.sendMessage(String.format(getMessage("success.config.view_modifier"), type + "." + key, modConfig.get("Fishing." + choice.name().toLowerCase())));
                break;
            }
            case "farming": {
                if (Material.matchMaterial(key) == null) {
                    sender.sendMessage(getMessage("error.argument.crop"));
                    return;
                }

                Material m = Material.matchMaterial(key);
                if (!w.isCrop(m)) {
                    sender.sendMessage(getMessage("error.argument.crop"));
                    return;
                }

                if (!modConfig.isSet("Farming." + m.name().toLowerCase())) {
                    sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                    return;
                }

                sender.sendMessage(String.format(getMessage("success.config.view_modifier"), type + "." + key, modConfig.get("Farming." + m.name().toLowerCase())));
                break;
            }
            case "death": {
                try {
                    EntityDamageEvent.DamageCause c = EntityDamageEvent.DamageCause.valueOf(key.replace("minecraft:", "").toUpperCase());

                    if (!modConfig.isSet("Death." + c.name().toLowerCase())) {
                        sender.sendMessage(getMessage("error.config.modifier_inexistent"));
                        return;
                    }

                    sender.sendMessage(String.format(getMessage("success.config.view_modifier"), type + "." + key, modConfig.get("Death." + c.name().toLowerCase())));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(getMessage("error.argument.cause"));
                    return;
                }
                break;
            }
        }
    }

    default void setDefaultEconomy(CommandSender sender, Economy econ) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        File funcFile = NovaConfig.getFunctionalityFile();
        FileConfiguration func = NovaConfig.loadFunctionalityFile();

        func.set("VaultEconomy", econ == null ? -1 : econ.getName());
        try { func.save(funcFile); } catch (IOException e) { NovaConfig.print(e); }
        NovaConfig.getConfiguration().reloadHooks();
        reloadFiles();

        if (econ != null) sender.sendMessage(String.format(getMessage("success.config.set"), "VaultEconomy", econ.getName()));
        else sender.sendMessage(getMessage("success.config.reset_default_economy"));
    }

    List<String> BL_CATEGORIES = Arrays.asList(
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
            .put("ratings", b -> Arrays.asList(
                        ChatColor.GOLD + String.format("%,.1f", b.getAverageRating()) + "⭐",
                        ChatColor.GREEN + String.format("%,d", b.getRatings().size()) + " " + get("constants.business.ratings")
                    ))
            .put("resources", b -> Arrays.asList(
                        ChatColor.GOLD + String.format("%,d", b.getTotalResources())
                    ))
            .put("revenue", b -> Arrays.asList(
                        ChatColor.DARK_GREEN + String.format("%,.2f", b.getTotalRevenue())
                    ))
            .build();

    Map<String, Material> BL_ICONS = ImmutableMap.<String, Material>builder()
            .put("ratings", Material.DIAMOND)
            .put("resources", Material.CHEST)
            .put("revenue", Material.GOLD_INGOT)
            .build();

    default void businessLeaderboard(Player p, String category) {
        if (!p.hasPermission("novaconomy.user.leaderboard")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (!Business.exists()) {
            p.sendMessage(getMessage("error.business.none"));
            return;
        }

        NovaInventory inv = genGUI(54, get("constants.business.leaderboard"));
        inv.setCancelled();

        for (int i = 30; i < 33; i++) inv.setItem(i, LOADING);
        for (int i = 37; i < 44; i++) inv.setItem(i, LOADING);

        p.openInventory(inv);
        new BukkitRunnable() {
            @Override
            public void run() {
                inv.setItem(13, builder(BL_ICONS.get(category),
                        meta -> {
                            meta.setDisplayName(ChatColor.GOLD + get("constants.business.leaderboard." + category));
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
                            .filter(b -> b.getRatings().size() > 0)
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
            }
        }.runTaskAsynchronously(NovaConfig.getPlugin());
    }

    default void basicConfig(CommandSender sender, String key, Object value) {
        if (!sender.hasPermission("novaconomy.admin.config")) {
            sender.sendMessage(ERROR_PERMISSION);
            return;
        }

        File configFile = NovaConfig.getConfigFile();
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        config.set(key, value);
        try { config.save(configFile); } catch (IOException e) { NovaConfig.print(e); }
        reloadFiles();

        sender.sendMessage(String.format(getMessage("success.config.set"), key, value));
    }

    default void corporationInfo(Player p) {
        if (!Corporation.existsByMember(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byMember(p);
        p.openInventory(generateCorporationData(corp, p, SortingType.BUSINESS_NAME_ASCENDING));

        if (!corp.isOwner(p)) corp.addView();
    }

    default void createCorporation(Player p, String name, Material icon) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.exists"));
            return;
        }

        if (Corporation.existsByMember(p)) {
            p.sendMessage(getError("error.corporation.exists.member"));
            return;
        }

        if (name.length() > Corporation.MAX_NAME_LENGTH) {
            p.sendMessage(String.format(getError("error.corporation.name.too_long"), ChatColor.YELLOW + "" + Corporation.MAX_NAME_LENGTH + ChatColor.RED));
            return;
        }

        try {
            Corporation.builder().setName(name).setOwner(p).setIcon(icon).build();
        } catch (UnsupportedOperationException e) {
            p.sendMessage(getError("error.corporation.exists.name"));
            return;
        }
        
        p.sendMessage(String.format(getSuccess("success.corporation.create"), name));
    }

    default void deleteCorporation(Player p, boolean confirm) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        if (confirm) {
            corp.delete();
            p.sendMessage(getSuccess("success.corporation.delete"));
        } else p.sendMessage(getError("error.corporation.confirm_delete"));
    }

    default void setCorporationDescription(Player p, String desc) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        corp.setDescription(desc);
        p.sendMessage(getSuccess("success.corporation.description"));
    }

    default void setCorporationIcon(Player p, Material icon) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        corp.setIcon(icon);
        p.sendMessage(String.format(getSuccess("success.corporation.icon"), ChatColor.GOLD + icon.name()));
    }

    default void setCorporationHeadquarters(Player p) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);

        if (corp.getLevel() < 3) {
            p.sendMessage(getError("error.corporation.too_low_level"));
            return;
        }

        Location l = p.getLocation();
        corp.setHeadquarters(l);
        p.sendMessage(String.format(getSuccess("success.corporation.headquarters"),
                ChatColor.GOLD + "" + l.getBlockX(),
                ChatColor.GOLD + "" + l.getBlockY(),
                ChatColor.GOLD + "" + l.getBlockZ())
        );
    }

    default void setCorporationName(Player p, String name) {
        if (!p.hasPermission("novaconomy.user.corporation.manage")) {
            p.sendMessage(ERROR_PERMISSION);
            return;
        }

        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        corp.setName(name);
        p.sendMessage(String.format(getSuccess("success.corporation.name"), name));
    }

    default void corporationAchievements(Player p) {
        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        p.openInventory(Generator.generateCorporationAchievements(corp));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationLeveling(Player p) {
        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        p.openInventory(Generator.generateCorporationLeveling(corp, corp.getLevel()));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void corporationStatistics(Player p) {
        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);
        p.openInventory(Generator.generateCorporationStatistics(corp));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

    default void inviteBusiness(Player p, Business b) {
        if (!Corporation.exists(p)) {
            p.sendMessage(getError("error.corporation.none"));
            return;
        }

        if (b.getParentCorporation() != null) {
            p.sendMessage(getError("error.corporation.invite.business"));
            return;
        }

        Corporation corp = Corporation.byOwner(p);

        if (!(corp.getSetting(Settings.Corporation.JOIN_TYPE) == Corporation.JoinType.INVITE_ONLY)) {
            p.sendMessage(getError("error.corporation.invite_only"));
            return;
        }

        corp.inviteBusiness(b);
        p.sendMessage(String.format(getSuccess("success.corporation.invite.business"), ChatColor.GOLD + b.getName()));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    }

}
