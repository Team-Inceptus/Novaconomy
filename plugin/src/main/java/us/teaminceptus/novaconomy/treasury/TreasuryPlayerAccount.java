package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.response.EconomySubscriber;
import me.lokka30.treasury.api.economy.transaction.EconomyTransaction;
import me.lokka30.treasury.api.economy.transaction.EconomyTransactionInitiator;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

class TreasuryPlayerAccount implements PlayerAccount {

    private final NovaPlayer np;

    TreasuryPlayerAccount(NovaPlayer p) {
        this.np = p;
        treasuryTrans.put(p.getPlayer().getUniqueId(), new ArrayList<>());
    }

    private Map<UUID, List<EconomyTransaction>> treasuryTrans = new HashMap<>();

    @Override
    public @NotNull UUID getUniqueId() {
        return null;
    }

    @Override
    public Optional<String> getName() {
        return Optional.of(np.getPlayer().getName());
    }

    @Override
    public void retrieveBalance(@NotNull Currency c, @NotNull EconomySubscriber<BigDecimal> sub) {
        sub.succeed(BigDecimal.valueOf(np.getBalance(TreasuryCurrency.getEconomy(c))));
    }

    @Override
    public void setBalance(@NotNull BigDecimal amount, @NotNull EconomyTransactionInitiator<?> initiator, @NotNull Currency c, @NotNull EconomySubscriber<BigDecimal> sub) {
        np.setBalance(TreasuryCurrency.getEconomy(c), amount.doubleValue());
        sub.succeed(BigDecimal.valueOf(np.getBalance(TreasuryCurrency.getEconomy(c))));
    }

    @Override
    public void doTransaction(@NotNull EconomyTransaction trans, @NotNull EconomySubscriber<BigDecimal> sub) {
        Economy econ = Economy.getEconomy(trans.getCurrencyID());
        np.add(econ, trans.getTransactionAmount().doubleValue());
        treasuryTrans.get(this.np.getPlayer().getUniqueId()).add(trans);
        sub.succeed(BigDecimal.valueOf(np.getBalance(econ)));
    }

    @Override
    public void deleteAccount(@NotNull EconomySubscriber<Boolean> sub) {
        sub.succeed(false);
    }

    @Override
    public void retrieveHeldCurrencies(@NotNull EconomySubscriber<Collection<String>> sub) {
        sub.succeed(Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toSet()));
    }

    @Override
    public void retrieveTransactionHistory(int transactionCount, @NotNull Temporal from, @NotNull Temporal to, @NotNull EconomySubscriber<Collection<EconomyTransaction>> sub) {
        List<EconomyTransaction> l = treasuryTrans.get(this.np.getPlayer().getUniqueId());
        sub.succeed(l.subList(0, transactionCount >= l.size() ? l.size() - 1 : transactionCount)
                .stream()
                .filter(t -> t.getTimestamp().isAfter(Instant.from(from)) && t.getTimestamp().isBefore(Instant.from(to)))
                .collect(Collectors.toSet())
        );
    }
}
