package us.teaminceptus.novaconomy.util.command;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class MaterialSelector {

    private final Set<Material> materials = new HashSet<>();

    MaterialSelector() {}

    MaterialSelector(Material... materials) {
        this.materials.addAll(Arrays.asList(materials));
    }

    MaterialSelector(Iterable<Material> materials) {
        materials.forEach(this.materials::add);
    }

    public MaterialSelector add(Material material) {
        materials.add(material);
        return this;
    }

    public Set<Material> getMaterials() {
        return ImmutableSet.copyOf(materials);
    }

    @SuppressWarnings("unchecked")
    public static MaterialSelector of(String entry) {
        String[] split = entry.split(":");
        String entry0 = split[split.length - 1];

        if (entry0.equalsIgnoreCase("all"))
            return new MaterialSelector(Material.values());

        if (entry0.startsWith("#")) {
            try {
                Class<?> tags = Class.forName("org.bukkit.Tag");

                Object tag = tags.getField(entry0.substring(1).toUpperCase()).get(null);
                Set<Material> materials = (Set<Material>) tags.getMethod("getValues").invoke(tag);

                return new MaterialSelector(materials);
            } catch (ClassNotFoundException ignored) {
                return null;
            } catch (ReflectiveOperationException e) {
                NovaConfig.print(e);
            }
        } else
            return new MaterialSelector(Material.matchMaterial(entry0));

        return null;
    }
}
