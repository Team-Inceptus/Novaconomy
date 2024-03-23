package us.teaminceptus.novaconomy.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaScheduler implements NovaScheduler {

    private Plugin plugin;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Using FoliaScheduler");
    }

    @Override
    public void teleport(Entity en, Location l) {
        en.teleportAsync(l);
    }

    @Override
    public void syncContext(Consumer<NovaTask> consumer) {
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> consumer.accept(task::cancel));
    }

    @Override
    public void sync(Entity en, Runnable runnable) {
        en.getScheduler().run(plugin, task -> runnable.run(), null);
    }

    @Override
    public void asyncContext(Consumer<NovaTask> consumer) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> consumer.accept(task::cancel));
    }

    @Override
    public void syncLaterContext(Consumer<NovaTask> consumer, long delay) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> consumer.accept(task::cancel), delay);
    }

    @Override
    public void syncLater(Entity en, Runnable runnable, long delay) {
        en.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delay);
    }

    @Override
    public void asyncLaterContext(Consumer<NovaTask> consumer, long delay) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, task -> consumer.accept(task::cancel), delay * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void syncRepeatingContext(Consumer<NovaTask> consumer, long delay, long period) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> consumer.accept(task::cancel), delay, period);
    }

    @Override
    public void syncRepeating(Entity en, Runnable runnable, long delay, long period) {
        en.getScheduler().runAtFixedRate(plugin, task -> runnable.run(), null, delay, period);
    }

    @Override
    public void asyncRepeatingContext(Consumer<NovaTask> consumer, long delay, long period) {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> consumer.accept(task::cancel), delay * 50, period * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancelAll() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
