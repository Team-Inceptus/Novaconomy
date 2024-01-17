package us.teaminceptus.novaconomy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;
import us.teaminceptus.novaconomy.api.NovaConfig;
import us.teaminceptus.novaconomy.api.economy.Economy;
import us.teaminceptus.novaconomy.util.NovaUtil;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.messages.MessageHandler.format;

public final class ModifierReader {

    private ModifierReader() {}

    public static final Map<String, Map<String, Set<Entry<Economy, Double>>>> LOADED_MODIFIERS = new HashMap<>();

    public static Map<String, Map<String, Set<Entry<Economy, Double>>>> getAllModifiers() throws IllegalArgumentException {
        if (!LOADED_MODIFIERS.isEmpty()) return LOADED_MODIFIERS;

        Map<String, Map<String, Set<Entry<Economy, Double>>>> mods = new HashMap<>();
        FileConfiguration config = NovaConfig.getPlugin().getConfig();

        if (config.isConfigurationSection("NaturalCauses.Modifiers")) {
            ConfigurationSection modifiers = config.getConfigurationSection("NaturalCauses.Modifiers");

            modifiers.getKeys(false).forEach(s -> {
                if (s.equalsIgnoreCase("Death")) return;
                ConfigurationSection modifier = modifiers.getConfigurationSection(s);
                Map<String, Set<Entry<Economy, Double>>> map = new HashMap<>();

                modifier.getValues(false).forEach((k, v) -> {
                    Set<Entry<Economy, Double>> value = new HashSet<>();
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

        LOADED_MODIFIERS.putAll(mods);
        return LOADED_MODIFIERS;
    }

    public static Map<String, Set<Entry<Economy, Double>>> getModifier(String mod) {
        return getAllModifiers().get(mod);
    }

    public static Map<EntityDamageEvent.DamageCause, Double> getDeathModifiers() {
        Map<EntityDamageEvent.DamageCause, Double> mods = new EnumMap<>(EntityDamageEvent.DamageCause.class);

        FileConfiguration config = NovaConfig.getConfig();

        if (config.isConfigurationSection("NaturalCauses.Modifiers.Death")) {
            ConfigurationSection modifiers = config.getConfigurationSection("NaturalCauses.Modifiers.Death");

            modifiers.getValues(false).forEach((k, v) -> {
                EntityDamageEvent.DamageCause cause;
                try {
                    cause = EntityDamageEvent.DamageCause.valueOf(k.toUpperCase());
                } catch (IllegalArgumentException e) {
                    NovaConfig.getPlugin().getLogger().severe("Invalid Damage Cause in Death Modifiers: " + k);
                    return;
                }

                double d;
                try {
                    d = Double.parseDouble(v.toString());
                    if (d == 0) {
                        NovaConfig.getPlugin().getLogger().severe("Invalid Death Modifier (cannot be 0): " + k + ": " + v);
                        return;
                    }
                } catch (NumberFormatException e) {
                    NovaConfig.getPlugin().getLogger().severe("Invalid Death Modifier in Death Modifiers: " + k + ": " + v);
                    return;
                }

                mods.put(cause, d);
            });
        }

        return mods;
    }

    public static Entry<Economy, Double> readString(String s) {
        try {
            char s1 = s.charAt(0);
            char s2 = s.charAt(s.length() - 1);

            if (!Economy.exists(s1) && !Economy.exists(s2)) return null;

            String remove = Economy.exists(s1) ? s1 + "" : s2 + "";
            Economy econ = Economy.exists(s1) ? Economy.bySymbol(s1) : Economy.bySymbol(s2);
            double amountD = Double.parseDouble(s.replaceAll("[" + remove + "]", ""));
            return new AbstractMap.SimpleEntry<>(econ, amountD);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toModString(Entry<Economy, Double> entry) {
        if (entry == null) return null;
        return format("%,.0f", entry.getValue()) + entry.getKey().getSymbol();
    }

    public static List<String> toModList(List<Entry<Economy, Double>> list) {
        if (list == null) return Collections.emptyList();
        if (list.isEmpty()) return Collections.emptyList();
        return list.stream()
            .filter(Objects::nonNull)
            .map(ModifierReader::toModString).collect(Collectors.toList());
    }

}
