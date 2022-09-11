package us.teaminceptus.novaconomy.api.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.lang.reflect.Field;
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

        /***
         * Fetches the description annotation on this NovaSetting. If no description is present, null is returned.
         * @return {@link SettingDescription} annotation
         */
        @Nullable
        SettingDescription getDescription();

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
        @SettingDescription("settings.personal.notifications")
        NOTIFICATIONS("constants.settings.name.notifications", NovaConfig.getConfiguration()::hasNotifications),
        /**
         * Whether the player's balance is publicly visible
         */
        @SettingDescription("settings.personal.balance")
        PUBLIC_BALANCE("constants.settings.name.public_balance"),

        ;

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

        @Nullable
        @Override
        public SettingDescription getDescription() {
            try {
                Field f = Personal.class.getDeclaredField(name());
                return f.getAnnotation(SettingDescription.class);
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
                return null;
            }
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
        @SettingDescription("settings.business.home")
        PUBLIC_HOME("constants.settings.name.public_home"),
        /**
         * Whether the consumers of the Business can see the rating.
         */
        PUBLIC_RATING("constants.settings.name.public_rating"),
        /**
         * Whether this business is discoverable.
         */
        @SettingDescription("settings.business.discovery")
        PUBLIC_DISCOVERY("constants.settings.name.public_discovery"),
        /**
         * Whether the Business will automatically deposit 15% of its earnings into its advertising balance.
         */
        @SettingDescription("settings.business.deposit")
        AUTOMATIC_DEPOSIT("constants.settings.name.automatic_deposit", false),
        /**
         * Whether the Business allows advertising from other businesses.
         */
        @SettingDescription("settings.business.advertising")
        EXTERNAL_ADVERTISEMENT("constants.settings.name.advertisement")
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

        @Nullable
        @Override
        public SettingDescription getDescription() {
            try {
                Field f = Business.class.getDeclaredField(name());
                return f.getAnnotation(SettingDescription.class);
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
                return null;
            }
        }
    }

}
