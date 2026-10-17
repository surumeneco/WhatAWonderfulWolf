package co.surumene.www.spawn;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Pure calculations only off-thread; Bukkit/world application remains on the server thread. */
public final class AsyncWolfFounderCoordinator {
    private final Plugin plugin;
    private final Logger logger;
    private final ThreadPoolExecutor worker;
    private final ArrayDeque<Runnable> waiting = new ArrayDeque<>();
    private volatile boolean closed;

    public AsyncWolfFounderCoordinator(Plugin plugin, Logger logger) {
        this.plugin = Objects.requireNonNull(plugin);
        this.logger = Objects.requireNonNull(logger);
        this.worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(16),
                task -> {
                    Thread thread = new Thread(task, "www-founder-worker");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public <T> boolean submit(Callable<T> calculation, Consumer<T> mainThreadCommit,
                              Consumer<Exception> onFailure) {
        Objects.requireNonNull(calculation);
        Objects.requireNonNull(mainThreadCommit);
        Objects.requireNonNull(onFailure);
        if (closed || waiting.size() >= 128) return false;
        waiting.addLast(() -> {
            T result = null;
            Exception failure = null;
            try {
                result = calculation.call();
            } catch (Exception ex) {
                failure = ex;
            }
            T completed = result;
            Exception error = failure;
            if (closed || !plugin.isEnabled()) return;
            try {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (closed || !plugin.isEnabled()) return;
                        if (error != null) onFailure.accept(error);
                        else mainThreadCommit.accept(completed);
                    } finally {
                        tick();
                    }
                });
            } catch (IllegalStateException ignored) {
                // Plugin was disabled while computing.
            }
        });
        tick();
        return true;
    }

    public void tick() {
        if (closed || waiting.isEmpty()
                || worker.getQueue().remainingCapacity() == 0) return;
        Runnable task = waiting.removeFirst();
        try {
            worker.execute(task);
        } catch (RejectedExecutionException rejected) {
            if (!closed) waiting.addFirst(task);
        }
    }

    public Plugin plugin() { return plugin; }

    public void shutdown() {
        closed = true;
        waiting.clear();
        worker.shutdownNow();
    }
}
