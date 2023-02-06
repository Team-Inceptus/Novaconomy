package us.teaminceptus.novaconomy.api;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.api.util.BusinessProduct;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
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

    // Business Products

    /**
     * Sorts a product by its name in ascending order.
     */
    SortingType<BusinessProduct> PRODUCT_NAME_ASCENDING = (p1, p2) -> p1.getItem().getType().name().compareTo(p1.getItem().getType().name());

    /**
     * Sorts a product by its name in descending order.
     */
    SortingType<BusinessProduct> PRODUCT_NAME_DESCENDING = PRODUCT_NAME_ASCENDING.reversed();

    /**
     * Sorts a product by its price in ascending order.
     */
    SortingType<BusinessProduct> PRODUCT_PRICE_ASCENDING = (p1, p2) -> p2.getPrice().compareTo(p1.getPrice());

    /**
     * Sorts a product by its price in descending order.
     */
    SortingType<BusinessProduct> PRODUCT_PRICE_DESCENDING = PRODUCT_PRICE_ASCENDING.reversed();

    /**
     * Sorts a business product by its stock in ascending order.
     */
    SortingType<BusinessProduct> PRODUCT_STOCK_ASCENDING = (p1, p2) -> {
        Business b = p1.getBusiness();
        return Integer.compare(b.getTotalStock(p1.getItem()), b.getTotalStock(p2.getItem()));
    };

    /**
     * Sorts a business product by its stock in descending order.
     */
    SortingType<BusinessProduct> PRODUCT_STOCK_DESCENDING = PRODUCT_STOCK_ASCENDING.reversed();

    @Override
    default SortingType<T> reversed() {
        return (o1, o2) -> compare(o2, o1);
    }

    @Override
    default SortingType<T> thenComparing(Comparator<? super T> other) {
        return (o1, o2) -> {
            int result = compare(o1, o2);
            return result != 0 ? result : other.compare(o1, o2);
        };
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

    /**
     * Fetches all SortingType values of a specific type.
     * @param clazz Type of SortingType
     * @return SortingType values
     * @param <T> Type of SortingType
     */
    @NotNull
    @SuppressWarnings("unchecked")
    static <T> SortingType<T>[] values(@NotNull Class<T> clazz) {
        try {
            List<SortingType<T>> values = new ArrayList<>();

            for (Field f : SortingType.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Modifier.isFinal(f.getModifiers())) continue;
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (!SortingType.class.isAssignableFrom(f.getType())) continue;

                ParameterizedType type = (ParameterizedType) f.getGenericType();
                Class<?> fClass = (Class<?>) type.getActualTypeArguments()[0];
                if (clazz.isAssignableFrom(fClass))
                    values.add((SortingType<T>) f.get(null));
            }

            return values.toArray(new SortingType[0]);
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }

        throw new RuntimeException("Failed to fetch SortingType values");
    }

}
