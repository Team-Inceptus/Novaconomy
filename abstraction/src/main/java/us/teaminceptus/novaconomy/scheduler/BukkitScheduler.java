package us.teaminceptus.novaconomy.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public final class BukkitScheduler implements NovaScheduler {

    private Plugin plugin;

    public BukkitScheduler(Plugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Using BukkitScheduler");
    }

    @Override
    public void teleport(Entity en, Location l) {
        en.teleport(l);
    }

    @Override
    public void syncContext(Consumer<NovaTask> consumer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(this::cancel);
            }
        }.runTask(plugin);
    }

    @Override
    public void asyncContext(Consumer<NovaTask> consumer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(this::cancel);
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public void syncLaterContext(Consumer<NovaTask> consumer, long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(this::cancel);
            }
        }.runTaskLater(plugin, delay);
    }

    @Override
    public void asyncLaterContext(Consumer<NovaTask> consumer, long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(this::cancel);
            }
        }.runTaskLaterAsynchronously(plugin, delay);
    }

    @Override
    public void syncRepeatingContext(Consumer<NovaTask> consumer, long delay, long period) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(this::cancel);
            }
        }.runTaskTimer(plugin, delay, period);
    }

    @Override
    public void asyncRepeatingContext(Consumer<NovaTask> consumer, long delay, long period) {
        new BukkitRunnable() {
            @Override
            public void run() {
                consumer.accept(this::cancel);
            }
        }.runTaskTimerAsynchronously(plugin, delay, period);
    }

    @Override
    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
