package us.teaminceptus.novaconomy.abstraction;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.ChatPaginator;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.player.PlayerPayEvent;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public interface CommandWrapper {

    default void loadCommands() {}

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
        for (String name : plugin.getDescription().getCommands().keySet()) {
            PluginCommand pcmd = Bukkit.getPluginCommand(name);

            if (pcmd.getPermission() != null && !(sender.hasPermission(pcmd.getPermission()))) continue;

            if (sender.isOp())
                commandInfo.add(ChatColor.GOLD + pcmd.getUsage() + ChatColor.WHITE + " - " + ChatColor.GREEN + pcmd.getDescription() + ChatColor.WHITE + " | " + ChatColor.BLUE + (pcmd.getPermission() == null ? "No Permissions" : pcmd.getPermission()));
            else
                commandInfo.add(ChatColor.GOLD + pcmd.getUsage() + ChatColor.WHITE + " - " + ChatColor.GREEN + pcmd.getDescription());
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
        NovaConfig.reloadInterest();
        NovaConfig.reloadLanguages();
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "businesses.yml"));
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "functionality.yml"));
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
        p.sendMessage(String.format(getMessage("success.economy.convert"), (from.getSymbol() + "") + amount + "", (to.getSymbol() + "") + Math.floor(toBal * 100) / 100) + "");
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

        Wrapper wr = getWrapper();
        Inventory inv = wr.genGUI(36, getMessage("constants.economy.exchange"));

        List<Economy> economies = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList());

        Economy economy1 = economies.get(0);
        ItemStack econ1 = new ItemStack(economy1.getIcon());
        ItemMeta e1Meta = econ1.getItemMeta();
        e1Meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + amount + "" + economy1.getSymbol()));
        econ1.setItemMeta(e1Meta);
        wr.setID(econ1, "exchange:1");
        wr.setNBT(econ1, "economy", economy1.getUniqueId().toString());
        wr.setNBT(econ1, "amount", amount);
        inv.setItem(12, econ1);

        Economy economy2 = economies.get(1);
        ItemStack econ2 = new ItemStack(economy2.getIcon());
        ItemMeta e2Meta = econ1.getItemMeta();
        e2Meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + economy1.convertAmount(economy2, amount) + "" + economy1.getSymbol()));
        econ1.setItemMeta(e2Meta);
        wr.setID(econ2, "exchange:2");
        wr.setNBT(econ2, "economy", economy2.getUniqueId().toString());
        wr.setNBT(econ2, "amount", Math.floor(economy1.convertAmount(economy2, amount) * 100) / 100);
        inv.setItem(14, econ2);

        ItemStack yes = new ItemStack(limeWool());
        ItemMeta yMeta = yes.getItemMeta();
        yMeta.setDisplayName(ChatColor.GREEN + get("constants.yes"));
        yes.setItemMeta(yMeta);
        wr.setID(yes, "yes:exchange");
        inv.setItem(28, yes);

        ItemStack no = new ItemStack(redWool());
        ItemMeta nMeta = no.getItemMeta();
        nMeta.setDisplayName(ChatColor.RED + get("constants.cancel"));
        no.setItemMeta(nMeta);
        wr.setID(no, "no:exchange");
        inv.setItem(30, no);

        p.openInventory(inv);
    }

    static ItemStack limeWool() {
        if (getWrapper().isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 5);
        else return new ItemStack(Material.matchMaterial("LIME_WOOL"));
    }

    static ItemStack redWool() {
        if (getWrapper().isLegacy()) return new ItemStack(Material.matchMaterial("WOOL"), 14);
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
        if (!sender.hasPermission("novaconomy.economy")) {
            sender.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        NovaConfig.getConfiguration().setInterestEnabled(enabled);
        String key = "success.economy." + (enabled ? "enable" : "disable" ) + "_interest";
        sender.sendMessage(getMessage(key));
    }

    default void createCheck(Player p, Economy econ, double amount) {
        if (!p.hasPermission("novaconomy.economy.check")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }

        if (amount < 1) {
            p.sendMessage(getMessage("error.argument.amount"));
            return;
        }

        p.getInventory().addItem(getWrapper().createCheck(econ, amount));
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

            getWrapper().sendActionbar(p, String.format(getMessage("success.economy.receive_actionbar"), Math.floor(e.getAmount() * 100) / 100, e.getPayer().getName()));
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
        p.openInventory(getWrapper().generateBusinessData(b));
    }

    default void businessQuery(Player p, Business b) {
        if (!p.hasPermission("novaconomy.user.business.query")) {
            p.sendMessage(getMessage("error.permission.argument"));
            return;
        }
        p.openInventory(getWrapper().generateBusinessData(b));
    }

    default void addProduct(Player p, double price) {
        if (Economy.getEconomies().size() < 1) {
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

        Wrapper wr = getWrapper();

        List<String> sortedList = new ArrayList<>();
        Economy.getEconomies().forEach(econ -> sortedList.add(econ.getName()));
        sortedList.sort(String.CASE_INSENSITIVE_ORDER);
        Economy econ = Economy.getEconomy(sortedList.get(0));

        Inventory inv = wr.genGUI(36, pr.hasItemMeta() && pr.getItemMeta().hasDisplayName() ? pr.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(pr.getType().name().replace('_', ' ')));

        List<String> prLore = new ArrayList<>();
        prLore.add(String.format(get("constants.business.price"), price, econ.getSymbol()));

        ItemStack economyWheel = new ItemStack(econ.getIconType());
        economyWheel = wr.setNBT(economyWheel, "economy", econ.getName().toLowerCase());
        wr.setID(economyWheel, "economy:wheel:add_product");

        ItemMeta eMeta = economyWheel.getItemMeta();
        eMeta.setDisplayName(ChatColor.GOLD + econ.getName());
        economyWheel.setItemMeta(eMeta);
        inv.setItem(22, economyWheel);

        pr = getWrapper().setNBT(pr, "price", price);
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
        if (Economy.getEconomies().size() < 1) {
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
        confirm = getWrapper().setID(confirm, "business:add_resource");

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
        if (Economy.getEconomies().size() < 1) {
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

        Wrapper wr = getWrapper();
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
}
