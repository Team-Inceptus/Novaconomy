package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.transaction.EconomyTransaction;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

final class TreasuryPlayerAccount implements PlayerAccount {

    private final NovaPlayer np;

    TreasuryPlayerAccount(NovaPlayer p) {
        this.np = p;
        treasuryTrans.put(p.getPlayer().getUniqueId(), new ArrayList<>());
    }

    private static final Map<UUID, List<EconomyTransaction>> treasuryTrans = new HashMap<>();

    @Override
    public @NotNull UUID identifier() {
        return np.getPlayer().getUniqueId();
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(np.getPlayerName());
    }

    @Override
    public CompletableFuture<BigDecimal> retrieveBalance(@NotNull Currency c) {
        return CompletableFuture.completedFuture(BigDecimal.valueOf(np.getBalance(TreasuryCurrency.getEconomy(c))));
    }

    @Override
    public CompletableFuture<BigDecimal> doTransaction(@NotNull EconomyTransaction trans) {
        Economy econ = Economy.getEconomy(trans.getCurrencyId());
        np.add(econ, trans.getAmount().doubleValue());
        treasuryTrans.get(this.np.getPlayer().getUniqueId()).add(trans);

        return CompletableFuture.completedFuture(BigDecimal.valueOf(np.getBalance(econ)));
    }

    @Override
    public CompletableFuture<Boolean> deleteAccount() {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Collection<String>> retrieveHeldCurrencies() {
        return CompletableFuture.completedFuture(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Collection<EconomyTransaction>> retrieveTransactionHistory(int transactionCount, @NotNull Temporal from, @NotNull Temporal to) {
        List<EconomyTransaction> l = treasuryTrans.get(this.np.getPlayer().getUniqueId());
        return CompletableFuture.completedFuture(l.subList(0, transactionCount >= l.size() ? l.size() - 1 : transactionCount)
                .stream()
                .filter(t -> t.getTimestamp().isAfter(Instant.from(from)) && t.getTimestamp().isBefore(Instant.from(to)))
                .collect(Collectors.toSet())
        );
    }
}
