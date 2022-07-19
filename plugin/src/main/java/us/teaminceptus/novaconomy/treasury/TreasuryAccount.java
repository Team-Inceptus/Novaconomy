package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.common.misc.TriState;
import me.lokka30.treasury.api.common.response.FailureReason;
import me.lokka30.treasury.api.economy.account.AccountPermission;
import me.lokka30.treasury.api.economy.account.NonPlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.response.EconomyException;
import me.lokka30.treasury.api.economy.response.EconomySubscriber;
import me.lokka30.treasury.api.economy.transaction.EconomyTransaction;
import me.lokka30.treasury.api.economy.transaction.EconomyTransactionInitiator;
import me.lokka30.treasury.api.economy.transaction.EconomyTransactionType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.treasury.TreasuryCurrency.getEconomy;

class TreasuryAccount implements NonPlayerAccount, ConfigurationSerializable {

    private final String id;
    private String name;
    private final Map<Economy, Double> balances;

    private final List<String> members;

    private final Map<String, Map<String, Boolean>> memberPermissions;

    final static File globalF = new File(NovaConfig.getDataFolder(), "global.yml");

    static FileConfiguration global;
    static ConfigurationSection treasuryAccounts;

    public TreasuryAccount(String id) {
        this.id = id;

        TreasuryAccount acc = (TreasuryAccount) treasuryAccounts.get(id);
        this.name = acc.name;
        this.balances = acc.balances;
        this.members = acc.members;
        this.memberPermissions = acc.memberPermissions;
    }

    public TreasuryAccount(String id, String name) {
        this.id = id;
        this.name = name;
        this.balances = new HashMap<>();
        this.members = new ArrayList<>();
        this.memberPermissions = new HashMap<>();
        transactions.put(id, new ArrayList<>());
    }

    public TreasuryAccount(String id, String name, Map<String, Double> balances, List<String> members, Map<String, Map<String, Boolean>> memberPermissions) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.memberPermissions = memberPermissions;

        Map<Economy, Double> balEcon = new HashMap<>();
        balances.forEach((k, v) -> balEcon.put(Economy.getEconomy(k), v));
        this.balances = balEcon;
    }

    static boolean exists(String id) {
        return treasuryAccounts.contains(id);
    }

    @Override
    public @NotNull String getIdentifier() {
        return id;
    }

    static Set<TreasuryAccount> getAccounts() {
        Set<TreasuryAccount> accs = new HashSet<>();
        for (String id : treasuryAccounts.getKeys(false)) accs.add((TreasuryAccount) treasuryAccounts.get(id));

        return accs;
    }

    private void save() {
        treasuryAccounts.set(id, this);
        try { global.save(globalF); } catch (IOException e) {
            Bukkit.getLogger().severe(e.getClass().getSimpleName());
            Bukkit.getLogger().severe(e.getMessage());
            for (StackTraceElement s : e.getStackTrace()) Bukkit.getLogger().severe(s.toString());
        }
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public void setName(@Nullable String name, @NotNull EconomySubscriber<Boolean> subscription) {
        this.name = name;
        save();
        subscription.succeed(true);
    }

    @Override
    public void retrieveBalance(@NotNull Currency currency, @NotNull EconomySubscriber<BigDecimal> subscription) {
        subscription.succeed(BigDecimal.valueOf(balances.getOrDefault(getEconomy(currency), 0.0)));
    }

    @Override
    public void setBalance(@NotNull BigDecimal amount, @NotNull EconomyTransactionInitiator<?> initiator, @NotNull Currency currency, @NotNull EconomySubscriber<BigDecimal> subscription) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            subscription.fail(new EconomyException(FailureReason.of("Cannot set a negative balance")));
            return;
        }

        balances.put(getEconomy(currency), amount.doubleValue());
        save();
        subscription.succeed(amount);
    }

    @Override
    public void doTransaction(@NotNull EconomyTransaction trans, @NotNull EconomySubscriber<BigDecimal> sub) {
        double amount = trans.getTransactionType() == EconomyTransactionType.DEPOSIT ? trans.getTransactionAmount().doubleValue() : -trans.getTransactionAmount().doubleValue();
        Economy econ = Economy.getEconomy(trans.getCurrencyID());
        balances.put(econ, balances.get(econ) + amount);
        save();
        sub.succeed(BigDecimal.valueOf(amount));
    }

    @Override
    public void deleteAccount(@NotNull EconomySubscriber<Boolean> subscription) {
        treasuryAccounts.set(id, null);
        save();
        subscription.succeed(true);
    }

    @Override
    public void retrieveHeldCurrencies(@NotNull EconomySubscriber<Collection<String>> subscription) {
        subscription.succeed(balances.keySet().stream().map(Economy::getName).collect(Collectors.toSet()));
    }

    private static final Map<String, List<EconomyTransaction>> transactions = new HashMap<>();

    @Override
    public void retrieveTransactionHistory(int transactionCount, @NotNull Temporal from, @NotNull Temporal to, @NotNull EconomySubscriber<Collection<EconomyTransaction>> subscription) {
        subscription.succeed(transactions.get(this.id)
                .subList(0, transactionCount)
                .stream()
                .filter(t -> t.getTimestamp().isAfter(Instant.from(from)) && t.getTimestamp().isBefore(Instant.from(to)))
                .collect(Collectors.toSet()));
    }

    @Override
    public void retrieveMemberIds(@NotNull EconomySubscriber<Collection<UUID>> subscription) {
        subscription.succeed(members.stream().map(UUID::fromString).collect(Collectors.toSet()));
    }

    @Override
    public void isMember(@NotNull UUID player, @NotNull EconomySubscriber<Boolean> subscription) {
        subscription.succeed(members.contains(player.toString()));
    }

    @Override
    public void setPermission(@NotNull UUID p, @NotNull TriState value, @NotNull EconomySubscriber<TriState> sub, @NotNull AccountPermission... perms) {
        Map<String, Boolean> playerPerms = new HashMap<>(memberPermissions.get(p.toString()));
        for (AccountPermission perm : perms) playerPerms.put(perm.name(), value.asBoolean());
        memberPermissions.put(p.toString(), playerPerms);
        sub.succeed(value);
        save();
    }

    @Override
    public void retrievePermissions(@NotNull UUID p, @NotNull EconomySubscriber<Map<AccountPermission, TriState>> sub) {
        Map<AccountPermission, TriState> perms = new HashMap<>();
        memberPermissions.getOrDefault(p.toString(), new HashMap<>()).forEach((k, v) -> perms.put(AccountPermission.valueOf(k), TriState.fromBoolean(v)));
        sub.succeed(perms);
    }

    @Override
    public void hasPermission(@NotNull UUID p, @NotNull EconomySubscriber<TriState> sub, @NotNull AccountPermission... permissions) {
        Map<String, Boolean> playerPerms = memberPermissions.getOrDefault(p.toString(), new HashMap<>());
        AtomicBoolean has = new AtomicBoolean(true);
        for (AccountPermission perm : permissions) has.set(has.get() && playerPerms.getOrDefault(perm.name(), false));
        sub.succeed(TriState.fromBoolean(has.get()));
    }

    @SuppressWarnings("unchecked")
    public static TreasuryAccount deserialize(Map<String, Object> map) {
        return new TreasuryAccount((String) map.get("id"), (String) map.get("name"), (Map<String, Double>) map.get("balances"), (List<String>) map.get("members"), (Map<String, Map<String, Boolean>>) map.get("member_permissions"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Double> balString = new HashMap<>();
        balances.forEach((k, v) -> balString.put(k.getName(), v));

        return new HashMap<String, Object>() {{
            put("id", id);
            put("name", name);
            put("balances", balString);
            put("members", members);
            put("members_permissions", memberPermissions);
        }};
    }
}
