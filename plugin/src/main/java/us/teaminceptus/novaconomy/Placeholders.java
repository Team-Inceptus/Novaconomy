package us.teaminceptus.novaconomy;

import com.google.common.util.concurrent.AtomicDouble;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

class Placeholders extends PlaceholderExpansion {

    Placeholders() {
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
        put("business_id", p -> {
            if (Business.exists(p)) return Business.getByOwner(p).getUniqueId().toString();
            return "";
        });
        put("all_balances", p -> {
            AtomicDouble bal = new AtomicDouble(0);
            Economy.getEconomies().forEach(e -> bal.addAndGet(new NovaPlayer(p).getBalance(e)));
            return String.valueOf(bal.get());
        });
    }};

    private static final Map<String, BiFunction<OfflinePlayer, String, String>> OFFLINE_ARG_PH = new HashMap<String, BiFunction<OfflinePlayer, String, String>>() {{
        put("balance", (p, arg) -> {
            if (Economy.exists(arg)) return String.valueOf(new NovaPlayer(p).getBalance(Economy.getEconomy(arg)));
            return "0";
        });
    }};

    @Override
    public @NotNull String getIdentifier() {
        return "novaconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "team-inceptus";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    // Impl

    @Override
    public String onRequest(OfflinePlayer p, String arg) {
        if (OFFLINE_PH.containsKey(arg)) return OFFLINE_PH.get(arg).apply(p);
        if (OFFLINE_ARG_PH.containsKey(parseKey(arg))) return OFFLINE_ARG_PH.get(parseKey(arg)).apply(p, parseArgument(arg));
        return null;
    }
}
