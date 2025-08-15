package net.minecraft.server;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.bukkit.Bukkit;

import org.github.paperspigot.PaperSpigotConfig;
import org.github.paperspigot.exception.ServerInternalException;


import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.optimize.FastRegionFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class RegionFileCache {

    public static final Map<File, RegionFile> cache = new LinkedHashMap<>(PaperSpigotConfig.regionFileCacheSize, 0.75f, true); // Paper - HashMap -> LinkedHashMap
    public static Cache<File, RegionFile> b = Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofMinutes(15))
            .expireAfterWrite(Duration.ofMinutes(40))
            .maximumSize(Short.MAX_VALUE) // prevent insane mem usage (32k max chunks)
            .evictionListener((key, value, cause) -> {
                if (value != null) {
                    final RegionFile regionFile = ((RegionFile) value);
                    try {
                        regionFile.c();
                    } catch (IOException ioexception) {
                        ioexception.printStackTrace();
                        ServerInternalException.reportInternalException(ioexception); // Paper
                    }
                }
            }).build();

    public static void readConfig() {
        Duration duration = Duration.ofMillis(SettingsConfig.REGION_CACHE_EXPIRY);
        b = Caffeine.newBuilder()
                .maximumSize(SettingsConfig.REGION_MAX_CACHED_SIZE)
                .expireAfterAccess(duration)
                .expireAfterWrite(Math.max(duration.toMillis(), Duration.ofMinutes(40).toMillis()), TimeUnit.MILLISECONDS)
                .evictionListener((key, value, cause) -> {
                    if (value != null) {
                        final RegionFile regionFile = ((RegionFile) value);
                        try {
                            regionFile.c();
                        } catch (IOException ioexception) {
                            ioexception.printStackTrace();
                            ServerInternalException.reportInternalException(ioexception); // Paper
                        }
                    }
                }).build();
    }

    // PaperSpigot start
    public static synchronized RegionFile getRegionFile(File file, int i, int j) { return a(file, i, j); } // Paper - OBFHELPER
    public static synchronized RegionFile a(File file, int i, int j) {
        return a(file, i, j, true);
    }

    public static synchronized RegionFile a(File file, int i, int j, boolean create) {
        // PaperSpigot end
        File file1 = new File(file, "region");
        File file2 = new File(file1, "r." + (i >> 5) + "." + (j >> 5) + ".mca");
        RegionFile regionfile = SettingsConfig.REGION_OPTIMIZED_CACHE ? b.getIfPresent(file2) : RegionFileCache.cache.get(file2);

        if (regionfile != null) {
            return regionfile;
        } else {
            RegionFile regionfile1 = createRegionFile(create, file1, file2);
            if (regionfile1 != null) {
                if (SettingsConfig.REGION_OPTIMIZED_CACHE) {
                    b.put(file2, regionfile1);
                } else {
                    RegionFileCache.cache.put(file2, regionfile1);
                }
            }
            return regionfile1;
        }
    }

    private static RegionFile createRegionFile(final boolean create, final File file1, final File file2) {
        if (!create && !file2.exists()) {
            return null;
        } // PaperSpigot
        if (!file1.exists()) {
            file1.mkdirs();
        }

        if (RegionFileCache.cache.size() >= 256 && !SettingsConfig.REGION_OPTIMIZED_CACHE) {
            trimCache();
        }

        return SettingsConfig.FASTER_REGION_FILE ? new FastRegionFile(file2) : new RegionFile(file2);
    }

    // PAIL: clearCache
    public static synchronized void a() {
        trimCache();
    }

    // cSpigot start - Optimize region files
    public static void unloadWorld(File parentFolder) {
        if (!SettingsConfig.REGION_OPTIMIZED_CACHE) {
            Iterator<Map.Entry<File, RegionFile>> iterator = RegionFileCache.cache.entrySet().iterator(); // RegionFileCache.a should be RegionFileCache.cache
            while (iterator.hasNext()) {
                Map.Entry<File, RegionFile> entry = iterator.next();
                File child = entry.getKey().getAbsoluteFile();
                while (child != null) {
                    if (child.equals(parentFolder)) {
                        iterator.remove();
                        try {
                            entry.getValue().c(); // Should be RegionFile.close();
                        } catch (IOException ex) {
                            Bukkit.getLogger().log(Level.SEVERE, null, ex);
                        }
                        break;
                    }
                    child = child.getParentFile();
                }
            }
        } else {
            for ( Map.Entry<File, RegionFile> entry : RegionFileCache.b.asMap().entrySet() ) {
                File child = entry.getKey().getAbsoluteFile();
                while (child != null) {
                    if (child.equals(parentFolder)) {
                        RegionFileCache.b.invalidate(entry.getKey());
                        try {
                            entry.getValue().c(); // Should be RegionFile.close();
                        } catch (IOException ex) {
                            Bukkit.getLogger().log(Level.SEVERE, null, ex);
                        }
                        break;
                    }
                    child = child.getParentFile();
                }
            }
        }
    }
    // cSpigot end

    private static synchronized void trimCache() {
        Iterator<Map.Entry<File, RegionFile>> itr = RegionFileCache.cache.entrySet().iterator();
        int count = RegionFileCache.cache.size() - PaperSpigotConfig.regionFileCacheSize;
        while (count-- >= 0 && itr.hasNext()) {
            try {
                itr.next().getValue().c();
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
                ServerInternalException.reportInternalException(ioexception);
            }
            itr.remove();
        }
    }

    // PAIL: load(File regionFile, int x, int z)
    public static DataInputStream c(File file, int i, int j) {
        RegionFile regionfile = a(file, i, j);

        return regionfile.getReadStream(i & 31, j & 31);
    }

    // PAIL: save(File regionFile, int x, int z)
    public static DataOutputStream d(File file, int i, int j) {
        RegionFile regionfile = a(file, i, j);

        return regionfile.writeStream(i & 31, j & 31);
    }
}
