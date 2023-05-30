package us.teaminceptus.novaconomy.v1_15_R1;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftInventoryCustom;

import com.google.common.collect.ImmutableMap;

import us.teaminceptus.novaconomy.abstraction.NovaInventory;

final class NovaInventory1_15_R1 extends CraftInventoryCustom implements NovaInventory{

    private final Map<String, Object> attributes = new HashMap<>();

    public NovaInventory1_15_R1(String id, String name, int size) {
        super(null, size, name);

        setAttribute("_id", id);
        setAttribute("_name", name);
    }

    @Override
    public Map<String, Object> getAttribtes() {
        return ImmutableMap.copyOf(attributes);
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
}
