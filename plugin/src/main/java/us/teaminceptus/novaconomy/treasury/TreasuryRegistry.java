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
import org.bukkit.plugin.Plugin;
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

    private final Novaconomy plugin;

    public TreasuryRegistry(Novaconomy plugin) {
        this.plugin = plugin;
        reloadTreasury();
    }

    public void reloadTreasury() {
        Plugin plugin = NovaConfig.getPlugin();
        ServiceRegistry r = ServiceRegistry.INSTANCE;
        if (r.serviceFor(EconomyProvider.class).isPresent() && !(r.serviceFor(EconomyProvider.class).get().get() instanceof TreasuryRegistry)) {
            plugin.getLogger().info("Other EconomyProvider Registration Found - Treasury will not be used");
            return;
        }

        if (r.serviceFor(EconomyProvider.class).get().get() instanceof TreasuryRegistry) r.unregister(EconomyProvider.class, this);
        r.registerService(EconomyProvider.class, this, plugin.getName(), ServicePriority.HIGH);
        plugin.getLogger().info("Injected Novaconomy EconomyProvider into Treasury");
    }

    @Override
    public @NotNull Set<OptionalEconomyApiFeature> getSupportedOptionalEconomyApiFeatures() {
        return Collections.singleton(OptionalEconomyApiFeature.TRANSACTION_EVENTS);
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
    public void retrievePlayerAccountIds(@NotNull EconomySubscriber<Collection<UUID>> subscription) {
        subscription.succeed(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
    }

    @Override
    public void hasAccount(@NotNull String identifier, @NotNull EconomySubscriber<Boolean> subscription) {
        subscription.succeed(getPlayer(identifier) != null);
    }

    @Override
    public void retrieveAccount(@NotNull String identifier, @NotNull EconomySubscriber<Account> subscription) {
        subscription.succeed(new TreasuryPlayerAccount(new NovaPlayer(getPlayer(identifier))));
    }

    @Override
    public void createAccount(@Nullable String name, @NotNull String identifier, @NotNull EconomySubscriber<Account> subscription) {
        retrieveAccount(identifier, subscription);
    }

    @Override
    public void retrieveAccountIds(@NotNull EconomySubscriber<Collection<String>> subscription) {
        subscription.succeed(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).collect(Collectors.toSet()));
    }

    @Override
    public void retrieveNonPlayerAccountIds(@NotNull EconomySubscriber<Collection<String>> subscription) {
        subscription.succeed(Collections.emptySet());
    }

    @Override
    public @NotNull Currency getPrimaryCurrency() {
        return null;
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
