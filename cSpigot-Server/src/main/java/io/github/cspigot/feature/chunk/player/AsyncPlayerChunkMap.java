package io.github.cspigot.feature.chunk.player;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 3/9/2024
 */
@Log4j2
public class AsyncPlayerChunkMap extends PlayerChunkMap {

    public final List<AsyncPlayerChunk> loadingChunks = Lists.newLinkedList();
    public final List<AsyncPlayerChunk> sendingChunks = Lists.newArrayList();

    private boolean loadChunks = false;
    private boolean sendChunks = false;

    public AsyncPlayerChunkMap(WorldServer worldserver, int viewDistance) {
        super(worldserver, viewDistance);

        log.info("[cSpigot] Using cSpigot's async implementation for world {}", worldserver.worldData.getName());
    }

    public void flush() {
        long i = this.getWorld().getTime();
        PlayerChunkMap.PlayerChunk chunk;

        if (i - this.h > 8000L) {
            this.h = i;

            // cSpigot - I don't know why bukkit used an iterator here, since for loop would be better
            // because they aren't removing any items from the list and hence no cme would occur
            for ( PlayerChunk playerChunk : this.f ) {
                chunk = playerChunk;
                chunk.b();
                chunk.a();
            }
        } else {
            // cSpigot start - Use queue
            while ((chunk = this.e.poll()) != null) {
                chunk.b(); // updateChunk
            }
            // cSpigot end
        }

        if (this.loadChunks && i % 4L == 0L) {
            this.loadChunks = false;
            this.loadingChunks.sort((playerchunk1, playerchunk2) -> ComparisonChain.start().compare(playerchunk1.radius(), playerchunk2.radius()).result());
        }

        if (this.sendChunks && i % 4L == 2L) {
            this.sendChunks = false;
            this.sendingChunks.sort((playerchunk1, playerchunk2) -> ComparisonChain.start().compare(playerchunk1.radius(), playerchunk2.radius()).result());
        }

        if (!this.loadingChunks.isEmpty()) {
            // Spigot start
            org.spigotmc.SlackActivityAccountant activityAccountant = this.getWorld().getMinecraftServer().slackActivityAccountant;
            activityAccountant.startActivity(0.5);
            int chunkGensAllowed = getWorld().spigotConfig.maxBulkChunk;
            // Spigot end

            Iterator iterator1 = this.loadingChunks.iterator();

            while (iterator1.hasNext()) {
                AsyncPlayerChunk playerchunk1 = (AsyncPlayerChunk) iterator1.next();

                if (playerchunk1.getChunk() == null) {
                    boolean flag = playerchunk1.test(PlayerChunkMap.CAN_GEN_CHUNKS);
                    // Paper start
                    if (flag && !playerchunk1.chunkExists && chunkGensAllowed-- <= 0) {
                        continue;
                    }
                    // Paper end

                    if (playerchunk1.loadChunk(flag)) {
                        iterator1.remove();
                        if (playerchunk1.sendAll()) {
                            this.sendingChunks.remove(playerchunk1);
                        }

                        if (activityAccountant.activityTimeIsExhausted()) { // Spigot
                            break;
                        }
                    }
                    // CraftBukkit start - SPIGOT-2891: remove once chunk has been provided
                } else {
                    iterator1.remove();
                }
                // CraftBukkit end
            }

            activityAccountant.endActivity(); // Spigot
        }

        if (!this.sendingChunks.isEmpty()) {
            int j = getWorld().paperSpigotConfig.maxChunkSendsPerTick; // Paper
            Iterator iterator2 = this.sendingChunks.iterator();

            while (iterator2.hasNext()) {
                AsyncPlayerChunk playerchunk2 = (AsyncPlayerChunk) iterator2.next();

                if (playerchunk2.sendAll()) {
                    iterator2.remove();
                    --j;
                    if (j < 0) {
                        break;
                    }
                }
            }
        }

        if (this.managedPlayers.isEmpty()) {
            if (!wasNotEmpty) {
                return; // CraftBukkit - Only do unload when we go from non-empty to empty
            }
            WorldProvider worldprovider = this.getWorld().worldProvider;

            if (!worldprovider.e()) {
                this.getWorld().chunkProviderServer.b();
            }
            // CraftBukkit start
            wasNotEmpty = false;
        } else {
            wasNotEmpty = true;
        }
        // CraftBukkit end
    }

    /**
     * Fetch a player chunk from the stored map.
     *
     * @param x      {@link Integer} X.
     * @param z      {@link Integer} Z.
     * @param create {@link Boolean} Should we create it?
     * @return       {@link PlayerChunk} The retrieved/created chunk.
     */
    public AsyncPlayerChunk fetchChunk(int x, int z, boolean create) {
        long key = d(x, z);

        AsyncPlayerChunk chunk = (AsyncPlayerChunk) this.d.get(key);
        if (chunk != null) {
            return chunk;
        }

        if (create) {
            chunk = this.createChunk(x, z);
            this.d.put(key, chunk);
            this.f.add(chunk);
            if (chunk.getChunk() == null) {
                this.loadingChunks.add(chunk);
            }

            if (!chunk.sendAll()) {
                this.sendingChunks.add(chunk);
            }
        }

        return chunk;
    }

    public boolean isInChunk(EntityPlayer entityPlayer, int x, int z) {
        AsyncPlayerChunk chunk = this.fetchChunk(x, z, false);
        return chunk != null && chunk.getB().contains(entityPlayer) && chunk.done;
    }

    public void addPlayer(EntityPlayer entityPlayer) {
        int i = (int) entityPlayer.locX >> 4;
        int j = (int) entityPlayer.locZ >> 4;

        entityPlayer.d = entityPlayer.locX;
        entityPlayer.e = entityPlayer.locZ;

        // CraftBukkit start - Load nearby chunks first
        List<ChunkCoordIntPair> chunkList = new LinkedList<>();

        // PaperSpigot start - Player view distance API
        for (int k = i - entityPlayer.viewDistance; k <= i + entityPlayer.viewDistance; ++k) {
            for (int l = j - entityPlayer.viewDistance; l <= j + entityPlayer.viewDistance; ++l) {
                // PaperSpigot end
                chunkList.add(new ChunkCoordIntPair(k, l));
            }
        }

        chunkList.sort(new ChunkCoordComparator(entityPlayer));
        for (ChunkCoordIntPair pair : chunkList) {
            this.a(pair.x, pair.z, true).a(entityPlayer);
        }
        // CraftBukkit end

        this.managedPlayers.add(entityPlayer);
        this.updateRequired();
    }

    public void removePlayer(EntityPlayer entityPlayer) {
        super.removePlayer(entityPlayer);
        this.updateRequired();
    }

    public void movePlayer(EntityPlayer entityplayer) {
        int i = (int) entityplayer.locX >> 4;
        int j = (int) entityplayer.locZ >> 4;
        double d0 = entityplayer.d - entityplayer.locX;
        double d1 = entityplayer.e - entityplayer.locZ;
        double d2 = d0 * d0 + d1 * d1;

        if (d2 >= 64.0D) {
            int k = (int) entityplayer.d >> 4;
            int l = (int) entityplayer.e >> 4;
            int i1 = entityplayer.viewDistance; // Paper - Player view distance API

            int j1 = i - k;
            int k1 = j - l;

            List<ChunkCoordIntPair> chunksToLoad = new LinkedList<ChunkCoordIntPair>(); // CraftBukkit

            if (j1 != 0 || k1 != 0) {
                for (int l1 = i - i1; l1 <= i + i1; ++l1) {
                    for (int i2 = j - i1; i2 <= j + i1; ++i2) {
                        if (!this.a(l1, i2, k, l, i1)) {
                            chunksToLoad.add(new ChunkCoordIntPair(l1, i2)); // CraftBukkit
                        }

                        if (!this.a(l1 - j1, i2 - k1, i, j, i1)) {
                            AsyncPlayerChunk playerchunk = (AsyncPlayerChunk) this.fetchChunk(l1 - j1, i2 - k1, false);

                            if (playerchunk != null) {
                                playerchunk.b(entityplayer);
                            }
                        } else { // Paper start
                            AsyncPlayerChunk playerchunk = (AsyncPlayerChunk) this.fetchChunk(l1 - j1, i2 - k1, false);
                            if (playerchunk != null) {
                                playerchunk.checkHighPriority(entityplayer); // Paper
                            }
                        }
                        // Paper end
                    }
                }

                entityplayer.d = entityplayer.locX;
                entityplayer.e = entityplayer.locZ;
                this.updateRequired();

                // CraftBukkit start - send nearest chunks first
                Collections.sort(chunksToLoad, new ChunkCoordComparator(entityplayer));
                for (ChunkCoordIntPair pair : chunksToLoad) {
                    // Paper start
                    AsyncPlayerChunk c = this.fetchChunk(pair.x, pair.z, true);
                    c.checkHighPriority(entityplayer);
                    c.addPlayer(entityplayer);
                    // Paper end
                }
                // CraftBukkit end
            }
        }
    }

    public void updateRequired() {
        this.loadChunks = true;
        this.sendChunks = true;
    }

    public void setViewDistanceForAll(int i) {
        i = MathHelper.clamp(i, 3, 32);
        if (i != this.g) {
            List<EntityPlayer> list = Lists.newArrayList(this.managedPlayers);
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer) iterator.next();
                this.setViewDistance(entityplayer, i, false); // Paper - Split, don't mark sort pending, we'll handle it after
            }

            this.g = i;
            this.updateRequired();
        }
    }

    public void updateViewDistance(EntityPlayer player, int viewDistance) {
       this.setViewDistance(player, viewDistance, true);
    }

    public void setViewDistance(EntityPlayer player, int i, boolean markSort) {
        i = MathHelper.clamp(i, 3, 32);
        int oldViewDistance = player.viewDistance;
        if (i != oldViewDistance) {
            int j = i - oldViewDistance;

            int k = (int) player.locX >> 4;
            int l = (int) player.locZ >> 4;
            int i1;
            int j1;

            if (j > 0) {
                for (i1 = k - i; i1 <= k + i; ++i1) {
                    for (j1 = l - i; j1 <= l + i; ++j1) {
                        PlayerChunk playerchunk = this.fetchChunk(i1, j1, true);

                        if (!playerchunk.getB().contains(player)) {
                            playerchunk.a(player);
                        }
                    }
                }
            } else {
                for (i1 = k - oldViewDistance; i1 <= k + oldViewDistance; ++i1) {
                    for (j1 = l - oldViewDistance; j1 <= l + oldViewDistance; ++j1) {
                        if (!this.a(i1, j1, k, l, i)) {
                            this.a(i1, j1, true).b(player);
                        }
                    }
                }
                if (markSort) {
                    this.updateRequired();
                }
            }
        }
    }

    // Paper start - Player view distance API
    public void setViewDistance(EntityPlayer player, int distanceIn) {
        final int oldViewDistance = player.viewDistance;

        // This represents the view distance that we will set on the player
        // It can exist as a negative value
        int playerViewDistance = MathHelper.clamp(distanceIn, 3, 32);

        // This value is the one we actually use to update the chunk map
        // We don't ever want this to be a negative
        int toSet = playerViewDistance;

        if (distanceIn < 0) {
            playerViewDistance = -1;
            toSet = this.getViewDistance();
        }

        if (toSet != oldViewDistance) {
            // Order matters
            this.updateViewDistance(player, toSet);
            player.viewDistance = playerViewDistance;

            //Force update entity trackers
            this.getWorld().getTracker().updatePlayer(player);
        }
    }
    // Paper end

    @Override
    public void a(int i) {
        this.setViewDistanceForAll(i);
    }

    @Override
    protected AsyncPlayerChunk createChunk(int i, int j) {
        return new AsyncPlayerChunk(this, i, j);
    }

    @Override
    public PlayerChunkMap.PlayerChunk a(int i, int j, boolean flag) {
        return this.fetchChunk(i, j, flag);
    }

    /*@Override
    public boolean a(EntityPlayer entityplayer, int i, int j) {
        return this.isInChunk(entityplayer, i, j);
    }*/
}
