package us.teaminceptus.novaconomy;

import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

class VaultChat {

    static Chat chat;

    static void reloadChat() {
        if (Novaconomy.hasVault()) {
            RegisteredServiceProvider<Chat> rsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp.getProvider() != null) chat = rsp.getProvider();
        }
    }

    static boolean isInGroup(List<String> groups, Player p) {
        if (chat == null) return false;
        AtomicBoolean b = new AtomicBoolean(false);
        for (String s : groups) {
            Pattern patt = Pattern.compile(s);
            for (String g : chat.getPlayerGroups(p)) if (patt.matcher(g).matches()) {
                b.set(true);
                break;
            }
        }

        return b.get();
    }

}
