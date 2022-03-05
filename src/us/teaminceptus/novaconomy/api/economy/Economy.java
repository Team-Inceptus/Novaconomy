package us.teaminceptus.novaconomy.api.economy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import us.teaminceptus.novaconomy.Novaconomy;
import us.teaminceptus.novaconomy.api.NovaPlayer;

/**
 * Represents an Economy
 *
 */
public final class Economy implements ConfigurationSerializable {

	private final char symbol;
	private final ConfigurationSection section;
	private final String name;
	private final ItemStack icon;
	private final boolean hasNaturalIncrease;
	private final double conversionScale;

	private boolean interestEnabled;
	
	private Economy(ConfigurationSection section, String name, ItemStack icon, char symbol, boolean naturalIncrease, double conversionScale) {
		FileConfiguration economies = Novaconomy.getEconomiesFile();
		
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
		
		if (!(section.isString("symbol"))) {
			section.set("symbol", this.symbol);
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
		
		try {
			economies.save(new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder(), "economies.yml"));
		} catch (IOException e) {
			JavaPlugin.getPlugin(Novaconomy.class).getLogger().info("Error saving economies.yml");
			e.printStackTrace();
		}
	}

	// Implementation & Recommended Implementation
		
	/**
	 * Serialization inside a YML File
	 * @return Serialized Economy
	 * @apiNote This is only meant to be passed to {@link Economy#deserialize(Map)}
	 */
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
	
	/**
	 * Serialization from a YML File
	 * @param serial Map of serialization
	 * @throws NullPointerException if a necessary part is missing
	 * @return Economy Class
	 * @see Economy#serialize()
	 * @apiNote Using this method from a constructed map may break the plugin. Only input Maps from {@link Economy#serialize()}
	 */
	public static final Economy deserialize(Map<String, Object> serial) throws NullPointerException {
		Economy econ = new Economy((ConfigurationSection) serial.get("section"), (String) serial.get("name"), (ItemStack) serial.get("icon"), (char) serial.get("symbol"), (boolean) serial.get("increase-naturally"), (double) serial.get("conversion-scale"));

		econ.interestEnabled = (boolean) serial.get("interest");
		
		return econ;
	}
	
	// Other
	
	/**
	 * Whether or not this Economy supports interest
	 * @return true if allows, else false
	 */
	public boolean hasInterest() {
		return section.getBoolean("interest");
	}
	
	/**
	 * Fetch a list of all economies supporting interest
	 * @return List of Interest Economies
	 */
	public static List<Economy> getInterestEconomies() {
		return getEconomies().stream().filter(econ -> econ.hasInterest()).toList();
	}
	
	/**
	 * Set the interest acception state of this economy
	 * @param interest Whether or not to allow interest
	 */
	public void setInterest(boolean interest) {
		this.interestEnabled = interest;
		setValues();
	}

	private void setValues() {
		section.set("interest", this.interestEnabled);

		section.set("name", this.name);
		section.set("icon", this.icon);
		section.set("symbol", this.symbol);
		section.set("increase-naturally", this.hasNaturalIncrease);
		section.set("conversion-scale", this.conversionScale);
	}
	
	/**
	 * Remove an Economy from the Plugin
	 * @param name Name of the Economy
	 * @see Economy#removeEconomy(Economy)
	 */
	public static void removeEconomy(String name) {
		removeEconomy(getEconomy(name));
	}
	
	/**
	 * Remove an Economy from the Plugin
	 * @param econ Economy to remove
	 * @throws IllegalArgumentException if economy is null
	 */
	public static void removeEconomy(Economy econ) throws IllegalArgumentException {
		if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
		
		FileConfiguration config = Novaconomy.getEconomiesFile();

		config.set(econ.getEconomySection().getCurrentPath(), null);
		for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
			NovaPlayer np = new NovaPlayer(p);
			FileConfiguration playerConfig = np.getPlayerConfig();

			playerConfig.set(econ.getName(), null);
			
			try {
				playerConfig.save(new File(JavaPlugin.getPlugin(Novaconomy.class).getDataFolder().getPath() + "/players", p.getUniqueId().toString() + ".yml"));
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
	
	/**
	 * Fetch an Economy 
	 * @param name Name of the Economy
	 * @return Found Economy, or null if none found
	 */
	public static Economy getEconomy(String name) {
		ConfigurationSection section = Novaconomy.getEconomiesFile().getConfigurationSection(name.toLowerCase());
		if (section != null) {
			return new Economy(section, section.getString("name"), section.getItemStack("icon"), section.getString("symbol").charAt(0), section.getBoolean("increase-naturally"), section.getDouble("conversion-scale"));
		} else return null;
	}
	
	/**
	 * Get the Symbol of this Economy
	 * @return {@link Character} representing this icon
	 */
	public final char getSymbol() {
		return this.symbol;
	}
	
	/**
	 * Whether or not this economy will naturally increase (not the same as Interest)
	 * <p>
	 * An economy increasing naturally means that it increases from NaturalCauses (i.e. Mining, Fishing). Specific events can be turned off in the configuration.
	 * <p>
	 * Death Decrease applies to ALL Economies, so turn it off globally if you don't want that.
	 * @return true if naturally increases, else false
	 */
	public final boolean hasNaturalIncrease() {
		return this.hasNaturalIncrease;
	}
	
	/**
	 * Return the scale of which this economy will be converted to a different economy
	 * <p>
	 * An economy with a conversion scale of {@code 1} and another with a conversion scale of {@code 0.5} would have a 2:1 ratio, meaning that 100 in the first economy would be 200 in the second economy.
	 * @return conversion scale of this economy
	 */
	public final double getConversionScale() {
		return this.conversionScale;
	}
	
	/**
	 * Fetch a list of all economies registered on the Plugin
	 * @return List of Registered Economies
	 */
	public static final List<Economy> getEconomies() {
		List<Economy> economies = new ArrayList<>();
		
		Novaconomy.getEconomiesFile().getValues(false).values().forEach(obj -> {
			if (obj instanceof ConfigurationSection s) {
				economies.add(getEconomy(s.getName()));
			}
		});

		return economies;
	}
	
	/**
	 * Fetch a list of economies that increase naturally
	 * @return List of Economies that increase naturally
	 */
	public static final List<Economy> getNaturalEconomies() {
		return getEconomies().stream().filter(econ -> econ.hasNaturalIncrease()).toList();
	}
	
	/**
	 * Return the ConfigurationSection that this Economy is stored in
	 * @return {@link ConfigurationSection} of this economy
	 */
	public final ConfigurationSection getEconomySection() {
		return this.section;
	}
	
	/**
	 * Fetch the name of this economy
	 * @return Name of this economy
	 */
	public final String getName() {
	 	return this.name;
	}
	
	/**
	 * Get the Icon of this Economy
	 * @return Icon of this Economy
	 */
	public final ItemStack getIcon() {
		return this.icon;
	}
	
	/**
	 * Convert this economy to another economy
	 * @param to The New Economy to convert to
	 * @param fromAmount How much amount is to be converted
	 * @return Converted amount in the other economy's form
	 * @throws IllegalArgumentException if to or from is null, or economies are identical
	 * @see Economy#convertAmount(Economy, Economy, double)
	 */
	public final double convertAmount(Economy to, double fromAmount) throws IllegalArgumentException {
		return convertAmount(this, to, fromAmount);
	}
	
	/**
	 * Convert one economy's balance to another
	 * @param from The Economy to convert from
	 * @param to The Economy to convert to
	 * @param fromAmount How much amount is to be converted
	 * @return Converted amount in the other economy's form
	 * @throws IllegalArgumentException if to or from is null, or economies are identical
	 */
	public static final double convertAmount(Economy from, Economy to, double fromAmount) throws IllegalArgumentException {
		if (from == null) throw new IllegalArgumentException("Economy from is null");
		if (to == null) throw new IllegalArgumentException("Economy to is null");
		if (from.getName().equals(to.getName())) throw new IllegalArgumentException("Economies are identical");
		double scale = from.getConversionScale() / to.getConversionScale();

		return fromAmount * scale;
	}
	
	/**
	 * Fetch a Builder used for creating economies
	 * @return {@link Builder} Class
	 */
	public static Economy.Builder builder() {
		return new Economy.Builder();
	}
	
	/**
	 * Class used for Creating Economies
	 * @see Economy#builder()
	 */
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
		
		/**
		 * Set the Symbol of this Economy
		 * @param symbol Symbol of this economy
		 * @return Builder Class, for chaining
		 */
		public Builder setSymbol(char symbol) {
			this.symbol = symbol;
			return this;
		}
		
		/**
		 * Set the name of this Economy
		 * @param name Name of the Economy
		 * @return Builder Class, for chaining
		 */
		public Builder setName(String name) {
			this.name = name;
			return this;
		}
		
		/**
		 * Set the Icon of this Economy
		 * @param iconM Icon Material
		 * @return Builder Class, for chaining
		 * @see Builder#setIcon(ItemStack)
		 */
		public Builder setIcon(Material iconM) {
			ItemStack icon = new ItemStack(iconM);
			ItemMeta meta = icon.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + this.name);
			icon.setItemMeta(meta);

			this.icon = icon;
			
			return this;
		}
		
		/**
		 * Set the Icon of this Economy
		 * @param stack {@link ItemStack} for this economy
		 * @return Builder Class, for chaining
		 */
		public Builder setIcon(ItemStack stack) {
			this.icon = stack;
			return this;
		}
		
		/**
		 * Set whether or not this economy increases naturally (i.e. Increases from Mining)
		 * @param increaseNaturally Whether or not this economy should increase from Natural Causes
		 * @return Builder Class, for chaining
		 */
		public Builder setIncreaseNaturally(boolean increaseNaturally) {
			this.increaseNaturally = increaseNaturally;
			return this;
		}
		
		/**
		 * Set the Conversion Scale (used to convert the value to other economies)
		 * @param scale Scale of this economy
		 * @return Builder Class, for chaining
		 */
		public Builder setConversionScale(double scale) {
			this.conversionScale = scale;
			return this;
		}
		
		/**
		 * Builds this economy
		 * @return Created Economy
		 * @throws IllegalArgumentException if name is null (icon's default is Gold Ingot)
		 * @throws UnsupportedOperationException if already exists
		 */
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