package io.github.cspigot.feature.chunk.player;

import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import net.minecraft.server.*;

import io.github.cspigot.chunk.AsyncPriority;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.chunk.AsyncChunkProvider;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 3/8/2024
 */

@Log4j2
public class AsyncPlayerChunk extends PlayerChunkMap.PlayerChunk {

    private final AsyncPlayerChunkMap chunkMap;

    /**
     * The consumer accepted on this player chunk's chunk.
     */
    private Consumer<Chunk> loadedConsumer;
    /**
     * The chunk request sent to {@link AsyncChunkProvider}
     */
    @Getter private AsyncChunkProvider.CancellableChunkRequest chunkRequest;
    /**
     * Is this chunk marked high priority for loading?
     */
    private boolean markedHigh = false;
    /**
     * Does the chunk exist or will it have to generate?
     */
    boolean chunkExists;
    /**
     * Have we loaded/generated and sent the chunk once?
     */
    public boolean done; // Paper - package-private

    /**
     * The main constructor for our AsyncPlayerChunk.
     *
     * @param playerChunkMap {@link AsyncPlayerChunkMap map}
     * @param x              {@link Integer} Location X
     * @param z              {@link Integer} Location Z
     */
    public AsyncPlayerChunk(AsyncPlayerChunkMap playerChunkMap, int x, int z) {
        super(playerChunkMap, x, z);

        this.chunkMap = playerChunkMap;
        this.b = this.getB();
    }

    public void loadChunk(int i, int j) {
        ChunkProviderServer chunkProvider = playerChunkMap.getWorld().chunkProviderServer;
        if (this.loadedConsumer == null) {
            this.loadedConsumer = (chunk) -> {
                this.chunkRequest = null;
                this.setChunk(chunk);
            };
        }
        chunkProvider.removeFromQueue(i, j);
        this.chunk = chunkProvider.getChunkAt(i, j, true, true, this.loadedConsumer);
        this.chunkExists = this.chunk != null || chunkProvider.chunkGoingToExists(i, j); // Paper
    }

    /**
     * Check if this chunk requires high priority.
     *
     * @param player {@link EntityPlayer} player.
     */
    public void checkHighPriority(EntityPlayer player) {
        if (this.done || this.markedHigh || this.chunk != null) {
            return;
        }
        final double dist = this.getDistance(player.locX, player.locZ);
        if (dist > 8) {
            return;
        }
        if (dist >= 3 && this.getDistance(player, 5) > 3.5) {
            return;
        }

        this.markedHigh = true;
        this.playerChunkMap.getWorld().chunkProviderServer.bumpPriority(this.location);
        if (this.chunkRequest == null) {
            this.requestChunkIfNeeded(PlayerChunkMap.CAN_GEN_CHUNKS.test(player));
        }
    }

    /**
     * Add the given player to the Player Chunk.
     *
     * @param entityPlayer {@link EntityPlayer} player.
     */
    public void addPlayer(EntityPlayer entityPlayer) { // CraftBukkit - added final to argument
        if (this.b.contains(entityPlayer)) {
            log.debug("Failed to add player. {} already is in chunk {}, {}", entityPlayer, this.location.x, this.location.z);
            return;
        }

        if (this.b.isEmpty()) {
            this.g = playerChunkMap.getWorld().getTime();
        }

        this.b.add(entityPlayer);
        if (this.done) {
            this.sendChunk(entityPlayer);
        } else {
            this.checkHighPriority(entityPlayer);
            ChunkProviderServer chunkProviderServer = this.chunkMap.getWorld().chunkProviderServer;
            Consumer<Chunk> consumer = (chunk) -> {
                loadedConsumer.accept(chunk);
                this.done = true;
                this.sendChunk(entityPlayer);
            };
            if (SettingsConfig.GENERATION_FRIENDLY_CHUNKS) {
                chunkProviderServer.getChunkAtFuture(this.location.x, this.location.z, true, true, AsyncPriority.URGENT, consumer); // run it async and non-blocking coz yes.
            } else {
                chunkProviderServer.getChunkAt(this.location.x, this.location.z, true, true, AsyncPriority.URGENT, consumer);
            }
            // cSpigot end
        }
    }

    /**
     * Remove the given player from the Player Chunk.
     *
     * @param entityPlayer {@link EntityPlayer} player.
     */
    public void removePlayer(EntityPlayer entityPlayer) {
        if (!this.b.contains(entityPlayer)) return;

        if (this.done) {
            entityPlayer.playerConnection.sendPacket(PacketPlayOutMapChunk.unload(this.location.x, this.location.z));
            Chunk chunk = this.chunk;
            boolean ready = chunk != null && chunk.isReady();
            if (ready) {
                if (!(chunk instanceof EmptyChunk) && PlayerChunkUnloadEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    new PlayerChunkUnloadEvent(chunk.bukkitChunk, entityPlayer.getBukkitEntity()).callEvent();
                }
            }
        }

        this.b.remove(entityPlayer);
        if (this.b.isEmpty()) {
            long i = this.location.x + 2147483647L | this.location.z + 2147483647L << 32;
            this.saveChunk();

            this.playerChunkMap.d.remove(i);
            this.playerChunkMap.f.remove(this);
            this.chunkMap.loadingChunks.remove(this);
            this.chunkMap.sendingChunks.remove(this);

            if (this.dirtyCount > 0) {
                this.playerChunkMap.e.remove(this);
            }
            this.playerChunkMap.a().chunkProviderServer.queueUnload(this.location.x, this.location.z);
        }
    }

    /**
     * Mark this chunk as last saved now.
     */
    private void saveChunk() {
        long i = this.playerChunkMap.getWorld().getTime();

        if (this.chunk != null) {
            chunk.c(chunk.w() + i - this.g);
        }

        this.g = i;
    }

    /**
     * Request a chunk to be loaded/generated if needed.
     *
     * @param flag {@link Boolean} Should generate?
     */
    private void requestChunkIfNeeded(boolean flag) {
        if (chunkRequest == null) {
            chunkRequest = this.playerChunkMap.getWorld().chunkProviderServer.requestChunk(this.location.x, this.location.z, flag, markedHigh ? AsyncPriority.URGENT : AsyncPriority.HIGH, loadedConsumer);
            this.chunk = chunkRequest.getChunk(); // Paper)
        }
    }

    public void markDirty(int i, int j, int k) {
        if (!this.done) return;

        if (this.dirtyCount == 0) {
            this.playerChunkMap.e.add(this);
        }

        this.f |= 1 << (j >> 4);
        if (this.dirtyCount < 64) {
            short short0 = (short) (i << 12 | k << 8 | j);

            for (short l = 0; l < this.dirtyCount; ++l) {
                if (this.dirtyBlocks.get(l) == short0) {
                    return;
                }
            }
            this.dirtyBlocks.put((short) this.dirtyCount++, short0);
        }
    }

    public void b() {
        if (!this.done || chunk == null) return;

        WorldServer worldServer = playerChunkMap.getWorld();
        if (this.dirtyCount != 0) {
            int i;
            int j;
            int k;

            if (this.dirtyCount == 1) {
                // cSpigot start - use fast util
                i = (this.dirtyBlocks.get((short) 0) >> 12 & 15) + this.location.x * 16;
                j = this.dirtyBlocks.get((short) 0) & 255;
                k = (this.dirtyBlocks.get((short) 0) >> 8 & 15) + this.location.z * 16;
                // cSpigot end
                BlockPosition blockposition = new BlockPosition(i, j, k);
                this.broadcastPacket(new PacketPlayOutBlockChange(worldServer, blockposition));
                if (worldServer.getType(blockposition).getBlock().isTileEntity()) {
                    this.updateTileEntity(worldServer.getTileEntity(blockposition));
                }
            } else {
                if (this.dirtyCount == 64) {
                    this.broadcastPacket(new PacketPlayOutMapChunk(chunk, false, this.f));

                    i = this.location.x * 16;
                    j = this.location.z * 16;
                    for (k = 0; k < 16; ++k) {
                        if ((this.f & 1 << k) != 0) {
                            int l = k << 4;
                            List<TileEntity> list = worldServer.getTileEntities(i, l, j, i + 16, l + 16, j + 16);
                            for (TileEntity tileEntity : list) {
                                this.updateTileEntity(tileEntity);
                            }
                        }
                    }
                } else {
                    this.broadcastPacket(new PacketPlayOutMultiBlockChange(this.dirtyCount, this.dirtyBlocks, chunk));

                    for (i = 0; i < this.dirtyCount; ++i) {
                        // cSpigot start - use fast util
                        j = (this.dirtyBlocks.get((short) i) >> 12 & 15) + this.location.x * 16;
                        k = this.dirtyBlocks.get((short) i) & 255;
                        int l = (this.dirtyBlocks.get((short) i)>> 8 & 15) + this.location.z * 16;
                        // cSpigot end

                        BlockPosition blockposition1 = new BlockPosition(j, k, l);
                        if (worldServer.getType(blockposition1).getBlock().isTileEntity()) {
                            this.updateTileEntity(worldServer.getTileEntity(blockposition1));
                        }
                    }
                }
            }

            this.dirtyCount = 0;
            this.f = 0;
        }
    }

    public boolean sendAll() {
        if (this.done) {
            return true;
        } else if (this.chunk == null) {
            return false;
        } else if (!this.chunk.isReady()) {
            return false;
        }/* else if (!this.chunk.world.chunkPacketBlockController.onChunkPacketCreate(this.chunk, '\uffff', false)) { // Paper - Anti-Xray - Load nearby chunks if necessary
            return false; // Paper - Anti-Xray - Wait and try again later
        }*/ else {
            this.dirtyCount = 0;
            this.f = 0;
            this.done = true;
            if (!this.players.isEmpty()) {
                //Packet<?> packet = new PacketPlayOutMapChunk(this.chunk, true, 65535);
                Iterator iterator = this.b.iterator();

                while (iterator.hasNext()) {
                    EntityPlayer entityplayer = (EntityPlayer) iterator.next();

                    entityplayer.chunkCoordIntPairQueue.add(this.location);
                    //this.updateTracker(entityplayer);
                }

            }
            return true;
        }
    }

    private void broadcastPacket(Packet<?> packet) {
        if (!this.done) return;

        for ( EntityPlayer entityPlayer : this.b ) {
            if (!entityPlayer.chunkCoordIntPairQueue.contains(this.location)) {
                entityPlayer.playerConnection.sendPacket(packet);
            }
        }
    }

    private void updateTileEntity(TileEntity tileentity) {
        if (tileentity == null) return;

        Packet<?> packet = tileentity.getUpdatePacket();
        if (packet == null) return;

        this.broadcastPacket(packet);
    }

    private void sendChunk(EntityPlayer entityPlayer) {
        if (!done) return;

        if (!entityPlayer.chunkCoordIntPairQueue.contains(this.location)) {
            entityPlayer.chunkCoordIntPairQueue.add(this.location);
        }
    }

    public double radius() {
        double d0 = Double.MAX_VALUE;

        for ( EntityPlayer entityplayer : this.b ) {
            double d1 = this.location.a(entityplayer);

            if (d1 < d0) {
                d0 = d1;
            }
        }

        return d0;
    }

    public boolean test(Predicate<EntityPlayer> predicate) {
        return this.b.stream().anyMatch(predicate);
    }

    public boolean loadChunk(boolean flag) {
        if (this.chunk != null) {
            return true;
        } else {
            // Paper start - async chunks
            requestChunkIfNeeded(flag);
            // Paper end
            return this.chunk != null;
        }
    }


    private double getDistance(EntityPlayer player, int inFront) {
        final float yaw = MathHelper.normalizeYaw(player.yaw);
        final double x = player.locX + (inFront * Math.cos(Math.toRadians(yaw)));
        final double z = player.locZ + (inFront * Math.sin(Math.toRadians(yaw)));
        return this.getDistance(x, z);
    }

    private double getDistance(double blockX, double blockZ) {
        final double x = this.location.x - ((int)Math.floor(blockX) >> 4);
        final double z = this.location.z - ((int)Math.floor(blockZ) >> 4);
        return Math.sqrt((x * x) + (z * z));
    }

    @Override
    public void a(EntityPlayer entityPlayer) {
        this.addPlayer(entityPlayer);
    }

    @Override
    public void b(EntityPlayer entityPlayer) {
        this.removePlayer(entityPlayer);
    }

    @Override
    public void a(TileEntity tileEntity) {
        this.updateTileEntity(tileEntity);
    }

    @Override
    public void a(Packet<?> packet) {
        this.broadcastPacket(packet);
    }

    @Override
    public void a(int i, int j, int k) {
        this.markDirty(i, j, k);
    }

    // For FAWE and other plugins hooking in to this class.
    private final List<EntityPlayer> b;
}
