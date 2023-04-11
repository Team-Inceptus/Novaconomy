package us.teaminceptus.novaconomy.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestLanguagePlugin {

    @Test
    @DisplayName("Test Langauge#getMessage")
    public void testLanguageMessage() {
        for (Language l : Language.values()) {
            Assertions.assertNotNull(l.getMessage("plugin.prefix"));
            Assertions.assertNotEquals(l.getMessage("plugin.prefix"), "Unknown Value");
            Assertions.assertEquals(l.getMessage("unknownkey"), "Unknown Value");
        }
    }

}
