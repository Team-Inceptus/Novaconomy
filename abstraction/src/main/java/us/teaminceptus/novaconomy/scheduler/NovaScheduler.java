package us.teaminceptus.novaconomy.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import us.teaminceptus.novaconomy.api.NovaConfig;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface NovaScheduler {

    NovaScheduler scheduler = getScheduler();

    void teleport(Entity en, Location l);

    void syncContext(Consumer<NovaTask> consumer);

    default void sync(Runnable runnable) {
        syncContext(task -> runnable.run());
    }

    default void sync(Entity en, Runnable runnable) {
        sync(runnable);
    }

    void asyncContext(Consumer<NovaTask> consumer);

    default void async(Runnable runnable) {
        asyncContext(task -> runnable.run());
    }

    void syncLaterContext(Consumer<NovaTask> consumer, long delay);

    default void syncLater(Runnable runnable, long delay) {
        syncLaterContext(task -> runnable.run(), delay);
    }

    default void syncLater(Entity en, Runnable runnable, long delay) {
        syncLater(runnable, delay);
    }

    void asyncLaterContext(Consumer<NovaTask> consumer, long delay);

    default void asyncLater(Runnable runnable, long delay) {
        asyncLaterContext(task -> runnable.run(), delay);
    }

    void syncRepeatingContext(Consumer<NovaTask> consumer, long delay, long period);

    default void syncRepeating(Supplier<Boolean> condition, Runnable runnable, long delay, long period) {
        syncRepeatingContext(task -> {
            if (!condition.get()) return;
            runnable.run();
        }, delay, period);
    }

    default void syncRepeating(Runnable runnable, long delay, long period) {
        syncRepeatingContext(task -> runnable.run(), delay, period);
    }

    default void syncRepeating(Entity en, Runnable runnable, long delay, long period) {
        syncRepeating(runnable, delay, period);
    }

    void asyncRepeatingContext(Consumer<NovaTask> consumer, long delay, long period);

    default void asyncRepeating(Runnable runnable, long delay, long period) {
        asyncRepeatingContext(task -> runnable.run(), delay, period);
    }

    default void asyncRepeating(Supplier<Boolean> condition, Runnable runnable, long delay, long period) {
        asyncRepeatingContext(task -> {
            if (!condition.get()) return;
            runnable.run();
        }, delay, period);
    }

    void cancelAll();

    // Fetcher

    static NovaScheduler getScheduler() {
        Plugin p = NovaConfig.getPlugin();

        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");

            Constructor<? extends NovaScheduler> folia = Class.forName("us.teaminceptus.novaconomy.scheduler.FoliaScheduler")
                    .asSubclass(NovaScheduler.class)
                    .getConstructor(Plugin.class);

            return folia.newInstance(p);
        } catch (ClassNotFoundException e) {
            return new BukkitScheduler(p);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
