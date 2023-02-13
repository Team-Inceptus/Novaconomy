package us.teaminceptus.novaconomy.util.inventory;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.util.NovaSound;

import static sun.net.NetProperties.get;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.util.inventory.Generator.genGUI;

public final class InventorySelector {

    private InventorySelector() { throw new UnsupportedOperationException(); }


    public static NovaInventory confirm(Player p, Runnable confirm) {
        return confirm(p, confirm, () -> {
            p.closeInventory();
            NovaSound.BLOCK_NOTE_BLOCK_PLING.playFailure(p);
        });
    }

    @NotNull
    public static NovaInventory confirm(Player p, Runnable confirm, Runnable cancel) {
        NovaInventory inv = genGUI("confirm_menu", 27, get("constants.are_you_sure"));
        inv.setCancelled();

        inv.setAttribute("accept_action", confirm);
        inv.setAttribute("cancel_action", cancel);

        inv.setItem(21, builder(Items.LIME_WOOL,
                meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.yes")),
                nbt -> nbt.set("type", "accept"))
        );

        inv.setItem(23, builder(Items.RED_WOOL,
                meta -> meta.setDisplayName(ChatColor.RED + get("constants.cancel")),
                nbt -> nbt.set("type", "cancel"))
        );

        return inv;
    }

}
