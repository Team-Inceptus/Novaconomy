package us.teaminceptus.novaconomy.util;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.SortingType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static us.teaminceptus.novaconomy.abstraction.Wrapper.USER_AGENT;
import static us.teaminceptus.novaconomy.messages.MessageHandler.format;
import static us.teaminceptus.novaconomy.messages.MessageHandler.get;
import static us.teaminceptus.novaconomy.scheduler.NovaScheduler.scheduler;

@SuppressWarnings("unchecked")
public final class NovaUtil {

    private static final Map<SortingType<?>, String> SORTING_TYPE_NAMES = new HashMap<>();

    @SuppressWarnings("serial")
    private static final NavigableMap<Integer, String> ROMAN_MAP = new TreeMap<Integer, String>() {
        {
            put(1000, "M");
            put(900, "CM");
            put(500, "D");
            put(400, "CD");
            put(100, "C");
            put(90, "XC");
            put(50, "L");
            put(40, "XL");
            put(10, "X");
            put(9, "IX");
            put(5, "V");
            put(4, "IV");
            put(1, "I");
        }
    };

    static {
        try {
            for (Field f : SortingType.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Modifier.isFinal(f.getModifiers())) continue;
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (!SortingType.class.isAssignableFrom(f.getType())) continue;

                SortingType<?> type = (SortingType<?>) f.get(null);
                SORTING_TYPE_NAMES.put(type, f.getName().toLowerCase().replaceFirst("_", "."));
            }
        } catch (ReflectiveOperationException e) {
            NovaConfig.print(e);
        }
    }

    private NovaUtil() {}

    public static void sync(Runnable r) {
        scheduler.sync(r);
    }

    public static void async(Runnable r) {
        scheduler.async(r);
    }

    public static String getDisplayName(@NotNull SortingType<?> type) {
        return get("constants.sorting_types." + getId(type));
    }

    public static String getId(@NotNull SortingType<?> type) {
        return SORTING_TYPE_NAMES.get(type);
    }

    public static SortingType<?> byId(String id) {
        return SORTING_TYPE_NAMES.entrySet()
                .stream()
                .filter(e -> e.getValue().equalsIgnoreCase(id))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public static <T> SortingType<T> byId(String id, Class<T> cast) {
        return (SortingType<T>) byId(id);
    }

    public static String toRoman(int number) {
        int i = ROMAN_MAP.floorKey(number);
        if (number == i) return ROMAN_MAP.get(number);

        return ROMAN_MAP.get(i) + toRoman(number - i);
    }

    public static String formatTimeAgo(OfflinePlayer p, long start) {
        long time = System.currentTimeMillis();
        long diff = time - start;
    
        long seconds = diff / 1000;
    
        if (seconds < 2) return get(p, "constants.time.ago.milli_ago");
        if (seconds >= 2 && seconds < 60) return format(p, get(p, "constants.time.ago.seconds_ago"), format(p, "%,d", seconds));
    
        long minutes = seconds / 60;
        if (minutes < 60) return format(p, get(p, "constants.time.ago.minutes_ago"), format(p, "%,d", minutes));
    
        long hours = minutes / 60;
        if (hours < 24) return format(p, get(p, "constants.time.ago.hours_ago"), format(p, "%,d", hours));
    
        long days = hours / 24;
        if (days < 7) return format(p, get(p, "constants.time.ago.days_ago"), format(p, "%,d", days));
    
        long weeks = days / 7;
        if (weeks < 4) return format(p, get(p, "constants.time.ago.weeks_ago"), format(p, "%,d", weeks));
    
        long months = weeks / 4;
        if (months < 12) return format(p, get(p, "constants.time.ago.months_ago"), format(p, "%,d", months));
    
        long years = months / 12;
        return format(p, get(p, "constants.time.ago.years_ago"), format(p, "%,d", years));
    }


    public static String capitalize(@NotNull String str) {
        if (str.isEmpty()) return str;

        String[] words = str.split("\\s");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase());
            sb.append(word.substring(1).toLowerCase());
            sb.append(" ");
        }

        return sb.toString().trim();
    }

    private static final char[] SUFFIXES = "KMBTQEXSON".toCharArray();

    public static String suffix(double num) {
        if (num < 0) return "-" + suffix(-num);
        if (num < 1000) return format("%,.2f", num);

        int index = (int) (Math.log10(num) / 3);
        String suffix = SUFFIXES[index - 1] + "";

        return format("%.2f%s", num / Math.pow(1000, index), suffix);
    }

    public static UUID untrimUUID(String old) {
        String p1 = old.substring(0, 8);
        String p2 = old.substring(8, 12);
        String p3 = old.substring(12, 16);
        String p4 = old.substring(16, 20);
        String p5 = old.substring(20, 32);

        String newUUID = p1 + "-" + p2 + "-" + p3 + "-" + p4 + "-" + p5;

        return UUID.fromString(newUUID);
    }

    public static OfflinePlayer getPlayer(String name) {
        if (Bukkit.getPlayer(name) != null) return Bukkit.getPlayer(name);

        if (Bukkit.getOnlineMode()) try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String inputLine;
                while ((inputLine = input.readLine()) != null) builder.append(inputLine);

                Gson g = new Gson();
                return Bukkit.getOfflinePlayer(untrimUUID(g.fromJson(builder.toString(), APIPlayer.class).id));
            }

        } catch (IOException e) {
            NovaConfig.print(e);
        } catch (Exception e) {
            return null;
        }
        else return Bukkit.getPlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
        return null;
    }

     static class APIPlayer {

        public final String name;
        public final String id;

        public APIPlayer(String name, String id) {
            this.name = name;
            this.id = id;
        }

    }
}
