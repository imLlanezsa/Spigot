package io.github.cspigot.feature.chunk.collection;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.*;

import org.bukkit.craftbukkit.util.LongHash;

import org.spigotmc.AsyncCatcher;
import io.github.cspigot.config.SettingsConfig;

// cSpigot - Backported from Paper 1.13.2
@Log4j2
public class AsyncChunkMap extends ParentChunkMap {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public AsyncChunkMap(int i) {
        super(i, 0.5f);
    }

    public Chunk put(long key, Chunk chunk) {
        if (AsyncCatcher.outOfMain("chunk put")) {
            return MCUtil.ensureMain("chunk put", () -> this.put(key, chunk));
        }

        if (SettingsConfig.CHUNK_LOADED_CACHE) {
            int cacheKey = getChunkCacheKey(chunk.locX, chunk.locZ);
            long cachedKey = this.cachedKeys[cacheKey];
            if (cachedKey == chunk.chunkKey) {
                this.cachedChunks[cacheKey] = chunk;
            }
        }


        Chunk oldChunk = super.put(key, chunk);
        if (oldChunk == null) { // Paper - we should never be overwriting chunks
            // Update neighbor counts
            for (int x = -2; x < 3; x++) {
                for (int z = -2; z < 3; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }

                    Chunk neighbor = super.get(LongHash.toLong(chunk.locX + x, chunk.locZ + z));
                    if (neighbor != null) {
                        neighbor.setNeighborLoaded(-x, -z);
                        chunk.setNeighborLoaded(x, z);
                    }
                }
            }
        } else {
            log.error("Overwrote existing chunk! (" + chunk.world.getWorld().getName() + ":" + chunk.locX + "," + chunk.locZ + ")", new IllegalStateException());
        }
        return oldChunk;
    }

    public Chunk remove(long key) {
        if (AsyncCatcher.outOfMain("chunk remove")) {
            return MCUtil.ensureMain("chunk remove", () -> this.remove(key));
        }

        Chunk chunk = super.remove(key);

        if (SettingsConfig.CHUNK_LOADED_CACHE) {
            int cacheKey = getChunkCacheKey(chunk.locX, chunk.locZ);
            long cachedKey = this.cachedKeys[cacheKey];
            if (cachedKey == key) {
                this.cachedChunks[cacheKey] = null;
            }
        }

        Chunk oldChunk = super.remove(key);
        if (oldChunk != null) { // Paper - don't decrement if we didn't remove anything
            // Update neighbor counts
            for ( int x = -2; x < 3; x++ ) {
                for ( int z = -2; z < 3; z++ ) {
                    if (x == 0 && z == 0) {
                        continue;
                    }

                    Chunk neighbor = super.get(LongHash.toLong(oldChunk.locX + x, oldChunk.locZ + z)); // Paper
                    if (neighbor != null) {
                        neighbor.setNeighborUnloaded(-x, -z);
                        oldChunk.setNeighborUnloaded(x, z);
                    }
                }
            }
        }
        return oldChunk;
    }

    @Override
    public Chunk get(long key) {
        if (Thread.currentThread() == MinecraftServer.getServer().primaryThread) {
            // Check if its cached
            if (SettingsConfig.CHUNK_LOADED_CACHE) {
                int cacheKey = getChunkCacheKey(LongHash.msw(key), LongHash.lsw(key));
                long cachedKey = this.cachedKeys[cacheKey];
                if (cachedKey == key) {
                    return this.cachedChunks[cacheKey];
                }
            }

            Chunk chunk = super.get(key);

            // cache the chunk
            if (SettingsConfig.CHUNK_LOADED_CACHE) {
                int cacheKey = getChunkCacheKey(LongHash.msw(key), LongHash.lsw(key));
                this.cachedKeys[cacheKey] = key;
                this.cachedChunks[cacheKey] = chunk;
            }
            return chunk;
        } else {
            this.lock.readLock().lock();
            Chunk chunk;
            try {
                // Check if its cached
                if (SettingsConfig.CHUNK_LOADED_CACHE) {
                    int cacheKey = getChunkCacheKey(LongHash.msw(key), LongHash.lsw(key));
                    long cachedKey = this.cachedKeys[cacheKey];
                    if (cachedKey == key) {
                        return this.cachedChunks[cacheKey];
                    }
                }
                chunk = super.get(key);

                // cache the chunk
                if (SettingsConfig.CHUNK_LOADED_CACHE) {
                    int cacheKey = getChunkCacheKey(LongHash.msw(key), LongHash.lsw(key));
                    this.cachedKeys[cacheKey] = key;
                    this.cachedChunks[cacheKey] = chunk;
                }
            } finally {
                this.lock.readLock().unlock();
            }

            return chunk;
        }
    }


    // cSpigot start - Override object based methods to point to primitive ones.
    public Chunk put(Long olong, Chunk chunk) {
        return this.put(olong.longValue(), chunk);
    }

    public Chunk remove(Object key) {
        return this.remove(((Long) key).longValue());
    }
    // cSpigot end

    public void putAll(Map<? extends Long, ? extends Chunk> map) {
        throw new RuntimeException("Not yet implemented");
    }

    public boolean remove(Object object, Object object1) {
        throw new RuntimeException("Not yet implemented");
    }

}
