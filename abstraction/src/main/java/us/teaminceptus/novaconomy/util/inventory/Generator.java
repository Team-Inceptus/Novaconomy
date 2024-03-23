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
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.auction.AuctionHouse;
import us.teaminceptus.novaconomy.api.auction.AuctionProduct;
import us.teaminceptus.novaconomy.api.auction.Bid;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.business.BusinessProduct;
import us.teaminceptus.novaconomy.api.business.Rating;
import us.teaminceptus.novaconomy.api.corporation.*;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.economy.market.MarketCategory;
import us.teaminceptus.novaconomy.api.economy.market.Receipt;
import us.teaminceptus.novaconomy.api.events.business.BusinessAdvertiseEvent;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.Price;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.of;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.r;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.w;
import static us.teaminceptus.novaconomy.api.corporation.Corporation.toExperience;
import static us.teaminceptus.novaconomy.messages.MessageHandler.format;
import static us.teaminceptus.novaconomy.messages.MessageHandler.get;
import static us.teaminceptus.novaconomy.util.NovaUtil.*;
import static us.teaminceptus.novaconomy.util.inventory.Items.*;

public final class Generator {

    public static final int GUI_SPACE = 28;
    public static final String STORED = "stored";

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

    public static List<NovaInventory> generateBusinessData(Business b, Player viewer, boolean advertising, SortingType<? super BusinessProduct> sorter) {
        List<NovaInventory> invs = new ArrayList<>();
        int limit = ((b.getProducts().size() - 1) / 26) + 1;
        for (int i = 0; i < limit; i++) {
            final int fI = i;

            NovaInventory inv = genGUI(54, ChatColor.GOLD + b.getName());
            inv.setCancelled();

            inv.setAttribute("business", b.getUniqueId());
            inv.setAttribute("sorting_type", BusinessProduct.class);
            inv.setAttribute("sorting_function", (Function<SortingType<? super BusinessProduct>, NovaInventory>) s ->
                    generateBusinessData(b, viewer, advertising, s).get(fI));

            inv.setItem(18, sorter(sorter));
            inv.setItem(10, 16, 46, 52, GUI_BACKGROUND);

            if (!b.getRatings().isEmpty()) inv.setItem(44, Items.LOADING);
            for (int j = 46; j < 53; j++) inv.setItem(j, null);

            Corporation parent = b.getParentCorporation();
            if (parent != null) {
                CorporationRank rank = parent.getRank(b);
                ItemStack pIcon = builder(parent.getPublicIcon(),
                        meta -> meta.setLore(Arrays.asList(
                                ChatColor.GOLD + get(viewer, "constants.parent_corporation"),
                                ChatColor.GOLD + rank.getName()
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
            ItemStack owner = builder(createPlayerHead(anonymous ? null : b.getOwner()),
                    meta -> {
                        meta.setDisplayName(anonymous ? ChatColor.AQUA + get(viewer, "constants.business.anonymous") : format(viewer, get(viewer, "constants.owner"), b.getOwner().getName()));
                        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_OWNER))
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + get(viewer, "constants.business.hidden")));
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
                        meta.setDisplayName(pHome ? ChatColor.AQUA + get(viewer, "constants.business.home") : ChatColor.RED + get(viewer, "constants.business.anonymous_home"));
                        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_HOME))
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + get(viewer, "constants.business.hidden")));
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
                    meta -> meta.setDisplayName(ChatColor.AQUA + get(viewer, "constants.business.invites")),
                    nbt -> {
                        nbt.setID("business:invites");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );

            ItemStack advInfo = builder(Material.BUCKET,
                    meta -> meta.setDisplayName(ChatColor.AQUA + get(viewer, "constants.business.advertising")),
                    nbt -> nbt.setID("business:advertising")
            );

            ItemStack supplyChests = builder(Material.CHEST,
                    meta -> meta.setDisplayName(ChatColor.GOLD + get(viewer, "constants.business.supply_chests")),
                    nbt -> {
                        nbt.setID("business:supply_chests");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );

            ItemStack settings = builder(Material.NETHER_STAR,
                    meta -> meta.setDisplayName(ChatColor.GREEN + get(viewer, "constants.settings.business")),
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
                            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                            lore.add(" ");
                            lore.add(
                                    format(viewer, get(viewer, "constants.price"), format("%,.2f", p.getAmount()), String.valueOf(p.getEconomy().getSymbol()))
                            );
                            if (!b.isInStock(item)) {
                                lore.add(ChatColor.RED + get(viewer, "constants.business.no_stock"));
                                stock.set(false);
                            } else {
                                AtomicInteger index = new AtomicInteger();
                                b.getResources().forEach(res -> {
                                    if (item.isSimilar(res)) index.addAndGet(res.getAmount());
                                });

                                lore.add(format(viewer, get(viewer, "constants.business.stock_left"), format("%,d", index.get())));
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
                        meta.setDisplayName(pStats ? ChatColor.AQUA + get(viewer, "constants.business.statistics") : ChatColor.RED + get(viewer, "constants.business.anonymous_statistics"));
                        if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_STATISTICS))
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + get(viewer, "constants.business.hidden")));
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
                meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.business.keywords"));
                if (!b.getKeywords().isEmpty())
                    meta.setLore(Arrays.asList(ChatPaginator.wordWrap(ChatColor.AQUA + String.join(", ", b.getKeywords()), 30)));
            }));

            if (!b.getRatings().isEmpty()) {
                boolean pRating = b.getSetting(Settings.Business.PUBLIC_RATING) || b.isOwner(viewer);
                double avg = b.getAverageRating();
                int avgI = (int) Math.round(avg - 1);

                ItemStack rating = Items.builder(pRating ? RATING_MATS[avgI] : Material.BARRIER,
                        meta -> {
                            meta.setDisplayName(pRating ? ChatColor.YELLOW + format("%,.1f", avg) + "⭐" : ChatColor.RED + get(viewer, "constants.business.anonymous_rating"));
                            if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_RATING))
                                meta.setLore(Collections.singletonList(ChatColor.YELLOW + get(viewer, "constants.business.hidden")));
                        });
                inv.setItem(44, rating);
            } else inv.setItem(44, null);

            if (r.nextBoolean() && advertising && b.getSetting(Settings.Business.EXTERNAL_ADVERTISEMENT) && NovaConfig.getConfiguration().isAdvertisingEnabled()) {
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
            boolean viewerTp = c.getSetting(Settings.Corporation.PUBLIC_HEADQUARTERS) || (c.getMembers().contains(viewer) && c.getRank(viewer).hasPermission(CorporationPermission.TELEPORT_TO_HEADQUARTERS));
            if (c.getSetting(Settings.Corporation.PUBLIC_HEADQUARTERS) || viewerTp || c.isOwner(viewer)) {
                ItemStack hq = builder(Material.GLASS,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.corporation.headquarters")),
                        nbt -> {
                            nbt.setID("corporation:hq");
                            nbt.set(CORPORATION_TAG, c.getUniqueId());
                        }
                );
                inv.setItem(11, hq);
            }
        } else
            if (c.isOwner(viewer)) inv.setItem(11, Items.LOCKED);

        ItemStack achievements = builder(Material.BOOK,
                meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.corporation.achievements")),
                nbt -> {
                    nbt.setID("corporation:achievements");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(12, achievements);

        ItemStack owner = Items.builder(createPlayerHead(c.getOwner()),
                meta -> meta.setDisplayName(format(viewer, get(viewer, "constants.owner"), c.getOwner().getName()))
        );
        inv.setItem(13, owner);

        ItemStack statistics = builder(Material.PAINTING,
                meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.corporation.statistics")),
                nbt -> {
                    nbt.setID("corporation:statistics");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(14, statistics);

        ItemStack leveling = builder(Material.GOLD_BLOCK,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + format(viewer, get(viewer, "constants.level"), format("%,d", c.getLevel())));
                    meta.setLore(Collections.singletonList(
                            ChatColor.GOLD + format(viewer, get(viewer, "constants.experience"), format("%,.2f", c.getExperience()))
                    ));
                }, nbt -> {
                    nbt.setID("corporation:leveling");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(15, leveling);

        ItemStack ranks = builder(Material.IRON_INGOT,
                meta -> meta.setDisplayName(ChatColor.LIGHT_PURPLE + get(viewer, "constants.ranks")),
                nbt -> {
                    nbt.setID("corporation:ranks");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                });
        inv.setItem(16, ranks);

        if (c.getSetting(Settings.Corporation.FEATURE_PRODUCTS) && !c.getChildren().isEmpty() && r.nextDouble() < 0.4) {
            List<Map.Entry<Business, BusinessProduct>> products = c.getChildren()
                    .stream()
                    .map(b -> {
                        List<BusinessProduct> bps = b.getProducts()
                                .stream()
                                .filter(p -> p.getBusiness().isInStock(p.getItem()))
                                .collect(Collectors.toList());

                        return new AbstractMap.SimpleEntry<>(b, bps.isEmpty() ? null : bps.get(r.nextInt(bps.size())));
                    })
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toList());

            if (!products.isEmpty()) {
                Map.Entry<Business, BusinessProduct> random = products.get(r.nextInt(products.size()));
                Business b = random.getKey();
                BusinessProduct p = random.getValue();
                ItemStack item = p.getItem();

                ItemStack product = builder(item.clone(),
                        meta -> {
                            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                            lore.addAll(Arrays.asList(
                                    " ",
                                    ChatColor.AQUA + get(viewer, "constants.featured_product"),
                                    ChatColor.DARK_AQUA + b.getName(),
                                    ChatColor.GOLD + "----------",
                                    " ",
                                    format(viewer, get(viewer, "constants.price"), format("%,.2f", p.getAmount()), String.valueOf(p.getEconomy().getSymbol()))
                            ));
                            AtomicInteger index = new AtomicInteger();
                            b.getResources().forEach(res -> {
                                if (item.isSimilar(res)) index.addAndGet(res.getAmount());
                            });

                            lore.add(format(viewer, get(viewer, "constants.business.stock_left"), format("%,d", index.get())));

                            meta.setLore(lore);
                        }, nbt -> {
                            nbt.setID("product:buy");
                            nbt.set("product:in_stock", true);
                            nbt.set(PRODUCT_TAG, p);
                        }
                );
                inv.setItem(36, product);
            }
        }

        // Children

        inv.setItem(27, sorter(childrenSort));

        List<Business> children = c.getChildren()
                .stream()
                .sorted(childrenSort)
                .limit(14)
                .collect(Collectors.toList());

        for (int i = 0; i < children.size(); i++) {
            Business b = children.get(i);
            CorporationRank rank = c.getRank(b);
            int index = i < 7 ? GUI_SPACE + i : 37 + i;

            ItemStack bIcon = builder(b.getIcon(),
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + "[" + rank.getPrefix() + "] " + meta.getDisplayName());
                        meta.setLore(Collections.singletonList(ChatColor.GOLD + rank.getName()));
                    },
                    nbt -> {
                        nbt.setID("business:click");
                        nbt.set(BUSINESS_TAG, b.getUniqueId());
                    }
            );
            inv.setItem(index, bIcon);
        }

        // Admin Settings

        if (c.getMembers().contains(viewer)) {
            CorporationRank pRank = c.getRank(viewer);

            if (pRank.hasPermission(CorporationPermission.EDIT_DETAILS)) {
                ItemStack editDesc = builder(Items.OAK_SIGN,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.corporation.edit_description")),
                        nbt -> {
                            nbt.setID("corporation:edit_desc");
                            nbt.set(CORPORATION_TAG, c.getUniqueId());
                        }
                );
                inv.setItem(26, editDesc);
            }

            if (pRank.hasPermission(CorporationPermission.EDIT_SETTINGS)) {
                ItemStack settings = builder(Material.NETHER_STAR,
                        meta -> meta.setDisplayName(ChatColor.GREEN + get(viewer, "constants.settings.corporation")),
                        nbt -> {
                            nbt.setID("corporation:settings");
                            nbt.set(CORPORATION_TAG, c.getUniqueId());
                        }
                );
                inv.setItem(53, settings);
            }
        }

        return inv;
    }

    public static NovaInventory generateCorporationLeveling(@NotNull Corporation c, int currentLevel, Player viewer) {
        NovaInventory inv = genGUI(36, ChatColor.DARK_BLUE + c.getName() + " | " + get(viewer, "constants.leveling"));
        int level = c.getLevel();

        inv.setCancelled();
        inv.setAttribute(CORPORATION_TAG, c.getUniqueId());
        inv.setAttribute("current_level", currentLevel);

        ItemStack next = Items.next("corp_leveling");
        ItemStack prev = Items.prev("corp_leveling");

        if (currentLevel > 1) inv.setItem(19, prev);
        if (currentLevel < (Corporation.MAX_LEVEL - 3)) inv.setItem(25, next);

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

            lore.add((completed ? ChatColor.GREEN : ChatColor.RED) + format(viewer, get(viewer, "constants.corporation.increase_children"), 5));

            if (cLevel == 3)
                lore.add(ChatColor.LIGHT_PURPLE + get(viewer, "constants.corporation.headquarters"));

            if (cLevel <= 50 && cLevel % 5 == 0)
                lore.add(ChatColor.DARK_GREEN + format(viewer, get(viewer, "constants.corporation.profit_modifier"), 10 + "%"));

            if (cLevel <= 54 && cLevel % 6 == 0)
                lore.add(ChatColor.BLUE + format(viewer, get(viewer, "constants.corporation.rank_slots"), 1));

            // Icon Setting

            ItemStack icon;

            if (cLevel < level) icon = Items.LIME_STAINED_GLASS_PANE;
            else if (cLevel == level) icon = c.getPublicIcon();
            else icon = Items.YELLOW_STAINED_GLASS_PANE;

            icon = Items.builder(icon,
                    meta -> {
                        meta.setDisplayName(format((completed ? ChatColor.GREEN : ChatColor.YELLOW) + get(viewer, "constants.level"), format("%,d", cLevel)));
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

    public static NovaInventory generateCorporationAchievements(Corporation c, Player viewer) {
        NovaInventory inv = genGUI(54, ChatColor.DARK_BLUE + c.getName() + " | " + get(viewer, "constants.achievements"));
        inv.setCancelled();

        inv.setItem(13, c.getPublicIcon());

        for (int i = 0; i < 14; i++) {
            if (CorporationAchievement.values().length <= i) break;

            CorporationAchievement a = CorporationAchievement.values()[i];
            int index = i < 7 ? GUI_SPACE + i : 37 + i;

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
                            lore.add(ChatColor.GOLD + "+" + format(viewer, get(viewer, "constants.experience"), format("%,.0f", a.getExperienceReward() * next)));
                            lore.add(" ");
                            lore.add(ChatColor.DARK_GREEN + format(viewer, get(viewer, "constants.completed"), format("%,.1f", a.getProgress(c)) + "%"));
                        } else
                            lore.add(ChatColor.DARK_GREEN + format(viewer, get(viewer, "constants.completed"), "100.0%"));

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

    public static NovaInventory generateCorporationStatistics(@NotNull Corporation c, Player viewer) {
        NovaInventory inv = genGUI(45, ChatColor.DARK_BLUE + c.getName() + " | " + get(viewer, "constants.statistics"));
        inv.setCancelled();

        inv.setItem(13, c.getPublicIcon());

        ItemStack views = Items.builder(Material.matchMaterial("SPYGLASS") == null ? Material.COMPASS : Material.matchMaterial("SPYGLASS"),
                meta -> meta.setDisplayName(format(viewer, get(viewer, "constants.views"), format("%,d", c.getViews())))
        );
        inv.setItem(21, views);

        ItemStack moneyMade = Items.builder(Material.DIAMOND,
                meta -> {
                    meta.setDisplayName(ChatColor.AQUA + get(viewer, "constants.stats.global.total_made"));
                    meta.setLore(Collections.singletonList(
                            ChatColor.GOLD + format("%,.2f", c.getStatistics().getTotalProfit())
                    ));
                }
        );
        inv.setItem(22, moneyMade);

        ItemStack productsSold = Items.builder(Material.matchMaterial("BUNDLE") == null ? Material.CHEST : Material.matchMaterial("BUNDLE"),
                meta -> meta.setDisplayName(format(viewer, get(viewer, "constants.stats.global.sold"), format("%,d", c.getStatistics().getTotalProductsSold())))
        );
        inv.setItem(23, productsSold);

        inv.setItem(40, builder(BACK, nbt -> {
            nbt.setID("corporation:click");
            nbt.set(CORPORATION_TAG, c.getUniqueId());
        }));

        return inv;
    }

    public static NovaInventory genGUI(String id, int size, String name) {
        if (size < 9 || size > 54) throw new IllegalArgumentException("Invalid size " + size);
        if (size % 9 > 0) throw new IllegalArgumentException("Invalid size " + size);

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

        int pageCount = Math.floorDiv(ratings.size() - 1, GUI_SPACE) + 1;

        for (int i = 0; i < pageCount; i++) {
            NovaInventory inv = genGUI(54, ChatColor.DARK_AQUA + get(p, "constants.business.ratings") + " - " + format(p, get(p, "constants.page"), i + 1));
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

            List<Rating> rlist = new ArrayList<>(ratings.subList(i * GUI_SPACE, Math.min((i + 1) * GUI_SPACE, ratings.size())));
            rlist.forEach(r -> {
                OfflinePlayer owner = r.getOwner();
                NovaPlayer np = new NovaPlayer(owner);
                boolean anon = np.getSetting(Settings.Personal.ANONYMOUS_RATING);

                inv.addItem(builder(createPlayerHead(anon ? null : owner),
                        meta -> {
                            meta.setDisplayName(ChatColor.AQUA + (anon ? get(p, "constants.business.anonymous") : owner.getName()));

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

        int pageCount = Math.floorDiv(econs.size() - 1, GUI_SPACE) + 1;

        boolean ad = r.nextBoolean() && NovaConfig.getConfiguration().isAdvertisingEnabled();
        Business randB = Business.randomAdvertisingBusiness();

        for (int i = 0; i < pageCount; i++) {
            NovaInventory inv = genGUI(54, get(p, "constants.balances") + " - " + format(p, get(p, "constants.page"), i + 1));
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

            List<Economy> elist = new ArrayList<>(econs.subList(i * GUI_SPACE, Math.min((i + 1) * GUI_SPACE, econs.size())));
            elist.forEach(econ -> {
                double balance = np.getBalance(econ);
                boolean debt = balance < 0 || (balance == 0 && NovaConfig.getConfiguration().isNegativeBalancesIncludeZero());

                ItemStack item = econ.getIcon().clone();
                ItemMeta eMeta = item.getItemMeta();
                eMeta.setLore(Collections.singletonList(
                        (debt ? ChatColor.RED : ChatColor.GOLD) + format("%,.2f", balance) + econ.getSymbol()
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

    public static List<NovaInventory> getBankBalanceGUI(SortingType<Economy> sorter, Player viewer) {
        List<NovaInventory> invs = new ArrayList<>();

        ItemStack nextA = next("bank_balance");
        ItemStack prevA = prev("bank_balance");

        List<Economy> econs = new ArrayList<>(Economy.getEconomies())
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());

        int pageCount = Math.max(Math.floorDiv(econs.size() - 1, GUI_SPACE), 1);

        boolean ad = r.nextBoolean() && NovaConfig.getConfiguration().isAdvertisingEnabled();
        Business randB = Business.randomAdvertisingBusiness();

        for (int i = 0; i < pageCount; i++) {
            NovaInventory inv = genGUI(54, get(viewer, "constants.bank.balance") + " - " + format(viewer, get(viewer, "constants.page"), i + 1));
            inv.setCancelled();

            final int fI = i;
            inv.setAttribute("sorting_type", Economy.class);
            inv.setAttribute("sorting_function", (Function<SortingType<Economy>, NovaInventory>) s ->
                    getBankBalanceGUI(s, viewer).get(fI));

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

            List<Economy> elist = new ArrayList<>(econs.subList(i * GUI_SPACE, Math.min((i + 1) * GUI_SPACE, econs.size())));
            elist.forEach(econ -> {
                ItemStack item = new ItemStack(econ.getIconType());
                modelData(item, econ.getCustomModelData());

                ItemMeta iMeta = item.getItemMeta();
                iMeta.setDisplayName(ChatColor.AQUA + format("%,.2f", Bank.getBalance(econ)) + econ.getSymbol() + " (" + econ.getName() + ")");
                List<String> topDonors = new ArrayList<>();
                topDonors.add(ChatColor.YELLOW + get(viewer, "constants.bank.top_donors"));
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

    public static NovaInventory generateBusinessDiscovery(Player viewer, SortingType<Business> sorter, String... keywords) {
        NovaInventory discover = genGUI(54, get(viewer, "constants.business.discover"));
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
        for (int i = 0; i < GUI_SPACE; i++) {
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
    public static NovaInventory generateBusinessInvites(@NotNull Business b, @NotNull SortingType<CorporationInvite> sorter, Player viewer) {
        NovaInventory inv = genGUI(36, get(viewer, "constants.business.invites"));
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
                    meta -> meta.setDisplayName(ChatColor.WHITE + get(viewer, "constants.business.invites.none_yet"))
            ));

            inv.addItem(9, GUI_BACKGROUND);
            return inv;
        } else {
            inv.setAttribute("sorting_type", CorporationInvite.class);
            inv.setAttribute("sorting_function", (Function<SortingType<CorporationInvite>, NovaInventory>) s -> generateBusinessInvites(b, s, viewer));

            inv.setItem(18, sorter(sorter));

            invites.forEach(i -> {
                Corporation from = i.getFrom();
                ItemStack item = builder(from.getIcon(),
                        meta -> {
                            meta.setDisplayName(ChatColor.GOLD + from.getName());
                            meta.setLore(Arrays.asList(
                                    ChatColor.AQUA + formatTimeAgo(viewer, i.getInvitedTimestamp().getTime()),
                                    " ",
                                    ChatColor.YELLOW + get(viewer, "constants.business.invites.right_click_accept"),
                                    ChatColor.YELLOW + get(viewer, "constants.business.invites.left_click_decline")
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
        NovaInventory inv = genGUI(54, get(p, "constants.market"));
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
                                ChatColor.GREEN + format(p, get(p, "constants.market.purchases_left"), ChatColor.GOLD + format("%,d", purchasesLeft))
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
                        meta.setDisplayName(ChatColor.DARK_GREEN + get(p, "constants.market.last_restock"));
                        meta.setLore(Collections.singletonList(ChatColor.AQUA + formatTimeAgo(p, NovaConfig.getMarket().getLastRestockTimestamp().getTime())));
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

        inv.setItem(10, 19, GUI_SPACE, 37, GUI_BACKGROUND);

        List<Material> products = category.getItems()
                .stream()
                .filter(m -> !NovaConfig.getMarket().getBlacklistedMaterials().contains(m))
                .collect(Collectors.toList())
                .subList(page * GUI_SPACE, Math.min(category.getItems().size(), (page + 1) * GUI_SPACE))
                .stream()
                .sorted(sorter)
                .collect(Collectors.toList());

        inv.setAttribute("page", page);
        int pages = (category.getItems().size() / GUI_SPACE) + 1;

        if (pages > 1 && page > 0)
            inv.setItem(47, builder(Items.head("arrow_left_gray"),
                    meta -> meta.setDisplayName(ChatColor.AQUA + get(p, "constants.prev")),
                    nbt -> {
                        nbt.setID("market:page");
                        nbt.set("page", page - 1);
                        nbt.set("operation", false);
                    }
            ));

        if (pages > 1 && page < (pages - 1))
            inv.setItem(53, builder(Items.head("arrow_right_gray"),
                    meta -> meta.setDisplayName(ChatColor.AQUA + get(p, "constants.next")),
                    nbt -> {
                        nbt.setID("market:page");
                        nbt.set("page", page + 1);
                        nbt.set("operation", true);
                    }
            ));

        inv.setItem(17, 26, 35, 44, null);
        inv.setItem(45, Items.economyWheel("market", econ, p));

        for (int i = 0; i < Math.min(products.size(), GUI_SPACE); i++) {
            int index = 11 + i + ((i / 7) * 2);
            Material m = products.get(i);

            double price = NovaConfig.getMarket().getPrice(m, econ);

            inv.setItem(index, builder(m,
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + WordUtils.capitalizeFully(m.name().replace('_', ' ')));

                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.GOLD + format("%,.2f", price) + econ.getSymbol());

                        if (np.getPurchases(m).stream().anyMatch(Receipt::isRecent))
                            lore.add(ChatColor.GREEN + get(p, "constants.market.purchased_recently"));

                        lore.add(" ");
                        if (NovaConfig.getMarket().getStock(m) <= 0)
                            lore.add(ChatColor.RED + get(p, "constants.business.no_stock"));
                        else
                            lore.add(ChatColor.LIGHT_PURPLE + format(p, get(p, "constants.business.stock_left"), ChatColor.BLUE + format("%,d", NovaConfig.getMarket().getStock(m))));

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

    public static List<NovaInventory> generateBusinessSupplyChests(@NotNull Business business, @NotNull SortingType<Block> sorter, Player viewer) {
        List<NovaInventory> invs = new ArrayList<>();

        List<Block> blocks = business.getSupplyChests()
                .stream()
                .map(Chest::getBlock)
                .sorted(sorter)
                .collect(Collectors.toList());

        int limit = (business.getSupplyChests().size() / GUI_SPACE) + 1;

        for (int i = 0; i < limit; i++) {
            NovaInventory inv = genGUI(54, get(viewer, "constants.business.supply_chests"));

            final int fI = i;
            inv.setItem(18, Items.sorter(sorter));

            inv.setAttribute("business", business.getUniqueId());
            inv.setAttribute("sorter", sorter);
            inv.setAttribute("sorting_type", Block.class);
            inv.setAttribute("sorting_function", (Function<SortingType<Block>, NovaInventory>) s -> generateBusinessSupplyChests(business, s, viewer).get(fI));

            List<Block> chests = blocks.subList(i * GUI_SPACE, Math.min(blocks.size(), (i + 1) * GUI_SPACE));
            chests.forEach(b -> inv.addItem(NBTWrapper.builder(Material.CHEST,
                    meta -> {
                        meta.setDisplayName(ChatColor.BLUE + b.getWorld().getName() + ChatColor.GOLD + " | " + ChatColor.YELLOW + b.getX() + ", " + b.getY() + ", " + b.getZ());
                        meta.setLore(Arrays.asList(
                                " ",
                                ChatColor.YELLOW + get(viewer, "constants.click_remove")
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
                    inv.setItem(47, Items.prev(STORED));

                if (i < (limit - 1))
                    inv.setItem(53, Items.next(STORED));
            }

            invs.add(inv);
        }

        if (limit > 1)
            for (NovaInventory inv : invs) inv.setAttribute("invs", invs);

        return invs;
    }

    public static List<NovaInventory> generateAuctionHouse(@NotNull OfflinePlayer viewer, @NotNull SortingType<? super AuctionProduct> sortingType, @NotNull String searchQuery) {
        return generateAuctionHouse(viewer, sortingType, searchQuery, a -> !a.isExpired());
    }

    public static List<NovaInventory> generateAuctionHouse(@NotNull OfflinePlayer viewer, @NotNull SortingType<? super AuctionProduct> sortingType, @NotNull String searchQuery, @NotNull Predicate<AuctionProduct> internal) {
        List<NovaInventory> invs = new ArrayList<>();
        NovaPlayer np = new NovaPlayer(viewer);

        List<AuctionProduct> items = AuctionHouse.getProducts()
                .stream()
                .filter(internal)
                .filter(a -> {
                    if (searchQuery.isEmpty()) return true;

                    ItemStack item = a.getItem();
                    String display = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "";

                    AtomicBoolean contains = new AtomicBoolean(false);

                    for (String s : searchQuery.split("\\s")) {
                        if (item.getType().name().toLowerCase().contains(s.toLowerCase())) contains.compareAndSet(false, true);
                        if (display.toLowerCase().contains(s.toLowerCase())) contains.compareAndSet(false, true);
                    }

                    return contains.get();
                })
                .sorted(sortingType)
                .collect(Collectors.toList());

        int limit = (items.size() / GUI_SPACE) + 1;

        for (int i = 0; i < limit; i++) {
            NovaInventory inv = genGUI(54, get(viewer, "constants.auction_house") + " | " + format(viewer, get(viewer, "constants.page"), i + 1));
            inv.setCancelled();
            inv.setAttribute("auction:search_query", searchQuery);

            inv.setItem(4, createPlayerHead(viewer));

            if (searchQuery.isEmpty())
                inv.setItem(6, builder(OAK_SIGN,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.search")),
                        nbt -> nbt.setID("auction_house:search")

                ));
            else
                inv.setItem(6, builder(RED_WOOL,
                        meta -> {
                            meta.setDisplayName(ChatColor.RED + get(viewer, "constants.clear_search"));
                            meta.setLore(Collections.singletonList(ChatColor.YELLOW + "\"" + searchQuery + "\""));
                        },
                        nbt -> {
                            nbt.setID("auction_house:search");
                            nbt.set("clear", true);
                        }
                ));

            inv.setItem(7, builder(Material.PAPER,
                    meta -> meta.setDisplayName(ChatColor.GREEN + get(viewer, "constants.jump_to_page")),
                    nbt -> nbt.setID("auction_house:jump_page")
            ));

            final int fI = i;
            inv.setAttribute("sorting_type", AuctionProduct.class);
            inv.setAttribute("sorting_function", (Function<SortingType<? super AuctionProduct>, NovaInventory>) s ->
                    generateAuctionHouse(viewer, s, searchQuery).get(fI));

            inv.setItem(18, Items.sorter(sortingType));

            List<AuctionProduct> products = items.subList(i * GUI_SPACE, Math.min(items.size(), (i + 1) * GUI_SPACE));
            products.forEach(a -> {
                boolean loose = a.isLoosePrice();
                Price price = !a.isBuyNow() && !AuctionHouse.getBids(a).isEmpty() ? AuctionHouse.getTopBid(a).getPrice() : a.getPrice();

                inv.addItem(builder(a.getItem(),
                        meta -> meta.setLore(Arrays.asList(
                                (loose ? ChatColor.AQUA : ChatColor.GOLD) + price.toString() + (loose ? "(" + get(viewer, "constants.loose_price") + ")" : ""),
                                ChatColor.GREEN + formatTimeAgo(viewer, a.getPostedTimestamp().getTime()),
                                a.isBuyNow() ? ChatColor.DARK_PURPLE + get(viewer, "constants.sorting_types.auction.buy_now") : "",
                                " ",
                                ChatColor.YELLOW + get(viewer, "constants.right_click_info"),
                                ChatColor.YELLOW + get(viewer, "constants.auction_house.left_click")
                        )),
                        nbt -> {
                            nbt.setID("auction:click");
                            nbt.set(PRODUCT_TAG, a.getUUID());
                        })
                );
            });

            if (!AuctionHouse.getBidsBy(viewer).isEmpty())
                inv.setItem(49, builder(Material.GOLD_INGOT,
                        meta -> meta.setDisplayName(ChatColor.GOLD + get(viewer, "constants.auction_house.my_bids")),
                        nbt -> {
                            nbt.setID("auction_house:my_bids");
                            nbt.set("bidder", viewer.getUniqueId());
                        }
                ));

            if (!AuctionHouse.getProducts(viewer).isEmpty())
                inv.setItem(50, builder(Material.IRON_DOOR,
                        meta -> meta.setDisplayName(ChatColor.LIGHT_PURPLE + get(viewer, "constants.auction_house.my_auctions")),
                        nbt -> {
                            nbt.setID("auction_house:my_auctions");
                            nbt.set("owner", viewer.getUniqueId());
                        }
                ));

            if (!np.getWonAuctions().isEmpty())
                inv.setItem(51, builder(Material.CHEST,
                        meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.auction_house.won_auctions")),
                        nbt -> {
                            nbt.setID("auction_house:won_auctions");
                            nbt.set("owner", viewer.getUniqueId());
                        }
                ));


            if (limit > 1) {
                if (i > 0)
                    inv.setItem(47, Items.prev(STORED));

                if (i < (limit - 1))
                    inv.setItem(53, Items.next(STORED));
            }

            invs.add(inv);
        }

        if (limit > 1)
            for (NovaInventory inv : invs) inv.setAttribute("invs", invs);

        return invs;
    }

    public static NovaInventory generateAuctionInfo(@NotNull OfflinePlayer viewer, @NotNull AuctionProduct a, @Nullable String searchQuery) {
        NovaInventory inv = genGUI(45, get(viewer, "constants.auction_house"));
        inv.setCancelled();
        inv.setAttribute("auction:search_query", searchQuery);

        inv.setItem(4, createPlayerHead(viewer));

        SimpleDateFormat format = new SimpleDateFormat(AuctionProduct.EXPIRATION_FORMAT.toPattern(), Language.getCurrentLocale());
        inv.setItem(13, Items.builder(a.getItem(),
                meta -> meta.setLore(Arrays.asList(
                        ChatColor.LIGHT_PURPLE + "ID: " + a.getUUID().toString().replace("-", ""),
                        " ",
                        ChatColor.RED + format(ChatColor.RED + get(viewer, "constants.expires_on"), format.format(a.getExpirationTimestamp())),
                        ChatColor.YELLOW + a.getPrice().toString(),
                        ChatColor.GOLD + formatTimeAgo(viewer, a.getPostedTimestamp().getTime())
                ))
        ));

        if (!a.isBuyNow() && !AuctionHouse.getBids(a).isEmpty()) {
            Bid top = AuctionHouse.getTopBid(a);
            inv.setItem(30, Items.builder(Material.CHEST,
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.top_bid"));
                        meta.setLore(Collections.singletonList(
                                ChatColor.AQUA + top.getBidder().getName() + " | " + ChatColor.GOLD + top.getPrice()
                        ));
                    }
            ));
        }

        if (a.getOwner().equals(viewer)) {
            inv.setItem(31, builder(Material.REDSTONE_BLOCK,
                    meta -> meta.setDisplayName(ChatColor.RED + get(viewer, "constants.remove_item")),
                    nbt -> {
                        nbt.setID("auction:remove_item");
                        nbt.set(PRODUCT_TAG, a.getUUID());
                    })
            );

            inv.setItem(32, builder(IRON_BARS,
                    meta -> meta.setDisplayName(ChatColor.YELLOW + get(viewer, "constants.auction_house.end_auction")),
                    nbt -> {
                        nbt.setID("auction:end");
                        nbt.set(PRODUCT_TAG, a.getUUID());
                    })
            );
        }

        inv.setItem(38, builder(BACK, nbt -> nbt.setID("auction_house")));

        return inv;
    }

    public static List<NovaInventory> generateWonAuctions(@NotNull Player p) {
        NovaPlayer np = new NovaPlayer(p);

        List<NovaInventory> invs = new ArrayList<>();
        List<AuctionProduct> wonProducts = np.getWonAuctions().stream()
                .sorted(Comparator.comparing(AuctionProduct::getExpirationTimestamp))
                .collect(Collectors.toList());

        int limit = (wonProducts.size() / GUI_SPACE) + 1;

        for (int i = 0; i < limit; i++) {
            List<AuctionProduct> pageProducts = wonProducts.subList(i * GUI_SPACE, Math.min(wonProducts.size(), (i + 1) * GUI_SPACE));
            NovaInventory won = genGUI(54, get(p, "constants.auction_house.won_auctions"));
            won.setCancelled();
            won.setAttribute("page", i);

            for (AuctionProduct product : pageProducts)
                won.addItem(builder(product.getItem(),
                        meta -> meta.setLore(Arrays.asList(
                                ChatColor.GOLD + format(p, get(p, "constants.price"), format("%,.2f", product.getPrice().getAmount()), product.getPrice().getEconomy().getSymbol()),
                                " ",
                                ChatColor.YELLOW + get(p, "constants.click_to_claim")
                        )), nbt -> {
                            nbt.setID("auction:claim");
                            nbt.set(PRODUCT_TAG, product.getUUID());
                        })
                );

            if (limit > 1) {
                if (i > 0)
                    won.setItem(47, Items.prev(STORED));

                if (i < (limit - 1))
                    won.setItem(53, Items.next(STORED));
            }

            invs.add(won);
        }

        if (limit > 1)
            invs.forEach(i -> i.setAttribute("invs", invs));

        return invs;
    }

    public static NovaInventory generateCorporationRanks(@NotNull Player p, @NotNull Corporation c) {
        NovaInventory inv = genGUI(54, c.getName() + " | " + ChatColor.DARK_PURPLE + get(p, "constants.ranks"));
        inv.setCancelled();

        CorporationRank pRank = c.getRank(p);

        inv.setItem(3, c.getPublicIcon());
        inv.setItem(5, Items.builder(createPlayerHead(p),
                meta -> {
                    meta.setDisplayName(ChatColor.AQUA + "[" + pRank.getPrefix() + "] " + p.getName());
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + pRank.getName()));
                })
        );

        inv.setItem(10, 11, 12, 14, 15, 16, GUI_BACKGROUND);

        inv.setItem(13, builder(Material.EMERALD,
                meta -> {
                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + get(p, "constants.corporation.owner"));
                    meta.setLore(Collections.singletonList(ChatColor.YELLOW + c.getOwner().getName()));
                    meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }, nbt -> {
                    nbt.setID("player_stats");
                    nbt.set("player", c.getOwner().getUniqueId());
                })
        );

        List<CorporationRank> ranks = c.getRanks()
                .stream()
                .filter(r -> !r.getIdentifier().equals(CorporationRank.OWNER_RANK))
                .sorted(Comparator.comparingInt(CorporationRank::getPriority))
                .collect(Collectors.toList());

        for (int i = 0; i < ranks.size(); i++) {
            int index = i < 5 ? 20 + i : 24 + i;
            if (index == 34) index = 40;

            CorporationRank rank = ranks.get(i);
            inv.setItem(index, builder(rank.getIcon(),
                    meta -> {
                        meta.setDisplayName(ChatColor.YELLOW + "[" + rank.getPrefix() + "] " + rank.getName());
                        meta.setLore(Arrays.asList(
                                ChatColor.AQUA + format(p, get(p, "constants.members"), format("%,d", c.getMembers(rank).size())),
                                " ",
                                ChatColor.YELLOW + get(p, "constants.left_click_edit")
                        ));
                    }, nbt -> {
                        nbt.setID("corporation:edit_rank");
                        nbt.set(CORPORATION_TAG, c.getUniqueId());
                        nbt.set("rank", rank.getIdentifier());
                    })
            );
        }

        inv.setItem(46, builder(BACK, nbt -> {
            nbt.setID("corporation:click");
            nbt.set(CORPORATION_TAG, c.getUniqueId());
        }));

        return inv;
    }

    public static NovaInventory generateCorporationRankEditor(@NotNull Player p, @NotNull CorporationRank rank) {
        if (rank.getIdentifier().equals(CorporationRank.OWNER_RANK)) throw new AssertionError("Cannot edit owner rank");

        Corporation c = rank.getCorporation();
        NovaInventory inv = genGUI(54, c.getName() + " | " + ChatColor.DARK_BLUE + rank.getName());
        inv.setCancelled();

        inv.setItem(3, c.getPublicIcon());
        inv.setItem(5, Items.builder(rank.getIcon(),
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + "[" + rank.getPrefix() + "] " + rank.getName());
                    meta.setLore(Arrays.asList(ChatColor.GOLD + format(p, get(p, "constants.members"), format("%,d", c.getMembers(rank).size())) ));
                })
        );

        // Name, Icon, Prefix

        inv.setItem(17, builder(OAK_SIGN,
                meta -> {
                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + get(p, "constants.set_name"));
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + rank.getName()));
                },
                nbt -> {
                    nbt.setID("corporation:edit_rank:item");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                    nbt.set("rank", rank.getIdentifier());
                    nbt.set(TYPE_TAG, "name");
                })
        );

        inv.setItem(26, builder(Material.GLASS,
                meta -> {
                    meta.setDisplayName(ChatColor.AQUA + get(p, "constants.set_prefix"));
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + rank.getPrefix()));
                },
                nbt -> {
                    nbt.setID("corporation:edit_rank:item");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                    nbt.set("rank", rank.getIdentifier());
                    nbt.set(TYPE_TAG, "prefix");
                })
        );

        inv.setItem(35, builder(IRON_BARS,
                meta -> {
                    meta.setDisplayName(ChatColor.YELLOW + get(p, "constants.set_icon"));
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + rank.getIcon().name().toLowerCase()));
                },
                nbt -> {
                    nbt.setID("corporation:edit_rank:item");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                    nbt.set("rank", rank.getIdentifier());
                    nbt.set(TYPE_TAG, "icon");
                })
        );


        // Permissions

        List<CorporationPermission> perms = Arrays.stream(CorporationPermission.values())
                .sorted(Comparator.comparing(CorporationPermission::name))
                .collect(Collectors.toList());

        for (int i = 0; i < perms.size(); i++) {
            int index = i < 7 ? 19 + i : 21 + i;
            CorporationPermission permission = perms.get(i);
            boolean perm = rank.hasPermission(permission);

            inv.setItem(index, generateCorporationPermissionNode(c, rank, permission, perm, p));
        }

        inv.setItem(46, builder(BACK, nbt -> {
            nbt.setID("corporation:ranks");
            nbt.set(CORPORATION_TAG, c.getUniqueId());
        }));

        return inv;
    }

    public static ItemStack generateCorporationPermissionNode(Corporation c, CorporationRank rank, CorporationPermission permission, boolean perm, Player viewer) {
        return builder(perm ? LIME_WOOL : RED_WOOL,
                meta -> {
                    String nameKey = "constants.corporation.permission." + permission.name().toLowerCase();

                    meta.setDisplayName(ChatColor.YELLOW + get(nameKey) + ": " + (perm ? ChatColor.GREEN + get(viewer, "constants.on") : ChatColor.RED + get(viewer, "constants.off")));
                    meta.setLore(Arrays.stream(ChatPaginator.wordWrap(get(nameKey + ".desc"), 30))
                            .map(s -> ChatColor.GRAY + s)
                            .collect(Collectors.toList())
                    );

                    if (perm) {
                        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }
                }, nbt -> {
                    nbt.setID("corporation:edit_rank:toggle_permission");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                    nbt.set("rank", rank.getIdentifier());
                    nbt.set("permission", permission.name());
                    nbt.set("state", perm);
                }
       );
    }

    public static NovaInventory generateLanguageSettings(Player p) {
        NovaInventory inv = genGUI(27, get(p, "constants.language"));
        inv.setCancelled();

        NovaPlayer np = new NovaPlayer(p);

        ItemStack head = Items.builder(createPlayerHead(p), meta -> meta.setDisplayName(ChatColor.AQUA + p.getName()));
        inv.setItem(4, head);

        inv.setItem(12, Items.builder(OAK_SIGN,
                meta -> {
                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + get(p, "constants.language.name"));
                    meta.setLore(Arrays.stream(ChatPaginator.wordWrap(get(p, "constants.language.desc"), 30))
                            .map(s -> ChatColor.GRAY + s)
                            .collect(Collectors.toList()));
                }
        ));

        inv.setItem(14, builder(CYAN_WOOL,
                meta -> meta.setDisplayName(ChatColor.AQUA + get(p, "constants.language.select")),
                nbt -> nbt.setID("language:select")
        ));

        inv.setItem(19, builder(BACK, nbt -> nbt.setID("back:settings")));
        return inv;
    }

    // Utilities

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
