package us.teaminceptus.novaconomy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import us.teaminceptus.novaconomy.api.economy.market.MarketCategory;

public class TestNovaconomy {
    
    @Test
    @DisplayName("Test Novaconomy#prices")
    public void testMarketPrices() {
         for (MarketCategory c : MarketCategory.values())
             for (String s : c.getItemNames())
                Assertions.assertTrue(Novaconomy.prices.containsKey(s), "Missing items in " + c.name() + ": Missing '" + s + "'");
    }

}
