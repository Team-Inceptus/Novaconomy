package us.teaminceptus.novaconomy.vault;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Main Vault Registry class for injecting
 */
public class VaultRegistry {

    private final Novaconomy plugin;

    /**
     * Constructs the VaultRegistry.
     * @param plugin Plugin to use
     */
    public VaultRegistry(Novaconomy plugin) {
        this.plugin = plugin;
        if (Economy.getEconomies().size() == 0) {
            plugin.getLogger().info("No Economies Created - Vault will not be used");
            return;
        }

        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp != null) {
            plugin.getLogger().info("Other Economy Registration Found - Vault will not be used");
            return;
        }

        Object o = NovaConfig.loadFunctionalityFile().get("VaultEconomy", -1);
        if (o instanceof Integer) {
            plugin.getLogger().info("VaultEconomy is disabled in the config - Vault will not be used");
            return;
        }

        Economy first = Economy.getEconomies().stream().sorted(Comparator.comparing(Economy::getName)).collect(Collectors.toList()).get(0);
        Economy econ = Economy.getEconomy(NovaConfig.loadFunctionalityFile().getString("VaultEconomy", first.getName()));

        Bukkit.getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, new VaultEconomy(econ), plugin, ServicePriority.Highest);
        plugin.getLogger().info("Injected Economy \"" + econ.getName() + "\"" + " (" + econ.getUniqueId() + ")" + " into Vault");
    }

}
