package io.github.cspigot.feature.optimize;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.cspigot.utils.IntegerPair;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.time.Duration;

import java.util.zip.DeflaterOutputStream;

public class FastRegionFile extends net.minecraft.server.RegionFile {

    private final LoadingCache<IntegerPair, Boolean> existsCache;

    public FastRegionFile(File file) {
        super(file);
        existsCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .refreshAfterWrite(Duration.ofSeconds(30))
                .build(key -> super.chunkExists(key.a, key.b));
    }

    @Override
    public boolean chunkExists(int i, int j) {
        return Boolean.TRUE.equals(existsCache.get(new IntegerPair(i, j)));
    }

    @Override
    public DataOutputStream writeStream(int i, int j) { // PAIL: getChunkOutputStream
        // PAIL: isInvalidRegion
        existsCache.invalidate(new IntegerPair(i, j));
        return this.d(i, j) ? null : new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new OptiChunkBuffer(i, j))));
    }

    public class OptiChunkBuffer extends ChunkBuffer {
        public OptiChunkBuffer(int i, int j) {
            super(i, j);
        }

        public void close() {
            super.close();
            existsCache.invalidateAll();
        }

        @Override
        public synchronized void write(int b) {
            existsCache.invalidateAll();
            super.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            existsCache.invalidateAll();
            super.write(b, off, len);
        }
    }
}
