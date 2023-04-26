package us.teaminceptus.novaconomy.util.inventory;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import us.teaminceptus.novaconomy.abstraction.CommandWrapper;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.util.NovaUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.TYPE_TAG;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.get;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.w;
import static us.teaminceptus.novaconomy.util.NovaUtil.format;

public final class Items {

    // Pre-Builds

    public static final ItemStack ARROW = builder(Material.PAPER, meta -> meta.setDisplayName(ChatColor.YELLOW + "->"));

    public static final ItemStack COMING_SOON = builder(Material.BEDROCK, meta -> meta.setDisplayName(ChatColor.DARK_PURPLE + get("constants.coming_soon")));

    public static final ItemStack COMMAND_BLOCK = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("COMMAND_BLOCK")),
            () -> new ItemStack(Material.matchMaterial("COMMAND"))
    );

    public static final ItemStack LIME_WOOL = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("LIME_WOOL")),
            () -> new ItemStack(Material.matchMaterial("WOOL"), 1, (short) 5)
    );

    public static final ItemStack RED_WOOL = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("RED_WOOL")),
            () -> new ItemStack(Material.matchMaterial("WOOL"), 1, (short) 14)
    );

    public static final ItemStack LIME_STAINED_GLASS_PANE = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("LIME_STAINED_GLASS_PANE")),
            () -> new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 5)
    );

    public static final ItemStack RED_STAINED_GLASS_PANE = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("RED_STAINED_GLASS_PANE")),
            () -> new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 14)
    );

    public static final ItemStack YELLOW_STAINED_GLASS_PANE = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("YELLOW_STAINED_GLASS_PANE")),
            () -> new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 4)
    );

    public static final ItemStack CANCEL = NBTWrapper.builder(RED_WOOL,
            meta -> meta.setDisplayName(ChatColor.RED + get("constants.cancel")),
            nbt -> nbt.setID("no:close")
    );

    public static final ItemStack GUI_BACKGROUND = builder(
            checkLegacy(
                    () -> new ItemStack(Material.matchMaterial("BLACK_STAINED_GLASS_PANE")),
                    () -> new ItemStack(Material.matchMaterial("STAINED_GLASS_PANE"), 1, (short) 15)
            ), meta -> meta.setDisplayName(" ")
    );

    public static final ItemStack CONFIRM = builder(Material.BEACON,
            meta -> meta.setDisplayName(get("constants.confirm"))
    );

    public static final ItemStack BACK = builder(head("arrow_left_gray"),
            meta -> meta.setDisplayName(ChatColor.RED + get("constants.back"))
    );

    public static final ItemStack NEXT = builder(head("arrow_right"),
            meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.next"))
    );

    public static final ItemStack PREVIOUS = builder(head("arrow_left"),
            meta -> meta.setDisplayName(ChatColor.AQUA + get("constants.prev"))
    );

    public static final ItemStack LOADING = builder(head("loading"),
            meta -> meta.setDisplayName(ChatColor.DARK_RED + get("constants.loading"))
    );

    public static final ItemStack CYAN_WOOL = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("CYAN_WOOL")),
            () -> new ItemStack(Material.matchMaterial("WOOL"), 1, (short) 6)
    );

    public static final ItemStack OAK_SIGN = checkLegacy(
            () -> {
                Material m;
                try {
                    m = Material.matchMaterial("OAK_SIGN");
                } catch (IllegalArgumentException e) {
                    m = Material.matchMaterial("SIGN");
                }
                return new ItemStack(m);
            },
            () -> new ItemStack(Material.matchMaterial("SIGN"))
    );

    public static final ItemStack YELLOW_TERRACOTTA = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("YELLOW_TERRACOTTA")),
            () -> new ItemStack(Material.matchMaterial("STAINED_CLAY"), 1, (short) 4)
    );

    public static final ItemStack LOCKED = builder(Material.BEDROCK,
            meta -> meta.setDisplayName(ChatColor.DARK_PURPLE + get("constants.locked"))
    );

    public static final ItemStack LIME_DYE = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("LIME_DYE")),
            () -> new ItemStack(Material.matchMaterial("INK_SACK"), 1, (short) 10)
    );

    public static final ItemStack GRAY_DYE = checkLegacy(
            () -> new ItemStack(Material.matchMaterial("GRAY_DYE")),
            () -> new ItemStack(Material.matchMaterial("INK_SACK"), 1, (short) 8)
    );

    // Static Util

    public static ItemStack sorter(SortingType<?> sortingType) {
        return NBTWrapper.builder(YELLOW_TERRACOTTA,
                meta -> meta.setDisplayName(ChatColor.GREEN + NovaUtil.format(get("constants.sorting_by"), ChatColor.YELLOW + NovaUtil.getDisplayName(sortingType))),
                nbt -> {
                    nbt.setID("sorter");
                    nbt.set(TYPE_TAG, NovaUtil.getId(sortingType));
                }
        );

    }

    public static ItemStack head(String name) {
        Properties p = new Properties();
        try {
            p.load(NovaConfig.getPlugin().getClass().getResourceAsStream("/util/heads.properties"));

            String texture = p.getProperty(name);
            ItemStack head = new ItemStack(w.isLegacy() ? Material.matchMaterial("SKULL_ITEM") : Material.matchMaterial("PLAYER_HEAD"));

            SkullMeta meta = (SkullMeta) head.getItemMeta();
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            try {
                Method mtd = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                mtd.setAccessible(true);
                mtd.invoke(meta, profile);
            } catch (Exception e) {
                NovaConfig.print(e);
            }
            head.setItemMeta(meta);

            return head;
        } catch (IOException e) {
            NovaConfig.print(e);
            return null;
        }
    }


    public static ItemStack yes(String type) {
        return NBTWrapper.builder(LIME_WOOL,
                meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.yes")),
                nbt -> nbt.setID("yes:" + type)
        );
    }

    public static ItemStack yes(String type, Consumer<NBTWrapper> nbt) {
        return NBTWrapper.builder(yes(type), nbt);
    }

    public static ItemStack cancel(String type) {
        return NBTWrapper.builder(RED_WOOL,
                meta -> meta.setDisplayName(ChatColor.RED + get("constants.cancel")),
                nbt -> nbt.setID("no:" + type)
        );
    }

    public static ItemStack cancel(String type, Consumer<NBTWrapper> nbt) {
        return NBTWrapper.builder(cancel(type), nbt);
    }

    public static ItemStack next(String type) {
        return NBTWrapper.builder(NEXT,
                nbt -> nbt.setID("next:" + type)
        );
    }

    public static ItemStack prev(String type) {
        return NBTWrapper.builder(PREVIOUS,
                nbt -> nbt.setID("prev:" + type)
        );
    }

    public static ItemStack invalid(String arg0) {
        return builder(Material.BARRIER,
                meta -> meta.setDisplayName(format(get("error.economy.invalid_amount"), arg0))
        );
    }

    public static ItemStack checkLegacy(Supplier<ItemStack> contemporary, Supplier<ItemStack> legacy) {
        return w.isLegacy() ? legacy.get() : contemporary.get();
    }

    public static ItemStack economyWheel() {
        return economyWheel(null);
    }

    public static ItemStack economyWheel(String suffix) {
        Economy econ = Economy.getEconomies()
                .stream()
                .sorted(Economy::compareTo)
                .collect(Collectors.toList())
                .get(0);
                
        return economyWheel(suffix, econ);
    }

    public static ItemStack economyWheel(String suffix, Economy econ) {
        ItemStack economyWheel = NBTWrapper.builder(econ.getIconType(),
        meta -> meta.setDisplayName(ChatColor.GOLD + econ.getName()),
        nbt -> {
                nbt.set(CommandWrapper.ECON_TAG, econ.getUniqueId());
                nbt.setID("economy:wheel" + (suffix == null ? "" : ":" + suffix));
        });
        
        Generator.modelData(economyWheel, econ.getCustomModelData());
        return economyWheel;
    }

    @NotNull
    public static ItemStack builder(ItemStack item, Consumer<ItemMeta> metaC) {
        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        metaC.accept(meta);
        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    public static ItemStack builder(Material m, int amount, Consumer<ItemMeta> metaC) {
        return builder(new ItemStack(m, amount), metaC);
    }

    @NotNull
    public static ItemStack builder(Material m, Consumer<ItemMeta> metaC) {
        return builder(m, 1, metaC);
    }

    public static ItemStack createPlayerHead(@Nullable OfflinePlayer p) {
        return NBTWrapper.builder(w.isLegacy() ? Material.matchMaterial("SKULL_ITEM") : Material.matchMaterial("PLAYER_HEAD"),
                meta -> ((SkullMeta) meta).setOwner(p == null ? "" : p.getName()),
                nbt -> {
                    if (p != null) {
                        nbt.setID("player_stats");
                        nbt.set("player", p.getUniqueId());
                    }
                }
        );
    }
}
