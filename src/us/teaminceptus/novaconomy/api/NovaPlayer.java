package us.teaminceptus.novaconomy.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.economy.Economy;

public class NovaPlayer {

	private final OfflinePlayer player;
	
	private final File playerFile;
	private final FileConfiguration playerConfig;

	public NovaPlayer(OfflinePlayer p) {
		this.player = p;

		if (!(Novaconomy.getPlayerDirectory().exists())) {
			Novaconomy.getPlayerDirectory().mkdir();
		}

		this.playerFile = new File(Novaconomy.getPlayerDirectory(), p.getUniqueId().toString() + ".yml");

		if (!(this.playerFile.exists())) {
			try {
				this.playerFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.playerConfig = YamlConfiguration.loadConfiguration(playerFile);

		reloadValues();
	}

	public final double getBalance(Economy econ) throws IllegalArgumentException {
		if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
		return this.playerConfig.getConfigurationSection("economies").getConfigurationSection(econ.getName().toLowerCase()).getDouble("balance");
	}

	public final void setBalance(Economy econ, double newBal) throws IllegalArgumentException {
		if (newBal < 0) throw new IllegalArgumentException("Balance cannot be negative");
		if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

		this.playerConfig.getConfigurationSection("economies").getConfigurationSection(econ.getName().toLowerCase()).set("balance", newBal);
	}

	public final void add(Economy econ, double add) {
		if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

		setBalance(econ, getBalance(econ) + add);
	}

	public final void remove(Economy econ, double remove) {
		if (econ == null) throw new IllegalArgumentException("Economy cannot be null");

		setBalance(econ, getBalance(econ) - remove);
	}

	private void reloadValues() {
		OfflinePlayer p = player;

		// General Info
		if (!(playerConfig.isString("name"))) {
			playerConfig.set("name", p.getName());
		}

		if (!(playerConfig.isBoolean("op"))) {
			playerConfig.set("op", p.isOp());
		}

		// Economies
		if (!(playerConfig.isConfigurationSection("economies"))) {
			playerConfig.createSection("economies");
		}

		ConfigurationSection economies = playerConfig.getConfigurationSection("economies");

		if (Economy.getEconomies().size() > 0)
		for (Economy e : Economy.getEconomies()) {
			if (!(economies.isConfigurationSection(e.getName().toLowerCase()))) {
				economies.createSection(e.getName().toLowerCase());
			}

			ConfigurationSection econ = economies.getConfigurationSection(e.getName().toLowerCase());

			if (!(econ.isDouble("balance"))) {
				econ.set("balance", 0D);
			}
		}
	}

}