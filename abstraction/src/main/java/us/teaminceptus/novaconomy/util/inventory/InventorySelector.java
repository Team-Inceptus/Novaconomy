package us.teaminceptus.novaconomy.util.inventory;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import us.teaminceptus.novaconomy.abstraction.NovaInventory;
import us.teaminceptus.novaconomy.api.SortingType;
import us.teaminceptus.novaconomy.api.business.Business;
import us.teaminceptus.novaconomy.api.corporation.Corporation;
import us.teaminceptus.novaconomy.util.NovaSound;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static us.teaminceptus.novaconomy.abstraction.CommandWrapper.BUSINESS_TAG;
import static us.teaminceptus.novaconomy.abstraction.NBTWrapper.builder;
import static us.teaminceptus.novaconomy.abstraction.Wrapper.get;
import static us.teaminceptus.novaconomy.util.inventory.Generator.genGUI;
import static us.teaminceptus.novaconomy.util.inventory.Items.sorter;

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

        while (inv.firstEmpty() != -1)
            inv.setItem(inv.firstEmpty(), Items.GUI_BACKGROUND);

        return inv;
    }

    public static NovaInventory selectCorporationChildren(Player p, SortingType<Business> sorter, String searchQuery, Consumer<Business> consumer) {
        NovaInventory inv = genGUI("select_corporation_children", 54, get("constants.corporation.select_child"));
        inv.setCancelled();

        Corporation c = Corporation.byOwner(p);
        if (c == null) throw new IllegalArgumentException("Player is not an owner of a Corporation");

        inv.setAttribute("find_action", consumer);

        inv.setAttribute("sorting_type", Business.class);
        inv.setAttribute("sorting_function", (Function<SortingType<Business>, NovaInventory>) s ->
                selectCorporationChildren(p, s, searchQuery, consumer)
        );
        inv.setAttribute("sorter", sorter);

        inv.setItem(9, sorter(sorter));
        inv.setItem(17, builder(Items.OAK_SIGN,
                meta -> meta.setDisplayName(ChatColor.GREEN + get("constants.search")),
                nbt -> nbt.setID("find_child:search"))
        );

        List<Business> children = c.getChildren().stream()
                .sorted(sorter)
                .filter(b -> searchQuery.isEmpty() || b.getName().toLowerCase().contains(searchQuery.toLowerCase()))
                .limit(28)
                .collect(Collectors.toList());

        children.forEach(b -> inv.addItem(builder(b.getIcon(), nbt -> nbt.set(BUSINESS_TAG, b.getUniqueId()) )) );

        return inv;
    }

}
