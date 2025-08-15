package net.minecraft.server;

import com.google.common.collect.ImmutableList;

import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ShortArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortMap;

import lombok.Getter;
import lombok.Setter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.chunk.AsyncChunkProvider;
import io.github.cspigot.feature.chunk.player.AsyncPlayerChunk;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class PlayerChunkMap {

    private static final Predicate<EntityPlayer> b = (entityplayer) -> entityplayer != null && (!entityplayer.isSpectator() || entityplayer.world.getGameRules().getBoolean("spectatorsGenerateChunks"));
    public static final Predicate<EntityPlayer> CAN_GEN_CHUNKS = b; // Paper - OBFHELPER

    private static final Logger a = LogManager.getLogger();
    private final WorldServer world;
    protected final List<EntityPlayer> managedPlayers = new ObjectArrayList<>(); // cSpigot - ArrayList -> ObjectArrayList

    public final LongHashMap<PlayerChunkMap.PlayerChunk> d = new LongHashMap<>(SettingsConfig.CHUNK_MAP_SIZE);
    public LongHashMap<PlayerChunkMap.PlayerChunk> getChunks() { return this.d; } // Paper - OBFHELPER

    public final Queue<PlayerChunkMap.PlayerChunk> e = new ConcurrentLinkedQueue<>(); // updatingChunks
    public final List<PlayerChunkMap.PlayerChunk> f = new ObjectArrayList<>(); // visibleChunks
    public int g; public int getViewDistance() { return g; } // Paper OBFHELPER
    protected long h;
    private final int[][] i = new int[][] { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    protected boolean wasNotEmpty; // CraftBukkit - add field

    public PlayerChunkMap(WorldServer worldserver, int viewDistance /* Spigot */) {
        this.world = worldserver;
        this.a(viewDistance); // Spigot
    }

    public WorldServer getWorld() { return this.a(); } // cSpigot - OBFHELPER

    /**
     * Returns the world
     */
    public WorldServer a() {
        return this.world;
    }

//    public Iterator<Chunk> iterator() {
//        final Iterator<PlayerChunk> iterator = this.f.iterator();
//
//        return new AbstractIterator<Chunk>() {
//            protected Chunk computeNext() {
//                while (true) {
//                    if (iterator.hasNext()) {
//                        PlayerChunk playerchunk = iterator.next();
//                        Chunk chunk = playerchunk.getChunk();
//
//                        if (chunk == null) {
//                            continue;
//                        }
//
//                        if (!chunk.isSetup()) {
//                            return chunk;
//                        }
//
//                        if (!playerchunk.a(128.0D, PREDICATE)) {
//                            continue;
//                        }
//
//                        return chunk;
//                    }
//
//                    return (Chunk) this.endOfData();
//                }
//            }
//        };
//    }

    public void flush() {
        long i = this.world.getTime();
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

        // this.e.clear(); // CraftBukkit - Removals are already covered
        if (this.managedPlayers.isEmpty()) {
            if (!wasNotEmpty) {
                return; // CraftBukkit - Only do unload when we go from non-empty to empty
            }
            WorldProvider worldprovider = this.world.worldProvider;

            if (!worldprovider.e()) {
                this.world.chunkProviderServer.b();
            }
            // CraftBukkit start
            wasNotEmpty = false;
        } else {
            wasNotEmpty = true;
        }
        // CraftBukkit end
    }


    public boolean chunkExists(int i, int j) {
        return a(i, j);
    }

    public boolean a(int i, int j) {
        long key = getChunkKey(i, j);
        return this.d.get(key) != null;
    }

    /**
     * Fetch chunk, can be force loaded
     */
    public PlayerChunkMap.PlayerChunk a(int i, int j, boolean flag) {
        long k = d(i, j);
        PlayerChunkMap.PlayerChunk playerchunkmap_playerchunk = this.d.get(k);

        if (playerchunkmap_playerchunk == null && flag) {
            playerchunkmap_playerchunk = this.createChunk(i, j);
            this.d.put(k, playerchunkmap_playerchunk);
            this.f.add(playerchunkmap_playerchunk);
        }

        return playerchunkmap_playerchunk;
    }

    // cSpigot start - Add async chunks support
    protected PlayerChunk createChunk(int i, int j) {
        return new PlayerChunkMap.PlayerChunk(this, i, j);
    }
    // cSpigot end

    // CraftBukkit start - add method
    public final boolean isChunkInUse(int x, int z) {
        PlayerChunk pi = a(x, z, false);
        if (pi != null) {
            return (pi.b.size() > 0);
        }
        return false;
    }
    // CraftBukkit end

    public void flagDirty(BlockPosition blockposition) {
        int i = blockposition.getX() >> 4;
        int j = blockposition.getZ() >> 4;
        PlayerChunkMap.PlayerChunk chunk = this.a(i, j, false);

        if (chunk != null) {
            chunk.a(blockposition.getX() & 15, blockposition.getY(), blockposition.getZ() & 15);
        }
    }

    public void addPlayer(EntityPlayer entityplayer) {
        int i = (int) entityplayer.locX >> 4;
        int j = (int) entityplayer.locZ >> 4;

        entityplayer.d = entityplayer.locX;
        entityplayer.e = entityplayer.locZ;

        // CraftBukkit start - Load nearby chunks first
        List<ChunkCoordIntPair> chunkList = new LinkedList<>();

        // PaperSpigot start - Player view distance API
        for (int k = i - entityplayer.viewDistance; k <= i + entityplayer.viewDistance; ++k) {
            for (int l = j - entityplayer.viewDistance; l <= j + entityplayer.viewDistance; ++l) {
                // PaperSpigot end
                chunkList.add(new ChunkCoordIntPair(k, l));
            }
        }

        chunkList.sort(new ChunkCoordComparator(entityplayer));
        for (ChunkCoordIntPair pair : chunkList) {
            this.a(pair.x, pair.z, true).a(entityplayer);
        }
        // CraftBukkit end

        this.managedPlayers.add(entityplayer);
        this.b(entityplayer);
    }

    public void b(EntityPlayer entityplayer) {
        // Nacho - no need for an ArrayList if the contents aren't modified
        ImmutableList<ChunkCoordIntPair> arraylist = ImmutableList.copyOf(entityplayer.chunkCoordIntPairQueue);
        int i = 0;
        int j = entityplayer.viewDistance; // PaperSpigot - Player view distance API
        int k = (int) entityplayer.locX >> 4;
        int l = (int) entityplayer.locZ >> 4;
        int i1 = 0;
        int j1 = 0;
        ChunkCoordIntPair chunkcoordintpair = this.a(k, l, true).location;

        entityplayer.chunkCoordIntPairQueue.clear();
        if (arraylist.contains(chunkcoordintpair)) {
            entityplayer.chunkCoordIntPairQueue.add(chunkcoordintpair);
        }

        int k1;

        for (k1 = 1; k1 <= j * 2; ++k1) {
            for (int l1 = 0; l1 < 2; ++l1) {
                int[] aint = this.i[i++ % 4];

                for (int i2 = 0; i2 < k1; ++i2) {
                    i1 += aint[0];
                    j1 += aint[1];
                    chunkcoordintpair = this.a(k + i1, l + j1, true).location;
                    if (arraylist.contains(chunkcoordintpair)) {
                        entityplayer.chunkCoordIntPairQueue.add(chunkcoordintpair);
                    }
                }
            }
        }

        i %= 4;

        for (k1 = 0; k1 < j * 2; ++k1) {
            i1 += this.i[i][0];
            j1 += this.i[i][1];
            chunkcoordintpair = this.a(k + i1, l + j1, true).location;
            if (arraylist.contains(chunkcoordintpair)) {
                entityplayer.chunkCoordIntPairQueue.add(chunkcoordintpair);
            }
        }
    }

    public void removePlayer(EntityPlayer entityplayer) {
        int i = (int) entityplayer.d >> 4;
        int j = (int) entityplayer.e >> 4;

        // PaperSpigot start - Player view distance API
        for (int k = i - entityplayer.viewDistance; k <= i + entityplayer.viewDistance; ++k) {
            for (int l = j - entityplayer.viewDistance; l <= j + entityplayer.viewDistance; ++l) {
                // PaperSpigot end
                PlayerChunkMap.PlayerChunk chunk = this.a(k, l, false);

                if (chunk != null) {
                    chunk.b(entityplayer);
                }
            }
        }

        this.managedPlayers.remove(entityplayer);
    }

    protected boolean a(int i, int j, int k, int l, int i1) {
        int j1 = i - k;
        int k1 = j - l;

        if (j1 >= -i1 && j1 <= i1) {
            return k1 >= -i1 && k1 <= i1;
        }
        return false;
    }

    public void movePlayer(EntityPlayer entityplayer) {
        int i = (int) entityplayer.locX >> 4;
        int j = (int) entityplayer.locZ >> 4;
        double diffX = entityplayer.d - entityplayer.locX;
        double diffZ = entityplayer.e - entityplayer.locZ;
        double length2D = diffX * diffX + diffZ * diffZ;

        if (length2D >= 64.0D) {
            int k = (int) entityplayer.d >> 4;
            int l = (int) entityplayer.e >> 4;
            int i1 = entityplayer.viewDistance; // PaperSpigot - Player view distance API
            int j1 = i - k;
            int k1 = j - l;
            List<ChunkCoordIntPair> chunksToLoad = new LinkedList<>(); // CraftBukkit

            if (j1 != 0 || k1 != 0) {
                for (int l1 = i - i1; l1 <= i + i1; ++l1) {
                    for (int i2 = j - i1; i2 <= j + i1; ++i2) {
                        if (!this.a(l1, i2, k, l, i1)) {
                            chunksToLoad.add(new ChunkCoordIntPair(l1, i2)); // CraftBukkit
                        }
                        if (!this.a(l1 - j1, i2 - k1, i, j, i1)) {
                            PlayerChunkMap.PlayerChunk playerChunk = this.a(l1 - j1, i2 - k1, false);
                            if (playerChunk != null) {
                                playerChunk.b(entityplayer);
                            }
                        }
                    }
                }

                this.b(entityplayer);
                entityplayer.d = entityplayer.locX;
                entityplayer.e = entityplayer.locZ;

                // CraftBukkit start - send nearest chunks first
                chunksToLoad.sort(new ChunkCoordComparator(entityplayer));
                for (ChunkCoordIntPair pair : chunksToLoad) {
                    // Paper start
                    PlayerChunk playerChunk = this.a(pair.x, pair.z, true);
                    playerChunk.a(entityplayer);
                    // Paper end
                }

                if (j1 > 1 || j1 < -1 || k1 > 1 || k1 < -1) { // Spigot - missed diff
                    entityplayer.chunkCoordIntPairQueue.sort(new ChunkCoordComparator(entityplayer));
                }
                // CraftBukkit end
            }
        }
    }

    public boolean a(EntityPlayer entityplayer, int i, int j) {
        PlayerChunkMap.PlayerChunk chunk = this.a(i, j, false);

        return chunk != null && chunk.b.contains(entityplayer) && !entityplayer.chunkCoordIntPairQueue.contains(chunk.location);
    }

    public void a(int i) {
        i = MathHelper.clamp(i, 3, 32);
        if (i != this.g) {
            int j = i - this.g;
            // Nacho - no need for an ArrayList if the contents aren't modified
            ImmutableList<EntityPlayer> arraylist = ImmutableList.copyOf(this.managedPlayers);

            for (EntityPlayer entityplayer : arraylist) {
                int k = (int) entityplayer.locX >> 4;
                int l = (int) entityplayer.locZ >> 4;
                int i1;
                int j1;

                if (j > 0) {
                    for (i1 = k - i; i1 <= k + i; ++i1) {
                        for (j1 = l - i; j1 <= l + i; ++j1) {
                            PlayerChunk chunk = this.a(i1, j1, true);
                            if (!chunk.b.contains(entityplayer)) {
                                chunk.a(entityplayer);
                            }
                        }
                    }
                } else {
                    for (i1 = k - this.g; i1 <= k + this.g; ++i1) {
                        for (j1 = l - this.g; j1 <= l + this.g; ++j1) {
                            if (!this.a(i1, j1, k, l, i)) {
                                this.a(i1, j1, true).b(entityplayer);
                            }
                        }
                    }
                }
            }

            this.g = i;
        }
    }

    // PaperSpigot start - Player view distance API
    public void updateViewDistance(EntityPlayer player, int viewDistance) {
        viewDistance = MathHelper.clamp(viewDistance, 1, 32); // cSpigot -> Allow distance from 1, not 3
        if (viewDistance != player.viewDistance) {
            int cx = (int) player.locX >> 4;
            int cz = (int) player.locZ >> 4;

            if (viewDistance - player.viewDistance > 0) {
                for (int x = cx - viewDistance; x <= cx + viewDistance; ++x) {
                    for (int z = cz - viewDistance; z <= cz + viewDistance; ++z) {
                        PlayerChunkMap.PlayerChunk chunk = this.a(x, z, true);
                        if (!chunk.b.contains(player)) {
                            chunk.a(player);
                        }
                    }
                }
            } else {
                for (int x = cx - player.viewDistance; x <= cx + player.viewDistance; ++x) {
                    for (int z = cz - player.viewDistance; z <= cz + player.viewDistance; ++z) {
                        if (!this.a(x, z, cx, cz, viewDistance)) {
                            this.a(x, z, true).b(player);
                        }
                    }
                }
            }

            player.viewDistance = viewDistance;
        }
    }
    // PaperSpigot end

    // Paper start
    public void shutdown() {
        for ( PlayerChunk playerChunk : this.getChunks().values() ) {
            if (!(playerChunk instanceof AsyncPlayerChunk)) continue;

            AsyncPlayerChunk asyncPlayerChunk = (AsyncPlayerChunk) playerChunk;
            AsyncChunkProvider.CancellableChunkRequest chunkRequest = asyncPlayerChunk.getChunkRequest();
            if (chunkRequest != null) {
                chunkRequest.cancel();
            }
        }
    }
    // Paper end

    public static int getFurthestViewableBlock(int i) {
        return i * 16 - 16;
    }

    @Getter
    public static class PlayerChunk {
        protected final PlayerChunkMap playerChunkMap;
        protected final List<EntityPlayer> b = new ObjectArrayList<>(); // cSpigot - ArrayList -> ObjectArrayList
        protected final ChunkCoordIntPair location;
        protected final Short2ShortMap dirtyBlocks = new Short2ShortArrayMap(64); // cSpigot - use fast util
        protected int dirtyCount; // 64 is full chunk update required, 1 is single block change, rest is multi block change
        protected int f;
        protected long g;

        // CraftBukkit start - add fields
        protected final Map<EntityPlayer, Runnable> players = new Object2ObjectOpenHashMap<>();
        protected boolean loaded = false;
        protected final Runnable loadedRunnable = () -> PlayerChunk.this.loaded = true;
        // CraftBukkit end

        // cSpigot (Paper) start - Backport Paper 1.13.2 chunk system
        @Setter
        protected Chunk chunk;
        // cSpigot end

        public PlayerChunk(PlayerChunkMap playerchunkmap, int i, int j) {
            this.playerChunkMap = playerchunkmap;
            this.location = new ChunkCoordIntPair(i, j);
            // cSpigot start
            this.loadChunk(i, j);
            // cSpigot end
        }

        public void loadChunk(int i, int j) {
            ChunkProviderServer chunkProvider = playerChunkMap.getWorld().chunkProviderServer;
            this.chunk = chunkProvider.getChunkAt(i, j, true, true, (chunk) -> {
                this.loadedRunnable.run();
                this.chunk = chunk;
            });
        }

        // cSpigot start - Chunk Snapshot API
        public void resend() {
            if (this.dirtyCount == 0) {
                playerChunkMap.e.add(this);
            }
            this.dirtyCount = 64;
            this.f = 0xFFFF;
        }
        // cSpigot end

        // addPlayer
        public void a(final EntityPlayer entityplayer) { // CraftBukkit - added final to argument
            if (this.b.contains(entityplayer)) {
                PlayerChunkMap.a.debug("Failed to add player. {} already is in chunk {}, {}", entityplayer, this.location.x, this.location.z);
            } else {
                if (this.b.isEmpty()) {
                    this.g = playerChunkMap.world.getTime();
                }

                this.b.add(entityplayer);
                // CraftBukkit start - use async chunk io
                Runnable playerRunnable;
                if (this.loaded) {
                    playerRunnable = null;
                    entityplayer.chunkCoordIntPairQueue.add(this.location);
                } else {
                    playerRunnable = () -> entityplayer.chunkCoordIntPairQueue.add(PlayerChunk.this.location);
                    playerChunkMap.a().chunkProviderServer.getChunkAt(this.location.x, this.location.z, playerRunnable);
                }

                this.players.put(entityplayer, playerRunnable);
                // CraftBukkit end
            }
        }

        // removePlayer
        public void b(EntityPlayer entityplayer) {
            if (this.b.contains(entityplayer)) {
                // CraftBukkit start - If we haven't loaded yet don't load the chunk just so we
                // can clean it up
                if (!this.loaded) { // cSpigot
                    ChunkIOExecutor.dropQueuedChunkLoad(playerChunkMap.a(), this.location.x, this.location.z, this.players.get(entityplayer));

                    this.b.remove(entityplayer);
                    this.players.remove(entityplayer);

                    if (this.b.isEmpty()) {
                        ChunkIOExecutor.dropQueuedChunkLoad(playerChunkMap.a(), this.location.x, this.location.z, this.loadedRunnable);
                        long i = this.location.x + 2147483647L | this.location.z + 2147483647L << 32;
                        playerChunkMap.d.remove(i);
                        playerChunkMap.f.remove(this);
                    }

                    return;
                }
                // CraftBukkit end

                entityplayer.playerConnection.sendPacket(PacketPlayOutMapChunk.unload(this.location.x, this.location.z));

                if (chunk != null && !(chunk instanceof EmptyChunk) && PlayerChunkUnloadEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    new PlayerChunkUnloadEvent(chunk.bukkitChunk, entityplayer.getBukkitEntity()).callEvent();
                }

                this.players.remove(entityplayer); // CraftBukkit
                this.b.remove(entityplayer);
                entityplayer.chunkCoordIntPairQueue.remove(this.location);
                if (this.b.isEmpty()) {
                    long i = this.location.x + 2147483647L | this.location.z + 2147483647L << 32;

                    this.a(chunk);
                    playerChunkMap.d.remove(i);
                    playerChunkMap.f.remove(this);
                    if (this.dirtyCount > 0) {
                        playerChunkMap.e.remove(this);
                    }

                    playerChunkMap.a().chunkProviderServer.queueUnload(this.location.x, this.location.z);
                }
            }
        }

        public void a() {
            // cSpigot start
            if (this.chunk == null) {
                this.chunk = playerChunkMap.world.getChunkAt(this.location.x, this.location.z);
            }
            this.a(chunk);
            // cSpigot end
        }

        private void a(Chunk chunk) {
            long i = playerChunkMap.world.getTime();
            if (chunk != null) {
                chunk.c(chunk.w() + i - this.g);
            }
            this.g = i;
        }

        public void a(int i, int j, int k) {
            if (this.dirtyCount == 0) {
                playerChunkMap.e.add(this);
            }

            this.f |= 1 << (j >> 4);
            if (this.dirtyCount < 64) {
                short short0 = (short) (i << 12 | k << 8 | j);

                for (short l = 0; l < this.dirtyCount; ++l) {
                    if (this.dirtyBlocks.get(l) == short0) { // cSpigot - use fast util
                        return;
                    }
                }

                this.dirtyBlocks.put((short) this.dirtyCount++, short0); // cSpigot - use fast util
            }
        }

        public void a(Packet<?> packet) {
            for (EntityPlayer entityplayer : this.b) {
                if (!entityplayer.chunkCoordIntPairQueue.contains(this.location)) {
                    entityplayer.playerConnection.sendPacket(packet);
                }
            }
        }

        public void b() {
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

                    this.a(new PacketPlayOutBlockChange(playerChunkMap.world, blockposition));
                    if (playerChunkMap.world.getType(blockposition).getBlock().isTileEntity()) {
                        this.a(playerChunkMap.world.getTileEntity(blockposition));
                    }
                } else {
                    int l;

                    if (this.dirtyCount == 64) {
                        i = this.location.x * 16;
                        j = this.location.z * 16;
                        this.a(new PacketPlayOutMapChunk(
                                playerChunkMap.world.getChunkAt(this.location.x, this.location.z), false, this.f));

                        for (k = 0; k < 16; ++k) {
                            if ((this.f & 1 << k) != 0) {
                                l = k << 4;
                                List<TileEntity> list = playerChunkMap.world.getTileEntities(i, l, j, i + 16,
                                        l + 16, j + 16);
                                for (TileEntity tileEntity : list) {
                                    this.a(tileEntity);
                                }
                            }
                        }
                    } else {
                        this.a(new PacketPlayOutMultiBlockChange(this.dirtyCount, this.dirtyBlocks,
                                playerChunkMap.world.getChunkAt(this.location.x, this.location.z)));

                        for (i = 0; i < this.dirtyCount; ++i) {
                            // cSpigot start - use fast util
                            j = (this.dirtyBlocks.get((short) i) >> 12 & 15) + this.location.x * 16;
                            k = this.dirtyBlocks.get((short) i) & 255;
                            l = (this.dirtyBlocks.get((short) i)>> 8 & 15) + this.location.z * 16;
                            // cSpigot end
                            BlockPosition blockposition1 = new BlockPosition(j, k, l);

                            if (playerChunkMap.world.getType(blockposition1).getBlock().isTileEntity()) {
                                this.a(playerChunkMap.world.getTileEntity(blockposition1));
                            }
                        }
                    }
                }

                this.dirtyCount = 0;
                this.f = 0;
            }
        }

        public void a(TileEntity tileentity) {
            if (tileentity != null) {
                Packet packet = tileentity.getUpdatePacket();

                if (packet != null) {
                    this.a(packet);
                }
            }

        }
    }

    // CraftBukkit start - Sorter to load nearby chunks first
    public static class ChunkCoordComparator implements java.util.Comparator<ChunkCoordIntPair> {
        private final int x;
        private final int z;

        public ChunkCoordComparator(EntityPlayer entityplayer) {
            x = (int) entityplayer.locX >> 4;
            z = (int) entityplayer.locZ >> 4;
        }

        @Override
        public int compare(ChunkCoordIntPair a, ChunkCoordIntPair b) {
            if (a.equals(b)) {
                return 0;
            }

            // Subtract current position to set center point
            int ax = a.x - this.x;
            int az = a.z - this.z;
            int bx = b.x - this.x;
            int bz = b.z - this.z;

            int result = ((ax - bx) * (ax + bx)) + ((az - bz) * (az + bz));
            if (result != 0) {
                return result;
            }

            if (ax < 0) {
                if (bx < 0) {
                    return bz - az;
                } else {
                    return -1;
                }
            } else if (bx < 0) {
                return 1;
            } else {
                return az - bz;
            }
        }
    }
    // CraftBukkit end

    private static long getChunkKey(int i, int j) {
        return d(i, j);
    }

    public static long d(int i, int j) {
        return (long) i + 2147483647L | (long) j + 2147483647L << 32;
    }

    // cSpigot start - ChunkSnapshot API
    public void resend(int chunkX, int chunkZ) {
        PlayerChunk playerchunk = this.a(chunkX, chunkZ, false);

        if (playerchunk != null) playerchunk.resend();
    }
    // cSpigot end
}
