package us.teaminceptus.novaconomy.api.economy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import us.teaminceptus.novaconomy.Novaconomy;

public final class Economy implements ConfigurationSerializable {

	private final char symbol;
	private final ConfigurationSection section;
	private final String name;
	private final ItemStack icon;
	private final boolean hasNaturalIncrease;
	private final double conversionScale;

	private boolean interestEnabled;
	
	private Economy(ConfigurationSection section, String name, ItemStack icon, char symbol, boolean naturalIncrease, double conversionScale) {
		this.symbol = symbol;
		this.section = section;
		this.name = name;
		this.icon = icon;
		this.hasNaturalIncrease = naturalIncrease;
		this.conversionScale = conversionScale;

		if (!(section.isString("name"))) {
			section.set("name", this.name);
		}

		if (!(section.isItemStack("icon"))) {
			section.set("icon", this.icon);
		}

		if (section.getString("symbol").length() > 1) {
			section.set("symbol", this.symbol);
		}

		if (!(section.isBoolean("increase-naturally"))) {
			section.set("increase-naturally", this.hasNaturalIncrease);
		}

		if (!(section.isDouble("conversion-scale"))) {
			section.set("conversion-scale", this.conversionScale);
		}

		if (!(section.isBoolean("interest"))) {
			section.set("interest", true);
		}

		this.interestEnabled = section.getBoolean("interest");
	}

	// Implementation & Recommended Implementation

	public final Map<String, Object> serialize() {
		Map<String, Object> serial = new HashMap<>();
		serial.put("section", section);
		serial.put("name", section.getString("name"));
		serial.put("icon", section.getItemStack("icon"));
		serial.put("symbol", section.get("symbol"));
		serial.put("increase-naturally", section.get("increase-naturally"));
		serial.put("conversion-scale", section.getDouble("conversion-scale"));
		serial.put("interest", section.getBoolean("interest"));
		
		return serial;
	}

	public static final Economy deserialize(Map<String, Object> serial) {
		Economy econ = new Economy(serial.get("section"), (String) serial.get("name"), (ItemStack) section.get("icon"), (char) section.get("symbol"), (boolean) section.get("increase-naturally"), (double) serial.get("conversion-scale")));

		econ.interestEnabled = serial.get
	}
	
	// Other

	public boolean hasInterest() {
		return section.getBoolean("interest");
	}

	public static List<Economy> getInterestEconomies() {
		return getEconomies().stream().filter(econ -> econ.hasInterest()).toList();
	}

	public void setInterest(boolean interest) {
		this.interestEnabled = interest;
		setValues();
	}

	private void setValues() {
		section.set("interest", this.interest);

		section.set("name", this.name);
		section.set("icon", this.icon);
		section.set("symbol", this.symbol);
		section.set("increase-naturally", this.hasNaturalIncrease);
		section.set("conversion-scale", this.conversionScale);
	}

	public static void removeEconomy(String name) {
		removeEconomy(getEconomy(name));
	}

	public static void removeEconomy(Economy econ) throws IllegalArgumentException {
		if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
		
		FileConfiguration config = Novaconomy.getEconomiesFile();

		config.set(econ.getSection(), null);
		for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
			NovaPlayer np = new NovaPlayer(p);
			FileConfiguration playerConfig = np.getPlayerConfig();

			playerConfig.set(econ.getName(), null);
			
			try {
				playerConfig.save(new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder().getPath() + "/players", p.getUniqueId().toString() + ".yml");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			config.save(new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder(), "economies.yml"));
		} catch (IOException e) {
			JavaPlugin.getPlugin(Novaconomy.class).getLogger().info("Error removing economy " + econ.getName());
			e.printStackTrace();
		}
	}

	public static Economy getEconomy(String name) {
		ConfigurationSection section = Novaconomy.getEconomiesFile().getConfigurationSection(name.toLowerCase());
		if (section != null) {
			return new Economy(section, section.getString("name"), section.getItemStack("icon"), (char) section.get("symbol"), section.getBoolean("increase-naturally"));
		} else return null;
	}

	public final char getSymbol() {
		return this.symbol;
	}

	public final boolean hasNaturalIncrease() {
		return this.hasNaturalIncrease;
	}

	public final double getConversionScale() {
		return this.conversionScale;
	}

	public static final List<Economy> getEconomies() {
		List<Economy> economies = new ArrayList<>();
		
		Novaconomy.getEconomiesFile().getValues(false).values().forEach(obj -> {
			if (obj instanceof ConfigurationSection s) {
				economies.add(getEconomy(s.getName()));
			}
		});

		return economies;
	}

	public static final List<Economy> getNaturalEconomies() {
		return getEconomies().stream().filter(econ -> econ.hasNaturalIncrease()).toList();
	}

	public final ConfigurationSection getEconomySection() {
		return this.section;
	}

	public final String getName() {
	 	return this.name;
	}

	public final ItemStack getIcon() {
		return this.icon;
	}

	public final double convertAmount(Economy to, double fromAmount) throws IllegalArgumentException {
		return convertAmount(this, to, fromAmount);
	}

	public static final double convertAmount(Economy from, Economy to, double fromAmount) throws IllegalArgumentException {
		if (from == null) throw new IllegalArgumentException("Economy from is null");
		if (to == null) throw new IllegalArgumentException("Economy to is null");
		if (from.getName().equals(to.getName())) throw new IllegalArgumentException("Economies are identical");
		double scale = from.getConversionScale() / to.getConversionScale();

		return fromAmount * scale;
	}

	public static Economy.Builder builder() {
		return new Economy.Builder();
	}

	public static final class Builder {
		char symbol;
		String name;
		ItemStack icon;
		boolean increaseNaturally;
		double conversionScale;
		
		private Builder() {
			this.icon = new ItemStack(Material.GOLD_INGOT);
			this.symbol = '$';
			this.increaseNaturally = true;
			this.conversionScale = 1;
		}

		public Builder setSymbol(char symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setIcon(Material icon) {
			ItemStack icon = new ItemStack(icon);
			ItemMeta meta = icon.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + this.name);
			icon.setItemMeta(meta);

			this.icon = icon;
			
			return this;
		}

		public Builder setIcon(ItemStack stack) {
			this.icon = stack;
		}

		public Builder setIncreaseNaturally(boolean increaseNaturally) {
			this.increaseNaturally = increaseNaturally;
			return this;
		}

		public Builder setConversionScale(double scale) {
			this.conversionScale = scale;
			return this;
		}

		public final Economy build() throws IllegalArgumentException, UnsupportedOperationException {
			if (this.name == null) throw new IllegalArgumentException("Name cannot be null");

			if (Novaconomy.getEconomiesFile().getConfigurationSection(this.name.toLowerCase()) != null)
			throw new UnsupportedOperationException("Economy already exists");

			ConfigurationSection es = Novaconomy.getEconomiesFile().createSection(this.name.toLowerCase());

			Economy econ = new Economy(es, this.name, this.icon, this.symbol, this.increaseNaturally, this.conversionScale);
			return econ;
		}
	}

}