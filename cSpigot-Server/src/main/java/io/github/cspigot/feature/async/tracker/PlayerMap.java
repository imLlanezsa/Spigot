package io.github.cspigot.feature.async.tracker;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MathHelper;
import io.github.cspigot.config.SettingsConfig;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class PlayerMap {
    private static final int CHUNK_BITS = 5;
    private final Long2ObjectMap<Collection<EntityPlayer>> map = Long2ObjectMaps.synchronize(SettingsConfig.PLAYER_MAP_HASH_MAP ?
            new Long2ObjectOpenHashMap<>() :
            new Long2ObjectArrayMap<>());

    private static long xzToKey(long x, long z) {
        return (x << 32) + z - Integer.MIN_VALUE;
    }

    public void add(EntityPlayer player) {
        int x = MathHelper.floor(player.locX) >> CHUNK_BITS;
        int z = MathHelper.floor(player.locZ) >> CHUNK_BITS;
        long key = xzToKey(x, z);
        Collection<EntityPlayer> list = map.get(key);
        if (list == null) {
            list = new ConcurrentLinkedQueue<>();
            map.put(key, list);
        }
        list.add(player);
        player.playerMapX = x;
        player.playerMapZ = z;
    }

    public void move(EntityPlayer player) {
        int x = MathHelper.floor(player.locX) >> CHUNK_BITS;
        int z = MathHelper.floor(player.locZ) >> CHUNK_BITS;

        // did we move?
        if (x == player.playerMapX && z == player.playerMapZ) {
            return;
        }

        // do remove
        long key = xzToKey(player.playerMapX, player.playerMapZ);
        Collection<EntityPlayer> list = map.get(key);
        // cSpigot start - Fix NPE
        if (list == null) {
            this.add(player);
            return;
        }
        // cSpigot end
        list.remove(player);
        if (list.isEmpty()) {
            map.remove(key);
        }

        // do add
        key = xzToKey(x, z);
        list = map.get(key);
        if (list == null) {
            list = new ConcurrentLinkedDeque<>();
            map.put(key, list);
        }
        list.add(player);
        player.playerMapX = x;
        player.playerMapZ = z;
    }

    public void remove(EntityPlayer player) {
        long key = xzToKey(player.playerMapX, player.playerMapZ);
        Collection<EntityPlayer> list = map.get(key);
        if (list == null) {
            // player has not yet been added to this playermap, this happens when
            // teleporting to another world during PlayerJoinEvent
            return;
        }
        list.remove(player);
        if (list.isEmpty()) {
            map.remove(key);
        }
    }

    public void forEachNearby(double x, double y, double z, double distance, boolean useRadius, Consumer<EntityPlayer> function) {
        int chunkXMax = MathHelper.floor(x + distance) >> CHUNK_BITS;
        int chunkZMax = MathHelper.floor(z + distance) >> CHUNK_BITS;

        for (int chunkX = MathHelper.floor(x - distance) >> CHUNK_BITS; chunkX <= chunkXMax; chunkX++) {
            for (int chunkZ = MathHelper.floor(z - distance) >> CHUNK_BITS; chunkZ <= chunkZMax; chunkZ++) {
                Collection<EntityPlayer> players = map.get(xzToKey(chunkX, chunkZ));
                if (players != null && !players.isEmpty()) {
                    for (EntityPlayer player : players) {
                        if (!useRadius || player.e(x, y, z) < distance * distance) {
                            function.accept(player);
                        }
                    }
                }
            }
        }
    }
}