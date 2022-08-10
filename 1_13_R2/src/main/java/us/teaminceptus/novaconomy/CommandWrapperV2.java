package us.teaminceptus.novaconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import revxrsal.commands.annotation.*;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.exception.CommandErrorException;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
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

            handler.getAutoCompleter().registerParameterSuggestions(Economy.class, SuggestionProvider.of(() -> toStringList(Economy::getName, Economy.getEconomies())));

            handler.registerValueResolver(Material.class, ctx -> Material.matchMaterial(ctx.popForParameter()));
            handler.getAutoCompleter().registerParameterSuggestions(Material.class, SuggestionProvider.of(() -> toStringList(m -> m.name().toLowerCase(), Material.values())));

            handler.registerValueResolver(Business.class, ctx -> {
                Business b = Business.getByName(ctx.popForParameter());
                if (b == null) throw new CommandErrorException(getMessage("error.argument.business"));
                return b;
            });
            handler.getAutoCompleter().registerParameterSuggestions(Business.class, SuggestionProvider.of(() -> toStringList(Business::getName, Business.getBusinesses())));

            handler.getAutoCompleter().registerParameterSuggestions(boolean.class, SuggestionProvider.of("true", "false"));

            handler.registerValueResolver(OfflinePlayer.class, ctx -> {
                OfflinePlayer p = Wrapper.getPlayer(ctx.popForParameter());
                if (p == null) throw new CommandErrorException(getMessage("error.argument.player"));
                return p;
            });

            handler.getAutoCompleter().registerParameterSuggestions(OfflinePlayer.class, SuggestionProvider.of(() -> toStringList(OfflinePlayer::getName, Bukkit.getOfflinePlayers())));

            handler.getAutoCompleter().registerSuggestion("event", SuggestionProvider.of(() -> toStringList(NovaConfig.CustomTaxEvent::getIdentifier, NovaConfig.getConfiguration().getAllCustomEvents())));
            handler.getAutoCompleter().registerSuggestion("settings", SuggestionProvider.of(Arrays.asList("business", "personal")));

            handler.register(this);
            new EconomyCommands(this);
            new BusinessCommands(this);
            new BankCommands(this);
            new BountyCommands(this);

            handler.registerBrigadier();
            handler.setLocale(Language.getCurrentLanguage().getLocale());
            plugin.getLogger().info("Loaded Command Version v2 (1.13.2+)");
        }
    }

    private static String getMessage(String key) {
        return CommandWrapper.getMessage(key);
    }

    private static <T> List<String> toStringList(Function<T, String> func, Collection<T> elements) {
        List<String> list = new ArrayList<>();
        for (T element : elements) list.add(func.apply(element));

        return list;
    }

    @SafeVarargs
    private static <T> List<String> toStringList(Function<T, String> func, T... elements) {
        return toStringList(func, Arrays.asList(elements));
    }

    // Lamp Impl

    private boolean economyCount(Player p) {
        if (Economy.getEconomies().isEmpty()) {
            p.sendMessage(getMessage("error.economy.none"));
            return false;
        }

        return true;
    }

    @Override
    @Command({"ehelp", "nhelp", "novahelp", "econhelp", "economyhelp"})
    @Description("Economy help")
    @Usage("/ehelp")
    public void help(CommandSender sender) {
        CommandWrapper.super.help(sender);
    }

    @Override
    @Command({"balance", "bal", "novabal"})
    @Description("Access your balances from all economies")
    @Usage("/balance")
    @CommandPermission("novaconomy.user.balance")
    public void balance(Player p) {
        if (economyCount(p)) CommandWrapper.super.balance(p);
    }

    @Override
    @Command({"pay", "econpay", "novapay", "givemoney", "givebal"})
    @Description("Pay another user")
    @CommandPermission("novaconomy.user.pay")
    public void pay(Player p, Player target, Economy economy, @Range(min = 0.01) double amount) { CommandWrapper.super.pay(p, target, economy, amount); }

    @Override
    @Command({"convert", "conv"})
    @Description("Convert one balance in an economy to another balance")
    @Usage("/convert <econ-from> <econ-to> <amount>")
    @CommandPermission("novaconomy.user.convert")
    public void convert(Player p, @Named("from-economy") Economy from, @Named("to-economy") Economy to, @Range(min = 0.01) double amount) { CommandWrapper.super.convert(p, from, to, amount); }

    @Override
    @Command({"novaconomyreload", "novareload", "nreload", "econreload"})
    @Usage("/novareload")
    @Description("Reload Novaconomy Configuration")
    @CommandPermission("novaconomy.admin.reloadconfig")
    public void reloadConfig(CommandSender sender) { CommandWrapper.super.reloadConfig(sender); }

    @Command({"createcheck", "check", "ncheck", "nc", "novacheck"})
    @Usage("/createcheck <economy> <amount>")
    @Description("Create a Novaconomy Check redeemable for a certain amount of money")
    @CommandPermission("novaconomy.user.check")
    public void createCheck(Player p, @Range(min = 1) double amount, Economy econ) { CommandWrapper.super.createCheck(p, econ, amount, true); }

    @Override
    @Command({"exchange", "convertgui", "convgui", "exch"})
    @Usage("/exchange <amount>")
    @Description("Convert one balance in an economy to another balance (with a GUI)")
    @CommandPermission("novaconomy.user.convert")
    public void exchange(Player p, @Range(min = 0.01) double amount) { CommandWrapper.super.exchange(p, amount); }

    @Override
    @Command({"balanceleaderboard", "bleaderboard", "nleaderboard", "bl", "nl", "novaleaderboard", "balboard", "novaboard"})
    @Usage("/balanceleaderboard [<economy>]")
    @Description("View the top 15 balances of all or certain economies")
    @CommandPermission("novaconomy.user.leaderboard")
    public void balanceLeaderboard(Player p, @Optional Economy econ) { CommandWrapper.super.balanceLeaderboard(p, econ); }

    @Override
    @Command({"settings", "novasettings", "nsettings"})
    @Usage("/settings [<business|personal>]")
    @Description("Manage your Novaconomy Settings")
    @AutoComplete("@settings")
    public void settings(Player p, @Optional String section) { CommandWrapper.super.settings(p, section); }

    @Override
    @Command({"taxevent", "customtax"})
    @Usage("/taxevent <event> [<self>]")
    @Description("Call a Custom Tax Event from the configuration")
    @CommandPermission("novaconomy.admin.tax_event")
    @AutoComplete("@event *")
    public void callEvent(CommandSender sender, String event, @Default("true") boolean self) { CommandWrapper.super.callEvent(sender, event, self); }

    @Override
    @Command({"rate", "novarate", "nrate", "ratebusiness"})
    @Usage("/rate <business> [<comment>]")
    @Description("Rate a business")
    @CommandPermission("novaconomy.user.rate")
    public void rate(Player p, Business business, @Default("") String comment) { CommandWrapper.super.rate(p, business, comment); }

    @Command({"business", "nbusiness"})
    @Description("Manage your Novaconomy Business")
    @Usage("/business <create|info|delete|addproduct|stock|query|...> <args...>")
    private static final class BusinessCommands {

        private final CommandWrapperV2 wrapper;

        private BusinessCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = CommandWrapperV2.handler;
            handler.register(this);
        }

        @Subcommand({"info", "information"})
        public void businessInfo(Player p) { wrapper.businessInfo(p); }

        @Subcommand("query")
        @CommandPermission("novaconomy.user.business.query")
        public void businessQuery(Player p, Business business) { wrapper.businessQuery(p, business); }

        @Subcommand("create")
        @CommandPermission("novaconomy.user.business.create")
        public void businessCreate(Player p, String name, Material icon) { wrapper.createBusiness(p, name, icon);}

        @Subcommand({"addproduct", "addp"})
        public void addProduct(Player p, @Range(min = 0.01) double price) { wrapper.addProduct(p, price); }

        @Subcommand({"addresource", "stock", "addr", "addstock"})
        @CommandPermission("novaconomy.user.business.resources")
        public void addResource(Player p) { wrapper.addResource(p); }

        @Subcommand({"removeproduct", "removep"})
        public void removeProduct(Player p) { wrapper.removeProduct(p); }

        @Subcommand("home")
        @CommandPermission("novaconomy.user.business.home")
        public void home(Player p) { wrapper.businessHome(p, false); }

        @Subcommand("sethome")
        @CommandPermission("novaconomy.user.business.home")
        public void setHome(Player p) { wrapper.businessHome(p, true); }

        @Subcommand("delete")
        public void deleteBusiness(Player p, @Default String confirm) { wrapper.deleteBusiness(p, confirm.equalsIgnoreCase("confirm")); }

        @Subcommand({"statistics", "stats"})
        public void statistics(Player p) { wrapper.statistics(p, Business.getByOwner(p));}

        @Subcommand({"settings", "setting"})
        public void settings(Player p) { wrapper.settings(p, "business"); }

        @Subcommand("rating")
        public void businessRating(Player p, OfflinePlayer target) { wrapper.businessRating(p, target); }

        @Subcommand("discover")
        public void discoverBusiness(Player p) { wrapper.discoverBusinesses(p); }

        @Subcommand("remove")
        public void removeBusiness(CommandSender sender, Business b, @Default String confirm) { wrapper.removeBusiness(sender, b, confirm.equalsIgnoreCase("confirm"));}

        @Subcommand({"editprice", "price"})
        public void editPrice(Player p, @Range(min = 0.01) double newPrice, @Optional Economy economy) { wrapper.editPrice(p, newPrice, economy); }
    }

    @Command({"nbank", "bank", "globalbank", "gbank"})
    @Description("Interact with the Global Novaconomy Bank")
    @Usage("/bank <info|deposit|withdraw|transfer|leaderboard|...> <args...>")
    private static final class BankCommands {
        private final CommandWrapperV2 wrapper;

        private BankCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = CommandWrapperV2.handler;
            handler.register(this);
        }

        @Subcommand({"info", "information", "balances", "balance"})
        @CommandPermission("novaconomy.user.bank.info")
        public void bankBalances(Player p) { wrapper.bankBalances(p); }

        @Subcommand("deposit")
        @CommandPermission("novaconomy.user.bank.deposit")
        public void bankDeposit(Player p, @Range(min = 0.01) double amount, Economy econ) { wrapper.bankDeposit(p, amount, econ);}

        @Subcommand("withdraw")
        @CommandPermission("novaconomy.user.bank.withdraw")
        public void bankWithdraw(Player p, @Range(min = 0.01) double amount, Economy econ) { wrapper.bankWithdraw(p, amount, econ);}
    }

    @Command({"economy", "econ", "novaecon", "novaconomy", "necon"})
    @Description("Manage economies or their balances")
    @Usage("/economy <create|delete|addbal|removebal|info> <args...>")
    private static final class EconomyCommands {

        private final CommandWrapperV2 wrapper;

        private EconomyCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = CommandWrapperV2.handler;

            handler.getAutoCompleter().registerSuggestion("symbol", SuggestionProvider.of("'$'", "'%'", "Q", "L", "P", "A", "a", "r", "R", "C", "c", "D", "d", "W", "w", "B", "b"));
            handler.getAutoCompleter().registerSuggestion("interest", SuggestionProvider.of("enable", "disable"));

            handler.register(this);
        }

        @Subcommand({"create", "make"})
        @AutoComplete("* @symbol *")
        @CommandPermission("novaconomy.economy.create")
        public void createEconomy(CommandSender sender, String name, String symbol, Material icon, @Default("1") @Range(min = 0.01, max = Integer.MAX_VALUE) double scale, @Named("natural-increase") @Default("true") boolean naturalIncrease) {
            wrapper.createEconomy(sender, name, symbol.contains("\"") || symbol.contains("'") ? symbol.charAt(1) : symbol.charAt(0), icon, scale, naturalIncrease);
        }

        @Subcommand({"remove", "delete", "removeeconomy", "deleteeconomy"})
        @CommandPermission("novaconomy.economy.delete")
        public void removeEconomy(CommandSender sender, Economy economy) {
            wrapper.removeEconomy(sender, economy);
        }

        @Subcommand("interest")
        @AutoComplete("@interest")
        @CommandPermission("novaconomy.economy.interest")
        public void interest(CommandSender sender, @Default("enable") String interest) {
            wrapper.interest(sender, interest.equalsIgnoreCase("enable") || interest.equalsIgnoreCase("true"));
        }

        @Subcommand({"setbalance", "setbal"})
        @CommandPermission("novaconomy.economy.setbalance")
        public void setBalance(CommandSender sender, Economy economy, Player target, @Range(min = 0) double amount) {
            wrapper.setBalance(sender, economy, target, amount);
        }

        @Subcommand({"addbalance", "addbal"})
        @CommandPermission("novaconomy.economy.addbalance")
        public void addBalance(CommandSender sender, Economy economy, Player target, @Range(min = 0) double amount) {
            wrapper.addBalance(sender, economy, target, amount);
        }

        @Subcommand({"removebalance", "removebal"})
        @CommandPermission("novaconomy.economy.removebalance")
        public void removeBalance(CommandSender sender, Economy economy, Player target, @Range(min = 0) double amount) {
            wrapper.removeBalance(sender, economy, target, amount);
        }

        @Subcommand({"check", "createcheck"})
        @CommandPermission("novaconomy.economy.check")
        public void createCheck(Player p, Economy economy, @Named("amount") @Range(min = 1) double amount) { wrapper.createCheck(p, economy, amount, false); }

        @Subcommand("info")
        @CommandPermission("novaconomy.economy.info")
        public void info(CommandSender sender, Economy economy) {
            wrapper.economyInfo(sender, economy);
        }
    }

    @Command({"bounty", "novabounty", "nbounty"})
    @Description("Manage your Novaconomy Bounties")
    @Usage("/bounty <owned|create|delete|self> <args...>")
    private static final class BountyCommands {

        private final CommandWrapperV2 wrapper;

        BountyCommands(CommandWrapperV2 wrapper) {
            this.wrapper = wrapper;

            BukkitCommandHandler handler = CommandWrapperV2.handler;
            handler.register(this);
        }

        @Subcommand({"create", "add"})
        @CommandPermission("novaconomy.user.bounty.manage")
        public void createBounty(Player p, OfflinePlayer target, Economy economy, @Range(min = 0.01) double amount) { wrapper.createBounty(p, target, economy, amount); }

        @Subcommand({"delete", "remove"})
        @CommandPermission("novaconomy.user.bounty.manage")
        public void removeBounty(Player p, OfflinePlayer target) { wrapper.deleteBounty(p, target); }

        @Subcommand("owned")
        @CommandPermission("novaconomy.user.bounty.list")
        public void listOwnedBounties(Player p) { wrapper.listBounties(p, true); }

        @Subcommand("self")
        @CommandPermission("novaconomy.user.bounty.list")
        public void listSelfBounties(Player p) { wrapper.listBounties(p, false); }

    }
}
