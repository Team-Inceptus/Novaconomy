package us.teaminceptus.novaconomy.api;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Represents a Language supported by Novaconomy
 */
public enum Language {

    /**
     * Represents English (No Identifier)
     */
    ENGLISH(""),
    /**
     * Represents Spanish / Español
     */
    SPANISH("_es"),
    /**
     * Represents French / Français
     */
    FRENCH("_fr"),
    /**
     * Represents German / Deutsch
     */
    GERMAN("_de"),
    /**
     * Represents Portuguese / Português
     */
    PORTUGUESE("_pt"),
    /**
     * Represents Japanese / 日本
     */
    JAPANESE("_ja"),
    /**
     * Represents Chinese Simplified / 中国人
     */
    CHINESE("_zh"),
    /**
     * Represents Italian / Italiano
     */
    ITALIAN("_it"),
    /**
     * Represents Swedish / Svenska
     */
    SWEDISH("_sv"),
    /**
     * Represents Norwegain Bokmål / Norsk Bokmål
     */
    NORWEGIAN_BOKMAL("_nb"),
    /**
     * Represents Finnish / Suomi
     */
    FINNISH("_fi"),
    /**
     * Represents Korean / 한국어
     */
    KOREAN("_ko"),
    /**
     * Represents Indonesian / Indonesia
     */
    INDONESIAN("_id"),
    /**
     * Represents Russian / Русский
     */
    RUSSIAN("_ru"),

    ;

    private final String id;

    private final Locale locale;

    Language(String identifier) {
        this.id = identifier;

        final Locale l;
        switch (identifier.replace("_", "")) {
            case "": l = Locale.ENGLISH; break;
            case "fr": l = Locale.FRENCH; break;
            case "de": l = Locale.GERMAN; break;
            case "ja": l = Locale.JAPANESE; break;
            case "zh": l = Locale.CHINESE; break;
            case "it": l = Locale.ITALIAN; break;
            case "ko": l = Locale.KOREAN; break;
            default: l = new Locale(identifier.replace("_", ""));
        }

        this.locale = l;
    }

    /**
     * Fetches the Identifier of this Language.
     * @return Identifier
     */
    @NotNull
    public String getIdentifier() {
        if (this == ENGLISH) return "en";
        return this.id.replace("_", "");
    }

    /**
     * Fetches the Locale belonging to this Language.
     * @return Locale
     */
    @NotNull
    public Locale getLocale() {
        return locale;
    }

    /**
     * Fetches the Message of this Language.
     * @param key Key to fetch
     * @return Message for this language
     */
    public String getMessage(String key) {
        if (key == null) return null;

        Properties p = new Properties();
        try (InputStream str = NovaConfig.class.getResourceAsStream("/lang/novaconomy" + id + ".properties")) {
            p.load(str);
            str.close();
            return ChatColor.translateAlternateColorCodes('&', p.getProperty(key, "Unknown Value"));
        } catch (IOException e) {
            NovaConfig.print(e);
            return "Unknown Value";
        }
    }

    /**
     * Fetches a Language by its identifier.
     * @param id ID to fetch (e.g. "es" for Spanish)
     * @return Found language, or English if not found
     */
    @NotNull
    public static Language getById(@NotNull String id) {
        if (id.equalsIgnoreCase("en")) return ENGLISH;
        for (Language l : values()) if (l.id.replace("_", "").equalsIgnoreCase(id)) return l;

        return ENGLISH;
    }

    /**
     * Fetches the current Language for Novaconomy.
     * @return Current Language
     */
    @NotNull
    public static Language getCurrentLanguage() {
        return getById(NovaConfig.getConfiguration().getLanguage());
    }

    /**
     * Fetches a message in the current language.
     * @param key Key to fetch
     * @return Message in the current language
     */
    public static String getCurrentMessage(@NotNull String key) {
        return getCurrentLanguage().getMessage(key);
    }

    /**
     * Fetches the locale for the current language.
     * @return Current Locale
     */
    @NotNull
    public static Locale getCurrentLocale() {
        return getCurrentLanguage().getLocale();
    }

}
