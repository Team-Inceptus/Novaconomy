package us.teaminceptus.novaconomy.vault;

import com.google.gson.Gson;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class VaultEconomy extends AbstractEconomy implements net.milkbowl.vault.economy.Economy {

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
        return amount + "" + econ.getSymbol();
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

    private static class APIPlayer {

        public final String name;
        public final String id;

        public APIPlayer(String name, String id) {
            this.name = name;
            this.id = id;
        }

    }

    private static OfflinePlayer getPlayer(String name) {
        if (Bukkit.getOnlineMode()) try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Java 8 PlutoChat Bot");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String inputLine;
                while ((inputLine = input.readLine()) != null) builder.append(inputLine);

                Gson g = new Gson();
                return Bukkit.getOfflinePlayer(untrimUUID(g.fromJson(builder.toString(), APIPlayer.class).id));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        else
            return Bukkit.getPlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
        return null;
    }

    public static UUID untrimUUID(String oldUUID) {
        String p1 = oldUUID.substring(0, 8);
        String p2 = oldUUID.substring(8, 12);
        String p3 = oldUUID.substring(12, 16);
        String p4 = oldUUID.substring(16, 20);
        String p5 = oldUUID.substring(20, 32);

        String newUUID = p1 + "-" + p2 + "-" + p3 + "-" + p4 + "-" + p5;

        return UUID.fromString(newUUID);
    }

    @Override
    public double getBalance(String playerName) {
        return new NovaPlayer(getPlayer(playerName)).getBalance(econ);
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
        NovaPlayer np = new NovaPlayer(getPlayer(playerName));
        np.remove(econ, amount);
        return new VaultEconomyResponse(amount, np.getBalance(econ));
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        NovaPlayer np = new NovaPlayer(getPlayer(playerName));
        np.add(econ, amount);
        return new VaultEconomyResponse(amount, np.getBalance(econ));
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Vault Banks are not apart of Novaconomy");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
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
