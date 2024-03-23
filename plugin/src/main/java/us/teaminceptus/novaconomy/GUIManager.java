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
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.auction.AuctionHouse;
import us.teaminceptus.novaconomy.api.auction.AuctionProduct;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;
import us.teaminceptus.novaconomy.api.business.BusinessStatistics;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationPermission;
import us.teaminceptus.novaconomy.api.corporation.CorporationRank;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.MarketCategory;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;
import us.teaminceptus.novaconomy.api.events.business.*;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationSettingChangeEvent;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationTeleportHeadquartersEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerRateBusinessEvent;
import us.teaminceptus.novaconomy.api.events.player.PlayerSettingChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPayEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.player.PlayerStatistics;
import us.teaminceptus.novaconomy.api.settings.SettingDescription;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;
import us.teaminceptus.novaconomy.api.util.Product;
import us.teaminceptus.novaconomy.util.NovaSound;
import us.teaminceptus.novaconomy.util.NovaUtil;
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
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.*;
import static us.teaminceptus.novaconomy.messages.MessageHandler.*;
import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;
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
        return Economy.byId(of(item).getUUID(ECON_TAG));
    }

    @Nullable
    private static Business getBusiness(ItemStack item) {
        return Business.byId(of(item).getUUID(BUSINESS_TAG));
    }

    @Nullable
    private static Corporation getCorporation(ItemStack item) {
        return Corporation.byId(of(item).getUUID(CORPORATION_TAG));
    }

    private static boolean getButton(ItemStack item) {
        return of(item).getBoolean("enabled");
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

        if (add && !np.canAfford(econ, amount)) {
            messages.sendMessage(p, "error.economy.invalid_amount", get(p, "constants.deposit"));
            return;
        }

        String msg = format("%,.2f", amount) + econ.getSymbol();

        if (add) {
            np.remove(econ, amount);
            b.addAdvertisingBalance(amount, econ);
            messages.sendMessage(p, "success.business.advertising_deposit", msg, b.getName());
        } else {
            np.add(econ, amount);
            b.removeAdvertisingBalance(amount, econ);
            messages.sendMessage(p, "success.business.advertising_withdraw", msg, b.getName());
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

        p.openInventory(generateCorporationLeveling(c, next, p));
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
                int index = economies.indexOf(Economy.byName(econ)) + 1;
                Economy newEcon = economies.get(index >= economies.size() ? 0 : index);

                inv.setItem(e.getSlot(), newEcon.getIcon());
            })
            .put("product:buy", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                ItemStack item = e.getCurrentItem();
                String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : NovaUtil.capitalize(item.getType().name().replace('_', ' '));

                if (!of(item).getBoolean("product:in_stock")) {
                    messages.send(p, "error.business.not_in_stock", name);
                    return;
                }

                BusinessProduct pr = (BusinessProduct) getProduct(item);

                if (!np.canAfford(pr, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseProducts())) {
                    messages.send(p, "error.economy.invalid_amount", get(p, "constants.purchase"));
                    return;
                }

                NovaInventory purchase = genGUI(27, NovaUtil.capitalize(get(p, "constants.purchase")) + " \"" + ChatColor.RESET + name + ChatColor.RESET + "\"?");
                purchase.setCancelled();

                for (int i = 10; i < 17; i++) purchase.setItem(i, GUI_BACKGROUND);

                purchase.setItem(13, item);

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

                        purchase.setItem(add ? 13 + (i + 1) : 13 - (i + 1), amountI);
                    }

                purchase.setItem(21, yes("buy_product", nbt -> nbt.set(PRODUCT_TAG, pr)));
                purchase.setItem(23, cancel("no_product", nbt -> nbt.set(BUSINESS_TAG, pr.getBusiness().getUniqueId())));

                ItemStack amountPane = builder(item.getType(),
                        meta -> {
                            meta.setDisplayName(ChatColor.YELLOW + "1");
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
                        }, nbt -> nbt.set(AMOUNT_TAG, 1)
                );
                purchase.setItem(22, amountPane);

                p.openInventory(purchase);
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

                double price = pr.getPrice().getAmount() * newA;

                if (!np.canAfford(pr.getEconomy(), price, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseProducts()))
                    inv.setItem(21, invalid(get(p, "constants.purchase")));
                else
                    inv.setItem(21, yes("buy_product", nbt -> nbt.set(PRODUCT_TAG, pr)));

                inv.setAttribute(PRODUCT_TAG, pr);

                p.updateInventory();
            })
            .put("no:no_product", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Business b = getBusiness(item);

                messages.send(p, "cancel.business.purchase");
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
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                int slot = e.getRawSlot();
                ItemStack item = e.getCurrentItem().clone();

                List<String> sortedList = new ArrayList<>();
                Economy.getEconomies().forEach(econ -> sortedList.add(econ.getName()));
                sortedList.sort(String.CASE_INSENSITIVE_ORDER);

                Economy econ = getEconomy(item);
                int nextI = sortedList.indexOf(econ.getName()) + (e.getClick().isRightClick() ? -1 : 1);
                Economy next = sortedList.size() == 1 ? econ : Economy.byName(sortedList.get(nextI == sortedList.size() ? 0 : nextI));

                String[] split = of(item).getID().split(":");
                String suffix = split.length <= 2 ? null : split[split.length - 1];

                e.getView().setItem(slot, economyWheel(suffix, next, p));
                NovaSound.BLOCK_NOTE_BLOCK_PLING.play(e.getWhoClicked());
            })
            .put("economy:wheel:add_product", (e, inv) -> {
                items().get("economy:wheel").accept(e, inv);

                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Economy econ = getEconomy(item);

                ItemStack display = inv.getItem(13).clone();
                ItemMeta dMeta = display.getItemMeta();
                List<String> lore = dMeta.hasLore() ? new ArrayList<>() : dMeta.getLore();
                lore.add(format(p, get(p, "constants.price"), of(display).getDouble(PRICE_TAG) / econ.getConversionScale(), econ.getSymbol()));

                dMeta.setLore(lore);
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

                Economy next = economies.get(nextI).equalsIgnoreCase("all") ? null : Economy.byName(economies.get(nextI));
                getCommandWrapper().balanceLeaderboard(p, next);
            })
            .put("yes:buy_product", (e, inv) -> {
                ItemStack item = e.getCurrentItem();
                Player p = (Player) e.getWhoClicked();

                if (p.getInventory().firstEmpty() == -1) {
                    messages.send(p, "error.player.full_inventory");
                    return;
                }

                NovaPlayer np = new NovaPlayer(p);
                BusinessProduct bP = (BusinessProduct) getProduct(item);

                ItemStack product = bP.getItem();
                int size = Math.min((int) of(inv.getItem(22)).getDouble(AMOUNT_TAG), bP.getBusiness().getTotalStock(product));
                product.setAmount(size);

                Economy econ = bP.getEconomy();
                double amount = bP.getPrice().getAmount() * size;

                if (!np.canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseProducts())) {
                    messages.send(p, "error.economy.invalid_amount", get(p, "constants.purchase"));
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
                if (newTransactions.size() > PlayerStatistics.MAX_TRANSACTION_HISTORY) newTransactions.remove(0);
                pStats.setTransactionHistory(newTransactions);
                np.save();

                ItemStack clone = product.clone();
                clone.setAmount(1);
                Product bPrS = new Product(clone, bPr.getPrice());
                bStats.getProductSales().put(bPrS, bStats.getProductSales().getOrDefault(bPrS, 0) + product.getAmount());
                b.saveBusiness();

                String material = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : NovaUtil.capitalize(product.getType().name().replace('_', ' '));

                messages.send(p, "success.business.purchase", material, bP.getBusiness().getName());
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
                    messages.sendNotification(bOwner, "notification.business.purchase", name, material);
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

                    String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : NovaUtil.capitalize(product.getType().name().replace('_', ' '));
                    messages.sendMessage(p, "success.business.add_product", name);
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

                messages.sendMessage(p, "success.business.add_resource", b.getName());
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
                String name = product.hasItemMeta() && product.getItemMeta().hasDisplayName() ? product.getItemMeta().getDisplayName() : NovaUtil.capitalize(product.getType().name().replace('_', ' '));

                messages.send(p, "success.business.remove_product", name, b.getName());

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
                if (!takeEcon.isConvertable()) {
                    p.closeInventory();
                    messages.sendError(p, "error.economy.transfer_not_convertable", takeEcon.getName());
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
                    return;
                }

                double take = getAmount(takeItem);
                if (!np.canAfford(takeEcon, take, NovaConfig.getConfiguration().getWhenNegativeAllowConvertBalances())) {
                    p.closeInventory();
                    messages.sendError(p, "error.economy.invalid_amount", get(p, "constants.convert"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
                    return;
                }

                double max = NovaConfig.getConfiguration().getMaxConvertAmount(takeEcon);
                if (max >= 0 && take > max) {
                    messages.sendMessage(p, "error.economy.transfer_max", format("%,.2f", max) + takeEcon.getSymbol(), format("%,.2f", take) + takeEcon.getSymbol());
                    p.closeInventory();
                    return;
                }

                ItemStack giveItem = inv.getItem(14);
                Economy giveEcon = getEconomy(giveItem);
                if (!giveEcon.isConvertable()) {
                    p.closeInventory();
                    messages.sendError(p, "error.economy.transfer_not_convertable", giveEcon.getName());
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
                    return;
                }

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
                messages.sendMessage(p, "success.economy.convert", format("%,.2f", take) + takeEcon.getSymbol(), format("%,.2f", give) + giveEcon.getSymbol());
            })
            .put("no:close", (e, inv) -> {
                e.getWhoClicked().closeInventory();
                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(e.getWhoClicked());
            })
            .put("next:bank_balance", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                SortingType<Economy> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), Economy.class);
                CHANGE_PAGE_TRICONSUMER.accept(e, 1, getBankBalanceGUI(type, p));
            })
            .put("prev:bank_balance", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                SortingType<Economy> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), Economy.class);
                CHANGE_PAGE_TRICONSUMER.accept(e, -1, getBankBalanceGUI(type, p));
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
                                meta.setDisplayName(ChatColor.YELLOW + display + ": " + (value ? ChatColor.RED + get(p, "constants.off") : ChatColor.GREEN + get(p, "constants.on")));
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
                            Settings.Business<Boolean> sett = Settings.Business.valueOf(setting, Boolean.class);
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
                    messages.sendError(p, "constants.business.anonymous_home");
                    return;
                }

                if (!b.hasHome()) {
                    messages.sendRawMessage(p, b.isOwner(p) ? get(p, "error.business.no_home") : format(p, get(p, "error.business.no_home_user"), b.getName()));
                    return;
                }

                if (b.getHome().distanceSquared(p.getLocation()) < 16) {
                    messages.sendMessage(p, "error.business.too_close_home");
                    return;
                }

                messages.sendRaw(p, ChatColor.DARK_AQUA + get(p, "constants.teleporting"));

                BusinessTeleportHomeEvent event = new BusinessTeleportHomeEvent(p, b);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    scheduler.teleport(p, event.getLocation());
                    NovaSound.ENTITY_ENDERMAN_TELEPORT.play(p, 1F, 1F);
                }
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
                    messages.sendError(p, "constants.business.anonymous_statistics");
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
                    messages.sendMessage(p, "success.business.rate", b.getName(), rating);
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

                String display = pr.getItem().hasItemMeta() && pr.getItem().getItemMeta().hasDisplayName() ? pr.getItem().getItemMeta().getDisplayName() : NovaUtil.capitalize(pr.getItem().getType().name().replace('_', ' '));
                messages.sendMessage(p, "success.business.edit_price", display, format("%,.2f", price) + econ.getSymbol());
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

                if (!np.canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowPayPlayers()))
                    amount = np.getBalance(econ);

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

                if (!np.canAfford(econ, amount, NovaConfig.getConfiguration().getWhenNegativeAllowPayPlayers())) {
                    messages.sendMessage(p, "error.economy.invalid_amount_pay");
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

                messages.sendMessage(target, "success.economy.receive", amountS, gameName);
                w.sendActionbar(target, format(p, get(p, "success.economy.receive_actionbar"), amountS, gameName));
            })
            .put("corporation:hq", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                if (c.getHeadquarters() == null) {
                    messages.sendError(p, "error.corporation.no_hq");
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                if (!c.getSetting(Settings.Corporation.PUBLIC_HEADQUARTERS) && !c.getMembers().contains(p)) {
                    messages.sendError(p, "error.corporation.private_hq");
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                p.closeInventory();

                CorporationTeleportHeadquartersEvent event = new CorporationTeleportHeadquartersEvent(p, c);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) {
                    scheduler.teleport(p, event.getLocation());
                    messages.sendRaw(p, ChatColor.AQUA + get(p, "constants.teleporting"));
                    NovaSound.ENTITY_ENDERMAN_TELEPORT.playSuccess(p);
                }
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

                p.closeInventory();
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);

                w.sendSign(p, lines -> {
                    String desc = String.join("", lines);

                    if (desc.isEmpty()) {
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                    NovaInventory preview = genGUI(27, get(p, "constants.confirm"));
                    preview.setCancelled();

                    preview.setItem(13, Items.builder(OAK_SIGN,
                            meta -> {
                                meta.setDisplayName(ChatColor.GOLD + get(p, "constants.description"));
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

                c.setDescription(desc);
                messages.sendSuccess(p, "success.corporation.description");
                p.closeInventory();
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:leveling", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationLeveling(c, c.getLevel(), p));
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

                p.openInventory(generateCorporationAchievements(c, p));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:statistics", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationStatistics(c, p));
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
                        p.openInventory(confirm(p, cInv -> {
                            getCommandWrapper().acceptCorporationInvite(p, from);
                            p.closeInventory();
                        }, cInv -> {
                            p.openInventory(generateBusinessInvites(b, SortingType.CORPORATION_INVITE_CORPORATION_ASCENDING, p));
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        }));
                        break;
                    }
                    case LEFT:
                    case SHIFT_LEFT: {
                        p.openInventory(confirm(p, cInv -> {
                            getCommandWrapper().declineCorporationInvite(p, from);
                            p.closeInventory();
                        }, cInv -> {
                            p.openInventory(generateBusinessInvites(b, SortingType.CORPORATION_INVITE_CORPORATION_ASCENDING, p));
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

                p.openInventory(generateBusinessInvites(b, SortingType.CORPORATION_INVITE_CORPORATION_ASCENDING, p));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:settings", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                getCommandWrapper().settings(p, CORPORATION_TAG);
            })
            .put("economy:wheel:market_access", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                int slot = e.getRawSlot();
                ItemStack item = e.getCurrentItem().clone();

                List<String> econs = Economy.getEconomies()
                        .stream()
                        .filter(econ -> {
                            Set<Economy> whitelisted = NovaConfig.getMarket().getWhitelistedEconomies();
                            if (!whitelisted.isEmpty())
                                return whitelisted.contains(econ);

                            Set<Economy> blacklisted = NovaConfig.getMarket().getBlacklistedEconomies();
                            if (!blacklisted.isEmpty())
                                return !blacklisted.contains(econ);

                            return true;
                        })
                        .map(Economy::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList());

                Economy econ = getEconomy(item);
                int nextI = econs.indexOf(econ.getName()) + (e.getClick().isRightClick() ? -1 : 1);
                Economy next = econs.size() == 1 ? econ : Economy.byName(econs.get(nextI == econs.size() ? 0 : nextI));

                item.setType(next.getIconType());
                modelData(item, next.getCustomModelData());

                item = builder(item,
                        meta -> {
                            meta.setDisplayName(ChatColor.GOLD + next.getName());
                            meta.setLore(Collections.singletonList(
                                    format(ChatColor.AQUA + get(p, "constants.balance"), ChatColor.GOLD + format("%,.2f", np.getBalance(econ) + econ.getSymbol()))
                            ));
                        },
                        nbt -> nbt.set(ECON_TAG, next.getUniqueId())
                );

                e.getView().setItem(slot, item);
                NovaSound.BLOCK_NOTE_BLOCK_PLING.play(p);

                ItemStack display = Items.builder(inv.getItem(14).clone(),
                    meta -> meta.setLore(Arrays.asList(
                            ChatColor.GOLD + format(p, get(p, "constants.price"), format("%,.2f", NovaConfig.getMarket().getMarketMembershipCost(econ)), String.valueOf(econ.getSymbol()))
                    ))
                );
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
                    messages.sendError(p, "error.market.max_purchases");
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                ItemStack item = e.getCurrentItem();

                NBTWrapper nbt = of(item);
                Economy econ = getEconomy(item);
                Material product = Material.valueOf(nbt.getString(PRODUCT_TAG));

                if (NovaConfig.getMarket().getStock(product) <= 0) {
                    messages.sendError(p, "error.market.out_of_stock");
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                NovaInventory confirm = genGUI(27, get(p, "constants.market.confirm_buy_product"));
                confirm.setCancelled();

                confirm.setAttribute(ECON_TAG, econ.getUniqueId());

                confirm.setItem(12, new ItemStack(product));
                confirm.setItem(14, builder(OAK_SIGN,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + get(p, "constants.set_amount")),
                        n -> {
                            n.setID("market:product_amount");
                            n.set(PRODUCT_TAG, product.name());
                        }
                ));

                confirm.setItem(21, CANCEL);
                confirm.setItem(23, yes("buy_market_product"));

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
                            messages.sendError(p, "error.market.not_enough_stock");
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

                Economy econ = Economy.byId(inv.getAttribute(ECON_TAG, UUID.class));

                ItemStack productI = inv.getItem(12).clone();
                Material product = productI.getType();
                int amount = productI.getAmount();
                double price = NovaConfig.getMarket().getPrice(product, econ) * amount;

                if (!np.canAfford(econ, price, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseMarket())) {
                    messages.sendError(p, "error.market.not_enough_money");
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

                    messages.sendSuccess(p, "success.market.buy_product", ChatColor.GOLD + WordUtils.capitalizeFully(product.name().replace('_', ' ')));
                    p.closeInventory();
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                } catch (CancellationException ignored) {}
            })
            .put("market:buy_access", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                Economy econ = getEconomy(inv.getItem(12));

                double price = NovaConfig.getMarket().getMarketMembershipCost(econ);
                if (!np.canAfford(econ, price, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseMarket())) {
                    messages.sendError(p, "error.market.membership_cost");
                    p.closeInventory();
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                np.remove(econ, price);
                np.setMarketAccess(true);

                messages.sendSuccess(p, "success.market.membership");
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

                        messages.sendError(p, "error.market.not_sold", item.getType().name());
                        p.closeInventory();
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                double profit = items.stream()
                        .mapToDouble(i -> NovaConfig.getMarket().getPrice(i.getType(), econ) * i.getAmount())
                        .sum() * NovaConfig.getMarket().getSellPercentage();

                NovaInventory confirm = confirm(p, cInv -> {
                    np.add(econ, profit);

                    if (NovaConfig.getMarket().isSellStockEnabled())
                        items.forEach(i -> NovaConfig.getMarket().addStock(i.getType(), i.getAmount()));

                    messages.sendSuccess(p, "success.market.sell_items", format("%,d", items.size()));
                    p.closeInventory();
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                }, cInv -> {
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
                        meta -> meta.setDisplayName(ChatColor.GREEN + format(p, get(p, "constants.profit"), ChatColor.GOLD + format("%,.2f", profit) + econ.getSymbol()))
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

                p.openInventory(generateMarket(p, category, sorter, econ, page));
            })
            .put("next:business", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byId(inv.getAttribute("business", UUID.class));
                SortingType<? super BusinessProduct> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), BusinessProduct.class);

                CHANGE_PAGE_TRICONSUMER.accept(e, 1, generateBusinessData(b, p, !b.getOwner().equals(p), type));
            })
            .put("prev:business", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                Business b = Business.byId(inv.getAttribute("business", UUID.class));
                SortingType<? super BusinessProduct> type = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), BusinessProduct.class);

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
                NovaInventory confirm = confirm(p, cInv -> {
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

                p.openInventory(generateBusinessSupplyChests(b, SortingType.BLOCK_LOCATION_ASCENDING, p).get(0));
                NovaSound.BLOCK_CHEST_OPEN.play(p);
            })
            .put("auction_house", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                p.openInventory(generateAuctionHouse(p, SortingType.PRODUCT_NAME_ASCENDING, inv.getAttribute("auction:search_query", String.class, "")).get(0));
                NovaSound.BLOCK_CHEST_OPEN.play(p);
            })
            .put("auction:click", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                AuctionProduct product = AuctionHouse.byId(nbt.getUUID(PRODUCT_TAG));
                ClickType type = e.getClick();

                boolean loose = product.isLoosePrice();
                Price originalPrice = !product.isBuyNow() && !AuctionHouse.getBids(product).isEmpty() ? AuctionHouse.getTopBid(product).getPrice().add(1.0) : product.getPrice();

                if (type.isLeftClick()) {
                    NovaInventory confirm = confirm(p, cInv -> {
                        Economy econ = loose ? getEconomy(cInv.getItem(14)) : originalPrice.getEconomy();
                        Price price;

                        if (product.isBuyNow())
                            price = loose ? originalPrice.convertTo(econ) : originalPrice;
                        else {
                            double amount = of(cInv.getItem(15)).getDouble("current") / econ.getConversionScale();
                            price = new Price(econ, amount);
                        }

                        if (!np.canAfford(price, NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseAuction())) {
                            messages.sendError(p, "error.economy.invalid_amount", get(p, "constants.purchase"));
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                            return;
                        }

                        if (product.isBuyNow())
                            AuctionHouse.purchase(p, product);
                        else
                            AuctionHouse.bid(p, product, price);

                        p.closeInventory();
                        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                    }, cInv -> {
                        p.openInventory(inv);
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    });

                    if (loose) {
                        confirm.setItem(12, product.getItem());
                        confirm.setItem(14, economyWheel(p, originalPrice.getEconomy()));
                    } else
                        confirm.setItem(13, product.getItem());

                    if (!product.isBuyNow())
                        confirm.setItem(15, builder(OAK_SIGN,
                                meta -> {
                                    meta.setDisplayName(ChatColor.YELLOW + get(p, "constants.set_amount"));
                                    meta.setLore(Collections.singletonList(
                                            ChatColor.GOLD + format(p, get(p, "constants.price"), format("%,.2f", originalPrice.getAmount()), originalPrice.getEconomy().getSymbol())
                                    ));
                                },
                                n -> {
                                    n.setID("auction:bid");
                                    n.set("current", originalPrice.getRealAmount());
                                    n.set("min", originalPrice.getRealAmount());
                                    n.set("loose", loose);
                                    if (!loose) n.set(ECON_TAG, originalPrice.getEconomy().getUniqueId());
                                }
                        ));

                    p.openInventory(confirm);
                }
                else {
                    p.openInventory(generateAuctionInfo(p, product, inv.getAttribute("auction:search_query", String.class, "")));
                    NovaSound.BLOCK_ENDER_CHEST_OPEN.play(p);
                }
            })
            .put("auction:bid", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                Economy econ = nbt.getBoolean("loose") ? getEconomy(inv.getItem(14)) : getEconomy(item);
                double min = nbt.getDouble("min");

                w.sendSign(p, lines -> {
                    String amountS = String.join("", lines).replaceAll("\\s", "");

                    try {
                        double amount = Double.parseDouble(amountS) * econ.getConversionScale();

                        if (amount < min) {
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                            p.openInventory(inv);
                            return;
                        }

                        inv.setItem(e.getSlot(), builder(item,
                                meta -> meta.setLore(Collections.singletonList(
                                        ChatColor.GOLD + format(p, get(p, "constants.price"), format("%,.2f", amount), econ.getSymbol())
                                )),
                                n -> n.set("current", amount)
                        ));
                        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                    } catch (NumberFormatException ex) {
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    }

                    p.openInventory(inv);
                });
            })
            .put("auction:remove_item", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                AuctionProduct product = AuctionHouse.byId(nbt.getUUID("product"));
                if (!product.getOwner().equals(p)) {
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                NovaInventory confirm = confirm(p, cInv -> {
                    AuctionHouse.removeProduct(product);
                    np.awardAuction(product);

                    p.closeInventory();
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                });

                confirm.setItem(13, product.getItem());
                p.openInventory(confirm);
            })
            .put("auction_house:search", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                SortingType<? super AuctionProduct> sorting = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), AuctionProduct.class);

                ItemStack item = e.getCurrentItem();
                if (of(item).getBoolean("clear")) {
                    p.openInventory(generateAuctionHouse(p, sorting, "").get(0));
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playFailure(p);
                    return;
                }

                w.sendSign(p, lines -> {
                    String query = String.join("", lines).replaceAll("\\s", "");
                    if (query.isEmpty()) {
                        p.openInventory(inv);
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                    p.openInventory(generateAuctionHouse(p, sorting, query).get(0));
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                });
            })
            .put("auction_house:jump_page", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                String query = inv.getAttribute("auction:search_query", String.class);
                SortingType<? super AuctionProduct> sorting = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), AuctionProduct.class);

                w.sendSign(p, lines -> {
                    String pageS = String.join("", lines).replaceAll("\\s", "");
                    if (pageS.isEmpty()) {
                        p.openInventory(inv);
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                    try {
                        int page = Integer.parseInt(pageS);
                        List<NovaInventory> invs = generateAuctionHouse(p, sorting, query);

                        p.openInventory(invs.get(Math.max(Math.min(page - 1, invs.size() - 1), 0)));
                        NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                    } catch (NumberFormatException ex) {
                        p.openInventory(inv);
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    }
                });
            })
            .put("auction:add_item", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();

                ItemStack clicked = e.getCurrentItem();
                NBTWrapper nbt = of(clicked);
                Price price = new Price(getEconomy(inv.getItem(21)), nbt.getDouble(PRICE_TAG));
                ItemStack item = inv.getAttribute("item", ItemStack.class);
                boolean buyNow = getButton(inv.getItem(23));
                boolean loose = getButton(inv.getItem(24));

                AuctionProduct product = AuctionHouse.addItem(p, item, price, buyNow, loose);
                p.getInventory().removeItem(item);

                p.openInventory(generateAuctionInfo(p, product, ""));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("button", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                String name = nbt.getString("name");
                boolean next = !nbt.getBoolean("enabled");
                inv.setItem(e.getSlot(), button(name, next));
            })
            .put("auction:end", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                ItemStack item = e.getCurrentItem();
                AuctionProduct product = AuctionHouse.byId(of(item).getUUID("product"));

                NovaInventory confirm = confirm(p, cInv -> {
                    if (product.isBuyNow() || AuctionHouse.getBids(product).isEmpty())
                        np.awardAuction(product);

                    AuctionHouse.endAuction(product);
                    p.closeInventory();
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                });

                confirm.setItem(13, product.getItem());
                p.openInventory(confirm);
            })
            .put("auction_house:my_auctions", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                if (AuctionHouse.getProducts(p).isEmpty()) {
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                String query = inv.getAttribute("auction:search_query", String.class);
                SortingType<? super AuctionProduct> sorting = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), AuctionProduct.class);

                p.openInventory(generateAuctionHouse(p, sorting, query, a -> a.getOwner().equals(p)).get(0));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("auction_house:won_auctions", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);

                p.openInventory(generateWonAuctions(p).get(0));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("auction:claim", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                NovaPlayer np = new NovaPlayer(p);
                ItemStack item = e.getCurrentItem();

                int page = inv.getAttribute("page", Integer.class);
                AuctionProduct product = np.getWonAuction(of(item).getUUID(PRODUCT_TAG));

                if (product == null) {
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                if (!np.canAfford(product.getPrice(), NovaConfig.getConfiguration().getWhenNegativeAllowPurchaseAuction())) {
                    messages.sendError(p, "error.economy.invalid_amount", get(p, "constants.claim"));
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                np.remove(product.getPrice());
                np.awardAuction(product);
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                p.openInventory(generateWonAuctions(p).get(page));
            })
            .put("auction_house:my_bids", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                if (AuctionHouse.getBidsBy(p).isEmpty()) {
                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                    return;
                }

                String query = inv.getAttribute("auction:search_query", String.class);
                SortingType<? super AuctionProduct> sorting = NovaUtil.byId(of(inv.getItem(18)).getString(TYPE_TAG), AuctionProduct.class);

                p.openInventory(generateAuctionHouse(p, sorting, query, a ->
                        AuctionHouse.getBids(a)
                                .stream()
                                .anyMatch(b -> b.getBidder().equals(p))
                ).get(0));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:ranks", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);

                p.openInventory(generateCorporationRanks(p, c));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:edit_rank", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                Corporation c = getCorporation(item);
                CorporationRank rank = c.getRank(of(item).getUUID("rank"));

                p.openInventory(generateCorporationRankEditor(p, rank));
                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            })
            .put("corporation:edit_rank:item", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                Corporation c = getCorporation(item);
                CorporationRank rank = c.getRank(nbt.getUUID("rank"));
                String type = nbt.getString(TYPE_TAG);

                w.sendSign(p, lines -> {
                    String res = String.join(" ", lines).trim();
                    if (res.isEmpty()) {
                        p.openInventory(inv);
                        NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                        return;
                    }

                    switch (type) {
                        case "name": {
                            if (res.length() > CorporationRank.MAX_NAME_LENGTH) {
                                messages.sendError(p, "error.argument.length", CorporationRank.MAX_NAME_LENGTH);
                                p.openInventory(inv);
                                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                                return;
                            }

                            if (!CorporationRank.VALID_NAME.matcher(res).matches()) {
                                messages.sendError(p, "error.argument.name");
                                p.openInventory(inv);
                                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                                return;
                            }

                            rank.setName(res);
                            p.openInventory(Generator.generateCorporationRankEditor(p, rank));
                            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                            break;
                        }
                        case "prefix": {
                            if (res.length() > CorporationRank.MAX_PREFIX_LENGTH) {
                                messages.sendError(p, "error.argument.length", CorporationRank.MAX_PREFIX_LENGTH);
                                p.openInventory(inv);
                                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                                return;
                            }

                            if (!CorporationRank.VALID_PREFIX.matcher(res).matches()) {
                                messages.sendError(p, "error.argument.prefix");
                                p.openInventory(inv);
                                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                                return;
                            }

                            rank.setPrefix(res);
                            p.openInventory(Generator.generateCorporationRankEditor(p, rank));
                            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                            break;
                        }
                        case "icon": {
                            Material icon = Material.matchMaterial(res);
                            if (icon == null) {
                                messages.sendError(p, "error.argument.icon");
                                p.openInventory(inv);
                                NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                                return;
                            }

                            rank.setIcon(icon);
                            p.openInventory(Generator.generateCorporationRankEditor(p, rank));
                            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                            break;
                        }
                        default: {
                            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
                            throw new AssertionError("Unknown Type: " + type);
                        }
                    }
                });
            })
            .put("corporation:edit_rank:toggle_permission", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper itemNbt = of(item);

                Corporation c = getCorporation(item);
                CorporationRank rank = c.getRank(itemNbt.getUUID("rank"));
                CorporationPermission permission = CorporationPermission.valueOf(itemNbt.getString("permission"));
                boolean perm = !itemNbt.getBoolean("state");

                if (perm) {
                    rank.addPermission(permission);
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
                } else {
                    rank.removePermission(permission);
                    NovaSound.ENTITY_ARROW_HIT_PLAYER.playFailure(p);
                }

                inv.setItem(e.getSlot(), Generator.generateCorporationPermissionNode(c, rank, permission, perm, p));
            })
            .put("language:select", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();

                InventorySelector.selectLanguage(p, l -> {
                    NovaPlayer np = new NovaPlayer(p);
                    np.setLanguage(l);
                    messages.sendSuccess(p, "success.language.set", ChatColor.GOLD + NovaUtil.capitalize(l.name()));

                    NovaSound.BLOCK_NOTE_BLOCK_PLING.playSuccess(p);

                    p.openInventory(Generator.generateLanguageSettings(p));
                });
            })
            .build();

    static final Map<String, BiConsumer<InventoryClickEvent, NovaInventory>> CLICK_INVENTORIES = ImmutableMap.<String, BiConsumer<InventoryClickEvent, NovaInventory>>builder()
            .put("confirm_menu", (e, inv) -> {
                ItemStack item = e.getCurrentItem();
                String type = of(item).getString("type");
                if (type.isEmpty()) return;

                Consumer<NovaInventory> confirmR = inv.getAttribute("accept_action", Consumer.class);
                Consumer<NovaInventory> cancelR = inv.getAttribute("cancel_action", Consumer.class);

                switch (type) {
                    case "accept":
                        confirmR.accept(inv);
                        break;
                    case "cancel":
                        cancelR.accept(inv);
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
            .put("select_language", (e, inv) -> {
                Player p = (Player) e.getWhoClicked();
                ItemStack item = e.getCurrentItem();
                NBTWrapper nbt = of(item);

                Consumer<Language> action = inv.getAttribute("action", Consumer.class);

                if (nbt.hasID()) switch (nbt.getID()) {
                    case "language:option": {
                        String lang = nbt.getString("language");
                        if (lang.isEmpty()) return;

                        Language l = Language.getById(lang);
                        if (l == null) return;

                        action.accept(l);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException("Unknown ID: " + nbt.getID());
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
