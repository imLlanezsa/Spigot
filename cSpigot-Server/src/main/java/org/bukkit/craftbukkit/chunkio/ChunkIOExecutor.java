package org.bukkit.craftbukkit.chunkio;

import lombok.SneakyThrows;
import net.minecraft.server.*;
import org.bukkit.craftbukkit.util.AsynchronousExecutor;
import io.github.cspigot.config.SettingsConfig;

public class ChunkIOExecutor {

    static final int BASE_THREADS = SettingsConfig.CHUNK_THREADS; // cSpigot - configure value
    static final int PLAYERS_PER_THREAD = SettingsConfig.PLAYERS_PER_THREAD;

    private static final AsynchronousExecutor<QueuedChunk, Chunk, Runnable, RuntimeException> EXECUTOR;

    static  {
        EXECUTOR = SettingsConfig.ASYNC_CHUNKS ? null : new AsynchronousExecutor<>(new ChunkIOProvider(), BASE_THREADS);
    }

    public static Chunk syncChunkLoad(World world, ChunkRegionLoader loader, ChunkProviderServer provider, int x, int z) {
        return MCUtil.ensureMain("Async Chunk Load", () -> EXECUTOR.getSkipQueue(new QueuedChunk(x, z, loader, world, provider))); // Paper
    }

    public static void queueChunkLoad(World world, ChunkRegionLoader loader, ChunkProviderServer provider, int x, int z, Runnable runnable) {
        if (SettingsConfig.ASYNC_CHUNKS) return;
        EXECUTOR.add(new QueuedChunk(x, z, loader, world, provider), runnable);
    }

    // Abuses the fact that hashCode and equals for QueuedChunk only use world and coords
    public static void dropQueuedChunkLoad(World world, int x, int z, Runnable runnable) {
        if (SettingsConfig.ASYNC_CHUNKS) return;
        EXECUTOR.drop(new QueuedChunk(x, z, null, world, null), runnable);
    }

    public static void adjustPoolSize(int players) {
        if (SettingsConfig.ASYNC_CHUNKS) return;
        int size = Math.max(BASE_THREADS, (int) Math.ceil(players / PLAYERS_PER_THREAD));
        EXECUTOR.setActiveThreads(size);
    }

    public static boolean isQueuedToLoad(World world, int x, int z) {
        if (SettingsConfig.ASYNC_CHUNKS) return false;
        try {
            return EXECUTOR.hasTask(new QueuedChunk(x, z, null, world, null));
        } catch (IllegalStateException ex) {
            return false;
        }
    }


    @SneakyThrows
    public static void tick() {
        if (SettingsConfig.ASYNC_CHUNKS) return;
        if (EXECUTOR == null) {
            System.out.println("[cSpigot-Chunks] Async Notification: Java class loaders did the funny, we will sleep the thread for 5s.");
            Thread.sleep(5000);
        }
        EXECUTOR.finishActive();
    }
}
