package io.github.cspigot.feature.chunk.collection;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.Chunk;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.util.LongHash;
import io.github.cspigot.config.SettingsConfig;

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
public class ParentChunkMap extends Long2ObjectOpenHashMap<Chunk> {

    protected final Chunk[] cachedChunks = new Chunk[16];
    protected final long[] cachedKeys = new long[16];

    public ParentChunkMap(int i, float f) {
        super(i, f);

        if (SettingsConfig.CHUNK_LOADED_CACHE) {
            java.util.Arrays.fill(this.cachedKeys, LongHash.toLong(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
    }

    @Override
    public boolean containsKey(long key) {
        if (SettingsConfig.CHUNK_LOADED_CACHE && MinecraftServer.getServer().primaryThread == Thread.currentThread()) {
            int cacheKey = getChunkCacheKey(LongHash.msw(key), LongHash.lsw(key));
            long cachedKey = this.cachedKeys[cacheKey];
            if (cachedKey == key) {
                return true;
            }
            return super.containsKey(key);
        }
        return super.containsKey(key);
    }

    public static int getChunkCacheKey(int x, int z) {
        return x & 3 | ((z & 3) << 2);
    }
}
