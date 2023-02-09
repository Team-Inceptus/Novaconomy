package us.teaminceptus.novaconomy.api.corporation;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.Map;

/**
 * Utility class for Corporation Statistics
 */
public final class CorporationStatistics {
    
    private final Corporation c;

    CorporationStatistics(Corporation corp) {
        this.c = corp;
    }

    /**
     * Fetches all of the views this Corporation and its Children have received.
     * @return Total Views
     */
    public long getTotalViews() {
        return c.getChildren()
                .stream()
                .mapToLong(b -> b.getStatistics().getViews())
                .sum() + c.views;
    }

    /**
     * Fetches all of the product this Corporation's Children have sold.
     * @return Total Sales
     */
    public long getTotalSales() {
        return c.getChildren()
                .stream()
                .mapToLong(b -> b.getStatistics().getTotalSales())
                .sum();
    }

    /**
     * Fetches the complete amount of all of the money this Corporation has ever made, factoring an Economy's {@linkplain Economy#getConversionScale conversion scale}.
     * @return Total Profit
     */
    public double getTotalProfit() {
        return c.getChildren()
                .stream()
                .mapToDouble(b -> b.getStatistics().getProductSales()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().getPrice().getEconomy() != null)
                        .mapToDouble(e -> e.getKey().getPrice().getRealAmount() * e.getValue() * c.getProfitModifier())
                        .sum())
                .sum();
    }

    /**
     * Fetches the base amount of all of the money this Corporation has ever made.
     * @param econ Economy to use
     * @return Total Profit for the Economy
     */
    public double getProfit(@NotNull Economy econ) {
        return c.getChildren()
                .stream()
                .mapToDouble(b -> b.getStatistics().getProductSales()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().getPrice().getEconomy().equals(econ))
                        .mapToDouble(e -> e.getKey().getPrice().getAmount() * e.getValue() * c.getProfitModifier())
                        .sum())
                .sum();
    }

    /**
     * Fetches the total amount of products this Corporation has sold.
     * @return Total Products Sold
     */
    public long getTotalProductsSold() {
        return c.getChildren()
                .stream()
                .mapToLong(b -> b.getStatistics().getProductSales()
                        .entrySet()
                        .stream()
                        .mapToLong(Map.Entry::getValue)
                        .sum())
                .sum();
    }

}
