package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.common.NamespacedKey;
import me.lokka30.treasury.api.common.misc.TriState;
import me.lokka30.treasury.api.common.service.ServicePriority;
import me.lokka30.treasury.api.common.service.ServiceRegistry;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.AccountData;
import me.lokka30.treasury.api.economy.account.accessor.AccountAccessor;
import me.lokka30.treasury.api.economy.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main Treasury Registry class for injecting
 */
public final class TreasuryRegistry implements EconomyProvider {

    static final String TREASURY_ACCOUNTS = "TreasuryAccounts";
    // private final Novaconomy plugin;

    public TreasuryRegistry(Novaconomy plugin) {
        // this.plugin = plugin;

        ConfigurationSerialization.registerClass(TreasuryAccount.class);

        if (!NovaConfig.getGlobalFile().exists()) try { NovaConfig.getGlobalFile().createNewFile(); } catch (IOException e) { NovaConfig.print(e); }
        TreasuryAccount.global = NovaConfig.getGlobalStorage();
        TreasuryAccount.treasuryAccounts = TreasuryAccount.global.isConfigurationSection(TREASURY_ACCOUNTS) ? TreasuryAccount.global.getConfigurationSection(TREASURY_ACCOUNTS) : TreasuryAccount.global.createSection(TREASURY_ACCOUNTS);

        ServiceRegistry r = ServiceRegistry.INSTANCE;
        if (!r.serviceFor(EconomyProvider.class).isPresent()) {
            r.registerService(EconomyProvider.class, this, plugin.getName(), ServicePriority.HIGH);
            plugin.getLogger().info("Injected Novaconomy EconomyProvider into Treasury");
        }
    }

    @Override
    public CompletableFuture<Collection<UUID>> retrievePlayerAccountIds() {
        return CompletableFuture.completedFuture(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getUniqueId).collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(@NotNull AccountData data) {
        if (data.isPlayerAccount()) return CompletableFuture.completedFuture(true);
        return CompletableFuture.completedFuture(TreasuryAccount.getAccounts().stream().map(TreasuryAccount::identifier).collect(Collectors.toSet()).contains(data.getNonPlayerIdentifier().orElse(null)));
    }

    @Override
    public @NotNull AccountAccessor accountAccessor() {
        return TreasuryAccountAccessor.INSTANCE;
    }

    @Override
    public @NotNull CompletableFuture<Collection<NamespacedKey>> retrieveNonPlayerAccountIds() {
        return CompletableFuture.completedFuture(TreasuryAccount.getAccounts().stream().map(TreasuryAccount::identifier).collect(Collectors.toSet()));
    }

    @Override
    public @NotNull Currency getPrimaryCurrency() {
        Object o = NovaConfig.loadFunctionalityFile().get("VaultEconomy", -1);
        if (o instanceof String) {
            String s = (String) o;
            return new TreasuryCurrency(Economy.getEconomy(s));
        } else {
            Optional<Economy> first = Economy.getEconomies()
                    .stream()
                    .min(Comparator.comparing(Economy::getName));

            if (!first.isPresent()) throw new RuntimeException("No economies created");
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
    public CompletableFuture<TriState> registerCurrency(@NotNull Currency currency) {
        try {
            Economy newE = Economy.builder()
                    .setName(currency.getIdentifier())
                    .setSymbol(currency.getSymbol().charAt(0))
                    .build();

            return CompletableFuture.completedFuture(TriState.fromBoolean(newE != null));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(TriState.FALSE);
        }
    }

    @Override
    public @NotNull CompletableFuture<TriState> unregisterCurrency(@NotNull Currency currency) {
        if (!Economy.exists(currency.getIdentifier())) return CompletableFuture.completedFuture(TriState.FALSE);
        Economy.removeEconomy(currency.getIdentifier());
        return CompletableFuture.completedFuture(TriState.TRUE);
    }
}
