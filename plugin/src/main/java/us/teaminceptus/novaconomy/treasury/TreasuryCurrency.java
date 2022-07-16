package us.teaminceptus.novaconomy.treasury;

import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.response.EconomySubscriber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.UUID;

class TreasuryCurrency implements Currency {

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
        return econ.getSymbol() + "";
    }

    @Override
    public char getDecimal() {
        return '.';
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

    @Override
    public @NotNull String getDisplayNameSingular() {
        return fromPlural(getDisplayNamePlural());
    }

    @Override
    public @NotNull String getDisplayNamePlural() {
        return toPlural(econ.getName());
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
    public void to(@NotNull Currency c, @NotNull BigDecimal amount, @NotNull EconomySubscriber<BigDecimal> subscription) {
        subscription.succeed(BigDecimal.valueOf(econ.convertAmount(getEconomy(c), amount.doubleValue())));
    }

    @Override
    public void parse(@NotNull String formatted, @NotNull EconomySubscriber<BigDecimal> subscription) {
        subscription.succeed(BigDecimal.valueOf(Double.parseDouble(formatted.replaceAll("[^\\d.-]", ""))));
    }

    @Override
    public @NotNull BigDecimal getStartingBalance(@Nullable UUID playerID) {
        return BigDecimal.ZERO;
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount, @Nullable Locale locale) {
        return DecimalFormat.getInstance(locale).format(amount.doubleValue());
    }

    @Override
    public @NotNull String format(@NotNull BigDecimal amount, @Nullable Locale locale, int precision) {
        double num = precision < 0 ? 1 : Math.pow(10, precision);
        return DecimalFormat.getInstance(locale).format(Math.floor(amount.doubleValue() * num) / num);
    }
}
