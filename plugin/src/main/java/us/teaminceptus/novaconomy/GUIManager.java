package us.teaminceptus.novaconomy;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.business.BusinessProductAddEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessProductRemoveEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessSettingChangeEvent;
import us.teaminceptus.novaconomy.api.events.business.BusinessStockEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerRateBusinessEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerSettingChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPayEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.util.Items;
import us.teaminceptus.novaconomy.util.NovaSound;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.Novaconomy.getCommandWrapper;
import static us.teaminceptus.novaconomy.Novaconomy.r;
import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.of;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.getID;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.hasID;
import static us.teaminceptus.novaconomy.util.Items.*;

@SuppressWarnings("unchecked")
public final class GUIManager implements Listener {
    
    public GUIManager(Novaconomy plugin) {
        // this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

	static final String SETTING_TAG = "setting";

	private static double getAmount(ItemStack item) {
		return of(item).getDouble(AMOUNT_TAG);
	}

	@Nullable
	private static Economy getEconomy(ItemStack item) {
		return Economy.getEconomy(of(item).getUUID(ECON_TAG));
	}

	@Nullable
	private static Business getBusiness(ItemStack item) {
		return Business.getById(of(item).getUUID(BUSINESS_TAG));
	}

	@FunctionalInterface
	private interface TriConsumer<T, U, L> {
		void accept(T t, U u, L l);
	}

	static final TriConsumer<InventoryClickEvent, Integer, List<Inventory>> CHANGE_PAGE_TRICONSUMER = (e, i, l) -> {
		HumanEntity p = e.getWhoClicked();
		ItemStack item = e.getCurrentItem();
		int nextPage = of(item).getInt("page") + i;
		Inventory nextInv = nextPage >= l.size() ? l.get(0) : l.get(nextPage);

		p.openInventory(nextInv);
		NovaSound.ITEM_BOOK_PAGE_TURN.playSuccess(p);
	};

	static final BiConsumer<InventoryClickEvent, Boolean> BUSINESS_ADVERTISING_BICONSUMER = (e, add) -> {
		if (!(e.getWhoClicked() instanceof Player)) return;
		Player p = (Player) e.getWhoClicked();
		NovaPlayer np = new NovaPlayer(p);

		ItemStack item = e.getCurrentItem();
		Inventory inv = e.getView().getTopInventory();

		Business b = getBusiness(item);
		double amount = getAmount(item);

		ItemStack econWheel = inv.getItem(31);
		Economy econ = Economy.getEconomy(of(econWheel).getString(ECON_TAG));

		if (add && np.getBalance(econ) < amount) {
			p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), get("constants.deposit")));
			return;
		}

		String msg = String.format("%,.2f", amount) + econ.getSymbol();

		if (add) {
			np.remove(econ, amount);
			b.addAdvertisingBalance(amount, econ);
			p.sendMessage(String.format(getMessage("success.business.advertising_deposit"), msg, b.getName()));
		} else {
			np.add(econ, amount);
			b.removeAdvertisingBalance(amount, econ);
			p.sendMessage(String.format(getMessage("success.business.advertising_withdraw"), msg, b.getName()));
		}

		p.closeInventory();
		NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
	};

	static final BiConsumer<InventoryClickEvent, Integer> EXCHANGE_BICONSUMER = (e, i) -> {
		if (!(e.getWhoClicked() instanceof Player)) return;
		Player p = (Player) e.getWhoClicked();
		ItemStack item = e.getCurrentItem();
		Inventory inv = e.getView().getTopInventory();
		Economy econ = getEconomy(item);
		int oIndex = i == 14 ? 12 : 14;
		Economy econ2 = getEconomy(inv.getItem(oIndex));

		List<Economy> economies = Economy.getEconomies().stream()
				.filter(economy -> !economy.equals(econ2))
				.sorted(Comparator.comparing(Economy::getName))
				.collect(Collectors.toList());

		if (economies.size() == 1) {
			ItemStack ec1 = inv.getItem(12);
			ItemStack first = builder(ec1.clone(),
                    nbt -> {
                        nbt.setID("exchange:2");
                        nbt.set(ECON_TAG, of(ec1).getUUID(ECON_TAG));
                        nbt.set(AMOUNT_TAG, getAmount(ec1));
                    }
            );

			ItemStack ec2 = inv.getItem(14);
            ItemStack second = builder(ec1.clone(),
                    nbt -> {
                        nbt.setID("exchange:2");
                        nbt.set(ECON_TAG, of(ec2).getUUID(ECON_TAG));
                        nbt.set(AMOUNT_TAG, getAmount(ec2));
                    }
            );

			inv.setItem(14, first);
			inv.setItem(12, second);
			NovaSound.BLOCK_NOTE_BLOCK_PLING.playSuccess(p);
			return;
		}

		Economy next = economies.get(economies.indexOf(econ) + 1 >= economies.size() ? 0 : economies.indexOf(econ) + 1);
		double amount = i == 12 ? getAmount(item) : Math.floor(econ2.convertAmount(next, getAmount(inv.getItem(12)) * 100) / 100);

		ItemStack newItem = builder(next.getIcon(),
                meta -> meta.setLore(Collections.singletonList(ChatColor.YELLOW + String.format("%,.2f", amount) + next.getSymbol())),
                nbt -> {
                    nbt.setID("exchange:" + (i == 14 ? "2" : "1"));
                    nbt.set(ECON_TAG, next.getUniqueId());
                    nbt.set(AMOUNT_TAG, amount);
                });

		inv.setItem(e.getSlot(), newItem);

		if (i == 12) {
			double oAmount = Math.floor(next.convertAmount(econ2, amount) * 100) / 100;
			ItemStack other = builder(inv.getItem(14).clone(),
                    meta -> meta.setLore(Collections.singletonList(ChatColor.YELLOW + String.format("%,.2f", oAmount) + next.getSymbol())),
                    nbt -> {
                        nbt.setID("exchange:2");
                        nbt.set(ECON_TAG, econ2.getUniqueId());
                        nbt.set(AMOUNT_TAG, oAmount);
                    }
            );
			inv.setItem(14, other);
		}

		NovaSound.BLOCK_NOTE_BLOCK_PLING.playSuccess(p);
	};

    // Click Items

    static Map<String, BiConsumer<InventoryClickEvent, NovaInventory>> items() {
        return CLICK_ITEMS;
    }

	static final Map<String, BiConsumer<InventoryClickEvent, NovaInventory>> CLICK_ITEMS = ImmutableMap.<String, BiConsumer<InventoryClickEvent, NovaInventory>>builder()
    
    .put("economy_scroll", (e, inv) -> {
        ItemStack item = e.getCurrentItem();
        List<Economy> economies = Economy.getEconomies().stream().sorted().collect(Collectors.toList());

        String econ = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        int index = economies.indexOf(Economy.getEconomy(econ)) + 1;
        Economy newEcon = economies.get(index >= economies.size() ? 0 : index);

        inv.setItem(e.getSlot(), newEcon.getIcon());
    })
    .put(BUSINESS_TAG, (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        ItemStack item = e.getCurrentItem();
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        Business b = Business.getByName(name);
        p.openInventory(w.generateBusinessData(b, p, true));

        if (!b.isOwner(p)) {
            b.getStatistics().addView();
            b.saveBusiness();
        }
    })
    .put("product:buy", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);
        ItemStack item = e.getCurrentItem();
        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(item.getType().name().replace('_', ' '));

        if (!of(item).getBoolean("product:in_stock")) {
            p.sendMessage(String.format(get("error.business.not_in_stock"), name));
            return;
        }

        BusinessProduct pr = (BusinessProduct) getProduct(item);

        if (np.getBalance(pr.getEconomy()) < pr.getPrice().getAmount()) {
            p.sendMessage(String.format(get("error.economy.invalid_amount"), get("constants.purchase")));
            return;
        }

        Inventory purchaseGUI = w.genGUI(27, WordUtils.capitalizeFully(get("constants.purchase")) + " \"" + ChatColor.RESET + name + ChatColor.RESET + "\"?");
        for (int i = 10; i < 17; i++) purchaseGUI.setItem(i, w.getGUIBackground());

        purchaseGUI.setItem(13, item);

        for (int j = 0; j < 2; j++)
            for (int i = 0; i < 3; i++) {
                boolean add = j == 0;
                int amount = Math.min((int) Math.pow(10, i), 64);

                ItemStack amountI = builder(add ? LIME_STAINED_GLASS_PANE : RED_STAINED_GLASS_PANE, amount,
                        meta -> meta.setDisplayName((add ? ChatColor.GREEN + "+" : ChatColor.RED + "-") + amount),
                        nbt -> {
                            nbt.setID("product:amount");
                            nbt.set("add", add);
                            nbt.set(AMOUNT_TAG, amount);
                            nbt.set(PRODUCT_TAG, pr);
                        }
                );

                purchaseGUI.setItem(add ? 13 + (i + 1): 13 - (i + 1), amountI);
            }

        if (np.getBalance(pr.getEconomy()) < pr.getPrice().getAmount())
            purchaseGUI.setItem(21, Items.invalid(get("constants.purchase")));
        else
            purchaseGUI.setItem(21, yes("buy_product", nbt -> nbt.set(PRODUCT_TAG, pr)));

        inv.setAttribute(PRODUCT_TAG, pr);

        purchaseGUI.setItem(23, Items.cancel("no_product", nbt -> nbt.set(BUSINESS_TAG, pr.getBusiness().getUniqueId())));

        ItemStack amountPane = builder(item.getType(),
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + "1");
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                }, nbt -> nbt.set(AMOUNT_TAG, 1)
        );
        purchaseGUI.setItem(22, amountPane);

        p.openInventory(purchaseGUI);
        NovaSound.BLOCK_CHEST_OPEN.playFailure(p);
    })
    .put("product:amount", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);
        ItemStack item = e.getCurrentItem();

        boolean add = of(item).getBoolean("add");
        int prev = of(inv.getItem(22)).getInt(AMOUNT_TAG);
        int amount = of(item).getInt(AMOUNT_TAG);

        int newA = add ? Math.min(prev + amount, 64) : Math.max(prev - amount, 1);

        ItemStack newAmount = builder(inv.getItem(22), newA,
                meta -> meta.setDisplayName(ChatColor.YELLOW + "" + newA),
                nbt -> nbt.set(AMOUNT_TAG, newA)
        );
        inv.setItem(22, newAmount);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.play(e.getWhoClicked(), 1F, add ? 2F : 0F);

        BusinessProduct pr = (BusinessProduct) getProduct(item);

        if (np.getBalance(pr.getEconomy()) < pr.getPrice().getAmount() * newA)
            inv.setItem(21, Items.invalid(get("constants.purchase")));
        else
            inv.setItem(21, yes("buy_product", nbt -> nbt.set(PRODUCT_TAG, pr)));

        inv.setAttribute(PRODUCT_TAG, pr);

        p.updateInventory();
    })
    .put("no:close", (e, inv) -> {
        HumanEntity en = e.getWhoClicked();
        en.closeInventory();
    })
    .put("no:no_product", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Business b = getBusiness(item);

        p.sendMessage(get("cancel.business.purchase"));
        p.openInventory(w.generateBusinessData(b, p, true));
        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);

        if (!b.isOwner(p)) {
            b.getStatistics().addView();
            b.saveBusiness();
        }
    })
    .put("economy:wheel", (e, inv) -> {
        int slot = e.getRawSlot();
        ItemStack item = e.getCurrentItem().clone();

        List<String> sortedList = new ArrayList<>();
        Economy.getEconomies().forEach(econ -> sortedList.add(econ.getName()));
        sortedList.sort(String.CASE_INSENSITIVE_ORDER);

        Economy econ = getEconomy(item);
        int nextI = sortedList.indexOf(econ.getName()) + 1;
        Economy next = sortedList.size() == 1 ? econ : Economy.getEconomy(sortedList.get(nextI == sortedList.size() ? 0 : nextI));

        item.setType(next.getIconType());
        modelData(item, next.getCustomModelData());

        item = builder(item,
                meta -> meta.setDisplayName(ChatColor.GOLD + next.getName()),
                nbt -> nbt.set(ECON_TAG, next.getUniqueId())
        );

        e.getView().setItem(slot, item);
        NovaSound.BLOCK_NOTE_BLOCK_PLING.play(e.getWhoClicked());
    })
    .put("economy:wheel:add_product", (e, inv) -> {
        items().get("economy:wheel").accept(e, inv);

        ItemStack item = e.getCurrentItem();
        Economy econ = getEconomy(item);

        ItemStack confirm = builder(inv.getItem(23), nbt -> nbt.set(ECON_TAG, econ.getUniqueId()));
        inv.setItem(23, confirm);

        ItemStack display = new ItemStack(inv.getItem(13));
        ItemMeta dMeta = display.getItemMeta();
        dMeta.setLore(Collections.singletonList(String.format(get("constants.business.price"), of(display).getDouble(PRICE_TAG), econ.getSymbol())));
        display.setItemMeta(dMeta);
        inv.setItem(13, display);
    })
    .put("economy:wheel:leaderboard", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        String econ = of(item).getString("economy");

        List<String> economies = new ArrayList<>();
        economies.add("all");
        Economy.getEconomies().stream().map(Economy::getName).sorted(Comparator.reverseOrder()).forEach(economies::add);
        int nextI = economies.indexOf(econ) + 1;
        if (nextI >= economies.size()) nextI = 0;

        Economy next = economies.get(nextI).equalsIgnoreCase("all") ? null : Economy.getEconomy(economies.get(nextI));
        getCommandWrapper().balanceLeaderboard(p, next);
    })
    .put("yes:buy_product", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;

        ItemStack item = e.getCurrentItem();
        Player p = (Player) e.getWhoClicked();

        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage(get("error.player.full_inventory"));
            return;
        }

        NovaPlayer np = new NovaPlayer(p);
        BusinessProduct bP = (BusinessProduct) getProduct(item);

        if (!np.canAfford(bP)) {
            p.sendMessage(String.format(get("error.economy.invalid_amount"), get("constants.purchase")));
            p.closeInventory();
            return;
        }

        ItemStack product = bP.getItem();
        int size = Math.min((int) of(inv.getItem(22)).getDouble(AMOUNT_TAG), bP.getBusiness().getTotalStock(product));
        product.setAmount(size);

        Economy econ = bP.getEconomy();
        double amount = bP.getPrice().getAmount() * size;

        if (np.getBalance(econ) < amount) {
            p.sendMessage(String.format(get("error.economy.invalid_amount"), get("constants.purchase")));
            p.closeInventory();
            return;
        }

        np.remove(econ, amount);

        PlayerStatistics pStats = np.getStatistics();
        pStats.setProductsPurchased(pStats.getProductsPurchased() + size);

        bP.getBusiness().removeResource(product);
        p.getInventory().addItem(product);

        Business b = bP.getBusiness();
        Product bPr = new Product(bP);
        BusinessStatistics bStats = b.getStatistics();
        bStats.setTotalSales(bStats.getTotalSales() + product.getAmount());

        BusinessStatistics.Transaction t = new BusinessStatistics.Transaction(p, bP, System.currentTimeMillis());
        bStats.setLastTransaction(t);

        List<BusinessStatistics.Transaction> newTransactions = new ArrayList<>(pStats.getTransactionHistory());
        newTransactions.add(t);
        if (newTransactions.size() > 10) newTransactions.remove(0);
        pStats.setTransactionHistory(newTransactions);
        np.save();

        ItemStack clone = product.clone();
        clone.setAmount(1);
        Product bPrS = new Product(clone, bPr.getPrice());
        bStats.getProductSales().put(bPrS, bStats.getProductSales().getOrDefault(bPrS, 0) + product.getAmount());
        b.saveBusiness();

        String material = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(product.getType().name().replace('_', ' '));

        p.sendMessage(String.format(get("success.business.purchase"), material, bP.getBusiness().getName()));
        p.closeInventory();
        NovaSound.ENTITY_ARROW_HIT_PLAYER.play(p);

        NovaPlayer owner = new NovaPlayer(bP.getBusiness().getOwner());
        if (b.getSetting(Settings.Business.AUTOMATIC_DEPOSIT)) {
            owner.add(econ, amount * 0.85);
            b.addAdvertisingBalance(amount * 0.15, econ);
        } else owner.add(econ, amount);

        if (owner.isOnline() && owner.hasNotifications()) {
            String name = p.getDisplayName() == null ? p.getName() : p.getDisplayName();
            Player bOwner = owner.getOnlinePlayer();
            bOwner.sendMessage(String.format(get("notification.business.purchase"), name, material));
            NovaSound.ENTITY_ARROW_HIT_PLAYER.play(bOwner, 1F, 2F);
        }

        PlayerPurchaseProductEvent event = new PlayerPurchaseProductEvent(p, bP, t, size);
        Bukkit.getPluginManager().callEvent(event);
    })
    .put("business:add_product", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Business b = Business.getByOwner(p);
        ItemStack item = e.getCurrentItem();
        NBTWrapper nbt = of(item);

        double price = nbt.getDouble(PRICE_TAG);
        Economy econ = Economy.getEconomy(nbt.getString(ECON_TAG));
        ItemStack product = w.normalize(nbt.getItem("item"));

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
    })
    .put("business:add_resource", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Business b = Business.getByOwner(p);
        inv.setAttribute("added", true);

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
            if (getID(item).equalsIgnoreCase("business:add_resource")) continue;

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
    })
    .put("product:remove", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Business b = Business.getByOwner(p);
        ItemStack item = e.getCurrentItem();
        BusinessProduct pr = (BusinessProduct) getProduct(item);

        ItemStack product = pr.getItem();

        b.removeProduct(pr);
        List<ItemStack> stock = new ArrayList<>(pr.getBusiness().getResources()).stream()
                .filter(product::isSimilar)
                .collect(Collectors.toList());

        b.removeResource(stock);
        String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : WordUtils.capitalizeFully(product.getType().name().replace('_', ' '));

        p.sendMessage(String.format(get("success.business.remove_product"), name, b.getName()));

        stock.forEach(i -> {
            if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), i);
            else p.getInventory().addItem(i);
        });

        p.closeInventory();

        BusinessProductRemoveEvent event = new BusinessProductRemoveEvent(pr);
        Bukkit.getPluginManager().callEvent(event);
    })
    .put("exchange:1", (e, inv) -> EXCHANGE_BICONSUMER.accept(e, 12))
    .put("exchange:2", (e, inv) -> EXCHANGE_BICONSUMER.accept(e, 14))
    .put("yes:exchange", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);

        ItemStack takeItem = inv.getItem(12);
        Economy takeEcon = getEconomy(takeItem);
        double take = getAmount(takeItem);
        if (np.getBalance(takeEcon) < take) {
            p.closeInventory();
            p.sendMessage(String.format(getMessage("error.economy.invalid_amount"), get("constants.convert")));
            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
            return;
        }

        double max = NovaConfig.getConfiguration().getMaxConvertAmount(takeEcon);
        if (max >= 0 && take > max) {
            p.sendMessage(String.format(getMessage("error.economy.transfer_max"), String.format("%,.2f", max) + takeEcon.getSymbol(), String.format("%,.2f", take) + takeEcon.getSymbol()));
            p.closeInventory();
            return;
        }

        ItemStack giveItem = inv.getItem(14);
        Economy giveEcon = getEconomy(giveItem);
        double give = getAmount(inv.getItem(14));

        double takeBal = np.getBalance(takeEcon);
        PlayerChangeBalanceEvent event1 = new PlayerChangeBalanceEvent(p, takeEcon, take, takeBal, takeBal - take, false);
        Bukkit.getPluginManager().callEvent(event1);
        if (!event1.isCancelled()) np.remove(takeEcon, take);

        double giveBal = np.getBalance(giveEcon);
        PlayerChangeBalanceEvent event2 = new PlayerChangeBalanceEvent(p, giveEcon, give, giveBal, giveBal + give, false);
        Bukkit.getPluginManager().callEvent(event2);
        if (!event2.isCancelled()) np.add(giveEcon, give);

        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        p.closeInventory();
        p.sendMessage(String.format(getMessage("success.economy.convert"), String.format("%,.2f", take) + takeEcon.getSymbol(), String.format("%,.2f", give) + "" + giveEcon.getSymbol()));
    })
    .put("no:close_effect", (e, inv) -> {
        e.getWhoClicked().closeInventory();
        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
    })
    .put("next:bank_balance", (e, inv) -> CHANGE_PAGE_TRICONSUMER.accept(e, 1, CommandWrapper.getBankBalanceGUI()))
    .put("prev:bank_balance", (e, inv) -> CHANGE_PAGE_TRICONSUMER.accept(e, -1, CommandWrapper.getBankBalanceGUI()))
    .put("next:balance", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        CHANGE_PAGE_TRICONSUMER.accept(e, 1, CommandWrapper.getBalancesGUI(p));
    })
    .put("prev:balance", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        CHANGE_PAGE_TRICONSUMER.accept(e, -1, CommandWrapper.getBalancesGUI(p));
    })
    .put(SETTING_TAG, (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        String section = of(item).getString(SETTING_TAG);

        getCommandWrapper().settings(p, section);
    })
    .put("setting_toggle", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        NBTWrapper cNBT = of(item);

        String display = cNBT.getString("display");
        String section = cNBT.getString("section");
        String setting = cNBT.getString(SETTING_TAG);
        boolean value = cNBT.getBoolean("value");

        ItemStack nItem = builder(value ? RED_WOOL : LIME_WOOL,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + display + ": " + (value ? ChatColor.RED + get("constants.off") : ChatColor.GREEN + get("constants.on")));
                    if (!value) {
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                }, nbt -> {
                    nbt.setID("setting_toggle");
                    nbt.set("display", display);
                    nbt.set("section", section);
                    nbt.set(SETTING_TAG, setting);
                    nbt.set("value", !value);
                }
        );

        e.getView().setItem(e.getRawSlot(), nItem);
        p.updateInventory();

        if (value) NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p); else NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);

        if (section.equalsIgnoreCase(BUSINESS_TAG)) {
            Business b = Business.getByOwner(p);
            Settings.Business sett = Settings.Business.valueOf(setting);
            b.setSetting(sett, !value);

            BusinessSettingChangeEvent event = new BusinessSettingChangeEvent(b, value, !value, sett);
            Bukkit.getPluginManager().callEvent(event);
        } else {
            NovaPlayer np = new NovaPlayer(p);
            Settings.Personal sett = Settings.Personal.valueOf(setting);
            np.setSetting(sett, !value);

            PlayerSettingChangeEvent event = new PlayerSettingChangeEvent(p, value, !value, sett);
            Bukkit.getPluginManager().callEvent(event);
        }
    })
    .put("back:settings", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        getCommandWrapper().settings(p, null);
    })
    .put("business:home", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        boolean anonymous = of(item).getBoolean("anonymous");
        Business b = getBusiness(item);

        if (anonymous) {
            p.sendMessage(get("plugin.prefix") + ChatColor.RED + get("constants.business.anonymous_home"));
            return;
        }

        if (!b.hasHome()) {
            p.sendMessage(b.isOwner(p) ? getMessage("error.business.no_home") : String.format(getMessage("error.business.no_home_user"), b.getName()));
            return;
        }

        if (b.getHome().distanceSquared(p.getLocation()) < 16) {
            p.sendMessage(getMessage("error.business.too_close_home"));
            return;
        }

        p.sendMessage(ChatColor.DARK_AQUA + get("constants.teleporting"));
        p.teleport(b.getHome());
        NovaSound.ENTITY_ENDERMAN_TELEPORT.play(p, 1F, 1F);
    })
    .put("business:settings", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        getCommandWrapper().settings(p, BUSINESS_TAG);
    })
    .put("business:statistics", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Business b = getBusiness(item);
        boolean anonymous = of(item).getBoolean("anonymous");

        if (anonymous) {
            p.sendMessage(get("plugin.prefix") + ChatColor.RED + get("constants.business.anonymous_statistics"));
            return;
        }

        getCommandWrapper().businessStatistics(p, b);
    })
    .put("business:rating", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        ItemStack item = e.getCurrentItem();
        int rating = of(item).getInt("rating");
        int newRating = rating + 1 > 4 ? 0 : rating + 1;

        ItemStack nItem = builder(item.clone(),
                meta -> meta.setDisplayName(ChatColor.YELLOW + "" + (newRating + 1) + "⭐"),
                nbt -> {
                    nbt.setID("business:rating");
                    nbt.set("rating", newRating);
                }
        );
        nItem.setType(CommandWrapper.getRatingMats()[newRating]);
        inv.setItem(e.getSlot(), nItem);

        String comment = of(inv.getItem(21)).getString("comment");
        String business = of(inv.getItem(21)).getString(BUSINESS_TAG);

        ItemStack confirm = builder(inv.getItem(21).clone(),
                nbt -> {
                    nbt.setID("yes:business_rate");
                    nbt.set("rating", newRating);
                    nbt.set("comment", comment);
                    nbt.set(BUSINESS_TAG, business);
                }
        );

        inv.setItem(21, confirm);

        (newRating > 1 ? NovaSound.ENTITY_ARROW_HIT_PLAYER : NovaSound.BLOCK_NOTE_BLOCK_PLING).play(p, 1F, 0.4F * (newRating + 1));
    })
    .put("yes:business_rate", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);
        ItemStack item = e.getCurrentItem();
        NBTWrapper nbt = of(item);

        int rating = nbt.getInt("rating") + 1;
        UUID businessId = nbt.getUUID(BUSINESS_TAG);
        String comment = nbt.getString("comment");

        Rating r = new Rating(p, businessId, rating, System.currentTimeMillis(), comment);
        Business b = Business.getById(businessId);

        PlayerRateBusinessEvent event = new PlayerRateBusinessEvent(p, b, r);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            np.setRating(event.getRating());
            p.closeInventory();
            p.sendMessage(String.format(getMessage("success.business.rate"), b.getName(), rating));
            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        }
    })
    .put("business:click", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        Business b = getBusiness(item);

        boolean notOwner = !b.isOwner(p);

        p.openInventory(w.generateBusinessData(b, p, notOwner));
        NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p, 1F, 0.5F);

        if (notOwner) {
            b.getStatistics().addView();
            b.saveBusiness();
        }
    })
    .put("product:edit_price", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        BusinessProduct pr = (BusinessProduct) getProduct(item);

        double price = of(item).getDouble(PRICE_TAG);
        Economy econ = getEconomy(item);
        Business b = pr.getBusiness();

        b.getProduct(pr.getItem()).setPrice(new Price(econ, price));

        String display = pr.getItem().hasItemMeta() && pr.getItem().getItemMeta().hasDisplayName() ? pr.getItem().getItemMeta().getDisplayName() : WordUtils.capitalizeFully(pr.getItem().getType().name().replace('_', ' '));
        p.sendMessage(String.format(getMessage("success.business.edit_price"), display, String.format("%,.2f", price) + econ.getSymbol()));
        p.closeInventory();
    })
    .put("player_stats", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        ItemStack item = e.getCurrentItem();
        Player p = (Player) e.getWhoClicked();
        OfflinePlayer target = Bukkit.getOfflinePlayer(of(item).getUUID("player"));

        getCommandWrapper().playerStatistics(p, target);
    })
    .put("business:advertising", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        getCommandWrapper().businessAdvertising(p);
    })
    .put("business:change_advertising", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        Business b = getBusiness(item);
        boolean add = of(item).getBoolean("add");
        double amount = getAmount(item);
        amount = add ? amount : -amount;

        ItemStack econWheel = inv.getItem(31);
        Economy econ = Economy.getEconomy(of(econWheel).getString(ECON_TAG));
        double currentTotal = getAmount(inv.getItem(39));
        double newAmount = Math.max(currentTotal + amount, 0);

        ItemStack confirm = builder(inv.getItem(39),
                nbt -> {
                    nbt.set(AMOUNT_TAG, newAmount);
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                }
        );
        inv.setItem(39, confirm);

        ItemStack total = Items.builder(inv.getItem(40),
                meta -> meta.setDisplayName(ChatColor.GOLD + String.format("%,.0f", newAmount) + econ.getSymbol())
        );
        inv.setItem(40, total);

        (add ? NovaSound.ENTITY_ARROW_HIT_PLAYER : NovaSound.BLOCK_NOTE_BLOCK_PLING).play(p, 1F, add ? 2F : 0F);
    })
    .put("economy:wheel:change_advertising", (e, inv) -> {
        items().get("economy:wheel").accept(e, inv);

        ItemStack item = e.getCurrentItem();
        Economy econ = Economy.getEconomy(of(item).getString(ECON_TAG));

        ItemStack confirm = inv.getItem(39);
        double currentTotal = getAmount(confirm);

        ItemStack total = Items.builder(inv.getItem(40),
                meta -> meta.setDisplayName(ChatColor.GOLD + String.format("%,.0f", currentTotal) + econ.getSymbol())
        );
        inv.setItem(40, total);
    })
    .put("yes:deposit_advertising", (e, inv) -> BUSINESS_ADVERTISING_BICONSUMER.accept(e, true))
    .put("yes:withdraw_advertising", (e, inv) -> BUSINESS_ADVERTISING_BICONSUMER.accept(e, false))
    .put("business:click:advertising", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        items().get("business:click").accept(e, inv);
        ItemStack item = e.getCurrentItem();

        Business to = getBusiness(item);
        Business from = Business.getById(of(item).getUUID("from_business"));

        double add = NovaConfig.getConfiguration().getBusinessAdvertisingReward();
        if (to.getAdvertisingBalance() < add) return;

        Set<Economy> economies = Economy.getClickableRewardEconomies();
        Economy randomEcon = economies.stream().skip(r.nextInt(economies.size())).findFirst().orElse(null);

        to.removeAdvertisingBalance(add, randomEcon);
        from.addAdvertisingBalance(add, randomEcon);
    })
    .put("business:click:advertising_external", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        items().get("business:click").accept(e, inv);
        ItemStack item = e.getCurrentItem();

        Business to = getBusiness(item);
        double remove = NovaConfig.getConfiguration().getBusinessAdvertisingReward();

        Set<Economy> economies = Economy.getClickableRewardEconomies();
        Economy randomEcon = economies.stream().skip(r.nextInt(economies.size())).findFirst().orElse(null);

        to.removeAdvertisingBalance(remove, randomEcon);
    })
    .put("business:pick_rating", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        NBTWrapper nbt = of(item);

        OfflinePlayer owner = Bukkit.getOfflinePlayer(nbt.getUUID("owner"));
        boolean anon = nbt.getBoolean("anonymous");

        if (anon) return;

        getCommandWrapper().businessRating(p, owner);
    })
    .put("business:all_ratings", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        Business b = getBusiness(item);

        p.openInventory(CommandWrapper.getRatingsGUI(p, b).get(0));
    })
    .put("next:ratings", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        Business b = getBusiness(item);

        CHANGE_PAGE_TRICONSUMER.accept(e, 1, CommandWrapper.getRatingsGUI(p, b));
    })
    .put("prev:ratings", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        Business b = getBusiness(item);

        CHANGE_PAGE_TRICONSUMER.accept(e, -1, CommandWrapper.getRatingsGUI(p, b));
    })
    .put("business:leaderboard_category", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        String category = of(item).getString("category");
        String nextCategory = BL_CATEGORIES.get(BL_CATEGORIES.indexOf(category) == BL_CATEGORIES.size() - 1 ? 0 : BL_CATEGORIES.indexOf(category) + 1);

        getCommandWrapper().businessLeaderboard(p, nextCategory);
    })
    .put("economy:wheel:pay", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        items().get("economy:wheel").accept(e, inv);

        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);
        ItemStack item = e.getCurrentItem();

        Economy econ = getEconomy(item);

        ItemStack head1 = inv.getItem(10).clone();
        ItemMeta h1Meta = head1.getItemMeta();
        h1Meta.setLore(Collections.singletonList(ChatColor.GOLD + String.format("%,.2f", np.getBalance(econ)) + econ.getSymbol()));
        head1.setItemMeta(h1Meta);
        inv.setItem(10, head1);

        ItemStack currentAmountO = inv.getItem(40);
        double amount = getAmount(currentAmountO);

        if (np.getBalance(econ) < amount) amount = np.getBalance(econ);
        final double fAmount = amount;

        ItemStack currentAmount = builder(currentAmountO,
                meta -> meta.setDisplayName(ChatColor.GOLD + String.format("%,.2f", fAmount) + econ.getSymbol()),
                nbt -> {
                    nbt.set(ECON_TAG, econ.getUniqueId());
                    nbt.set(AMOUNT_TAG, fAmount);
                }
        );
        currentAmount.setType(econ.getIconType());

        inv.setItem(40, currentAmount);
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    })
    .put("pay:amount", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);

        ItemStack item = e.getCurrentItem();

        boolean add = of(item).getBoolean("add");
        double amount = getAmount(item) * (add ? 1 : -1);

        ItemStack currentAmountO = inv.getItem(40);
        double cAmount = getAmount(currentAmountO);
        Economy econ = getEconomy(currentAmountO);

        double bal = np.getBalance(econ);

        final double newAmount;
        final NovaSound sound;

        if (amount + cAmount > bal) {
            newAmount = bal;
            sound = NovaSound.BLOCK_NOTE_BLOCK_PLING;
        } else if (amount + cAmount < 0) {
            newAmount = 0;
            sound = NovaSound.BLOCK_NOTE_BLOCK_PLING;
        } else {
            newAmount = amount + cAmount;
            sound = NovaSound.ENTITY_ARROW_HIT_PLAYER;
        }

        ItemStack currentAmount = builder(currentAmountO,
                meta -> meta.setDisplayName(ChatColor.GOLD + String.format("%,.2f", newAmount) + econ.getSymbol()),
                nbt -> {
                    nbt.set(ECON_TAG, econ.getUniqueId());
                    nbt.set(AMOUNT_TAG, newAmount);
                }
        );

        inv.setItem(40, currentAmount);

        sound.play(p, 1F, sound == NovaSound.BLOCK_NOTE_BLOCK_PLING ? 0F : (add ? 2F : 0F));
    })
    .put("pay:confirm", (e, inv) -> {
        if (!(e.getWhoClicked() instanceof Player)) return;
        final Player p = (Player) e.getWhoClicked();
        final NovaPlayer np = new NovaPlayer(p);

        ItemStack item = e.getCurrentItem();
        final Player target = Bukkit.getPlayer(of(item).getUUID("target"));
        final NovaPlayer nt = new NovaPlayer(target);

        ItemStack currentAmountO = inv.getItem(40);
        Economy econ = getEconomy(currentAmountO);
        double amount = getAmount(currentAmountO);

        if (amount <= 0) {
            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
            return;
        }

        if (np.getBalance(econ) < amount) {
            p.sendMessage(getMessage("error.economy.invalid_amount_pay"));
            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
            return;
        }

        double bal = nt.getBalance(econ);

        PlayerPayEvent event = new PlayerPayEvent(target, p, econ, amount, bal, bal + amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
            return;
        }

        np.remove(econ, amount);
        p.closeInventory();
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);

        nt.add(econ, amount);

        String amountS = String.format("%,.2f", amount) + econ.getSymbol();
        String gameName = p.getDisplayName() == null ? p.getName() : p.getDisplayName();

        target.sendMessage(String.format(getMessage("success.economy.receive"), amountS, gameName));
        w.sendActionbar(target, String.format(get("success.economy.receive_actionbar"), amountS, gameName));
    })
    
    .build();

    private static Product getProduct(ItemStack item) {
        NBTWrapper nbt = of(item);
        if (!nbt.isProduct()) throw new IllegalArgumentException("ItemStack is not a product! (Missing 'product' tag)");

        return nbt.getProduct(PRODUCT_TAG);
    }

    // Inventory

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getClickedInventory() instanceof NovaInventory)) return;
        NovaInventory inv = (NovaInventory) e.getClickedInventory();
        e.setCancelled(inv.isCancelled());

        if (e.getCurrentItem() == null) return;
        ItemStack item = e.getCurrentItem();

        if (item.isSimilar(w.getGUIBackground())) e.setCancelled(true);
        if (!item.hasItemMeta()) return;
        if (!hasID(item)) return;

        String id = getID(item);
        if (id == null || !hasID(item)) return;
        if (!CLICK_ITEMS.containsKey(id)) return;

        if (!e.isCancelled()) e.setCancelled(true);
        CLICK_ITEMS.get(id).accept(e, inv);
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory() instanceof NovaInventory)) return;
        NovaInventory inv = (NovaInventory) e.getView().getTopInventory();
        e.setCancelled(inv.isCancelled());

        for (ItemStack item : e.getNewItems().values()) {
            if (item == null) return;
            if (item.isSimilar(w.getGUIBackground())) e.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("unchecked")
    public void close(InventoryCloseEvent e) {
        if (!(e.getInventory() instanceof NovaInventory)) return;
        NovaInventory inv = (NovaInventory) e.getInventory();
        if (inv == null) return;

        switch (inv.getId()) {
            case "return_items": {
                if (inv.getAttribute("added", Boolean.class)) return;

                Player p = inv.getAttribute("player", Player.class);
                List<String> ignore = inv.getAttribute("ignore_ids", List.class);

                for (ItemStack i : inv.getContents()) {
                    if (i == null) continue;
                    if (ignore.contains(getID(i))) continue;

                    if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), i);
                    else p.getInventory().addItem(i);
                }
                break;
            }
            default: break;
        }
    }

    @EventHandler
    public void move(InventoryMoveItemEvent e) {
        if (e.getItem() == null) return;
        ItemStack item = e.getItem();

        if (!(e.getDestination() instanceof NovaInventory)) return;
        NovaInventory inv = (NovaInventory) e.getDestination();
        e.setCancelled(inv.isCancelled());

        if (item.isSimilar(w.getGUIBackground())) e.setCancelled(true);

        String id = getID(item);
        if (id.length() > 0 && CLICK_ITEMS.containsKey(id)) e.setCancelled(true);
    }

}
