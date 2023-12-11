package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.common.NamespacedKey;
import me.lokka30.treasury.api.common.misc.TriState;
import me.lokka30.treasury.api.economy.account.AccountPermission;
import me.lokka30.treasury.api.economy.account.NonPlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.transaction.EconomyTransaction;
import me.lokka30.treasury.api.economy.transaction.EconomyTransactionType;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.treasury.TreasuryCurrency.getEconomy;

final class TreasuryAccount implements NonPlayerAccount, ConfigurationSerializable {

    private final NamespacedKey id;
    private String name;
    private final Map<Economy, Double> balances;

    private final List<String> members;

    private final Map<String, Map<String, Boolean>> memberPermissions;

    final static File globalF = new File(NovaConfig.getDataFolder(), "global.yml");

    static FileConfiguration global;
    static ConfigurationSection treasuryAccounts;

    public TreasuryAccount(NamespacedKey id) {
        this.id = id;

        TreasuryAccount acc = (TreasuryAccount) treasuryAccounts.get(id.toString());
        this.name = acc.name;
        this.balances = acc.balances;
        this.members = acc.members;
        this.memberPermissions = acc.memberPermissions;
    }

    public TreasuryAccount(NamespacedKey id, String name) {
        this.id = id;
        this.name = name;
        this.balances = new HashMap<>();
        this.members = new ArrayList<>();
        this.memberPermissions = new HashMap<>();
        transactions.put(id, new ArrayList<>());
    }

    public TreasuryAccount(NamespacedKey id, String name, Map<String, Double> balances, List<String> members, Map<String, Map<String, Boolean>> memberPermissions) {
        this.id = id;
        this.name = name;
        this.members = members;
        this.memberPermissions = memberPermissions;

        Map<Economy, Double> balEcon = new HashMap<>();
        balances.forEach((k, v) -> balEcon.put(Economy.byName(k), v));
        this.balances = balEcon;
    }

    static boolean exists(String id) {
        return treasuryAccounts.contains(id);
    }

    @Override
    public @NotNull NamespacedKey identifier() {
        return id;
    }

    static Set<TreasuryAccount> getAccounts() {
        Set<TreasuryAccount> accs = new HashSet<>();
        for (String id : treasuryAccounts.getKeys(false)) accs.add((TreasuryAccount) treasuryAccounts.get(id));

        return accs;
    }

    private void save() {
        treasuryAccounts.set(id.toString(), this);
        try { global.save(globalF); } catch (IOException e) {
            NovaConfig.print(e);
        }
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public CompletableFuture<Boolean> setName(@Nullable String name) {
        this.name = name;
        save();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public @NotNull CompletableFuture<BigDecimal> retrieveBalance(@NotNull Currency currency) {
        return CompletableFuture.completedFuture(BigDecimal.valueOf(balances.getOrDefault(getEconomy(currency), 0.0)));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteAccount() {
        treasuryAccounts.set(id.toString(), null);
        save();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<BigDecimal> doTransaction(@NotNull EconomyTransaction trans) {
        double amount = trans.getType() == EconomyTransactionType.DEPOSIT ? trans.getAmount().doubleValue() : -trans.getAmount().doubleValue();
        Economy econ = Economy.byName(trans.getCurrencyId());
        balances.put(econ, balances.get(econ) + amount);
        save();
        return CompletableFuture.completedFuture(BigDecimal.valueOf(amount));
    }

    @Override
    public @NotNull CompletableFuture<Collection<String>> retrieveHeldCurrencies() {
        return CompletableFuture.completedFuture(balances.keySet().stream().map(Economy::getName).collect(Collectors.toSet()));
    }

    private static final Map<NamespacedKey, List<EconomyTransaction>> transactions = new HashMap<>();

    @Override
    public @NotNull CompletableFuture<Collection<EconomyTransaction>> retrieveTransactionHistory(int transactionCount, @NotNull Temporal from, @NotNull Temporal to) {
        return CompletableFuture.completedFuture(transactions.get(this.id)
                .subList(0, transactionCount)
                .stream()
                .filter(t -> t.getTimestamp().isAfter(Instant.from(from)) && t.getTimestamp().isBefore(Instant.from(to)))
                .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Collection<UUID>> retrieveMemberIds() {
        return CompletableFuture.completedFuture(members.stream().map(UUID::fromString).collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Boolean> isMember(@NotNull UUID player) {
        return CompletableFuture.completedFuture(members.contains(player.toString()));
    }

    @Override
    public @NotNull CompletableFuture<Boolean> setPermissions(@NotNull UUID player, @NotNull Map<AccountPermission, TriState> permissionsMap) {
        memberPermissions.get(player.toString())
                .putAll(permissionsMap.entrySet()
                    .stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().name(), e.getValue().asBoolean()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Map<AccountPermission, TriState>> retrievePermissions(@NotNull UUID p) {
        Map<AccountPermission, TriState> perms = new HashMap<>();
        memberPermissions.getOrDefault(p.toString(), new HashMap<>()).forEach((k, v) -> perms.put(AccountPermission.valueOf(k), TriState.fromBoolean(v)));
        return CompletableFuture.completedFuture(perms);
    }

    @Override
    public @NotNull CompletableFuture<Map<UUID, Map<AccountPermission, TriState>>> retrievePermissionsMap() {
        return null;
    }

    @Override
    public CompletableFuture<TriState> hasPermissions(@NotNull UUID p, @NotNull AccountPermission... permissions) {
        Map<String, Boolean> playerPerms = memberPermissions.getOrDefault(p.toString(), new HashMap<>());
        AtomicBoolean has = new AtomicBoolean(true);

        for (AccountPermission perm : permissions) has.set(has.get() && playerPerms.getOrDefault(perm.name(), false));

        return CompletableFuture.completedFuture(TriState.fromBoolean(has.get()));
    }

    @SuppressWarnings("unchecked")
    public static TreasuryAccount deserialize(Map<String, Object> map) {
        String[] split = map.get("id").toString().split(":");
        return new TreasuryAccount(NamespacedKey.of(split[0], split[1]), (String) map.get("name"), (Map<String, Double>) map.get("balances"), (List<String>) map.get("members"), (Map<String, Map<String, Boolean>>) map.get("member_permissions"));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Double> balString = new HashMap<>();
        balances.forEach((k, v) -> balString.put(k.getName(), v));

        return new HashMap<String, Object>() {{
            put("id", id.toString());
            put("name", name);
            put("balances", balString);
            put("members", members);
            put("members_permissions", memberPermissions);
        }};
    }
}
