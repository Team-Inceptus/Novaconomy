package us.teaminceptus.novaconomy.v1_20_R3;

import com.google.common.collect.ImmutableMap;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftInventoryCustom;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;

import java.util.HashMap;
import java.util.Map;

final class NovaInventory1_20_R3 extends CraftInventoryCustom implements NovaInventory{

    private final Map<String, Object> attributes = new HashMap<>();

    public NovaInventory1_20_R3(String id, String name, int size) {
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
