package us.teaminceptus.novaconomy.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum NovaSound {

    ENTITY_ARROW_HIT_PLAYER("SUCCESSFUL_HIT"),
    BLOCK_NOTE_BLOCK_PLING("NOTE_PLING", "BLOCK_NOTE_PLING"),
    BLOCK_ENDER_CHEST_OPEN,
    BLOCK_ANVIL_USE("ANVIL_USE"),
    ENTITY_ENDERMAN_TELEPORT("ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT"),
    BLOCK_CHEST_OPEN("CHEST_OPEN", "ENTITY_CHEST_OPEN"),
    ITEM_BOOK_PAGE_TURN
    ;

    private final List<String> sounds = new ArrayList<>();

    NovaSound(String... sounds) {
        // Ensure names are uppercase
        this.sounds.add(name().toUpperCase());
        this.sounds.addAll(Arrays.asList(sounds).stream().map(String::toUpperCase).collect(Collectors.toList()));
    }

    public Sound find() {
        for (String sound : sounds) try {
                return Sound.valueOf(sound);
            } catch (IllegalArgumentException ignored) {}

        return null;
    }

    public void play(Location l, float volume, float pitch) {
        Sound s = find();
        if (s == null) return;

        l.getWorld().playSound(l, find(), volume, pitch);
    }

    public void play(@NotNull HumanEntity en, float volume, float pitch) { play(en.getLocation(), volume, pitch); }

    public void playSuccess(@NotNull HumanEntity en) { play(en, 1F, 2F); }

    public void playSuccess(@NotNull CommandSender sender) { if (sender instanceof HumanEntity) playSuccess((HumanEntity) sender); }

    public void playFailure(@NotNull CommandSender sender) { if (sender instanceof HumanEntity) playFailure((HumanEntity) sender); }

    public void playFailure(@NotNull HumanEntity en) { play(en, 1F, 0F);}

    public void play(@NotNull HumanEntity en) { play(en, 1F, 1F); }

}
