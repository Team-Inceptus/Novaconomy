package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.common.response.FailureReason;
import me.lokka30.treasury.api.common.service.ServicePriority;
import me.lokka30.treasury.api.common.service.ServiceRegistry;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.Account;
import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.misc.OptionalEconomyApiFeature;
import me.lokka30.treasury.api.economy.response.EconomyException;
import me.lokka30.treasury.api.economy.response.EconomySubscriber;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.*;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.Novaconomy.getPlayer;

/**
 * Main Treasury Registry class for injecting
 */
public final class TreasuryRegistry implements EconomyProvider {

    static final String TREASURY_ACCOUNTS = "TreasuryAccounts";
    private final Novaconomy plugin;

    public TreasuryRegistry(Novaconomy plugin) {
        this.plugin = plugin;

        ConfigurationSerialization.registerClass(TreasuryAccount.class);
        TreasuryAccount.global = NovaConfig.getGlobalStorage();
        TreasuryAccount.treasuryAccounts = TreasuryAccount.global.isConfigurationSection(TREASURY_ACCOUNTS) ? TreasuryAccount.global.getConfigurationSection(TREASURY_ACCOUNTS) : TreasuryAccount.global.createSection(TREASURY_ACCOUNTS);

        ServiceRegistry r = ServiceRegistry.INSTANCE;
        if (!r.serviceFor(EconomyProvider.class).isPresent()) {
            r.registerService(EconomyProvider.class, this, plugin.getName(), ServicePriority.HIGH);
            plugin.getLogger().info("Injected Novaconomy EconomyProvider into Treasury");
        }
    }

    @Override
    public @NotNull Set<OptionalEconomyApiFeature> getSupportedOptionalEconomyApiFeatures() {
        return Collections.emptySet();
    }

    @Override
    public void hasPlayerAccount(@NotNull UUID accountId, @NotNull EconomySubscriber<Boolean> subscription) {
        subscription.succeed(true);
    }

    @Override
    public void retrievePlayerAccount(@NotNull UUID accountId, @NotNull EconomySubscriber<PlayerAccount> subscription) {
        subscription.succeed(new TreasuryPlayerAccount(new NovaPlayer(Bukkit.getOfflinePlayer(accountId))));
    }

    @Override
    public void createPlayerAccount(@NotNull UUID accountId, @NotNull EconomySubscriber<PlayerAccount> subscription) {
        retrievePlayerAccount(accountId, subscription);
    }

    @Override
    public void retrievePlayerAccountIds(@NotNull EconomySubscriber<Collection<UUID>> sub) {
        sub.succeed(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
    }

    @Override
    public void hasAccount(@NotNull String id, @NotNull EconomySubscriber<Boolean> sub) {
        sub.succeed(TreasuryAccount.getAccounts().stream().map(TreasuryAccount::getIdentifier).collect(Collectors.toSet()).contains(id));
    }

    @Override
    public void retrieveAccount(@NotNull String id, @NotNull EconomySubscriber<Account> sub) {
        if (getPlayer(id) != null) sub.succeed(new TreasuryPlayerAccount(new NovaPlayer(getPlayer(id))));
        else sub.succeed(new TreasuryAccount(id));
    }

    @Override
    public void createAccount(@Nullable String name, @NotNull String id, @NotNull EconomySubscriber<Account> sub) {
        if (getPlayer(id) != null) retrieveAccount(id, sub);
        else sub.succeed(new TreasuryAccount(id, name));
    }

    @Override
    public void retrieveAccountIds(@NotNull EconomySubscriber<Collection<String>> sub) {
        Set<String> playerIds = Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).collect(Collectors.toSet());
        Set<String> nonPlayerIds = TreasuryAccount.getAccounts().stream().map(TreasuryAccount::getIdentifier).collect(Collectors.toSet());

        Set<String> all = new HashSet<>(playerIds);
        all.addAll(nonPlayerIds);
        sub.succeed(all);
    }

    @Override
    public void retrieveNonPlayerAccountIds(@NotNull EconomySubscriber<Collection<String>> sub) {
        sub.succeed(TreasuryAccount.getAccounts().stream().map(TreasuryAccount::getIdentifier).collect(Collectors.toSet()));
    }

    @Override
    public @NotNull Currency getPrimaryCurrency() {
        Object o = NovaConfig.loadFunctionalityFile().get("VaultEconomy", -1);
        if (o instanceof String) {
            String s = (String) o;
            return new TreasuryCurrency(Economy.getEconomy(s));
        } else {
            Optional<Economy> first = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).findFirst();
            if (!first.isPresent()) throw new RuntimeException(new EconomyException(FailureReason.of("No economies creatded")));
            return new TreasuryCurrency(first.get());
        }
    }

    @Override
    public Optional<Currency> findCurrency(@NotNull String identifier) {
        return Optional.of(new TreasuryCurrency(Economy.getEconomy(identifier)));
    }

    @Override
    public Set<Currency> getCurrencies() {
        return Economy.getEconomies().stream().map(TreasuryCurrency::new).collect(Collectors.toSet());
    }

    @Override
    public void registerCurrency(@NotNull Currency currency, @NotNull EconomySubscriber<Boolean> subscription) {
        try {
            Economy newE = Economy.builder()
                    .setName(currency.getDisplayNameSingular())
                    .setSymbol(currency.getSymbol().charAt(0))
                    .build();

            subscription.succeed(newE != null);
        } catch (Exception e) {
            subscription.fail(new EconomyException(FailureReason.of(e.getMessage())));
        }
    }
}
