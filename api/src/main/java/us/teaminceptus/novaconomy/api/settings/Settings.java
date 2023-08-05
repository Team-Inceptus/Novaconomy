package us.teaminceptus.novaconomy.api.settings;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.teaminceptus.novaconomy.api.Language;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.corporation.Corporation.JoinType;
import us.teaminceptus.novaconomy.api.events.business.BusinessSupplyEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
    public static final class Business<T> implements NovaSetting<T> {
        /**
         * Whether the owner of the Business is public.
         */
        public static final Business<Boolean> PUBLIC_OWNER = ofBoolean("public_owner", "constants.settings.name.public_owner", true);
        /**
         * Whether the statistics of the Business are public.
         */
        public static final Business<Boolean> PUBLIC_STATISTICS = ofBoolean("public_statistics", "constants.settings.name.public_stats", true);
        /**
         * Whether consumers of the Business can teleport to its home.
         */
        @SettingDescription("settings.business.home")
        public static final Business<Boolean> PUBLIC_HOME = ofBoolean("public_home", "constants.settings.name.public_home", true);
        /**
         * Whether the consumers of the Business can see the rating.
         */
        @SettingDescription("settings.business.rating")
        public static final Business<Boolean> PUBLIC_RATING = ofBoolean("public_rating", "constants.settings.name.public_rating", true);
        /**
         * Whether this business is discoverable.
         */
        @SettingDescription("settings.business.discovery")
        public static final Business<Boolean> PUBLIC_DISCOVERY = ofBoolean("public_discovery", "constants.settings.name.public_discovery", true);
        /**
         * Whether the Business will automatically deposit 15% of its earnings into its advertising balance.
         */
        @SettingDescription("settings.business.deposit")
        public static final Business<Boolean> AUTOMATIC_DEPOSIT = ofBoolean("automatic_deposit", "constants.settings.name.automatic_deposit", false);
        /**
         * Whether the Business allows advertising from other businesses.
         */
        @SettingDescription("settings.business.advertising")
        public static final Business<Boolean> EXTERNAL_ADVERTISEMENT = ofBoolean("external_advertisement", "constants.settings.name.advertisement", NovaConfig.getConfiguration()::isAdvertisingEnabled);

        /**
         * The interval at which the Business will supply its stock from its supply locations.
         * @see BusinessSupplyEvent.Interval
         */
        @SettingDescription("settings.business.supply_interval")
        public static final Business<BusinessSupplyEvent.Interval> SUPPLY_INTERVAL = ofEnum("supply_interval","constants.settings.name.supply_interval", BusinessSupplyEvent.Interval.class, BusinessSupplyEvent.Interval.HOUR);

        private final String key;
        private final Supplier<T> defaultValue;
        private final String dKey;
        private final Set<T> possibleValues;
        private final Class<T> clazz;

        private Business(String key, String dKey, Class<T> clazz, Supplier<T> defaultValue, Set<T> possibleValues) {
            this.key = key;
            this.dKey = dKey;
            this.defaultValue = defaultValue;
            this.possibleValues = possibleValues;
            this.clazz = clazz;
        }

        private static <T extends Enum<T>> Business<T> ofEnum(String key, String dKey, Class<T> clazz, T defaultValue) {
            return new Business<>(key, dKey, clazz, () -> defaultValue, ImmutableSet.copyOf(defaultValue.getDeclaringClass().getEnumConstants()));
        }

        private static Business<Boolean> ofBoolean(String key, String dKey, boolean defaultValue) {
            return new Business<>(key, dKey, Boolean.class, () -> defaultValue, ImmutableSet.of(true, false));
        }

        private static Business<Boolean> ofBoolean(String key, String dKey, Supplier<Boolean> defaultValue) {
            return new Business<>(key, dKey, Boolean.class, defaultValue, ImmutableSet.of(true, false));
        }

        @Override
        public T getDefaultValue() {
            return defaultValue.get();
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
                Field f = Business.class.getDeclaredField(name());
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
        public static Business<?>[] values() {
            List<Business<?>> list = new ArrayList<>();

            try {
                for (Field f : Business.class.getDeclaredFields()) {
                    if (!Modifier.isPublic(f.getModifiers())) continue;
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!Business.class.isAssignableFrom(f.getType())) continue;

                    list.add((Business<?>) f.get(null));
                }
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
            }

            return list.toArray(new Business[0]);
        }

        /**
         * Fetches the Corporation Setting with the given {@linkplain #name name}.
         * @param name The name of the Corporation Setting.
         * @return Setting Found, or null if not found
         */
        @Nullable
        public static Business<?> valueOf(@Nullable String name) {
            if (name == null) return null;

            return Arrays.stream(values())
                    .filter(c -> c.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Fetches the Business Setting with the given {@linkplain #name name} and {@linkplain #getType type}.
         * @param <T> Type of the Setting
         * @param name The name of the Corporation Setting.
         * @param clazz The type of the Corporation Setting.
         * @return Setting Found, or null if not found
         */
        public static <T> Business<T> valueOf(@Nullable String name, @Nullable Class<T> clazz) {
            if (clazz == null) return null;

            return Arrays.stream(values())
                    .filter(b -> b.name().equalsIgnoreCase(name))
                    .filter(b -> clazz == null || clazz.isAssignableFrom(b.getType()))
                    .map(b -> (Business<T>) b)
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
            if (Boolean.class.isAssignableFrom(clazz) || Boolean.TYPE.isAssignableFrom(clazz))
                return (T) Boolean.valueOf(value);

            if (clazz.isEnum())
                // Will not compile without cast on Java 8
                return (T) Enum.valueOf(clazz.asSubclass(Enum.class), value.toUpperCase());

            throw new IllegalArgumentException("Unknown Business Setting: " + key);
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

        /**
         * Whether the Corporation has enabled corporation-only chat.
         */
        @SettingDescription("settings.corporation.chat")
        public static final Corporation<Boolean> CHAT = ofBoolean("chat", "constants.settings.name.chat", true);

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

        private static Corporation<Boolean> ofBoolean(String key, String dKey, boolean defaultValue) {
            return new Corporation<>(key, dKey, Boolean.class, defaultValue, ImmutableSet.of(true, false));
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
                    .filter(c -> clazz == null || clazz.isAssignableFrom(c.getType()))
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
                case "chat": return (T) Boolean.valueOf(value);
                default:
                    throw new IllegalArgumentException("Unknown Corporation Setting: " + key);
            }
        }

    }


}
