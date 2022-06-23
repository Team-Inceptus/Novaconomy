package us.teaminceptus.novaconomy.api;

import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
    CHINESE("_zh");

    private final String id;

    Language(String identifier) {
        this.id = identifier;
    }

    /**
     * Fetches the Identifier of this Language.
     * @return Identifier
     */
    public String getIdentifier() {
        return this.id.replace("_", "");
    }

    /**
     * Fetches the Message of this Language.
     * @param key Key to fetch
     * @return Message for this language
     */
    public String getMessage(String key) {
        if (key == null) return null;

        try {
            Properties p = new Properties();
            File f = new File(NovaConfig.getDataFolder(), "novaconomy" + id + ".properties");
            InputStream str = Files.newInputStream(f.toPath()); 
            p.load(str);
            str.close();

            return ChatColor.translateAlternateColorCodes('&', p.getProperty(key, "Unknown Value"));
        } catch (IOException e) {
            NovaConfig.getLogger().severe(e.getMessage());
            return null;
        }
    }

    /**
     * Fetches a Language by its identifier.
     * @param id ID to fetch (e.g. "es" for Spanish)
     * @return Found language, or English if not found
     */
    public static Language getById(String id) {
        if (id.equalsIgnoreCase("en")) return ENGLISH;

        for (Language l : values()) if (l.id.replace("_", "").equalsIgnoreCase(id)) return l;

        return ENGLISH;
    }

    /**
     * Fetches the current Language for Novaconomy.
     * @return Current Language
     */
    public static Language getCurrentLanguage() {
        return getById(NovaConfig.getConfiguration().getLanguage());
    }

}
