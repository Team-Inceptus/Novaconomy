package us.teaminceptus.novaconomy.api.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestPrice {

    @Test
    @DisplayName("Test Price")
    public void testPrice() {
        Price p1 = new Price(120);
        Price p2 = new Price(120);
        Assertions.assertEquals(p1, p2);
    }

    @Test
    @DisplayName("Test Price#getAmount & Price#setAmount")
    public void testPriceAmount() {
        Price p = new Price(120);
        Assertions.assertEquals(p.getAmount(), 120);

        p.setAmount(100);
        Assertions.assertEquals(p.getAmount(), 100);
    }

    @Test
    @DisplayName("Test Price Operations")
    public void testPriceOperations() {
        Price p1 = new Price(120);
        Price p2 = new Price(100);

        Assertions.assertEquals(new Price(p1.getAmount()).add(p2), new Price(220));
        Assertions.assertEquals(new Price(p1.getAmount()).remove(p2), new Price(20));
    }

    @Test
    @DisplayName("Test Price#compareTo")
    public void testPriceCompareTo() {
        Price p1 = new Price(120);
        Price p2 = new Price(100);

        Assertions.assertEquals(p1.compareTo(p2), 1);
        Assertions.assertEquals(p2.compareTo(p1), -1);
    }

}
