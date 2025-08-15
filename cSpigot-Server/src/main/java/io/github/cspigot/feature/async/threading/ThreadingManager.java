package io.github.cspigot.feature.async.threading;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import io.github.cspigot.feature.async.threading.factory.NamePriorityThreadFactory;
import io.github.cspigot.config.SettingsConfig;

import java.util.concurrent.*;

@Getter
public class ThreadingManager {

    // Main Factory
    private final NamePriorityThreadFactory factory = new NamePriorityThreadFactory(
            Thread.currentThread().getPriority() - 1, "cSpigot - Async Thread")
            .setLogThreads(true)
            .setDaemon(true);

    // Async Entity Tracker - Increment threads by two to account for potential overhead, and the async tracker itself (this fixes crashes)
    private final ExecutorService trackerThreadPool = Executors.newFixedThreadPool(
            SettingsConfig.ASYNC_ENTITY_THREADS + 2, new ThreadFactoryBuilder().setNameFormat("cSpigot - EntityTracker - %d").build()
    );
    private final ExecutorService dispatcherThread = Executors.newSingleThreadExecutor(factory);

    // Fast Packet Queue
    private final ScheduledExecutorService fastPacketQueueThread = Executors.newSingleThreadScheduledExecutor(factory);

    // cSpigot's Original Thread Pool
    private final ScheduledExecutorService mainPool = new ScheduledThreadPoolExecutor(4, factory);

    public void execute(Runnable runnable) {
        mainPool.execute(runnable);
    }

    public void shutdown() {
        mainPool.shutdown();
    }

}
