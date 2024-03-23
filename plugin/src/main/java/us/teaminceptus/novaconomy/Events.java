package us.teaminceptus.novaconomy;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.bank.Bank;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.business.BusinessViewEvent;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationExperienceChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.player.Bounty;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.util.NovaSound;
import us.teaminceptus.novaconomy.util.NovaUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.Novaconomy.isIgnored;
import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.AMOUNT_TAG;
import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.ECON_TAG;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.r;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.w;
import static us.teaminceptus.novaconomy.api.corporation.CorporationAchievement.*;
import static us.teaminceptus.novaconomy.messages.MessageHandler.*;

final class Events implements Listener {

    private final Novaconomy plugin;

    protected Events(Novaconomy plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    // Util Events

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        w.addPacketInjector(p);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        w.removePacketInjector(p);
    }
    
    // Functionality Events

    @EventHandler
    public void claimCheck(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (p.getInventory().getItemInHand() == null) return;

        NovaPlayer np = new NovaPlayer(p);
        ItemStack item = e.getItem();
        if (!hasID(item)) return;
        if (!getID(item).equalsIgnoreCase("economy:check")) return;

        NBTWrapper nbt = of(item);

        Economy econ = Economy.byId(nbt.getUUID(ECON_TAG));
        double amount = nbt.getDouble(AMOUNT_TAG);

        np.add(econ, amount);
        NovaUtil.sync(() -> {
            if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
            else w.removeItem(e);

            NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
        });
    }

    @EventHandler
    public void claimBounty(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        final Player killer;

        if (e.getDamager() instanceof Player) killer = (Player) e.getDamager();
        else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player)
            killer = (Player) ((Projectile) e.getDamager()).getShooter();
        else if (e.getDamager() instanceof Tameable && ((Tameable) e.getDamager()).getOwner() instanceof Player)
            killer = (Player) ((Tameable) e.getDamager()).getOwner();
        else return;

        if (!plugin.hasBounties()) return;
        Player target = (Player) e.getEntity();
        if (target.getHealth() - e.getFinalDamage() > 0) return;
        NovaPlayer nt = new NovaPlayer(target);
        if (nt.getSelfBounties().isEmpty()) return;
        NovaPlayer nk = new NovaPlayer(killer);

        String kName = killer.getDisplayName() == null ? killer.getName() : killer.getDisplayName();
        String tName = target.getDisplayName() == null ? target.getName() : target.getDisplayName();
        boolean broadcast = plugin.isBroadcastingBounties();

        String key = "bounties." + target.getUniqueId();
        AtomicDouble amount = new AtomicDouble();
        AtomicInteger bountyCount = new AtomicInteger();

        for (Bounty b : nt.getSelfBounties()) {
            OfflinePlayer owner = b.getOwner();

            if (broadcast)
                Bukkit.broadcastMessage(format(get("success.bounty.broadcast"), kName, tName, format("%,.2f", b.getAmount()) + b.getEconomy().getSymbol()));
            else if (owner.isOnline())
                messages.sendMessage(owner.getPlayer(), "success.bounty.redeem", kName, tName);

            amount.addAndGet(b.getAmount());
            bountyCount.incrementAndGet();
            nk.add(b.getEconomy(), b.getAmount());

            NovaUtil.sync(() -> {
                File pFile = new File(NovaConfig.getPlayerDirectory(), owner.getUniqueId() + ".yml");
                FileConfiguration pConfig = YamlConfiguration.loadConfiguration(pFile);

                pConfig.set(key, null);

                try {
                    pConfig.save(pFile);
                } catch (IOException err) {
                    NovaConfig.getLogger().severe(err.getMessage());
                }
            });
        }

        if (!broadcast)
            messages.sendMessage(killer, "success.bounty.claim", bountyCount.get(), tName);
    }

    @EventHandler
    public void moneyIncrease(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.hasKillIncrease()) return;
        if (Economy.getNaturalEconomies().isEmpty()) return;

        final Player p;
        Player tmpP = null;

        if (e.getDamager() instanceof Player) tmpP = (Player) e.getDamager();
        else if (plugin.hasIndirectKillIncrease()) {
            if (e.getDamager() instanceof Projectile) {
                Projectile proj = (Projectile) e.getDamager();
                if (proj.getShooter() instanceof Player) tmpP = (Player) proj.getShooter();
            }

            if (e.getDamager() instanceof Tameable) {
                Tameable t = (Tameable) e.getDamager();
                if (t.getOwner() instanceof Player) tmpP = (Player) t.getOwner();
            }
        }

        p = tmpP;
        if (p == null) return;

        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity en = (LivingEntity) e.getEntity();
        if (en.getHealth() - e.getFinalDamage() > 0) return;

        if (en instanceof Player) {
            Player target = (Player) en;
            NovaPlayer nt = new NovaPlayer(target);

            if (nt.getSelfBounties().stream().anyMatch(b -> b.getOwner().equals(p))) return;
        }

        String id = en.getType().name();

        NovaUtil.async(() -> {
            String category = "";

            try {
                Method m = LivingEntity.class.getDeclaredMethod("getCategory");
                m.setAccessible(true);
                Object o = m.invoke(en);
                if (o != null) category = o.toString();
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException err) {
                NovaConfig.print(err);
            }

            double iAmount = en.getMaxHealth();
            if (p.getEquipment().getItemInHand() != null && plugin.hasEnchantBonus()) {
                ItemStack hand = p.getEquipment().getItemInHand();
                if (hand.hasItemMeta() && hand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_MOBS))
                    iAmount += hand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_MOBS) * (r.nextInt(4) + 6);
            }

            if (ModifierReader.getModifier("Killing") == null) return;

            Map<String, Set<Map.Entry<Economy, Double>>> entry = ModifierReader.getModifier("Killing");
            if (isIgnored(p, id)) return;
            if (!entry.containsKey(id) && isIgnored(p, category)) return;

            final double amount = iAmount;
            String fCategory = category;

            updateEvent(p, id, plugin.getKillChance(), entry, fCategory, amount);
        });
    }

    @EventHandler
    public void moneyIncrease(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        if (e.getBlock().getDrops().isEmpty()) return;
        if (Economy.getNaturalEconomies().isEmpty()) return;

        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (isBlockIgnored(p, b)) return;

        String id = b.getType().name();
        boolean ageable = w.isAgeable(b);
        if (ageable && !plugin.hasFarmingIncrease()) return;
        if (!ageable && !plugin.hasMiningIncrease()) return;

        String mod = ageable ? "Farming" : "Mining";
        int chance = mod.equalsIgnoreCase("Farming") ? plugin.getFarmingChance() : plugin.getMiningChance();

        NovaUtil.async(() -> {
            double add = r.nextInt(3) + e.getExpToDrop();
            if (p.getEquipment().getItemInHand() != null && plugin.hasEnchantBonus()) {
                ItemStack hand = p.getEquipment().getItemInHand();
                if (hand.hasItemMeta() && hand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
                    add += hand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS) * (r.nextInt(3) + 4);
            }

            if (ModifierReader.getModifier(mod) == null) return;
            Map<String, Set<Map.Entry<Economy, Double>>> entry = ModifierReader.getModifier(mod);

            String tag = blockTag(entry, b);
            final double fAdd = add;

            updateEvent(p, id, chance, entry, tag, fAdd);
        });
    }

    @EventHandler
    public void moneyIncrease(BlockPlaceEvent e) {
        if (e.isCancelled()) return;
        if (!e.canBuild()) return;
        if (Economy.getNaturalEconomies().isEmpty()) return;

        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;

        Block b = e.getBlock();
        String id = b.getType().name();
        int chance = plugin.getBuildingChance();

        NovaUtil.async(() -> {
            double add = r.nextInt(2) + 3;

            if (ModifierReader.getModifier("Building") == null) return;
            Map<String, Set<Map.Entry<Economy, Double>>> entry = ModifierReader.getModifier("Building");

            String tag = blockTag(entry, b);
            updateEvent(p, id, chance, entry, tag, add);
        });
    }

    @EventHandler
    public void moneyIncrease(PlayerFishEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.hasFishingIncrease()) return;
        if (Economy.getNaturalEconomies().isEmpty()) return;

        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player p = e.getPlayer();
        String name = e.getCaught() instanceof Item ? ((Item) e.getCaught()).getItemStack().getType().name() : e.getCaught().getType().name();
        if (isIgnored(p, name)) return;

        double iAmount = e.getExpToDrop();

        if (p.getEquipment().getItemInHand() != null && plugin.hasEnchantBonus()) {
            ItemStack hand = p.getEquipment().getItemInHand();

            if (hand.hasItemMeta()) {
                if (hand.getItemMeta().hasEnchant(Enchantment.LURE))
                    iAmount += hand.getItemMeta().getEnchantLevel(Enchantment.LURE) * (r.nextInt(6) + 5);

                if (hand.getItemMeta().hasEnchant(Enchantment.LUCK))
                    iAmount += hand.getItemMeta().getEnchantLevel(Enchantment.LUCK) * (r.nextInt(8) + 6);
            }

        }

        final double amount = iAmount;
        NovaUtil.async(() -> {
            if (ModifierReader.getModifier("Fishing") == null) return;
            Map<String, Set<Map.Entry<Economy, Double>>> entry = ModifierReader.getModifier("Fishing");

            updateEvent(p, name, plugin.getFishingChance(), entry, null, amount);
        });
    }

    @EventHandler
    public void moneyDecrease(PlayerDeathEvent e) {
        if (!plugin.hasDeathDecrease()) return;

        Player p = e.getEntity();
        NovaPlayer np = new NovaPlayer(p);

        List<String> lost = new ArrayList<>();
        lost.add(get("constants.lost"));
        String id = p.getLastDamageCause().getCause().name();
        if (isIgnored(p, id)) return;

        double divider = plugin.getDeathDivider();

        if (plugin.hasEnchantBonus())
            for (EquipmentSlot s : EquipmentSlot.values()) {
                if (s == EquipmentSlot.HAND || s.name().equalsIgnoreCase("OFF_HAND")) continue;

                ItemStack item = getItem(p.getEquipment(), s);
                if (item == null) continue;
                if (!item.hasItemMeta()) continue;

                if (item.getItemMeta().hasEnchant(Enchantment.PROTECTION_ENVIRONMENTAL))
                    divider *= Math.max(Math.min(item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) / 2, 4), 1);
            }

        if (!ModifierReader.getDeathModifiers().isEmpty()) {
            Map<EntityDamageEvent.DamageCause, Double> modifiers = ModifierReader.getDeathModifiers();
            for (Map.Entry<EntityDamageEvent.DamageCause, Double> entry : modifiers.entrySet()) {
                EntityDamageEvent.DamageCause cause = p.getLastDamageCause().getCause();
                if (cause == entry.getKey()) {
                    divider = entry.getValue();
                    break;
                }
            }
        }

        for (Economy econ : Economy.getEconomies()) {
            if (np.isInDebt(econ)) continue;

            double ogAmount = np.getBalance(econ) / divider;
            double amount = Double.isNaN(ogAmount) ? 0 : ogAmount;
            lost.add(callRemoveBalanceEvent(p, econ, amount));
        }

        messages.sendRawNotification(p, String.join("\n", lost.toArray(new String[0])));
    }

    // Corporation Leveling

    @EventHandler
    public void onPurchase(PlayerPurchaseProductEvent e) {
        if (!plugin.hasProductIncrease()) return;

        Business b = e.getProduct().getBusiness();
        Corporation c = b.getParentCorporation();
        if (c == null) return;

        long sales = c.getStatistics().getTotalSales();
        if ((sales >= 5_000 && !(c.getAchievementLevel(SELLER) >= 1))
                || (sales >= 15_000 && !(c.getAchievementLevel(SELLER) >= 2))
                || (sales >= 100_000 && !(c.getAchievementLevel(SELLER) >= 3))
                || (sales >= 350_000 && !(c.getAchievementLevel(SELLER) >= 4))
                || (sales >= 750_000 && !(c.getAchievementLevel(SELLER) >= 5))
                || (sales >= 1_500_000 && !(c.getAchievementLevel(SELLER) >= 6))
                || (sales >= 5_000_000 && !(c.getAchievementLevel(SELLER) >= 7))
                || (sales >= 10_000_000 && !(c.getAchievementLevel(SELLER) >= 8))
                || (sales >= 25_000_000 && !(c.getAchievementLevel(SELLER) >= 9))
                || (sales >= 50_000_000 && !(c.getAchievementLevel(SELLER) >= 10))
                || (sales >= 100_000_000 && !(c.getAchievementLevel(SELLER) >= 11))
        ) c.awardAchievement(SELLER);

        double profit = c.getStatistics().getTotalProfit();
        if ((profit >= 100_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 1))
                || (profit >= 450_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 2))
                || (profit >= 1_000_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 3))
                || (profit >= 2_500_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 4))
                || (profit >= 7_500_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 5))
                || (profit >= 15_000_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 6))
                || (profit >= 40_000_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 7))
                || (profit >= 75_000_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 8))
                || (profit >= 125_000_000 && !(c.getAchievementLevel(BUSINESSMAN) >= 9))
        ) c.awardAchievement(BUSINESSMAN);

        double exp = Math.min(1, r.nextDouble() * 2) * (e.getProduct().getPrice().getRealAmount() / 2);

        CorporationExperienceChangeEvent event = new CorporationExperienceChangeEvent(c, c.getExperience(), c.getExperience() + exp);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        c.setExperience(event.getNewExperience());
    }

    @EventHandler
    public void onView(BusinessViewEvent e) {
        Business b = e.getBusiness();
        Corporation c = b.getParentCorporation();
        if (c == null) return;

        long views = c.getStatistics().getTotalViews();

        if ((views >= 10_000 && !(c.getAchievementLevel(ADVERTISER) >= 1))
                || (views >= 50_000 && !(c.getAchievementLevel(ADVERTISER) >= 2))
                || (views >= 150_000 && !(c.getAchievementLevel(ADVERTISER) >= 3))
                || (views >= 500_000 && !(c.getAchievementLevel(ADVERTISER) >= 4))
                || (views >= 1_000_000 && !(c.getAchievementLevel(ADVERTISER) >= 5))
                || (views >= 5_000_000 && !(c.getAchievementLevel(ADVERTISER) >= 6))
        ) c.awardAchievement(ADVERTISER);

        double exp = Math.min(0.5, r.nextDouble() / 2);

        CorporationExperienceChangeEvent event = new CorporationExperienceChangeEvent(c, c.getExperience(), c.getExperience() + exp);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        c.setExperience(event.getNewExperience());
    }

    // Util Methods

    private void updateEvent(Player p, String id, int chance, Map<String, Set<Map.Entry<Economy, Double>>> entry, String tag, double add) {
        if (r.nextInt(100) < chance)
            if ((id != null && entry.containsKey(id)) || (tag != null && entry.containsKey(tag))) {
                NovaUtil.sync(() -> {
                    Set<Map.Entry<Economy, Double>> value = entry.getOrDefault(id, entry.get(tag));

                    sendUpdateActionbar(p, value.stream()
                            .map(pair -> {
                                double amount = pair.getValue();
                                if (amount <= 0) return null;

                                return callAddBalanceEvent(p, pair.getKey(), amount, false);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
                    );
                });
            } else
                NovaUtil.sync(() -> update(p, add));
    }

    private void update(Player p, double amount) {
        List<String> msgs = new ArrayList<>();
        for (Economy econ : Economy.getNaturalEconomies()) msgs.add(callAddBalanceEvent(p, econ, amount, true));

        sendUpdateActionbar(p, msgs);
    }

    static final List<ChatColor> COLORS = Arrays.stream(ChatColor.values()).filter(ChatColor::isColor).collect(Collectors.toList());

    private String callAddBalanceEvent(Player p, Economy econ, double amount, boolean random) {
        NovaPlayer np = new NovaPlayer(p);
        double divider = r.nextInt(2) + 1;
        double increase = Math.min(random ? ((amount + r.nextInt(8) + 1) / divider) / econ.getConversionScale() : amount, plugin.getMaxIncrease());

        if (NovaConfig.getConfiguration().isNaturalCauseIncomeTaxEnabled() && !NovaConfig.getConfiguration().isNaturalCauseIncomeTaxIgnoring(p)) {
            double removed = increase * NovaConfig.getConfiguration().getNaturalCauseIncomeTax();
            increase -= removed;

            Bank.addBalance(econ, removed);
        }

        double previousBal = np.getBalance(econ);

        PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, increase, previousBal, previousBal + increase, true);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            np.add(econ, increase);

            return COLORS.get(r.nextInt(COLORS.size())) + "+" + format("%,.2f", (Math.floor(increase * 100) / 100)) + econ.getSymbol();
        }

        return "";
    }

    private void sendUpdateActionbar(Player p, List<String> added) {
        if (added == null || added.isEmpty()) return;
        if (new NovaPlayer(p).hasNotifications()) {
            List<String> msgs = new ArrayList<>(added);

            if (added.size() > 4) {
                msgs = added.subList(0, 4);
                msgs.add(ChatColor.WHITE + "...");
            }

            NovaSound.BLOCK_NOTE_BLOCK_PLING.playSuccess(p);
            w.sendActionbar(p, String.join(ChatColor.YELLOW + ", " + ChatColor.RESET, msgs.toArray(new String[0])));
        }
    }

    private ItemStack getItem(EntityEquipment i, EquipmentSlot s) {
        switch (s) {
            case FEET:
                return i.getBoots();
            case LEGS:
                return i.getLeggings();
            case CHEST:
                return i.getChestplate();
            case HEAD:
                return i.getHelmet();
            default:
                return i.getItemInHand();
        }
    }

    private static String blockTag(Map<String, Set<Map.Entry<Economy, Double>>> entry, Block b) {
        try {
            Class<?> keyed = Class.forName("org.bukkit.Keyed");
            Class<?> tag = Class.forName("org.bukkit.Tag");

            for (Field f : tag.getFields()) {
                if (!keyed.isInstance(b.getType())) break;

                String name = f.getName();
                if (name.startsWith("ENTITY_TYPES") || name.startsWith("REGISTRY")) continue;
                if (!tag.isAssignableFrom(f.getType())) continue;
                if (!entry.containsKey(name)) continue;

                Method isTagged = tag.getDeclaredMethod("isTagged", keyed);
                isTagged.setAccessible(true);

                Object tagObj = f.get(null);
                if (tagObj == null) continue;

                if ((boolean) isTagged.invoke(tagObj, b.getType())) return name;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | ClassCastException ignored) {
        } catch (Exception err) {
            NovaConfig.print(err);
        }

        return "";
    }

    private static boolean isBlockIgnored(Player p, Block b) {
        String id = b.getType().name();
        if (isIgnored(p, id)) return true;

        try {
            Class<?> keyed = Class.forName("org.bukkit.Keyed");
            Class<?> tag = Class.forName("org.bukkit.Tag");

            for (Field f : tag.getFields()) {
                if (!keyed.isInstance(b.getType())) break;
                if (!tag.isAssignableFrom(f.getType())) continue;

                String name = f.getName();
                if (name.startsWith("ENTITY_TYPES") || name.startsWith("REGISTRY")) continue;

                Method isTagged = tag.getDeclaredMethod("isTagged", keyed);
                isTagged.setAccessible(true);

                Object tagObj = f.get(null);
                if (tagObj == null) continue;

                if ((boolean) isTagged.invoke(tagObj, b.getType()))
                    if (isIgnored(p, name)) return true;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | ClassCastException ignored) {
        } catch (Exception err) {
            NovaConfig.print(err);
        }

        return false;
    }

    private String callRemoveBalanceEvent(Player p, Economy econ, double amount) {
        NovaPlayer np = new NovaPlayer(p);
        double previousBal = np.getBalance(econ);

        PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, amount, previousBal, previousBal - amount, true);
        if (!event.isCancelled()) np.remove(econ, amount);

        return ChatColor.DARK_RED + "- " + ChatColor.RED + format("%,.2f", Math.floor(amount * 100) / 100) + econ.getSymbol();
    }

}
