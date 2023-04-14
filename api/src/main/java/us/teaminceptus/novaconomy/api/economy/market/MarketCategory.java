package us.teaminceptus.novaconomy.api.economy.market;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableSet;

import us.teaminceptus.novaconomy.api.Language;

import static org.bukkit.Material.*;

/**
 * Represents a Market Category, holding a set of items or blocks
 */
public enum MarketCategory {

    // TODO Finish Categories

    /**
     * Consists of common crafting and building blocks/items
     */
    UTILITIES(STICK, GLOWSTONE_DUST, BOWL, BUCKET, ARROW, MILK_BUCKET, COMPASS,
        "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
        "warped_planks", "crimson_planks", "cherry_planks", "concrete", "powdered_concrete", "hardened_clay", 
        "terracotta", "recovery_compass", "clock"),

    /**
     * Consists of Natural mineral blocks
     */
    MINERALS(DIRT, GRAVEL, SAND, FLINT, STONE, COBBLESTONE, NETHERRACK, GLOWSTONE, SOUL_SAND,
        "red_sand", "coarse_dirt", "diorite", "granite", "andesite", "deepslate", "cobbled_deepslate",
        "polished_diorite", "polished_granite", "polished_andesite", "polished_deepslate", "calcite",
        "ender_stone", "end_stone", "mud", "soul_soil", "mycelium"),
    
    /**
     * Consists of common ores, ingots, nuggets, and redstone items
     */
    ORES_REDSTONE(COAL, IRON_INGOT, GOLD_NUGGET, GOLD_INGOT, REDSTONE, DIAMOND, EMERALD, MINECART,
        QUARTZ, QUARTZ_BLOCK,
        "copper_ingot", "lapis_lazuli", "iron_nugget", "netherite_scrap", "netherite_ingot",
        "lever", "repeater", "comparator"),

    /**
     * Consists of common outdoor decoration items and food
     */
    DECORATIONS_FOOD(GRASS, DEAD_BUSH, VINE, SUGAR_CANE, PUMPKIN, MELON, TORCH, GLASS, CARROT,
        POTATO, APPLE, BREAD, WHEAT, SEEDS, CACTUS, ICE, SNOW, PACKED_ICE, PRISMARINE, SEA_LANTERN,
        "tall_grass", "fern", "lily_pad", "large_fern", "lantern", "glass_pane", "glow_berries",
        "glow_lichen", "raw_beef", "beef", "porkchop", "chicken", "raw_chicken", "mutton", "campfire",
        "kelp", "peony", "sunflower", "poppy", "sea_pickle", "candle", "dark_prismarine", "shroomlight"),

    /**
     * Consists of common furniture and furniture utility blocks
     */
    FURNITURE(FURNACE, CHEST, ENDER_CHEST,
        "workbench", "crafting_table", "grindstone", "loom", "smithing_table", "fletching_table",
        "lectern", "lodestone"),

    /**
     * Consists of Mob Loot and Craftables from Loot
     */
    LOOT_CRAFTABLES(FEATHER, ROTTEN_FLESH, BONE, STRING, SLIME_BALL, SPIDER_EYE, GHAST_TEAR, 
        BLAZE_ROD, BLAZE_POWDER, ENDER_PEARL, EGG, LEATHER, PAPER, BOOK,
        "ender_eye", "eye_of_ender", "end_crystal", "ink_sac", "ink_sack", "gunpowder",
        "goat_horn", "nautilus_shell", "scute"),
    
    /**
     * Consists of items rarely found in the world
     */
    RARITIES(GOLDEN_APPLE, GOLDEN_CARROT)
    
    ;

    private final String[] itemNames;
    private final Material[] items;

    MarketCategory(Object... objs) {
        this.itemNames = Arrays.stream(objs).map(Object::toString).toArray(String[]::new);

        List<Material> items = new ArrayList<>();
        for (Object o : objs)
            if (o instanceof Material)
                items.add((Material) o);
            else if (o instanceof String)
                if (Material.matchMaterial(o.toString()) != null)
                    items.add(Material.matchMaterial(o.toString()));
            else
                throw new IllegalArgumentException("Invalid item type: " + o.getClass().getName());
    
        this.items = items.toArray(new Material[0]);
    }

    /**
     * Fetches an immutable set of all of the resolvable items in this MarketCategory
     * @return Immutable Set of all resolved items in this Category
     */
    @NotNull
    public Set<Material> getItems() {
        return ImmutableSet.copyOf(Arrays.stream(items)
                .sorted(Comparator.comparing(Material::name))
                .collect(Collectors.toSet())
        );
    }

    /**
     * Fetches an immutable set of all of the item names in this MarketCategory, whether they exist in the current Minecraft Version or not.
     * @return Immutable Set of all item names in this Category
     */
    @NotNull
    public Set<String> getItemNames() {
        return ImmutableSet.copyOf(Arrays.stream(itemNames)
                .sorted()
                .collect(Collectors.toSet())
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
