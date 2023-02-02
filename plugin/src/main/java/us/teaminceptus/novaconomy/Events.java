package us.teaminceptus.novaconomy;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.events.corporation.CorporationExperienceChangeEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerChangeBalanceEvent;
import us.teaminceptus.novaconomy.api.events.player.economy.PlayerPurchaseProductEvent;
import us.teaminceptus.novaconomy.api.player.Bounty;
import us.teaminceptus.novaconomy.api.player.NovaPlayer;
import us.teaminceptus.novaconomy.util.NovaSound;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.Novaconomy.isIgnored;
import static us.teaminceptus.novaconomy.Novaconomy.r;
import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.*;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.*;

final class Events implements Listener {

    private final Novaconomy plugin;

    protected Events(Novaconomy plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

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

        Economy econ = Economy.getEconomy(nbt.getUUID(ECON_TAG));
        double amount = nbt.getDouble(AMOUNT_TAG);

        np.add(econ, amount);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else w.removeItem(e);

                NovaSound.ENTITY_ARROW_HIT_PLAYER.playSuccess(p);
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void moneyIncrease(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.hasKillIncrease()) return;

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

        new BukkitRunnable() {
            @Override
            public void run() {
                String id = en.getType().name();
                String category = "";

                try {
                    Method m = LivingEntity.class.getDeclaredMethod("getCategory");
                    m.setAccessible(true);
                    Object o = m.invoke(en);
                    if (o != null) category = o.toString();
                } catch (NoSuchMethodException ignored) {
                } catch (ReflectiveOperationException err) {
                    plugin.getLogger().severe(err.getClass().getSimpleName());
                    plugin.getLogger().severe(err.getMessage());
                    for (StackTraceElement s : err.getStackTrace()) plugin.getLogger().severe(s.toString());
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

                final double fIAmount = iAmount;
                String fCategory = category;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (r.nextInt(100) < plugin.getKillChance())
                            if (entry.containsKey(id) || entry.containsKey(fCategory)) {
                                Set<Map.Entry<Economy, Double>> value = entry.getOrDefault(id, entry.get(fCategory));
                                List<String> msgs = new ArrayList<>();
                                for (Map.Entry<Economy, Double> entry : value) {
                                    double amount = entry.getValue();
                                    if (amount <= 0) continue;
                                    msgs.add(callAddBalanceEvent(p, entry.getKey(), amount, false));
                                }

                                sendUpdateActionbar(p, msgs);
                            } else update(p, fIAmount);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void moneyIncrease(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        if (e.getBlock().getDrops().size() < 1) return;

        Block b = e.getBlock();
        boolean ageable = w.isAgeable(b);
        String id = b.getType().name();

        if (ageable && !plugin.hasFarmingIncrease()) return;
        if (!ageable && !plugin.hasMiningIncrease()) return;

        Player p = e.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                double add = r.nextInt(3) + e.getExpToDrop();
                if (p.getEquipment().getItemInHand() != null && plugin.hasEnchantBonus()) {
                    ItemStack hand = p.getEquipment().getItemInHand();
                    if (hand.hasItemMeta() && hand.getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_BLOCKS))
                        add += hand.getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_BLOCKS) * (r.nextInt(3) + 4);
                }

                String mod = ageable ? "Farming" : "Mining";

                if (ModifierReader.getModifier(mod) == null) return;
                Map<String, Set<Map.Entry<Economy, Double>>> entry = ModifierReader.getModifier(mod);

                String tagName = null;
                boolean tagIgnore = false;

                try {
                    Class<?> keyed = Class.forName("org.bukkit.Keyed");
                    Class<?> tag = Class.forName("org.bukkit.Tag");

                    for (Field f : tag.getFields()) {
                        if (!keyed.isInstance(b.getType())) break;

                        String name = f.getName();
                        if (name.startsWith("ENTITY_TYPES")) continue;
                        if (name.startsWith("REGISTRY")) continue;

                        if (!tag.isAssignableFrom(f.getType())) continue;

                        if (isIgnored(p, name)) {
                            tagIgnore = true;
                            break;
                        }

                        if (!entry.containsKey(name)) continue;

                        Method isTagged = tag.getDeclaredMethod("isTagged", keyed);
                        isTagged.setAccessible(true);

                        Object tagObj = f.get(null);
                        if (tagObj == null) continue;

                        if ((boolean) isTagged.invoke(tagObj, b.getType())) if (entry.containsKey(name)) {
                            tagName = name;
                            break;
                        }
                    }
                } catch (ClassNotFoundException | NoSuchMethodException | ClassCastException ignored) {
                } catch (Exception err) {
                    NovaConfig.print(err);
                }

                if (isIgnored(p, id)) return;
                if (!entry.containsKey(id) && tagIgnore) return;

                int chance = mod.equalsIgnoreCase("Farming") ? plugin.getFarmingChance() : plugin.getMiningChance();

                if (r.nextInt(100) < chance) if (entry.containsKey(id) || tagName != null) {
                    final String ftn = tagName;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Set<Map.Entry<Economy, Double>> value = entry.getOrDefault(id, entry.get(ftn));
                            List<String> msgs = new ArrayList<>();
                            for (Map.Entry<Economy, Double> entry : value) {
                                double amount = entry.getValue();
                                if (amount <= 0) continue;
                                msgs.add(callAddBalanceEvent(p, entry.getKey(), amount, false));
                            }

                            sendUpdateActionbar(p, msgs);
                        }
                    }.runTask(plugin);
                } else update(p, add);
            }
        }.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void moneyIncrease(PlayerFishEvent e) {
        if (e.isCancelled()) return;
        if (!plugin.hasFishingIncrease()) return;

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

        if (ModifierReader.getModifier("Fishing") != null && r.nextInt(100) < plugin.getFishingChance()) {
            Map<String, Set<Map.Entry<Economy, Double>>> entries = ModifierReader.getModifier("Fishing");
            if (entries.containsKey(name)) {
                Set<Map.Entry<Economy, Double>> value = entries.get(name);
                List<String> msgs = new ArrayList<>();
                for (Map.Entry<Economy, Double> entry : value) {
                    double amount = entry.getValue();
                    if (amount <= 0) continue;
                    msgs.add(callAddBalanceEvent(p, entry.getKey(), amount, false));
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

    static final List<ChatColor> COLORS = Arrays.stream(ChatColor.values()).filter(ChatColor::isColor).collect(Collectors.toList());

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
                Bukkit.broadcastMessage(String.format(get("success.bounty.broadcast"), kName, tName, String.format("%,.2f", b.getAmount()) + b.getEconomy().getSymbol()));
            else if (owner.isOnline())
                owner.getPlayer().sendMessage(String.format(getMessage("success.bounty.redeem"), kName, tName));

            amount.addAndGet(b.getAmount());
            bountyCount.incrementAndGet();
            nk.add(b.getEconomy(), b.getAmount());

            new BukkitRunnable() {
                @Override
                public void run() {
                    File pFile = new File(NovaConfig.getPlayerDirectory(), owner.getUniqueId() + ".yml");
                    FileConfiguration pConfig = YamlConfiguration.loadConfiguration(pFile);

                    pConfig.set(key, null);

                    try {
                        pConfig.save(pFile);
                    } catch (IOException err) {
                        NovaConfig.getLogger().severe(err.getMessage());
                    }
                }
            }.runTask(plugin);
        }

        if (!broadcast)
            killer.sendMessage(String.format(getMessage("success.bounty.claim"), bountyCount.get(), tName));
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
            double ogAmount = np.getBalance(econ) / divider;
            double amount = Double.isNaN(ogAmount) ? 0 : ogAmount;
            lost.add(callRemoveBalanceEvent(p, econ, amount));
        }

        if (np.hasNotifications()) p.sendMessage(String.join("\n", lost.toArray(new String[0])));
    }

    private String callRemoveBalanceEvent(Player p, Economy econ, double amount) {
        NovaPlayer np = new NovaPlayer(p);
        double previousBal = np.getBalance(econ);

        PlayerChangeBalanceEvent event = new PlayerChangeBalanceEvent(p, econ, amount, previousBal, previousBal - amount, true);
        if (!event.isCancelled()) np.remove(econ, amount);

        return ChatColor.DARK_RED + "- " + ChatColor.RED + String.format("%,.2f", Math.floor(amount * 100) / 100) + econ.getSymbol();
    }

    // Corporation Leveling

    @EventHandler
    public void onPurchase(PlayerPurchaseProductEvent e) {
        if (!plugin.hasProductIncrease()) return;

        Business b = e.getProduct().getBusiness();
        Corporation c = b.getParentCorporation();
        if (c == null) return;

        double exp = Math.min(1, r.nextDouble() * 2) * (e.getProduct().getPrice().getRealAmount() / 2);

        CorporationExperienceChangeEvent event = new CorporationExperienceChangeEvent(c, c.getExperience(), c.getExperience() + exp);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;
        c.setExperience(event.getNewExperience());
    }

}
