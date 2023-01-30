package us.teaminceptus.novaconomy.api;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Sorting Type for organizing a list of specific items.
 * @param <T> Type of parameter to compare
 */
public interface SortingType<T> extends Comparator<T> {

    /**
     * Sorts an item by its name in ascending order.
     */
    SortingType<String> NAME_AZ = String::compareTo;

    /**
     * Sorts an item by its name in descending order.
     */
    SortingType<String> NAME_ZA = NAME_AZ.reversed();

    // Amounts

    /**
     * Sorts an item by its amount in ascending order.
     */
    SortingType<Double> BIGGEST_ASCENDING = Double::compareTo;

    /**
     * Sorts an item by its amount in descending order.
     */
    SortingType<Double> BIGGEST_DESCENDING = BIGGEST_ASCENDING.reversed();

    @Override
    default boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortingType<?> that = (SortingType<?>) o;

        return Objects.equals(this, that);
    }

    @Override
    default SortingType<T> reversed() {
        return (o1, o2) -> compare(o2, o1);
    }

    /**
     * Fetches all SortingType values.
     * @return SortingType values
     */
    @NotNull
    static SortingType<?>[] values() {
        try {
            List<SortingType<?>> values = new ArrayList<>();

            for (Field f : SortingType.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Modifier.isFinal(f.getModifiers())) continue;
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (!SortingType.class.isAssignableFrom(f.getType())) continue;

                values.add((SortingType<?>) f.get(null));
            }

            return values.toArray(new SortingType[0]);
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }

        throw new RuntimeException("Failed to fetch SortingType values");
    }

}
