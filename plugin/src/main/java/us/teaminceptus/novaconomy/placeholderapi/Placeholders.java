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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents the Novaconomy PlaceholderAPI Expansion
 */
public class Placeholders extends PlaceholderExpansion {

    private final Novaconomy plugin;

    public Placeholders(Novaconomy plugin) {
        this.plugin = plugin;
        register();
    }

    private static final Map<String, Function<OfflinePlayer, String>> OFFLINE_PH = ImmutableMap.<String, Function<OfflinePlayer, String>>builder()
            .put("business_name", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getName();
                return "";
            })
            .put("business_product_count", p -> {
                if (Business.exists(p)) return String.valueOf(Business.byOwner(p).getProducts().size());
                return "";
            })
            .put("business_resource_count", p -> {
                if (Business.exists(p)) return String.valueOf(Business.byOwner(p).getResources().size());
                return "";
            })
            .put("business_id", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getUniqueId().toString();
                return "";
            })
            .put("business_icon", p -> {
                if (Business.exists(p)) return Business.byOwner(p).getIcon().getType().toString();
                return "";
            })
            .put("all_balances", p -> {
                AtomicDouble bal = new AtomicDouble();
                Economy.getEconomies().forEach(e -> bal.addAndGet(new NovaPlayer(p).getBalance(e)));
                return String.valueOf(bal.get());
            })
            .put("last_withdrawal_timestamp", p -> String.valueOf(new NovaPlayer(p).getLastBankWithdraw().getTimestamp()))
            .put("last_withdrawal_amount", p -> String.valueOf(new NovaPlayer(p).getLastBankWithdraw().getAmount()))
            .put("last_withdrawal_economy", p -> String.valueOf(new NovaPlayer(p).getLastBankWithdraw().getEconomy().getName()))

            .put("last_deposit_timestamp", p -> String.valueOf(new NovaPlayer(p).getLastBankDeposit().getTimestamp()))
            .put("last_deposit_amount", p -> String.valueOf(new NovaPlayer(p).getLastBankDeposit().getAmount()))
            .put("last_deposit_economy", p -> String.valueOf(new NovaPlayer(p).getLastBankDeposit().getEconomy().getName()))

            .put("business_product_purchases", p -> {
                if (Business.exists(p)) return String.valueOf(Business.byOwner(p).getStatistics().getTotalSales());
                return "";
            })
            .put("business_last_transaction_timestamp", p -> {
                if (Business.exists(p)) return String.valueOf(Business.byOwner(p).getStatistics().getLastTransaction().getTimestamp().getTime());
                return "";
            })
            .put("business_last_transaction_amount", p -> {
                if (Business.exists(p)) return String.valueOf(Business.byOwner(p).getStatistics().getLastTransaction().getProduct().getAmount());
                return "";
            })
            .put("business_advertising_balance", p -> {
                if (Business.exists(p)) return String.valueOf(Business.byOwner(p).getAdvertisingBalance());
                return "";
            })
            .put("corporation_name", p -> {
                if (Corporation.exists(p)) return Corporation.byOwner(p).getName();
                return "";
            })
            .put("corporation_id", p -> {
                if (Corporation.exists(p)) return Corporation.byOwner(p).getUniqueId().toString();
                return "";
            })
            .put("corporation_icon", p -> {
                if (Corporation.exists(p)) return Corporation.byOwner(p).getIcon().name().toLowerCase();
                return "";
            })

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
        return "1.7.0";
    }

    // Impl

    @Override
    public List<String> getPlaceholders() {
        return new ArrayList<>(OFFLINE_PH.keySet());
    }

    @Override
    public String onRequest(OfflinePlayer p, String arg) {
        if (OFFLINE_PH.containsKey(arg)) return OFFLINE_PH.get(arg).apply(p);
        return null;
    }
}
