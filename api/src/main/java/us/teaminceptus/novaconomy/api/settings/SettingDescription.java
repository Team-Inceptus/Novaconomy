package us.teaminceptus.novaconomy.api.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an in-game Setting Description
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SettingDescription {

    /**
     * Language Description key to be displayed in game.
     * @return Language Description key
     */
    String value();

}
