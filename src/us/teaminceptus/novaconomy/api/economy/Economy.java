package us.teaminceptus.novaconomy.api.economy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import us.teaminceptus.novaconomy.Novaconomy;

public class Economy {

	private final char symbol;
	private final ConfigurationSection section;
	private final String name;
	private final ItemStack icon;
	private final boolean hasNaturalIncrease;

	private Economy(ConfigurationSection section, String name, ItemStack icon, char symbol, boolean naturalIncrease) {
		this.symbol = symbol;
		this.section = section;
		this.name = name;
		this.icon = icon;
		this.hasNaturalIncrase = naturalIncrease;

		if (!(section.isString("name"))) {
			section.set("name", this.name);
		}

		if (!(section.isItemStack("icon"))) {
			section.set("icon", this.icon);
		}

		if (!(section.get("symbol") instanceof Character)) {
			section.set("symbol", this.symbol);
		}

		if (!(section.isBoolean("increase-naturally"))) {
			section.set("increase-naturally", this.hasNaturalIncrase);
		}
	}

	public static Economy getEconomy(String name) {
		ConfigurationSection section = Novaconomy.getEconomiesFile().getConfigurationSection(name.toLowerCase());
		if (section != null) {
			return new Economy(section, section.getString("name"), section.getItemStack("icon"), (char) section.get("symbol"));
		} else return null;
	}

	public final char getSymbol() {
		return this.symbol;
	}

	public final boolean hasNaturalIncrease() {
		return this.hasNaturalIncrease;
	}

	public static final List<Economy> getEconomies() {
		List<Economy> economies = new ArrayList<>();
		
		Novaconomy.getEconomiesFile().getValues().values().forEach(obj -> {
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

	public static Economy.Builder builder() {
		return new Economy.Builder();
	}

	public static final class Builder {
		char symbol;
		String name;
		ItemStack icon;
		boolean increaseNaturally;
		
		private Builder() {
			this.icon = new ItemStack(Material.GOLD_INGOT);
			this.increaseNaturally = true;
		}

		public Builder setSymbol(char symbol) {
			this.symbol = symbol;
			return this;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public Builder setIcon(ItemStack icon) {
			this.icon = icon;
			return this;
		}

		public Builder setIncreaseNaturally(boolean increaseNaturally) {
			this.increaseNaturally = increaseNaturally;
			return this;
		}

		public Economy build() throws IllegalArgumentException, UnsupportedOperationException {
			if (this.name == null) throw new IllegalArgumentException("Name cannot be null");
			if (this.symbol == null) throw new IllegalArgumentException("Symbol cannot be null");

			if (Novaconomy.getEconomiesFile().getConfigurationSection(this.name.toLowerCase()) != null)
			throw new UnsupportedOperationException("Economy already exists");

			ConfigurationSection es = Novaconomy.getEconomiesFile().createSection(this.name.toLowerCase());

			es.set("name", this.name);
			es.set("icon", this.symbol);
			es.set("symbol", this.symbol);

			Economy econ = new Economy(es, this.name, this.icon, this.symbol);
		}
	}

}