package us.teaminceptus.novaconomy;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Bukkit;

import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.events.PlayerChangeBalanceEvent;

class Events implements Listener {

	protected Novaconomy plugin;

	protected Events(Novaconomy plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	static Random r = new Random();

	@EventHandler
	public void moneyIncrease(EntityDamageByEntityEvent e) {
		if (e.isCancelled()) return;
		if (!(e.getDamager() instanceof Player p)) return;
		if (!(e.getEntity() instanceof LivingEntity en)) return;
		if (en.getHealth() - e.getFinalDamage() > 0) return;

		NovaPlayer np = new NovaPlayer(p);

		for (Economy econ : Economy.getNaturalEconomies()) {
			double increase = en.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / r.nextInt(2) + 1.5;
			double previousBal = np.getBalance(econ);
			double newBal = previousBal + increase;

			PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, increase, previousBal, newBal, true);

			Bukkit.getPluginManager().callEvent(event);
			if (!(event.isCancelled())) {
				
			}
		}
	}
}