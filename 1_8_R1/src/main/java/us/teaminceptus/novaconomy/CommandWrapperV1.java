package us.teaminceptus.novaconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandWrapperV1 implements CommandWrapper, TabExecutor {

    private final Plugin plugin;

    public CommandWrapperV1(Plugin plugin) {
        this.plugin = plugin;
        loadCommands();
        plugin.getLogger().info("Loaded Command Version v1 (1.8 - 1.13)");
    }

    private PluginCommand createCommand(String name, String... aliases) {
        try {
            Constructor<PluginCommand> p = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            p.setAccessible(true);

            PluginCommand cmd = p.newInstance(name, plugin);
            if (aliases != null && aliases.length > 0) cmd.setAliases(Arrays.asList(aliases));
            return cmd;
        } catch (Exception e) {
            NovaConfig.getLogger().severe(e.getMessage());
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
            NovaConfig.getLogger().severe(e.getMessage());
        }
    }

    private boolean economyCount(CommandSender sender) {
        if (Economy.getEconomies().size() < 1) {
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
            case "overridelanguages": {
                loadLanguages(sender);
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

                try {
                    if (args.length < 1) {
                        p.sendMessage(getMessage("error.argument.player"));
                        return false;
                    }

                    if (Bukkit.getPlayer(args[0]) == null) {
                        p.sendMessage(getMessage("error.player.offline"));
                        return false;
                    }

                    Player target = Bukkit.getPlayer(args[0]);

                    if (args.length < 2) {
                        p.sendMessage(getMessage("error.argument.economy"));
                        return false;
                    }

                    if (!Economy.exists(args[1])) {
                        p.sendMessage(getMessage("error.argument.economy"));
                        return false;
                    }

                    Economy econ = Economy.getEconomy(args[1]);

                    if (args.length < 3) {
                        p.sendMessage(getMessage("error.argument.pay_amount"));
                        return false;
                    }

                    double amount = Double.parseDouble(args[2]);

                    pay(p, target, econ, amount);
                } catch (NumberFormatException e) {
                    p.sendMessage(getMessage("error.argument.pay_amount"));
                    return false;
                }

                break;
            }
            case "economy": {
                if (args.length < 1) {
                    sender.sendMessage(getMessage("error.argument"));
                    return false;
                }

                if (!economyCount(sender)) return false;

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
                            double scale = Double.parseDouble(args[4]);
                            boolean naturalIncrease = true;

                            if (!(args.length < 6)) {
                                if (!(args[5].equalsIgnoreCase("true")) && !(args[5].equalsIgnoreCase("false"))) {
                                    sender.sendMessage(getMessage("error.argument.bool"));
                                    return false;
                                }

                                naturalIncrease = Boolean.parseBoolean(args[5]);
                            }

                            createEconomy(sender, name, symbol, icon, scale, naturalIncrease);
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
                                sender.sendMessage(getMessage("error.player.offline"));
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

                            createCheck(p, econ, amount);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("error.argument.amount"));
                            return false;
                        }

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
                }
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
            case "ehelp": {
                suggestions.addAll(plugin.getDescription().getCommands().keySet());
                return suggestions;
            }
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

                        if (sender.hasPermission("novaconomy.economy")) suggestions.addAll(Arrays.asList("check", "createcheck", "create", "delete", "addbal", "setbalance", "removebal", "addbalance", "removebalance", "setbal"));
                        return suggestions;
                    }
                    case 2: {
                        if (!(args[0].equalsIgnoreCase("create")))
                            for (Economy econ : Economy.getEconomies()) suggestions.add(econ.getName());

                        return suggestions;
                    }
                    case 3: {
                        if (args[0].toLowerCase().contains("bal"))
                            for (Player p : Bukkit.getOnlinePlayers()) suggestions.add(p.getName());
                        else if (args[0].equalsIgnoreCase("create"))
                            suggestions.addAll(Arrays.asList("$", "%", "Q", "L", "P", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b"));


                        return suggestions;
                    }
                    case 4: {
                        if (args[0].equalsIgnoreCase("create"))
                            for (Material m : Material.values()) suggestions.add("minecraft:" + m.name().toLowerCase());

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
                        suggestions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        return suggestions;

                    case 2:
                        suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toList()));
                        return suggestions;
                }

                return suggestions;
            }
            case "business": {
                switch (args.length) {
                    case 1:
                        suggestions.addAll(Arrays.asList("info", "information", "query", "create", "addproduct", "addp", "removeproduct", "removep",
                                "addresource", "stock", "addr", "addstock"));
                        return suggestions;

                    case 2:
                        if (args[0].equalsIgnoreCase("query")) for (Business b : Business.getBusinesses()) suggestions.add(b.getName());
                        return suggestions;
                    case 3:
                        if (args[0].equalsIgnoreCase("create")) for (Material m : Material.values()) suggestions.add(m.name().toLowerCase());
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
                            suggestions.addAll(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toList()));
                }

                return suggestions;
            }
            default: return suggestions;
        }
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
