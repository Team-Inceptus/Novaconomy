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
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class CommandWrapperV2 implements CommandWrapper {

    private static BukkitCommandHandler handler;

    public CommandWrapperV2(Plugin plugin) {
        if (handler == null) handler = BukkitCommandHandler.create(plugin);

        handler.registerValueResolver(Economy.class, ctx -> {
            Economy econ = Economy.getEconomy(ctx.popForParameter());
            if (econ == null) throw new CommandErrorException(getMessage("error.argument.economy"));
            return econ;
        });
        handler.getAutoCompleter().registerParameterSuggestions(Economy.class, SuggestionProvider.of(toStringList(Economy::getName, Economy.getEconomies())));

        handler.registerValueResolver(char.class, ctx -> ctx.popForParameter().charAt(0));

        handler.getAutoCompleter().registerParameterSuggestions(boolean.class, SuggestionProvider.of("true", "false"));

        handler.register(this);
        new EconomyCommands(this);

//        handler.registerBrigadier();
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
    public void pay(Player p, Player target, Economy econ, @Range(min = 1) double amount) {
        CommandWrapper.super.pay(p, target, econ, amount);
    }

    @Command({"convert", "conv"})
    @Description("Convert one balance in an economy to another balance")
    @Usage("/convert <econ-from> <econ-to> <amount>")
    @CommandPermission("novaconomy.user.convert")
    public void convert(Player p, Economy from, Economy to, @Range(min = 1) double amount) {
        CommandWrapper.super.convert(p, from, to, amount);
    }

    @Command({"economy", "econ", "novaecon", "novaconomy", "necon"})
    @Description("Manage economies or their balances")
    @Usage("/economy <create|delete|addbal|removebal|info> <args...>")
    private static final class EconomyCommands {

        private final CommandWrapperV2 wrapper;

        private EconomyCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = wrapper.handler;

            handler.registerValueResolver(Material.class, ctx -> Material.matchMaterial(ctx.popForParameter()));
            handler.getAutoCompleter().registerParameterSuggestions(Material.class, SuggestionProvider.of(toStringList(m -> "minecraft:" + m.name().toLowerCase(), Material.values())));

            handler.getAutoCompleter().registerSuggestion("symbol", SuggestionProvider.of("$", "%", "Q", "L", "P", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b"));
            handler.getAutoCompleter().registerSuggestion("interest", SuggestionProvider.of("enable", "disable"));

            handler.register(this);
        }

        @Subcommand({"create", "make"})
        @AutoComplete("* @symbol *")
        @CommandPermission("novaconomy.economy.create")
        public void createEconomy(CommandSender sender, String name, char symbol, Material icon, @Range(min = 0.01) double scale, @Default("true") boolean naturalIncrease) {
            wrapper.createEconomy(sender, name, symbol, icon, scale, naturalIncrease);
        }

        @Subcommand({"remove", "delete", "removeeconomy", "deleteeconomy"})
        @CommandPermission("novaconomy.economy.delete")
        public void removeEconomy(CommandSender sender, Economy econ) {
            wrapper.removeEconomy(sender, econ);
        }

        @Subcommand("interest")
        @AutoComplete("@interest")
        @CommandPermission("novaconomy.economy")
        public void interest(CommandSender sender, @Default("enable") String interest) {
            wrapper.interest(sender, interest.equalsIgnoreCase("enable"));
        }

    }
}
