package us.teaminceptus.novaconomy.abstraction;

import com.cryptomorin.xseries.XSound;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
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
import org.bukkit.util.ChatPaginator;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.player.PlayerPayEvent;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface CommandWrapper {

    default void loadCommands() {}

    Wrapper wr = getWrapper();

    Map<String, List<String>> COMMANDS = new HashMap<String, List<String>>() {{
        put("ehelp", Arrays.asList("nhelp", "novahelp", "econhelp", "economyhelp"));
        put("economy", Arrays.asList("econ", "novaecon", "novaconomy", "necon"));
        put("balance", Arrays.asList("bal", "novabal", "nbal"));
        put("convert", Arrays.asList("conv"));
        put("exchange", Arrays.asList("convertgui", "convgui", "exch"));
        put("pay", Arrays.asList("givemoney", "novapay", "econpay", "givebal"));
        put("novaconomyreload", Arrays.asList("novareload", "nreload", "econreload"));
        put("business", Arrays.asList("nbusiness"));
        put("overridelanguages", Arrays.asList("overl", "overridemessages"));
        put("nbank", Arrays.asList("bank", "globalbank", "gbank"));
        put("createcheck", Arrays.asList("nc", "check", "novacheck", "ncheck"));
        put("balanceleaderboard", Arrays.asList("bleaderboard", "nleaderboard", "bl", "nl", "novaleaderboard", "balboard", "novaboard"));
    }};

    Map<String, String> COMMAND_PERMISSION = new HashMap<String, String>() {{
       put("economy", "novaconomy.economy");
       put("balance", "novaconomy.user.balance");
       put("convert", "novaconomy.user.convert");
       put("exchange", "novaconomy.user.convert");
       put("pay", "novaconomy.user.pay");
       put("novaconomyreload", "novaconomy.admin.reloadconfig");
       put("business", "novaconomy.user.business");
       put("overridelanguages", "novaconomy.admin.reloadconfig");
       put("createcheck", "novaconomy.user.check");
       put("balanceleaderboard", "novaconomy.user.leaderboard");
    }};

    Map<String, String> COMMAND_DESCRIPTION = new HashMap<String, String>() {{
       put("ehelp", "Economy help");
       put("economy", "Manage economies or their balances");
       put("balance", "Access your balances from all economies");
       put("convert", "Convert one balance in an economy to another balance");
       put("exchange", "Convert one balance in an economy to another balance (with a GUI)");
       put("pay", "Pay another user");
       put("novaconomyreload", "Reload Novaconomy Configuration");
       put("business", "Manage your Novaconomy Business");
       put("overridelanguages", "Load Default /Messages from Plugin JAR");
       put("nbank", "Interact with the Global Novaconomy Bank");
       put("createcheck", "Create a Novaconomy Check redeemable for a certain amount of money");
       put("balanceleaderboard", "View the top 15 balances in all or certain economies");
    }};

    Map<String, String> COMMAND_USAGE = new HashMap<String, String>() {{
       put("ehelp", "/ehelp");
       put("economy", "/economy <create|delete|addbal|removebal|info> <args...>");
       put("balance", "/balance");
       put("convert", "/convert <econ-from> <econ-to> <amount>");
       put("exchange", "/exchange <amount>");
       put("pay", "/pay <player> <economy> <amount>");
       put("novaconomyreload", "/novareload");
       put("business", "/business <create|delete|edit|stock> <args...>");
       put("overridelanguages", "/overridelanguages");
       put("createcheck", "/createcheck <economy> <amount>");
       put("balanceleaderboard", "/balanceleaderboard [<economy>]");
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
        Plugin plugin = getPlugin();
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

        NovaPlayer np = new NovaPlayer(p);
        List<String> balanceInfo = new ArrayList<>();

        for (Economy econ : Economy.getEconomies())
            balanceInfo.add(ChatColor.GOLD + econ.getName() + ChatColor.AQUA + " - " + ChatColor.GREEN + String.format("%,.2f", Math.floor(np.getBalance(econ) * 100) / 100) + econ.getSymbol());

        p.sendMessage(get("command.balance.balances") + "\n\n" + String.join("\n", balanceInfo.toArray(new String[0])));
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
        NovaConfig.reloadLanguages();
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
        p.sendMessage(String.format(getMessage("success.economy.convert"), amount + "" + from.getSymbol(), Math.floor(toBal * 100) / 100) + "" + to.getSymbol());
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

        Inventory inv = wr.genGUI(36, get("constants.economy.exchange"), new Wrapper.CancelHolder());

        List<Economy> economies = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());

        Economy economy1 = economies.get(0);
        ItemStack econ1 = new ItemStack(economy1.getIcon());
        ItemMeta e1Meta = econ1.getItemMeta();
        e1Meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + amount + "" + economy1.getSymbol()));
        econ1.setItemMeta(e1Meta);
        econ1 = wr.setID(econ1, "exchange:1");
        econ1 = wr.setNBT(econ1, "economy", economy1.getUniqueId().toString());
        econ1 = wr.setNBT(econ1, "amount", amount);
        inv.setItem(12, econ1);

        Economy economy2 = economies.get(1);
        ItemStack econ2 = new ItemStack(economy2.getIcon());
        ItemMeta e2Meta = econ2.getItemMeta();
        e2Meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + economy1.convertAmount(economy2, amount) + "" + economy2.getSymbol()));
        econ2.setItemMeta(e2Meta);
        econ2 = wr.setID(econ2, "exchange:2");
        econ2 = wr.setNBT(econ2, "economy", economy2.getUniqueId().toString());
        econ2 = wr.setNBT(econ2, "amount", Math.floor(economy1.convertAmount(economy2, amount) * 100) / 100);
        inv.setItem(14, econ2);

        ItemStack yes = new ItemStack(limeWool());
        ItemMeta yMeta = yes.getItemMeta();
        yMeta.setDisplayName(ChatColor.GREEN + get("constants.yes"));
        yes.setItemMeta(yMeta);
        yes = wr.setID(yes, "yes:exchange");
        inv.setItem(30, yes);

        ItemStack no = new ItemStack(redWool());
        ItemMeta nMeta = no.getItemMeta();
        nMeta.setDisplayName(ChatColor.RED + get("constants.cancel"));
        no.setItemMeta(nMeta);
        no = wr.setID(no, "no:exchange");
        inv.setItem(32, no);

        p.openInventory(inv);
    }

    static ItemStack limeWool() {
        if (wr.isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 5);
        else return new ItemStack(Material.matchMaterial("LIME_WOOL"));
    }

    static ItemStack redWool() {
        if (wr.isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 14);
        else return new ItemStack(Material.matchMaterial("RED_WOOL"));
    }

    default void createEconomy(CommandSender sender, String name, char symbol, Material icon, double scale, boolean naturalIncrease) {
        if (!sender.hasPermission("novaconomy.economy.create")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        for (Economy econ : Economy.getEconomies())
            if (econ.getName().equals(name)) {
                sender.sendMessage(getMessage("error.economy.exists"));
                return;
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
        };
        sender.sendMessage(String.join("\n", components));
    }

    default void addBalance(CommandSender sender, Economy econ, Player target, double add) {
        if (!sender.hasPermission("novaconomy.economy.addbalance")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (add <= 0) {
            sender.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        nt.add(econ, add);
        sender.sendMessage(String.format(getMessage("success.economy.addbalance"),  econ.getSymbol() + "", add, target.getName()));
    }

    default void removeBalance(CommandSender sender, Economy econ, Player target, double remove) {
        if (!sender.hasPermission("novaconomy.economy.removebalance")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaPlayer nt = new NovaPlayer(target);

        if (remove <= 0) {
            sender.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        nt.remove(econ, remove);
        sender.sendMessage(String.format(getMessage("success.economy.removebalance"),  econ.getSymbol() + "", remove, target.getName()));
    }

    default void loadLanguages(CommandSender sender) {
        if (!sender.hasPermission("novaconomy.admin.reloadconfig")) {
            sender.sendMessage(getMessage("error.permission"));
            return;
        }

        NovaConfig.reloadLanguages(true);
        sender.sendMessage(getMessage("success.override_languages"));
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
        sender.sendMessage(String.format(getMessage("success.economy.setbalance"),  econ.getSymbol() + "", balance, target.getName()));
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

    default void balanceLeaderboard(Player p, Economy econ ) {
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
        List<NovaPlayer> players = new ArrayList<>(Arrays.stream(Bukkit.getOfflinePlayers()).map(NovaPlayer::new).sorted(Comparator.comparing(func).reversed()).collect(Collectors.toList())).subList(0, Math.min(Bukkit.getOfflinePlayers().length, 15));

        Inventory inv = wr.genGUI(54, get("constants.balance_leaderboard"), new Wrapper.CancelHolder());

        ItemStack type = new ItemStack(Material.PAPER);
        ItemMeta tMeta = type.getItemMeta();
        if (economy) {
            type.setType(econ.getIconType());
            tMeta.setDisplayName(ChatColor.AQUA + econ.getName());
        } else tMeta.setDisplayName(ChatColor.AQUA + get("constants.all_economies"));
        type.setItemMeta(tMeta);
        type = wr.setID(type, "economy:wheel:leaderboard");
        type = wr.setNBT(type, "economy", economy ? econ.getName() : "all");

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

        p.getInventory().addItem(wr.createCheck(econ, amount));
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

            wr.sendActionbar(p, String.format(getMessage("success.economy.receive_actionbar"), Math.floor(e.getAmount() * 100) / 100, e.getPayer().getName()));
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

        if (confirm) Business.remove(b);
        else sender.sendMessage(String.format(getMessage("constants.confirm_command"), "/business remove confirm"));
    }

    default void businessInfo(Player p) {
        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }
        p.openInventory(wr.generateBusinessData(b));
    }

    default void businessQuery(Player p, Business b) {
        if (!p.hasPermission("novaconomy.user.business.query")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }
        p.openInventory(wr.generateBusinessData(b));
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

        Inventory inv = wr.genGUI(36, pr.hasItemMeta() && pr.getItemMeta().hasDisplayName() ? pr.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(pr.getType().name().replace('_', ' ')));

        List<String> prLore = new ArrayList<>();
        prLore.add(String.format(get("constants.business.price"), price, econ.getSymbol()));

        ItemStack economyWheel = new ItemStack(econ.getIconType());
        economyWheel = wr.setNBT(economyWheel, "economy", econ.getName().toLowerCase());
        economyWheel = wr.setID(economyWheel, "economy:wheel:add_product");

        ItemMeta eMeta = economyWheel.getItemMeta();
        eMeta.setDisplayName(ChatColor.GOLD + econ.getName());
        economyWheel.setItemMeta(eMeta);
        inv.setItem(22, economyWheel);

        pr = wr.setNBT(pr, "price", price);
        ItemMeta meta = pr.getItemMeta();
        meta.setLore(prLore);
        pr.setItemMeta(meta);
        inv.setItem(13, pr);

        ItemStack confirm = new ItemStack(Material.BEACON);
        ItemMeta cMeta = confirm.getItemMeta();
        cMeta.setDisplayName(get("constants.confirm"));
        confirm.setItemMeta(cMeta);
        confirm = wr.setID(confirm, "business:add_product");
        confirm = wr.setNBT(confirm, "item", product);
        confirm = wr.setNBT(confirm, "price", price);
        confirm = wr.setNBT(confirm, "economy", econ.getName().toLowerCase());
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
            p.sendMessage(getMessage("error.business.exists"));
        }
    }

    default void addResource(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return;
        }

        Business b = Business.getByOwner(p);
        if (b == null) {
            p.sendMessage(getMessage("error.business.not_an_owner"));
            return;
        }

        Inventory inv = Bukkit.createInventory(new ReturnItemsHolder(p, "business:add_resource"), 54, get("constants.business.add_stock"));

        ItemStack confirm = new ItemStack(Material.BEACON);
        confirm = wr.setID(confirm, "business:add_resource");

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


        Inventory inv = wr.genGUI(54, get("constants.business.remove_product"));
        Inventory bData = wr.generateBusinessData(b);

        List<ItemStack> items =  Arrays.stream(bData.getContents())
                .filter(Objects::nonNull)
                .filter(wr::hasID)
                .collect(Collectors.toList());

        items.replaceAll(item -> wr.setID(item, "product:remove"));
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
        ItemStack head = new ItemStack(wr.isLegacy() ? Material.matchMaterial("SKULL_ITEM") : Material.matchMaterial("PLAYER_HEAD"));
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(p.getName());
        head.setItemMeta(meta);
        return head;
    }

    static List<Inventory> getBankBalanceGUI() {
        List<Inventory> invs = new ArrayList<>();

        ItemStack nextArrow = new ItemStack(Material.ARROW);
        ItemMeta nMeta = nextArrow.getItemMeta();
        nMeta.setDisplayName(ChatColor.AQUA + get("constants.next"));
        nextArrow.setItemMeta(nMeta);
        nextArrow = wr.setID(nextArrow, "next:bank_balance");

        ItemStack backArrow = new ItemStack(Material.ARROW);
        ItemMeta bMeta = backArrow.getItemMeta();
        bMeta.setDisplayName(ChatColor.RED + get("constants.prev"));
        backArrow.setItemMeta(bMeta);
        backArrow = wr.setID(backArrow, "prev:bank_balance");

        List<Economy> econs = new ArrayList<>(Economy.getEconomies()).stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());
        int pageCount = (int) Math.floor((econs.size() - 1D) / 28D) + 1;
        for (int i = 0; i < pageCount; i++) {
            Inventory inv = wr.genGUI(54, get("constants.bank.balance") + " - " + String.format(get("constants.page"), i + 1), new Wrapper.CancelHolder());

            if (pageCount > 1 && i < pageCount - 1) {
                ItemStack next = nextArrow.clone();
                next = wr.setNBT(next, "page", i);
                inv.setItem(48, next);
            }

            if (i > 0) {
                ItemStack back = backArrow.clone();
                back = wr.setNBT(back, "page", i);
                inv.setItem(50, back);
            }

            List<Economy> elist = new ArrayList<>(econs.subList(i * 28, Math.min((i + 1) * 28, econs.size())));
            elist.forEach(econ -> {
                ItemStack item = new ItemStack(econ.getIconType());
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
