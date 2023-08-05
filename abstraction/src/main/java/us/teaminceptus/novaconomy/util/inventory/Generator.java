package us.teaminceptus.novaconomy.util.inventory;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.corporation.CorporationAchievement;
import us.teaminceptus.novaconomy.api.corporation.CorporationInvite;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.MarketCategory;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;
import us.teaminceptus.novaconomy.api.events.business.BusinessAdvertiseEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.of;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.*;
import static us.teaminceptus.novaconomy.api.corporation.Corporation.toExperience;
import static us.teaminceptus.novaconomy.util.NovaUtil.*;
import static us.teaminceptus.novaconomy.util.inventory.Items.*;

public final class Generator {

    private Generator() {
    }

    public static final Material[] RATING_MATS = new Material[]{
            Material.DIRT,
            Material.COAL,
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.DIAMOND
    };

    public static NovaInventory genGUI(int size, String name) {
        return genGUI("", size, name);
    }

    public static List<NovaInventory> generateBusinessData(Business b, Player viewer, boolean advertising, SortingType<BusinessProduct> sorter) {
        List<NovaInventory> invs = new ArrayList<>();
        int limit = ((b.getProducts().size() - 1) / 26) + 1;
        for (int i = 0; i < limit; i++) {
            final int fI = i;

            NovaInventory inv = genGUI(54, ChatColor.GOLD + b.getName());
            inv.setCancelled();

            inv.setAttribute("business", b.getUniqueId());
            inv.setAttribute("sorting_type", BusinessProduct.class);
            inv.setAttribute("sorting_function", (Function<SortingType<BusinessProduct>, NovaInventory>) s ->
                    generateBusinessData(b, viewer, advertising, s).get(fI));

            inv.setItem(18, sorter(sorter));
            inv.setItem(10, 16, 46, 52, GUI_BACKGROUND);

            if (!b.getRatings().isEmpty()) inv.setItem(44, Items.LOADING);
            for (int j = 46; j < 53; j++) inv.setItem(j, null);

            Corporation parent = b.getParentCorporation();
            if (parent != null) {
                ItemStack pIcon = builder(parent.getPublicIcon(),
                        meta -> meta.setLore(Collections.singletonList(
                                ChatColor.GOLD + get("constants.parent_corporation")
                        )), nbt -> {
                            nbt.setID("corporation:click");
                            nbt.set(CORPORATION_TAG, parent.getUniqueId());
                        }
                );
                inv.setItem(13, pIcon);
            }

            ItemStack icon = Items.builder(b.getIcon(),
                    meta -> {
                        meta.setDisplayName(ChatColor.GOLD + b.getName());
                        meta.setLore(Collections.singletonList(ChatColor.YELLOW + "ID: " + b.getUniqueId().toString().replace("-", "")));
                    }
            );
            inv.setItem(15, icon);

            boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(viewer);
            ItemStack owner = builder(w.createSkull(anonymous ? null : b.getOwner()),
                    meta -> {
                        meta.setDisplayName(anonymous ? ChatColor.AQUA + get("constants.business.anonymous") : format(get("constants.owner"), b.getOwner().getName()));
                        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                    }, nbt -> {
                        if (!anonymous) {
                            nbt.setID("player_stats");
                            nbt.set("player", b.getOwner().getUniqueId());
                        }
                    });
            inv.setItem(11, owner);

            boolean pHome = b.getSetting(Settings.Business.PUBLIC_HOME) || b.isOwner(viewer);
            ItemStack home = builder(pHome ? (w.isLegacy() ? Material.matchMaterial("WORKBENCH") : Material.matchMaterial("CRAFTING_TABLE")) : Material.BARRIER,
                    meta -> {
                        meta.setDisplayName(pHome ? ChatColor.AQUA + get("constants.business.home") : ChatColor.RED + get("constants.business.anonymous_home"));
                        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_HOME))
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                    }, nbt -> {
                        nbt.setID("business:home");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                        nbt.set("anonymous", !pHome);
                    }
            );
            ItemMeta hMeta = home.getItemMeta();
            home.setItemMeta(hMeta);
            inv.setItem(12, home);

            ItemStack invites = builder(Material.ENCHANTED_BOOK,
                    meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.business.invites")),
                    nbt -> {
                        nbt.setID("business:invites");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );

            ItemStack advInfo = builder(Material.BUCKET,
                    meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.business.advertising")),
                    nbt -> nbt.setID("business:advertising")
            );

            ItemStack supplyChests = builder(Material.CHEST,
                    meta -> meta.setDisplayName(ChatColor.GOLD + get("constants.business.supply_chests")),
                    nbt -> {
                        nbt.setID("business:supply_chests");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );

            ItemStack settings = builder(Material.NETHER_STAR,
                    meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.settings.business")),
                    nbt -> {
                        nbt.setID("business:settings");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );

            if (b.isOwner(viewer)) {
                inv.setItem(17, invites);
                inv.setItem(26, advInfo);
                inv.setItem(27, supplyChests);
                inv.setItem(53, settings);
            }

            AtomicInteger slot = new AtomicInteger(19);
            List<BusinessProduct> bProducts = b.getProducts()
                    .stream()
                    .sorted(sorter)
                    .collect(Collectors.toList())
                    .subList(i * 26, Math.min((i + 1) * 26, b.getProducts().size()));

            bProducts.forEach(p -> {
                if ((slot.get() + 1) % 9 == 0) slot.addAndGet(2);
                if (slot.get() == 46) slot.addAndGet(1);

                ItemStack item = p.getItem().clone();
                if (item.getType() == Material.AIR) return;

                AtomicBoolean stock = new AtomicBoolean(true);

                ItemStack product = builder(item.clone(),
                        meta -> {
                            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                            lore.add(" ");
                            lore.add(
                                    format(get("constants.price"), format("%,.2f", p.getAmount()).replace("D", ""), String.valueOf(p.getEconomy().getSymbol()))
                            );

                            lore.add(" ");
                            if (!b.isInStock(item)) {
                                lore.add(ChatColor.RED + get("constants.business.no_stock"));
                                stock.set(false);
                            } else {
                                AtomicInteger index = new AtomicInteger();
                                b.getResources().forEach(res -> {
                                    if (item.isSimilar(res)) index.addAndGet(res.getAmount());
                                });

                                lore.add(format(get("constants.business.stock_left"), format("%,d", index.get())));
                            }

                            meta.setLore(lore);
                        }, nbt -> nbt.setID("product:buy")
                );

                NBTWrapper nbt = of(product);

                nbt.set("product:in_stock", stock.get());
                nbt.set(PRODUCT_TAG, p);

                inv.setItem(slot.get(), nbt.getItem());

                slot.incrementAndGet();
            });

            if (limit > 1) {
                if (i > 0)
                    inv.setItem(46, NBTWrapper.builder(
                            Items.prev("business"),
                            nbt -> nbt.set("page", fI)
                    ));
                if (i < (limit - 1))
                    inv.setItem(52, NBTWrapper.builder(
                            Items.next("business"),
                            nbt -> nbt.set("page", fI)
                    ));
            }

            boolean pStats = b.getSetting(Settings.Business.PUBLIC_STATISTICS) || b.isOwner(viewer);
            ItemStack stats = builder(pStats ? Material.PAPER : Material.BARRIER,
                    meta -> {
                        meta.setDisplayName(pStats ? ChatColor.AQUA + get("constants.business.statistics") : ChatColor.RED + get("constants.business.anonymous_statistics"));
                        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_STATISTICS))
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                    }, nbt -> {
                        nbt.setID("business:statistics");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                        nbt.set("anonymous", !pStats);
                    }
            );
            inv.setItem(14, stats);

            Material kMaterial;
            try {
                kMaterial = Material.valueOf("SIGN");
            } catch (IllegalArgumentException e) {
                kMaterial = Material.valueOf("OAK_SIGN");
            }

            inv.setItem(35, Items.builder(kMaterial, meta -> {
                meta.setDisplayName(ChatColor.YELLOW + get("constants.business.keywords"));
                if (!b.getKeywords().isEmpty())
                    meta.setLore(Arrays.asList(ChatPaginator.wordWrap(ChatColor.AQUA + String.join(", ", b.getKeywords()), 30)));
            }));

            if (!b.getRatings().isEmpty()) {
                boolean pRating = b.getSetting(Settings.Business.PUBLIC_RATING) || b.isOwner(viewer);
                double avg = b.getAverageRating();
                int avgI = (int) Math.round(avg - 1);

                ItemStack rating = Items.builder(pRating ? RATING_MATS[avgI] : Material.BARRIER,
                        meta -> {
                            meta.setDisplayName(pRating ? ChatColor.YELLOW + format("%,.1f", avg) + "⭐" : ChatColor.RED + get("constants.business.anonymous_rating"));
                            if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_RATING))
                                meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                        });
                inv.setItem(44, rating);
            } else inv.setItem(44, null);

            if (Wrapper.r.nextBoolean() && advertising && b.getSetting(Settings.Business.EXTERNAL_ADVERTISEMENT) && NovaConfig.getConfiguration().isAdvertisingEnabled()) {
                Business rand = Business.randomAdvertisingBusiness();
                if (rand != null && !b.isBlacklisted(rand)) {
                    BusinessAdvertiseEvent event = new BusinessAdvertiseEvent(rand);
                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled())
                        inv.setItem(27, builder(rand.getPublicIcon(), nbt -> {
                            nbt.setID("business:click:advertising");
                            nbt.set(BUSINESS_TAG, rand.getUniqueId());
                            nbt.set("from_business", b.getUniqueId());
                        }));
                }
            }

            invs.add(inv);
        }

        return invs;
    }

    public static NovaInventory generateCorporationData(Corporation c, Player viewer, SortingType<Business> childrenSort) {
        NovaInventory inv = genGUI(54, ChatColor.DARK_BLUE + c.getName());
        inv.setCancelled();

        inv.setAttribute("sorting_type", Business.class);
        inv.setAttribute("sorting_function", (Function<SortingType<Business>, NovaInventory>) s ->
                generateCorporationData(c, viewer, s)
        );

        ItemStack icon = Items.builder(c.getPublicIcon(),
                meta -> {
                    String display = meta.getDisplayName();
                    meta.setDisplayName(display + " (" + c.getChildren().size() + "/" + c.getMaxChildren() + ")");

                    List<String> lore = new ArrayList<>();
                    lore.add(" ");

                    String desc = c.getDescription();

                    if (desc.isEmpty()) return;

                    lore.addAll(Arrays.stream(ChatPaginator.wordWrap(desc, 30))
                            .map(s -> ChatColor.GOLD + s)
                            .collect(Collectors.toList())
                    );

                    meta.setLore(lore);
                }
        );
        inv.setItem(4, icon);

        int level = c.getLevel();

        if (level >= 3) {
            ItemStack hq = builder(Material.GLASS,
                    meta -> meta.setDisplayName(ChatColor.YELLOW + get("constants.corporation.headquarters")),
                    nbt -> {
                        nbt.setID("corporation:hq");
                        nbt.set(CORPORATION_TAG, c.getUniqueId());
                    }
            );
            inv.setItem(11, hq);
        } else if (c.isOwner(viewer)) inv.setItem(11, Items.LOCKED);

        ItemStack achievements = builder(Material.BOOK,
                meta -> meta.setDisplayName(ChatColor.YELLOW + get("constants.corporation.achievements")),
                nbt -> {
                    nbt.setID("corporation:achievements");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(12, achievements);

        ItemStack owner = Items.builder(createPlayerHead(c.getOwner()),
                meta -> meta.setDisplayName(format(get("constants.owner"), c.getOwner().getName()))
        );
        inv.setItem(13, owner);

        ItemStack statistics = builder(Material.PAINTING,
                meta -> meta.setDisplayName(ChatColor.YELLOW + get("constants.corporation.statistics")),
                nbt -> {
                    nbt.setID("corporation:statistics");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(14, statistics);

        ItemStack leveling = builder(Material.GOLD_BLOCK,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + format(get("constants.level"), format("%,d", c.getLevel())));
                    meta.setLore(Collections.singletonList(
                            ChatColor.GOLD + format(get("constants.experience"), format("%,.2f", c.getExperience()))
                    ));
                }, nbt -> {
                    nbt.setID("corporation:leveling");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(15, leveling);

        // Children

        inv.setItem(27, sorter(childrenSort));

        List<Business> children = c.getChildren()
                .stream()
                .sorted(childrenSort)
                .limit(14)
                .collect(Collectors.toList());

        for (int i = 0; i < children.size(); i++) {
            Business b = children.get(i);
            int index = i < 7 ? 28 + i : 37 + i;

            ItemStack bIcon = builder(b.getIcon(),
                    nbt -> {
                        nbt.setID("business:click");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );
            inv.setItem(index, bIcon);
        }

        // Admin Settings

        if (c.isOwner(viewer)) {
            ItemStack editDesc = builder(Items.OAK_SIGN,
                    meta -> meta.setDisplayName(ChatColor.YELLOW + get("constants.corporation.edit_description")),
                    nbt -> {
                        nbt.setID("corporation:edit_desc");
                        nbt.set(CORPORATION_TAG, c.getUniqueId());
                    }
            );
            inv.setItem(26, editDesc);

            ItemStack settings = builder(Material.NETHER_STAR,
                    meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.settings.corporation")),
                    nbt -> {
                        nbt.setID("corporation:settings");
                        nbt.set(CORPORATION_TAG, c.getUniqueId());
                    }
            );
            inv.setItem(53, settings);
        }

        return inv;
    }

    public static NovaInventory generateCorporationLeveling(@NotNull Corporation c, int currentLevel) {
        NovaInventory inv = genGUI(36, ChatColor.DARK_BLUE + c.getName() + " | " + get("constants.leveling"));
        int level = c.getLevel();

        inv.setCancelled();
        inv.setAttribute(CORPORATION_TAG, c.getUniqueId());
        inv.setAttribute("current_level", currentLevel);

        ItemStack next = Items.next("corp_leveling");
        ItemStack prev = Items.prev("corp_leveling");

        if (currentLevel > 1) inv.setItem(19, prev);
        if (currentLevel < Corporation.MAX_LEVEL) inv.setItem(25, next);

        for (int i = 0; i < 7; i++) {
            int index = 10 + i;
            final int cLevel = (currentLevel - 3) + i;

            if (cLevel < 1) {
                inv.setItem(index, Items.GUI_BACKGROUND);
                continue;
            }

            boolean completed = cLevel < level;

            List<String> lore = new ArrayList<>();
            lore.add((completed ? ChatColor.GREEN : ChatColor.YELLOW) + suffix(c.getExperience()) + " / " + suffix(toExperience(cLevel)));
            lore.add(" ");

            // Completed Information

            lore.add((completed ? ChatColor.GREEN : ChatColor.RED) + format(get("constants.corporation.increase_children"), 5));

            if (cLevel == 3)
                lore.add(ChatColor.LIGHT_PURPLE + get("constants.corporation.headquarters"));

            if (cLevel >= 5 && cLevel <= 50 && cLevel % 5 == 0)
                lore.add(ChatColor.DARK_GREEN + format(get("constants.corporation.profit_modifier"), 10 + "%"));

            // Icon Setting

            ItemStack icon;

            if (cLevel < level) icon = Items.LIME_STAINED_GLASS_PANE;
            else if (cLevel == level) icon = c.getPublicIcon();
            else icon = Items.YELLOW_STAINED_GLASS_PANE;

            icon = Items.builder(icon,
                    meta -> {
                        meta.setDisplayName(format((completed ? ChatColor.GREEN : ChatColor.YELLOW) + get("constants.level"), format("%,d", cLevel)));
                        if (cLevel > 1) meta.setLore(lore);

                        if (completed) {
                            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        }
                    }
            );

            inv.setItem(index, icon);
        }

        inv.setItem(31, builder(BACK, nbt -> {
            nbt.setID("corporation:click");
            nbt.set(CORPORATION_TAG, c.getUniqueId());
        }));

        return inv;
    }

    public static NovaInventory generateCorporationAchievements(Corporation c) {
        NovaInventory inv = genGUI(54, ChatColor.DARK_BLUE + c.getName() + " | " + get("constants.achievements"));
        inv.setCancelled();

        inv.setItem(13, c.getPublicIcon());

        for (int i = 0; i < 14; i++) {
            if (CorporationAchievement.values().length <= i) break;

            CorporationAchievement a = CorporationAchievement.values()[i];
            int index = i < 7 ? 28 + i : 37 + i;

            ItemStack icon = Items.builder(a.getIcon(),
                    meta -> {
                        int currentLevel = c.getAchievementLevel(a);
                        meta.setDisplayName(ChatColor.GOLD + a.getDisplayName() + (currentLevel > 0 ? " " + toRoman(currentLevel) : ""));

                        List<String> lore = new ArrayList<>();
                        lore.add(" ");
                        lore.addAll(Arrays.stream(ChatPaginator.wordWrap(a.getDescription(), 30))
                                .map(s -> ChatColor.GRAY + s)
                                .collect(Collectors.toList()));
                        lore.add(" ");

                        if (currentLevel < a.getMaxLevel()) {
                            int next = currentLevel + 1;

                            lore.add(ChatColor.YELLOW + a.getDisplayName() + " " + toRoman(next) + ":");
                            lore.add(ChatColor.GOLD + "+" + format(get("constants.experience"), format("%,.0f", a.getExperienceReward() * next)));
                            lore.add(" ");
                            lore.add(ChatColor.DARK_GREEN + format(get("constants.completed"), format("%,.1f", a.getProgress(c)) + "%"));
                        } else
                            lore.add(ChatColor.DARK_GREEN + format(get("constants.completed"), "100.0%"));

                        meta.setLore(lore);
                    }
            );

            inv.setItem(index, icon);
        }

        inv.setItem(49, builder(BACK, nbt -> {
            nbt.setID("corporation:click");
            nbt.set(CORPORATION_TAG, c.getUniqueId());
        }));

        return inv;
    }

    public static NovaInventory generateCorporationStatistics(@NotNull Corporation c) {
        NovaInventory inv = genGUI(45, ChatColor.DARK_BLUE + c.getName() + " | " + get("constants.statistics"));
        inv.setCancelled();

        inv.setItem(13, c.getPublicIcon());

        ItemStack views = Items.builder(Material.matchMaterial("SPYGLASS") == null ? Material.COMPASS : Material.matchMaterial("SPYGLASS"),
                meta -> meta.setDisplayName(format(get("constants.views"), format("%,d", c.getViews())))
        );
        inv.setItem(21, views);

        ItemStack moneyMade = Items.builder(Material.DIAMOND,
                meta -> {
                    meta.setDisplayName(ChatColor.AQUA + get("constants.stats.global.total_made"));
                    meta.setLore(Collections.singletonList(
                            ChatColor.GOLD + format("%,.2f", c.getStatistics().getTotalProfit())
                    ));
                }
        );
        inv.setItem(22, moneyMade);

        ItemStack productsSold = Items.builder(Material.matchMaterial("BUNDLE") == null ? Material.CHEST : Material.matchMaterial("BUNDLE"),
                meta -> meta.setDisplayName(format(get("constants.stats.global.sold"), format("%,d", c.getStatistics().getTotalProductsSold())))
        );
        inv.setItem(23, productsSold);

        inv.setItem(40, builder(BACK, nbt -> {
            nbt.setID("corporation:click");
            nbt.set(CORPORATION_TAG, c.getUniqueId());
        }));

        return inv;
    }

    public static NovaInventory genGUI(String id, int size, String name) {
        if (size < 9 || size > 54) return null;
        if (size % 9 > 0) return null;

        NovaInventory inv = w.createInventory(id, name, size);
        ItemStack bg = Items.GUI_BACKGROUND;

        if (size < 27) return inv;

        for (int i = 0; i < 9; i++) inv.setItem(i, bg);
        for (int i = size - 9; i < size; i++) inv.setItem(i, bg);
        for (int i = 1; i < Math.floor((double) size / 9D) - 1; i++) {
            inv.setItem(i * 9, bg);
            inv.setItem(((i + 1) * 9) - 1, bg);
        }

        return inv;
    }

    public static ItemStack createCheck(Economy econ, double amount) throws IllegalArgumentException {
        if (econ == null) throw new IllegalArgumentException("Economy cannot be null");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");

        return builder(Material.PAPER,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + format("%,.2f", amount) + econ.getSymbol());
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + format("%,.2f", amount) + econ.getName() + "(s)"));
                }, nbt -> {
                    nbt.setID("economy:check");
                    nbt.set("economy", econ.getUniqueId());
                    nbt.set("amount", amount);
                });
    }

    public static List<NovaInventory> getRatingsGUI(OfflinePlayer p, @NotNull Business b) {
        List<NovaInventory> invs = new ArrayList<>();

        if (b.getRatings().isEmpty()) return invs;

        ItemStack nextA = builder(NEXT,
                nbt -> {
                    nbt.setID("next:ratings");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                });

        ItemStack prevA = builder(PREVIOUS,
                nbt -> {
                    nbt.setID("prev:ratings");
                    nbt.set(BUSINESS_TAG, b.getUniqueId());
                });

        List<Rating> ratings = b.getRatings();

        int pageCount = (int) Math.floor((ratings.size() - 1) / 28D) + 1;

        for (int i = 0; i < pageCount; i++) {
            NovaInventory inv = genGUI(54, ChatColor.DARK_AQUA + get("constants.business.ratings") + " - " + format(get("constants.page"), i + 1));
            inv.setCancelled();

            inv.setItem(4, b.getPublicIcon());

            if (pageCount > 1 && i < pageCount - 1) {
                NBTWrapper nbtN = of(nextA.clone());
                nbtN.set("page", i);
                inv.setItem(48, nbtN.getItem());
            }

            if (i > 0) {
                NBTWrapper nbtP = of(prevA.clone());
                nbtP.set("page", i);
                inv.setItem(50, nbtP.getItem());
            }

            List<Rating> rlist = new ArrayList<>(ratings.subList(i * 28, Math.min((i + 1) * 28, ratings.size())));
            rlist.forEach(r -> {
                OfflinePlayer owner = r.getOwner();
                NovaPlayer np = new NovaPlayer(owner);
                boolean anon = np.getSetting(Settings.Personal.ANONYMOUS_RATING);

                inv.addItem(builder(createPlayerHead(anon ? null : owner),
                        meta -> {
                            meta.setDisplayName(ChatColor.AQUA + (anon ? get("constants.business.anonymous") : owner.getName()));

                            StringBuilder sBuilder = new StringBuilder();
                            for (int j = 0; j < r.getRatingLevel(); j++) sBuilder.append("⭐");

                            meta.setLore(
                                    Arrays.asList(
                                            " ",
                                            ChatColor.GOLD + sBuilder.toString(),
                                            ChatColor.YELLOW + "\"" + r.getComment() + "\""
                                    )
                            );
                        }, nbt -> {
                            nbt.setID("business:pick_rating");
                            nbt.set("owner", owner.getUniqueId());
                            nbt.set("anonymous", anon);
                        }
                ));
            });

            invs.add(inv);
        }

        return invs;
    }

    public static List<NovaInventory> getBalancesGUI(OfflinePlayer p, SortingType<Economy> sorter) {
        List<NovaInventory> invs = new ArrayList<>();
        NovaPlayer np = new NovaPlayer(p);

        ItemStack nextA = next("balance");
        ItemStack prevA = prev("balance");

        List<Economy> econs = new ArrayList<>(Economy.getEconomies())
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());

        int pageCount = (int) Math.floor((econs.size() - 1D) / 28D) + 1;

        boolean ad = r.nextBoolean() && NovaConfig.getConfiguration().isAdvertisingEnabled();
        Business randB = Business.randomAdvertisingBusiness();

        for (int i = 0; i < pageCount; i++) {
            NovaInventory inv = genGUI(54, get("constants.balances") + " - " + format(get("constants.page"), i + 1));
            inv.setCancelled();

            final int fI = i;
            inv.setAttribute("sorting_type", Economy.class);
            inv.setAttribute("sorting_function", (Function<SortingType<Economy>, NovaInventory>) s ->
                    getBalancesGUI(p, s).get(fI));

            inv.setItem(4, createPlayerHead(p));
            inv.setItem(18, sorter(sorter));

            if (pageCount > 1 && i < pageCount - 1) {
                NBTWrapper nbtN = of(nextA.clone());
                nbtN.set("page", i);
                inv.setItem(48, nbtN.getItem());
            }

            if (i > 0) {
                NBTWrapper nbtP = of(prevA.clone());
                nbtP.set("page", i);
                inv.setItem(50, nbtP.getItem());
            }

            List<Economy> elist = new ArrayList<>(econs.subList(i * 28, Math.min((i + 1) * 28, econs.size())));
            elist.forEach(econ -> {
                ItemStack item = econ.getIcon().clone();
                ItemMeta eMeta = item.getItemMeta();
                eMeta.setLore(Collections.singletonList(
                        ChatColor.GOLD + format("%,.2f", np.getBalance(econ)) + econ.getSymbol()
                ));
                item.setItemMeta(eMeta);
                inv.addItem(item);
            });

            if (ad && randB != null) {
                BusinessAdvertiseEvent event = new BusinessAdvertiseEvent(randB);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) inv.setItem(27, builder(randB.getPublicIcon(),
                        nbt -> {
                            nbt.setID("business:click:advertising_external");
                            nbt.set(BUSINESS_TAG, randB.getUniqueId());
                        }
                ));
            }

            invs.add(inv);
        }

        return invs;
    }

    public static List<NovaInventory> getBankBalanceGUI(SortingType<Economy> sorter) {
        List<NovaInventory> invs = new ArrayList<>();

        ItemStack nextA = next("bank_balance");
        ItemStack prevA = prev("bank_balance");

        List<Economy> econs = new ArrayList<>(Economy.getEconomies())
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());
        int pageCount = (int) Math.floor((econs.size() - 1D) / 28D) + 1;

        boolean ad = r.nextBoolean() && NovaConfig.getConfiguration().isAdvertisingEnabled();
        Business randB = Business.randomAdvertisingBusiness();

        for (int i = 0; i < pageCount; i++) {
            NovaInventory inv = genGUI(54, get("constants.bank.balance") + " - " + format(get("constants.page"), i + 1));
            inv.setCancelled();

            final int fI = i;
            inv.setAttribute("sorting_type", Economy.class);
            inv.setAttribute("sorting_function", (Function<SortingType<Economy>, NovaInventory>) s ->
                    getBankBalanceGUI(s).get(fI));

            inv.setItem(18, sorter(sorter));

            if (pageCount > 1 && i < pageCount - 1) {
                NBTWrapper nbtN = of(nextA.clone());
                nbtN.set("page", i);
                inv.setItem(48, nbtN.getItem());
            }

            if (i > 0) {
                NBTWrapper nbtP = of(prevA.clone());
                nbtP.set("page", i);
                inv.setItem(50, nbtP.getItem());
            }

            List<Economy> elist = new ArrayList<>(econs.subList(i * 28, Math.min((i + 1) * 28, econs.size())));
            elist.forEach(econ -> {
                ItemStack item = new ItemStack(econ.getIconType());
                modelData(item, econ.getCustomModelData());

                ItemMeta iMeta = item.getItemMeta();
                iMeta.setDisplayName(ChatColor.AQUA + format("%,.2f", Bank.getBalance(econ)) + econ.getSymbol() + " (" + econ.getName() + ")");
                List<String> topDonors = new ArrayList<>();
                topDonors.add(ChatColor.YELLOW + get("constants.bank.top_donors"));
                topDonors.add(" ");
                List<String> topDonorsNames = NovaPlayer.getTopDonators(econ, 10).stream().map(NovaPlayer::getPlayerName).collect(Collectors.toList());
                List<Double> topDonorsAmounts = NovaPlayer.getTopDonators(econ, 10).stream().map(n -> n.getDonatedAmount(econ)).collect(Collectors.toList());
                for (int j = 0; j < topDonorsNames.size(); j++)
                    if (j < 2)
                        topDonors.add(new ChatColor[]{ChatColor.GOLD, ChatColor.GRAY}[j] + "#" + (j + 1) + " - " + topDonorsNames.get(j) + " | " + format("%,.2f", topDonorsAmounts.get(j)) + econ.getSymbol());
                    else if (j == 2)
                        topDonors.add(ChatColor.translateAlternateColorCodes('&', "&x&c&c&6&6&3&3#3 - " + topDonorsNames.get(j) + " | " + format("%,.2f", topDonorsAmounts.get(j)) + econ.getSymbol()));
                    else
                        topDonors.add(ChatColor.BLUE + "#" + (j + 1) + " - " + topDonorsNames.get(j) + " | " + format("%,.2f", topDonorsAmounts.get(j)) + econ.getSymbol());
                iMeta.setLore(topDonors);
                item.setItemMeta(iMeta);
                inv.addItem(item);
            });

            if (ad && randB != null) {
                BusinessAdvertiseEvent event = new BusinessAdvertiseEvent(randB);
                Bukkit.getPluginManager().callEvent(event);

                if (!event.isCancelled()) inv.setItem(27, builder(randB.getPublicIcon(),
                        nbt -> {
                            nbt.setID("business:click:advertising_external");
                            nbt.set(BUSINESS_TAG, randB.getUniqueId());
                        }
                ));
            }

            invs.add(inv);
        }

        return invs;
    }

    public static NovaInventory generateBusinessDiscovery(SortingType<Business> sorter, String... keywords) {
        NovaInventory discover = genGUI(54, get("constants.business.discover"));
        discover.setCancelled();

        List<Business> businesses = Business.getBusinesses()
                .stream()
                .filter(b -> {
                    if (keywords.length == 0) return true;

                    AtomicBoolean contains = new AtomicBoolean(false);

                    for (String s : keywords)
                        if (b.getName().toLowerCase().contains(s.toLowerCase())) contains.compareAndSet(false, true);

                    return contains.get();
                })
                .sorted(sorter)
                .collect(Collectors.toList());

        if (businesses.isEmpty()) return null;

        Collections.shuffle(businesses);
        for (int i = 0; i < 28; i++) {
            int index = 10 + i;
            if (index > 16) index += 2;
            if (index > 25) index += 2;
            if (index > 34) index += 2;
            if (index > 43) break;

            if (businesses.size() > i) {
                Business b = businesses.get(i);

                discover.setItem(index, builder(b.getPublicIcon(),
                        nbt -> {
                            nbt.setID("business:click");
                            nbt.set(BUSINESS_TAG, b.getUniqueId());
                        }
                ));
            } else discover.setItem(index, GUI_BACKGROUND);
        }

        return discover;
    }

    @NotNull
    public static NovaInventory generateBusinessInvites(@NotNull Business b, @NotNull SortingType<CorporationInvite> sorter) {
        NovaInventory inv = genGUI(36, get("constants.business.invites"));
        inv.setCancelled();

        inv.setItem(19, 20, 23, 24, GUI_BACKGROUND);

        inv.setItem(31, builder(BACK, nbt -> {
            nbt.setID("business:click");
            nbt.set(BUSINESS_TAG, b.getUniqueId());
        }));

        List<CorporationInvite> invites = b.getInvites()
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());

        if (invites.isEmpty()) {
            inv.setItem(13, Items.builder(Material.PAPER,
                    meta -> meta.setDisplayName(ChatColor.WHITE + get("constants.business.invites.none_yet"))
            ));

            inv.addItem(9, GUI_BACKGROUND);
            return inv;
        } else {
            inv.setAttribute("sorting_type", CorporationInvite.class);
            inv.setAttribute("sorting_function", (Function<SortingType<CorporationInvite>, NovaInventory>) s -> generateBusinessInvites(b, s));

            inv.setItem(18, sorter(sorter));

            invites.forEach(i -> {
                Corporation from = i.getFrom();
                ItemStack item = builder(from.getIcon(),
                        meta -> {
                            meta.setDisplayName(ChatColor.GOLD + from.getName());
                            meta.setLore(Arrays.asList(
                                    ChatColor.AQUA + formatTimeAgo(i.getInvitedTimestamp().getTime()),
                                    " ",
                                    ChatColor.YELLOW + get("constants.business.invites.right_click_accept"),
                                    ChatColor.YELLOW + get("constants.business.invites.left_click_decline")
                            ));
                        }, nbt -> {
                            nbt.setID("business:invite");
                            nbt.set(BUSINESS_TAG, b.getUniqueId());
                            nbt.set("from", from.getUniqueId());
                        }
                );
                inv.addItem(item);
            });
        }

        return inv;
    }

    public static NovaInventory generateMarket(@NotNull Player p, @NotNull MarketCategory category, @NotNull SortingType<Material> sorter, @NotNull Economy econ, int page) {
        NovaInventory inv = genGUI(54, get("constants.market"));
        inv.setCancelled();

        inv.setAttribute("category", category);

        NovaPlayer np = new NovaPlayer(p);

        long max = NovaConfig.getMarket().getMaxPurchases();
        long purchasesLeft = max - np.getPurchases().stream().filter(Receipt::isRecent).count();
        inv.setItem(3, Items.builder(Items.createPlayerHead(p),
                meta -> {
                    meta.setDisplayName(ChatColor.DARK_PURPLE + p.getName());
                    if (max > 0 && !p.hasPermission("novaconomy.admin.market.bypass_limit")) {
                        meta.setLore(Arrays.asList(
                                ChatColor.GREEN + format(get("constants.market.purchases_left"), ChatColor.GOLD + format("%,d", purchasesLeft))
                        ));
                    }
                }
        ));

        inv.setItem(5, Items.sorter(sorter));
        inv.setAttribute("sorter", sorter);
        inv.setAttribute("sorting_type", Material.class);
        inv.setAttribute("sorting_function", (Function<SortingType<Material>, NovaInventory>) s -> generateMarket(p, category, s, econ, page));

        if (NovaConfig.getMarket().getLastRestockTimestamp() != null) {
            inv.setItem(8, Items.builder(Items.CLOCK,
                    meta -> {
                        meta.setDisplayName(ChatColor.DARK_GREEN + get("constants.market.last_restock"));
                        meta.setLore(Collections.singletonList(ChatColor.AQUA + formatTimeAgo(NovaConfig.getMarket().getLastRestockTimestamp().getTime())));
                    }
            ));
        }

        MarketCategory[] categories = Arrays.stream(MarketCategory.values())
                .filter(m -> category.ordinal() - 1 <= m.ordinal() && m.ordinal() <= category.ordinal() + 3)
                .sorted(Comparator.comparingInt(MarketCategory::ordinal))
                .toArray(MarketCategory[]::new);

        for (int i = 0; i < 4; i++) {
            int index = (i * 9) + 9;
            if (categories.length <= i) {
                inv.setItem(index, null);
                continue;
            }

            MarketCategory c = categories[i];

            Material icon = c.getItems().stream().findFirst().orElse(Material.DIRT);
            inv.setItem(index, builder(icon,
                    meta -> meta.setDisplayName(ChatColor.DARK_AQUA + c.getLocalizedName()),
                    nbt -> {
                        nbt.setID("market:category");
                        nbt.set("category", c.name());
                    }
            ));
        }

        inv.setItem(10, 19, 28, 37, GUI_BACKGROUND);

        List<Material> products = category.getItems()
                .stream()
                .filter(m -> !NovaConfig.getMarket().getBlacklistedMaterials().contains(m))
                .collect(Collectors.toList())
                .subList(page * 28, Math.min(category.getItems().size(), (page + 1) * 28))
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());

        inv.setAttribute("page", page);
        int pages = (category.getItems().size() / 28) + 1;

        if (pages > 1 && page > 0)
            inv.setItem(47, builder(Items.head("arrow_left_gray"),
                    meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.prev")),
                    nbt -> {
                        nbt.setID("market:page");
                        nbt.set("page", page - 1);
                        nbt.set("operation", false);
                    }
            ));

        if (pages > 1 && page < (pages - 1))
            inv.setItem(53, builder(Items.head("arrow_right_gray"),
                    meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.next")),
                    nbt -> {
                        nbt.setID("market:page");
                        nbt.set("page", page + 1);
                        nbt.set("operation", true);
                    }
            ));

        inv.setItem(17, 26, 35, 44, null);
        inv.setItem(45, Items.economyWheel("market", econ));

        for (int i = 0; i < Math.min(products.size(), 28); i++) {
            int index = 11 + i + ((i / 7) * 2);
            Material m = products.get(i);

            double price = NovaConfig.getMarket().getPrice(m, econ);

            inv.setItem(index, builder(m,
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + WordUtils.capitalizeFully(m.name().replace('_', ' ')));

                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.GOLD + format("%,.2f", price) + econ.getSymbol());

                        if (np.getPurchases(m).stream().anyMatch(Receipt::isRecent))
                            lore.add(ChatColor.GREEN + get("constants.market.purchased_recently"));

                        lore.add(" ");
                        if (NovaConfig.getMarket().getStock(m) <= 0)
                            lore.add(ChatColor.RED + get("constants.business.no_stock"));
                        else
                            lore.add(ChatColor.LIGHT_PURPLE + format(get("constants.business.stock_left"), ChatColor.BLUE + format("%,d", NovaConfig.getMarket().getStock(m))));

                        meta.setLore(lore);
                    },
                    nbt -> {
                        nbt.setID("market:buy_product");
                        nbt.set(PRODUCT_TAG, m.name());
                        nbt.set(ECON_TAG, econ.getUniqueId());
                    }
            ));
        }

        return inv;
    }

    public static List<NovaInventory> generateBusinessSupplyChests(@NotNull Business business, @NotNull SortingType<Block> sorter) {
        List<NovaInventory> invs = new ArrayList<>();

        List<Block> blocks = business.getSupplyChests()
                .stream()
                .map(Chest::getBlock)
                .sorted(sorter)
                .collect(Collectors.toList());

        int limit = (business.getSupplyChests().size() / 28) + 1;

        for (int i = 0; i < limit; i++) {
            NovaInventory inv = genGUI(54, get("constants.business.supply_chests"));

            final int fI = i;
            inv.setItem(18, Items.sorter(sorter));

            inv.setAttribute("business", business.getUniqueId());
            inv.setAttribute("sorter", sorter);
            inv.setAttribute("sorting_type", Block.class);
            inv.setAttribute("sorting_function", (Function<SortingType<Block>, NovaInventory>) s -> generateBusinessSupplyChests(business, s).get(fI));

            List<Block> chests = blocks.subList(i * 28, Math.min(blocks.size(), (i + 1) * 28));
            chests.forEach(b -> inv.addItem(NBTWrapper.builder(Material.CHEST,
                    meta -> {
                        meta.setDisplayName(ChatColor.BLUE + b.getWorld().getName() + ChatColor.GOLD + " | " + ChatColor.YELLOW + b.getX() + ", " + b.getY() + ", " + b.getZ());
                        meta.setLore(Arrays.asList(
                                " ",
                                ChatColor.YELLOW + get("constants.click_remove")
                        ));
                    }, nbt -> {
                        nbt.setID("business:remove_supply_chest");
                        nbt.set("world", b.getWorld().getUID());
                        nbt.set("x", b.getX());
                        nbt.set("y", b.getY());
                        nbt.set("z", b.getZ());
                    })
                )
            );

            if (limit > 1) {
                if (i > 0)
                    inv.setItem(47, Items.prev("stored"));

                if (i < (limit - 1))
                    inv.setItem(53, Items.next("stored"));
            }

            invs.add(inv);
        }

        if (limit > 1)
            for (NovaInventory inv : invs) inv.setAttribute("invs", invs);

        return invs;
    }

    public static void modelData(@NotNull ItemStack item, int data) {
        ItemMeta meta = item.getItemMeta();
        try {
            Method m = meta.getClass().getDeclaredMethod("setCustomModelData", Integer.class);
            m.setAccessible(true);
            m.invoke(meta, data);
        } catch (NoSuchMethodException ignored) {
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }
        item.setItemMeta(meta);
    }
}
