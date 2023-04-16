package us.teaminceptus.novaconomy.api.economy.market;

import com.google.common.collect.ImmutableList;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.Language;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Material.*;

/**
 * Represents a Market Category, holding a set of items or blocks
 */
public enum MarketCategory {

    /**
     * Consists of common crafting and building blocks/items
     */
    UTILITIES(STICK, GLOWSTONE_DUST, BOWL, BUCKET, ARROW, MILK_BUCKET, COMPASS, PAPER, BOOK,
            "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
            "warped_planks", "crimson_planks", "cherry_planks", "concrete", "powdered_concrete", "hardened_clay",
            "terracotta", "recovery_compass", "clock", "brush", "shears", "flint_and_steel"
    ),

    /**
     * Consists of Natural mineral blocks
     */
    MINERALS(DIRT, GRAVEL, SAND, FLINT, STONE, COBBLESTONE, NETHERRACK, GLOWSTONE, SOUL_SAND,
            "red_sand", "coarse_dirt", "diorite", "granite", "andesite", "deepslate", "cobbled_deepslate",
            "calcite", "ender_stone", "end_stone", "mud", "soul_soil", "blackstone", "basalt", "obsidian",
            "tuff"
    ),
    
    /**
     * Consists of common ores, ingots, and nuggets
     */
    ORES(COAL, IRON_INGOT, GOLD_NUGGET, GOLD_INGOT, REDSTONE, DIAMOND, EMERALD, QUARTZ,
            "copper_ingot", "lapis_lazuli", "iron_nugget", "netherite_scrap", "netherite_ingot"
    ),

    /**
     * Consists of common outdoor decoration items and food
     */
    DECORATIONS_FOOD(GRASS, DEAD_BUSH, VINE, SUGAR_CANE, PUMPKIN, TORCH, GLASS, CARROT,
            POTATO, APPLE, BREAD, WHEAT, SEEDS, CACTUS, ICE, SNOW, PACKED_ICE, PRISMARINE, SEA_LANTERN,
            "tall_grass", "fern", "lily_pad", "large_fern", "lantern", "glass_pane", "glow_berries",
            "glow_lichen", "raw_beef", "beef", "porkchop", "chicken", "raw_chicken", "mutton", "campfire",
            "kelp", "peony", "sunflower", "poppy", "sea_pickle", "candle", "dark_prismarine", "shroomlight",
            "mycelium", "podzol", "dripleaf", "small_dripleaf", "soul_lantern", "sweet_berries", "soul_campfire"
    ),

    /**
     * Consists of common furniture and furniture utility blocks
     */
    FURNITURE(FURNACE, CHEST, ENDER_CHEST, NOTE_BLOCK, JUKEBOX,
            "workbench", "crafting_table", "grindstone", "loom", "smithing_table", "fletching_table",
            "lectern", "lodestone", "barrel"
    ),

    /**
     * Consists of Mob Loot items
     */
    LOOT(FEATHER, ROTTEN_FLESH, BONE, STRING, SLIME_BALL, SPIDER_EYE, GHAST_TEAR,
            BLAZE_ROD, BLAZE_POWDER, ENDER_PEARL, EGG, LEATHER, PRISMARINE_SHARD,
            PRISMARINE_CRYSTALS, SPONGE,
            "ender_eye", "eye_of_ender", "end_crystal", "ink_sac", "ink_sack", "gunpowder",
            "goat_horn", "nautilus_shell", "scute"
    ),
    
    /**
     * Consists of items rarely found in the world
     */
    RARITIES(GOLDEN_APPLE, GOLDEN_CARROT,
            "enchanted_golden_apple", "creeper_banner_pattern", "skull_banner_pattern",
            "globe_banner_pattern", "piglin_banner_pattern", "wither_skeleton_skull",
            "skeleton_skull", "creeper_head"
    )
    
    ;

    private final String[] itemNames;
    private final Material[] items;

    MarketCategory(Object... objs) {
        this.itemNames = Arrays.stream(objs).map(Object::toString).map(String::toLowerCase).toArray(String[]::new);

        Set<Material> items = new HashSet<>();
        for (Object o : objs)
            if (o instanceof Material)
                items.add((Material) o);
            else if (o instanceof String) {
                if (Material.matchMaterial(o.toString()) != null)
                    items.add(Material.matchMaterial(o.toString()));
            }
            else
                throw new IllegalArgumentException("Invalid item type: " + o.getClass().getName());
    
        this.items = items.toArray(new Material[0]);
    }

    /**
     * Fetches an immutable list of all of the resolvable items in this MarketCategory
     * @return Immutable List of all resolved items in this Category
     */
    @NotNull
    public List<Material> getItems() {
        return ImmutableList.copyOf(Arrays.stream(items)
                .sorted(Comparator.comparing(Material::name))
                .collect(Collectors.toList())
        );
    }

    /**
     * Fetches an immutable list of all of the item names in this MarketCategory, whether they exist in the current Minecraft Version or not.
     * @return Immutable List of all item names in this Category
     */
    @NotNull
    public List<String> getItemNames() {
        return ImmutableList.copyOf(Arrays.stream(itemNames)
                .sorted()
                .collect(Collectors.toList())
        );
    }

    /**
     * Fetches the localized name of this MarketCategory
     * @return Localized Name
     */
    @NotNull
    public String getLocalizedName() {
        return Language.getCurrentMessage("market.category." + name().toLowerCase());
    }
    
}
