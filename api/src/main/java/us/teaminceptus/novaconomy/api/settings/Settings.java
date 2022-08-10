package us.teaminceptus.novaconomy.api.settings;

import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Represents a Setting Category or global Settings
 */
public final class Settings {

    private Settings() { throw new UnsupportedOperationException("Do not instantiate"); }

    /**
     * Interface all Setting Enum Categories Implement
     * @param <T> Setting Type
     */
    public interface NovaSetting<T> {

        /**
         * Fetches the default value of this setting.
         * @return default value of setting
         */
        @NotNull
        T getDefaultValue();

        /**
         * Fetches the display name of this setting in the current language.
         * @return display name of setting
         */
        @NotNull
        String getDisplayName();

        /**
         * Fetches the identifier of this setting.
         * @return identifier of setting
         */
        @NotNull
        String name();

    }

    /**
     * Creates an Array of all settings.
     * @return Array of all settings
     */
    public static NovaSetting<?>[] values() {
        List<NovaSetting<?>> settings = new ArrayList<>();

        settings.addAll(Arrays.asList(Business.values()));
        settings.addAll(Arrays.asList(Personal.values()));

        return settings.toArray(new NovaSetting[0]);
    }

    /**
     * Represents Personal Settings
     */
    public enum Personal implements NovaSetting<Boolean> {
        /**
         * Whether the Player will receive plugin notifications
         */
        NOTIFICATIONS("constants.settings.name.notifications", NovaConfig.getConfiguration()::hasNotifications);

        private final BooleanSupplier defaultValue;
        private final String dKey;

        Personal(String dKey, boolean defaultV) { this(dKey, () -> defaultV); }

        Personal(String dKey, BooleanSupplier defaultV) { this.dKey = dKey; this.defaultValue = defaultV; }

        Personal(String dKey) { this(dKey, true); }

        @Override
        public Boolean getDefaultValue() {
            return defaultValue.getAsBoolean();
        }

        @NotNull
        @Override
        public String getDisplayName() {
            String lang = NovaConfig.getConfiguration().getLanguage();
            return Language.getById(lang).getMessage(dKey);
        }
    }

    /**
     * Represents Business Settings
     */
    public enum Business implements NovaSetting<Boolean> {
        /**
         * Whether the owner of the Business is public.
         */
        PUBLIC_OWNER("constants.settings.name.public_owner"),
        /**
         * Whether the statistics of the Business are public.
         */
        PUBLIC_STATISTICS("constants.settings.name.public_stats"),
        /**
         * Whether consumers of the Business can teleport to its home.
         */
        PUBLIC_HOME("constants.settings.name.public_home"),
        /**
         * Whether the consumers of the Business can see the rating.
         */
        PUBLIC_RATING("constants.settings.name.public_rating"),
        /**
         * Whether this business is discoverable.
         */
        PUBLIC_DISCOVERY("constants.settings.name.public_discovery"),
        ;

        private final boolean defaultValue;
        private final String dKey;

        Business(String dKey, boolean defaultV) { this.dKey = dKey; this.defaultValue = defaultV; }

        Business(String dKey) { this(dKey, true); }

        @Override
        public Boolean getDefaultValue() {
            return defaultValue;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            String lang = NovaConfig.getConfiguration().getLanguage();
            return Language.getById(lang).getMessage(dKey);
        }
    }

}
