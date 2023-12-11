package us.teaminceptus.novaconomy.vault;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Main Vault Registry class for injecting
 */
public class VaultRegistry {

    public static void reloadVault() {
        Plugin plugin = NovaConfig.getPlugin();
        if (Economy.getEconomies().isEmpty()) {
            plugin.getLogger().info("No Economies Created - Vault will not be used");
            return;
        }

        if (getVaultEconomy() == null) {
            inject();
            return;
        }

        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            inject();
            return;
        }

        if (rsp != null && !(rsp.getProvider() instanceof VaultEconomy))
            plugin.getLogger().info("Other Economy Registration Found - Vault will not be used");

        if (rsp.getProvider() instanceof VaultEconomy && !rsp.getProvider().getName().equals(getVaultEconomy().getName())) {
            Bukkit.getServicesManager().unregister(net.milkbowl.vault.economy.Economy.class, rsp.getProvider());
            inject();
        }
    }

    private static Economy getVaultEconomy() {
        Object o = NovaConfig.loadFunctionalityFile().get("VaultEconomy", -1);
        if (o instanceof Integer) return null;

        Economy first = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList()).get(0);
        return Economy.byName(NovaConfig.loadFunctionalityFile().getString("VaultEconomy", first.getName()));
    }

    private static void inject() {
        Plugin plugin = NovaConfig.getPlugin();
        if (getVaultEconomy() == null) {
            plugin.getLogger().info("VaultEconomy is disabled in the config - Registering All Economies...");

            for (RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp : Bukkit.getServicesManager().getRegistrations(net.milkbowl.vault.economy.Economy.class))
                if (rsp.getProvider() instanceof VaultEconomy)
                    Bukkit.getServicesManager().unregister(net.milkbowl.vault.economy.Economy.class, rsp.getProvider());

            for (Economy econ : Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList()).subList(1, Economy.getEconomies().size())) {
                VaultEconomy v = new VaultEconomy(econ);
                if (Bukkit.getServicesManager().getRegistrations(net.milkbowl.vault.economy.Economy.class).stream().anyMatch(r -> r.getProvider().getName().equals(v.getName())))
                    continue;

                Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, v, plugin, ServicePriority.Normal);
            }

            Economy main = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList()).get(0);
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultEconomy(main), plugin, ServicePriority.High);

            plugin.getLogger().info("Registered All Economies, Main Economy is: " + main.getName() + " (" + main.getUniqueId() + ")");
        } else {
            Economy econ = getVaultEconomy();

            Bukkit.getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultEconomy(econ), plugin, ServicePriority.Normal);
            plugin.getLogger().info("Injected Economy \"" + econ.getName() + "\"" + " (" + econ.getUniqueId() + ")" + " into Vault");
        }
    }

}
