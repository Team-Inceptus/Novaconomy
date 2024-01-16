package us.teaminceptus.novaconomy.abstraction.test;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import us.teaminceptus.novaconomy.abstraction.NBTWrapper;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.abstraction.Wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class TestWrapper implements Wrapper {

    @Override
    public int getCommandVersion() {
        return 0;
    }

    @Override
    public void sendActionbar(Player p, String message) {}

    @Override
    public void sendActionbar(Player p, BaseComponent component) {}

    @Override
    public boolean isAgeable(Block b) {
        return false;
    }

    @Override
    public void removeItem(PlayerInteractEvent p) {}

    @Override
    public boolean isCrop(Material m) {
        return false;
    }

    @Override
    public List<Material> getCrops() {
        return new ArrayList<>();
    }

    @Override
    public NovaInventory createInventory(String id, String name, int size) {
        return null;
    }

    @Override
    public NBTWrapper createNBTWrapper(ItemStack item) {
        return null;
    }

    @Override
    public void addPacketInjector(Player p) {}

    @Override
    public void removePacketInjector(Player p) {}

    @Override
    public void sendSign(Player p, Consumer<String[]> lines) {}

}
