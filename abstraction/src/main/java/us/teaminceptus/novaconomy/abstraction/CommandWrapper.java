package us.teaminceptus.novaconomy.abstraction;

import com.cryptomorin.xseries.XSound;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.ChatPaginator;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.CommandTaxEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPayEvent;
import us.teaminceptus.novaconomy.api.player.Bounty;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface CommandWrapper {

    default void loadCommands() {}

    Wrapper w = getWrapper();

    String BUSINESS_TAG = "business";
    Map<String, List<String>> COMMANDS = new HashMap<String, List<String>>() {{
        put("ehelp", Arrays.asList("nhelp", "novahelp", "econhelp", "economyhelp"));
        put("economy", Arrays.asList("econ", "novaecon", "novaconomy", "necon"));
        put("balance", Arrays.asList("bal", "novabal", "nbal"));
        put("convert", Arrays.asList("conv"));
        put("exchange", Arrays.asList("convertgui", "convgui", "exch"));
        put("pay", Arrays.asList("givemoney", "novapay", "econpay", "givebal"));
        put("novaconomyreload", Arrays.asList("novareload", "nreload", "econreload"));
        put(BUSINESS_TAG, Arrays.asList("nbusiness"));
        put("nbank", Arrays.asList("bank", "globalbank", "gbank"));
        put("createcheck", Arrays.asList("nc", "check", "novacheck", "ncheck"));
        put("balanceleaderboard", Arrays.asList("bleaderboard", "nleaderboard", "bl", "nl", "novaleaderboard", "balboard", "novaboard"));
        put("bounty", Arrays.asList("novabounty", "nbounty"));
        put("taxevent", Arrays.asList("customtax"));
        put("settings", Arrays.asList("novasettings", "nsettings"));
        put("rate", Arrays.asList("nrate", "novarate", "ratebusiness"));
        put("statistics", Arrays.asList("stats", "pstats", "pstatistics", "playerstats", "playerstatistics", "nstats", "nstatistics"));
    }};

    Map<String, String> COMMAND_PERMISSION = new HashMap<String, String>() {{
       put("economy", "novaconomy.economy");
       put("balance", "novaconomy.user.balance");
       put("convert", "novaconomy.user.convert");
       put("exchange", "novaconomy.user.convert");
       put("pay", "novaconomy.user.pay");
       put("novaconomyreload", "novaconomy.admin.reloadconfig");
       put(BUSINESS_TAG, "novaconomy.user.business");
       put("createcheck", "novaconomy.user.check");
       put("balanceleaderboard", "novaconomy.user.leaderboard");
       put("bounty", "novaconomy.user.bounty");
       put("taxevent", "novaconomy.admin.tax_event");
       put("settings", "novaconomy.user.settings");
       put("rate", "novaconomy.user.rate");
       put("statistics", "novaconomy.user.stats");
    }};

    Map<String, String> COMMAND_DESCRIPTION = new HashMap<String, String>() {{
       put("ehelp", "Economy help");
       put("economy", "Manage economies or their balances");
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
    }};

    Map<String, String> COMMAND_USAGE = new HashMap<String, String>() {{
       put("ehelp", "/ehelp");
       put("economy", "/economy <create|delete|addbal|removebal|info> <args...>");
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
    }};

    static Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("Novaconomy");
    }

    static String get(String key) {
        String lang = NovaConfig.getConfiguration().getLanguage();
        return Language.getById(lang).getMessage(key);
    }

    static String getMessage(String key) { return get("plugin.prefix") + get(key); }

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
            p.sendMessage(getMessage("error.permission"));
            return;
        }

        p.sendMessage(get("command.loading"));
        p.openInventory(getBalancesGUI(p).get(0));
        XSound.ENTITY_ARROW_HIT_PLAYER.play(p, 3F, 2F);
    }

    default void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("novaconomy.admin.reloadconfig")) {
            sender.sendMessage(getMessage("error.permission"));
            return;
        }

        sender.sendMessage(get("command.reload.reloading"));
        Plugin plugin = getPlugin();
        plugin.reloadConfig();
        NovaConfig.loadConfig();
        NovaConfig.reloadRunnables();
        NovaConfig.loadFunctionalityFile();
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "businesses.yml"));
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "global.yml"));
        sender.sendMessage(get("command.reload.success"));
    }

    default void convert(Player p, Economy from, Economy to, double amount) {
        if (!p.hasPermission("novaconomy.user.convert")) {
            p.sendMessage(getMessage("error.permission"));
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
            p.sendMessage(getMessage("error.permission"));
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

        Inventory inv = w.genGUI(36, get("constants.economy.exchange"), new Wrapper.CancelHolder());

        List<Economy> economies = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());

        Economy economy1 = economies.get(0);
        ItemStack econ1 = new ItemStack(economy1.getIcon());
        ItemMeta e1Meta = econ1.getItemMeta();
        e1Meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + amount + "" + economy1.getSymbol()));
        econ1.setItemMeta(e1Meta);
        econ1 = w.setID(econ1, "exchange:1");
        econ1 = w.setNBT(econ1, "economy", economy1.getUniqueId().toString());
        econ1 = w.setNBT(econ1, "amount", amount);
        inv.setItem(12, econ1);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta pMeta = paper.getItemMeta();
        pMeta.setDisplayName(ChatColor.YELLOW + "->");
        paper.setItemMeta(pMeta);
        inv.setItem(13, paper);

        Economy economy2 = economies.get(1);
        ItemStack econ2 = new ItemStack(economy2.getIcon());
        ItemMeta e2Meta = econ2.getItemMeta();
        e2Meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + economy1.convertAmount(economy2, amount) + "" + economy2.getSymbol()));
        econ2.setItemMeta(e2Meta);
        econ2 = w.setID(econ2, "exchange:2");
        econ2 = w.setNBT(econ2, "economy", economy2.getUniqueId().toString());
        econ2 = w.setNBT(econ2, "amount", Math.floor(economy1.convertAmount(economy2, amount) * 100) / 100);
        inv.setItem(14, econ2);

        ItemStack yes = new ItemStack(limeWool());
        ItemMeta yMeta = yes.getItemMeta();
        yMeta.setDisplayName(ChatColor.GREEN + get("constants.yes"));
        yes.setItemMeta(yMeta);
        yes = w.setID(yes, "yes:exchange");
        inv.setItem(30, yes);

        ItemStack no = new ItemStack(redWool());
        ItemMeta nMeta = no.getItemMeta();
        nMeta.setDisplayName(ChatColor.RED + get("constants.cancel"));
        no.setItemMeta(nMeta);
        no = w.setID(no, "no:close_effect");
        inv.setItem(32, no);

        p.openInventory(inv);
    }

    static ItemStack limeWool() {
        if (w.isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 5);
        else return new ItemStack(Material.matchMaterial("LIME_WOOL"));
    }

    static ItemStack redWool() {
        if (w.isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 14);
        else return new ItemStack(Material.matchMaterial("RED_WOOL"));
    }

    default void createEconomy(CommandSender sender, String name, char symbol, Material icon, double scale, boolean naturalIncrease) {
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
            Economy.builder().setName(name).setSymbol(symbol).setIcon(icon).setIncreaseNaturally(naturalIncrease).setConversionScale(scale).build();
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
                String.format(get("constants.economy.natural_increase"), econ.hasNaturalIncrease() + ""),
                String.format(get("constants.economy.symbol"), econ.getSymbol() + ""),
                String.format(get("constants.economy.scale"), Math.floor(econ.getConversionScale() * 100) / 100 + ""),
                String.format(get("constants.economy.custom_model_data"), String.format("%,.0f", (double) econ.getCustomModelData()))
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
            p.sendMessage(getMessage("error.permission"));
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

        Inventory inv = w.genGUI(54, get("constants.balance_leaderboard"), new Wrapper.CancelHolder());

        ItemStack type = new ItemStack(Material.PAPER);
        ItemMeta tMeta = type.getItemMeta();
        if (economy) {
            type.setType(econ.getIconType());
            modelData(type, econ.getCustomModelData());
            tMeta.setDisplayName(ChatColor.AQUA + econ.getName());
        } else tMeta.setDisplayName(ChatColor.AQUA + get("constants.all_economies"));
        type.setItemMeta(tMeta);
        type = w.setID(type, "economy:wheel:leaderboard");
        type = w.setNBT(type, "economy", economy ? econ.getName() : "all");

        inv.setItem(13, type);

        for (int i = 0; i < players.size(); i++) {
            int index = i == 0 ? 22 : i + 27;
            if (index > 34) index = index + 2;
            int level = i + 1;
            ChatColor color = new ChatColor[]{ChatColor.GOLD, ChatColor.GRAY, ChatColor.YELLOW, ChatColor.AQUA}[Math.min(i, 3)];

            NovaPlayer np = players.get(i);
            ItemStack head = createPlayerHead(np.getPlayer());
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName(color + "#" + level + " - " + (np.isOnline() && np.getOnlinePlayer().getDisplayName() != null ? np.getOnlinePlayer().getDisplayName() : np.getPlayer().getName()));
            meta.setLore(Collections.singletonList(ChatColor.GOLD + String.format("%,.2f", economy ? np.getBalance(econ) : np.getTotalBalance()) + (economy ? econ.getSymbol() : "")));
            head.setItemMeta(meta);

            inv.setItem(index, head);
        }
        p.openInventory(inv);
        XSound.BLOCK_NOTE_BLOCK_PLING.play(p, 3F, 1F);
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

        p.getInventory().addItem(w.createCheck(econ, amount));
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

    default void pay(Player p, Player target, Economy econ, double amount) {
        if (!p.hasPermission("novaconomy.user.pay")) {
            p.sendMessage(getMessage("error.permission"));
            return;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage(getMessage("error.economy.pay_self"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        NovaPlayer nt = new NovaPlayer(target);

        if (np.getBalance(econ) < amount) {
            p.sendMessage(getMessage("error.economy.invalid_amount_pay"));
            return;
        }

        PlayerPayEvent e = new PlayerPayEvent(p, target, econ, amount, nt.getBalance(econ), nt.getBalance(econ) + amount);

        Bukkit.getPluginManager().callEvent(e);

        if (!e.isCancelled()) {
            np.remove(econ, amount);
            nt.add(econ, amount);

            w.sendActionbar(p, String.format(getMessage("success.economy.receive_actionbar"), Math.floor(e.getAmount() * 100) / 100, e.getPayer().getName()));
            target.sendMessage(String.format(getMessage("success.economy.receive"), econ.getSymbol() + Math.floor(e.getAmount() * 100) / 100 + "", e.getPayer().getName() + ""));
        }
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
            sender.sendMessage(getMessage("error.permission"));
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
        p.openInventory(w.generateBusinessData(b, p));
        XSound.BLOCK_ENDER_CHEST_OPEN.play(p, 3F, 0.5F);
    }

    default void businessQuery(Player p, Business b) {
        if (!p.hasPermission("novaconomy.user.business.query")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }
        p.openInventory(w.generateBusinessData(b, p));
        XSound.BLOCK_ENDER_CHEST_OPEN.play(p, 3F, 0.5F);
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

        List<String> sortedList = new ArrayList<>();
        Economy.getEconomies().forEach(econ -> sortedList.add(econ.getName()));
        sortedList.sort(String.CASE_INSENSITIVE_ORDER);
        Economy econ = Economy.getEconomy(sortedList.get(0));

        Inventory inv = w.genGUI(36, pr.hasItemMeta() && pr.getItemMeta().hasDisplayName() ? pr.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(pr.getType().name().replace('_', ' ')));

        List<String> prLore = new ArrayList<>();
        prLore.add(String.format(get("constants.business.price"), price, econ.getSymbol()));

        ItemStack economyWheel = new ItemStack(econ.getIconType());
        modelData(economyWheel, econ.getCustomModelData());
        economyWheel = w.setNBT(economyWheel, "economy", econ.getName().toLowerCase());
        economyWheel = w.setID(economyWheel, "economy:wheel:add_product");

        ItemMeta eMeta = economyWheel.getItemMeta();
        eMeta.setDisplayName(ChatColor.GOLD + econ.getName());
        economyWheel.setItemMeta(eMeta);
        inv.setItem(22, economyWheel);

        pr = w.setNBT(pr, "price", price);
        ItemMeta meta = pr.getItemMeta();
        meta.setLore(prLore);
        pr.setItemMeta(meta);
        inv.setItem(13, pr);

        ItemStack confirm = new ItemStack(Material.BEACON);
        ItemMeta cMeta = confirm.getItemMeta();
        cMeta.setDisplayName(get("constants.confirm"));
        confirm.setItemMeta(cMeta);
        confirm = w.setID(confirm, "business:add_product");
        confirm = w.setNBT(confirm, "item", product);
        confirm = w.setNBT(confirm, "price", price);
        confirm = w.setNBT(confirm, "economy", econ.getName().toLowerCase());
        inv.setItem(23, confirm);

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

        Inventory inv = Bukkit.createInventory(new ReturnItemsHolder(p, "business:add_resource"), 54, get("constants.business.add_stock"));

        ItemStack confirm = new ItemStack(Material.BEACON);
        confirm = w.setID(confirm, "business:add_resource");

        ItemMeta cMeta = confirm.getItemMeta();
        cMeta.setDisplayName(get("constants.confirm"));
        cMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        cMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        List<String> lore = new ArrayList<>();
        for (int i = 1; i < 4; i++) lore.add(get("constants.business.add_resource." + i));

        cMeta.setLore(Arrays.asList(ChatPaginator.wordWrap(String.join("\n\n", lore), 30)));
        confirm.setItemMeta(cMeta);
        inv.setItem(49, confirm);

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


        Inventory inv = w.genGUI(54, get("constants.business.remove_product"), new Wrapper.CancelHolder());
        Inventory bData = w.generateBusinessData(b, p);

        List<ItemStack> items = Arrays.stream(bData.getContents())
                .filter(Objects::nonNull)
                .filter(w::hasID)
                .filter(i -> w.getNBTBoolean(i, "is_product"))
                .collect(Collectors.toList());

        items.replaceAll(item -> w.setID(item, "product:remove"));
        items.forEach(inv::addItem);

        p.openInventory(inv);
    }

    default void bankBalances(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        p.openInventory(getBankBalanceGUI().get(0));
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
            XSound.ENTITY_ENDERMAN_TELEPORT.play(p, 3F, 1F);
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

        if (target.getUniqueId().equals(p.getUniqueId())) {
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

        if (target.getUniqueId().equals(p.getUniqueId())) {
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
        try { config.save(f); } catch (IOException e) { Bukkit.getLogger().severe(e.getMessage()); }
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

        Inventory inv = w.genGUI(36, owned ? get("constants.bounty.all") : get("constants.bounty.self"), new Wrapper.CancelHolder());
        for (int i = 10; i < 12; i++) inv.setItem(i, w.getGUIBackground());
        for (int i = 15; i < 17; i++) inv.setItem(i, w.getGUIBackground());


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
        XSound.BLOCK_NOTE_BLOCK_PLING.play(p, 3F, 1F);
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(uid);
                    NovaPlayer np = new NovaPlayer(p);
                    prices.forEach(np::remove);
                    if (deposit) prices.forEach(Bank::addBalance);

                    if (p.isOnline())
                        p.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', custom.getMessage()));
                }
            }.runTask(NovaConfig.getPlugin());

        sender.sendMessage(String.format(getMessage("success.tax.custom_event"), custom.getName()));
    }

    default void settings(Player p, String section) {
        final Inventory settings;
        NovaPlayer np = new NovaPlayer(p);
        if (section == null) {
            settings = w.genGUI(27, get("constants.settings.select"), new Wrapper.CancelHolder());

            ItemStack personal = createPlayerHead(p);
            ItemMeta meta = personal.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + get("constants.settings.player"));
            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            personal.setItemMeta(meta);
            personal = w.setID(personal, "setting");
            personal = w.setNBT(personal, "setting", "personal");

            ItemStack business = new ItemStack(Material.BOOK);
            ItemMeta meta2 = business.getItemMeta();
            meta2.setDisplayName(ChatColor.YELLOW + get("constants.settings.business"));
            meta2.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
            meta2.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            business.setItemMeta(meta2);
            business = w.setID(business, "setting");
            business = w.setNBT(business, "setting", BUSINESS_TAG);

            settings.addItem(personal, business);
        } else {
            if (!section.equalsIgnoreCase("personal") && !section.equalsIgnoreCase(BUSINESS_TAG)) {
                p.sendMessage(getMessage("error.settings.section_inexistent"));
                return;
            }

            boolean business = section.equalsIgnoreCase(BUSINESS_TAG);

            if (business && !Business.exists(p)) {
                p.sendMessage(getMessage("error.business.not_an_owner"));
                return;
            }

            settings = w.genGUI(36, get("constants.settings." + (business ? BUSINESS_TAG : "player")), new Wrapper.CancelHolder());

            BiConsumer<Settings.NovaSetting<Boolean>, Boolean> func = (sett, value) -> {
                ItemStack item = new ItemStack(value ? limeWool() : redWool());
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + sett.getDisplayName() + ": " + (value ? ChatColor.GREEN + get("constants.on") : ChatColor.RED + get("constants.off")));
                if (value) {
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);

                item = w.setID(item, "setting_toggle");
                item = w.setNBT(item, "display", sett.getDisplayName());
                item = w.setNBT(item, "section", section);
                item = w.setNBT(item, "setting", sett.name());
                item = w.setNBT(item, "value", value);

                settings.addItem(item);
            };

            if (business) {
                Business b = Business.getByOwner(p);
                for (Settings.Business sett : Settings.Business.values()) {
                    boolean value = b.getSetting(sett);
                    func.accept(sett, value);
                }
            } else for (Settings.Personal sett : Settings.Personal.values()) {
                boolean value = np.getSetting(sett);
                func.accept(sett, value);
            }

            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta meta = back.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + get("constants.back"));
            back.setItemMeta(meta);

            back = w.setID(back, "back:settings");

            settings.setItem(31, back);
        }

        p.openInventory(settings);
        XSound.BLOCK_ANVIL_USE.play(p, 3F, 1.5F);
    }

    default void businessStatistics(Player p, Business b) {
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Inventory stats = w.genGUI(36, get("constants.business.statistics"), new Wrapper.CancelHolder());
        BusinessStatistics statistics = b.getStatistics();

        boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(p);
        ItemStack owner = w.createSkull(anonymous ? null : b.getOwner());
        ItemMeta oMeta = owner.getItemMeta();
        oMeta.setDisplayName(anonymous ? ChatColor.AQUA + get("constants.business.anonymous") : String.format(get("constants.business.owner"), b.getOwner().getName()));
        if (b.isOwner(p) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
            oMeta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
        owner.setItemMeta(oMeta);
        stats.setItem(12, owner);

        ItemStack created = new ItemStack(Material.EGG);
        ItemMeta cMeta = created.getItemMeta();
        cMeta.setDisplayName(ChatColor.YELLOW + String.format(get("constants.business.stats.created"), formatTimeAgo(b.getCreationDate().getTime())));
        created.setItemMeta(cMeta);
        stats.setItem(14, created);

        ItemStack sold = new ItemStack(Material.EMERALD);
        ItemMeta sMeta = sold.getItemMeta();
        sMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE + get("constants.business.stats.global"));
        sMeta.setLore(Arrays.asList(
                "",
                String.format(get("constants.business.stats.global.sold"), String.format("%,.0f", (double) statistics.getTotalSales())),
                String.format(get("constants.business.stats.global.resources"), String.format("%,.0f", (double) statistics.getTotalResources())),
                String.format(get("constants.business.stats.global.ratings"), String.format("%,.0f", (double) b.getRatings().size()))
        ));
        sold.setItemMeta(sMeta);
        stats.setItem(20, sold);

        final ItemStack latest;
        if (statistics.hasLatestTransaction()) {
            BusinessStatistics.Transaction latestT = statistics.getLastTransaction();
            Product pr = latestT.getProduct();
            ItemStack prI = pr.getItem();
            OfflinePlayer buyer = latestT.getBuyer();

            latest = createPlayerHead(buyer);
            ItemMeta lMeta = latest.getItemMeta();
            lMeta.setDisplayName(ChatColor.YELLOW + get("constants.business.stats.global.latest"));
            String display = prI.hasItemMeta() && prI.getItemMeta().hasDisplayName() ? prI.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(prI.getType().name().replace('_', ' '));
            lMeta.setLore(Arrays.asList(
                    ChatColor.AQUA + "" + ChatColor.UNDERLINE + (buyer.isOnline() && buyer.getPlayer().getDisplayName() != null ? buyer.getPlayer().getDisplayName() : buyer.getName()),
                    " ",
                    ChatColor.WHITE + display + " (" + prI.getAmount() + ")" + ChatColor.GOLD + " | " + ChatColor.BLUE + String.format("%,.2f", pr.getAmount() * prI.getAmount()) + pr.getEconomy().getSymbol(),
                    ChatColor.DARK_AQUA + formatTimeAgo(latestT.getTimestamp().getTime())
            ));
            latest.setItemMeta(lMeta);
        } else {
            latest = new ItemStack(Material.PAPER);
            ItemMeta lMeta = latest.getItemMeta();
            lMeta.setDisplayName(ChatColor.RESET + get("constants.business.no_transactions"));
            latest.setItemMeta(lMeta);
        }
        stats.setItem(21, latest);

        Map<Product, Integer> productSales = statistics.getProductSales();

        ItemStack totalMoney = new ItemStack(Material.GOLD_INGOT);
        ItemMeta mMeta = totalMoney.getItemMeta();
        mMeta.setDisplayName(ChatColor.YELLOW + get("constants.business.stats.global.total_made"));
        List<String> lore = new ArrayList<>();

        List<Economy> econs = Economy.getEconomies().stream().sorted(Collections.reverseOrder(Comparator.comparing(Economy::getName))).collect(Collectors.toList());
        Map<Economy, Double> totals = new HashMap<>();

        AtomicInteger i = new AtomicInteger();
        for (Economy econ : econs) {
            if (i.get() > 5) {
                i.set(-1);
                break;
            }

            double total = 0;
            for (Product pr : productSales.keySet().stream().filter(pr -> pr.getEconomy().equals(econ)).collect(Collectors.toSet()))
                total += pr.getAmount() * productSales.get(pr);

            if (total == 0) continue;
            totals.put(econ, total);
            i.incrementAndGet();
        }

        List<Map.Entry<Economy, Double>> sortedTotals = totals.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());

        boolean switcher = false;
        for (Map.Entry<Economy, Double> entry : sortedTotals) {
            Economy econ = entry.getKey();
            double total = entry.getValue();

            lore.add((switcher ? ChatColor.AQUA : ChatColor.BLUE) + String.format("%,.2f", total) + econ.getSymbol());
            switcher = !switcher;
        }

        if (i.get() == -1) lore.add(ChatColor.WHITE + "...");
        mMeta.setLore(lore);

        totalMoney.setItemMeta(mMeta);
        stats.setItem(23, totalMoney);

        final ItemStack top;

        if (productSales.size() > 0) {
            List<Map.Entry<Product, Integer>> topProd = productSales
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toList());

            top = new ItemStack(Material.DIAMOND);
            ItemMeta tMeta = top.getItemMeta();
            tMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.UNDERLINE + get("constants.business.stats.global.top"));

            List<String> pLore = new ArrayList<>();
            pLore.add(" ");

            for (int j = 0; j < Math.min(5, topProd.size()); j++) {
                Map.Entry<Product, Integer> entry = topProd.get(j);
                Product pr = entry.getKey();
                int sales = entry.getValue();
                int num = j + 1;

                ItemStack item = pr.getItem();
                String display = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(item.getType().name().replace('_', ' '));

                pLore.add(ChatColor.YELLOW + "#" + num + ") " + ChatColor.RESET + display + ChatColor.GOLD + " - " + ChatColor.BLUE + String.format("%,.2f", pr.getAmount()) + pr.getEconomy().getSymbol() + ChatColor.GOLD + " | " + ChatColor.AQUA + String.format("%,.0f", (double) sales));
            }

            tMeta.setLore(pLore);
            top.setItemMeta(tMeta);
        } else {
            top = new ItemStack(Material.PAPER);
            ItemMeta tMeta = top.getItemMeta();
            tMeta.setDisplayName(ChatColor.RESET + get("constants.business.no_products"));
            top.setItemMeta(tMeta);
        }
        stats.setItem(24, top);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + get("constants.back"));
        back.setItemMeta(bMeta);
        back = w.setID(back, "back:business");
        back = w.setNBT(back, "business", b.getUniqueId().toString());
        stats.setItem(31, back);

        p.openInventory(stats);
    }

    Material[] RATING_MATS = new Material[] {
            Material.DIRT,
            Material.COAL,
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.DIAMOND
    };

    default void rate(Player p, Business b, String comment) {
        if (!p.hasPermission("novaconomy.user.rate")) {
            p.sendMessage(getMessage("error.permission"));
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

        Inventory rate = w.genGUI(36, String.format(get("constants.rating"), b.getName()), new Wrapper.CancelHolder());

        ItemStack ratingWheel = new ItemStack(RATING_MATS[2]);
        ItemMeta rMeta = ratingWheel.getItemMeta();
        rMeta.setDisplayName(ChatColor.YELLOW + "3⭐");
        ratingWheel.setItemMeta(rMeta);
        ratingWheel = w.setID(ratingWheel, "business:rating");
        ratingWheel = w.setNBT(ratingWheel, "rating", 2);
        rate.setItem(13, ratingWheel);

        ItemStack sComment = new ItemStack(Material.SIGN);
        ItemMeta sMeta = sComment.getItemMeta();
        sMeta.setDisplayName(ChatColor.YELLOW + "\"" + (comment.isEmpty() ? get("constants.no_comment") : comment) + "\"");
        sComment.setItemMeta(sMeta);
        rate.setItem(14, sComment);

        ItemStack confirm = new ItemStack(limeWool());
        ItemMeta cMeta = confirm.getItemMeta();
        cMeta.setDisplayName(get("constants.confirm"));
        confirm.setItemMeta(cMeta);
        confirm = w.setID(confirm, "yes:business_rate");
        confirm = w.setNBT(confirm, "rating", 2);
        confirm = w.setNBT(confirm, "business", b.getUniqueId().toString());
        confirm = w.setNBT(confirm, "comment", comment);
        rate.setItem(21, confirm);

        ItemStack cancel = new ItemStack(redWool());
        ItemMeta caMeta = cancel.getItemMeta();
        caMeta.setDisplayName(ChatColor.RED + get("constants.cancel"));
        cancel.setItemMeta(caMeta);
        cancel = w.setID(cancel, "no:close_effect");
        rate.setItem(23, cancel);

        p.openInventory(rate);
    }

    default void businessRating(Player p, OfflinePlayer target) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        if (target.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage(getMessage("error.business.rate_self"));
            return;
        }

        Business b = Business.getByOwner(p);

        Optional<Rating> r = b.getRatings().stream().filter(ra -> ra.isOwner(target)).findFirst();
        if (!r.isPresent()) {
            p.sendMessage(getMessage("error.business.no_rating"));
            return;
        }

        Rating rating = r.get();
        Inventory pr = w.genGUI(27, target.getName() + " - \"" + b.getName() + "\"", new Wrapper.CancelHolder());

        ItemStack head = createPlayerHead(target);
        ItemMeta hMeta = head.getItemMeta();
        hMeta.setDisplayName(ChatColor.YELLOW + target.getName());
        hMeta.setLore(Collections.singletonList(ChatColor.AQUA + formatTimeAgo(rating.getTimestamp().getTime())));
        head.setItemMeta(hMeta);
        pr.setItem(12, head);

        ItemStack pRating = new ItemStack(RATING_MATS[rating.getRatingLevel() - 1]);
        ItemMeta rMeta = pRating.getItemMeta();
        rMeta.setDisplayName(ChatColor.YELLOW + "" + rating.getRatingLevel() + "⭐");
        rMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "\"" + (rating.getComment().isEmpty() ? get("constants.no_comment") : rating.getComment()) + "\""));
        pRating.setItemMeta(rMeta);
        pr.setItem(14, pRating);

        p.openInventory(pr);
    }

    default void discoverBusinesses(Player p) {
        if (!p.hasPermission("novaconomy.user.business.discover")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        Inventory discover = w.genGUI(54, get("constants.business.discover"), new Wrapper.CancelHolder());

        for (int i = 0; i < 28; i++) {
            int index = 10 + i;
            if (index > 16) index += 2;
            if (index > 25) index += 2;
            if (index > 34) index += 2;
            discover.setItem(index, loading());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                List<Business> businesses = new ArrayList<>();
                for (Business b : Business.getBusinesses()) {
                    if (b.isOwner(p)) continue;
                    if (!b.getSetting(Settings.Business.PUBLIC_DISCOVERY)) continue;
                    businesses.add(b);
                }

                Collections.shuffle(businesses);
                for (int i = 0; i < 28; i++) {
                    int index = 10 + i;
                    if (index > 16) index += 2;
                    if (index > 25) index += 2;
                    if (index > 34) index += 2;
                    if (index > 43) return;

                    if (businesses.size() > i) {
                        Business b = businesses.get(i);

                        boolean pOwner = b.getSetting(Settings.Business.PUBLIC_OWNER);
                        boolean pRating = b.getSetting(Settings.Business.PUBLIC_RATING);

                        ItemStack icon = new ItemStack(b.getIcon().getType());
                        ItemMeta hMeta = icon.getItemMeta();
                        hMeta.setDisplayName(ChatColor.GOLD + b.getName());
                        StringBuilder rB = new StringBuilder();
                        for (int j = 0; j < Math.round(b.getAverageRating()); j++) rB.append("⭐");

                        hMeta.setLore(Arrays.asList(
                                pOwner ? String.format(get("constants.business.owner"), b.getOwner().getName()) : get("constants.business.anonymous"),
                                pRating ? " " : "",
                                pRating ? ChatColor.YELLOW + rB.toString() : "")
                        );
                        icon.setItemMeta(hMeta);

                        icon = w.setID(icon, "business:discover");
                        icon = w.setNBT(icon, "business", b.getUniqueId().toString());

                        discover.setItem(index, icon);
                    } else discover.setItem(index, w.getGUIBackground());
                }

                XSound.ENTITY_ARROW_HIT_PLAYER.play(p, 3F, 2F);
            }
        }.runTaskAsynchronously(NovaConfig.getPlugin());
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
        Inventory select = w.genGUI(54, get("constants.business.select_product"), new Wrapper.CancelHolder());
        Inventory bData = w.generateBusinessData(b, p);

        List<ItemStack> items = Arrays.stream(bData.getContents())
                .filter(Objects::nonNull)
                .filter(w::hasID)
                .filter(i -> w.getNBTBoolean(i, "is_product"))
                .collect(Collectors.toList());

        items.replaceAll(item -> w.setID(item, "product:edit_price"));
        items.replaceAll(item -> w.setNBT(item, "price", newPrice));
        items.replaceAll(item -> {
            Economy economy = econ == null ? w.getNBTProduct(item, "product").getEconomy() : econ;
            return w.setNBT(item, "economy", economy.getUniqueId().toString());
        });
        items.forEach(select::addItem);

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
            op.sendMessage(getMessage("error.permission"));
            return;
        }

        NovaPlayer np = new NovaPlayer(target);
        PlayerStatistics stats = np.getStatistics();

        Inventory inv = w.genGUI(36, get("constants.player_statistics"), new Wrapper.CancelHolder());

        ItemStack head = createPlayerHead(target);
        ItemMeta hMeta = head.getItemMeta();
        hMeta.setDisplayName(ChatColor.LIGHT_PURPLE + get("constants.player_statistics"));
        hMeta.setLore(Collections.singletonList(ChatColor.YELLOW + (online && op.getDisplayName() == null ? target.getName() : op.getDisplayName())));
        head.setItemMeta(hMeta);
        inv.setItem(4, head);

        ItemStack maxBal = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta mbMeta = maxBal.getItemMeta();
        mbMeta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.highest_balance"));

        String s = stats.getHighestBalance() == null ? String.format("%,.2f", np.getTotalBalance()) : stats.getHighestBalance().toString();
        mbMeta.setLore(Collections.singletonList(ChatColor.GOLD + s));
        maxBal.setItemMeta(mbMeta);
        inv.setItem(10, maxBal);

        ItemStack purchased = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta pMeta = purchased.getItemMeta();
        pMeta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.products_purchased"));
        pMeta.setLore(Collections.singletonList(ChatColor.GOLD + String.format("%,.0f", (double) stats.getProductsPurchased())));
        purchased.setItemMeta(pMeta);
        inv.setItem(12, purchased);

        ItemStack bank = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bMeta = bank.getItemMeta();
        bMeta.setDisplayName(ChatColor.YELLOW + get("constants.player_statistics.bank"));
        bMeta.setLore(Arrays.asList(
                String.format(get("constants.player_statistics.bank.total_withdrawn"), String.format("%,.2f", stats.getTotalWithdrawn()))
        ));
        bank.setItemMeta(bMeta);
        inv.setItem(14, bank);

        op.openInventory(inv);
        XSound.BLOCK_ANVIL_USE.play(p, 3F, 1.5F);
    }

    default void businessRecover(Player p) {
        if (!Business.exists(p)) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b.getLeftoverStock().size() == 0) {
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
        while(it.hasNext()) {
            ItemStack item = it.next();
            if (p.getInventory().firstEmpty() == -1) {
                overflow = true;
                break;
            }
            items.add(item);
            it.remove();
        }

        if (b.getLeftoverStock().size() > 0) overflow = true;
        b.removeResource(items);
        p.getInventory().addItem(items.toArray(new ItemStack[0]));

        p.sendMessage(getMessage("success.business.recover"));
        if (overflow) p.sendMessage(get("constants.business.stock_overflow"));
    }

    // Util Classes & Other Static Methods

    class ReturnItemsHolder implements InventoryHolder {

        private final Player p;
        private final List<String> ignoreIds;

        private boolean added;

        public ReturnItemsHolder(Player p, String... ignoreIds) {
            this.p = p;
            this.ignoreIds = Arrays.asList(ignoreIds);
            this.added = false;
        }


        @Override
        public Inventory getInventory() {
            return null;
        }

        public Player player() {
            return this.p;
        }

        public boolean added() {
            return this.added;
        }

        public void added(boolean added) { this.added = added; }

        public List<String> ignoreIds() { return this.ignoreIds; }
    }

    static String formatTimeAgo(long start) {
        long time = System.currentTimeMillis();
        long diff = time - start;

        double seconds = (double) diff / 1000D;

        if (seconds < 2) return get("constants.time.ago.milli_ago");
        if (seconds >= 2 && seconds < 60) return String.format(get("constants.time.ago.seconds_ago"), String.format("%,.0f", seconds));

        double minutes = seconds / 60D;
        if (minutes < 60) return get("constants.time.ago.minutes_ago");

        double hours = minutes / 60D;
        if (hours < 24) return String.format(get("constants.time.ago.hours_ago"), String.format("%,.0f", hours));

        double days = hours / 24D;
        if (days < 7) return String.format(get("constants.time.ago.days_ago"), String.format("%,.0f", days));

        double weeks = days / 7D;
        if (weeks < 4) return String.format(get("constants.time.ago.weeks_ago"), String.format("%,.0f", weeks));

        double months = weeks / 4D;
        if (months < 12) return String.format(get("constants.time.ago.months_ago"), String.format("%,.0f", months));

        double years = months / 12D;
        return String.format(get("constants.time.ago.years_ago"), String.format("%,.0f", years));
    }

    static String getServerVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
    }

    static Wrapper getWrapper() {
        try {
            return (Wrapper) Class.forName("us.teaminceptus.novaconomy.Wrapper" + getServerVersion()).getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Wrapper not Found: " + getServerVersion());
        }
    }

    static ItemStack createPlayerHead(OfflinePlayer p) {
        ItemStack head = new ItemStack(w.isLegacy() ? Material.matchMaterial("SKULL_ITEM") : Material.matchMaterial("PLAYER_HEAD"));
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(p.getName());
        head.setItemMeta(meta);
        head = w.setID(head, "player_stats");
        head = w.setNBT(head, "player", p.getUniqueId().toString());
        return head;
    }

    static ItemStack loading() {
        ItemStack load = new ItemStack(createPlayerHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmRkMmViMGM2ZjhhOTU0M2VmNWZkNzI1MjVjYzJmYWIzNTY2M2NkNzA5MTM1ZTQzYjhlMjU3ZGMwYjc1ODk0OCJ9fX0="));
        ItemMeta meta = load.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED + get("constants.loading"));
        load.setItemMeta(meta);
        return load;
    }

    static ItemStack createPlayerHead(String texture) {
        ItemStack head = new ItemStack(w.isLegacy() ? Material.matchMaterial("SKULL_ITEM") : Material.matchMaterial("PLAYER_HEAD"));
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", texture));
        try {
            Method mtd = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
            mtd.setAccessible(true);
            mtd.invoke(meta, profile);
        } catch (Exception e) {
            Bukkit.getLogger().severe(e.getClass().getSimpleName());
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) Bukkit.getLogger().severe(el.toString());
        }
        head.setItemMeta(meta);
        return head;
    }

    static void modelData(ItemStack item, int data) {
        ItemMeta meta = item.getItemMeta();
        try {
            Method m = meta.getClass().getDeclaredMethod("setCustomModelData", Integer.class);
            m.setAccessible(true);
            m.invoke(meta, data);
        } catch (NoSuchMethodException ignored) {}
        catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }
        item.setItemMeta(meta);
    }
    static List<Inventory> getBalancesGUI(OfflinePlayer p) {
        List<Inventory> invs = new ArrayList<>();
        NovaPlayer np = new NovaPlayer(p);

        ItemStack nextA = new ItemStack(Material.ARROW);
        ItemMeta nMeta = nextA.getItemMeta();
        nMeta.setDisplayName(ChatColor.AQUA + get("constants.next"));
        nextA.setItemMeta(nMeta);
        nextA = w.setID(nextA, "next:balance");

        ItemStack backA = new ItemStack(Material.ARROW);
        ItemMeta bMeta = backA.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + get("constants.prev"));
        backA.setItemMeta(bMeta);
        backA = w.setID(backA, "prev:balance");

        List<Economy> econs = new ArrayList<>(Economy.getEconomies()).stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());
        int pageCount = (int) Math.floor((econs.size() - 1D) / 28D) + 1;
        for (int i = 0; i < pageCount; i++) {
            Inventory inv = w.genGUI(54, get("constants.balances") + " - " + String.format(get("constants.page"), i + 1), new Wrapper.CancelHolder());

            ItemStack head = createPlayerHead(p);
            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + p.getName());
            head.setItemMeta(meta);
            inv.setItem(4, head);

            if (pageCount > 1 && i < pageCount - 1) {
                ItemStack next = nextA.clone();
                next = w.setNBT(next, "page", i);
                inv.setItem(48, next);
            }

            if (i > 0) {
                ItemStack back = backA.clone();
                back = w.setNBT(back, "page", i);
                inv.setItem(50, back);
            }

            List<Economy> elist = new ArrayList<>(econs.subList(i * 28, Math.min((i + 1) * 28, econs.size())));
            elist.forEach(econ -> {
                ItemStack item = econ.getIcon().clone();
                ItemMeta eMeta = item.getItemMeta();
                eMeta.setLore(Collections.singletonList(
                        ChatColor.GOLD + String.format("%,.2f", np.getBalance(econ)) + econ.getSymbol()
                ));
                item.setItemMeta(eMeta);
                inv.addItem(item);
            });

            invs.add(inv);
        }

        return invs;
    }


    static List<Inventory> getBankBalanceGUI() {
        List<Inventory> invs = new ArrayList<>();

        ItemStack nextA = new ItemStack(Material.ARROW);
        ItemMeta nMeta = nextA.getItemMeta();
        nMeta.setDisplayName(ChatColor.AQUA + get("constants.next"));
        nextA.setItemMeta(nMeta);
        nextA = w.setID(nextA, "next:bank_balance");

        ItemStack backA = new ItemStack(Material.ARROW);
        ItemMeta bMeta = backA.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + get("constants.prev"));
        backA.setItemMeta(bMeta);
        backA = w.setID(backA, "prev:bank_balance");

        List<Economy> econs = new ArrayList<>(Economy.getEconomies()).stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());
        int pageCount = (int) Math.floor((econs.size() - 1D) / 28D) + 1;
        for (int i = 0; i < pageCount; i++) {
            Inventory inv = w.genGUI(54, get("constants.bank.balance") + " - " + String.format(get("constants.page"), i + 1), new Wrapper.CancelHolder());

            if (pageCount > 1 && i < pageCount - 1) {
                ItemStack next = nextA.clone();
                next = w.setNBT(next, "page", i);
                inv.setItem(48, next);
            }

            if (i > 0) {
                ItemStack back = backA.clone();
                back = w.setNBT(back, "page", i);
                inv.setItem(50, back);
            }

            List<Economy> elist = new ArrayList<>(econs.subList(i * 28, Math.min((i + 1) * 28, econs.size())));
            elist.forEach(econ -> {
                ItemStack item = new ItemStack(econ.getIconType());
                modelData(item, econ.getCustomModelData());

                ItemMeta iMeta = item.getItemMeta();
                iMeta.setDisplayName(ChatColor.AQUA + String.format("%,.2f", Bank.getBalance(econ)) + "" + econ.getSymbol());
                List<String> topDonors = new ArrayList<>();
                topDonors.add(ChatColor.YELLOW + get("constants.bank.top_donors"));
                topDonors.add(" ");
                List<String> topDonorsNames = NovaPlayer.getTopDonators(econ, 5).stream().map(NovaPlayer::getPlayerName).collect(Collectors.toList());
                List<Double> topDonorsAmounts = NovaPlayer.getTopDonators(econ, 5).stream().map(n -> n.getDonatedAmount(econ)).collect(Collectors.toList());
                for (int j = 0; j < topDonorsNames.size(); j++)
                    if (j < 2)
                        topDonors.add(new ChatColor[]{ChatColor.GOLD, ChatColor.GRAY}[j] + "#" + (j + 1) + " - " + topDonorsNames.get(j) + " | " + String.format("%,.2f", topDonorsAmounts.get(j) ) + econ.getSymbol());
                    else if (j == 3)
                        topDonors.add(ChatColor.translateAlternateColorCodes('&', "&x&c&6&3#3 - " + topDonorsNames.get(j) + " | " + String.format("%,.2f", topDonorsAmounts.get(j) ) + econ.getSymbol() ));
                    else topDonors.add(ChatColor.BLUE + "#" + (j + 1) + " - " + topDonorsNames.get(j) + " | " + String.format("%,.2f", topDonorsAmounts.get(j) ) + econ.getSymbol());
                iMeta.setLore(topDonors);
                item.setItemMeta(iMeta);
                inv.addItem(item);
            });

            invs.add(inv);
        }

        return invs;
    }
}
