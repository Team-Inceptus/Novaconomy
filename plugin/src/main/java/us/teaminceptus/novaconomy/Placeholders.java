package us.teaminceptus.novaconomy;

import com.google.common.util.concurrent.AtomicDouble;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

class Placeholders extends PlaceholderExpansion {

    private final Novaconomy plugin;

    Placeholders(Novaconomy plugin) {
        this.plugin = plugin;
        register();
    }

    private static String parseArgument(String s) {
        if (!s.contains("[")) return "";
        if (!s.contains("]")) return "";
        return s.replaceFirst("\\[", "").substring(0, s.lastIndexOf("]"));
    }

    private static String parseKey(String s) {
        if (!s.contains("[")) return s;
        if (!s.contains("]")) return s;
        return s.split("\\[")[0];
    }

    private static final Map<String, Function<OfflinePlayer, String>> OFFLINE_PH = new HashMap<String, Function<OfflinePlayer, String>>() {{
        put("business_name", p -> {
           if (Business.exists(p)) return Business.getByOwner(p).getName();
           return "";
        });
        put("business_product_count", p -> {
            if (Business.exists(p)) return String.valueOf(Business.getByOwner(p).getProducts().size());
            return "";
        });
        put("business_resource_count", p -> {
            if (Business.exists(p)) return String.valueOf(Business.getByOwner(p).getResources().size());
            return "";
        });

        put("business_id", p -> {
            if (Business.exists(p)) return Business.getByOwner(p).getUniqueId().toString();
            return "";
        });
        put("all_balances", p -> {
            AtomicDouble bal = new AtomicDouble();
            Economy.getEconomies().forEach(e -> bal.addAndGet(new NovaPlayer(p).getBalance(e)));
            return String.valueOf(bal.get());
        });
        put("last_withdrawal_timestamp", p -> String.valueOf(new NovaPlayer(p).getLastBankWithdraw().getTimestamp()));
        put("last_withdrawal_amount", p -> String.valueOf(new NovaPlayer(p).getLastBankWithdraw().getAmount()));
        put("last_withdrawal_economy", p -> String.valueOf(new NovaPlayer(p).getLastBankWithdraw().getEconomy().getName()));

        put("last_deposit_timestamp", p -> String.valueOf(new NovaPlayer(p).getLastBankDeposit().getTimestamp()));
        put("last_deposit_amount", p -> String.valueOf(new NovaPlayer(p).getLastBankDeposit().getAmount()));
        put("last_deposit_economy", p -> String.valueOf(new NovaPlayer(p).getLastBankDeposit().getEconomy().getName()));

        put("business_product_purchases", p -> {
            if (Business.exists(p)) return String.valueOf(Business.getByOwner(p).getStatistics().getTotalSales());
            return "";
        });
        put("business_last_transaction_timestamp", p -> {
            if (Business.exists(p)) return String.valueOf(Business.getByOwner(p).getStatistics().getLastTransaction().getTimestamp().getTime());
            return "";
        });
        put("business_last_transaction_amount", p -> {
            if (Business.exists(p)) return String.valueOf(Business.getByOwner(p).getStatistics().getLastTransaction().getProduct().getAmount());
            return "";
        });
    }};

    private static final Map<String, BiFunction<OfflinePlayer, String, String>> OFFLINE_ARG_PH = new HashMap<String, BiFunction<OfflinePlayer, String, String>>() {{
        put("balance", (p, arg) -> {
            if (Economy.exists(arg)) return String.valueOf(new NovaPlayer(p).getBalance(Economy.getEconomy(arg)));
            return "0";
        });
        put("donated", (p, arg) -> {
            if (Economy.exists(arg)) return String.valueOf(new NovaPlayer(p).getDonatedAmount(Economy.getEconomy(arg)));
            return "0";
        });
    }};

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
        return "1.0.2";
    }

    // Impl

    @Override
    public List<String> getPlaceholders() {
        List<String> ph = new ArrayList<>();
        ph.addAll(OFFLINE_PH.keySet());
        ph.addAll(OFFLINE_ARG_PH.keySet());

        return ph;
    }

    @Override
    public String onRequest(OfflinePlayer p, String arg) {
        if (OFFLINE_PH.containsKey(arg)) return OFFLINE_PH.get(arg).apply(p);
        if (OFFLINE_ARG_PH.containsKey(parseKey(arg))) return OFFLINE_ARG_PH.get(parseKey(arg)).apply(p, parseArgument(arg));
        return null;
    }
}
