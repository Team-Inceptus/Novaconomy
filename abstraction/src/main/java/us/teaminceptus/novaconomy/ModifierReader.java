package us.teaminceptus.novaconomy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ModifierReader {

    public static Map<String, Map<String, Set<Map<Economy, Double>>>> getAllModifiers() throws IllegalArgumentException {
        Map<String, Map<String, Set<Map<Economy, Double>>>> mods = new HashMap<>();
        FileConfiguration config = NovaConfig.getPlugin().getConfig();

        if (config.isConfigurationSection("NaturalCauses.Modifiers")) {
            ConfigurationSection modifiers = config.getConfigurationSection("NaturalCauses.Modifiers");

            modifiers.getKeys(false).forEach(s -> {
                ConfigurationSection modifier = modifiers.getConfigurationSection(s);
                Map<String, Set<Map<Economy, Double>>> map = new HashMap<>();

                modifier.getValues(false).forEach((k, v) -> {
                    Set<Map<Economy, Double>> value = new HashSet<>();
                    String amount = v.toString();

                    if (amount.contains("[") && amount.contains("]")) {
                        amount = amount.replaceAll("[\\[\\]]", "").replace(" ", "");
                        String[] amounts = amount.split(",");
                        for (String am : amounts) {
                            if (readString(am) == null)
                                throw new IllegalArgumentException("No valid amount found for \"" + k + ": " + amount + "\"");
                            value.add(readString(am));
                        }
                    } else {
                        if (readString(amount) == null)
                            throw new IllegalArgumentException("No valid amount found for \"" + k + ": " + amount + "\"");
                        value.add(readString(amount));
                    }

                    map.put(k.toUpperCase(), value);
                });

                mods.put(s, map);
            });
        }

        return mods;
    }

    public static Map<String, Set<Map<Economy, Double>>> getModifier(String mod) {
        return getAllModifiers().get(mod);
    }

    public static Map<Economy, Double> readString(String s) {
        try {
            char s1 = s.charAt(0);
            char s2 = s.charAt(s.length() - 1);

            if (!Economy.exists(s1) && !Economy.exists(s2)) return null;

            String remove = Economy.exists(s1) ? s1 + "" : s2 + "";
            Economy econ = Economy.exists(s1) ? Economy.getEconomy(s1) : Economy.getEconomy(s2);
            double amountD = Double.parseDouble(s.replaceAll("[" + remove + "]", ""));
            return Collections.singletonMap(econ, amountD);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toModString(Map<Economy, Double> map) {
        if (map == null) return null;
        if (map.isEmpty()) return null;

        Entry<Economy, Double> entry = map.entrySet().stream().findFirst().orElse(null);
        if (entry == null) return null;
        return entry.getValue() + "" + entry.getKey().getSymbol();
    }

    public static List<String> toModList(List<Map<Economy, Double>> list) {
        if (list == null) return null;
        if (list.isEmpty()) return null;
        return list.stream()
            .filter(Objects::nonNull)
            .map(ModifierReader::toModString).collect(Collectors.toList());
    }

}
