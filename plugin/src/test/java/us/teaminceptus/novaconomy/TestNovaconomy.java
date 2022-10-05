package us.teaminceptus.novaconomy;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.*;

public class TestNovaconomy {

    private static Novaconomy plugin;
    private static ServerMock server;

    @BeforeAll
    public static void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Novaconomy.class);
    }

    @AfterAll
    public static void tearDown() {
        MockBukkit.unload();
    }

    @Test
    @DisplayName("Test Configuration")
    public void testConfiguration() {
        plugin.setAdvertisingEnabled(true);
        Assertions.assertTrue(plugin.isAdvertisingEnabled());
        plugin.getLogger().info("Advertising: " + plugin.isAdvertisingEnabled());

        plugin.setEnchantBonus(false);
        Assertions.assertFalse(plugin.hasEnchantBonus());
        plugin.getLogger().info("Enchant Bonus: " + plugin.hasEnchantBonus());
    }

}
