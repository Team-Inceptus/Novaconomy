package us.teaminceptus.novaconomy;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.MarketCategory;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;
import us.teaminceptus.novaconomy.api.events.business.*;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationSettingChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerRateBusinessEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerSettingChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPayEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.settings.SettingDescription;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.util.NovaSound;
import us.teaminceptus.novaconomy.util.NovaUtil;
import us.teaminceptus.novaconomy.util.NovaWord;
import us.teaminceptus.novaconomy.util.inventory.Generator;
import us.teaminceptus.novaconomy.util.inventory.InventorySelector;
import us.teaminceptus.novaconomy.util.inventory.Items;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.Novaconomy.getCommandWrapper;
import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.of;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.*;
import static us.teaminceptus.novaconomy.util.NovaUtil.format;
import static us.teaminceptus.novaconomy.util.inventory.Generator.*;
import static us.teaminceptus.novaconomy.util.inventory.InventorySelector.confirm;
import static us.teaminceptus.novaconomy.util.inventory.Items.*;

@SuppressWarnings("unchecked")
final class GUIManager implements Listener {

    public GUIManager(Novaconomy plugin) {
        // this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static double getAmount(ItemStack item) {
        return of(item).getDouble(AMOUNT_TAG);
    }

    @Nullable
    private static Economy getEconomy(ItemStack item) {
        return Economy.getEconomy(of(item).getUUID(ECON_TAG));
    }

    @Nullable
    private static Business getBusiness(ItemStack item) {
        return Business.byId(of(item).getUUID(BUSINESS_TAG));
    }

    @Nullable
    private static Corporation getCorporation(ItemStack item) {
        return Corporation.byId(of(item).getUUID(CORPORATION_TAG));
    }

    @FunctionalInterface
    private interface TriConsumer<T, U, L> {
        void accept(T t, U u, L l);
    }

    static final TriConsumer<InventoryClickEvent, Integer, List<NovaInventory>> CHANGE_PAGE_TRICONSUMER = (e, i, l) -> {
        HumanEntity p = e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        int nextPage = of(item).getInt("page") + i;
        Inventory nextInv = nextPage >= l.size() ? l.get(0) : l.get(nextPage);

        p.openInventory(nextInv);
        NovaSound.ITEM_BOOK_PAGE_TURN.playSuccess(p);
    };

    static final BiConsumer<InventoryClickEvent, Boolean> BUSINESS_ADVERTISING_BICONSUMER = (e, add) -> {
        Player p = (Player) e.getWhoClicked();
        NovaPlayer np = new NovaPlayer(p);

        ItemStack item = e.getCurrentItem();
        Inventory inv = e.getView().getTopInventory();

        Business b = getBusiness(item);
        double amount = getAmount(item);

        ItemStack econWheel = inv.getItem(31);
        Economy econ = getEconomy(econWheel);

        if (add && np.getBalance(econ) < amount) {
            p.sendMessage(format(getMessage("error.economy.invalid_amount"), get("constants.deposit")));
            return;
        }

        String msg = format("%,.2f", amount) + econ.getSymbol();

        if (add) {
            np.remove(econ, amount);
            b.addAdvertisingBalance(amount, econ);
            p.sendMessage(format(getMessage("success.business.advertising_deposit"), msg, b.getName()));
        } else {
            np.add(econ, amount);
            b.removeAdvertisingBalance(amount, econ);
            p.sendMessage(format(getMessage("success.business.advertising_withdraw"), msg, b.getName()));
        }

        p.closeInventory();
        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
    };

    static final BiConsumer<InventoryClickEvent, Integer> EXCHANGE_BICONSUMER = (e, i) -> {
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
                meta -> meta.setLore(Collections.singletonList(ChatColor.YELLOW + format("%,.2f", amount) + next.getSymbol())),
                nbt -> {
                    nbt.setID("exchange:" + (i == 14 ? "2" : "1"));
                    nbt.set(ECON_TAG, next.getUniqueId());
                    nbt.set(AMOUNT_TAG, amount);
                });

        inv.setItem(e.getSlot(), newItem);

        if (i == 12) {
            double oAmount = Math.floor(next.convertAmount(econ2, amount) * 100) / 100;
            ItemStack other = builder(inv.getItem(14).clone(),
                    meta -> meta.setLore(Collections.singletonList(ChatColor.YELLOW + format("%,.2f", oAmount) + next.getSymbol())),
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

    static final TriConsumer<InventoryClickEvent, NovaInventory, Integer> CORPORATION_LEVELING_TRICONSUMER = (e, inv, l) -> {
        Player p = (Player) e.getWhoClicked();

        Corporation c = Corporation.byId(inv.getAttribute(CORPORATION_TAG, UUID.class));
        int next = inv.getAttribute("current_level", Integer.class) + l;

        p.openInventory(generateCorporationLeveling(c, next));
        NovaSound.ENTITY_ARROW_HIT_PLAYER.play(p, 1F, 1F + l);
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
            .put("product:buy", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                ItemStack item = e.getCurrentItem();
                String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : NovaWord.capitalize(item.getType().name().replace('_', ' '));

                if (!of(item).getBoolean("product:in_stock")) {
                    p.sendMessage(format(get("error.business.not_in_stock"), name));
                    return;
                }

                BusinessProduct pr = (BusinessProduct) getProduct(item);

                if (np.getBalance(pr.getEconomy()) < pr.getPrice().getAmount()) {
                    p.sendMessage(format(get("error.economy.invalid_amount"), get("constants.purchase")));
                    return;
                }

                NovaInventory purchaseGUI = genGUI(27, NovaWord.capitalize(get("constants.purchase")) + " \"" + ChatColor.RESET + name + ChatColor.RESET + "\"?");
                purchaseGUI.setCancelled();

                for (int i = 10; i < 17; i++) purchaseGUI.setItem(i, GUI_BACKGROUND);

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

                        purchaseGUI.setItem(add ? 13 + (i + 1) : 13 - (i + 1), amountI);
                    }

                if (np.getBalance(pr.getEconomy()) < pr.getPrice().getAmount())
                    purchaseGUI.setItem(21, invalid(get("constants.purchase")));
                else
                    purchaseGUI.setItem(21, yes("buy_product", nbt -> nbt.set(PRODUCT_TAG, pr)));

                inv.setAttribute(PRODUCT_TAG, pr);

                purchaseGUI.setItem(23, cancel("no_product", nbt -> nbt.set(BUSINESS_TAG, pr.getBusiness().getUniqueId())));

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
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                ItemStack item = e.getCurrentItem();

                boolean add = of(item).getBoolean("add");
                int prev = of(inv.getItem(22)).getInt(AMOUNT_TAG);
                int amount = of(item).getInt(AMOUNT_TAG);

                int newA = add ? Math.min(prev + amount, 64) : Math.max(prev - amount, 1);

                ItemStack newAmount = builder(inv.getItem(22), newA,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + String.valueOf(newA)),
                        nbt -> nbt.set(AMOUNT_TAG, newA)
                );
                inv.setItem(22, newAmount);
                NovaSound.ENTITY_ARROW_HIT_PLAYER.play(e.getWhoClicked(), 1F, add ? 2F : 0F);

                BusinessProduct pr = (BusinessProduct) getProduct(item);

                if (np.getBalance(pr.getEconomy()) < pr.getPrice().getAmount() * newA)
                    inv.setItem(21, invalid(get("constants.purchase")));
                else
                    inv.setItem(21, yes("buy_product", nbt -> nbt.set(PRODUCT_TAG, pr)));

                inv.setAttribute(PRODUCT_TAG, pr);

                p.updateInventory();
            })
            .put("no:no_product", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                p.sendMessage(get("cancel.business.purchase"));
                p.openInventory(generateBusinessData(b, p, true, SortingType.PRODUCT_NAME_ASCENDING).get(0));
                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);

                if (!b.isOwner(p)) {
                    b.getStatistics().addView();
                    b.saveBusiness();

                    BusinessViewEvent event = new BusinessViewEvent(p, b);
                    Bukkit.getPluginManager().callEvent(event);
                }
            })
            .put("economy:wheel", (e, inv) -> {
                e.setCancelled(true);

                int slot = e.getRawSlot();
                ItemStack item = e.getCurrentItem().clone();

                List<String> sortedList = new ArrayList<>();
                Economy.getEconomies().forEach(econ -> sortedList.add(econ.getName()));
                sortedList.sort(String.CASE_INSENSITIVE_ORDER);

                Economy econ = getEconomy(item);
                int nextI = sortedList.indexOf(econ.getName()) + (e.getClick().isRightClick() ? -1 : 1);
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

                ItemStack display = new ItemStack(inv.getItem(13));
                ItemMeta dMeta = display.getItemMeta();
                dMeta.setLore(Collections.singletonList(format(get("constants.price"), of(display).getDouble(PRICE_TAG), econ.getSymbol())));
                display.setItemMeta(dMeta);
                inv.setItem(13, display);
            })
            .put("economy:wheel:leaderboard", (e, inv) -> {
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
                ItemStack item = e.getCurrentItem();
                Player p = (Player) e.getWhoClicked();

                if (p.getInventory().firstEmpty() == -1) {
                    p.sendMessage(get("error.player.full_inventory"));
                    return;
                }

                NovaPlayer np = new NovaPlayer(p);
                BusinessProduct bP = (BusinessProduct) getProduct(item);

                if (!np.canAfford(bP)) {
                    p.sendMessage(format(get("error.economy.invalid_amount"), get("constants.purchase")));
                    p.closeInventory();
                    return;
                }

                ItemStack product = bP.getItem();
                int size = Math.min((int) of(inv.getItem(22)).getDouble(AMOUNT_TAG), bP.getBusiness().getTotalStock(product));
                product.setAmount(size);

                Economy econ = bP.getEconomy();
                double amount = bP.getPrice().getAmount() * size;

                if (np.getBalance(econ) < amount) {
                    p.sendMessage(format(get("error.economy.invalid_amount"), get("constants.purchase")));
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

                String material = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : NovaWord.capitalize(product.getType().name().replace('_', ' '));

                p.sendMessage(format(get("success.business.purchase"), material, bP.getBusiness().getName()));
                p.closeInventory();
                NovaSound.ENTITY_ARROW_HIT_PLAYER.play(p);

                NovaPlayer owner = new NovaPlayer(bP.getBusiness().getOwner());
                double mod = b.getParentCorporation() == null ? 1 : b.getParentCorporation().getProfitModifier();
                double aAmount = amount * mod;

                if (NovaConfig.getConfiguration().isBusinessIncomeTaxEnabled() && !NovaConfig.getConfiguration().isBusinessIncomeTaxIgnoring(b)) {
                    double removed = aAmount * NovaConfig.getConfiguration().getBusinessIncomeTax();
                    aAmount -= removed;

                    Bank.addBalance(econ, removed);
                }

                if (b.getSetting(Settings.Business.AUTOMATIC_DEPOSIT)) {
                    owner.add(econ, aAmount * 0.85);
                    b.addAdvertisingBalance(aAmount * 0.15, econ);
                } else owner.add(econ, aAmount * mod);

                if (owner.isOnline() && owner.hasNotifications()) {
                    String name = p.getDisplayName() == null ? p.getName() : p.getDisplayName();
                    Player bOwner = owner.getOnlinePlayer();
                    bOwner.sendMessage(format(get("notification.business.purchase"), name, material));
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.play(bOwner, 1F, 2F);
                }

                PlayerPurchaseProductEvent event = new PlayerPurchaseProductEvent(p, bP, t, size);
                Bukkit.getPluginManager().callEvent(event);
            })
            .put("business:add_product", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byOwner(p);
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                double price = nbt.getDouble(PRICE_TAG);
                Economy econ = getEconomy(inv.getItem(23));
                ItemStack product = inv.getAttribute("item", ItemStack.class);

                Product pr = new Product(product, econ, price);

                BusinessProductAddEvent event = new BusinessProductAddEvent(new BusinessProduct(pr, b));
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    Product added = new Product(pr.getItem(), pr.getPrice());
                    b.addProduct(added);

                    String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : NovaWord.capitalize(product.getType().name().replace('_', ' '));
                    p.sendMessage(format(getMessage("success.business.add_product"), name));
                    p.closeInventory();
                }
            })
            .put("business:add_resource", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byOwner(p);
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

                p.sendMessage(format(getMessage("success.business.add_resource"), b.getName()));
                p.closeInventory();

                BusinessStockEvent event = new BusinessStockEvent(b, p, extra, resources);
                Bukkit.getPluginManager().callEvent(event);
            })
            .put("product:remove", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byOwner(p);
                ItemStack item = e.getCurrentItem();
                BusinessProduct pr = (BusinessProduct) getProduct(item);

                ItemStack product = pr.getItem();

                b.removeProduct(pr);
                List<ItemStack> stock = new ArrayList<>(pr.getBusiness().getResources()).stream()
                        .filter(product::isSimilar)
                        .collect(Collectors.toList());

                b.removeResource(stock);
                String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : NovaWord.capitalize(product.getType().name().replace('_', ' '));

                p.sendMessage(format(get("success.business.remove_product"), name, b.getName()));

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
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                ItemStack takeItem = inv.getItem(12);
                Economy takeEcon = getEconomy(takeItem);
                double take = getAmount(takeItem);
                if (np.getBalance(takeEcon) < take) {
                    p.closeInventory();
                    p.sendMessage(format(getMessage("error.economy.invalid_amount"), get("constants.convert")));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
                    return;
                }

                double max = NovaConfig.getConfiguration().getMaxConvertAmount(takeEcon);
                if (max >= 0 && take > max) {
                    p.sendMessage(format(getMessage("error.economy.transfer_max"), format("%,.2f", max) + takeEcon.getSymbol(), format("%,.2f", take) + takeEcon.getSymbol()));
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
                p.sendMessage(format(getMessage("success.economy.convert"), format("%,.2f", take) + takeEcon.getSymbol(), format("%,.2f", give) + giveEcon.getSymbol()));
            })
            .put("no:close", (e, inv) -> {
                e.getWhoClicked().closeInventory();
                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
            })
            .put("next:bank_balance", (e, inv) -> {
                SortingType<Economy> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), Economy.class);
                CHANGE_PAGE_TRICONSUMER.accept(e, 1, getBankBalanceGUI(type));
            })
            .put("prev:bank_balance", (e, inv) -> {
                SortingType<Economy> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), Economy.class);
                CHANGE_PAGE_TRICONSUMER.accept(e, -1, getBankBalanceGUI(type));
            })
            .put("next:balance", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                SortingType<Economy> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), Economy.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, 1, getBalancesGUI(p, type));
            })
            .put("prev:balance", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                SortingType<Economy> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), Economy.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, -1, getBalancesGUI(p, type));
            })
            .put(SETTING_TAG, (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                String section = of(item).getString(SETTING_TAG);

                getCommandWrapper().settings(p, section);
            })
            .put("setting_toggle", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper cNBT = of(item);

                String display = cNBT.getString("display");
                String section = cNBT.getString("section");
                String setting = cNBT.getString(SETTING_TAG);

                SettingDescription desc = Arrays.stream(Settings.values())
                        .filter(s -> s.name().equalsIgnoreCase(setting))
                        .findFirst()
                        .map(Settings.NovaSetting::getDescription)
                        .orElse(null);

                String valueS = cNBT.getString("value");
                Class<?> type = cNBT.getClass("type");

                ItemStack nItem;
                if (Boolean.class.isAssignableFrom(type)) {
                    boolean value = Boolean.parseBoolean(valueS);

                    nItem = builder(value ? RED_WOOL : LIME_WOOL,
                            meta -> {
                                meta.setDisplayName(ChatColor.YELLOW + display + ": " + (value ? ChatColor.RED + get("constants.off") : ChatColor.GREEN + get("constants.on")));
                                if (!value) {
                                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                }

                                if (desc != null) {
                                    List<String> lore = new ArrayList<>();
                                    lore.add(" ");
                                    lore.addAll(Arrays.stream(ChatPaginator.wordWrap(get(desc.value()), 30)).map(s -> ChatColor.GRAY + s).collect(Collectors.toList()));
                                    meta.setLore(lore);
                                }
                            }, nbt -> {
                                nbt.setID("setting_toggle");
                                nbt.set("display", display);
                                nbt.set("section", section);
                                nbt.set(SETTING_TAG, setting);
                                nbt.set("type", type);
                                nbt.set("value", String.valueOf(!value));
                            }
                    );

                    if (value) NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    else NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);

                    switch (section.toLowerCase()) {
                        case BUSINESS_TAG: {
                            Business b = Business.byOwner(p);
                            Settings.Business sett = Settings.Business.valueOf(setting);
                            b.setSetting(sett, !value);

                            BusinessSettingChangeEvent event = new BusinessSettingChangeEvent(b, value, !value, sett);
                            Bukkit.getPluginManager().callEvent(event);
                            break;
                        }
                        case CORPORATION_TAG: {
                            Corporation c = Corporation.byOwner(p);
                            Settings.Corporation<Boolean> sett = Settings.Corporation.valueOf(setting, Boolean.class);
                            c.setSetting(sett, !value);

                            CorporationSettingChangeEvent event = new CorporationSettingChangeEvent(c, value, !value, sett);
                            Bukkit.getPluginManager().callEvent(event);
                            break;
                        }
                        case "personal": {
                            NovaPlayer np = new NovaPlayer(p);
                            Settings.Personal sett = Settings.Personal.valueOf(setting);
                            np.setSetting(sett, !value);

                            PlayerSettingChangeEvent event = new PlayerSettingChangeEvent(p, value, !value, sett);
                            Bukkit.getPluginManager().callEvent(event);
                            break;
                        }
                    }
                } else if (Enum.class.isAssignableFrom(type)) {
                    Enum<?> value = Enum.valueOf(type.asSubclass(Enum.class), valueS);
                    Enum<?> next = (Enum<?>) type.getEnumConstants()[value.ordinal() + 1 >= type.getEnumConstants().length ? 0 : value.ordinal() + 1];

                    nItem = builder(CYAN_WOOL,
                            meta -> {
                                meta.setDisplayName(ChatColor.YELLOW + display + ": " + ChatColor.AQUA + next.name().toUpperCase());

                                if (desc != null) {
                                    List<String> lore = new ArrayList<>();
                                    lore.add(" ");
                                    lore.addAll(Arrays.stream(ChatPaginator.wordWrap(get(desc.value()), 30)).map(s -> ChatColor.GRAY + s).collect(Collectors.toList()));
                                    meta.setLore(lore);
                                }
                            },
                            nbt -> {
                                nbt.setID("setting_toggle");
                                nbt.set("display", display);
                                nbt.set("section", section);
                                nbt.set(SETTING_TAG, setting);
                                nbt.set("type", type);
                                nbt.set("value", next.name());
                            }
                    );

                    NovaSound.ITEM_BOOK_PAGE_TURN.play(p);

                    switch (section.toLowerCase()) {
                        case CORPORATION_TAG: {
                            Corporation c = Corporation.byOwner(p);

                            @SuppressWarnings({"rawtypes"})
                            Settings.Corporation<Enum> sett = Settings.Corporation.valueOf(setting, Enum.class);
                            c.setSetting(sett, next);

                            CorporationSettingChangeEvent event = new CorporationSettingChangeEvent(c, value, next, sett);
                            Bukkit.getPluginManager().callEvent(event);
                            break;
                        }
                    }
                } else throw new RuntimeException("Unknown setting type: " + type);

                e.getView().setItem(e.getRawSlot(), nItem);
                p.updateInventory();
            })
            .put("back:settings", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                getCommandWrapper().settings(p, null);
            })
            .put("business:home", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                boolean anonymous = of(item).getBoolean("anonymous");
                Business b = getBusiness(item);

                if (anonymous) {
                    p.sendMessage(get("plugin.prefix") + ChatColor.RED + get("constants.business.anonymous_home"));
                    return;
                }

                if (!b.hasHome()) {
                    p.sendMessage(b.isOwner(p) ? getMessage("error.business.no_home") : format(getMessage("error.business.no_home_user"), b.getName()));
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
                Player p = (Player) e.getWhoClicked();
                getCommandWrapper().settings(p, BUSINESS_TAG);
            })
            .put("business:statistics", (e, inv) -> {
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
                Player p = (Player) e.getWhoClicked();

                ItemStack item = e.getCurrentItem();
                int rating = of(item).getInt("rating");
                int newRating = rating + 1 > 4 ? 0 : rating + 1;

                ItemStack nItem = builder(item.clone(),
                        meta -> meta.setDisplayName(ChatColor.YELLOW + String.valueOf(newRating + 1) + "⭐"),
                        nbt -> {
                            nbt.setID("business:rating");
                            nbt.set("rating", newRating);
                        }
                );
                nItem.setType(RATING_MATS[newRating]);
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
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                int rating = nbt.getInt("rating") + 1;
                UUID businessId = nbt.getUUID(BUSINESS_TAG);
                String comment = nbt.getString("comment");

                Rating r = new Rating(p, businessId, rating, System.currentTimeMillis(), comment);
                Business b = Business.byId(businessId);

                PlayerRateBusinessEvent event = new PlayerRateBusinessEvent(p, b, r);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    np.setRating(event.getRating());
                    p.closeInventory();
                    p.sendMessage(format(getMessage("success.business.rate"), b.getName(), rating));
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                }
            })
            .put("business:click", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                boolean notOwner = !b.isOwner(p);

                p.openInventory(generateBusinessData(b, p, notOwner, SortingType.PRODUCT_NAME_ASCENDING).get(0));
                NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p, 1F, 0.5F);

                if (notOwner) {
                    b.getStatistics().addView();
                    b.saveBusiness();

                    BusinessViewEvent event = new BusinessViewEvent(p, b);
                    Bukkit.getPluginManager().callEvent(event);
                }
            })
            .put("product:edit_price", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                BusinessProduct pr = (BusinessProduct) getProduct(item);

                double price = of(item).getDouble(PRICE_TAG);
                Economy econ = getEconomy(item);
                Business b = pr.getBusiness();

                b.getProduct(pr.getItem()).setPrice(new Price(econ, price));

                String display = pr.getItem().hasItemMeta() && pr.getItem().getItemMeta().hasDisplayName() ? pr.getItem().getItemMeta().getDisplayName() : NovaWord.capitalize(pr.getItem().getType().name().replace('_', ' '));
                p.sendMessage(format(getMessage("success.business.edit_price"), display, format("%,.2f", price) + econ.getSymbol()));
                p.closeInventory();
            })
            .put("player_stats", (e, inv) -> {
                ItemStack item = e.getCurrentItem();
                Player p = (Player) e.getWhoClicked();
                OfflinePlayer target = Bukkit.getOfflinePlayer(of(item).getUUID("player"));

                getCommandWrapper().playerStatistics(p, target);
            })
            .put("business:advertising", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();

                getCommandWrapper().businessAdvertising(p);
            })
            .put("business:change_advertising", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();

                Business b = getBusiness(item);
                boolean add = of(item).getBoolean("add");
                double amount = getAmount(item);
                amount = add ? amount : -amount;

                ItemStack econWheel = inv.getItem(31);
                Economy econ = getEconomy(econWheel);
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
                        meta -> meta.setDisplayName(ChatColor.GOLD + format("%,.0f", newAmount) + econ.getSymbol())
                );
                inv.setItem(40, total);

                (add ? NovaSound.ENTITY_ARROW_HIT_PLAYER : NovaSound.BLOCK_NOTE_BLOCK_PLING).play(p, 1F, add ? 2F : 0F);
            })
            .put("economy:wheel:change_advertising", (e, inv) -> {
                items().get("economy:wheel").accept(e, inv);

                ItemStack item = e.getCurrentItem();
                Economy econ = getEconomy(item);

                ItemStack confirm = inv.getItem(39);
                double currentTotal = getAmount(confirm);

                ItemStack total = Items.builder(inv.getItem(40),
                        meta -> meta.setDisplayName(ChatColor.GOLD + format("%,.0f", currentTotal) + econ.getSymbol())
                );
                inv.setItem(40, total);
            })
            .put("yes:deposit_advertising", (e, inv) -> BUSINESS_ADVERTISING_BICONSUMER.accept(e, true))
            .put("yes:withdraw_advertising", (e, inv) -> BUSINESS_ADVERTISING_BICONSUMER.accept(e, false))
            .put("business:click:advertising", (e, inv) -> {
                items().get("business:click").accept(e, inv);
                ItemStack item = e.getCurrentItem();

                Business to = getBusiness(item);
                Business from = Business.byId(of(item).getUUID("from_business"));

                double add = NovaConfig.getConfiguration().getBusinessAdvertisingReward();
                if (to.getAdvertisingBalance() < add) return;

                Set<Economy> economies = Economy.getClickableRewardEconomies();
                Economy randomEcon = economies.stream().skip(r.nextInt(economies.size())).findFirst().orElse(null);

                to.removeAdvertisingBalance(add, randomEcon);
                from.addAdvertisingBalance(add, randomEcon);
            })
            .put("business:click:advertising_external", (e, inv) -> {
                items().get("business:click").accept(e, inv);
                ItemStack item = e.getCurrentItem();

                Business to = getBusiness(item);
                double remove = NovaConfig.getConfiguration().getBusinessAdvertisingReward();

                Set<Economy> economies = Economy.getClickableRewardEconomies();
                Economy randomEcon = economies.stream().skip(r.nextInt(economies.size())).findFirst().orElse(null);

                to.removeAdvertisingBalance(remove, randomEcon);
            })
            .put("business:pick_rating", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                OfflinePlayer owner = Bukkit.getOfflinePlayer(nbt.getUUID("owner"));
                boolean anon = nbt.getBoolean("anonymous");

                if (anon) return;

                getCommandWrapper().businessRating(p, owner);
            })
            .put("business:all_ratings", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                p.openInventory(getRatingsGUI(p, b).get(0));
            })
            .put("next:ratings", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                CHANGE_PAGE_TRICONSUMER.accept(e, 1, getRatingsGUI(p, b));
            })
            .put("prev:ratings", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                CHANGE_PAGE_TRICONSUMER.accept(e, -1, getRatingsGUI(p, b));
            })
            .put("business:leaderboard_category", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();

                String category = of(item).getString("category");
                String nextCategory = BL_CATEGORIES.get(BL_CATEGORIES.indexOf(category) == BL_CATEGORIES.size() - 1 ? 0 : BL_CATEGORIES.indexOf(category) + 1);

                getCommandWrapper().businessLeaderboard(p, nextCategory);
            })
            .put("economy:wheel:pay", (e, inv) -> {
                items().get("economy:wheel").accept(e, inv);

                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                ItemStack item = e.getCurrentItem();

                Economy econ = getEconomy(item);

                ItemStack head1 = inv.getItem(10).clone();
                ItemMeta h1Meta = head1.getItemMeta();
                h1Meta.setLore(Collections.singletonList(ChatColor.GOLD + format("%,.2f", np.getBalance(econ)) + econ.getSymbol()));
                head1.setItemMeta(h1Meta);
                inv.setItem(10, head1);

                ItemStack currentAmountO = inv.getItem(40);
                double amount = getAmount(currentAmountO);

                if (np.getBalance(econ) < amount) amount = np.getBalance(econ);
                final double fAmount = amount;

                ItemStack currentAmount = builder(currentAmountO,
                        meta -> meta.setDisplayName(ChatColor.GOLD + format("%,.2f", fAmount) + econ.getSymbol()),
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
                        meta -> meta.setDisplayName(ChatColor.GOLD + format("%,.2f", newAmount) + econ.getSymbol()),
                        nbt -> {
                            nbt.set(ECON_TAG, econ.getUniqueId());
                            nbt.set(AMOUNT_TAG, newAmount);
                        }
                );

                inv.setItem(40, currentAmount);

                sound.play(p, 1F, sound == NovaSound.BLOCK_NOTE_BLOCK_PLING ? 0F : (add ? 2F : 0F));
            })
            .put("pay:confirm", (e, inv) -> {
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

                String amountS = format("%,.2f", amount) + econ.getSymbol();
                String gameName = p.getDisplayName() == null ? p.getName() : p.getDisplayName();

                target.sendMessage(format(getMessage("success.economy.receive"), amountS, gameName));
                w.sendActionbar(target, format(get("success.economy.receive_actionbar"), amountS, gameName));
            })
            .put("corporation:hq", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                if (c.getHeadquarters() == null) {
                    p.sendMessage(getError("error.corporation.no_hq"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                p.closeInventory();
                p.teleport(c.getHeadquarters());
                p.sendMessage(get("constants.teleporting"));
                NovaSound.ENTITY_ENDERMAN_TELEPORT.playSuccess(p);
            })
            .put("corporation:click", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationData(c, p, SortingType.BUSINESS_NAME_ASCENDING));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);

                if (!c.isOwner(p)) c.addView();
            })
            .put("next:corp_leveling", (e, inv) -> CORPORATION_LEVELING_TRICONSUMER.accept(e, inv, 1))
            .put("prev:corp_leveling", (e, inv) -> CORPORATION_LEVELING_TRICONSUMER.accept(e, inv, -1))
            .put("corporation:edit_desc", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                if (!c.isOwner(p)) {
                    p.sendMessage(getError("error.corporation.not_owner"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                p.closeInventory();
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);

                w.sendSign(p, lines -> {
                    String desc = String.join("", lines);

                    if (desc.isEmpty()) {
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                    NovaInventory preview = genGUI(27, get("constants.confirm"));
                    preview.setCancelled();

                    preview.setItem(13, Items.builder(OAK_SIGN,
                            meta -> {
                                meta.setDisplayName(ChatColor.GOLD + get("constants.description"));
                                meta.setLore(Collections.singletonList(
                                        ChatColor.YELLOW + "\"" + desc + "\""
                                ));
                            }
                    ));

                    preview.setItem(21, CANCEL);
                    preview.setItem(23, builder(CONFIRM, nbt -> {
                        nbt.setID("yes:corporation:edit_desc");
                        nbt.set(CORPORATION_TAG, c.getUniqueId());
                        nbt.set("description", desc);
                    }));

                    p.openInventory(preview);
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                });
            })
            .put("yes:corporation:edit_desc", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);
                String desc = of(item).getString("description");

                if (!c.isOwner(p)) {
                    p.sendMessage(getError("error.corporation.not_owner"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                c.setDescription(desc);
                p.sendMessage(getSuccess("success.corporation.description"));
                p.closeInventory();
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:leveling", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationLeveling(c, c.getLevel()));
            })
            .put("sorter", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();

                int add = e.isRightClick() ? -1 : 1;

                SortingType<?> type = NovaUtil.byId(of(item).getString(TYPE_TAG));

                Function<SortingType<?>, NovaInventory> nextInventory = inv.getAttribute("sorting_function", Function.class);
                Class<?> sortingType = inv.getAttribute("sorting_type", Class.class);

                SortingType<?>[] array = SortingType.values(sortingType);

                int nextI = Arrays.asList(array).indexOf(type) + add;
                if (nextI < 0) nextI = array.length - 1;
                if (nextI >= array.length) nextI = 0;

                SortingType<?> next = array[nextI];

                p.openInventory(nextInventory.apply(next));
                NovaSound.ITEM_BOOK_PAGE_TURN.play(p);
            })
            .put("corporation:achievements", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationAchievements(c));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:statistics", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationStatistics(c));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("business:invite", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);
                Corporation from = Corporation.byId(of(item).getUUID("from"));

                switch (e.getClick()) {
                    case RIGHT:
                    case SHIFT_RIGHT: {
                        p.openInventory(confirm(p, () -> {
                            getCommandWrapper().acceptCorporationInvite(p, from);
                            p.closeInventory();
                        }, () -> {
                            p.openInventory(generateBusinessInvites(b, SortingType.CORPORATION_INVITE_CORPORATION_ASCENDING));
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        }));
                        break;
                    }
                    case LEFT:
                    case SHIFT_LEFT: {
                        p.openInventory(confirm(p, () -> {
                            getCommandWrapper().declineCorporationInvite(p, from);
                            p.closeInventory();
                        }, () -> {
                            p.openInventory(generateBusinessInvites(b, SortingType.CORPORATION_INVITE_CORPORATION_ASCENDING));
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        }));
                        break;
                    }
                    default:
                        break;
                }
            })
            .put("business:invites", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                p.openInventory(generateBusinessInvites(b, SortingType.CORPORATION_INVITE_CORPORATION_ASCENDING));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:settings", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                getCommandWrapper().settings(p, CORPORATION_TAG);
            })
            .put("economy:wheel:market_access", (e, inv) -> {
                items().get("economy:wheel").accept(e, inv);

                Economy econ = getEconomy(inv.getItem(12));

                ItemStack display = inv.getItem(14).clone();
                ItemMeta meta = display.getItemMeta();
                meta.setLore(Arrays.asList(
                        ChatColor.GOLD + format(get("constants.price"), format("%,.2f", NovaConfig.getMarket().getMarketMembershipCost(econ)), String.valueOf(econ.getSymbol()))
                ));
                display.setItemMeta(meta);
                inv.setItem(14, display);
            })
            .put("market:category", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                MarketCategory category = MarketCategory.valueOf(of(item).getString("category"));

                SortingType<Material> sorter = inv.getAttribute("sorter", SortingType.class);
                Economy econ = getEconomy(inv.getItem(45));
                int page = inv.getAttribute("page", Integer.class);

                p.openInventory(generateMarket(p, category, sorter, econ, page));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.play(p, 1F, 1.5F);
            })
            .put("market:page", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();

                NBTWrapper nbt = of(item);
                MarketCategory category = inv.getAttribute("category", MarketCategory.class);
                int next = nbt.getInt("page");
                boolean add = nbt.getBoolean("operation");

                SortingType<Material> sorter = inv.getAttribute("sorter", SortingType.class);
                Economy econ = getEconomy(inv.getItem(45));

                p.openInventory(generateMarket(p, category, sorter, econ, next));
                if (add) NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                else NovaSound.ENTITY_ARROW_HIT_PLAYER.playFailure(p);
            })
            .put("market:buy_product", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                long max = NovaConfig.getMarket().getMaxPurchases();
                long purchasesLeft = max - np.getPurchases().stream().filter(Receipt::isRecent).count();
                if (max > 0 && purchasesLeft <= 0) {
                    p.sendMessage(getError("error.market.max_purchases"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                ItemStack item = e.getCurrentItem();

                NBTWrapper nbt = of(item);
                Economy econ = getEconomy(item);
                Material product = Material.valueOf(nbt.getString(PRODUCT_TAG));

                if (NovaConfig.getMarket().getStock(product) <= 0) {
                    p.sendMessage(getError("error.market.out_of_stock"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                NovaInventory confirm = genGUI(27, get("constants.market.confirm_buy_product"));
                confirm.setCancelled();

                confirm.setAttribute(ECON_TAG, econ.getUniqueId());

                confirm.setItem(12, new ItemStack(product));
                confirm.setItem(14, builder(Items.OAK_SIGN,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + get("constants.set_amount")),
                        n -> {
                            n.setID("market:product_amount");
                            n.set(PRODUCT_TAG, product.name());
                        }
                ));

                confirm.setItem(21, Items.CANCEL);
                confirm.setItem(23, Items.yes("buy_market_product"));

                p.openInventory(confirm);
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("market:product_amount", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Material product = Material.valueOf(of(item).getString(PRODUCT_TAG));

                w.sendSign(p, lines -> {
                    String amountS = String.join("", lines);

                    if (amountS.isEmpty()) {
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                    try {
                        int amount = Integer.parseInt(amountS);

                        if (NovaConfig.getMarket().getStock(product) < amount) {
                            p.sendMessage(getError("error.market.not_enough_stock"));
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                            p.openInventory(inv);
                            return;
                        }

                        ItemStack newItem = inv.getItem(12).clone();

                        if (amount <= 0 || amount > newItem.getMaxStackSize()) {
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                            p.openInventory(inv);
                            return;
                        }

                        newItem.setAmount(amount);
                        inv.setItem(12, newItem);
                        p.openInventory(inv);
                    } catch (NumberFormatException ex) {
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        p.openInventory(inv);
                    }
                });
            })
            .put("yes:buy_market_product", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                Economy econ = Economy.getEconomy(inv.getAttribute(ECON_TAG, UUID.class));

                ItemStack productI = inv.getItem(12).clone();
                Material product = productI.getType();
                int amount = productI.getAmount();

                if (np.getBalance(econ) < NovaConfig.getMarket().getPrice(product, econ) * amount) {
                    p.sendMessage(getError("error.market.not_enough_money"));
                    p.closeInventory();
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                try {
                    NovaConfig.getMarket().buy(p, product, amount, econ);

                    if (p.getInventory().firstEmpty() == -1)
                        p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(product, amount));
                    else
                        p.getInventory().addItem(new ItemStack(product, amount));

                    p.sendMessage(format(getSuccess("success.market.buy_product"), ChatColor.GOLD + WordUtils.capitalizeFully(product.name().replace('_', ' '))));
                    p.closeInventory();
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                } catch (CancellationException ignored) {}
            })
            .put("market:buy_access", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                Economy econ = getEconomy(inv.getItem(12));

                double price = NovaConfig.getMarket().getMarketMembershipCost(econ);
                if (np.getBalance(econ) < price) {
                    p.sendMessage(getError("error.market.membership_cost"));
                    p.closeInventory();
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                np.remove(econ, price);
                np.setMarketAccess(true);

                p.sendMessage(getSuccess("success.market.membership"));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                getCommandWrapper().openMarket(p, econ);
            })
            .put("market:sell_items", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                Economy econ = getEconomy(inv.getItem(50));

                List<ItemStack> items = Arrays.stream(inv.getContents())
                        .filter(Objects::nonNull)
                        .filter(i -> !of(i).hasID())
                        .collect(Collectors.toList());

                for (ItemStack item : items)
                    if (!NovaConfig.getMarket().isSold(item.getType())) {
                        items.forEach(i -> {
                            if (p.getInventory().firstEmpty() == -1)
                                p.getWorld().dropItemNaturally(p.getLocation(), i);
                            else
                                p.getInventory().addItem(i);
                        });

                        p.sendMessage(format(getError("error.market.not_sold"), item.getType().name()));
                        p.closeInventory();
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                double profit = items.stream()
                        .mapToDouble(i -> NovaConfig.getMarket().getPrice(i.getType(), econ) * i.getAmount())
                        .sum() * NovaConfig.getMarket().getSellPercentage();

                NovaInventory confirm = confirm(p, () -> {
                    np.add(econ, profit);

                    if (NovaConfig.getMarket().isSellStockEnabled())
                        items.forEach(i -> NovaConfig.getMarket().addStock(i.getType(), i.getAmount()));

                    p.sendMessage(format(getSuccess("success.market.sell_items"), format("%,d", items.size())));
                    p.closeInventory();
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                }, () -> {
                    items.forEach(i -> {
                        if (p.getInventory().firstEmpty() == -1)
                            p.getWorld().dropItemNaturally(p.getLocation(), i);
                        else
                            p.getInventory().addItem(i);
                    });

                    p.closeInventory();
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                });

                ItemStack icon = Items.builder(econ.getIconType(),
                        meta -> meta.setDisplayName(ChatColor.GREEN + format(get("constants.profit"), ChatColor.GOLD + format("%,.2f", profit) + econ.getSymbol()))
                );
                modelData(icon, econ.getCustomModelData());
                confirm.setItem(13, icon);

                p.openInventory(confirm);
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("economy:wheel:market", (e, inv) -> {
                items().get("economy:wheel").accept(e, inv);

                Player p = (Player) e.getWhoClicked();

                Economy econ = getEconomy(e.getCurrentItem());
                MarketCategory category = inv.getAttribute("category", MarketCategory.class);
                SortingType<Material> sorter = inv.getAttribute("sorter", SortingType.class);
                int page = inv.getAttribute("page", Integer.class);

                p.openInventory(Generator.generateMarket(p, category, sorter, econ, page));
            })
            .put("next:business", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byId(inv.getAttribute("business", UUID.class));
                SortingType<BusinessProduct> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), BusinessProduct.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, 1, generateBusinessData(b, p, !b.getOwner().equals(p), type));
            })
            .put("prev:business", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byId(inv.getAttribute("business", UUID.class));
                SortingType<BusinessProduct> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), BusinessProduct.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, -1, generateBusinessData(b, p, !b.getOwner().equals(p), type));
            })
            .put("prev:stored", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                List<NovaInventory> invs = inv.getAttribute("invs", List.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, -1, invs);
            })
            .put("next:stored", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                List<NovaInventory> invs = inv.getAttribute("invs", List.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, 1, invs);
            })
            .put("corporation:leaderboard_category", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();

                String category = of(item).getString("category");
                String nextCategory = CL_CATEGORIES.get(CL_CATEGORIES.indexOf(category) == CL_CATEGORIES.size() - 1 ? 0 : CL_CATEGORIES.indexOf(category) + 1);

                getCommandWrapper().corporationLeaderboard(p, nextCategory);
            })
            .put("business:remove_supply_chest", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem(); NBTWrapper nbt = of(item);

                World w = Bukkit.getWorld(nbt.getUUID("world"));
                int x = nbt.getInt("x"), y = nbt.getInt("y"), z = nbt.getInt("z");
                Business bus = Business.byId(inv.getAttribute("business", UUID.class));

                Block b = w.getBlockAt(x, y, z);
                NovaInventory confirm = InventorySelector.confirm(p, () -> {
                    bus.removeSupplyChest(b.getLocation());
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                    p.closeInventory();
                });
                confirm.setItem(13, Items.builder(Material.CHEST,
                        meta -> meta.setDisplayName(ChatColor.BLUE + b.getWorld().getName() + ChatColor.GOLD + " | " + ChatColor.YELLOW + b.getX() + ", " + b.getY() + ", " + b.getZ())
                ));
                p.openInventory(confirm);
                NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
            })
            .put("business:supply_chests", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = getBusiness(e.getCurrentItem());

                p.openInventory(Generator.generateBusinessSupplyChests(b, SortingType.BLOCK_LOCATION_ASCENDING).get(0));
                NovaSound.BLOCK_CHEST_OPEN.play(p);
            })
            .build();

    static final Map<String, BiConsumer<InventoryClickEvent, NovaInventory>> CLICK_INVENTORIES = ImmutableMap.<String, BiConsumer<InventoryClickEvent, NovaInventory>>builder()
            .put("confirm_menu", (e, inv) -> {
                ItemStack item = e.getCurrentItem();
                String type = of(item).getString("type");
                if (type.isEmpty()) return;

                Runnable confirmR = inv.getAttribute("accept_action", Runnable.class);
                Runnable cancelR = inv.getAttribute("cancel_action", Runnable.class);

                switch (type) {
                    case "accept":
                        confirmR.run();
                        break;
                    case "cancel":
                        cancelR.run();
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown Type: " + type);
                }
            })
            .put("select_corporation_children", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                Consumer<Business> findAction = inv.getAttribute("find_action", Consumer.class);
                SortingType<Business> sorter = inv.getAttribute("sorter", SortingType.class);

                if (nbt.hasID()) switch (nbt.getID()) {
                    case "find_child:search": {
                        w.sendSign(p, lines -> {
                            String name = String.join("", lines).replaceAll("\\s", "");
                            if (name.isEmpty()) {
                                p.openInventory(InventorySelector.selectCorporationChildren(p, sorter, "", findAction));
                                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                                return;
                            }

                            p.openInventory(InventorySelector.selectCorporationChildren(p, sorter, name, findAction));
                            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                        });
                        break;
                    }
                    case "sorter":
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown ID: " + nbt.getID());
                }
                else {
                    Business b = getBusiness(item);
                    findAction.accept(b);
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                }
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
        if (!(e.getWhoClicked() instanceof Player)) return;

        NovaInventory inv = (NovaInventory) e.getClickedInventory();
        e.setCancelled(inv.isCancelled());

        if (e.getCurrentItem() == null) return;
        ItemStack item = e.getCurrentItem();

        if (item.isSimilar(GUI_BACKGROUND)) {
            e.setCancelled(true);
            return;
        }

        if (CLICK_INVENTORIES.containsKey(inv.getId())) CLICK_INVENTORIES.get(inv.getId()).accept(e, inv);

        String id = getID(item);
        if (CLICK_ITEMS.containsKey(id)) CLICK_ITEMS.get(id).accept(e, inv);
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory() instanceof NovaInventory)) return;
        NovaInventory inv = (NovaInventory) e.getView().getTopInventory();
        e.setCancelled(inv.isCancelled());

        for (ItemStack item : e.getNewItems().values()) {
            if (item == null) continue;
            if (item.isSimilar(GUI_BACKGROUND)) e.setCancelled(true);
            if (CLICK_ITEMS.containsKey(getID(item))) e.setCancelled(true);
        }
    }

    @EventHandler
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
            default:
                break;
        }
    }

    @EventHandler
    public void move(InventoryMoveItemEvent e) {
        if (e.getItem() == null) return;
        ItemStack item = e.getItem();

        if (!(e.getDestination() instanceof NovaInventory)) return;
        NovaInventory inv = (NovaInventory) e.getDestination();
        e.setCancelled(inv.isCancelled());

        if (item.isSimilar(GUI_BACKGROUND)) e.setCancelled(true);

        String id = getID(item);
        if (CLICK_ITEMS.containsKey(id)) e.setCancelled(true);
    }

}
