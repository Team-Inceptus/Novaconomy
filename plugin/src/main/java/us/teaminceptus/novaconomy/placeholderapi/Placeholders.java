package us.teaminceptus.novaconomy.placeholderapi;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AtomicDouble;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the Novaconomy PlaceholderAPI Expansion
 */
public class Placeholders extends PlaceholderExpansion {

    private final Novaconomy plugin;

    public Placeholders(Novaconomy plugin) {
        this.plugin = plugin;
        register();
    }

    private static <T> Map<String, Function<OfflinePlayer, Object>> withArguments(String base, Iterable<T> list, Function<T, String> toString, BiFunction<OfflinePlayer, T, Object> function) {
        Map<String, Function<OfflinePlayer, Object>> map = new HashMap<>();
        for (T t : list)
            map.put(String.format(base, toString.apply(t)), p -> function.apply(p, t));

        return map;
    }

    // 1.7.2 Note: Introduction of Argumentative Placeholders requires real-time Update

    private static final Supplier<Map<String, Function<OfflinePlayer, Object>>> PLACEHOLDERS = () -> ImmutableMap.<String, Function<OfflinePlayer, Object>>builder()
            .put("business_name", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getName();
                return "";
            })
            .put("business_product_count", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getProducts().size();
                return "";
            })
            .put("business_resource_count", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getResources().size();
                return "";
            })
            .put("business_id", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getUniqueId();
                return "";
            })
            .put("business_icon", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getIcon().getType();
                return "";
            })
            .put("all_balances", p -> {
                AtomicDouble bal = new AtomicDouble();
                Economy.getEconomies().forEach(e -> bal.addAndGet(new NovaPlayer(p).getBalance(e)));
                return bal.get();
            })
            .put("last_withdrawal_timestamp", p -> new NovaPlayer(p).getLastBankWithdraw().getTimestamp())
            .put("last_withdrawal_amount", p -> new NovaPlayer(p).getLastBankWithdraw().getAmount())
            .put("last_withdrawal_economy", p -> new NovaPlayer(p).getLastBankWithdraw().getEconomy().getName())

            .put("last_deposit_timestamp", p -> new NovaPlayer(p).getLastBankDeposit().getTimestamp())
            .put("last_deposit_amount", p -> new NovaPlayer(p).getLastBankDeposit().getAmount())
            .put("last_deposit_economy", p -> new NovaPlayer(p).getLastBankDeposit().getEconomy().getName())

            .put("business_product_purchases", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getStatistics().getTotalSales();
                return "";
            })
            .put("business_last_transaction_timestamp", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getStatistics().getLastTransaction().getTimestamp().getTime();
                return "";
            })
            .put("business_last_transaction_amount", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getStatistics().getLastTransaction().getProduct().getAmount();
                return "";
            })
            .put("business_advertising_balance", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getAdvertisingBalance();
                return "";
            })
            .put("corporation_name", p -> {
                if (Corporation.exists(p)) return Corporation.byOwner(p).getName();
                return "";
            })
            .put("corporation_id", p -> {
                if (Corporation.exists(p)) return Corporation.byOwner(p).getUniqueId();
                return "";
            })
            .put("corporation_icon", p -> {
                if (Corporation.exists(p)) return Corporation.byOwner(p).getIcon().name().toLowerCase();
                return "";
            })
            .put("all_donated_amount", p -> {
                AtomicDouble donated = new AtomicDouble();
                NovaPlayer np = new NovaPlayer(p);
                np.getAllDonatedAmounts().values().forEach(donated::addAndGet);
                return donated.get();
            })
            // Argumentative Placeholders
            .putAll(withArguments("%s_balance", Economy.getEconomies(), econ -> econ.getName().toLowerCase(),
                (p, econ) -> new NovaPlayer(p).getBalance(econ))
            )
            .putAll(withArguments("%s_donated_amount", Economy.getEconomies(), econ -> econ.getName().toLowerCase(),
                (p, econ) -> new NovaPlayer(p).getDonatedAmount(econ))
            )
            .build();

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return "team-inceptus";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    // Impl

    @Override
    public List<String> getPlaceholders() {
        return new ArrayList<>(PLACEHOLDERS.get().keySet());
    }

    @Override
    public String onRequest(OfflinePlayer p, String arg) {
        Map<String, Function<OfflinePlayer, Object>> map = PLACEHOLDERS.get();
        if (map.containsKey(arg)) return map.get(arg).apply(p).toString();
        return null;
    }
}
