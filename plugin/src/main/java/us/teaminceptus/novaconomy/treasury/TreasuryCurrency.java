package us.teaminceptus.novaconomy.treasury;

import com.google.common.collect.ImmutableMap;
import me.lokka30.treasury.api.economy.account.Account;
import me.lokka30.treasury.api.economy.currency.Currency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class TreasuryCurrency implements Currency {

    private final Economy econ;

    TreasuryCurrency(Economy econ) {
        this.econ = econ;
    }

    static Economy getEconomy(Currency c) {
        return Economy.getEconomy(c.getIdentifier());
    }

    @Override
    public @NotNull String getIdentifier() {
        return econ.getName();
    }

    @Override
    public @NotNull String getSymbol() {
        return String.valueOf(econ.getSymbol());
    }

    @Override
    public char getDecimal(@Nullable Locale locale) {
        return String.format(locale, "%,.2f", 1.1).charAt(1);
    }

    @Override
    public @NotNull Map<Locale, Character> getLocaleDecimalMap() {
        Map<Locale, Character> map = new HashMap<>();
        for (Locale l : Locale.getAvailableLocales())
            map.put(l, getDecimal(l));

        return ImmutableMap.copyOf(map);
    }

    private static String fromPlural(String str) {
        if (str.endsWith("s")) return str.substring(0, str.length() - 1);
        if (str.endsWith("es")) return str.substring(0, str.length() - 2) + "s";
        if (str.endsWith("ies")) return str.substring(0, str.length() - 3) + "y";
        return str;
    }

    private static String toPlural(String str) {
        if (str.endsWith("y")) return str.substring(0, str.length() - 1) + "ies";
        if (str.endsWith("s")) return str + "es";
        return str + "s";
    }

    public @NotNull String getDisplayNameSingular() {
        return fromPlural(getDisplayNamePlural());
    }

    public @NotNull String getDisplayNamePlural() {
        return toPlural(econ.getName());
    }

    @Override
    @NotNull
    public String getDisplayName(@NotNull BigDecimal value, @Nullable Locale locale) {
        double val = value.doubleValue();

        if (value.equals(BigDecimal.ZERO)) return getDisplayNamePlural();
        if (val == 1) return getDisplayNameSingular();

        return getDisplayNamePlural();
    }

    @Override
    public int getPrecision() {
        return 2;
    }

    @Override
    public boolean isPrimary() {
        Object o = NovaConfig.loadFunctionalityFile().get("VaultEconomy", -1);
        return o instanceof String && o.equals(econ.getName());
    }

    @Override
    public CompletableFuture<BigDecimal> parse(@NotNull String formatted, @Nullable Locale locale) {
        return CompletableFuture.completedFuture(BigDecimal.valueOf(Double.parseDouble(formatted.replaceAll("[^\\d.-]", ""))));
    }

    @Override
    public @NotNull BigDecimal getStartingBalance(@NotNull Account account) { return BigDecimal.ZERO; }

    @Override
    public @NotNull BigDecimal getConversionRate() {
        return BigDecimal.valueOf(econ.getConversionScale());
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount, @Nullable Locale locale) {
        return DecimalFormat.getInstance(locale == null ? Locale.getDefault() : locale).format(amount.doubleValue());
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount, @Nullable Locale locale, int precision) {
        double num = precision < 0 ? 1 : Math.pow(10, precision);
        return DecimalFormat.getInstance(locale == null ? Locale.getDefault() : locale).format(Math.floor(amount.doubleValue() * num) / num);
    }
}
