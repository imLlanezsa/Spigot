package io.github.cspigot.feature.async.lighting;

import net.minecraft.server.Chunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldServer;
import io.github.cspigot.config.SettingsConfig;

import java.util.ArrayDeque;

public class PaperLightingQueue {
    private static final long MAX_TIME = (1000000 * (50L + SettingsConfig.MAX_TICK_MS_LOST_LIGHT_QUEUE));

    public static void processQueue(long curTime) {
        final long startTime = System.nanoTime();
        final long maxTickTime = MAX_TIME - (startTime - curTime);

        if (maxTickTime <= 0) {
            return;
        }

        START:
        for ( WorldServer world : MinecraftServer.getServer().getWorlds()) {
            for ( Chunk chunk : world.getChunkProviderServer().chunks.values() ) {
                if (chunk.lightingQueue.processQueue(startTime, maxTickTime)) {
                    break START;
                }
            }
        }
    }

    public static class LightingQueue extends ArrayDeque<Runnable> {
        final private Chunk chunk;

        public LightingQueue(Chunk chunk) {
            super();
            this.chunk = chunk;
        }

        /**
         * Processes the lighting queue for this chunk
         *
         * @param startTime If start Time is 0, we will not limit execution time
         * @param maxTickTime Maximum time to spend processing lighting updates
         * @return true to abort processing furthur lighting updates
         */
        private boolean processQueue(long startTime, long maxTickTime) {
            if (this.isEmpty()) {
                return false;
            }
            if (isOutOfTime(maxTickTime, startTime)) {
                return true;
            }

            Runnable lightUpdate;
            while ((lightUpdate = this.poll()) != null) {
                lightUpdate.run();
                if (isOutOfTime(maxTickTime, startTime)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Flushes lighting updates to unload the chunk
         */
        public void processUnload() {
            if (!SettingsConfig.QUEUE_LIGHT_UPDATES) {
                return;
            }
            processQueue(0, 0); // No timeout

            final int radius = 1;
            for (int x = chunk.locX - radius; x <= chunk.locX + radius; ++x) {
                for (int z = chunk.locZ - radius; z <= chunk.locZ + radius; ++z) {
                    if (x == chunk.locX && z == chunk.locZ) {
                        continue;
                    }

                    Chunk neighbor = chunk.world.getChunkIfLoaded(x, z);
                    if (neighbor != null) {
                        neighbor.lightingQueue.processQueue(0, 0); // No timeout
                    }
                }
            }
        }
    }

    private static boolean isOutOfTime(long maxTickTime, long startTime) {
        return startTime > 0 && System.nanoTime() - startTime > maxTickTime;
    }
}
