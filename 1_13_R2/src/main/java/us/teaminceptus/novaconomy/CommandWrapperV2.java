package us.teaminceptus.novaconomy;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import revxrsal.commands.annotation.*;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.exception.CommandErrorException;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class CommandWrapperV2 implements CommandWrapper {

    private static BukkitCommandHandler handler;

    public CommandWrapperV2(Plugin plugin) {
        if (handler == null) {
            handler = BukkitCommandHandler.create(plugin);

            handler.registerValueResolver(Economy.class, ctx -> {
                Economy econ = Economy.getEconomy(ctx.popForParameter());
                if (econ == null) throw new CommandErrorException(getMessage("error.argument.economy"));
                return econ;
            });
            handler.getAutoCompleter().registerParameterSuggestions(Economy.class, SuggestionProvider.of(toStringList(Economy::getName, Economy.getEconomies())));

            handler.registerValueResolver(Material.class, ctx -> Material.matchMaterial(ctx.popForParameter()));
            handler.getAutoCompleter().registerParameterSuggestions(Material.class, SuggestionProvider.of(toStringList(m -> m.name().toLowerCase(), Material.values())));

            handler.registerValueResolver(Business.class, ctx -> {
                Business b = Business.getByName(ctx.popForParameter());
                if (b == null) throw new CommandErrorException(getMessage("error.argument.business"));
                return b;
            });
            handler.getAutoCompleter().registerParameterSuggestions(Business.class, SuggestionProvider.of(toStringList(Business::getName, Business.getBusinesses())));

            handler.getAutoCompleter().registerParameterSuggestions(boolean.class, SuggestionProvider.of("true", "false"));

            handler.register(this);
            new EconomyCommands(this);
            new BusinessCommands(this);

            handler.registerBrigadier();
        }
        plugin.getLogger().info("Loaded Command Version v2 (1.13.2+)");
    }

    private static String getMessage(String key) {
        return CommandWrapper.getMessage(key);
    }

    private static <T> List<String> toStringList(Function<T, String> func, Collection<T> elements) {
        List<String> list = new ArrayList<>();
        for (T element : elements) list.add(func.apply(element));

        return list;
    }

    private static @SafeVarargs <T> List<String> toStringList(Function<T, String> func, T... elements) {
        return toStringList(func, Arrays.asList(elements));
    }

    // Lamp Impl

    private boolean economyCount(Player p) {
        if (Economy.getEconomies().size() < 1) {
            p.sendMessage(getMessage("error.economy.none"));
            return false;
        }

        return true;
    }

    @Command({"ehelp", "nhelp", "novahelp", "econhelp", "economyhelp"})
    @Description("Economy help")
    @Usage("/ehelp")
    public void help(CommandSender sender) {
        CommandWrapper.super.help(sender);
    }

    @Command({"balance", "bal", "novabal"})
    @Description("Access your balances from all economies")
    @Usage("/balance")
    @CommandPermission("novaconomy.user.balance")
    public void balance(Player p) {
        if (economyCount(p)) CommandWrapper.super.balance(p);
    }

    @Command({"pay", "econpay", "novapay", "givemoney", "givebal"})
    @Description("Pay another user")
    @CommandPermission("novaconomy.user.pay")
    public void pay(Player p, @Named("target") Player target, @Named("economy") Economy econ, @Named("amount") @Range(min = 0.01) double amount) {
        CommandWrapper.super.pay(p, target, econ, amount);
    }

    @Command({"convert", "conv"})
    @Description("Convert one balance in an economy to another balance")
    @Usage("/convert <econ-from> <econ-to> <amount>")
    @CommandPermission("novaconomy.user.convert")
    public void convert(Player p, @Named("from-economy") Economy from, @Named("to-economy") Economy to, @Named("amount") @Range(min = 0.01) double amount) {
        CommandWrapper.super.convert(p, from, to, amount);
    }

    @Command({"novaconomyreload", "novareload", "nreload", "econreload"})
    @Usage("/novareload")
    @Description("Reload Novaconomy Configuration")
    @CommandPermission("novaconomy.admin.reloadconfig")
    public void reloadConfig(CommandSender sender) {
        CommandWrapper.super.reloadConfig(sender);
    }

    @Command({"business", "nbusiness"})
    @Description("Manage your Novaconomy Business")
    @Usage("/business <create|info|delete|edit|stock|query> <args...>")
    private static final class BusinessCommands {

        private final CommandWrapperV2 wrapper;

        private BusinessCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = CommandWrapperV2.handler;
            handler.register(this);
        }

        @Subcommand({"info", "information"})
        public void businessInfo(Player p) {
            wrapper.businessInfo(p);
        }

        @Subcommand("query")
        @CommandPermission("novaconomy.user.business.query")
        public void businessQuery(Player p, @Named("business") Business b) {
            wrapper.businessQuery(p, b);
        }

    }

    @Command({"economy", "econ", "novaecon", "novaconomy", "necon"})
    @Description("Manage economies or their balances")
    @Usage("/economy <create|delete|addbal|removebal|info> <args...>")
    private static final class EconomyCommands {

        private final CommandWrapperV2 wrapper;

        private EconomyCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = CommandWrapperV2.handler;

            handler.getAutoCompleter().registerSuggestion("symbol", SuggestionProvider.of("$", "%", "Q", "L", "P", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b"));
            handler.getAutoCompleter().registerSuggestion("interest", SuggestionProvider.of("enable", "disable"));

            handler.register(this);
        }

        @Subcommand({"create", "make"})
        @AutoComplete("* @symbol *")
        @CommandPermission("novaconomy.economy.create")
        public void createEconomy(CommandSender sender, @Named("name") String name, @Named("symbol") String symbol, @Named("icon") Material icon, @Named("scale") @Default("1") @Range(min = 0.01, max = Integer.MAX_VALUE) double scale, @Named("natural-increase") @Default("true") boolean naturalIncrease) {
            wrapper.createEconomy(sender, name, symbol.charAt(0), icon, scale, naturalIncrease);
        }

        @Subcommand({"remove", "delete", "removeeconomy", "deleteeconomy"})
        @CommandPermission("novaconomy.economy.delete")
        public void removeEconomy(CommandSender sender, @Named("economy") Economy econ) {
            wrapper.removeEconomy(sender, econ);
        }

        @Subcommand("interest")
        @AutoComplete("@interest")
        @CommandPermission("novaconomy.economy")
        public void interest(CommandSender sender, @Default("enable") String interest) {
            wrapper.interest(sender, interest.equalsIgnoreCase("enable") || interest.equalsIgnoreCase("true"));
        }

        @Subcommand({"setbalance", "setbal"})
        @CommandPermission("novaconomy.economy.setbalance")
        public void setBalance(CommandSender sender, @Named("economy") Economy econ, @Named("target") Player target, @Named("amount") @Range(min = 0) double amount) {
            wrapper.setBalance(sender, econ, target, amount);
        }

        @Subcommand({"addbalance", "addbal"})
        @CommandPermission("novaconomy.economy.addbalance")
        public void addBalance(CommandSender sender, @Named("economy") Economy econ, @Named("target") Player target, @Named("amount") @Range(min = 0) double add) {
            wrapper.addBalance(sender, econ, target, add);
        }

        @Subcommand({"removebalance", "removebal"})
        @CommandPermission("novaconomy.economy.removebalance")
        public void removeBalance(CommandSender sender, @Named("economy") Economy econ, @Named("target") Player target, @Named("amount") @Range(min = 0) double remove) {
            wrapper.removeBalance(sender, econ, target, remove);
        }

        @Subcommand({"check", "createcheck"})
        @CommandPermission("novaconomy.economy.check")
        public void createCheck(Player p, @Named("economy") Economy econ, @Named("amount") @Range(min = 1) double amount) {
            wrapper.createCheck(p, econ, amount);
        }

        @Subcommand("info")
        @CommandPermission("novaconomy.economy.info")
        public void info(CommandSender sender, @Named("economy") Economy econ) {
            wrapper.economyInfo(sender, econ);
        }

    }
}
