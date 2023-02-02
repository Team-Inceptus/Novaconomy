package us.teaminceptus.novaconomy.api;

import org.jetbrains.annotations.NotNull;

import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a Sorting Type for organizing a list of specific items.
 * @param <T> Type of parameter to compare
 */
@FunctionalInterface
public interface SortingType<T> extends Comparator<T> {
   
    // Economies

    /**
     * Sorts an economy by its name in ascending order.
     */
    SortingType<Economy> ECONOMY_NAME_ASCENDING = (e1, e2) -> e1.getName().compareTo(e2.getName());

    /**
     * Sorts an economy by its name in descending order.
     */
    SortingType<Economy> ECONOMY_NAME_DESCENDING = ECONOMY_NAME_ASCENDING.reversed();

    /**
     * Sorts an economy by whether it has natural causes.
     */
    SortingType<Economy> ECONOMY_NATURAL_CAUSES = (e1, e2) -> Boolean.compare(e1.hasNaturalIncrease(), e2.hasNaturalIncrease());


    // Business

    /**
     * Sorts a business by its name in ascending order.
     */
    SortingType<Business> BUSINESS_NAME_ASCENDING = (b1, b2) -> b1.getName().compareTo(b2.getName());

    /**
     * Sorts a business by its name in descending order.
     */
    SortingType<Business> BUSINESS_NAME_DESCENDING = BUSINESS_NAME_ASCENDING.reversed();

    /**
     * Sorts a business by its creation date in ascending order.
     */
    SortingType<Business> BUSINESS_CREATION_DATE_ASCENDING = (b1, b2) -> b1.getCreationDate().compareTo(b2.getCreationDate());

    /**
     * Sorts a business by its creation date in descending order.
     */
    SortingType<Business> BUSINESS_CREATION_DATE_DESCENDING = BUSINESS_CREATION_DATE_ASCENDING.reversed();



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
