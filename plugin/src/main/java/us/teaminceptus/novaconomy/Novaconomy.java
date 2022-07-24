package us.teaminceptus.novaconomy;

import com.cryptomorin.xseries.XSound;
import com.google.common.util.concurrent.AtomicDouble;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.apache.commons.lang.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
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
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.NovaPlayer;
import us.teaminceptus.novaconomy.api.bounty.Bounty;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.AutomaticTaxEvent;
import us.teaminceptus.novaconomy.api.events.InterestEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessProductAddEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessProductRemoveEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessStockEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerMissTaxEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.treasury.TreasuryRegistry;
import us.teaminceptus.novaconomy.vault.VaultRegistry;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class representing this Plugin
 * @see NovaConfig
 */
public final class Novaconomy extends JavaPlugin implements NovaConfig {

	private static final String ECON_TAG = "economy";
	private static final String AMOUNT_TAG = "amount";
	private static final String PRICE_TAG = "price";
	private static final String PRODUCT_TAG = "product";

	/**
	 * Main Novaconomy Constructor
	 * <strong>DO NOT INSTANTIATE THIS WAY</strong>
	 */
	public Novaconomy() { /* Constructor should only be called by Bukkit Plugin Class Loader */}

	private static final SecureRandom r = new SecureRandom();
	private static final Wrapper w = getWrapper();
	private static File playerDir;
	private static FileConfiguration economiesFile;
	
	private static FileConfiguration config;
	private static ConfigurationSection interest;
	private static ConfigurationSection ncauses;

	private static String prefix;

	/**
	 * Fetches a Message from the current activated Language File.
	 * @param key Message Key
	 * @return Found Message, or "Unknown Value" if not found
	 */
	public static String get(String key) {
		String lang = NovaConfig.getConfiguration().getLanguage();
		return Language.getById(lang).getMessage(key);
	}

	/**
	 * Performs an API request to turn an OfflinePlayer's name to an OfflinePlayer object.
	 * @param name OfflinePlayer Name
	 * @return OfflinePlayer Object
	 */
	public static OfflinePlayer getPlayer(String name) {
		return Wrapper.getPlayer(name);
	}

	private static class ModifierReader {

		private static boolean isIgnored(String s) {
			AtomicBoolean state = new AtomicBoolean(false);

			FileConfiguration config = NovaConfig.getPlugin().getConfig();

			Set<String> ignore = config.getStringList("NaturalCauses.Ignore").stream().map(String::toUpperCase).collect(Collectors.toSet());
			state.set(ignore.stream().anyMatch(s::equalsIgnoreCase));

			return state.get();
		}

		private static Map<String, Map<String, Set<Map<Economy, Double>>>> getAllModifiers() throws IllegalArgumentException {
			Map<String, Map<String, Set<Map<Economy, Double>>>> mods = new HashMap<>();
			FileConfiguration config = NovaConfig.getPlugin().getConfig();

			if (config.isConfigurationSection("NaturalCauses.Modifiers")) {
				ConfigurationSection modifiers = config.getConfigurationSection("NaturalCauses.Modifiers");

				modifiers.getKeys(false).forEach(s -> {
					ConfigurationSection modifier = modifiers.getConfigurationSection(s);
					Map<String, Set<Map<Economy, Double>>> map = new HashMap<>();

					modifier.getValues(false).forEach((k, v) -> {
						Set<Map<Economy, Double>> value = new HashSet<>();
						String amount = v.toString();

						if (amount.contains("[") && amount.contains("]")) {
							amount = amount.replaceAll("[\\[\\]]", "").replace(" ", "");
							String[] amounts = amount.split(",");
							for (String am : amounts) {
								if (readString(am) == null) throw new IllegalArgumentException("No valid amount found for " + k + ": " + amount);
								value.add(readString(am));
							}
						} else {
							if (readString(amount) == null) throw new IllegalArgumentException("No valid amount found for " + k + ": " + amount);
							value.add(readString(amount));
						}

						map.put(k.toUpperCase(), value);
					});

					mods.put(s, map);
				});
			}

			return mods;
		}

		private static Map<String, Set<Map<Economy, Double>>> getModifier(String mod) {
			return getAllModifiers().get(mod);
		}

		private static Map<Economy, Double> readString(String s) {
			char s1 = s.charAt(0);
			char s2 = s.charAt(s.length() - 1);

			if (!Economy.exists(s1) && !Economy.exists(s2)) return null;

			String remove = Economy.exists(s1) ? s1 + "" : s2 + "";
			Economy econ = Economy.exists(s1) ? Economy.getEconomy(s1) : Economy.getEconomy(s2);
			double amountD = Double.parseDouble(s.replaceAll("[" + remove + "]", ""));
			return Collections.singletonMap(econ, amountD);
		}

	}


	/**
	 * Fetches a Message from the current language file, formatted with the plugin's prefix in the front.
	 * @param key Message Key
	 * @return Message with current language, or "Unkown Value" if not found.
	 * @see #get(String) 
	 */
	public static String getMessage(String key) { return prefix + get(key); }

	private static void updateRunnables() {
		Novaconomy plugin = getPlugin(Novaconomy.class);

		config = plugin.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		try { if (INTEREST_RUNNABLE.getTaskId() != -1) INTEREST_RUNNABLE.cancel(); } catch (IllegalStateException ignored) {}
		try { if (TAXES_RUNNABLE.getTaskId() != -1) TAXES_RUNNABLE.cancel(); } catch (IllegalStateException ignored) {}

		INTEREST_RUNNABLE = new BukkitRunnable() {
			@Override
			public void run() {
				if (!(NovaConfig.getConfiguration().isInterestEnabled())) { cancel(); return; }
				runInterest();
			}
		};

		TAXES_RUNNABLE = new BukkitRunnable() {
			@Override
			public void run() {
				if (!(NovaConfig.getConfiguration().hasAutomaticTaxes())) { cancel(); return; }
				runTaxes();
			}
		};

		new BukkitRunnable() {
			@Override
			public void run() {
				INTEREST_RUNNABLE.runTaskTimer(plugin, plugin.getInterestTicks(), plugin.getInterestTicks());
				TAXES_RUNNABLE.runTaskTimer(plugin, plugin.getTaxesTicks(), plugin.getTaxesTicks());
			}
		}.runTask(plugin);
	}

	private static final Set<Material> ores = new HashSet<>(Arrays.stream(Material.values()).filter(m -> m.name().endsWith("ORE") || m.name().equalsIgnoreCase("ANCIENT_DEBRIS")).collect(Collectors.toSet()));

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
			Wrapper wrapper = w;
			ItemStack item = e.getItem();
			if (!w.hasID(item)) return;
			if (!w.getID(item).equalsIgnoreCase("economy:check")) return;

			Economy econ = Economy.getEconomy(UUID.fromString(wrapper.getNBTString(item, ECON_TAG)));
			double amount = wrapper.getNBTDouble(item, AMOUNT_TAG);

			np.add(econ, amount);
			new BukkitRunnable() {
				@Override
				public void run() {
					if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
					else p.setItemInHand(null);
				}
			}.runTask(plugin);
			XSound.ENTITY_ARROW_HIT_PLAYER.play(p, 3F, 2F);
		}
		
		@EventHandler
		public void moneyIncrease(EntityDamageByEntityEvent e) {
			if (e.isCancelled()) return;
			if (!plugin.hasKillIncrease()) return;

			final Player p;
			if (e.getDamager() instanceof Tameable) {
				Tameable t = (Tameable) e.getDamager();
				if (t.isTamed() && t.getOwner() instanceof Player) p = (Player) t.getOwner();
				else p = null;
			} else if (e.getDamager() instanceof Player) p = (Player) e.getDamager();
			else p = null;

			if (p == null) return;

			if (!(e.getEntity() instanceof LivingEntity)) return;
			LivingEntity en = (LivingEntity) e.getEntity();
			if (en.getHealth() - e.getFinalDamage() > 0) return;

			String id = en.getType().name();
			if (ModifierReader.isIgnored(id)) return;

			double iAmount = en.getMaxHealth();
			if (p.getEquipment().getItemInHand() != null) {
				ItemStack hand = p.getEquipment().getItemInHand();
				if (hand.hasItemMeta() && hand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_MOBS))
					iAmount += hand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_MOBS) * (r.nextInt(4) + 6);
			}

			if (ModifierReader.getModifier("Killing") != null && r.nextInt(100) < getKillChance()) {
				Map<String, Set<Map<Economy, Double>>> entry = ModifierReader.getModifier("Killing");
				if (entry.containsKey(id)) {
					Set<Map<Economy, Double>> value = entry.get(id);
					List<String> msgs = new ArrayList<>();
					for (Map<Economy, Double> map : value)
						for (Economy econ : map.keySet()) {
							double amount = map.get(econ);
							if (amount <= 0) continue;
							msgs.add(callAddBalanceEvent(p, econ, amount, false));
						}

					sendUpdateActionbar(p, msgs);
				} else update(p, iAmount);
			}
		}
		@EventHandler
		public void moneyIncrease(BlockBreakEvent e) {
			if (e.isCancelled()) return;
			if (!plugin.hasMiningIncrease()) return;
			if (e.getBlock().getDrops().size() < 1) return;

			Block b = e.getBlock();
			Player p = e.getPlayer();

			String id = b.getType().name();
			if (ModifierReader.isIgnored(id)) return;

			double add = e.getExpToDrop() == 0 && ores.contains(b.getType()) ? r.nextInt(3) : e.getExpToDrop();
			if (p.getEquipment().getItemInHand() != null) {
				ItemStack hand = p.getEquipment().getItemInHand();
				if (hand.hasItemMeta() && hand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
					add += hand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS) * (r.nextInt(3) + 4);
			}

			if (!w.isAgeable(b) && ModifierReader.getModifier("Mining") != null && r.nextInt(100) < getMiningChance()) {
				Map<String, Set<Map<Economy, Double>>> entry = ModifierReader.getModifier("Mining");
				if (entry.containsKey(id)) {
					Set<Map<Economy, Double>> value = entry.get(id);
					List<String> msgs = new ArrayList<>();
					for (Map<Economy, Double> map : value)
						for (Economy econ : map.keySet()) {
							double amount = map.get(econ);
							if (amount <= 0) continue;
							msgs.add(callAddBalanceEvent(p, econ, amount, false));
						}

					sendUpdateActionbar(p, msgs);
				} else update(p, add);
			}
			else if (w.isAgeable(b) && ModifierReader.getModifier("Farming") != null && r.nextInt(100) < getFarmingChance()) {
				Map<String, Set<Map<Economy, Double>>> entry = ModifierReader.getModifier("Farming");
				if (entry.containsKey(id)) {
					Set<Map<Economy, Double>> value = entry.get(id);
					List<String> msgs = new ArrayList<>();
					for (Map<Economy, Double> map : value)
						for (Economy econ : map.keySet()) {
							double amount = map.get(econ);
							if (amount <= 0) continue;
							msgs.add(callAddBalanceEvent(p, econ, amount, false));
						}

					sendUpdateActionbar(p, msgs);
				} else update(p, add);
			}
		}
		
		@EventHandler
		public void moneyIncrease(PlayerFishEvent e) {
			if (e.isCancelled()) return;
			if (!plugin.hasFishingIncrease()) return;
			
			if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

			Player p = e.getPlayer();
			String name = e.getCaught() instanceof Item ? ((Item) e.getCaught()).getItemStack().getType().name() : e.getCaught().getType().name();
			if (ModifierReader.isIgnored(name)) return;

			double iAmount = e.getExpToDrop();

			if (p.getEquipment().getItemInHand() != null) {
				ItemStack hand = p.getEquipment().getItemInHand();

				if (hand.hasItemMeta()) {
					if (hand.getItemMeta().hasEnchant(Enchantment.LURE))
						iAmount += hand.getItemMeta().getEnchantLevel(Enchantment.LURE) * (r.nextInt(8) + 6);

					if (hand.getItemMeta().hasEnchant(Enchantment.LUCK))
						iAmount += hand.getItemMeta().getEnchantLevel(Enchantment.LUCK) * (r.nextInt(8) + 6);

				}

			}

			if (ModifierReader.getModifier("Fishing") != null && r.nextInt(100) < getFishingChance()) {
				Map<String, Set<Map<Economy, Double>>> entry = ModifierReader.getModifier("Fishing");
				if (entry.containsKey(name)) {
					Set<Map<Economy, Double>> value = entry.get(name);
					List<String> msgs = new ArrayList<>();
					for (Map<Economy, Double> map : value)
						for (Economy econ : map.keySet()) {
							double amount = map.get(econ);
							if (amount <= 0) continue;
							msgs.add(callAddBalanceEvent(p, econ, amount, false));
						}

					sendUpdateActionbar(p, msgs);
				} else update(p, iAmount);
			}
		}

		private void update(Player p, double amount) {
			List<String> msgs = new ArrayList<>();
			for (Economy econ : Economy.getNaturalEconomies()) msgs.add(callAddBalanceEvent(p, econ, amount, true));

			sendUpdateActionbar(p, msgs);
		}

		private String callAddBalanceEvent(Player p, Economy econ, double amount, boolean random) {
			NovaPlayer np = new NovaPlayer(p);
			double divider = r.nextInt(2) + 1;
			double increase = Math.min(random ? ((amount + r.nextInt(8) + 1) / divider) / econ.getConversionScale() : amount, plugin.getMaxIncrease());

			double previousBal = np.getBalance(econ);

			PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);

			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				np.add(econ, increase);

				return COLORS.get(r.nextInt(COLORS.size())) + "+" + String.format("%,.2f", (Math.floor(increase * 100) / 100)) + econ.getSymbol();
			}

			return "";
		}

		private void sendUpdateActionbar(Player p, List<String> added) {
			XSound.BLOCK_NOTE_BLOCK_PLING.play(p, 3F, 2F);
			if (plugin.hasNotifications()) w.sendActionbar(p, String.join(ChatColor.YELLOW + ", " + ChatColor.RESET, added.toArray(new String[0])));
		}

		private ItemStack getItem(EntityEquipment i, EquipmentSlot s) {
			switch (s) {
				case FEET: return i.getBoots();
				case LEGS: return i.getLeggings();
				case CHEST: return i.getChestplate();
				case HEAD: return i.getHelmet();
				default: return i.getItemInHand();
			}
		}

		@EventHandler
		public void claimBounty(EntityDamageByEntityEvent e) {
			if (!(e.getEntity() instanceof Player)) return;
			if (!(e.getDamager() instanceof Player)) return;
			Player killer = (Player) e.getDamager();
			Player target = (Player) e.getEntity();
			if (target.getHealth() - e.getFinalDamage() > 0) return;
			NovaPlayer nt = new NovaPlayer(target);
			if (nt.getSelfBounties().isEmpty()) return;
			NovaPlayer nk = new NovaPlayer(killer);

			String kName = killer.getDisplayName() == null ? killer.getName() : killer.getDisplayName();
			String tName = target.getDisplayName() == null ? target.getName() : target.getDisplayName();
			boolean broadcast = plugin.isBroadcastingBounties();

			AtomicDouble amount = new AtomicDouble(0);
			AtomicInteger bountyCount = new AtomicInteger(0);
			String key = "bounties." + target.getUniqueId();
			for (Bounty b : nt.getSelfBounties()) {
				FileConfiguration config = b.getOwner().getPlayerConfig();
				File f = b.getOwner().getPlayerFile();
				config.set(key, null);
				try { config.save(f); } catch (IOException err) { Bukkit.getLogger().severe(err.getMessage()); }

				if (b.getOwner().isOnline()) b.getOwner().getOnlinePlayer().sendMessage(String.format(getMessage("success.bounty.redeem"), kName, tName));
				if (broadcast)
					Bukkit.broadcastMessage(String.format(get("success.bounty.broadcast"), kName, tName, String.format("%,.2f", b.getAmount())));

				amount.addAndGet(b.getAmount());
				bountyCount.incrementAndGet();
				nk.add(b.getEconomy(), b.getAmount());
			}

			if (!broadcast) killer.sendMessage(String.format(getMessage("success.bounty.claim"), bountyCount.get(), tName));
		}
		
		@EventHandler
		public void moneyDecrease(PlayerDeathEvent e) {
			if (!plugin.hasDeathDecrease()) return;
			
			Player p = e.getEntity();
			NovaPlayer np = new NovaPlayer(p);
			
			List<String> lost = new ArrayList<>();
			lost.add(get("constants.lost"));
			String id = p.getLastDamageCause().getCause().name();
			if (ModifierReader.isIgnored(id)) return;

			double divider = getDeathDivider();

			for (EquipmentSlot s : EquipmentSlot.values()) {
				if (s == EquipmentSlot.HAND || s.name().equalsIgnoreCase("OFF_HAND")) continue;

				ItemStack item = getItem(p.getEquipment(), s);
				if (item == null) continue;
				if (!item.hasItemMeta()) continue;

				if (item.getItemMeta().hasEnchant(Enchantment.PROTECTION_ENVIRONMENTAL))
					divider /= Math.min(item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) / 2, 4);
			}


			if (ModifierReader.getModifier("Death") != null && ModifierReader.getModifier("Death").containsKey(id)) {
				Set<Map<Economy, Double>> value = ModifierReader.getModifier("Death").get(id);
				List<String> msgs = new ArrayList<>();
				for (Map<Economy, Double> map : value)
					for (Economy econ : map.keySet()) {
						double amount = map.get(econ);
						if (amount <= 0) continue;
						lost.add(callRemoveBalanceEvent(p, econ, amount));
					}
			} else for (Economy econ : Economy.getEconomies()) lost.add(callRemoveBalanceEvent(p, econ, np.getBalance(econ) / divider));
			
			if (plugin.hasNotifications()) p.sendMessage(String.join("\n", lost.toArray(new String[0])));
		}

		private String callRemoveBalanceEvent(Player p, Economy econ, double amount) {
			NovaPlayer np = new NovaPlayer(p);
			double previousBal = np.getBalance(econ);

			PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, amount, previousBal, previousBal - amount, true);
			if (!event.isCancelled()) np.remove(econ, amount);

			return ChatColor.DARK_RED + "- " + ChatColor.RED + econ.getSymbol() + Math.floor(amount * 100) / 100;
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

			if (item.isSimilar(w.getGUIBackground())) e.setCancelled(true);
			if (!item.hasItemMeta()) return;
			if (!w.hasID(item)) return;

			String id =  w.getNBTString(item, "id");
			if (id == null) return;
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
						if (h.ignoreIds().contains(w.getID(i))) continue;

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

			if (item.isSimilar(w.getGUIBackground())) e.setCancelled(true);
			if (inv.getHolder() != null && inv.getHolder() instanceof Wrapper.CancelHolder) e.setCancelled(true);

			String id = w.getNBTString(item, "id");
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
				e.getWhoClicked().openInventory(w.generateBusinessData(b));
			});

			put("product:buy", e -> {
				ItemStack item = e.getCurrentItem();
				String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(item.getType().name().replace('_', ' '));
				HumanEntity p = e.getWhoClicked();

				if (!w.getNBTBoolean(item, "product:in_stock")) {
					p.sendMessage(String.format(Novaconomy.get("error.business.not_in_stock"), name));
					return;
				}

				List<BusinessProduct> products = new ArrayList<>();
				BusinessProduct pr = (BusinessProduct) w.getNBTProduct(item, PRODUCT_TAG);

				Inventory inv = w.genGUI(27, WordUtils.capitalizeFully(Novaconomy.get("constants.purchase")) + " \"" + ChatColor.RESET + name + ChatColor.RESET + "\"?", new Wrapper.CancelHolder());
				for (int i = 10; i < 17; i++) inv.setItem(i, w.getGUIBackground());

				inv.setItem(13, item);

				ItemStack yes = Items.yes("buy_product").clone();
				yes = w.setNBT(yes, PRODUCT_TAG, pr);
				yes = w.setNBT(yes, PRICE_TAG, pr.getPrice().getAmount());
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

				Economy econ = Economy.getEconomy(w.getNBTString(item, ECON_TAG));
				int nextI = sortedList.indexOf(econ.getName()) + 1;
				Economy next = sortedList.size() == 1 ? econ : Economy.getEconomy(sortedList.get(nextI == sortedList.size() ? 0 : nextI));

				item.setType(next.getIcon().getType());
				item = w.setNBT(item, ECON_TAG, next.getName().toLowerCase());

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
				Economy econ = Economy.getEconomy(w.getNBTString(item, ECON_TAG));

				Inventory inv = e.getClickedInventory();

				ItemStack confirm = inv.getItem(23);
				confirm = w.setNBT(confirm, ECON_TAG, econ.getName().toLowerCase());
				inv.setItem(23, confirm);

				ItemStack display = inv.getItem(13);
				ItemMeta dMeta = display.getItemMeta();
				dMeta.setLore(Collections.singletonList(String.format(Novaconomy.get("constants.business.price"), w.getNBTDouble(display, PRICE_TAG), econ.getSymbol())));
				display.setItemMeta(dMeta);
				inv.setItem(13, display);
			});
			put("economy:wheel:leaderboard", e -> {
				if (!(e.getWhoClicked() instanceof Player)) return;
				Player p = (Player) e.getWhoClicked();
				ItemStack item = e.getCurrentItem();
				String econ = w.getNBTString(item, "economy");

				List<String> economies = new ArrayList<>();
				economies.add("all");
				Economy.getEconomies().stream().map(Economy::getName).sorted(Comparator.reverseOrder()).forEach(economies::add);
				int nextI = economies.indexOf(econ) + 1;
				if (nextI >= economies.size()) nextI = 0;

				Economy next = economies.get(nextI).equalsIgnoreCase("all") ? null : Economy.getEconomy(economies.get(nextI));
				getCommandWrapper().balanceLeaderboard(p, next);
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
				BusinessProduct bP = (BusinessProduct) w.getNBTProduct(item, PRODUCT_TAG);

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

				double price = w.getNBTDouble(item, PRICE_TAG);
				Economy econ = Economy.getEconomy(w.getNBTString(item, ECON_TAG));
				ItemStack product = w.normalize(w.getNBTItem(item, "item"));

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
					if (w.getID(item).equalsIgnoreCase("business:add_resource")) continue;

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

				BusinessProduct pr = (BusinessProduct) w.getNBTProduct(item, PRODUCT_TAG);
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

			put("exchange:1", e -> EXCHANGE_BICONSUMER.accept(e, 12));
			put("exchange:2", e -> EXCHANGE_BICONSUMER.accept(e, 14));

			put("yes:exchange", e -> {
				if (!(e.getWhoClicked() instanceof Player)) return;
				Player p = (Player) e.getWhoClicked();
				NovaPlayer np = new NovaPlayer(p);
				Inventory inv = e.getView().getTopInventory();

				ItemStack takeItem = inv.getItem(12);
				Economy takeEcon = Economy.getEconomy(UUID.fromString(w.getNBTString(takeItem, ECON_TAG)));
				double take = w.getNBTDouble(takeItem, AMOUNT_TAG);
				if (np.getBalance(takeEcon) < take) {
					p.closeInventory();
					p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), Novaconomy.get("constants.convert")));
					XSound.BLOCK_NOTE_BLOCK_PLING.play(e.getWhoClicked(), 3F, 0F);
					return;
				}

				double max = NovaConfig.getConfiguration().getMaxConvertAmount(takeEcon);
				if (max >= 0 && take > max) {
					p.sendMessage(String.format(getMessage("error.economy.transfer_max"), String.format("%,.2f", max) + takeEcon.getSymbol(), String.format("%,.2f", take) + takeEcon.getSymbol()));
					p.closeInventory();
					return;
				}

				ItemStack giveItem = inv.getItem(14);
				Economy giveEcon = Economy.getEconomy(UUID.fromString(w.getNBTString(giveItem, ECON_TAG)));
				double give = w.getNBTDouble(inv.getItem(14), AMOUNT_TAG);

				double takeBal = np.getBalance(takeEcon);
				PlayerChangeBalanceEvent event1 = new PlayerChangeBalanceEvent(p, takeEcon, take, takeBal, takeBal - take, false);
				Bukkit.getPluginManager().callEvent(event1);
				if (!event1.isCancelled()) np.remove(takeEcon, take);

				double giveBal = np.getBalance(giveEcon);
				PlayerChangeBalanceEvent event2 = new PlayerChangeBalanceEvent(p, giveEcon, give, giveBal, giveBal + give, false);
				Bukkit.getPluginManager().callEvent(event2);
				if (!event2.isCancelled()) np.add(giveEcon, give);

				XSound.ENTITY_ARROW_HIT_PLAYER.play(p, 3F, 2F);
				p.closeInventory();
				p.sendMessage(String.format(getMessage("success.economy.convert"), take + "" + takeEcon.getSymbol(), give + "" + giveEcon.getSymbol()));
			});

			put("no:exchange", e -> {
				e.getWhoClicked().closeInventory();
				XSound.BLOCK_NOTE_BLOCK_PLING.play(e.getWhoClicked(), 3F, 0F);
			});

			put("next:bank_balance", e -> CHANGE_PAGE_TRICONSUMER.accept(e, 1, CommandWrapper.getBankBalanceGUI()));
			put("prev:bank_balance", e -> CHANGE_PAGE_TRICONSUMER.accept(e, -1, CommandWrapper.getBankBalanceGUI()));
		}
	};

	@FunctionalInterface
	private interface TriConsumer<T, U, L> {
		void accept(T t, U u, L l);
	}

	private static final TriConsumer<InventoryClickEvent, Integer, List<Inventory>> CHANGE_PAGE_TRICONSUMER = (e, i, l) -> {
		HumanEntity p = e.getWhoClicked();
		ItemStack item = e.getCurrentItem();
		int nextPage = (int) w.getNBTDouble(item, "page") + i;
		Inventory nextInv = nextPage >= l.size() ? l.get(0) : l.get(nextPage);

		p.openInventory(nextInv);
		XSound.ITEM_BOOK_PAGE_TURN.play(p, 3F, 2F);
	};

	private static final BiConsumer<InventoryClickEvent, Integer> EXCHANGE_BICONSUMER = (e, i) -> {
		if (!(e.getWhoClicked() instanceof Player)) return;
		Player p = (Player) e.getWhoClicked();
		ItemStack item = e.getCurrentItem();
		Inventory inv = e.getView().getTopInventory();
		Economy econ = Economy.getEconomy(UUID.fromString(w.getNBTString(item, ECON_TAG)));
		int oIndex = i == 14 ? 12 : 14;
		Economy econ2 = Economy.getEconomy(UUID.fromString(w.getNBTString(inv.getItem(oIndex), ECON_TAG)));

		List<Economy> economies = Economy.getEconomies().stream()
				.filter(economy -> !economy.equals(econ) && !economy.equals(econ2))
				.sorted(Comparator.comparing(Economy::getName))
				.collect(Collectors.toList());

		if (economies.size() == 0) {
			ItemStack ec1 = inv.getItem(12);
			ItemStack first = ec1.clone();
			first = w.setID(first, "exchange:2");
			first = w.setNBT(first, ECON_TAG, w.getNBTString(ec1, ECON_TAG));
			first = w.setNBT(first, AMOUNT_TAG, w.getNBTDouble(ec1, AMOUNT_TAG));

			ItemStack ec2 = inv.getItem(14);
			ItemStack second = ec2.clone();
			second = w.setID(second, "exchange:1");
			second = w.setNBT(second, ECON_TAG, w.getNBTString(ec2, ECON_TAG));
			second = w.setNBT(second, AMOUNT_TAG, w.getNBTDouble(ec2, AMOUNT_TAG));

			inv.setItem(14, first);
			inv.setItem(12, second);
			XSound.BLOCK_NOTE_BLOCK_PLING.play(p, 3F, 2F);
			return;
		}

		Economy next = economies.get(economies.indexOf(econ) + 1 >= economies.size() ? 0 : economies.indexOf(econ) + 1);
		ItemStack newItem = new ItemStack(next.getIcon());

		ItemMeta meta = newItem.getItemMeta();
		double amount = i == 12 ? w.getNBTDouble(item, AMOUNT_TAG) : Math.floor(econ.convertAmount(next, w.getNBTDouble(inv.getItem(oIndex), AMOUNT_TAG) * 100) / 100);
		meta.setLore(Collections.singletonList(ChatColor.YELLOW + "" + amount + "" + next.getSymbol()));
		newItem.setItemMeta(meta);

		newItem = w.setID(newItem, "exchange:" + (i == 14 ? "2" : "1"));
		newItem = w.setNBT(newItem, ECON_TAG, next.getUniqueId().toString());
		newItem = w.setNBT(newItem, AMOUNT_TAG, amount);

		inv.setItem(e.getSlot(), newItem);
		XSound.BLOCK_NOTE_BLOCK_PLING.play(p, 3F, 2F);
	};

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
				if (dec.equalsIgnoreCase("auto")) tempV = w.getCommandVersion();
				else tempV = Integer.parseInt(dec);
			} catch (IllegalArgumentException e) { tempV = w.getCommandVersion(); }

			wrapperVersion = tempV;
			return (CommandWrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".CommandWrapperV" + wrapperVersion).getConstructor(Plugin.class).newInstance(NovaConfig.getPlugin());
		} catch (Exception e) {
			NovaConfig.getLogger().severe(e.getMessage());
			return null;
		}
	}

	private static String getServerVersion() {
		return  Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
	}

	private static Wrapper getWrapper() {
		try {
			return (Wrapper) Class.forName(Novaconomy.class.getPackage().getName() + ".Wrapper" + getServerVersion()).getConstructor().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException("Wrapper not Found: " + getServerVersion());
		}
	}

	private static void runInterest() {
		if (!NovaConfig.getConfiguration().isInterestEnabled()) return;
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
				np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.interest"), i + " ", i == 1 ? get("constants.economy") : get("constants.economies")));
		}
	}
	
	private static BukkitRunnable INTEREST_RUNNABLE = new BukkitRunnable() {
		@Override
		public void run() {
			if (!NovaConfig.getConfiguration().isInterestEnabled()) { cancel(); return; }
			runInterest();
		}
	};

	private static void runTaxes() {
		Novaconomy plugin = getPlugin(Novaconomy.class);
		if (!plugin.hasAutomaticTaxes()) return;
		Map<NovaPlayer, Map<Economy, Double>> previousBals = new HashMap<>();
		Map<NovaPlayer, Map<Economy, Double>> amounts = new HashMap<>();

		Collection<? extends OfflinePlayer> players = plugin.hasOnlineTaxes() ? Bukkit.getOnlinePlayers() : Arrays.asList(Bukkit.getOfflinePlayers());

		for (OfflinePlayer p : players) {
			if (plugin.canIgnoreTaxes(p)) continue;
			NovaPlayer np = new NovaPlayer(p);

			Map<Economy, Double> previousBal = new HashMap<>();
			Map<Economy, Double> amount = new HashMap<>();

			for (Economy econ : Economy.getTaxableEconomies()) {
				previousBal.put(econ, np.getBalance(econ));
				double amountD = plugin.getMinimumPayment(econ);
				if (amountD > 0) amount.put(econ, amountD);
			}

			previousBals.put(np, previousBal);
			if (amount.size() > 0) amounts.put(np, amount);
		}

		if (amounts.size() < 1) return;
		AutomaticTaxEvent event = new AutomaticTaxEvent(previousBals, amounts);
		Bukkit.getPluginManager().callEvent(event);

		Map<NovaPlayer, Map<Economy, Double>> missedMap = new HashMap<>();
		if (!event.isCancelled()) {
			for (NovaPlayer np : previousBals.keySet()) {
				missedMap.put(np, new HashMap<>());
				for (Economy econ : previousBals.get(np).keySet()) {
					double amount = amounts.get(np).get(econ);

					if (np.getBalance(econ) < amount) {
						Map<Economy, Double> newMap = new HashMap<>(missedMap.get(np));
						newMap.put(econ, amount);
						missedMap.put(np, newMap);
						np.deposit(econ, np.getBalance(econ));

						PlayerMissTaxEvent event2 = new PlayerMissTaxEvent(np.getPlayer(), amount - np.getBalance(econ), econ);
						Bukkit.getPluginManager().callEvent(event2);
					} else np.deposit(econ, amount);

				}
			}
			sendTaxNotifications(previousBals.keySet(), missedMap);
		}
	}

	private static void sendTaxNotifications(Collection<NovaPlayer> players, Map<NovaPlayer, Map<Economy, Double>> missedMap) {
		Novaconomy plugin = getPlugin(Novaconomy.class);
		if (!plugin.hasNotifications()) return;
		for (NovaPlayer np : players) {
			if (!np.getPlayer().isOnline()) continue;
			int j = missedMap.get(np).size();
			int i = Economy.getTaxableEconomies().size() - j;
			if (j > 0)
				np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.tax.missed"), j + " ", j == 1 ? get("constants.economy") : get("constants.economies")));

			if (i > 0)
				np.getOnlinePlayer().sendMessage(String.format(getMessage("notification.tax"), i + " ", i == 1 ? get("constants.economy") : get("constants.economies")));
		}
	}

	private static BukkitRunnable TAXES_RUNNABLE = new BukkitRunnable() {
		@Override
		public void run() {
			if (!NovaConfig.getConfiguration().hasAutomaticTaxes()) { cancel(); return; }
			runTaxes();
		}
	};

	private static FileConfiguration funcConfig;

	private static final List<Class<? extends ConfigurationSerializable>> SERIALIZABLE = new ArrayList<Class<? extends ConfigurationSerializable>>() {{
		add(Economy.class);
		add(Business.class);
		add(Price.class);
		add(Product.class);
		add(BusinessProduct.class);
		add(Bounty.class);
	}};

	private void loadPlaceholders() {
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			getLogger().info("Placeholder API Found! Hooking...");
			new Placeholders(this);
			getLogger().info("Hooked into Placeholder API!");
		}
	}

	private void loadVault() {
		if (hasVault()) {
			getLogger().info("Vault Found! Hooking...");
			new VaultRegistry(this);
		}
	}

	private void loadTreasury() {
		if (Bukkit.getPluginManager().getPlugin("Treasury") != null) {
			getLogger().info("Treasury Found! Hooking...");
			new TreasuryRegistry(this);
		}
	}

	/**
	 * Called when the Plugin enables
	 */
	@Override
	public void onEnable() {
		NovaConfig.loadConfig();

		funcConfig = NovaConfig.loadFunctionalityFile();
		playerDir = new File(getDataFolder(), "players");
		File economyFile = new File(getDataFolder(), "economies.yml");
		try {
			if (!(economyFile.exists())) economyFile.createNewFile();
			if (!(playerDir.exists())) playerDir.mkdir();
		} catch (IOException e) {
			getLogger().severe("Error loading files & folders");
			getLogger().severe(e.getMessage());
		}
		economiesFile = YamlConfiguration.loadConfiguration(economyFile);
		config = this.getConfig();
		interest = config.getConfigurationSection("Interest");
		ncauses = config.getConfigurationSection("NaturalCauses");

		File businesses = new File(getDataFolder(), "businesses.yml");
		if (!businesses.exists()) saveResource("businesses.yml", false);

		File globalF = new File(getDataFolder(), "global.yml");
		if (!globalF.exists()) saveResource("global.yml", false);

		FileConfiguration global = YamlConfiguration.loadConfiguration(globalF);
		for (Economy econ : Economy.getEconomies()) if (!global.isSet("Bank." + econ.getName())) global.set("Bank." + econ.getName(), 0);
		try { global.save(globalF); } catch (IOException e) { getLogger().severe(e.getMessage()); for (StackTraceElement s : e.getStackTrace()) getLogger().severe(s.toString()); }

		getLogger().info("Loaded Languages & Configuration...");
		SERIALIZABLE.forEach(ConfigurationSerialization::registerClass);

		prefix = get("plugin.prefix");

		getLogger().info("Loaded Files...");

		getCommandWrapper();
		new Events(this);

		INTEREST_RUNNABLE.runTaskTimer(this, getInterestTicks(), getInterestTicks());
		TAXES_RUNNABLE.runTaskTimer(this, getTaxesTicks(), getTaxesTicks());

		getLogger().info("Loaded Core Functionality...");

		// Placeholders
		loadPlaceholders();

		// Vault + Treasury
		loadVault();
		loadTreasury();
		getLogger().info("Loaded Optional Hooks...");

		// Update Checker
		new UpdateChecker(this, UpdateCheckSource.SPIGOT, "100503")
				.setDownloadLink("https://www.spigotmc.org/resources/novaconomy.100503/")
				.setNotifyOpsOnJoin(true)
				.setChangelogLink("https://github.com/Team-Inceptus/Novaconomy/releases/")
				.setUserAgent("Java 8 Novaconomy User Agent")
				.checkEveryXHours(1)
				.checkNow();

		// bStats
		Metrics metrics = new Metrics(this, PLUGIN_ID);

		metrics.addCustomChart(new SimplePie("used_language", () -> Language.getById(this.getLanguage()).name()));
		metrics.addCustomChart(new SimplePie("command_version", () -> getWrapper().getCommandVersion() + ""));
		metrics.addCustomChart(new SingleLineChart("economy_count", () -> Economy.getEconomies().size()));
		metrics.addCustomChart(new SingleLineChart("business_count", () -> Business.getBusinesses().size()));
		metrics.addCustomChart(new SingleLineChart("bounty_count", () -> {
			AtomicInteger count = new AtomicInteger(0);
			for (OfflinePlayer p : Bukkit.getOfflinePlayers()) count.addAndGet(new NovaPlayer(p).getOwnedBounties().size());
			return count.get();
		}));

		getLogger().info("Loaded Dependencies...");
		saveConfig();
		getLogger().info("Successfully loaded Novaconomy");
	}

	private static final int PLUGIN_ID = 15322;

	/**
	 * Whether the server is currently running on a legacy platform (1.8-1.12) and Command Version 1 is active.
	 * <br><br>
	 * Setting the command version to 1 in functionality.yml will not change this value.
	 * @return true if legacy server, else false
	 */
	public static boolean isLegacy() { return w.isLegacy(); }

	/**
	 * Fetches the Directory of all Player data.
	 * @return the player directory
	 */
	public static File getPlayerDirectory() {
		return playerDir;
	}

	/**
	 * Fetches the Economy Configuration File.
	 * @return the economy configuration file
	 */
	public static FileConfiguration getEconomiesFile() {
		return economiesFile;
	}
	
	@Override
	public long getInterestTicks() { return interest.getLong("IntervalTicks"); }

	@Override
	public boolean isInterestEnabled() { return interest.getBoolean("Enabled"); }

	@Override
	public void setInterestEnabled(boolean enabled) { interest.set("Enabled", enabled); saveConfig(); }

	@Override
	public double getMaxConvertAmount(Economy econ) {
		if (funcConfig.getConfigurationSection("EconomyMaxConvertAmounts").contains(econ.getName())) return funcConfig.getDouble("EconomyMaxConvertAmounts." + econ.getName());
		return funcConfig.getDouble("MaxConvertAmount", -1);
	}

	@Override
	public void reloadHooks() {
		VaultRegistry.reloadVault();
		getLogger().info("Reloaded API Hooks");
	}

	private boolean isIncludedIn(List<String> list, OfflinePlayer p) {
		if (list == null || list.size() < 1) return false;
		AtomicBoolean b = new AtomicBoolean(false);

		for (String s : list) {
			Pattern patt = Pattern.compile(s);
			if (patt.matcher(p.getName()).matches() || (s.equalsIgnoreCase("OPS") && p.isOp()) || (s.equalsIgnoreCase("NONOPS") && !p.isOp())) {
				b.set(true);
				break;
			}
		}

		if (p.isOnline()) {
			Player op = p.getPlayer();
			for (String s : list) {
				if (b.get()) break;
				Pattern patt = Pattern.compile(s);
				for (PermissionAttachmentInfo info : op.getEffectivePermissions())
					if (patt.matcher(info.getPermission()).matches()) {
						b.set(true);
						break;
					}
			}
			if (hasVault() && VaultChat.isInGroup(list, op)) b.set(true);
		}

		return b.get();
	}

	static boolean hasVault() {
		return Bukkit.getPluginManager().getPlugin("Vault") != null;
	}

	@Override
	public double getMaxWithdrawAmount(Economy econ) {
		ConfigurationSection sec = config.getConfigurationSection("Taxes.MaxWithdraw");
		return sec.contains(econ.getName()) ? sec.getDouble(econ.getName()) : sec.getDouble("Global", 100);
	}

	@Override
	public boolean canBypassWithdraw(OfflinePlayer p) {
		return isIncludedIn(config.getStringList("Taxes.MaxWithdraw.Bypass"), p);
	}

	@Override
	public boolean canIgnoreTaxes(OfflinePlayer p) {
		return isIncludedIn(config.getStringList("Taxes.Ignore"), p);
	}

	@Override
	public boolean hasAutomaticTaxes() {
		return config.getBoolean("Taxes.Automatic.Enabled", false);
	}

	@Override
	public long getTaxesTicks() {
		return config.getLong("Taxes.Automatic.Interval");
	}

	@Override
	public double getMinimumPayment(Economy econ) {
		return config.getDouble("Taxes.Minimums." + econ.getName(), config.getDouble("Taxes.Minimums.Global", 0));
	}

	@Override
	public boolean hasOnlineTaxes() {
		return config.getBoolean("Taxes.Online", false);
	}

	@Override
	public void setOnlineTaxes(boolean enabled) {
		config.set("Taxes.Online", enabled);
		saveConfig();
	}

	@Override
	public boolean hasCustomTaxes() {
		return config.getBoolean("Taxes.Events.Enabled", false);
	}

	@Override
	public void setCustomTaxes(boolean enabled) {
		config.set("Taxes.Events.Enabled", enabled);
		saveConfig();
	}

	@Override
	public double getMaxIncrease() {
		return config.getDouble("NaturalCauses.MaxIncrease", 1000) <= 0 ? Double.MAX_VALUE : config.getDouble("NaturalCauses.MaxIncrease", 1000);
	}

	@Override
	public void setMaxIncrease(double max) {
		config.set("NaturalCauses.MaxIncrease", max);
		saveConfig();
	}

	@Override
	public boolean hasEnchantBonus() {
		return config.getBoolean("NaturalCauses.EnchantBonus", true);
	}

	@Override
	public void setEnchantBonus(boolean enabled) {
		config.set("NaturalCauses.EnchantBonus", enabled);
		saveConfig();
	}

	@Override
	public boolean isBroadcastingBounties() {
		return config.getBoolean("Bounties.Broadcast", true);
	}

	@Override
	public void setBroadcastingBounties(boolean broadcast) {
		config.set("Bounties.Broadcast", broadcast);
		saveConfig();
	}

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
		saveConfig();
	}

	@Override
	public void setDeathDecrease(boolean decrease) {
		ncauses.set("DeathDecrease", decrease);
		saveConfig();
	}

	@Override
	public boolean hasNotifications() {
		return config.getBoolean("Notifications", true);
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