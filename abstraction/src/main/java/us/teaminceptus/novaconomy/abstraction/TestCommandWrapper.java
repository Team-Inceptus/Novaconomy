package us.teaminceptus.novaconomy.abstraction;

import org.bukkit.plugin.Plugin;

public final class TestCommandWrapper implements CommandWrapper {

    public TestCommandWrapper(Plugin plugin) {
        plugin.getLogger().info("Loaded Test Command Wrapper");
    }

    // empty class

}
