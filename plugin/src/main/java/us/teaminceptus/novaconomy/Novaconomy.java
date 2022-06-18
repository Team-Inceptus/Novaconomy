package us.teaminceptus.novaconomy;

import com.cryptomorin.xseries.XSound;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.apache.commons.lang.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimpleBarChart;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.InterestEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessProductAddEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessProductRemoveEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessStockEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class representing this Plugin
 * @see NovaConfig
 */
public class Novaconomy extends JavaPlugin implements NovaConfig {

	public static final Random r = new Random();
	private static final Wrapper wr = getWrapper();
	private static File playerDir;
	private static FileConfiguration economiesFile;
	
	private static FileConfiguration config;
	private static ConfigurationSection interest;
	private static ConfigurationSection ncauses;

	private static String prefix;

	public static String get(String key) {
		String lang = NovaConfig.getConfiguration().getLanguage();
		return Language.getById(lang).getMessage(key);
	}

	public static String getMessage(String key) { return prefix + get(key); }

	private static void updateInterest() {
		Novaconomy plugin = getPlugin(Novaconomy.class);

		config = plugin.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		if (INTEREST_RUNNABLE.getTaskId() != -1) INTEREST_RUNNABLE.cancel();

		INTEREST_RUNNABLE = new BukkitRunnable() {
			@Override
			public void run() {
				if (!(NovaConfig.getConfiguration().isInterestEnabled())) cancel();

				Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
				Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();

				for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
					NovaPlayer np = new NovaPlayer(p);

					Map<Economy, Double> previousBal = new HashMap<>();
					Map<Economy, Double> amount = new HashMap<>();
					for (Economy econ : Economy.getInterestEconomies()) {
						double balance = np.getBalance(econ);
						double add = (balance * (NovaConfig.getConfiguration().getInterestMultiplier() - 1)) / econ.getConversionScale();

						previousBal.put(econ, balance);
						amount.put(econ, add);
					}

					previousBals.put(np, previousBal);
					amounts.put(np, amount);
				}

				InterestEvent event = new InterestEvent(previousBals, amounts);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) for (NovaPlayer np : previousBals.keySet()) {
					int i = 0;
					for (Economy econ : previousBals.get(np).keySet()) {
						np.add(econ, amounts.get(np).get(econ));
						i++;
					}

					if (np.isOnline() && NovaConfig.getConfiguration().hasNotifications())
						np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.interest"), i + "", (i == 1 ? get("constants.economy") : get("constants.economies"))));
				}
			}

		};

		new BukkitRunnable() {
			@Override
			public void run() {
				INTEREST_RUNNABLE.runTaskTimer(plugin, plugin.getIntervalTicks(), plugin.getIntervalTicks());
			}
		}.runTask(plugin);

	}

	private class Events implements Listener {
		
		private final Novaconomy plugin;
		
		protected Events(Novaconomy plugin) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
			this.plugin = plugin;
		}

		@EventHandler
		public void claimCheck(PlayerInteractEvent e) {
			if (e.getItem() == null) return;
			if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
			Player p = e.getPlayer();
			NovaPlayer np = new NovaPlayer(p);
			Wrapper wrapper = wr;
			ItemStack item = e.getItem();
			if (!wr.hasID(item)) return;
			if (!wr.getID(item).equalsIgnoreCase("economy:check")) return;

			Economy econ = Economy.getEconomy(UUID.fromString(wrapper.getNBTString(item, "economy")));
			double amount = wrapper.getNBTDouble(item, "amount");

			np.add(econ, amount);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
					else p.setItemInHand(null);
				}
			}.runTask(plugin);
			XSound.ENTITY_ARROW_HIT_PLAYER.play(p);
		}
		
		@EventHandler
		public void moneyIncrease(EntityDamageByEntityEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasKillIncrease())) return;
			if (!(e.getDamager() instanceof Player)) return;
			Player p = (Player) e.getDamager();
			if (!(e.getEntity() instanceof LivingEntity)) return;
			LivingEntity en = (LivingEntity) e.getEntity();
			if (en.getHealth() - e.getFinalDamage() > 0) return;

			update(p, en.getMaxHealth());
		}
		
		@EventHandler
		public void moneyIncrease(BlockBreakEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasMiningIncrease())) return;
			if (e.getBlock().getDrops().size() < 1) return;
			
			Block b = e.getBlock();
			Player p = e.getPlayer();
			
			if (!(ores.contains(b.getType()))) return;

			update(p, e.getExpToDrop());
		}
		
		@EventHandler
		public void moneyIncrease(PlayerFishEvent e) {
			if (e.isCancelled()) return;
			if (!(plugin.hasFishingIncrease())) return;
			
			if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
			
			Player p = e.getPlayer();
			update(p, e.getExpToDrop());
		}

		private void update(Player p, double amount) {
			NovaPlayer np = new NovaPlayer(p);
			List<String> added = new ArrayList<>();

			for (Economy econ : Economy.getNaturalEconomies()) {
				double divider = r.nextInt(2) + 1;
				double increase = ((amount + r.nextInt(8) + 1) / divider) / econ.getConversionScale();

				double previousBal = np.getBalance(econ);

				PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);

				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					np.add(econ, increase);

					String message = COLORS.get(r.nextInt(COLORS.size())) + "+" + (Math.floor(increase * 100) / 100 + econ.getSymbol() + "").replace("D", "") + econ.getSymbol();
					added.add(message);
				}
			}

			XSound.BLOCK_NOTE_BLOCK_PLING.play(p, 3F, 2F);
			if (plugin.hasNotifications()) wr.sendActionbar(p, String.join(ChatColor.YELLOW + ", " + ChatColor.RESET, added.toArray(new String[0])));
		}
		
		@EventHandler
		public void moneyDecrease(PlayerDeathEvent e) {
			if (!(plugin.hasDeathDecrease())) return;
			
			Player p = e.getEntity();
			NovaPlayer np = new NovaPlayer(p);
			
			List<String> lost = new ArrayList<>();
			
			lost.add(get("constants.lost"));
			
			for (Economy econ : Economy.getEconomies()) {
				double amount = np.getBalance(econ) / getDeathDivider();
				np.remove(econ, amount);
				
				lost.add(ChatColor.DARK_RED + "- " + ChatColor.RED + econ.getSymbol() + Math.floor(amount * 100) / 100);
			}
			
			if (plugin.hasNotifications()) p.sendMessage(String.join("\n", lost.toArray(new String[0])));
		}

		// Inventory

		@EventHandler
		public void click(InventoryClickEvent e) {
			Inventory inv = e.getClickedInventory();
			if (inv == null) return;
			if (inv instanceof PlayerInventory) return;
			if (inv.getHolder() != null && inv.getHolder() instanceof Wrapper.CancelHolder) e.setCancelled(true);

			if (e.getCurrentItem() == null) return;
			ItemStack item = e.getCurrentItem();

			if (item.isSimilar(wr.getGUIBackground())) e.setCancelled(true);
			if (!item.hasItemMeta()) return;

			String id = wr.getNBTString(item, "id");
			if (!e.isCancelled()) e.setCancelled(true);
			if (id.length() > 0 && CLICK_ITEMS.containsKey(id)) CLICK_ITEMS.get(id).accept(e);
		}

		@EventHandler
		public void close(InventoryCloseEvent e) {
			Inventory inv = e.getInventory();
			if (inv == null) return;

			if (inv.getHolder() != null) {
				InventoryHolder holder = inv.getHolder();
				if (holder instanceof CommandWrapper.ReturnItemsHolder) {
					CommandWrapper.ReturnItemsHolder h = (CommandWrapper.ReturnItemsHolder) holder;
					Player p = h.player();

					if (h.added()) return;

					for (ItemStack i : inv.getContents()) {
						if (i == null) continue;
						if (h.ignoreIds().contains(wr.getID(i))) continue;

						if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), i);
						else p.getInventory().addItem(i);
					}
				}
			}
		}

		@EventHandler
		public void move(InventoryMoveItemEvent e) {
			if (e.getItem() == null) return;
			ItemStack item = e.getItem();
			Inventory inv = e.getInitiator();

			if (item.isSimilar(wr.getGUIBackground())) e.setCancelled(true);
			if (inv.getHolder() != null && inv.getHolder() instanceof Wrapper.CancelHolder) e.setCancelled(true);

			String id = wr.getNBTString(item, "id");
			if (id.length() > 0 && CLICK_ITEMS.containsKey(id)) e.setCancelled(true);
		}
	}

	private static final List<ChatColor> COLORS = Arrays.stream(ChatColor.values()).filter(ChatColor::isColor).collect(Collectors.toList());
	
	private static final Map<String, Consumer<InventoryClickEvent>> CLICK_ITEMS = new HashMap<String, Consumer<InventoryClickEvent>>() {{
			put("economy_scroll", e -> {
				ItemStack item = e.getCurrentItem();
				List<Economy> economies = Economy.getEconomies().stream().sorted().collect(Collectors.toList());
				Inventory inv = e.getClickedInventory();

				String econ = ChatColor.stripColor(item.getItemMeta().getDisplayName());
				int index = economies.indexOf(Economy.getEconomy(econ)) + 1;
				Economy newEcon = economies.get(index >= economies.size() ? 0 : index);

				inv.setItem(e.getSlot(), newEcon.getIcon());
			});

			put("business", e -> {
				ItemStack item = e.getCurrentItem();
				String name = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
				Business b = Business.getByName(name);
				e.getWhoClicked().openInventory(wr.generateBusinessData(b));
			});

			put("product:buy", e -> {
				ItemStack item = e.getCurrentItem();
				String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(item.getType().name().replace('_', ' '));
				HumanEntity p = e.getWhoClicked();

				if (!wr.getNBTBoolean(item, "product:in_stock")) {
					p.sendMessage(String.format(Novaconomy.get("error.business.not_in_stock"), name));
					return;
				}

				List<BusinessProduct> products = new ArrayList<>();
				BusinessProduct pr = (BusinessProduct) wr.getNBTProduct(item, "product");

				Inventory inv = wr.genGUI(27, WordUtils.capitalizeFully(Novaconomy.get("constants.purchase")) + " \"" + ChatColor.RESET + name + ChatColor.RESET + "\"?", new Wrapper.CancelHolder());
				for (int i = 10; i < 17; i++) inv.setItem(i, wr.getGUIBackground());

				inv.setItem(13, item);

				ItemStack yes = Items.yes("buy_product").clone();
				yes = wr.setNBT(yes, "product", pr);
				yes = wr.setNBT(yes, "price", pr.getPrice().getAmount());
				inv.setItem(21, yes);

				ItemStack cancel = Items.cancel("no_product").clone();
				inv.setItem(23, cancel);

				p.openInventory(inv);
				XSound.BLOCK_CHEST_OPEN.play(p, 3F, 0F);
			});

			put("no:close", e -> {
				HumanEntity en = e.getWhoClicked();
				en.closeInventory();
			});

			put("no:no_product", e -> {
				HumanEntity en = e.getWhoClicked();
				en.closeInventory();
				en.sendMessage(Novaconomy.get("cancel.business.purchase"));
				XSound.BLOCK_NOTE_BLOCK_PLING.play(en, 3F, 0F);
			});

			put("economy:wheel", e -> {
				int slot = e.getRawSlot();
				ItemStack item = e.getCurrentItem().clone();

				List<String> sortedList = new ArrayList<>();
				Economy.getEconomies().forEach(econ -> sortedList.add(econ.getName()));
				sortedList.sort(String.CASE_INSENSITIVE_ORDER);

				Economy econ = Economy.getEconomy(wr.getNBTString(item, "economy"));
				int nextI = sortedList.indexOf(econ.getName()) + 1;
				Economy next = sortedList.size() == 1 ? econ : Economy.getEconomy(sortedList.get(nextI == sortedList.size() ? 0 : nextI));

				item.setType(next.getIcon().getType());
				item = wr.setNBT(item, "economy", next.getName().toLowerCase());

				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(ChatColor.GOLD + next.getName());
				item.setItemMeta(meta);

				e.getView().setItem(slot, item);
				XSound.BLOCK_NOTE_BLOCK_PLING.play(e.getWhoClicked());
			});

			put("economy:wheel:add_product", e -> {
				get("economy:wheel").accept(e);

				int slot = e.getRawSlot();
				ItemStack item = e.getCurrentItem();
				Economy econ = Economy.getEconomy(wr.getNBTString(item, "economy"));

				Inventory inv = e.getClickedInventory();

				ItemStack confirm = inv.getItem(23);
				confirm = wr.setNBT(confirm, "economy", econ.getName().toLowerCase());
				inv.setItem(23, confirm);

				ItemStack display = inv.getItem(13);
				ItemMeta dMeta = display.getItemMeta();
				dMeta.setLore(Collections.singletonList(String.format(Novaconomy.get("constants.business.price"), wr.getNBTDouble(display, "price"), econ.getSymbol())));
				display.setItemMeta(dMeta);
				inv.setItem(13, display);
			});

			put("yes:buy_product", e -> {
				if (!(e.getWhoClicked() instanceof Player)) return;

				ItemStack item = e.getCurrentItem();
				Player p = (Player) e.getWhoClicked();

				if (p.getInventory().firstEmpty() == -1) {
					p.sendMessage(Novaconomy.get("error.player.full_inventory"));
					return;
				}

				NovaPlayer np = new NovaPlayer(p);
				BusinessProduct bP = (BusinessProduct) wr.getNBTProduct(item, "product");

				if (!np.canAfford(bP)) {
					p.sendMessage(String.format(Novaconomy.get("error.economy.invalid_amount"), Novaconomy.get("constants.purchase")));
					p.closeInventory();
					return;
				}

				ItemStack product = bP.getItem();

				Economy econ = bP.getEconomy();
				double amount = bP.getPrice().getAmount();

				np.remove(econ, amount);
				bP.getBusiness().removeResource(product);
				p.getInventory().addItem(product);
				String material = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(product.getType().name().replace('_', ' '));

				p.sendMessage(String.format(Novaconomy.get("success.business.purchase"), material, bP.getBusiness().getName()));
				p.closeInventory();
				XSound.ENTITY_ARROW_HIT_PLAYER.play(p);

				NovaPlayer owner = new NovaPlayer(bP.getBusiness().getOwner());
				owner.add(econ, amount);

				if (owner.isOnline() && NovaConfig.getConfiguration().hasNotifications()) {
					String name = p.getDisplayName() == null ? p.getName() : p.getDisplayName();
					Player bOwner = owner.getOnlinePlayer();
					bOwner.sendMessage(String.format(Novaconomy.get("notification.business.purchase"), name, material));
					XSound.ENTITY_ARROW_HIT_PLAYER.play(bOwner, 3F, 2F);
				}

				PlayerPurchaseProductEvent event = new PlayerPurchaseProductEvent(p, bP);
				Bukkit.getPluginManager().callEvent(event);
			});

			put("business:add_product", e -> {
				if (!(e.getWhoClicked() instanceof Player)) return;
				Player p = (Player) e.getWhoClicked();
				Business b = Business.getByOwner(p);
				ItemStack item = e.getCurrentItem();

				double price = wr.getNBTDouble(item, "price");
				Economy econ = Economy.getEconomy(wr.getNBTString(item, "economy"));
				ItemStack product = wr.normalize(wr.getNBTItem(item, "item"));

				Product pr = new Product(product, econ, price);

				BusinessProductAddEvent event = new BusinessProductAddEvent(new BusinessProduct(pr, b));
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(product.getType().name().replace('_', ' '));
					p.sendMessage(String.format(getMessage("success.business.add_product"), name));
					p.closeInventory();
					Product added = new Product(pr.getItem(), pr.getPrice());
					b.addProduct(added);
				}
			});

			put("business:add_resource", e -> {
				if (!(e.getWhoClicked() instanceof Player)) return;
				Player p = (Player) e.getWhoClicked();
				Business b = Business.getByOwner(p);
				InventoryView view = e.getView();
				Inventory inv = view.getTopInventory();
				CommandWrapper.ReturnItemsHolder h = (CommandWrapper.ReturnItemsHolder) inv.getHolder();
				h.added(true);

				List<ItemStack> res = new ArrayList<>();
				for (ItemStack i : inv.getContents()) {
					if (i == null) continue;
					res.add(i.clone());
				}

				List<ItemStack> extra = new ArrayList<>();
				List<ItemStack> resources = new ArrayList<>();

				// Remove Non-Products
				for (ItemStack item : res) {
					if (item == null) continue;
					if (wr.getID(item).equalsIgnoreCase("business:add_resource")) continue;

					if (b.isProduct(item)) resources.add(item);
					else extra.add(item);
				}

				b.addResource(resources);
				extra.forEach(i -> {
					if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), i);
					else p.getInventory().addItem(i);
				});

				p.sendMessage(String.format(getMessage("success.business.add_resource"), b.getName()));
				p.closeInventory();

				BusinessStockEvent event = new BusinessStockEvent(b, p, extra, resources);
				Bukkit.getPluginManager().callEvent(event);
			});

			put("product:remove", e -> {
				if (!(e.getWhoClicked() instanceof Player)) return;
				Player p = (Player) e.getWhoClicked();
				Business b = Business.getByOwner(p);
				ItemStack item = e.getCurrentItem();

				BusinessProduct pr = (BusinessProduct) wr.getNBTProduct(item, "product");
				ItemStack product = pr.getItem();

				b.removeProduct(pr);
				List<ItemStack> stock = new ArrayList<>(pr.getBusiness().getResources()).stream()
						.filter(product::isSimilar)
						.collect(Collectors.toList());

				b.removeResource(stock);
				String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(product.getType().name().replace('_', ' '));

				p.sendMessage(String.format(Novaconomy.get("success.business.remove_product"), name, b.getName()));

				stock.forEach(i -> {
					if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), i);
					else p.getInventory().addItem(i);
				});

				p.closeInventory();

				BusinessProductRemoveEvent event = new BusinessProductRemoveEvent(pr);
				Bukkit.getPluginManager().callEvent(event);
			});
		}
	};

	private static final Set<Material> ores = new HashSet<>(Arrays.stream(Material.values()).filter(m -> m.name().endsWith("ORE") || m.name().equalsIgnoreCase("ANCIENT_DEBRIS")).collect(Collectors.toSet()));

	private static CommandWrapper getCommandWrapper() {
		try {
			final int wrapperVersion;

			String dec;
			String k = "CommandVersion";

			if (funcConfig.isInt(k)) {
				int i = funcConfig.getInt(k, 3);
				dec = i > 2 || i < 1 ? "auto" : i + "";
			} else
				dec = !funcConfig.getString(k, "auto").equalsIgnoreCase("auto") ? "auto" : funcConfig.getString(k, "auto");

			int tempV;
			try {
				if (dec.equalsIgnoreCase("auto")) tempV = wr.getCommandVersion();
				else tempV = Integer.parseInt(dec);
			} catch (IllegalArgumentException e) { tempV = wr.getCommandVersion(); }

			wrapperVersion = tempV;
			return (CommandWrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".CommandWrapperV" + wrapperVersion).getConstructor(Plugin.class).newInstance(JavaPlugin.getPlugin(Novaconomy.class));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getServerVersion() {
		return  Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
	}

	private static Wrapper getWrapper() {
		try {
			return (Wrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".Wrapper" + getServerVersion()).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static BukkitRunnable INTEREST_RUNNABLE = new BukkitRunnable() {
		@Override
		public void run() {
			if (!(NovaConfig.getConfiguration().isInterestEnabled())) cancel();
			
			Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
			Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();
			
			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
				NovaPlayer np = new NovaPlayer(p);
				
				Map<Economy, Double> previousBal = new HashMap<>();
				Map<Economy, Double> amount = new HashMap<>();
				for (Economy econ : Economy.getInterestEconomies()) {
					double balance = np.getBalance(econ);
					double add = (balance * (NovaConfig.getConfiguration().getInterestMultiplier() - 1)) / econ.getConversionScale();
					
					previousBal.put(econ, balance);
					amount.put(econ, add);
				}
				
				previousBals.put(np, previousBal);
				amounts.put(np, amount);
			}
			
			InterestEvent event = new InterestEvent(previousBals, amounts);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) for (NovaPlayer np : previousBals.keySet()) {
				int i = 0;
				for (Economy econ : previousBals.get(np).keySet()) {
					np.add(econ, amounts.get(np).get(econ));
					i++;
				}

				if (np.isOnline() && NovaConfig.getConfiguration().hasNotifications())
					np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.interest"), i + "", i == 1 ? get("constants.economy") : get("constants.economies")));
			}
		}
		
	};

	private static FileConfiguration funcConfig;

	private static final List<Class<? extends ConfigurationSerializable>> SERIALIZABLE = new ArrayList<Class<? extends ConfigurationSerializable>>() {{
		add(Economy.class);
		add(Business.class);
		add(Price.class);
		add(Product.class);
		add(BusinessProduct.class);
	}};

	@Override
	public void onEnable() {
		saveDefaultConfig();
		saveConfig();

		funcConfig = NovaConfig.loadFunctionalityFile();
		File businesses = new File(getDataFolder(), "businesses.yml");
		if (!businesses.exists()) saveResource("businesses.yml", false);

		NovaConfig.reloadLanguages();
		getLogger().info("Loaded Languages & Configuration...");
		SERIALIZABLE.forEach(ConfigurationSerialization::registerClass);

		NovaConfig.loadBusinesses();

		playerDir = new File(getDataFolder(), "players");
		File economyFile = new File(getDataFolder(), "economies.yml");

		try {
			if (!(economyFile.exists())) economyFile.createNewFile();
			if (!(playerDir.exists())) playerDir.mkdir();
		} catch (IOException e) {
			getLogger().severe("Error loading files & folders");
			e.printStackTrace();
		}

		economiesFile = YamlConfiguration.loadConfiguration(economyFile);
		config = this.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		prefix = get("plugin.prefix");

		getLogger().info("Loaded Files...");

		getCommandWrapper();
		new Events(this);

		reloadValues();
		
		INTEREST_RUNNABLE.runTaskTimer(this, getIntervalTicks(), getIntervalTicks());

		getLogger().info("Loaded Core Functionality...");

		new UpdateChecker(this, UpdateCheckSource.SPIGOT, "100503")
				.setDownloadLink("https://www.spigotmc.org/resources/novaconomy.100503/")
				.setNotifyOpsOnJoin(true)
				.setChangelogLink("https://github.com/Team-Inceptus/Novaconomy/releases/")
				.setUserAgent("Java 8 Novaconomy User Agent")
				.checkEveryXHours(1)
				.checkNow();

		Metrics metrics = new Metrics(this, PLUGIN_ID);

		metrics.addCustomChart(new SimplePie("used_language", () -> Language.getById(this.getLanguage()).name()));
		metrics.addCustomChart(new SimpleBarChart("used_symbols", () -> {
			Map<String, Integer> map = new HashMap<>();
			for (Economy econ : Economy.getEconomies()) {
				String key = econ.getSymbol() + "";
				if (map.containsKey(key)) map.put(key, map.get(key) + 1);
				else map.put(key, map.get(key) + 1);
			}

			return map;
		}));
		metrics.addCustomChart(new SimpleBarChart("used_business_items", () -> {
			Map<String, Integer> map = new HashMap<>();
			for (Business b : Business.getBusinesses())
				for (BusinessProduct p : b.getProducts()) {
					String name = p.getItem().getType().name();
					if (map.containsKey(name)) map.put(name, map.get(name) + 1);
					else map.put(name, 1);
				}

			return map;
		}));
		metrics.addCustomChart(new SimplePie("command_version", () -> getWrapper().getCommandVersion() + ""));

		getLogger().info("Loaded Dependencies...");
		saveConfig();
		getLogger().info("Successfully loaded Novaconomy");
	}

	private static final int PLUGIN_ID = 15322;

	public static boolean isLegacy() { return wr.isLegacy(); }

	private void reloadValues() {
		NovaConfig.loadConfig();
	}
	
	public static File getPlayerDirectory() {
		return playerDir;
	}

	public static FileConfiguration getEconomiesFile() {
		return economiesFile;
	}
	
	@Override
	public long getIntervalTicks() { return interest.getLong("IntervalTicks"); }

	@Override
	public boolean isInterestEnabled() { return interest.getBoolean("Enabled"); }

	@Override
	public void setInterestEnabled(boolean enabled) { interest.set("Enabled", enabled); saveConfig(); }

	@Override
	public boolean hasMiningIncrease() { return ncauses.getBoolean("MiningIncrease"); }

	@Override
	public boolean hasFishingIncrease() { return ncauses.getBoolean("FishingIncrease"); }

	@Override
	public boolean hasKillIncrease() { return ncauses.getBoolean("KillIncrease"); }

	@Override
	public String getLanguage() { return config.getString("Language"); }


	@Override
	public boolean hasDeathDecrease() { return ncauses.getBoolean("DeathDecrease"); }

	@Override
	public boolean hasFarmingIncrease() { return ncauses.getBoolean("FarmingIncrease"); }

	@Override
	public double getInterestMultiplier() { return interest.getDouble("ValueMultiplier"); }

	@Override
	public void setInterestMultiplier(double multiplier) {
		interest.set("ValueMultiplier", multiplier);
		saveConfig();
	}

	@Override
	public int getMiningChance() {
		return ncauses.getInt("MiningIncreaseChance");
	}

	@Override
	public int getFishingChance() {
		return ncauses.getInt("FishingIncreaseChance");
	}

	@Override
	public int getKillChance() {
		return ncauses.getInt("KillIncreaseChance");
	}

	@Override
	public int getFarmingChance() {
		return ncauses.getInt("FarmingIncreaseChance");
	}

	@Override
	public void setKillChance(int chance) {
		ncauses.set("KillIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setFishingChance(int chance) {
		ncauses.set("FishingIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setMiningChance(int chance) {
		ncauses.set("MiningChanceIncrease", chance);
		saveConfig();
	}

	@Override
	public void setFarmingChance(int chance) {
		ncauses.set("FarmingIncreaseChance", chance);
		saveConfig();
	}

	@Override
	public void setFarmingIncrease(boolean increase) {
		ncauses.set("FarmingIncrease", increase);
		saveConfig();
	}

	@Override
	public void setMiningIncrease(boolean increase) {
		ncauses.set("MiningIncrease", increase);
		saveConfig();
	}

	@Override
	public void setKillIncrease(boolean increase) {
		ncauses.set("KillIncrease", increase);
	}

	@Override
	public void setDeathDecrease(boolean decrease) {
		ncauses.set("DeathDecrease", decrease);
		saveConfig();
	}

	@Override
	public boolean hasNotifications() {
		return config.getBoolean("Notifications");
	}

	@Override
	public void setDeathDivider(double divider) {
		ncauses.set("DeathDivider", divider);
		saveConfig();
	}

	@Override
	public double getDeathDivider() {
		return ncauses.getDouble("DeathDivider");
	}

}