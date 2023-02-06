package us.teaminceptus.novaconomy.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.get;

@SuppressWarnings("unchecked")
public final class NovaUtil {

    private static final Map<SortingType<?>, String> SORTING_TYPE_NAMES = new HashMap<>();

    private static final NavigableMap<Integer, String> ROMAN_MAP = new TreeMap<Integer, String>() {{
        put(1000, "M");
        put(900, "CM");
        put(500, "D");
        put(400, "CD");
        put(100, "C");
        put(90, "XC");
        put(50, "L");
        put(40, "XL");
        put(10, "X");
        put(9, "IX");
        put(5, "V");
        put(4, "IV");
        put(1, "I");
    }};

    static {
        try {
            for (Field f : SortingType.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Modifier.isFinal(f.getModifiers())) continue;
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (!SortingType.class.isAssignableFrom(f.getType())) continue;

                SortingType<?> type = (SortingType<?>) f.get(null);
                SORTING_TYPE_NAMES.put(type, f.getName().toLowerCase().replaceFirst("_", "."));
            }
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }
    }

    private NovaUtil() {}

    public static String suffix(double number){
        if (number >= 1_000_000_000_000L) return String.format("%.2fT", number / 1_000_000_000_000L);
        if (number >= 1_000_000_000) return String.format("%.2fB", number / 1_000_000_000);
        if (number >= 1_000_000) return String.format("%.2fM", number / 1_000_000);
        if (number >= 1_000) return String.format("%.2fK", number / 1_000);

        return String.format("%,.2f", number);
    }

    public static void sync(Runnable r) {
        new BukkitRunnable() {
            @Override
            public void run() {
                r.run();
            }
        }.runTask(NovaConfig.getPlugin());
    }

    public static String getDisplayName(@NotNull SortingType<?> type) {
        return get("constants.sorting_types." + getId(type));
    }

    public static String getId(@NotNull SortingType<?> type) {
        return SORTING_TYPE_NAMES.get(type);
    }

    public static SortingType<?> byId(String id) {
        return SORTING_TYPE_NAMES.entrySet()
                .stream()
                .filter(e -> e.getValue().equalsIgnoreCase(id))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static <T> SortingType<T> byId(String id, Class<T> cast) {
        return (SortingType<T>) byId(id);
    }

    public static String toRoman(int number) {
        int i = ROMAN_MAP.floorKey(number);
        if (number == i) return ROMAN_MAP.get(number);

        return ROMAN_MAP.get(i) + toRoman(number - i);
    }


}
