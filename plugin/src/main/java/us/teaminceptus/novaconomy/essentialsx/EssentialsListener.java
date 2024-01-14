package us.teaminceptus.novaconomy.essentialsx;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.ess3.api.events.NickChangeEvent;
import net.ess3.api.events.UserWarpEvent;
import net.ess3.api.events.teleport.TeleportWarmupEvent;
import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;

import static us.teaminceptus.novaconomy.messages.MessageHandler.*;

public final class EssentialsListener implements Listener {

    // private final Novaconomy plugin;

    public EssentialsListener(Novaconomy plugin) {
        // this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onNickChange(NickChangeEvent e) {
        if (!NovaConfig.getFunctionalityConfig().getBoolean("Essentials.NickCost.Enabled", false)) return;
    
        Economy econ = Economy.byName(NovaConfig.getFunctionalityConfig().getString("Essentials.NickCost.Economy", ""));
        if (econ == null) return;

        double amount = NovaConfig.getFunctionalityConfig().getDouble("Essentials.NickCost.Amount", 0.0);
        if (amount <= 0) return;

        Player p = e.getAffected().getBase();
        NovaPlayer np = new NovaPlayer(p);

        if (!np.canAfford(econ, amount, true)) {
            e.setCancelled(true);
            messages.sendMessage(p, "error.economy.invalid_amount.essentials.nick");
            return;
        }

        np.remove(econ, amount);
    }

    @EventHandler
    public void onWarp(UserWarpEvent e) {
        if (!NovaConfig.getFunctionalityConfig().getBoolean("Essentials.WarpCost.Enabled", false)) return;
    
        Economy econ = Economy.byName(NovaConfig.getFunctionalityConfig().getString("Essentials.WarpCost.Economy", ""));
        if (econ == null) return;

        double amount = NovaConfig.getFunctionalityConfig().getDouble("Essentials.WarpCost.Amount", 0.0);
        if (amount <= 0) return;

        Player p = e.getUser().getBase();
        NovaPlayer np = new NovaPlayer(p);

        if (!np.canAfford(econ, amount, true)) {
            e.setCancelled(true);
            messages.sendMessage(p, "error.economy.invalid_amount.essentials.warp");
            return;
        }

        np.remove(econ, amount);
    }

    @EventHandler
    public void onTeleport(TeleportWarmupEvent e) {
        if (!NovaConfig.getFunctionalityConfig().getBoolean("Essentials.TeleportCost.Enabled", false)) return;
    
        Economy econ = Economy.byName(NovaConfig.getFunctionalityConfig().getString("Essentials.TeleportCost.Economy", ""));
        if (econ == null) return;

        double amount = NovaConfig.getFunctionalityConfig().getDouble("Essentials.TeleportCost.Amount", 0.0);
        if (amount <= 0) return;

        Player p = e.getTeleportee().getBase();
        NovaPlayer np = new NovaPlayer(p);

        if (!np.canAfford(econ, amount, true)) {
            e.setCancelled(true);
            messages.sendMessage(p, "error.economy.invalid_amount.essentials.teleport");
            return;
        }

        np.remove(econ, amount);
    }


    
}
