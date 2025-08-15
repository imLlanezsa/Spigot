package io.github.cspigot.feature.chunk.collection;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.Chunk;
import net.minecraft.server.MCUtil;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.util.LongHash;
import org.spigotmc.AsyncCatcher;
import io.github.cspigot.config.SettingsConfig;

import java.util.Map;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 3/2/2024
 */

@Log4j2
public class SyncChunkMap extends ParentChunkMap {

    public SyncChunkMap(int i, float f) {
        super(i, f);
    }

    @Override
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

        return super.put(key, chunk);
    }

    @Override
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

        return chunk;
    }

    @Override
    public Chunk get(long key) {
        if (SettingsConfig.CHUNK_LOADED_CACHE && MinecraftServer.getServer().primaryThread == Thread.currentThread()) {
            // Check if its cached
            int cacheKey = getChunkCacheKey(LongHash.msw(key), LongHash.lsw(key));
            long cachedKey = this.cachedKeys[cacheKey];
            if (cachedKey == key) {
                return this.cachedChunks[cacheKey];
            }
            Chunk chunk = super.get(key);

            // cache the chunk
            this.cachedKeys[cacheKey] = key;
            this.cachedChunks[cacheKey] = chunk;
            return chunk;
        }
        return super.get(key);
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
