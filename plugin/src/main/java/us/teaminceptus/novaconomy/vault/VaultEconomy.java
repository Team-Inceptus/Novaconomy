package us.teaminceptus.novaconomy.vault;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import java.util.List;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.util.NovaUtil.getPlayer;

class VaultEconomy extends AbstractEconomy {

    public static final EconomyResponse NO_BANKS = new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    private final Economy econ;

    public VaultEconomy(Economy econ) {
        this.econ = econ;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return econ.getName();
    }

    @Override
    public boolean hasBankSupport() {
        return true;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        return amount + String.valueOf(econ.getSymbol());
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
    public String currencyNamePlural() {
        return toPlural(econ.getName());
    }

    @Override
    public String currencyNameSingular() {
        return fromPlural(currencyNamePlural());
    }

    @Override
    public boolean hasAccount(String playerName) {
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer p = getPlayer(playerName);
        if (p == null) return 0;

        return new NovaPlayer(p).getBalance(econ);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) == amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer p = getPlayer(playerName);
        if (p == null) return new VaultEconomyResponse(amount, 0);

        NovaPlayer np = new NovaPlayer(p);
        np.remove(econ, amount);
        return new VaultEconomyResponse(amount, np.getBalance(econ));
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer p = getPlayer(playerName);
        if (p == null) return new VaultEconomyResponse(amount, 0);

        NovaPlayer np = new NovaPlayer(p);
        np.add(econ, amount);
        return new VaultEconomyResponse(amount, np.getBalance(econ));
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return NO_BANKS;
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Cannot delete banks");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new VaultEconomyResponse(0, Bank.getBalance(Economy.byName(name)));
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        boolean has = Bank.getBalance(Economy.byName(name)) == amount;
        return new EconomyResponse(0, Bank.getBalance(Economy.byName(name)), has ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE, null);
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        Economy econ = Economy.byName(name);
        Bank.removeBalance(econ, amount);
        return new VaultEconomyResponse(0, Bank.getBalance(Economy.byName(name)));
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        Economy econ = Economy.byName(name);
        Bank.addBalance(econ, amount);
        return new VaultEconomyResponse(0, Bank.getBalance(Economy.byName(name)));
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return NO_BANKS;
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new VaultEconomyResponse(0, 0);
    }

    @Override
    public List<String> getBanks() {
        return Economy.getEconomies().stream().map(Economy::getName).collect(Collectors.toList());
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return false;
    }
}
