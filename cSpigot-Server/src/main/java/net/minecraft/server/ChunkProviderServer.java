package net.minecraft.server;

import com.destroystokyo.paper.utils.CachedSizeConcurrentLinkedQueue;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.github.paperspigot.event.ServerExceptionEvent;
import org.github.paperspigot.exception.ServerInternalException;
// CraftBukkit end

// cSpigot start
import io.github.cspigot.feature.chunk.collection.AsyncChunkMap;
import io.github.cspigot.feature.chunk.AsyncChunkProvider;
import io.github.cspigot.chunk.AsyncPriority;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.api.chunk.collection.LongQueueSet;
import io.github.cspigot.feature.chunk.collection.SyncChunkMap;
import io.github.cspigot.utils.collection.BukkitChunkMap;
// cSpigot end

public class ChunkProviderServer implements IChunkProvider {

    protected static final Logger b = LogManager.getLogger();
    public Collection<Long> unloadQueue;
    public Chunk emptyChunk;
    public IChunkProvider chunkProvider;
    public IChunkLoader chunkLoader; // FlamePaper - Make chunkloader public
    public boolean forceChunkLoad = false; // CraftBukkit - true -> false

    // cSpigot start - Make expected size configurable and use proper parent/child maps with caching.
    public Map<Long, Chunk> chunks = !SettingsConfig.ASYNC_CHUNKS ?
            new SyncChunkMap(SettingsConfig.CHUNK_MAP_SIZE, 0.5f) :
            new AsyncChunkMap(SettingsConfig.CHUNK_MAP_SIZE);

    public WorldServer world;

    // cSpigot - Sync lock for async provider
    protected final Object SYNC_LOCK = new Object(); // cSpigot
    // cSpigot end

    public ChunkProviderServer(WorldServer worldserver, IChunkLoader ichunkloader, IChunkProvider ichunkprovider) {
        this.emptyChunk = new EmptyChunk(worldserver, Integer.MIN_VALUE, Integer.MIN_VALUE); // Migot
        this.unloadQueue = SettingsConfig.BUKKIT_UNLOAD_QUEUE ? new LongQueueSet() : new CachedSizeConcurrentLinkedQueue<>();
        if (SettingsConfig.USE_BUKKIT_CHUNK_MAP) {
            this.chunks = new BukkitChunkMap<>();
        }
        this.world = worldserver;
        this.chunkLoader = ichunkloader;
        this.chunkProvider = ichunkprovider;
    }

    private ChunkRegionLoader checkedRegionLoader = null;
    public boolean doesChunkExist(int n, int n2) {
        if (this.checkedRegionLoader == null && this.chunkLoader instanceof ChunkRegionLoader) {
            this.checkedRegionLoader = (ChunkRegionLoader)this.chunkLoader;
        }
        if (this.checkedRegionLoader != null) {
            return this.checkedRegionLoader.chunkExists(this.world, n, n2);
        }
        return false;
    }

    public boolean isChunkLoaded(int i, int j) {
        long key = LongHash.toLong(i, j); // IonSpigot - Only create key once
        return this.chunks.get(key) != null;
    }

    public Collection<Chunk> a() {
        return this.chunks.values();
    }

    public void removeFromQueue(int i, int j) {
        long key = LongHash.toLong(i, j); // IonSpigot - Only create key once
        this.unloadQueue.remove(key);
    }

    public void queueUnload(int i, int j) {
        long key = LongHash.toLong(i, j); // IonSpigot - Only create key once
        Chunk chunk = chunks.get(key);
        // PaperSpigot start - Don't unload chunk if it contains an entity that loads chunks
        if (chunk != null) {
            for ( List<Entity> entities : chunk.entitySlices ) {
                for ( Entity entity : entities ) {
                    if (entity.loadChunks) {
                        return;
                    }
                }
            }
        }
        // PaperSpigot end
        if (this.world.worldProvider.e()) {
            if (!this.world.c(i, j)) {
                // CraftBukkit start
                this.unloadQueue.add(key);

                if (chunk != null) {
                    chunk.mustSave = true;
                }
                // CraftBukkit end
            }
        } else {
            // CraftBukkit start
            this.unloadQueue.add(key);

            if (chunk != null) {
                chunk.mustSave = true;
            }
            // CraftBukkit end
        }

    }

    public void b() {
        Iterator iterator = this.chunks.values().iterator();

        while (iterator.hasNext()) {
            Chunk chunk = (Chunk) iterator.next();

            this.queueUnload(chunk.locX, chunk.locZ);
        }

    }

    // cSpigot start - Backport Paper 1.13.2 chunk system
    public boolean chunkGoingToExists(int x, int z) {
        return ChunkIOExecutor.isQueuedToLoad(this.world, x, z);
    }

    public void bumpPriority(ChunkCoordIntPair coords) {
        // do nothing, override in async
    }

    public void bumpAreaPriority(ChunkCoordIntPair coords, int radius) {
        for ( ChunkCoordIntPair c : this.getSpiralOutChunks(coords.asPosition(), radius) ) {
            this.bumpPriority(c);
        }
    }

    public List<ChunkCoordIntPair> getSpiralOutChunks(BlockPosition blockposition, int radius) {
        List<ChunkCoordIntPair> list = new ArrayList<>();

        list.add(new ChunkCoordIntPair(blockposition.getX() >> 4, blockposition.getZ() >> 4));
        for ( int r = 1; r <= radius; r++ ) {
            int x = -r;
            int z = r;

            // Iterates the edge of half of the box; then negates for other half.
            while (x <= r && z > -r) {
                list.add(new ChunkCoordIntPair((blockposition.getX() + (x << 4)) >> 4, (blockposition.getZ() + (z << 4)) >> 4));
                list.add(new ChunkCoordIntPair((blockposition.getX() - (x << 4)) >> 4, (blockposition.getZ() - (z << 4)) >> 4));

                if (x < r) {
                    x++;
                } else {
                    z--;
                }
            }
        }
        return list;
    }

    public Chunk getChunkAt(int x, int z, boolean load, boolean gen, Consumer<Chunk> consumer) {
        return this.getChunkAt(x, z, load, gen, AsyncPriority.NORMAL, consumer);
    }

    public CompletableFuture<Chunk> getChunkAtFuture(int x, int z, boolean load, boolean gen, Consumer<Chunk> consumer) {
        return CompletableFuture.completedFuture(this.getChunkAt(x, z, load, gen, AsyncPriority.NORMAL, consumer));
    }

    public Chunk getChunkAt(int x, int z, boolean load, boolean gen, AsyncPriority priority, Consumer<Chunk> consumer) {
        return this.getChunkAt(x, z, consumer != null
                ? () -> consumer.accept(this.chunks.get(LongHash.toLong(x, z)))
                : null);
    }

    public CompletableFuture<Chunk> getChunkAtFuture(int x, int z, boolean load, boolean gen, AsyncPriority priority, Consumer<Chunk> consumer) {
        return CompletableFuture.completedFuture(this.getChunkAt(x, z, consumer != null
                ? () -> consumer.accept(this.chunks.get(LongHash.toLong(x, z)))
                : null));
    }

    public Chunk getChunkAt(int x, int z, boolean load, boolean gen) {
        return this.getChunkAt(x, z, null);
    }

    public AsyncChunkProvider.CancellableChunkRequest requestChunk(int x, int z, boolean gen, AsyncPriority priority, Consumer<Chunk> consumer) {
        Chunk chunk = this.getChunkAt(x, z, true, gen, priority, consumer);
        return new AsyncChunkProvider.CancellableChunkRequest() {
            @Override
            public void cancel() {

            }

            @Override
            public Chunk getChunk() {
                return chunk;
            }
        };
    }
    // cSpigot end

    // CraftBukkit start - Add async variant, provide compatibility
    public Chunk getChunkIfLoaded(int x, int z) {
        return chunks.get(LongHash.toLong(x, z));
    }

    public Chunk getChunkAt(int i, int j) {
        return getChunkAt(i, j, null);
    }

    // cSpigot start - Backport Paper 1.13.2 chunk system
    public Chunk getChunkAt(int i, int j, Runnable runnable) {
        return this.getChunkAt(i, j, true, true, runnable);
    }
    // cSpigot end

    public Chunk getChunkAt(int i, int j, boolean load, boolean gen, Runnable runnable) {
        long key = LongHash.toLong(i, j); // IonSpigot - Only create key once
        unloadQueue.remove(key);

        Chunk chunk = chunks.get(key);
        ChunkRegionLoader loader = null;

        if (this.chunkLoader instanceof ChunkRegionLoader) {
            loader = (ChunkRegionLoader) this.chunkLoader;

        }
        // We can only use the queue for already generated chunks
        if (load && chunk == null && loader != null && loader.chunkExists(world, i, j)) {
            if (runnable != null) {
                ChunkIOExecutor.queueChunkLoad(world, loader, this, i, j, runnable);
                return null;
            } else {
                chunk = ChunkIOExecutor.syncChunkLoad(world, loader, this, i, j);
            }
        } else if (gen && chunk == null) {
            chunk = originalGetChunkAt(i, j);
//        } else if (chunk == null) {
//            chunk = this.emptyChunk;
        }

        // If we didn't load the chunk async and have a callback run it now
        if (runnable != null) {
            runnable.run();
        }

        return chunk;
    }

    // cSpigot start - Optimize Chunk getting
    public Chunk getChunkAtNotCreate(int i, int j) {
        long l = LongHash.toLong(i, j);

        unloadQueue.remove(l); // TacoSpigot - directly invoke LongHash

        Chunk chunk = chunks.get(l);
        ChunkRegionLoader loader = null;

        if (this.chunkLoader instanceof ChunkRegionLoader) {
            loader = (ChunkRegionLoader) this.chunkLoader;
        }
        // We can only use the queue for already generated chunks
        if (chunk == null && loader != null && loader.chunkExists(world, i, j)) {
            chunk = ChunkIOExecutor.syncChunkLoad(world, loader, this, i, j);
        }

        return chunk;
    }
    // cSpigot end

    public Chunk originalGetChunkAt(int i, int j) {
        long key = LongHash.toLong(i, j); // IonSpigot - Only create key once
        this.unloadQueue.remove(key);

        Chunk chunk = this.chunks.get(key);
        boolean newChunk = false;
        // CraftBukkit end

        if (chunk == null) {
            chunk = this.loadChunk(i, j);
            if (chunk == null) {
                if (this.chunkProvider == null) {
                    chunk = this.emptyChunk;
                } else {
                    try {
                        chunk = this.chunkProvider.getOrCreateChunk(i, j);
                    } catch (Throwable throwable) {
                        throw this.generateChunkError(i, j, throwable); // cSpigot - move to method
                    }
                }
                newChunk = true; // CraftBukkit
            }

            this.chunks.put(key, chunk);
            this.postChunk(chunk, newChunk, false, i, j);
        }

        return chunk;
    }

    public void postChunk(Chunk chunk, boolean isNew, boolean recreateStructures, int locX, int locZ) {
        chunk.addEntities();

        if (recreateStructures && this.chunkProvider != null) {
            this.chunkProvider.recreateStructures(chunk, chunk.locX, chunk.locZ);
        }

        // CraftBukkit start
        Server server = world.getServer();
        if (server != null && chunk.bukkitChunk != null) {
            /*
             * If it's a new world, the first few chunks are generated inside
             * the World constructor. We can't reliably alter that, so we have
             * no way of creating a CraftWorld/CraftServer at that point.
             */
            server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, isNew));
        }

        // Update neighbor counts
        for ( int x = -2; x < 3; x++ ) {
            for ( int z = -2; z < 3; z++ ) {
                if (x == 0 && z == 0) {
                    continue;
                }

                Chunk neighbor = this.getChunkIfLoaded(chunk.locX + x, chunk.locZ + z);
                if (neighbor != null) {
                    neighbor.setNeighborLoaded(-x, -z);
                    chunk.setNeighborLoaded(x, z);
                }
            }
        }
        // CraftBukkit end
        chunk.loadNearby(this, this, locX, locZ);
        //world.timings.syncChunkLoadTimer.stopTiming(); // Spigot
    }

    public Chunk getOrCreateChunk(int i, int j) {
        // CraftBukkit start
        Chunk chunk = this.chunks.get(LongHash.toLong(i, j));
        chunk = chunk == null ? (!this.world.ad() && !this.forceChunkLoad ? this.emptyChunk : this.getChunkAt(i, j)) : chunk;

        if (chunk == emptyChunk) return chunk;

        if (i != chunk.locX || j != chunk.locZ) {
            // Paper start
            String msg = "Chunk (" + chunk.locX + ", " + chunk.locZ + ") stored at  (" + i + ", " + j + ") in world '" + world.getWorld().getName() + "'";
            b.error(msg);
            b.error(chunk.getClass().getName());
            ServerInternalException ex = new ServerInternalException(msg);
            ex.printStackTrace();
            Bukkit.getPluginManager().callEvent(new ServerExceptionEvent(ex));
            // Paper end
        }
        return chunk;
        // CraftBukkit end
    }

    // cSpigot start - Optimize Player movement
    public Chunk getChunkNotCreate(int i, int j) {
        // CraftBukkit start
        long l = LongHash.toLong(i, j);
        Chunk chunk = this.chunks.get(l);
        chunk = chunk == null ? ((!this.world.ad() && !this.forceChunkLoad) ? this.emptyChunk : this.getChunkAtNotCreate(i, j)) : chunk;

        if (chunk == null) chunk = this.emptyChunk;

        if (chunk == emptyChunk) return chunk;
        if (i != chunk.locX || j != chunk.locZ) {
            b.error("Chunk (" + chunk.locX + ", " + chunk.locZ + ") stored at  (" + i + ", " + j + ") in world '" + world.getWorld().getName() + "'");
            b.error(chunk.getClass().getName());
            Throwable ex = new Throwable();
            ex.fillInStackTrace();
            ex.printStackTrace();
        }

        return chunk;
        // CraftBukkit end
    }
    // cSpigot end

    public Chunk loadChunk(int i, int j) {
        if (this.chunkLoader == null) {
            return null;
        } else {
            try {
                Chunk chunk = this.chunkLoader.a(this.world, i, j);

                if (chunk != null) {
                    chunk.setLastSaved(this.world.getTime());
                    if (this.chunkProvider != null) {
                        //world.timings.syncChunkLoadStructuresTimer.startTiming(); // Spigot // Purpur
                        this.chunkProvider.recreateStructures(chunk, i, j);
                        //world.timings.syncChunkLoadStructuresTimer.stopTiming(); // Spigot // Purpur
                    }
                }

                return chunk;
            } catch (Exception exception) {
                // Paper start
                String msg = "Couldn\'t load chunk";
                ChunkProviderServer.b.error(msg, exception);
                ServerInternalException.reportInternalException(exception);
                // Paper end
                return null;
            }
        }
    }

    // cSpigot start
    protected ReportedException generateChunkError(int x, int z, Throwable throwable) {
        return a(x, z, throwable);
    } // Paper - OBFHELPER

    private ReportedException a(int i, int j, Throwable throwable) {
        CrashReport crashreport = CrashReport.a(throwable, "Exception generating new chunk");
        CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Chunk to be generated");

        crashreportsystemdetails.a("Location", String.format("%d,%d", i, j));
        crashreportsystemdetails.a("Position hash", LongHash.toLong(i, j));
        crashreportsystemdetails.a("Generator", this.chunkProvider.getName());
        return new ReportedException(crashreport);
    }

    public void saveChunkNOP(Chunk chunk) {
        if (canSave() && this.chunkLoader != null) {
            try {
                this.chunkLoader.b(this.world, chunk);
            } catch (Exception exception) {
                ChunkProviderServer.b.error("Couldn't save entities", exception);
            }

        }
    }

    public void saveChunk(Chunk chunk) {
        if (canSave() && this.chunkLoader != null) {
            try {
                chunk.setLastSaved(this.world.getTime());
                this.chunkLoader.a(this.world, chunk);
            } catch (IOException ioexception) {
                ChunkProviderServer.b.error("Couldn't save chunk", ioexception);
            } catch (ExceptionWorldConflict exceptionworldconflict) {
                ChunkProviderServer.b.error("Couldn't save chunk; already in use by another instance of Minecraft?", exceptionworldconflict);
            }

        }
    }

    public void getChunkAt(IChunkProvider ichunkprovider, int i, int j) {
        Chunk chunk = this.getOrCreateChunk(i, j);
        // cSpigot start
        if (!chunk.isDone()) {
            chunk.populate(this, this.chunkProvider);
        }
        // cSpigot end
    }

    public boolean a(IChunkProvider ichunkprovider, Chunk chunk, int i, int j) {
        return chunk.decorateStructures(this, this.chunkProvider); // cSpigot
    }

    public boolean saveChunks(boolean flag, IProgressUpdate iprogressupdate) {
        int i = 0;
        // CraftBukkit start
        Iterator iterator = this.chunks.values().iterator();
        while (iterator.hasNext()) {
            Chunk chunk = (Chunk) iterator.next();
            // CraftBukkit end

            if (flag) {
                this.saveChunkNOP(chunk);
            }

            if (chunk.a(flag)) {
                this.saveChunk(chunk);
                chunk.f(false);
                ++i;
            }
        }
        return true;
    }

    public void c() {
        IChunkLoader ichunkloader = this.chunkLoader;
        if (ichunkloader == null) return;
        //synchronized (SYNC_LOCK) {
        this.chunkLoader.b();
        //}
    }

    public boolean unloadChunks(BooleanSupplier booleansupplier) {
        if (!this.world.savingDisabled && (canSave())) {
            if (!this.unloadQueue.isEmpty()) {
                // CraftBukkit start
                Server server = this.world.getServer();
                // SportPaper start
                Iterator<Long> iterator = unloadQueue.iterator();
                for (int i = 0; i < 100 && iterator.hasNext(); ++i) {
                    long key = iterator.next();
                    iterator.remove(); // Spigot

                    Chunk chunk = this.chunks.get(key);
                    if (chunk == null || !chunk.canExpire()) continue;

                    // cSpigot - Don't unload a chunk that's in use
//                    if (world.getPlayerChunkMap().isChunkInUse(chunk.locX, chunk.locZ)) {
//                        continue;
//                    }

                    ChunkUnloadEvent event = new ChunkUnloadEvent(chunk.bukkitChunk);
                    server.getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        chunk.lightingQueue.processUnload();

                        // Update neighbor counts
                        for (int x = -2; x < 3; x++) {
                            for (int z = -2; z < 3; z++) {
                                if (x == 0 && z == 0) {
                                    continue;
                                }

                                Chunk neighbor = this.chunks.get(LongHash.toLong(chunk.locX + x, chunk.locZ + z));
                                if (neighbor != null) {
                                    neighbor.setNeighborUnloaded(-x, -z);
                                    chunk.setNeighborUnloaded(x, z);
                                }
                            }
                        }

//                        // cSpigot - Clean up PlayerChunkMap references
//                        PlayerChunkMap playerchunkmap = this.world.getPlayerChunkMap();
//                        PlayerChunkMap.PlayerChunk playerChunk = playerchunkmap.getChunks().get(PlayerChunkMap.d(chunk.locX, chunk.locZ));
//                        if (playerChunk != null) {
//                            playerChunk.loaded = false;
//                        }

                        chunk.removeEntities();
                        this.saveChunk(chunk);
                        this.saveChunkNOP(chunk);
                        this.chunks.remove(key); // CraftBukkit
                        // cSpigot end
                    }
                }
                // CraftBukkit end
            }

            if (this.chunkLoader != null) {
                this.chunkLoader.a();
            }
        }

        return this.chunkProvider.unloadChunks();
    }

    public boolean unloadChunks() {
        return this.unloadChunks(() -> true);
    }

    public boolean canSave() {
        return !this.world.savingDisabled;
    }

    public String getName() {
        // CraftBukkit - this.chunks.count() -> .size()
        return "ServerChunkCache: " + this.chunks.size() + " Drop: " + this.unloadQueue.size();
    }

    public List<BiomeBase.BiomeMeta> getMobsFor(EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        return this.chunkProvider.getMobsFor(enumcreaturetype, blockposition);
    }

    public BlockPosition findNearestMapFeature(World world, String s, BlockPosition blockposition) {
        return this.chunkProvider.findNearestMapFeature(world, s, blockposition);
    }

    public int getLoadedChunks() {
        // CraftBukkit - this.chunks.count() -> this.chunks.size()
        return this.chunks.size();
    }

    public void recreateStructures(Chunk chunk, int i, int j) {
    }

    public Chunk getChunkAt(BlockPosition blockposition) {
        return this.getOrCreateChunk(blockposition.getX() >> 4, blockposition.getZ() >> 4);
    }

    public void close(boolean save) {
        if (!save) {
            this.world.savingDisabled = true;
        }

        this.unloadChunks(() -> true);

        this.chunks.clear();
        this.chunkProvider = null;
        this.chunkLoader = null;
    }
}
