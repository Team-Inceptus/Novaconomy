package us.teaminceptus.novaconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CommandWrapperV1 implements CommandWrapper, TabExecutor {

    private final Plugin plugin;
    private static final Function<String, List<String>> modkeys = (s) -> {
        FileConfiguration config = NovaConfig.getConfig();
        return config.getConfigurationSection(s)
                .getKeys(false)
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    };

    private static final Wrapper w = Wrapper.getWrapper();

    public CommandWrapperV1(Plugin plugin) {
        this.plugin = plugin;
        loadCommands();
        plugin.getLogger().info("Loaded Command Version v1 (1.8+)");
    }

    private PluginCommand createCommand(String name, String... aliases) {
        try {
            Constructor<PluginCommand> p = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            p.setAccessible(true);

            PluginCommand cmd = p.newInstance(name, plugin);
            if (aliases != null && aliases.length > 0) cmd.setAliases(Arrays.asList(aliases));
            return cmd;
        } catch (Exception e) {
            NovaConfig.print(e);
            return null;
        }
    }

    private void register(PluginCommand cmd) {
        try {
            Server srv = Bukkit.getServer();
            Field bukkitmap = srv.getClass().getDeclaredField("commandMap");
            bukkitmap.setAccessible(true);

            CommandMap map = (CommandMap) bukkitmap.get(srv);
            map.register(cmd.getName(), cmd);
        } catch (Exception e) {
            NovaConfig.print(e);
        }
    }

    private boolean economyCount(CommandSender sender) {
        if (Economy.getEconomies().isEmpty()) {
            sender.sendMessage(getMessage("error.economy.none"));
            return false;
        }

        return true;
    }


    private static String getMessage(String key) {
        return CommandWrapper.getMessage(key);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName()) {
            case "ehelp": {
                help(sender);
                break;
            }
            case "balance": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;
                if (economyCount(p)) balance(p);
                break;
            }
            case "novaconomyreload": {
                reloadConfig(sender);
                break;
            }
            case "convert": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;
                if (!economyCount(p)) return false;

                try {
                    if (args.length < 1) {
                        p.sendMessage(getMessage("error.economy.transfer_from"));
                        return false;
                    }

                    if (!Economy.exists(args[0])) {
                        p.sendMessage(getMessage("error.economy.transfer_from"));
                        return false;
                    }

                    Economy from = Economy.getEconomy(args[0]);

                    if (args.length < 2) {
                        p.sendMessage(getMessage("error.economy.transfer_to"));
                        return false;
                    }

                    if (!Economy.exists(args[1])) {
                        p.sendMessage(getMessage("error.economy.transfer_to"));
                        return false;
                    }

                    Economy to = Economy.getEconomy(args[1]);

                    if (args.length < 3) {
                        p.sendMessage(getMessage("error.economy.transfer_amount"));
                        return false;
                    }

                    double amount = Double.parseDouble(args[2]);
                    convert(p, from, to, amount);
                } catch (NumberFormatException e) {
                    p.sendMessage(getMessage("error.economy.transfer_amount"));
                    return false;
                }
                break;
            }
            case "exchange": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                try {
                    if (args.length < 1) {
                        p.sendMessage(getMessage("error.economy.transfer_amount"));
                        return false;
                    }

                    double amount = Double.parseDouble(args[0]);
                    exchange(p, amount);
                } catch (NumberFormatException e) {
                    p.sendMessage(getMessage("error.economy.transfer_amount"));
                    return false;
                }
                break;
            }
            case "pay": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;
                if (!economyCount(p)) return false;

                if (args.length < 1) {
                    p.sendMessage(getMessage("error.argument.player"));
                    return false;
                }

                if (Bukkit.getPlayer(args[0]) == null) {
                    p.sendMessage(getMessage("error.argument.player"));
                    return false;
                }

                Player target = Bukkit.getPlayer(args[0]);

                Economy econ = null;

                if (args.length >= 2) {
                    econ = Economy.getEconomy(args[1]);

                    if (econ == null) {
                        p.sendMessage(getMessage("error.argument.economy"));
                        return false;
                    }
                }

                double amount = 0;

                if (args.length >= 3) try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    p.sendMessage(getMessage("error.argument.pay_amount"));
                    return false;
                }

                pay(p, target, econ, amount);
                break;
            }
            case "economy": {
                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument"));
                    return false;
                }

                switch (args[0].toLowerCase()) {
                    case "create": {
                        try {
                            if (args.length < 2) {
                                sender.sendMessage(getMessage("error.argument.name"));
                                return false;
                            }

                            String name = args[1];

                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.symbol"));
                                return false;
                            }

                            if (args[2].length() > 1) {
                                sender.sendMessage(getMessage("error.argument.symbol"));
                                return false;
                            }

                            char symbol = args[2].charAt(0);

                            if (args.length < 4) {
                                sender.sendMessage(getMessage("error.argument.icon"));
                                return false;
                            }

                            Material icon = Material.valueOf(args[3].replace("minecraft:", "").toUpperCase());

                            double scale = 1;

                            if (args.length >= 5) try {
                                scale = Double.parseDouble(args[4]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(getMessage("error.argument.scale"));
                                return false;
                            }

                            boolean naturalIncrease = true;

                            if (args.length >= 6) {
                                if (!(args[5].equalsIgnoreCase("true")) && !(args[5].equalsIgnoreCase("false"))) {
                                    sender.sendMessage(getMessage("error.argument.bool"));
                                    return false;
                                }

                                naturalIncrease = Boolean.parseBoolean(args[5]);
                            }

                            boolean clickableReward = true;

                            if (args.length >= 7) {
                                if (!(args[6].equalsIgnoreCase("true")) && !(args[6].equalsIgnoreCase("false"))) {
                                    sender.sendMessage(getMessage("error.argument.bool"));
                                    return false;
                                }

                                clickableReward = Boolean.parseBoolean(args[6]);
                            }

                            createEconomy(sender, name, symbol, icon, scale, naturalIncrease, clickableReward);
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(getMessage("error.argument"));
                            return false;
                        }
                        break;
                    }
                    case "delete": {
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);
                        removeEconomy(sender, econ);
                        break;
                    }
                    case "info": {
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        economyInfo(sender, econ);
                        break;
                    }
                    case "addbalance":
                    case "addbal":
                    case "removebalance":
                    case "removebal":
                    case "setbalance":
                    case "setbal": {
                        try {
                            if (args.length < 2) {
                                sender.sendMessage(getMessage("error.argument.economy"));
                                return false;
                            }

                            if (!Economy.exists(args[1])) {
                                sender.sendMessage(getMessage("error.economy.inexistent"));
                                return false;
                            }

                            Economy econ = Economy.getEconomy(args[1]);

                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.player"));
                                return false;
                            }

                            if (Bukkit.getPlayer(args[2]) == null) {
                                sender.sendMessage(getMessage("error.argument.player"));
                                return false;
                            }

                            Player target = Bukkit.getPlayer(args[2]);

                            if (args.length < 4) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            double amount = Double.parseDouble(args[3]);

                            if (args[0].startsWith("add")) addBalance(sender, econ, target, amount);
                            else if (args[0].startsWith("remove")) removeBalance(sender, econ, target, amount);
                            else if (args[0].startsWith("set")) setBalance(sender, econ, target, amount);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }
                        break;
                    }
                    case "interest": {
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument"));
                            return false;
                        }

                        if (!args[1].equalsIgnoreCase("enable") && !args[1].equalsIgnoreCase("disable")) {
                            sender.sendMessage(getMessage("error.argument"));
                            return false;
                        }

                        interest(sender, args[1].equalsIgnoreCase("enable") || args[1].equalsIgnoreCase("true"));
                        break;
                    }
                    case "check": case "createcheck": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;
                        if (!economyCount(p)) return false;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        try {
                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            double amount = Double.parseDouble(args[2]);

                            createCheck(p, econ, amount, false);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }

                        break;
                    }
                    case "modeldata":
                    case "custommodeldata":
                    case "setcustommodeldata":
                    case "setmodeldata": {
                        if (!economyCount(sender)) return false;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        try {
                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            int data = Integer.parseInt(args[2]);
                            setEconomyModel(sender, econ, data);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }
                        break;
                    }
                    case "seticon": case "icon": {
                        if (!sender.hasPermission("novaconomy.economy.create")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }
                        if (!economyCount(sender)) return false;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        if (args.length < 3) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        Material m = Material.matchMaterial(args[2]);

                        if (m == null) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        setEconomyIcon(sender, econ, m);
                        break;
                    }
                    case "setconversionscale":
                    case "setscale":
                    case "conversionscale":
                    case "scale": {
                        if (!sender.hasPermission("novaconomy.economy.create")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }
                        if (!economyCount(sender)) return false;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        try {
                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            double scale = Double.parseDouble(args[2]);
                            setEconomyScale(sender, econ, scale);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }
                        break;
                    }
                    case "setnaturalincrease":
                    case "setnatural":
                    case "natural":
                    case "naturalincrease": {
                        if (!sender.hasPermission("novaconomy.economy.create")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }
                        if (!economyCount(sender)) return false;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        if (args.length < 3) {
                            sender.sendMessage(getMessage("error.argument.bool"));
                            return false;
                        }

                        if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false")) {
                            sender.sendMessage(getMessage("error.argument.bool"));
                            return false;
                        }

                        boolean naturalIncrease = Boolean.parseBoolean(args[2].toLowerCase());
                        setEconomyNatural(sender, econ, naturalIncrease);
                        break;
                    }
                    case "setname":
                    case "name": {
                        if (!sender.hasPermission("novaconomy.economy.create")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }
                        if (!economyCount(sender)) return false;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        if (!Economy.exists(args[1])) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        if (args.length < 3) {
                            sender.sendMessage(getMessage("error.argument.name"));
                            return false;
                        }

                        setEconomyName(sender, econ, args[2]);
                        break;
                    }
                    default: {
                        sender.sendMessage(getMessage("error.argument"));
                        return false;
                    }
                }
                break;
            }
            case "business": {
                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument"));
                    return false;
                }

                switch (args[0].toLowerCase()) {
                    case "information": case "info": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;
                        businessInfo(p);
                        break;
                    }
                    case "query": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.business"));
                            return false;
                        }

                        if (Business.getByName(args[1]) == null) {
                            sender.sendMessage(getMessage("error.business.inexistent"));
                            return false;
                        }

                        Business b = Business.getByName(args[1]);
                        businessQuery(p, b);
                        break;
                    }
                    case "create": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.name"));
                            return false;
                        }

                        if (args.length < 3) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        Material icon = Material.matchMaterial(args[2]);

                        if (icon == null) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        if (!w.isItem(icon)) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        createBusiness(p, args[2], icon);
                        break;
                    }
                    case "addproduct": case "addp": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }

                        try {
                            double amount = Double.parseDouble(args[1]);
                            addProduct(p, amount);
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }
                        break;
                    }
                    case "addresource": case "stock": case "addr": case "addstock": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (!p.hasPermission("novaconomy.user.business.resources")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }

                        addResource(p);
                        break;
                    }
                    case "removeprodct": case "removep": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;
                        removeProduct(p);
                        break;
                    }
                    case "delete": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;
                        deleteBusiness(p, args.length < 2 && args[1].equalsIgnoreCase("confirm"));
                        break;
                    }
                    case "remove": {
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.business"));
                            return false;
                        }

                        if (Business.getByName(args[1]) == null) {
                            sender.sendMessage(getMessage("error.business.inexistent"));
                            return false;
                        }

                        Business b = Business.getByName(args[1]);

                        removeBusiness(sender, b, args.length < 3 && args[2].equalsIgnoreCase("confirm"));
                        break;
                    }
                    case "home": case "sethome": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (!p.hasPermission("novaconomy.user.business.home")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }

                        businessHome(p, args[0].equalsIgnoreCase("sethome"));
                        break;
                    }
                    case "stats": case "statistics": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (!Business.exists(p)) {
                            sender.sendMessage(getMessage("error.business.not_an_owner"));
                            return false;
                        }

                        businessStatistics(p, Business.getByOwner(p));
                        break;
                    }
                    case "rating": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 1) {
                            sender.sendMessage(getMessage("error.argument.player"));
                            return false;
                        }

                        OfflinePlayer target = Wrapper.getPlayer(args[0]);

                        if (target == null) {
                            sender.sendMessage(getMessage("error.argument.player"));
                            return false;
                        }

                        businessRating(p, target);
                        break;
                    }
                    case "discover": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (!p.hasPermission("novaconomy.user.business.discover")) {
                            sender.sendMessage(getMessage("error.permission.argument"));
                            return false;
                        }

                        StringBuilder keywords = new StringBuilder();

                        if (args.length >= 2) for (int i = 1; i < args.length; i++) keywords.append(args[i]).append(" ");
                        discoverBusinesses(p, keywords.toString().split("[, ]"));
                        break;
                    }
                    case "settings": case "setting": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        settings(p, "business");
                        break;
                    }
                    case "editprice": case "price": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }

                        try {
                            double amount = Double.parseDouble(args[1]);

                            if (amount <= 0) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            Economy econ = null;
                            if (args.length > 2) {
                                econ = Economy.getEconomy(args[2]);

                                if (econ == null) {
                                    sender.sendMessage(getMessage("error.argument.economy"));
                                    return false;
                                }
                            }

                            editPrice(p, amount, econ);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }

                        break;
                    }
                    case "setname": case "name": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.name"));
                            return false;
                        }

                        setBusinessName(p, args[1]);
                        break;
                    }
                    case "seticon": case "icon": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        Material icon = Material.matchMaterial(args[1]);

                        if (icon == null) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        if (!w.isItem(icon)) {
                            sender.sendMessage(getMessage("error.argument.icon"));
                            return false;
                        }

                        setBusinessIcon(p, icon);
                        break;
                    }
                    case "recover": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;
                        businessRecover(p);
                        break;
                    }
                    case "keywords":
                    case "keyword": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            listKeywords(p);
                            break;
                        }

                        switch (args[1].toLowerCase()) {
                            case "list":
                            case "l": {
                                listKeywords(p);
                                break;
                            }
                            case "add": {
                                if (args.length < 3) {
                                    sender.sendMessage(getMessage("error.argument.keywords"));
                                    return false;
                                }

                                List<String> keywords = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
                                addKeywords(p, keywords.toArray(new String[0]));
                                break;
                            }
                            case "remove":
                            case "delete": {
                                if (args.length < 3) {
                                    sender.sendMessage(getMessage("error.argument.keywords"));
                                    return false;
                                }

                                List<String> keywords = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
                                removeKeywords(p, keywords.toArray(new String[0]));
                                break;
                            }
                            default: {
                                sender.sendMessage(getMessage("error.argument"));
                                break;
                            }
                        }

                        break;
                    }
                    case "advertising":
                    case "ads":
                    case "advertise": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            businessAdvertising(p);
                            break;
                        }

                        switch (args[1].toLowerCase()) {
                            case "add":
                            case "addbal":
                            case "addbalance": {
                                businessAdvertisingChange(p, true);
                                break;
                            }
                            case "remove":
                            case "removebal":
                            case "removebalance": {
                                businessAdvertisingChange(p, false);
                                break;
                            }
                            default: {
                                p.sendMessage(getMessage("error.argument"));
                                return false;
                            }
                        }

                        break;
                    }
                    case "blacklist":
                    case "blackl":
                    case "blist":
                    case "bl": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        if (args.length < 2) {
                            listBlacklist(p);
                            break;
                        }

                        switch (args[1].toLowerCase()) {
                            case "add": {
                                if (args.length < 3) {
                                    p.sendMessage(getMessage("error.argument.business"));
                                    return false;
                                }

                                Business b = Business.getByName(args[2]);

                                if (b == null) {
                                    p.sendMessage(getMessage("error.argument.business"));
                                    return false;
                                }

                                addBlacklist(p, b);
                                break;
                            }
                            case "remove":
                            case "delete": {
                                if (args.length < 3) {
                                    p.sendMessage(getMessage("error.argument.business"));
                                    return false;
                                }

                                Business b = Business.getByName(args[2]);

                                if (b == null) {
                                    p.sendMessage(getMessage("error.argument.business"));
                                    return false;
                                }

                                removeBlacklist(p, b);
                                break;
                            }
                        }
                        break;
                    }
                    case "allratings":
                    case "allrating":
                    case "allr":
                    case "ar": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        allBusinessRatings(p);
                        break;
                    }
                    default: {
                        sender.sendMessage(getMessage("error.argument"));
                        return false;
                    }
                }
                break;
            }
            case "bank": {
                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument"));
                    return false;
                }

                switch (args[0].toLowerCase()) {
                    case "balances": case "balance": case "info": case "information": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;
                        bankBalances(p);
                        break;
                    }
                    case "deposit": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        try {
                            if (args.length < 2) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }
                            double amount = Double.parseDouble(args[1]);

                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.economy"));
                                return false;
                            }

                            Economy econ = Economy.getEconomy(args[2]);
                            if (econ == null) {
                                sender.sendMessage(getMessage("error.economy.inexistent"));
                                return false;
                            }

                            bankDeposit(p, amount, econ);
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }
                    }
                    case "withdraw": {
                        if (!(sender instanceof Player)) return false;
                        Player p = (Player) sender;

                        try {
                            if (args.length < 2) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }
                            double amount = Double.parseDouble(args[1]);

                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.economy"));
                                return false;
                            }

                            Economy econ = Economy.getEconomy(args[2]);
                            if (econ == null) {
                                sender.sendMessage(getMessage("error.economy.inexistent"));
                                return false;
                            }

                            bankWithdraw(p, amount, econ);
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }
                    }
                    default: {
                        sender.sendMessage(getMessage("error.argument"));
                        return false;
                    }
                }
            }
            case "createcheck": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                try {
                    if (args.length < 1) {
                        sender.sendMessage(getMessage("error.argument.economy"));
                        return false;
                    }
                    Economy econ = Economy.getEconomy(args[0]);

                    if (econ == null) {
                        sender.sendMessage(getMessage("error.economy.inexistent"));
                        return false;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(getMessage("error.argument.amount"));
                        return false;
                    }

                    double amount = Double.parseDouble(args[1]);
                    if (amount < 1) {
                        p.sendMessage(getMessage("error.argument.amount"));
                        return false;
                    }

                    createCheck(p, econ, amount, true);
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return false;
                }
            }
            case "balanceleaderboard": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                Economy econ = null;
                if (args.length > 0) {
                    econ = Economy.getEconomy(args[0]);
                    if (econ == null) {
                        sender.sendMessage(getMessage("error.economy.inexistent"));
                        return false;
                    }
                }

                balanceLeaderboard(p, econ);
                break;
            }
            case "bounty": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument"));
                    return false;
                }

                try {
                    switch (args[0].toLowerCase()) {
                        case "add":
                        case "create": {
                            if (args.length < 2) {
                                sender.sendMessage(getMessage("error.argument.player"));
                                return false;
                            }

                            OfflinePlayer target = Wrapper.getPlayer(args[1]);

                            if (target == null) {
                                sender.sendMessage(getMessage("error.argument.player"));
                                return false;
                            }

                            if (args.length < 3) {
                                sender.sendMessage(getMessage("error.argument.economy"));
                                return false;
                            }

                            Economy econ = Economy.getEconomy(args[2]);

                            if (econ == null) {
                                sender.sendMessage(getMessage("error.economy.inexistent"));
                                return false;
                            }

                            if (args.length < 4) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            double amount = Double.parseDouble(args[3]);

                            if (amount <= 0) {
                                sender.sendMessage(getMessage("error.argument.amount"));
                                return false;
                            }

                            createBounty(p, target, econ, amount);
                            break;
                        }
                        case "remove":
                        case "delete": {
                            if (args.length < 2) {
                                sender.sendMessage(getMessage("error.argument.player"));
                                return false;
                            }

                            OfflinePlayer target = Wrapper.getPlayer(args[1]);

                            if (target == null) {
                                sender.sendMessage(getMessage("error.argument.player"));
                                return false;
                            }

                            deleteBounty(p, target);
                            break;
                        }
                        case "self": {
                            listBounties(p, false);
                            break;
                        }
                        case "owned": {
                            listBounties(p, true);
                            break;
                        }
                        default: {
                            sender.sendMessage(getMessage("error.argument"));
                            return false;
                        }
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("error.argument.amount"));
                    return false;
                }
            }
            case "taxevent": {
                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument.event"));
                    return false;
                }

                boolean self = true;
                if (args.length > 2) self = Boolean.parseBoolean(args[1]);

                callEvent(sender, args[0], self);

                break;
            }
            case "settings": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                if (args.length < 1) settings(p, null);
                else {
                    if (!args[0].equalsIgnoreCase("business") && !args[0].equalsIgnoreCase("personal")) {
                        p.sendMessage(getMessage("error.argument"));
                        return false;
                    }

                    settings(p, args[0]);
                }
                break;
            }
            case "rate": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument.business"));
                    return false;
                }

                if (!Business.exists(args[0])) {
                    sender.sendMessage(getMessage("error.business.inexistent"));
                    return false;
                }

                Business b = Business.getByName(args[0]);
                String comment = args.length < 2 ? "" : String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                rate(p, b, comment);
                break;
            }
            case "statistics": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                OfflinePlayer target = p;

                if (args.length > 1) {
                    target = Wrapper.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(getMessage("error.argument.player"));
                        return false;
                    }
                }

                playerStatistics(p, target);
                break;
            }
            case "novaconfig": {
                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument"));
                    return false;
                }

                switch (args[0].toLowerCase()) {
                    case "reload":
                    case "rl": {
                        reloadConfig(sender);
                        break;
                    }
                    case "naturalcauses":
                    case "nc":
                    case "naturalc":
                    case "ncauses": {
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument"));
                            return false;
                        }

                        switch (args[1].toLowerCase()) {
                            case "view": {
                                if (args.length < 3) {
                                    sender.sendMessage(getMessage("error.argument.config"));
                                    return false;
                                }

                                configNaturalCauses(sender, args[2].toLowerCase(), null);
                                break;
                            }
                            case "set": {
                                if (args.length < 3) {
                                    sender.sendMessage(getMessage("error.argument.config"));
                                    return false;
                                }

                                if (args.length < 4) {
                                    sender.sendMessage(getMessage("error.argument"));
                                    return false;
                                }

                                configNaturalCauses(sender, args[2].toLowerCase(), args[3]);
                                break;
                            }
                            case "modifier":
                            case "mod": {
                                if (args.length < 3) {
                                    sender.sendMessage(getMessage("error.argument"));
                                    return false;
                                }

                                switch (args[2].toLowerCase()) {
                                    case "add": {
                                        if (args.length < 4) {
                                            sender.sendMessage(getMessage("error.argument.config"));
                                            break;
                                        }

                                        if (args.length < 5) {
                                            sender.sendMessage(getMessage("error.argument.config"));
                                            break;
                                        }

                                        if (args.length < 6) {
                                            sender.sendMessage(getMessage("error.argument"));
                                            break;
                                        }

                                        StringBuilder values = new StringBuilder();
                                        for (int i = 5; i < args.length; i++) values.append(args[i]).append(",");

                                        addCausesModifier(sender, args[3].toLowerCase(), args[4], values.toString().replace(" ", ",").split(","));
                                        break;
                                    }
                                    case "remove":
                                    case "delete": {
                                        if (args.length < 4) {
                                            sender.sendMessage(getMessage("error.argument.config"));
                                            break;
                                        }

                                        if (args.length < 5) {
                                            sender.sendMessage(getMessage("error.argument.config"));
                                            break;
                                        }

                                        removeCausesModifier(sender, args[3].toLowerCase(), args[4]);
                                        break;
                                    }
                                    case "view": {
                                        if (args.length < 4) {
                                            sender.sendMessage(getMessage("error.argument.config"));
                                            break;
                                        }

                                        if (args.length < 5) {
                                            sender.sendMessage(getMessage("error.argument.config"));
                                            break;
                                        }

                                        viewCausesModifier(sender, args[3].toLowerCase(), args[4]);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case "setdefaultecon":
                    case "setdefaulteconomy":
                    case "defaultecon":
                    case "defaulteconomy": {
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("error.argument.economy"));
                            return false;
                        }

                        Economy econ = Economy.getEconomy(args[1]);

                        if (econ == null) {
                            sender.sendMessage(getMessage("error.economy.inexistent"));
                            return false;
                        }

                        setDefaultEconomy(sender, econ);
                        break;
                    }
                }

                break;
            }
            case "businessleaderboard": {
                if (!(sender instanceof Player)) return false;
                Player p = (Player) sender;

                businessLeaderboard(p, "ratings");
                break;
            }
            default: {
                sender.sendMessage(getMessage("error.argument"));
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        switch (cmd.getName()) {
            case "convert": {
                switch (args.length) {
                    case 1: {
                        for (Economy econ : Economy.getEconomies()) suggestions.add(econ.getName());
                        return suggestions;
                    }
                    case 2: {
                        for (Economy econ : Economy.getEconomies()) {
                            if (econ.getName().equals(args[0])) continue;
                            suggestions.add(econ.getName());
                        }
                        return suggestions;
                    }
                }
                return suggestions;
            }
            case "economy": {
                switch (args.length) {
                    case 1: {
                        suggestions.add("info");

                        if (sender.hasPermission("novaconomy.economy")) suggestions.addAll(Arrays.asList(
                                "check", "createcheck", "create", "delete", "addbal", "setbalance",
                                "removebal", "addbalance", "removebalance", "setbal", "setmodeldata", "setcustommodeldata", "modeldata", "custommodeldata",
                                "seticon", "icon", "setconversionscale", "conversionscale", "setscale", "scale", "setnaturalincrease", "naturalincrease",
                                "setnatural", "natural", "setname", "name", "setclickablereward", "clickablereward", "setclickable", "clickable"));
                        return suggestions;
                    }
                    case 2: {
                        if (!(args[0].equalsIgnoreCase("create")))
                            suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toList()));

                        return suggestions;
                    }
                    case 3: {
                        switch (args[0].toLowerCase()) {
                            case "bal": {
                                suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                                return suggestions;
                            }
                            case "create": {
                                suggestions.addAll(Arrays.asList("$", "%", "Q", "L", "P", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b"));
                                return suggestions;
                            }
                            case "seticon": case "icon": {
                                suggestions.addAll(Arrays.stream(Material.values()).filter(w::isItem).map(Material::name).map(String::toLowerCase).collect(Collectors.toList()));
                                return suggestions;
                            }
                            case "setnaturalincrease":
                            case "naturalincrease":
                            case "setnatural":
                            case "natural":
                            case "setclickablereward":
                            case "clickablereward":
                            case "setclickable":
                            case "clickable": {
                                suggestions.addAll(Arrays.asList("true", "false"));
                                return suggestions;
                            }
                        }

                        return suggestions;
                    }
                    case 4: {
                        if (args[0].equalsIgnoreCase("create"))
                            suggestions.addAll(Arrays.stream(Material.values()).filter(w::isItem).map(Material::name).map(String::toLowerCase).collect(Collectors.toList()));

                        return suggestions;
                    }
                    case 6: {
                        if (args[0].equalsIgnoreCase("create"))
                            suggestions.addAll(Arrays.asList("true", "false"));

                        return suggestions;
                    }

                }
                return suggestions;
            }
            case "pay": {
                switch (args.length) {
                    case 1:
                        suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toSet()));
                        return suggestions;

                    case 2:
                        suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toSet()));
                        return suggestions;
                }

                return suggestions;
            }
            case "business": {
                switch (args.length) {
                    case 1:
                        suggestions.addAll(Arrays.asList("info", "information", "query", "create", "addproduct", "addp", "removeproduct", "removep",
                                "addresource", "stock", "addr", "addstock", "rating", "setting", "settings", "price", "editprice", "stats", "statistics", "discover",
                                "setname", "name", "seticon", "icon", "recover", "keyword", "keywords", "blacklist", "blackl", "bl", "blist", "ads", "advertise", "advertising",
                                "allratings", "allrating", "allr", "ar"));
                        return suggestions;

                    case 2:
                        switch (args[0].toLowerCase()) {
                            case "query": {
                                suggestions.addAll(Business.getBusinesses().stream().map(Business::getName).collect(Collectors.toSet()));
                                break;
                            }
                            case "keyword":
                            case "keywords":
                            case "blacklist":
                            case "blackl":
                            case "bl":
                            case "blist": {
                                suggestions.addAll(Arrays.asList("add", "remove", "delete", "list", "l"));
                                break;
                            }
                            case "ads":
                            case "advertising":
                            case "advertise": {
                                suggestions.addAll(Arrays.asList("add", "addbal", "addbalance", "remove", "removebal", "removebalance"));
                                break;
                            }
                        }

                        return suggestions;
                    case 3:
                        switch (args[0].toLowerCase()) {
                            case "create":
                                suggestions.addAll(Arrays.stream(Material.values()).filter(w::isItem).map(Material::name).map(String::toLowerCase).collect(Collectors.toSet()));
                                break;
                            case "rating":
                                suggestions.addAll(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).collect(Collectors.toSet()));
                                break;
                            case "price": case "editprice":
                                suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toSet()));
                                break;
                            case "blacklist":
                            case "blackl":
                            case "bl":
                            case "blist": {
                                suggestions.addAll(Business.getBusinesses().stream().map(Business::getName).collect(Collectors.toSet()));
                                break;
                            }

                        }
                        return suggestions;
                    case 4:
                        switch (args[0].toLowerCase()) {
                            case "blacklist":
                            case "blackl":
                            case "bl":
                            case "blist": {
                                switch (args[1].toLowerCase()) {
                                    case "add": {
                                        suggestions.addAll(Business.getBusinesses().stream().filter(b -> {
                                            OfflinePlayer p = Wrapper.getPlayer(sender.getName());
                                            return !b.isOwner(p) && !Business.getByOwner(p).isBlacklisted(b);
                                        }).map(Business::getName).collect(Collectors.toList()));
                                        break;
                                    }
                                    case "remove":
                                    case "delete": {
                                        suggestions.addAll(Business.getByOwner(Wrapper.getPlayer(sender.getName())).getBlacklist().stream().map(Business::getName).collect(Collectors.toList()));
                                        break;
                                    }
                                }
                            }
                        }

                        return suggestions;
                }
                return suggestions;
            }
            case "bank": {
                switch (args.length) {
                    case 1:
                        suggestions.addAll(Arrays.asList("balances", "balance", "info", "information", "deposit", "withdraw"));
                        return suggestions;
                    case 3:
                        if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw"))
                            suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toSet()));
                }
                return suggestions;
            }
            case "createcheck": case "balanceleaderboard": {
                if (args.length == 1) suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toSet()));
                return suggestions;
            }
            case "taxevent": {
                switch (args.length) {
                    case 1:
                        suggestions.addAll(NovaConfig.getConfiguration().getAllCustomEvents().stream().map(NovaConfig.CustomTaxEvent::getIdentifier).collect(Collectors.toSet()));
                        return suggestions;
                    case 2:
                        suggestions.addAll(Arrays.asList("true", "false"));
                        return suggestions;
                }
            }
            case "settings": {
                if (args.length == 1) suggestions.addAll(Arrays.asList("business", "personal"));
                return suggestions;
            }
            case "rate": {
                if (args.length == 1) suggestions.addAll(Business.getBusinesses().stream().map(Business::getName).collect(Collectors.toSet()));
                return suggestions;
            }
            case "novaconfig": {
                switch (args.length) {
                    case 1: {
                        suggestions.addAll(Arrays.asList("reload", "naturalcauses", "rl", "nc", "ncauses", "naturalc",
                                "business", "businesses", "bc", "bounty", "bounties"));
                        break;
                    }
                    case 2: {
                        switch (args[0].toLowerCase()) {
                            case "naturalcauses":
                            case "nc":
                            case "ncauses":
                            case "naturalc": {
                                suggestions.addAll(Arrays.asList("add, view, modifier"));
                                break;
                            }
                            case "business":
                            case "businesses":
                            case "bs": {
                                suggestions.addAll(Arrays.asList("advertising", "ads"));
                                break;
                            }
                            case "bounty":
                            case "bounties": {
                                suggestions.addAll(Arrays.asList("enable", "on", "disable", "off", "broadcast"));
                                break;
                            }
                        }
                    }
                    case 3: {
                        switch (args[0].toLowerCase()) {
                            case "naturalcauses":
                            case "nc":
                            case "ncauses":
                            case "naturalc": {
                                switch (args[1].toLowerCase()) {
                                    case "set":
                                    case "view": {
                                        suggestions.addAll(Arrays.asList("enchant_bonus", "max_increase", "kill_increase", "kill_increase_chance", "kill_increase_indirect", "fishing_increase",
                                                "fishing_increase_chance", "mining_increase", "mining_increase_chance", "farming_increase", "farming_increase_chance",
                                                "death_decrease", "death_divider"));
                                        break;
                                    }
                                    case "modifier":
                                    case "mod": {
                                        suggestions.addAll(Arrays.asList("add", "remove", "delete"));
                                        break;
                                    }
                                }

                                break;
                            }
                            case "business":
                            case "businesses":
                            case "bs": {
                                switch (args[1].toLowerCase()) {
                                    case "advertising":
                                    case "ads": {
                                        suggestions.addAll(Arrays.asList("enable", "on", "disable", "off", "clickreward"));
                                        break;
                                    }
                                }
                            }
                            case "bounty":
                            case "bounties": {
                                switch (args[1].toLowerCase()) {
                                    case "broadcast": {
                                        suggestions.addAll(Arrays.asList("true", "false"));
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case 4: {
                        switch (args[0].toLowerCase()) {
                            case "naturalcauses":
                            case "nc":
                            case "ncauses":
                            case "naturalc": {
                                switch (args[1].toLowerCase()) {
                                    case "modifier":
                                    case "mod": {
                                        suggestions.addAll(Arrays.asList("killing", "mining", "farming", "fishing", "death"));
                                        return suggestions;
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case 5: {
                        switch (args[0].toLowerCase()) {
                            case "naturalcauses":
                            case "nc":
                            case "ncauses":
                            case "naturalc": {
                                switch (args[1].toLowerCase()) {
                                    case "modifier":
                                    case "mod": {
                                        switch (args[2].toLowerCase()) {
                                            case "add":
                                            case "create": {
                                                FileConfiguration config = NovaConfig.getConfig();

                                                switch (args[3].toLowerCase()) {
                                                    case "mining": return Arrays.stream(Material.values())
                                                            .filter(Material::isBlock)
                                                            .filter(m -> m != Material.AIR)
                                                            .map(m -> m.name().toLowerCase())
                                                            .collect(Collectors.toList());
                                                    case "farming": return Arrays.stream(Material.values())
                                                            .filter(w::isCrop)
                                                            .map(m -> m.name().toLowerCase())
                                                            .collect(Collectors.toList());
                                                    case "fishing": {
                                                        List<String> values = new ArrayList<>();
                                                        values.addAll(Arrays.stream(Material.values())
                                                                .filter(w::isItem)
                                                                .map(m -> m.name().toLowerCase())
                                                                .collect(Collectors.toList()));
                                                        values.addAll(Arrays.stream(EntityType.values())
                                                                .filter(EntityType::isAlive)
                                                                .filter(e -> LivingEntity.class.isAssignableFrom(e.getEntityClass()))
                                                                .map(e -> e.name().toLowerCase())
                                                                .collect(Collectors.toList()));

                                                        return values;
                                                    }
                                                    case "killing": return Arrays.stream(EntityType.values())
                                                            .filter(EntityType::isAlive)
                                                            .filter(e -> LivingEntity.class.isAssignableFrom(e.getEntityClass()))
                                                            .map(e -> e.name().toLowerCase())
                                                            .collect(Collectors.toList());
                                                    case "death": return Arrays.stream(EntityDamageEvent.DamageCause.values())
                                                            .map(d -> d.name().toLowerCase())
                                                            .collect(Collectors.toList());
                                                    default: return new ArrayList<>();
                                                }
                                            }
                                            case "view":
                                            case "remove":
                                            case "delete": {
                                                switch (args[3].toLowerCase()) {
                                                    case "mining": return modkeys.apply("NaturalCauses.Modifiers.Mining");
                                                    case "farming": return modkeys.apply("NaturalCauses.Modifiers.Farming");
                                                    case "fishing": return modkeys.apply("NaturalCauses.Modifiers.Fishing");
                                                    case "killing": return modkeys.apply("NaturalCauses.Modifiers.Killing");
                                                    case "death": return modkeys.apply("NaturalCauses.Modifiers.Death");
                                                    default: return new ArrayList<>();
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                }


                return suggestions;
            }
        }

        return suggestions;
    }

    @Override
    public void loadCommands() {
        for (String cmd : COMMANDS.keySet()) {
            List<String> aliases = COMMANDS.get(cmd);
            String desc = COMMAND_DESCRIPTION.get(cmd);
            String usage = COMMAND_USAGE.get(cmd);

            PluginCommand pcmd = createCommand(cmd, aliases.toArray(new String[0]));

            if (pcmd == null) {
                NovaConfig.getLogger().severe("Error loading command: " + cmd + " ; !! PLEASE REPORT !!");
                continue;
            }

            pcmd.setExecutor(this);
            pcmd.setUsage(usage);
            pcmd.setTabCompleter(this);
            pcmd.setDescription(desc);
            if (COMMAND_PERMISSION.get(cmd) != null) pcmd.setPermission(COMMAND_PERMISSION.get(cmd));

            register(pcmd);
        }
    }
}