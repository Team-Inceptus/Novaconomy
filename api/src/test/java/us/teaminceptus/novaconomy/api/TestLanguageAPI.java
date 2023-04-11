package us.teaminceptus.novaconomy.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestLanguageAPI {

    @Test
    @DisplayName("Test Language")
    public void testLanguageFetcher() {
        for (Language l : Language.values()) {
            Assertions.assertNotNull(l.getLocale());
            Assertions.assertNotNull(l.getLocale().getDisplayName());
            Assertions.assertNotNull(l.getLocale().getLanguage());
        }
    }

}
