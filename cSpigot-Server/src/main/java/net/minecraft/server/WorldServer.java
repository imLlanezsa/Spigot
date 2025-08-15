package net.minecraft.server;

import com.destroystokyo.paper.util.IBlockDataList;
import com.destroystokyo.paper.util.ThreadUnsafeRandom;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.WeatherType;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.util.HashTreeSet;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spigotmc.AsyncCatcher;
import io.github.cspigot.api.command.util.ClassUtil;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.async.tracker.ParallelEntityTracker;
import io.github.cspigot.feature.chunk.AsyncChunkProvider;
import io.github.cspigot.feature.async.tracker.AsyncEntityTracker;
import io.github.cspigot.feature.chunk.player.AsyncPlayerChunkMap;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
// CraftBukkit end

public class WorldServer extends World implements IAsyncTaskHandler {

    private static final Logger a = LogManager.getLogger();
    private final MinecraftServer server;
    public EntityTracker tracker;
    private PlayerChunkMap manager;
    // private final Set<NextTickListEntry> L = Sets.newHashSet(); // PAIL: Rename nextTickListHash
    private final HashTreeSet<NextTickListEntry> M = new HashTreeSet<>(); // CraftBukkit - HashTreeSet // PAIL: Rename nextTickList
    final Map<UUID, Entity> entitiesByUUID = new LinkedHashMap<>();
    public ChunkProviderServer chunkProviderServer;
    public boolean savingDisabled;
    private boolean O;
    private int emptyTime;
    private final PortalTravelAgent Q;
    private final SpawnerCreature R = new SpawnerCreature();
    protected final VillageSiege siegeManager = new VillageSiege(this);
    private WorldServer.BlockActionDataList[] S = new WorldServer.BlockActionDataList[]{new WorldServer.BlockActionDataList(null), new WorldServer.BlockActionDataList(null)};
    private int T;
    private static final List<StructurePieceTreasure> U = Lists.newArrayList(new StructurePieceTreasure(Items.STICK, 0, 1, 3, 10), new StructurePieceTreasure(Item.getItemOf(Blocks.PLANKS), 0, 1, 3, 10), new StructurePieceTreasure(Item.getItemOf(Blocks.LOG), 0, 1, 3, 10), new StructurePieceTreasure(Items.STONE_AXE, 0, 1, 1, 3), new StructurePieceTreasure(Items.WOODEN_AXE, 0, 1, 1, 5), new StructurePieceTreasure(Items.STONE_PICKAXE, 0, 1, 1, 3), new StructurePieceTreasure(Items.WOODEN_PICKAXE, 0, 1, 1, 5), new StructurePieceTreasure(Items.APPLE, 0, 2, 3, 5), new StructurePieceTreasure(Items.BREAD, 0, 2, 3, 3), new StructurePieceTreasure(Item.getItemOf(Blocks.LOG2), 0, 1, 3, 10));
    public boolean hasPhysicsEvent = true; // Paper

    // cSpigot start
    private final Collection<NextTickListEntry> currentlyTickingBlocks = new ArrayList<>();
    public final Runnable updateTracker = () -> this.tracker.updatePlayers();
    // cSpigot end

    // CraftBukkit start
    public final int dimension;

    // Add env and gen to constructor
    public WorldServer(MinecraftServer minecraftserver, IDataManager idatamanager, WorldData worlddata, int i, MethodProfiler methodprofiler, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen) {
        super(idatamanager, worlddata, WorldProvider.byDimension(env.getId()), methodprofiler, false, gen, env);
        this.dimension = i;
        this.pvpMode = minecraftserver.getPVP();
        worlddata.world = this;
        // CraftBukkit end
        this.server = minecraftserver;

        // cSpigot start - Async Entity Tracker
        if (SettingsConfig.ASYNC_ENTITY_TRACKER) {
            this.tracker = SettingsConfig.SEMI_ASYNC_TRACKER ? new AsyncEntityTracker(this) : new ParallelEntityTracker(this);
        } else {
            this.tracker = new EntityTracker(this);
        }
        // cSpigot end

        this.manager = SettingsConfig.ASYNC_CHUNKS ? new AsyncPlayerChunkMap(this, spigotConfig.viewDistance) : new PlayerChunkMap(this, spigotConfig.viewDistance); // Spigot
        this.worldProvider.a(this);
        this.chunkProvider = this.k();
        this.Q = ((org.bukkit.craftbukkit.CraftTravelAgent) new org.bukkit.craftbukkit.CraftTravelAgent(this).setSearchRadius(paperSpigotConfig.portalSearchRadius)); // CraftBukkit // Paper - configurable search radius
        this.B();
        this.C();
        this.getWorldBorder().a(minecraftserver.aI());
    }

    public World b() {
        this.worldMaps = new PersistentCollection(this.dataManager);
        String s = PersistentVillage.a(this.worldProvider);
        PersistentVillage persistentvillage = (PersistentVillage) this.worldMaps.get(PersistentVillage.class, s);

        if (persistentvillage == null) {
            this.villages = new PersistentVillage(this);
            this.worldMaps.a(s, this.villages);
        } else {
            this.villages = persistentvillage;
            this.villages.a((World) this);
        }

        if (getServer().getScoreboardManager() == null) { // CraftBukkit
            this.scoreboard = new ScoreboardServer(this.server);
            PersistentScoreboard persistentscoreboard = (PersistentScoreboard) this.worldMaps.get(PersistentScoreboard.class, "scoreboard");

            if (persistentscoreboard == null) {
                persistentscoreboard = new PersistentScoreboard();
                this.worldMaps.a("scoreboard", persistentscoreboard);
            }

            persistentscoreboard.a(this.scoreboard);
            ((ScoreboardServer) this.scoreboard).a(persistentscoreboard);
            // CraftBukkit start
        } else {
            this.scoreboard = getServer().getScoreboardManager().getMainScoreboard().getHandle();
        }
        // CraftBukkit end
        this.getWorldBorder().setCenter(this.worldData.C(), this.worldData.D());
        this.getWorldBorder().setDamageAmount(this.worldData.I());
        this.getWorldBorder().setDamageBuffer(this.worldData.H());
        this.getWorldBorder().setWarningDistance(this.worldData.J());
        this.getWorldBorder().setWarningTime(this.worldData.K());
        if (this.worldData.F() > 0L) {
            this.getWorldBorder().transitionSizeBetween(this.worldData.E(), this.worldData.G(), this.worldData.F());
        } else {
            this.getWorldBorder().setSize(this.worldData.E());
        }

        // CraftBukkit start
        if (generator != null) {
            getWorld().getPopulators().addAll(generator.getDefaultPopulators(getWorld()));
        }
        // CraftBukkit end
        return this;
    }

    // CraftBukkit start
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        TileEntity result = super.getTileEntity(pos);
        Block type = getType(pos).getBlock();

        if (type == Blocks.CHEST || type == Blocks.TRAPPED_CHEST) { // Spigot
            if (!(result instanceof TileEntityChest)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.FURNACE) {
            if (!(result instanceof TileEntityFurnace)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.DROPPER) {
            if (!(result instanceof TileEntityDropper)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.DISPENSER) {
            if (!(result instanceof TileEntityDispenser)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.JUKEBOX) {
            if (!(result instanceof BlockJukeBox.TileEntityRecordPlayer)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.NOTEBLOCK) {
            if (!(result instanceof TileEntityNote)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.MOB_SPAWNER) {
            if (!(result instanceof TileEntityMobSpawner)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if ((type == Blocks.STANDING_SIGN) || (type == Blocks.WALL_SIGN)) {
            if (!(result instanceof TileEntitySign)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.ENDER_CHEST) {
            if (!(result instanceof TileEntityEnderChest)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.BREWING_STAND) {
            if (!(result instanceof TileEntityBrewingStand)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.BEACON) {
            if (!(result instanceof TileEntityBeacon)) {
                result = fixTileEntity(pos, type, result);
            }
        } else if (type == Blocks.HOPPER) {
            if (!(result instanceof TileEntityHopper)) {
                result = fixTileEntity(pos, type, result);
            }
        }

        return result;
    }

    private TileEntity fixTileEntity(BlockPosition pos, Block type, TileEntity found) {
        this.getServer().getLogger().log(Level.SEVERE, "Block at {0},{1},{2} is {3} but has {4}" + ". "
                + "Bukkit will attempt to fix this, but there may be additional damage that we cannot recover.", new Object[]{pos.getX(), pos.getY(), pos.getZ(), org.bukkit.Material.getMaterial(Block.getId(type)).toString(), found});

        if (type instanceof IContainer) {
            TileEntity replacement = ((IContainer) type).a(this, type.toLegacyData(this.getType(pos)));
            replacement.world = this;
            this.setTileEntity(pos, replacement);
            return replacement;
        } else {
            this.getServer().getLogger().severe("Don't know how to fix for this type... Can't do anything! :(");
            return found;
        }
    }

    private boolean canSpawn(int x, int z) {
        if (this.generator != null) {
            return this.generator.canSpawn(this.getWorld(), x, z);
        } else {
            return this.worldProvider.canSpawn(x, z);
        }
    }
    // CraftBukkit end

    public void doTick(BooleanSupplier shouldKeepTicking) {
        super.doTick(shouldKeepTicking);
        if (this.getWorldData().isHardcore() && this.getDifficulty() != EnumDifficulty.HARD) {
            this.getWorldData().setDifficulty(EnumDifficulty.HARD);
        }

        this.worldProvider.m().b();
        if (this.everyoneDeeplySleeping()) {
            if (this.getGameRules().getBoolean("doDaylightCycle")) {
                long i = this.worldData.getDayTime() + 24000L;

                this.worldData.setDayTime(i - i % 24000L);
            }

            this.e();
        }

        // CraftBukkit start - Only call spawner if we have players online and the world allows for mobs or animals
        long time = this.worldData.getTime();
        if (this.getGameRules().getBoolean("doMobSpawning") && this.worldData.getType() != WorldType.DEBUG_ALL_BLOCK_STATES && (this.allowMonsters || this.allowAnimals) && this.players.size() > 0) {
            //timings.mobSpawn.startTiming(); // Spigot // Purpur
            this.R.a(this, this.allowMonsters && (this.ticksPerMonsterSpawns != 0 && time % this.ticksPerMonsterSpawns == 0L), this.allowAnimals && (this.ticksPerAnimalSpawns != 0 && time % this.ticksPerAnimalSpawns == 0L), this.worldData.getTime() % 400L == 0L);
            //timings.mobSpawn.stopTiming(); // Spigot // Purpur
            // CraftBukkit end
        }
        // CraftBukkit end

        //timings.doChunkUnload.startTiming(); // Spigot //Purpur
        // Only unload if chunkProvider isn't null
        if (this.chunkProvider != null) {
            if (SettingsConfig.UNLOAD_CHUNKS) {
                this.chunkProvider.unloadChunks(shouldKeepTicking);
            } else {
                this.chunkProviderServer.unloadQueue.clear();
            }
        }
        //timings.doChunkUnload.stopTiming(); // Spigot // Purpur

        int j = this.a(1.0F);

        if (j != this.ab()) {
            this.c(j);
        }

        this.worldData.setTime(this.worldData.getTime() + 1L);
        if (this.getGameRules().getBoolean("doDaylightCycle")) {
            this.worldData.setDayTime(this.worldData.getDayTime() + 1L);
        }

        //timings.scheduledBlocks.startTiming(); // Spigot // Purpur
        this.a(false);
        //timings.scheduledBlocks.stopTiming(); // Spigot // Purpur

        //timings.chunkTicks.startTiming(); // Spigot // Purpur
        this.h();
        //timings.chunkTicks.stopTiming(); // Spigot // Purpur

        spigotConfig.antiXrayInstance.flushUpdates(this); // PaperSpigot

        //timings.doChunkMap.startTiming(); // Spigot // Purpur
        this.manager.flush();
        //timings.doChunkMap.stopTiming(); // Spigot // Purpur

        //timings.doVillages.startTiming(); // Spigot // Purpur
        this.villages.tick();
        this.siegeManager.a();
        //timings.doVillages.stopTiming(); // Spigot // Purpur

        //timings.doPortalForcer.startTiming(); // Spigot // Purpur
        this.Q.a(this.getTime());
        //timings.doPortalForcer.stopTiming(); // Spigot // Purpur

        //timings.doSounds.startTiming(); // Spigot // Purpur
        this.ak();
        //timings.doSounds.stopTiming(); // Spigot

        //timings.doChunkGC.startTiming(); // Spigot
        this.getWorld().processChunkGC(); // CraftBukkit
        //timings.doChunkGC.stopTiming(); // Spigot // Purpur

        if (SettingsConfig.ASYNC_MONITOR_TIMEOUT_ENABLED && ClassUtil.isNthTick(40)) {
            if (this.M.size() > 150000) {
                MinecraftServer.LOGGER.warn("[cSpigot-Crash Monitor] NextBlockTickList is extremely large: {}", this.M.size());
            }
            if (this.currentlyTickingBlocks.size() > 10000) {
                MinecraftServer.LOGGER.warn("[cSpigot-Crash Monitor] CurrentlyTickingBlocks is extremely large: {}", this.currentlyTickingBlocks.size());
            }
        }
    }

    public BiomeBase.BiomeMeta a(EnumCreatureType enumcreaturetype, BlockPosition blockposition) {
        List list = this.N().getMobsFor(enumcreaturetype, blockposition);

        return list != null && !list.isEmpty() ? (BiomeBase.BiomeMeta) WeightedRandom.a(this.random, list) : null;
    }

    public boolean a(EnumCreatureType enumcreaturetype, BiomeBase.BiomeMeta biomebase_biomemeta, BlockPosition blockposition) {
        List list = this.N().getMobsFor(enumcreaturetype, blockposition);

        return list != null && !list.isEmpty() ? list.contains(biomebase_biomemeta) : false;
    }

    public void everyoneSleeping() {
        this.O = false;
        if (!this.players.isEmpty()) {
            int i = 0;
            int j = 0;
            Iterator iterator = this.players.iterator();

            while (iterator.hasNext()) {
                EntityHuman entityhuman = (EntityHuman) iterator.next();

                if (entityhuman.isSpectator()) {
                    ++i;
                } else if (entityhuman.isSleeping() || entityhuman.fauxSleeping) {
                    ++j;
                }
            }

            this.O = j > 0 && j >= this.players.size() - i;
        }

    }

    protected void e() {
        this.O = false;
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityHuman entityhuman = (EntityHuman) iterator.next();

            if (entityhuman.isSleeping()) {
                entityhuman.a(false, false, true);
            }
        }

        if (this.getGameRules().getBoolean("doWeatherCycle")) {
            this.ag();
        }

    }

    private void ag() {
        this.worldData.setStorm(false);
        // CraftBukkit start
        // If we stop due to everyone sleeping we should reset the weather duration to some other random value.
        // Not that everyone ever manages to get the whole server to sleep at the same time....
        if (!this.worldData.hasStorm()) {
            this.worldData.setWeatherDuration(0);
        }
        // CraftBukkit end
        this.worldData.setThundering(false);
        // CraftBukkit start
        // If we stop due to everyone sleeping we should reset the weather duration to some other random value.
        // Not that everyone ever manages to get the whole server to sleep at the same time....
        if (!this.worldData.isThundering()) {
            this.worldData.setThunderDuration(0);
        }
        // CraftBukkit end
    }

    public boolean everyoneDeeplySleeping() {
        // PaperBin start - WorldServer#everyoneDeeplySleeping optimization
        if (this.players.isEmpty() || this.isClientSide || !this.O) {
            return false;
        }

        for (EntityHuman human : this.players) {
            if (!human.isSpectator() && !human.isDeeplySleeping() && !human.fauxSleeping) {
                return false;
            }

            // cSpigot start - ignore players who are not in overworld
            if (!human.isNether() && !human.isEnd()) {
                return false;
            }
            // cSpigot end
        }

        return true;
        // PaperBin end
    }

    protected void h() {
        super.h();

        if (this.worldData.getType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
            gnu.trove.iterator.TLongShortIterator iterator = this.chunkTickList.iterator();
            while (iterator.hasNext()) {
                iterator.advance();
                long chunkKey = iterator.key();
                int chunkX = World.keyToX(chunkKey);
                int chunkZ = World.keyToZ(chunkKey);

                long chunkHashKey = LongHash.toLong(chunkX, chunkZ);
                // If unloaded, or in proceed of being unloaded, drop it
                if (!this.chunkProviderServer.isChunkLoaded(chunkX, chunkZ) || this.chunkProviderServer.unloadQueue.contains(chunkHashKey)) {
                    iterator.remove();
                    continue;
                }

                Chunk chunk = this.getChunkAt(chunkX, chunkZ);
                //Chunk chunk = iterator.next();
                chunk.b(false);
                // Spigot end
            }
        } else {
            for (gnu.trove.iterator.TLongShortIterator iter = chunkTickList.iterator(); iter.hasNext(); )
            {
                iter.advance();
                long chunkCoord = iter.key();
                int chunkX = World.keyToX( chunkCoord );
                int chunkZ = World.keyToZ( chunkCoord );
                long key = LongHash.toLong(chunkX, chunkZ);
                // If unloaded, or in proceed of being unloaded, drop it
                if ( ( !this.chunkProvider.isChunkLoaded( chunkX, chunkZ ) ) || ( this.chunkProviderServer.unloadQueue.contains( key ) ) )
                {
                    iter.remove();
                    continue;
                }

                Chunk chunk = this.getChunkAt(chunkX, chunkZ);
                $cSpigot$tickChunk(chunk);
            }
        }

        // Spigot Start
        if (spigotConfig.clearChunksOnTick) {
            chunkTickList.clear();
        }
        // Spigot End

        // cSpigot end.
    }

    private final ThreadUnsafeRandom randomTickRandom = new ThreadUnsafeRandom();

    private void $cSpigot$tickChunk(Chunk chunk) {
        // cSpigot end
        int k = chunk.locX * 16;
        int l = chunk.locZ * 16;

        chunk.m();
        chunk.b(false);

        //if (!chunk.areNeighborsLoaded(1)) return; // Spigot

        int i1;
        BlockPosition blockposition;

        if (!this.paperSpigotConfig.disableThunder && this.random.nextInt(100000) == 0 && this.S() && this.R()) { // PaperSpigot - Disable thunder
            this.m = this.m * 3 + 1013904223;
            i1 = this.m >> 2;
            blockposition = this.getLightingStrikePosition(chunk, new BlockPosition(k + (i1 & 15), 0, l + (i1 >> 8 & 15)));
            if (this.isRainingAt(blockposition)) {
                this.strikeLightning(new EntityLightning(this, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ()));
            }
        }

        if (!this.paperSpigotConfig.disableIceAndSnow && this.random.nextInt(16) == 0) { // PaperSpigot - Disable ice and snow
            this.m = this.m * 3 + 1013904223;
            i1 = this.m >> 2;
            blockposition = this.q(new BlockPosition(k + (i1 & 15), 0, l + (i1 >> 8 & 15)));
            BlockPosition blockposition1 = blockposition.down();

            if (this.w(blockposition1)) {
                // CraftBukkit start
                this.tickIce(blockposition1);
                // CraftBukkit end
            }

            if (this.S() && this.f(blockposition, true)) {
                // CraftBukkit start
                this.tickSnow(blockposition);
                // CraftBukkit end
            }

            if (this.S() && this.getBiome(blockposition1).e()) {
                this.getType(blockposition1).getBlock().k(this, blockposition1);
            }
        }

        // cSpigot start - Backported Block/Chunk Ticking (1.16)
        if (SettingsConfig.DO_BLOCK_OPERATIONS && this.getGameRules().c("randomTickSpeed") > 0) {
            int randomTickSpeed = this.getGameRules().c("randomTickSpeed");
            ChunkSection[] sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < 16; ++sectionIndex) {
                ChunkSection section = sections[sectionIndex];
                if (section == null || !section.shouldTick() || section.tickingList.size() == 0) {
                    continue;
                }

                int yPos = sectionIndex << 4;

                for (int a = 0; a < randomTickSpeed; ++a) {
                    int tickingBlocks = section.tickingList.size();
                    int index = this.randomTickRandom.nextInt(16 * 16 * 16);
                    if (index >= tickingBlocks) {
                        continue;
                    }

                    long raw = section.tickingList.getRaw(index);
                    int location = IBlockDataList.getLocationFromRaw(raw);
                    int randomX = location & 15;
                    int randomY = ((location >>> (4 + 4)) & 255) | yPos;
                    int randomZ = (location >>> 4) & 15;

                    BlockPosition blockposition2 = new BlockPosition(k + randomX, randomY, l + randomZ);
                    IBlockData iblockdata = IBlockDataList.getBlockDataFromRaw(raw);

                    Block block = iblockdata.getBlock();
                    if (!block.isTicking()) continue;

                    block.randomTick(this, chunk, blockposition2, iblockdata, this.randomTickRandom);
                }
            }
        }
    }

    // cSpigot start - Optimize Block ticking
    private void tickIce(BlockPosition pos) {
        BlockState blockState = this.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getState();
        blockState.setTypeId(Block.getId(Blocks.ICE));

        BlockFormEvent iceBlockForm = new BlockFormEvent(blockState.getBlock(), blockState);
        this.getServer().getPluginManager().callEvent(iceBlockForm);
        if (!iceBlockForm.isCancelled()) {
            blockState.update(true);
        }
    }

    private void tickSnow(BlockPosition pos) {
        BlockState blockState = this.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getState();
        blockState.setTypeId(Block.getId(Blocks.SNOW_LAYER));

        BlockFormEvent snow = new BlockFormEvent(blockState.getBlock(), blockState);
        this.getServer().getPluginManager().callEvent(snow);
        if (!snow.isCancelled()) {
            blockState.update(true);
        }
    }

    protected BlockPosition getLightingStrikePosition(Chunk chunk, BlockPosition blockposition) {
        BlockPosition highestEntityPos = this.q(blockposition);
        AxisAlignedBB axisalignedbb = new AxisAlignedBB(highestEntityPos, new BlockPosition(highestEntityPos.getX(), this.getHeight(), highestEntityPos.getZ()));
        axisalignedbb = axisalignedbb.grow(3.0D, 3.0D, 3.0D);

        List<EntityLiving> list = new ArrayList<>();
        chunk.a(EntityLiving.class, axisalignedbb, list, (entity) -> entity != null && entity.isAlive() && this.i(entity.getChunkCoordinates()));

        if (list.isEmpty()) {
            return highestEntityPos;
        }

        return list.get(this.random.nextInt(list.size())).getChunkCoordinates();
    }
    // cSpigot end

    public boolean a(BlockPosition blockposition, Block block) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(blockposition, block);
        return this.currentlyTickingBlocks.contains(nextticklistentry);
    }

    public void a(BlockPosition blockposition, Block block, int i) {
        this.a(blockposition, block, i, 0);
    }

    public void a(BlockPosition blockposition, Block block, int i, int j) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(blockposition, block);
        byte b0;

        if (this.e && block.getMaterial() != Material.AIR) {
            if (block.N()) {
                b0 = 8;
                if (this.areChunksLoadedBetween(nextticklistentry.a.a(-b0, -b0, -b0), nextticklistentry.a.a(b0, b0, b0))) {
                    IBlockData iblockdata = this.getType(nextticklistentry.a);

                    if (iblockdata.getBlock().getMaterial() != Material.AIR && iblockdata.getBlock() == nextticklistentry.a()) {
                        iblockdata.getBlock().b(this, nextticklistentry.a, iblockdata, this.random);
                    }
                }

                return;
            } else {
                b0 = 0;
            }

            i = 1;
        } else {
            b0 = 0;
        }

        if (this.areChunksLoadedBetween(blockposition.a(-b0, -b0, -b0), blockposition.a(b0, b0, b0))) {
            if (block.getMaterial() != Material.AIR) {
                nextticklistentry.a((long) i + this.worldData.getTime());
                nextticklistentry.a(j);
            }

            // CraftBukkit - use M, PAIL: Rename nextTickList
            if (!this.M.contains(nextticklistentry)) {
                this.M.add(nextticklistentry);
            }
        }
    }

    public void b(BlockPosition blockposition, Block block, int i, int j) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(blockposition, block);

        nextticklistentry.a(j);
        if (block.getMaterial() != Material.AIR) {
            nextticklistentry.a((long) i + this.worldData.getTime());
        }

        this.M.add(nextticklistentry);
    }

    public void tickEntities() {
        if (false && this.players.isEmpty()) { // CraftBukkit - this prevents entity cleanup, other issues on servers with no players
            if (this.emptyTime++ >= 1200) {
                return;
            }
        } else {
            this.j();
        }

        super.tickEntities();
        spigotConfig.currentPrimedTnt = 0; // Spigot
    }

    public void j() {
        this.emptyTime = 0;
    }

    // cSpigot start - Optimize Block ticking
    public boolean a(boolean flag) {
        AsyncCatcher.catchOp("Next block ticking");

        if (this.worldData.getType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
            return false;
        } else {
            int i = this.M.size();

            if (false) { // CraftBukkit
                throw new IllegalStateException("TickNextTick list out of synch");
            } else {
                // PaperSpigot start - No, stop doing this, it affects things like redstone
                /*
                if (i > 1000) {
                    // CraftBukkit start - If the server has too much to process over time, try to alleviate that
                    if (i > 20 * 1000) {
                        i = i / 20;
                    } else {
                        i = 1000;
                    }
                    // CraftBukkit end
                */
                if (i > paperSpigotConfig.tickNextTickCap) {
                    i = paperSpigotConfig.tickNextTickCap;
                }
                // PaperSpigot end

                NextTickListEntry nextticklistentry;
                for ( int j = 0; j < i; ++j ) {
                    nextticklistentry = (NextTickListEntry) this.M.first();
                    if (!flag && nextticklistentry.b > this.worldData.getTime()) {
                        break;
                    }

                    // CraftBukkit - use M, PAIL: Rename nextTickList
                    this.M.remove(nextticklistentry);
                    this.currentlyTickingBlocks.add(nextticklistentry);
                }

                Iterator<NextTickListEntry> iterator = this.currentlyTickingBlocks.iterator();
                while (iterator.hasNext()) {
                    nextticklistentry = iterator.next();
                    iterator.remove();
                    byte b0 = 0;

                    if (nextticklistentry.a().loadChunks || this.areChunksLoadedBetween(nextticklistentry.a.a(-b0, -b0, -b0), nextticklistentry.a.a(b0, b0, b0))) {
                        IBlockData iblockdata = this.getType(nextticklistentry.a);
                        Block block = iblockdata.getBlock();

                        if (block.getMaterial() != Material.AIR && Block.a(iblockdata.getBlock(), nextticklistentry.a())) {
                            try {
                                block.b(this, nextticklistentry.a, iblockdata, this.random);
                            } catch (Throwable throwable) {
                                CrashReport crashreport = CrashReport.a(throwable, "Exception while ticking a block");
                                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Block being ticked");

                                CrashReportSystemDetails.a(crashreportsystemdetails, nextticklistentry.a, iblockdata);
                                throw new ReportedException(crashreport);
                            }
                        }
                    } else {
                        this.a(nextticklistentry.a, nextticklistentry.a(), 0);
                    }
                }

                this.currentlyTickingBlocks.clear();
                return !this.M.isEmpty();
            }
        }
    }

    public List<NextTickListEntry> a(Chunk chunk, boolean flag) {
        ChunkCoordIntPair chunkcoordintpair = chunk.j();
        int i = (chunkcoordintpair.x << 4) - 2;
        int j = i + 16 + 2;
        int k = (chunkcoordintpair.z << 4) - 2;
        int l = k + 16 + 2;

        return this.a(new StructureBoundingBox(i, 0, k, j, 256, l), flag);
    }

    public List<NextTickListEntry> a(StructureBoundingBox structureboundingbox, boolean flag) {
        AsyncCatcher.catchOp("Collecting next tick list entries"); // cSpigot
        ArrayList arraylist = null;

        for (int i = 0; i < 2; ++i) {
            Iterator iterator;

            if (i == 0) {
                iterator = this.M.iterator();
            } else {
                iterator = this.currentlyTickingBlocks.iterator(); // cSpigot
            }

            while (iterator.hasNext()) {
                NextTickListEntry nextticklistentry = (NextTickListEntry) iterator.next();
                BlockPosition blockposition = nextticklistentry.a;

                if (blockposition.getX() >= structureboundingbox.a && blockposition.getX() < structureboundingbox.d && blockposition.getZ() >= structureboundingbox.c && blockposition.getZ() < structureboundingbox.f) {
                    if (flag) {
                        // CraftBukkit - use M
                        iterator.remove();
                    }

                    if (arraylist == null) {
                        arraylist = Lists.newArrayList();
                    }

                    arraylist.add(nextticklistentry);
                }
            }
        }

        return arraylist;
    }

    /* CraftBukkit start - We prevent spawning in general, so this butchering is not needed
    public void entityJoinedWorld(Entity entity, boolean flag) {
        if (!this.getSpawnAnimals() && (entity instanceof EntityAnimal || entity instanceof EntityWaterAnimal)) {
            entity.die();
        }

        if (!this.getSpawnNPCs() && entity instanceof NPC) {
            entity.die();
        }

        super.entityJoinedWorld(entity, flag);
    }
    // CraftBukkit end */

    private boolean getSpawnNPCs() {
        return this.server.getSpawnNPCs();
    }

    private boolean getSpawnAnimals() {
        return this.server.getSpawnAnimals();
    }

    protected IChunkProvider k() {
        IChunkLoader ichunkloader = this.dataManager.createChunkLoader(this.worldProvider);

        // CraftBukkit start
        org.bukkit.craftbukkit.generator.InternalChunkGenerator gen;

        if (this.generator != null) {
            gen = new org.bukkit.craftbukkit.generator.CustomChunkGenerator(this, this.getSeed(), this.generator);
        } else if (this.worldProvider instanceof WorldProviderHell) {
            gen = new org.bukkit.craftbukkit.generator.NetherChunkGenerator(this, this.getSeed());
        } else if (this.worldProvider instanceof WorldProviderTheEnd) {
            gen = new org.bukkit.craftbukkit.generator.SkyLandsChunkGenerator(this, this.getSeed());
        } else {
            gen = new org.bukkit.craftbukkit.generator.NormalChunkGenerator(this, this.getSeed());
        }

        // cSpigot (Paper) start
        this.chunkProviderServer = SettingsConfig.ASYNC_CHUNKS
                ? new AsyncChunkProvider(this, ichunkloader, gen)
                : new ChunkProviderServer(this, ichunkloader, gen);
        // cSpigot end
        // CraftBukkit end
        return this.chunkProviderServer;
    }

    // cSpigot start - Optimize Tile Entity fetch
    public List<TileEntity> getTileEntities(int i, int j, int k, int l, int i1, int j1) {
        ArrayList arraylist = Lists.newArrayList();
        for ( int chunkX = (i >> 4); chunkX <= ((l - 1) >> 4); chunkX++ ) {
            for ( int chunkZ = (k >> 4); chunkZ <= ((j1 - 1) >> 4); chunkZ++ ) {
                Chunk chunk = getChunkAt(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for ( Object te : chunk.tileEntities.values() ) {
                    TileEntity tileentity = (TileEntity) te;
                    if ((tileentity.position.getX() >= i) && (tileentity.position.getY() >= j) && (tileentity.position.getZ() >= k) && (tileentity.position.getX() < l) && (tileentity.position.getY() < i1) && (tileentity.position.getZ() < j1)) {
                        arraylist.add(tileentity);
                    }
                }
            }
        }
        return arraylist;
    }
    // cSpigot end

    public boolean a(EntityHuman entityhuman, BlockPosition blockposition) {
        return !this.server.a(this, blockposition, entityhuman) && this.getWorldBorder().a(blockposition);
    }

    public void a(WorldSettings worldsettings) {
        if (!this.worldData.w()) {
            try {
                this.b(worldsettings);
                if (this.worldData.getType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
                    this.aj();
                }

                super.a(worldsettings);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception initializing level");

                try {
                    this.a(crashreport);
                } catch (Throwable throwable1) {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            this.worldData.d(true);
        }


    }

    private void aj() {
        this.worldData.f(false);
        this.worldData.c(true);
        this.worldData.setStorm(false);
        this.worldData.setThundering(false);
        this.worldData.i(1000000000);
        this.worldData.setDayTime(6000L);
        this.worldData.setGameType(WorldSettings.EnumGamemode.SPECTATOR);
        this.worldData.g(false);
        this.worldData.setDifficulty(EnumDifficulty.PEACEFUL);
        this.worldData.e(true);
        this.getGameRules().set("doDaylightCycle", "false");
    }

    private void b(WorldSettings worldsettings) {
        if (!this.worldProvider.e()) {
            this.worldData.setSpawn(BlockPosition.ZERO.up(this.worldProvider.getSeaLevel()));
        } else if (this.worldData.getType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
            this.worldData.setSpawn(BlockPosition.ZERO.up());
        } else {
            this.isLoading = true;
            WorldChunkManager worldchunkmanager = this.worldProvider.m();
            List list = worldchunkmanager.a();
            Random random = new Random(this.getSeed());
            BlockPosition blockposition = worldchunkmanager.a(0, 0, 256, list, random);
            int i = 0;
            int j = this.worldProvider.getSeaLevel();
            int k = 0;

            // CraftBukkit start
            if (this.generator != null) {
                Random rand = new Random(this.getSeed());
                org.bukkit.Location spawn = this.generator.getFixedSpawnLocation(((WorldServer) this).getWorld(), rand);

                if (spawn != null) {
                    if (spawn.getWorld() != ((WorldServer) this).getWorld()) {
                        throw new IllegalStateException("Cannot set spawn point for " + this.worldData.getName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                    } else {
                        this.worldData.setSpawn(new BlockPosition(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()));
                        this.isLoading = false;
                        return;
                    }
                }
            }
            // CraftBukkit end

            if (blockposition != null) {
                i = blockposition.getX();
                k = blockposition.getZ();
            } else {
                WorldServer.a.warn("Unable to find spawn biome");
            }

            int l = 0;

            while (!this.canSpawn(i, k)) { // CraftBukkit - use our own canSpawn
                i += random.nextInt(64) - random.nextInt(64);
                k += random.nextInt(64) - random.nextInt(64);
                ++l;
                if (l == 1000) {
                    break;
                }
            }

            this.worldData.setSpawn(new BlockPosition(i, j, k));
            this.isLoading = false;
            if (worldsettings.c()) {
                this.l();
            }

        }
    }

    protected void l() {
        WorldGenBonusChest worldgenbonuschest = new WorldGenBonusChest(WorldServer.U, 10);

        for (int i = 0; i < 10; ++i) {
            int j = this.worldData.c() + this.random.nextInt(6) - this.random.nextInt(6);
            int k = this.worldData.e() + this.random.nextInt(6) - this.random.nextInt(6);
            BlockPosition blockposition = this.r(new BlockPosition(j, 0, k)).up();

            if (worldgenbonuschest.generate(this, this.random, blockposition)) {
                break;
            }
        }

    }

    public BlockPosition getDimensionSpawn() {
        return this.worldProvider.h();
    }

    public void save(boolean flag, IProgressUpdate iprogressupdate) throws ExceptionWorldConflict {
        if (this.chunkProvider.canSave()) {
            org.bukkit.Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldSaveEvent(getWorld())); // CraftBukkit
            if (iprogressupdate != null) {
                iprogressupdate.a("Saving level");
            }

            this.a();
            if (iprogressupdate != null) {
                iprogressupdate.c("Saving chunks");
            }

            this.chunkProvider.saveChunks(flag, iprogressupdate);
            // CraftBukkit - ArrayList -> Collection
            Collection arraylist = this.chunkProviderServer.a();
            Iterator iterator = arraylist.iterator();

            while (iterator.hasNext()) {
                Chunk chunk = (Chunk) iterator.next();

                if (chunk != null && !this.manager.chunkExists(chunk.locX, chunk.locZ)) {
                    this.chunkProviderServer.queueUnload(chunk.locX, chunk.locZ);
                }
            }

        }
    }

    public void flushSave() {
        if (this.chunkProvider.canSave()) {
            this.chunkProvider.c();
        }
    }

    protected void a() throws ExceptionWorldConflict {
        this.checkSession();
        this.worldData.a(this.getWorldBorder().getSize());
        this.worldData.d(this.getWorldBorder().getCenterX());
        this.worldData.c(this.getWorldBorder().getCenterZ());
        this.worldData.e(this.getWorldBorder().getDamageBuffer());
        this.worldData.f(this.getWorldBorder().getDamageAmount());
        this.worldData.j(this.getWorldBorder().getWarningDistance());
        this.worldData.k(this.getWorldBorder().getWarningTime());
        this.worldData.b(this.getWorldBorder().j());
        this.worldData.e(this.getWorldBorder().i());
        // CraftBukkit start - save worldMaps once, rather than once per shared world
        if (!(this instanceof SecondaryWorldServer)) {
            this.worldMaps.a();
        }
        this.dataManager.saveWorldData(this.worldData, this.server.getPlayerList().t());
        // CraftBukkit end
    }

    protected void a(Entity entity) {
        super.a(entity);
        this.entitiesById.a(entity.getId(), entity);
        this.entitiesByUUID.put(entity.getUniqueID(), entity);
        Entity[] aentity = entity.aB();

        if (aentity != null) {
            for (int i = 0; i < aentity.length; ++i) {
                this.entitiesById.a(aentity[i].getId(), aentity[i]);
            }
        }

    }

    protected void b(Entity entity) {
        super.b(entity);
        this.entitiesById.d(entity.getId());
        this.entitiesByUUID.remove(entity.getUniqueID());
        Entity[] aentity = entity.aB();

        if (aentity != null) {
            for (int i = 0; i < aentity.length; ++i) {
                this.entitiesById.d(aentity[i].getId());
            }
        }

    }

    public boolean strikeLightning(Entity entity) {
        // CraftBukkit start
        LightningStrikeEvent lightning = new LightningStrikeEvent(this.getWorld(), (org.bukkit.entity.LightningStrike) entity.getBukkitEntity());
        this.getServer().getPluginManager().callEvent(lightning);

        if (lightning.isCancelled()) {
            return false;
        }
        if (super.strikeLightning(entity)) {
            this.server.getPlayerList().sendPacketNearby(entity.locX, entity.locY, entity.locZ, 512.0D, dimension, new PacketPlayOutSpawnEntityWeather(entity));
            // CraftBukkit end
            return true;
        } else {
            return false;
        }
    }

    public void broadcastEntityEffect(Entity entity, byte b0) {
        this.getTracker().sendPacketToEntity(entity, new PacketPlayOutEntityStatus(entity, b0));
    }

    public Explosion createExplosion(Entity entity, double d0, double d1, double d2, float f, boolean flag, boolean flag1) {
        // CraftBukkit start
        Explosion explosion = super.createExplosion(entity, d0, d1, d2, f, flag, flag1);

        if (explosion.wasCanceled) {
            return explosion;
        }

        /* Remove
        Explosion explosion = new Explosion(this, entity, d0, d1, d2, f, flag, flag1);

        explosion.a();
        explosion.a(false);
        */
        // CraftBukkit end - TODO: Check if explosions are still properly implemented
        if (!flag1) {
            explosion.clearBlocks();
        }

        if (SettingsConfig.SHOW_EXPLOSION) {
            for (EntityHuman entityhuman : this.players) {
                if (entityhuman.e(d0, d1, d2) < 4096.0D) {
                    ((EntityPlayer) entityhuman).playerConnection.sendPacket(new PacketPlayOutExplosion(d0, d1, d2, f, explosion.getBlocks(), (Vec3D) explosion.b().get(entityhuman)));
                }
            }
        }
        return explosion;
    }

    public void playBlockAction(BlockPosition blockposition, Block block, int i, int j) {
        BlockActionData blockactiondata = new BlockActionData(blockposition, block, i, j);
        Iterator iterator = this.S[this.T].iterator();

        BlockActionData blockactiondata1;

        do {
            if (!iterator.hasNext()) {
                this.S[this.T].add(blockactiondata);
                return;
            }

            blockactiondata1 = (BlockActionData) iterator.next();
        } while (!blockactiondata1.equals(blockactiondata));

    }

    private void ak() {
        while (!this.S[this.T].isEmpty()) {
            int i = this.T;

            this.T ^= 1;

            for (BlockActionData blockactiondata : this.S[i]) {
                if (this.a(blockactiondata)) {
                    // CraftBukkit - this.worldProvider.dimension -> this.dimension
                    this.server.getPlayerList().sendPacketNearby(blockactiondata.a().getX(), blockactiondata.a().getY(), blockactiondata.a().getZ(), 64.0D, dimension, new PacketPlayOutBlockAction(blockactiondata.a(), blockactiondata.d(), blockactiondata.b(), blockactiondata.c()));
                }
            }

            this.S[i].clear();
        }

    }

    private boolean a(BlockActionData blockactiondata) {
        IBlockData iblockdata = this.getType(blockactiondata.a());

        return iblockdata.getBlock() == blockactiondata.d() && iblockdata.getBlock().a(this, blockactiondata.a(), iblockdata, blockactiondata.b(), blockactiondata.c());
    }

    public void saveLevel() {
        this.dataManager.a();
    }

    protected void p() {
        boolean flag = this.S();

        super.p();

        if (flag != this.S()) {
            // Only send weather packets to those affected
            for (EntityHuman player : this.players) {
                if (player.world == this) {
                    ((EntityPlayer) player).setPlayerWeather((!flag ? WeatherType.DOWNFALL : WeatherType.CLEAR), false);
                }
            }
        }
        for (EntityHuman player : this.players) {
            if (player.world == this) {
                ((EntityPlayer) player).updateWeather(this.o, this.p, this.q, this.r);
            }
        }
        // CraftBukkit end

    }

    protected int q() {
        return this.server.getPlayerList().s();
    }

    public MinecraftServer getMinecraftServer() {
        return this.server;
    }

    public EntityTracker getTracker() {
        return this.tracker;
    }

    public PlayerChunkMap getPlayerChunkMap() {
        return this.manager;
    }

    public PortalTravelAgent getTravelAgent() {
        return this.Q;
    }

    public void a(EnumParticle enumparticle, double d0, double d1, double d2, int i, double d3, double d4, double d5, double d6, int... aint) {
        this.a(enumparticle, false, d0, d1, d2, i, d3, d4, d5, d6, aint);
    }

    public void a(EnumParticle enumparticle, boolean flag, double d0, double d1, double d2, int i, double d3, double d4, double d5, double d6, int... aint) {
        // CraftBukkit - visibility api support
        sendParticles(null, enumparticle, flag, d0, d1, d2, i, d3, d4, d5, d6, aint);
    }

    public void sendParticles(EntityPlayer sender, EnumParticle enumparticle, boolean flag, double d0, double d1, double d2, int i, double d3, double d4, double d5, double d6, int... aint) {
        // CraftBukkit end
        PacketPlayOutWorldParticles packetplayoutworldparticles = new PacketPlayOutWorldParticles(enumparticle, flag, (float) d0, (float) d1, (float) d2, (float) d3, (float) d4, (float) d5, (float) d6, i, aint);

        for (int j = 0; j < this.players.size(); ++j) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(j);
            if (sender != null && !entityplayer.getBukkitEntity().canSee(sender.getBukkitEntity()))
                continue; // CraftBukkit
            BlockPosition blockposition = entityplayer.getChunkCoordinates();
            double d7 = blockposition.c(d0, d1, d2);

            if (d7 <= 256.0D || flag && d7 <= 65536.0D) {
                entityplayer.playerConnection.sendPacket(packetplayoutworldparticles);
            }
        }

    }

    public Entity getEntity(UUID uuid) {
        return (Entity) this.entitiesByUUID.get(uuid);
    }

    public ListenableFuture<Object> postToMainThread(Runnable runnable) {
        return this.server.postToMainThread(runnable);
    }

    public boolean isMainThread() {
        return this.server.isMainThread();
    }

    public ChunkProviderServer getChunkProviderServer() {
        return chunkProviderServer;
    }

    static class BlockActionDataList extends ArrayList<BlockActionData> {

        private BlockActionDataList() {
        }

        BlockActionDataList(Object object) {
            this();
        }
    }
}
