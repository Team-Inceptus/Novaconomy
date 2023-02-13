package us.teaminceptus.novaconomy.api.settings;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.corporation.Corporation.JoinType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Represents a Setting Category or global Settings
 */
@SuppressWarnings("unchecked")
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
         * Fetches all of the possible values for this setting.
         * @return possible values of setting
         */
        @NotNull
        Set<T> getPossibleValues();

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

        /**
         * Fetches the description annotation on this NovaSetting. If no description is present, null is returned.
         * @return {@link SettingDescription} annotation
         */
        @Nullable
        SettingDescription getDescription();

        /**
         * Fetches the type of setting this is.
         * @return type of setting
         */
        @NotNull
        Class<T> getType();

        /**
         * Parses a value from a string.
         * @param value String to parse
         * @return Parsed value applicable to this setting
         */
        @NotNull
        T parseValue(@NotNull String value);

    }

    /**
     * Creates an Array of all settings.
     * @return Array of all settings
     */
    @NotNull
    public static NovaSetting<?>[] values() {
        List<NovaSetting<?>> settings = new ArrayList<>();

        settings.addAll(Arrays.asList(Business.values()));
        settings.addAll(Arrays.asList(Personal.values()));
        settings.addAll(Arrays.asList(Corporation.values()));

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
        /**
         * Whether the player is rating businesses anonymously.
         */
        @SettingDescription("settings.personal.rating")
        ANONYMOUS_RATING("constants.settings.name.anonymous_rate", false),

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
            return Language.getCurrentMessage(dKey);
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

        @Override
        public Set<Boolean> getPossibleValues() {
            return ImmutableSet.of(true, false);
        }

        @Override
        public Class<Boolean> getType() { return Boolean.class; }

        @NotNull
        @Override
        public Boolean parseValue(@NotNull String value) {
            return Boolean.parseBoolean(value);
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
        @SettingDescription("settings.business.rating")
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
        EXTERNAL_ADVERTISEMENT("constants.settings.name.advertisement", NovaConfig.getConfiguration()::isAdvertisingEnabled)
        ;

        private final BooleanSupplier defaultValue;
        private final String dKey;

        Business(String dKey, boolean defaultV) { this(dKey, () -> defaultV); }

        Business(String dKey) { this(dKey, true); }

        Business(String dKey, BooleanSupplier defaultV) { this.dKey = dKey; this.defaultValue = defaultV; }

        @Override
        public Boolean getDefaultValue() {
            return defaultValue.getAsBoolean();
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return Language.getCurrentMessage(dKey);
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

        @Override
        public Set<Boolean> getPossibleValues() {
            return ImmutableSet.of(true, false);
        }

        @Override
        public Class<Boolean> getType() { return Boolean.class; }

        @NotNull
        @Override
        public Boolean parseValue(@NotNull String value) {
            return Boolean.parseBoolean(value);
        }
    }

    /**
     * Represents Corporation Settings
     */
    public static final class Corporation<T> implements NovaSetting<T> {

        /**
         * The privacy setting of a Corporation, determining how and whether businesses can join.
         */
        @SettingDescription("settings.corporation.join_type")
        public static final Corporation<JoinType> JOIN_TYPE = ofEnum("join_type", "constants.settings.name.join_type", JoinType.class, JoinType.INVITE_ONLY);        

        private final String key;
        private final T defaultValue;
        private final String dKey;
        private final Set<T> possibleValues;
        private final Class<T> clazz;

        private Corporation(String key, String dKey, Class<T> clazz, T defaultValue, Set<T> possibleValues) {
            this.key = key;
            this.dKey = dKey;
            this.defaultValue = defaultValue;
            this.possibleValues = possibleValues;
            this.clazz = clazz;
        }

        private static <T extends Enum<T>> Corporation<T> ofEnum(String key, String dKey, Class<T> clazz, T defaultValue) {
            return new Corporation<>(key, dKey, clazz, defaultValue, ImmutableSet.copyOf(defaultValue.getDeclaringClass().getEnumConstants()));
        }

        @Override
        public T getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String name() {
            return key.toUpperCase();
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return Language.getCurrentMessage(dKey);
        }

        @Nullable
        @Override
        public SettingDescription getDescription() {
            try {
                Field f = Corporation.class.getDeclaredField(name());
                return f.getAnnotation(SettingDescription.class);
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
                return null;
            }
        }

        @Override
        public Set<T> getPossibleValues() {
            return possibleValues;
        }

        /**
         * Fetches an array of all of the Corporation Settings.
         * @return An array of all of the Corporation Settings.
         */
        @NotNull
        public static Corporation<?>[] values() {
            List<Corporation<?>> list = new ArrayList<>();

            try {
                for (Field f : Corporation.class.getDeclaredFields()) {
                    if (!Modifier.isPublic(f.getModifiers())) continue;
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!Corporation.class.isAssignableFrom(f.getType())) continue;

                    list.add((Corporation<?>) f.get(null));
                }
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
            }

            return list.toArray(new Corporation[0]);
        }

        /**
         * Fetches the Corporation Setting with the given {@linkplain #name name}.
         * @param name The name of the Corporation Setting.
         * @return Setting Found, or null if not found
         */
        @Nullable
        public static Corporation<?> valueOf(@Nullable String name) {
            if (name == null) return null;

            return Arrays.stream(values())
                    .filter(c -> c.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

        }

        /**
         * Fetches the Corporation Setting with the given {@linkplain #name name} and {@linkplain #getType type}.
         * @param <T> Type of the Setting
         * @param name The name of the Corporation Setting.
         * @param clazz The type of the Corporation Setting.
         * @return Setting Found, or null if not found
         */
        public static <T> Corporation<T> valueOf(@Nullable String name, @Nullable Class<T> clazz) {
            if (clazz == null) return null;

            return Arrays.stream(values())
                    .filter(c -> c.name().equalsIgnoreCase(name))
                    .filter(c -> clazz.isAssignableFrom(c.getType()))
                    .map(c -> (Corporation<T>) c)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Class<T> getType() {
            return clazz;
        }

        @NotNull
        @Override
        public T parseValue(@NotNull String value) {
            switch (key) {
                case "join_type": return (T) JoinType.valueOf(value.toUpperCase());
                default:
                    throw new IllegalArgumentException("Unknown Corporation Setting: " + key);
            }
        }

    }


}
