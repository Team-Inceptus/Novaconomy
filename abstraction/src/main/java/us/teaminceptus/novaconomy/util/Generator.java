package us.teaminceptus.novaconomy.util;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.PRODUCT_TAG;
import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.w;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.of;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.ChatPaginator;

import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.business.BusinessAdvertiseEvent;
import us.teaminceptus.novaconomy.api.settings.Settings;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.CORPORATION_TAG;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.get;

public final class Generator {
    
    private Generator() {}

    public static NovaInventory genGUI(int size, String name) {
        return genGUI("", size, name);
    }

    public static NovaInventory generateBusinessData(Business b, Player viewer, boolean advertising) {
        NovaInventory inv = genGUI(54, ChatColor.GOLD + b.getName());
        inv.setCancelled();
    
        if (!b.getRatings().isEmpty()) inv.setItem(44, Items.LOADING);
        for (int i = 46; i < 53; i++) inv.setItem(i, null);
    
        ItemStack icon = new ItemStack(b.getIcon());
        ItemMeta iMeta = icon.getItemMeta();
        iMeta.setDisplayName(ChatColor.GOLD + b.getName());
        iMeta.setLore(Collections.singletonList(ChatColor.YELLOW + "ID: " + b.getUniqueId().toString().replace("-", "")));
        icon.setItemMeta(iMeta);
        inv.setItem(15, icon);
    
        boolean anonymous = !b.getSetting(Settings.Business.PUBLIC_OWNER) && !b.isOwner(viewer);
        ItemStack owner = builder(w.createSkull(anonymous ? null : b.getOwner()),
                meta -> {
                    meta.setDisplayName(anonymous ? ChatColor.AQUA + get("constants.business.anonymous") : String.format(get("constants.business.owner"), b.getOwner().getName()));
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
                    nbt.set("business", b.getUniqueId());
                    nbt.set("anonymous", !pHome);
                }
        );
        ItemMeta hMeta = home.getItemMeta();
        home.setItemMeta(hMeta);
        inv.setItem(12, home);
    
        ItemStack settings = builder(Material.NETHER_STAR,
                meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.settings.business")),
                nbt -> {
                    nbt.setID("business:settings");
                    nbt.set("business", b.getUniqueId());
                }
        );
    
        if (b.isOwner(viewer)) inv.setItem(53, settings);
    
        AtomicInteger slot = new AtomicInteger(19);
        List<BusinessProduct> bProducts = b.getProducts();
    
        bProducts.forEach(p -> {
            if (slot.get() == 26) slot.set(28);
            if (slot.get() == 35) slot.set(37);
            if (slot.get() == 44) slot.set(46);
            if (slot.get() >= 53) return;
    
            ItemStack item = p.getItem().clone();
            if (item.getType() == Material.AIR) return;
    
            AtomicBoolean stock = new AtomicBoolean(true);
    
            ItemStack product = builder(item.clone(), 
                    meta -> {
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        lore.add(" ");
                        lore.add(String.format(get("constants.business.price"), String.format("%,.2f", p.getAmount()).replace("D", ""), p.getEconomy().getSymbol() + ""));
            
                        lore.add(" ");
                        if (!b.isInStock(item)) {
                            lore.add(ChatColor.RED + get("constants.business.no_stock"));
                            stock.set(false);
                        } else {
                            AtomicInteger index = new AtomicInteger();
                            b.getResources().forEach(res -> {
                                if (item.isSimilar(res)) index.addAndGet(res.getAmount());
                            });
            
                            lore.add(String.format(get("constants.business.stock_left"), String.format("%,d", index.get())));
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
    
        boolean pStats = b.getSetting(Settings.Business.PUBLIC_STATISTICS) || b.isOwner(viewer);
        ItemStack stats = builder(pStats ? Material.PAPER : Material.BARRIER,
                meta -> {
                    meta.setDisplayName(pStats ? ChatColor.AQUA + get("constants.business.statistics") : ChatColor.RED + get("constants.business.anonymous_statistics"));
                    if (b.isOwner(viewer) && !b.getSetting(Settings.Business.PUBLIC_STATISTICS))
                        meta.setLore(Collections.singletonList(ChatColor.YELLOW + get("constants.business.hidden")));
                }, nbt -> {
                    nbt.setID("business:statistics");
                    nbt.set("business", b.getUniqueId());
                    nbt.set("anonymous", !pStats);
                }
        );
        inv.setItem(14, stats);
    
        if (b.isOwner(viewer)) inv.setItem(26, builder(Material.BUCKET,
                meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.business.advertising")),
                nbt -> nbt.setID("business:advertising")
        ));
    
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
    
            ItemStack rating = Items.builder(pRating ? CommandWrapper.getRatingMats()[avgI] : Material.BARRIER,
                meta -> { meta.setDisplayName(pRating ? ChatColor.YELLOW + String.format("%,.1f", avg) + "⭐" : ChatColor.RED + get("constants.business.anonymous_rating"));
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
                        nbt.set("business", rand.getUniqueId());
                        nbt.set("from_business", b.getUniqueId());
                    }));
            }
        }
    
        return inv;
    }

    public static NovaInventory generateCorporationData(Corporation c, Player viewer, SortingType<Business> childrenSort) {
        NovaInventory inv = genGUI(54, String.format(get("constants.corporation.title"), c.getName()));
        inv.setCancelled();
    
        ItemStack hq = builder(Material.GLASS,
                meta -> meta.setDisplayName(ChatColor.YELLOW + get("constants.headquarters")),
                nbt -> {
                    nbt.setID("corporation:hq");
                    nbt.set(CORPORATION_TAG, c.getUniqueId());
                }
        );
        inv.setItem(12, hq);

        ItemStack icon = Items.builder(c.getPublicIcon(),
                meta -> {
                    List<String> lore = new ArrayList<>();
                    String idLine = ChatColor.YELLOW + "ID: " + c.getUniqueId().toString().replace("-", "");
                    String desc = c.getDescription();

                    if (desc.isEmpty()) {
                        lore.add(idLine);
                        meta.setLore(lore);
                        return;
                    }
                    
                    lore.addAll(Arrays.stream(ChatPaginator.wordWrap(desc, 30))
                            .map(s -> ChatColor.YELLOW + s)
                            .collect(Collectors.toList())
                    );

                    lore.add(" ");
                    lore.add(idLine);
                    meta.setLore(lore);
                }
        );
        inv.setItem(13, icon);
    
        ItemStack leveling = builder(Material.GOLD_BLOCK,
                meta -> {
                    meta.setDisplayName(String.format(get("constants.corporation.leveling"), String.format("%,d", c.getLevel()) ));
                    meta.setLore(Collections.singletonList(
                        String.format(get("constants.corporation.experience"), String.format("%,.2f", c.getExperience()))
                    ));
                }, nbt -> nbt.setID("corporation:leveling")
        );
        inv.setItem(14, leveling);

        // Children

        List<Business> children = c.getChildren()
                    .stream()
                    .sorted(childrenSort)
                    .limit(14)
                    .collect(Collectors.toList());

        return inv;
    }

    public static NovaInventory genGUI(String id, int size, String name) {
        if (size < 9 || size > 54) return null;
        if (size % 9 > 0) return null;
    
        NovaInventory inv = w.createInventory(id, name, size);
        ItemStack bg = w.getGUIBackground();
    
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
                    meta.setDisplayName("" + ChatColor.YELLOW + amount + econ.getSymbol());
                    meta.setLore(Collections.singletonList(ChatColor.GOLD + "" + amount + " " + econ.getName() + "(s)"));
                }, nbt -> {
                    nbt.setID("economy:check");
                    nbt.set("economy", econ.getUniqueId());
                    nbt.set("amount", amount);
                });
    }

}
