package us.teaminceptus.novaconomy.abstraction.test;

import org.bukkit.plugin.Plugin;

import us.teaminceptus.novaconomy.abstraction.CommandWrapper;

public final class TestCommandWrapper implements CommandWrapper {

    public TestCommandWrapper(Plugin plugin) {
        plugin.getLogger().info("Loaded Test Command Wrapper");
    }

    // empty class

}
