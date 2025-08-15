package net.minecraft.server;

import com.destroystokyo.paper.utils.CachedSizeConcurrentLinkedQueue;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import io.github.cspigot.cSpigot;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.ResourceLeakDetector;
import joptsimple.OptionSet;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;
import org.bukkit.craftbukkit.util.ServerShutdownThread;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SlackActivityAccountant;
import org.spigotmc.SpigotConfig;

import io.github.cspigot.api.chunk.generator.VoidWorldGenerator;
import io.github.cspigot.feature.chunk.AsyncChunkProvider;
import io.github.cspigot.chunk.AsyncPriority;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.async.lighting.PaperLightingQueue;
import io.github.cspigot.feature.tickloop.ReentrantIAsyncHandler;
import io.github.cspigot.feature.tickloop.TasksPerTick;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
// CraftBukkit end

public abstract class MinecraftServer extends ReentrantIAsyncHandler<TasksPerTick> implements Runnable, ICommandListener, IAsyncTaskHandler, IMojangStatistics {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final File a = new File("usercache.json");
    private static MinecraftServer l;
    public Convertable convertable;
    //private final MojangStatisticsGenerator n = new MojangStatisticsGenerator("server", this, az());
    public File universe;
    private final List<IUpdatePlayerListBox> p = Lists.newArrayList();
    protected final ICommandHandler b;
    public final MethodProfiler methodProfiler = new MethodProfiler();
    private ServerConnection q; // Spigot
    private final ServerPing r = new ServerPing();
    private final Random s = new Random();
    private String serverIp;
    private int u = -1;
    public WorldServer[] worldServer;
    private PlayerList v;
    protected volatile boolean isRunning;
    private boolean isRestarting = false; // Paper - flag to signify we're attempting to restart
    private boolean isStopped;
    private int ticks;
    protected final Proxy e;
    public String f;
    public int g;
    private boolean spawnAnimals;
    private boolean spawnNPCs;
    private boolean pvpMode;
    private boolean allowFlight;
    private String motd;
    private int F;
    private int G = 0;
    public final long[] h = new long[100];
    public long[][] i;
    private KeyPair H;
    private String I;
    private String J;
    private boolean demoMode;
    private boolean N;
    private String O = "";
    private String P = "";
    private boolean T;
    private boolean U;
    private final YggdrasilAuthenticationService V;
    private final MinecraftSessionService W;
    private long X = 0L;
    private final GameProfileRepository Y;
    private final UserCache Z;
    protected final Queue<FutureTask<?>> j = new ConcurrentLinkedQueue<>(); // Spigot, PAIL: taskQueue
    public final Queue<CompletableFuture<?>> awaitableTasks = new CachedSizeConcurrentLinkedQueue<>(); // cSpigot: Await delayed async tasks
    public final Queue<CompletableFuture<?>> entityTrackerTasks = new CachedSizeConcurrentLinkedQueue<>(); // cSpigot: Await delayed async tasks
    private final Collection<NetworkManager> disabledFlushes = SettingsConfig.ASYNC_ENTITY_TRACKER ? new ConcurrentLinkedQueue<>() : new ArrayList<>(); // cSpigot - Async Entity Tracker
    private Thread serverThread;
    private long ab = az();

    // CraftBukkit start
    public List<WorldServer> worlds = new CopyOnWriteArrayList<>();
    public org.bukkit.craftbukkit.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
    public static int currentTick = 0; // PaperSpigot - Further improve tick loop
    public final Thread primaryThread;

    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public java.util.Queue<Runnable> priorityProcessQueue = new CachedSizeConcurrentLinkedQueue<>(); // cSpigot

    public int autosavePeriod;
    // CraftBukkit end

    private long nextTickTime;
    private long delayedTasksMaxNextTickTime;
    private boolean mayHaveDelayedTasks;
    public boolean forceTicks;
    private long lastOverloadWarning;

    public volatile Thread shutdownThread; // Paper

    public static <S extends MinecraftServer> S spin(Function<Thread, S> serverFactory) {
        AtomicReference<S> reference = new AtomicReference<>();
        Thread thread = new Thread(() -> reference.get().run(), "Server thread");

        thread.setUncaughtExceptionHandler((thread1, throwable) -> MinecraftServer.LOGGER.error(throwable));
        S server = serverFactory.apply(thread); // CraftBukkit - decompile error

        reference.set(server);
        thread.setPriority(Thread.NORM_PRIORITY + 2); // Paper - boost priority
        thread.start();
        return server;
    }

    public MinecraftServer(OptionSet options, Proxy proxy, Thread thread, YggdrasilAuthenticationService yggdrasilauthenticationservice, MinecraftSessionService minecraftsessionservice, GameProfileRepository gameprofilerepository, UserCache usercache) {
        super("Server"); // cSpigot - backport modern tick loop
        io.netty.util.ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED); // Spigot - disable
        this.isRunning = true;
        this.e = proxy;
        MinecraftServer.l = this;
        this.b = this.h();
        this.V = yggdrasilauthenticationservice;
        this.W = minecraftsessionservice;
        this.Y = gameprofilerepository;
        this.Z = usercache;

        // cSpigot start - backport modern tick loop
        this.nextTickTime = SystemUtils.getMonotonicMillis(); // cSpigot
        this.serverThread = thread;
        this.primaryThread = thread;
        // cSpigot end

        this.options = options;

        Runtime.getRuntime().addShutdownHook(new ServerShutdownThread(this));
        //this.serverThread = primaryThread = new Thread(this, "Server thread"); // Moved from main
    }

    public abstract PropertyManager getPropertyManager();

    protected CommandDispatcher h() {
        return new CommandDispatcher();
    }

    protected abstract boolean init() throws IOException;

    protected void a(String s) {
        if (this.getConvertable().isConvertable(s)) {
            MinecraftServer.LOGGER.info("Converting map!");
            this.getConvertable().convert(s, new IProgressUpdate() {
                private long b = System.currentTimeMillis();

                public void a(String s) {
                }

                public void a(int i) {
                    if (System.currentTimeMillis() - this.b >= 1000L) {
                        this.b = System.currentTimeMillis();
                        MinecraftServer.LOGGER.info("Converting... " + i + "%");
                    }

                }

                public void c(String s) {
                }
            });
        }

    }

    protected void a(String s, String s1, long i, WorldType worldtype, String s2) {
        // cSpigot start - Native SWM support
        // https://github.com/Grinderwolf/Slime-World-Manager/blob/develop/slimeworldmanager-classmodifier/src/main/resources/v1_8_R3/MinecraftServer_loadWorlds.txt
        WorldServer[] defaultWorlds = (WorldServer[]) com.grinderwolf.swm.clsm.ClassModifier.getDefaultWorlds();

        // cSpigot start - Native Continued SWM support
        // https://github.com/Swofty-Developments/Continued-Slime-World-Manager/blob/develop/swoftyworldmanager-classmodifier/src/main/resources/v1_8_R3/MinecraftServer_loadWorlds.txt
        if (defaultWorlds == null) {
            defaultWorlds = (WorldServer[]) net.swofty.swm.clsm.ClassModifier.getDefaultWorlds();
        }
        // cSpigot end

        if (defaultWorlds != null) {
            LOGGER.info("[SWM-Native] Overriding default worlds");
            this.a(s);
            this.worldServer = new WorldServer[3];

            for (int index = 0; index < 3; index++) {
                if (index == 1 && !this.getAllowNether()) {
                    continue;
                }

                if (index == 2 && !this.server.getAllowEnd()) {
                    continue;
                }

                WorldServer world = defaultWorlds[index];
                byte dimension = (byte) (index == 1 ? -1 : (index == 2 ? 1 : 0));

                WorldSettings worldSettings = new WorldSettings(i, this.getGamemode(), this.getGenerateStructures(), this.isHardcore(), worldtype);
                String worldTypeString = org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();
                String name;

                if (dimension == 0) {
                    name = s;
                } else {
                    name = s + "_" + worldTypeString;
                }

                if (world == null) {
                    worldSettings.setGeneratorSettings(s2);
                }

                if (index == 0) {
                    if (world == null) {
                        IDataManager nbtStorage = new ServerNBTManager(this.server.getWorldContainer(), s1, true);

                        WorldData worldData = nbtStorage.getWorldData();
                        if (worldData == null) {
                            worldData = new WorldData(worldSettings, s1);
                        }
                        worldData.checkName(s1);

                        org.bukkit.generator.ChunkGenerator gen = SettingsConfig.FORCE_VOID_WORLD ? new VoidWorldGenerator() : this.server.getGenerator(name); // cSpigot - VoidWorld Generator

                        world = (WorldServer) new WorldServer(this, nbtStorage, worldData, dimension, this.methodProfiler, org.bukkit.World.Environment.getEnvironment(dimension), gen).b();
                        world.a(worldSettings);
                    } else {
                        SpigotConfig.disableStatSaving = true;
                    }

                    this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
                } else if (world == null) {
                    IDataManager nbtStorage = new ServerNBTManager(this.server.getWorldContainer(), name, true);

                    WorldData worldData = nbtStorage.getWorldData();
                    if (worldData == null) {
                        worldData = new WorldData(worldSettings, name);
                    }
                    worldData.checkName(name);

                    org.bukkit.generator.ChunkGenerator gen = SettingsConfig.FORCE_VOID_WORLD ? new VoidWorldGenerator() : this.server.getGenerator(name); // cSpigot - VoidWorld Generator

                    world = (WorldServer) new SecondaryWorldServer(this, nbtStorage, dimension, this.worlds.get(0), this.methodProfiler, worldData, org.bukkit.World.Environment.getEnvironment(dimension), gen).b();
                }

                this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));

                if (defaultWorlds[index] == null) {
                    world.addIWorldAccess(new WorldManager(this, world));
                }

                if (!this.T()) {
                    world.getWorldData().setGameType(this.getGamemode());
                }

                this.worlds.add(world);
                this.getPlayerList().setPlayerFileData(this.worlds.toArray(new WorldServer[0]));
            }

            this.a(this.getDifficulty());
            this.k();

            return;
        }
        // cSpigot end

        this.a(s);
        this.worldServer = new WorldServer[3];
        int worldCount = 3;

        for (int j = 0; j < worldCount; ++j) {
            WorldServer world;
            byte dimension = 0;

            if (j == 1) {
                if (getAllowNether()) {
                    dimension = -1;
                } else {
                    continue;
                }
            }

            if (j == 2) {
                if (server.getAllowEnd()) {
                    dimension = 1;
                } else {
                    continue;
                }
            }

            String worldType = org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();
            String name = (dimension == 0) ? s : s + "_" + worldType;

            org.bukkit.generator.ChunkGenerator gen = SettingsConfig.FORCE_VOID_WORLD ? new VoidWorldGenerator() : this.server.getGenerator(name); // cSpigot - VoidWorld Generator
            WorldSettings worldsettings = new WorldSettings(i, this.getGamemode(), this.getGenerateStructures(), this.isHardcore(), worldtype);
            worldsettings.setGeneratorSettings(s2);

            if (j == 0) {
                IDataManager idatamanager = new ServerNBTManager(server.getWorldContainer(), s1, true);
                WorldData worlddata = idatamanager.getWorldData();
                if (worlddata == null) {
                    worlddata = new WorldData(worldsettings, s1);
                }
                worlddata.checkName(s1); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
                if (this.X()) {
                    world = (WorldServer) (new DemoWorldServer(this, idatamanager, worlddata, dimension, this.methodProfiler, org.bukkit.World.Environment.getEnvironment(dimension), gen)).b();
                } else {
                    world = (WorldServer) (new WorldServer(this, idatamanager, worlddata, dimension, this.methodProfiler, org.bukkit.World.Environment.getEnvironment(dimension), gen)).b();
                }

                world.a(worldsettings);
                this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
            } else {
                String dim = "DIM" + dimension;

                File newWorld = new File(new File(name), dim);
                File oldWorld = new File(new File(s), dim);

                if ((!newWorld.isDirectory()) && (oldWorld.isDirectory())) {
                    MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder required ----");
                    MinecraftServer.LOGGER.info("Unfortunately due to the way that Minecraft implemented multiworld support in 1.6, Bukkit requires that you move your " + worldType + " folder to a new location in order to operate correctly.");
                    MinecraftServer.LOGGER.info("We will move this folder for you, but it will mean that you need to move it back should you wish to stop using Bukkit in the future.");
                    MinecraftServer.LOGGER.info("Attempting to move " + oldWorld + " to " + newWorld + "...");

                    if (newWorld.exists()) {
                        MinecraftServer.LOGGER.warn("A file or folder already exists at " + newWorld + "!");
                        MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                    } else if (newWorld.getParentFile().mkdirs()) {
                        if (oldWorld.renameTo(newWorld)) {
                            MinecraftServer.LOGGER.info("Success! To restore " + worldType + " in the future, simply move " + newWorld + " to " + oldWorld);
                            // Migrate world data too.
                            try {
                                com.google.common.io.Files.copy(new File(new File(s), "level.dat"), new File(new File(name), "level.dat"));
                            } catch (IOException exception) {
                                MinecraftServer.LOGGER.warn("Unable to migrate world data.");
                            }
                            MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder complete ----");
                        } else {
                            MinecraftServer.LOGGER.warn("Could not move folder " + oldWorld + " to " + newWorld + "!");
                            MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                        }
                    } else {
                        MinecraftServer.LOGGER.warn("Could not create path for " + newWorld + "!");
                        MinecraftServer.LOGGER.info("---- Migration of old " + worldType + " folder failed ----");
                    }
                }

                IDataManager idatamanager = new ServerNBTManager(server.getWorldContainer(), name, true);
                // world =, b0 to dimension, s1 to name, added Environment and gen
                WorldData worlddata = idatamanager.getWorldData();
                if (worlddata == null) {
                    worlddata = new WorldData(worldsettings, name);
                }
                worlddata.checkName(name); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
                world = (WorldServer) new SecondaryWorldServer(this, idatamanager, dimension, this.worlds.get(0), this.methodProfiler, worlddata, org.bukkit.World.Environment.getEnvironment(dimension), gen).b();
            }

            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));

            world.addIWorldAccess(new WorldManager(this, world));
            if (!this.T()) {
                world.getWorldData().setGameType(this.getGamemode());
            }

            worlds.add(world);
            getPlayerList().setPlayerFileData(worlds.toArray(new WorldServer[worlds.size()]));
        }

        // CraftBukkit end
        this.a(this.getDifficulty());
        this.k(); // Load worlds
    }

    protected void k() {
        int i = 0;

        // CraftBukkit start - fire WorldLoadEvent and handle whether to keep the spawn in memory
        for (int m = 0; m < worlds.size(); m++) {
            WorldServer worldserver = this.worlds.get(m);
            LOGGER.info("Preparing start region for level " + m + " (Seed: " + worldserver.getSeed() + ")");

            if (!worldserver.getWorld().getKeepSpawnInMemory()) {
                continue;
            }

            BlockPosition blockposition = worldserver.getSpawn();
            long j = az();
            i = 0;

            for (int k = -192; k <= 192 && this.isRunning(); k += 16) {
                for (int l = -192; l <= 192 && this.isRunning(); l += 16) {
                    long i1 = az();

                    if (i1 - j > 1000L) {
                        this.a_("Preparing spawn area", i * 100 / 625);
                        j = i1;
                    }

                    ++i;

                    if (SettingsConfig.ASYNC_SPAWN_CHUNKS) {
                        worldserver.chunkProviderServer.getChunkAtFuture(blockposition.getX() + k >> 4, blockposition.getZ() + l >> 4,
                                true, true, AsyncPriority.NORMAL, null); // cSpigot - use new methods
                    } else {
                        worldserver.chunkProviderServer.getChunkAt(blockposition.getX() + k >> 4, blockposition.getZ() + l >> 4);
                    }
                }
            }
        }

        for (WorldServer world : this.worlds) {
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(world.getWorld()));
            CraftWorld craftWorld = world.getWorld();
            craftWorld.setKeepSpawnInMemory(world.paperSpigotConfig.keepSpawnInMemory);
        }

        // CraftBukkit end
        this.s();
    }

    protected void a(String s, IDataManager idatamanager) {
        File file = new File(idatamanager.getDirectory(), "resources.zip");

        if (file.isFile()) {
            this.setResourcePack("level://" + s + "/" + file.getName(), "");
        }

    }

    public abstract boolean getGenerateStructures();

    public abstract WorldSettings.EnumGamemode getGamemode();

    public abstract EnumDifficulty getDifficulty();

    public abstract boolean isHardcore();

    public abstract int p();

    public abstract boolean q();

    public abstract boolean r();

    protected void a_(String s, int i) {
        this.f = s;
        this.g = i;
        MinecraftServer.LOGGER.info(s + ": " + i + "%");
    }

    protected void s() {
        this.f = null;
        this.g = 0;

        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD); // CraftBukkit
    }

    protected void saveChunks(boolean flag) throws ExceptionWorldConflict { // CraftBukkit - added throws
        if (!this.N) {
            WorldServer[] aworldserver = this.worldServer;
            int i = aworldserver.length;

            // CraftBukkit start
            for (WorldServer worldserver : worlds) {
                // CraftBukkit end

                if (worldserver != null) {
                    if (!flag) {
                        MinecraftServer.LOGGER.info("Saving chunks for level \'" + worldserver.getWorldData().getName() + "\'/" + worldserver.worldProvider.getName());
                    }

                    try {
                        worldserver.save(true, (IProgressUpdate) null);
                        worldserver.saveLevel(); // CraftBukkit
                    } catch (ExceptionWorldConflict exceptionworldconflict) {
                        MinecraftServer.LOGGER.warn(exceptionworldconflict.getMessage());
                    }
                }
            }

        }
    }

    // CraftBukkit start
    @Getter
    protected boolean hasStopped = false;
    private final Object stopLock = new Object();

    public final boolean hasStopped() {
        synchronized (stopLock) {
            return hasStopped;
        }
    }
    // CraftBukkit end

    public void stop() throws ExceptionWorldConflict { // CraftBukkit - added throws
        // CraftBukkit start - prevent double stopping on multiple threads
        synchronized (this.stopLock) {
            if (this.hasStopped) return;
            this.hasStopped = true;
        }
        // CraftBukkit end

        AsyncChunkProvider.stop(this); // Paper

        MinecraftServer.LOGGER.info("Stopping server");
        //SpigotTimings.stopServer(); // Spigot

        cSpigot.getInstance().shutDown();

        // CraftBukkit start
        if (this.server != null) {
            this.server.disablePlugins();
            this.server.waitForAsyncTasksShutdown(); // Paper
        }
        // CraftBukkit end
        if (this.getServerConnection() != null) {
            this.getServerConnection().shutdown();
        }

        if (this.getPlayerList() != null) {
            MinecraftServer.LOGGER.info("Saving players");
            this.getPlayerList().savePlayers();
            this.getPlayerList().u(isRestarting); // kickPlayers
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            } // CraftBukkit - SPIGOT-625 - give server at least a chance to send packets
        }

        if (this.worldServer != null) {
            MinecraftServer.LOGGER.info("Saving worlds");
            this.saveChunks(false);
        }

        // cSpigot start - remove snooper
//            if (this.n.d()) {
//                this.n.e();
//            }
        // cSpigot end

        // Spigot start
        MCUtil.asyncExecutor.shutdown(); // Paper
        try {
            MCUtil.asyncExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS); // Paper
        } catch (java.lang.InterruptedException ignored) {
        } // Paper
        if (org.spigotmc.SpigotConfig.saveUserCacheOnStopOnly) {
            LOGGER.info("Saving usercache.json");
            this.getUserCache().c(false); // Paper
        }
        // Spigot end

        LOGGER.info("Closing Server");

        try {
            if (SettingsConfig.SYSTEM_DEBUG) LOGGER.info("Closing console");
            ServerShutdownThread.handledConsole = true;
            net.minecrell.terminalconsole.TerminalConsoleAppender.close(); // Paper - Use TerminalConsoleAppender
        } catch (Exception ignored) {
        }
    }

    public String getServerIp() {
        return this.serverIp;
    }

    //PAIL: setServerIP
    public void c(String s) {
        this.serverIp = s;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    // Paper start - allow passing of the intent to restart
    public void safeShutdown() {
        safeShutdown(false);
    }

    public void safeShutdown(boolean isRestarting) {
        this.isRunning = false;
        this.isRestarting = isRestarting;
    }
    // Paper end

    public static final long SEC_IN_NANO = 1000000000;
    public static final int SAMPLE_INTERVAL = 20; // Paper
    public static int TPS = 20;
    public static int TICK_TIME = 1000000000 / TPS;
    private long lastTick = 0;
    private long catchupTime = 0;

    public final RollingAverage tps = new RollingAverage(1);
    public final RollingAverage tps1 = new RollingAverage(60);
    public final RollingAverage tps5 = new RollingAverage(60 * 5);
    public final RollingAverage tps15 = new RollingAverage(60 * 15);
    public double[] recentTps = new double[4]; // PaperSpigot - Fine have your darn compat with bad plugins
    public final SlackActivityAccountant slackActivityAccountant = new SlackActivityAccountant();

    public static class RollingAverage {
        private final int size;
        private long time;
        private java.math.BigDecimal total;
        private int index = 0;
        private final java.math.BigDecimal[] samples;
        private final long[] times;

        public RollingAverage(int size) {
            this.size = size;
            this.time = size * SEC_IN_NANO;
            this.total = dec(TPS).multiply(dec(SEC_IN_NANO)).multiply(dec(size));
            this.samples = new java.math.BigDecimal[size];
            this.times = new long[size];
            for (int i = 0; i < size; i++) {
                this.samples[i] = dec(TPS);
                this.times[i] = SEC_IN_NANO;
            }
        }

        private static java.math.BigDecimal dec(long t) {
            return new java.math.BigDecimal(t);
        }

        public void add(java.math.BigDecimal x, long t) {
            time -= times[index];
            total = total.subtract(samples[index].multiply(dec(times[index])));
            samples[index] = x;
            times[index] = t;
            time += t;
            total = total.add(x.multiply(dec(t)));
            if (++index == size) {
                index = 0;
            }
        }

        public double getAverage() {
            return total.divide(dec(time), 30, java.math.RoundingMode.HALF_UP).doubleValue();
        }
    }

    private static final java.math.BigDecimal TPS_BASE = new java.math.BigDecimal("1E9").multiply(new java.math.BigDecimal(SAMPLE_INTERVAL));
    // PaperSpigot End

    public void run() {
        try {
            if (this.init()) {
                this.ab = az();
                this.r.setMOTD(new ChatComponentText(this.motd));
                this.r.setServerInfo(new ServerPing.ServerData("1.8.8", 47));
                this.a(this.r);

                Arrays.fill(recentTps, 20);
                long start = System.nanoTime(), curTime, tickSection = start, timeUntilNextTick = 1000L / TPS; // Paper - Further improve server tick loop
                lastTick = start - TICK_TIME; // Paper
                while (this.isRunning) {
                    long i = ((curTime = System.nanoTime()) / (1000L * 1000L)) - this.nextTickTime; // Paper
                    if (i > 5000L && this.nextTickTime - this.lastOverloadWarning >= 30000L && ticks > 1500) { // CraftBukkit // cSpigot - prevent display of overload on first 500 ticks
                        long j = i / 50L;
                        if (this.server.getWarnOnOverload()) // CraftBukkit
                            MinecraftServer.LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                        this.nextTickTime += j * 50L;
                        this.lastOverloadWarning = this.nextTickTime;
                    }

                    if (++MinecraftServer.currentTick % SAMPLE_INTERVAL == 0) {
                        final long diff = curTime - tickSection;
                        java.math.BigDecimal currentTps = TPS_BASE.divide(new java.math.BigDecimal(diff), 30, java.math.RoundingMode.HALF_UP);
                        tps.add(currentTps, diff);
                        tps1.add(currentTps, diff);
                        tps5.add(currentTps, diff);
                        tps15.add(currentTps, diff);
                        // Backwards compat with bad plugins
                        recentTps[0] = tps.getAverage();
                        recentTps[1] = tps1.getAverage();
                        recentTps[2] = tps5.getAverage();
                        recentTps[3] = tps15.getAverage();
                        // Paper end
                        tickSection = curTime;
                    }
                    // Tuinity - replace logic

                    lastTick = curTime;
                    this.nextTickTime += timeUntilNextTick;

                    this.A(this::haveTime);
                    this.mayHaveDelayedTasks = true;
                    this.delayedTasksMaxNextTickTime = Math.max(getMillis() + timeUntilNextTick, this.nextTickTime);
                    this.waitUntilNextTick();
                    //this.methodProfiler.b(); // pop // Purpur
                }
                // Spigot end
            } else {
                this.a((CrashReport) null);
            }
        } catch (Throwable throwable) {
            MinecraftServer.LOGGER.error("Encountered an unexpected exception", throwable);
            // Spigot Start
            if (throwable.getCause() != null) {
                MinecraftServer.LOGGER.error("\tCause of unexpected exception was", throwable.getCause());
            }
            // Spigot End
            CrashReport crashreport = null;

            if (throwable instanceof ReportedException) {
                crashreport = this.b(((ReportedException) throwable).a());
            } else {
                crashreport = this.b(new CrashReport("Exception in server tick loop", throwable));
            }

            File file = new File(new File(this.y(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.a(file)) {
                MinecraftServer.LOGGER.error("This crash report has been saved to: " + file.getAbsolutePath());
            } else {
                MinecraftServer.LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.a(crashreport);
        } finally {
            try {
                this.isStopped = true;
                this.stop();
            } catch (Throwable throwable1) {
                MinecraftServer.LOGGER.error("Exception stopping the server", throwable1);
            } finally {
                this.z();
            }
        }
    }

    // cSpigot start - backport modern tick loop
    private boolean haveTime() {
        // CraftBukkit start
        if (isOversleep) return canOversleep();// Paper - because of our changes, this logic is broken
        return this.forceTicks || this.runningTask() || getMillis() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTime : this.nextTickTime);
    }

    // Paper start
    boolean isOversleep = false;

    private boolean canOversleep() {
        return this.mayHaveDelayedTasks && getMillis() < this.delayedTasksMaxNextTickTime;
    }

    private boolean canSleepForTickNoOversleep() {
        return this.forceTicks || this.runningTask() || getMillis() < this.nextTickTime;
    }
    // Paper end

    protected void waitUntilNextTick() {
        this.controlTerminate(() -> !this.canSleepForTickNoOversleep());
    }

    @Override
    protected TasksPerTick packUpRunnable(@NotNull Runnable runnable) {
        // Paper start - anything that does try to post to main during watchdog crash, run on watchdog
        if (this.hasStopped && Thread.currentThread().equals(shutdownThread)) {
            runnable.run();
            runnable = () -> {
            };
        }
        // Paper end
        return new TasksPerTick(this.ticks, runnable);
    }

    @Override
    protected boolean shouldRun(@NotNull TasksPerTick task) {
        return task.getTick() + 3 < this.ticks || this.haveTime();
    }

    @Override
    public boolean drawRunnable() {
        boolean flag = super.drawRunnable();

        this.mayHaveDelayedTasks = flag;
        return flag;
    }

    @Override
    public @NotNull Thread getMainThread() {
        return serverThread;
    }
    // cSpigot end

    private void a(ServerPing serverping) {
        File file = this.d("server-icon.png");

        if (file.isFile()) {
            ByteBuf bytebuf = Unpooled.buffer();
            ByteBuf bytebuf1 = null; // Paper - cleanup favicon bytebuf

            try {
                BufferedImage bufferedimage = ImageIO.read(file);

                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
                /* ByteBuf */
                bytebuf1 = Base64.encode(bytebuf); // Paper - cleanup favicon bytebuf

                serverping.setFavicon("data:image/png;base64," + bytebuf1.toString(Charsets.UTF_8));
            } catch (Exception exception) {
                MinecraftServer.LOGGER.error("Couldn't load server icon", exception);
            } finally {
                bytebuf.release();
                // Paper start - cleanup favicon bytebuf
                if (bytebuf1 != null) {
                    bytebuf1.release();
                }
                // Paper end - cleanup favicon bytebuf
            }
        }

    }

    public File y() {
        return new File(".");
    }

    protected void a(CrashReport crashreport) {
    }

    protected void z() {
    }

    // cSpigot - backport modern tick loop
    protected void A(BooleanSupplier shouldKeepTicking) throws ExceptionWorldConflict { // CraftBukkit - added throws
        //co.aikar.timings.TimingsManager.FULL_SERVER_TICK.startTiming(); // Spigot // cSpigot

        // cSpigot start - backport modern tick loop
        long i = getNanos();
        long tickStartTime = i; // Paper

        // Paper start - move oversleep into full server tick
        isOversleep = true;
        this.controlTerminate(() -> !this.canOversleep());
        isOversleep = false;
        // Paper end

        this.server.getPluginManager().callEvent(new com.destroystokyo.paper.event.server.ServerTickStartEvent(this.ticks + 1)); // Paper
        // cSpigot end

        ++this.ticks;
        if (this.T) {
            this.T = false;
        }
        this.B(shouldKeepTicking);
        if (i - this.X >= 5000000000L) {
            this.X = i;
            this.r.setPlayerSample(new ServerPing.ServerPingPlayerSample(this.J(), this.I()));
            GameProfile[] agameprofile = new GameProfile[Math.min(this.I(), org.spigotmc.SpigotConfig.playerSample)]; // Paper
            int j = MathHelper.nextInt(this.s, 0, this.I() - agameprofile.length);

            for (int k = 0; k < agameprofile.length; ++k) {
                agameprofile[k] = this.v.v().get(j + k).getProfile();
            }

            Collections.shuffle(Arrays.asList(agameprofile));
            this.r.b().a(agameprofile);
        }

        if (autosavePeriod > 0 && this.ticks % autosavePeriod == 0) { // CraftBukkit
            // SpigotTimings.worldSaveTimer.startTiming(); // Spigot // Purpur
            this.v.savePlayers();
            // Spigot Start
            // We replace this with saving each individual world as this.saveChunks(...) is broken,
            // and causes the main thread to sleep for random amounts of time depending on chunk activity
            // Also pass flag to only save modified chunks
            server.playerCommandState = true;
            for (World world : worlds) {
                world.getWorld().save(false);
            }
            server.playerCommandState = false;
            // this.saveChunks(true);
            // Spigot End
            // SpigotTimings.worldSaveTimer.stopTiming(); // Spigot // Purpur
        }

        // Await Entity Tracker and allow previous tick's packets to be sent
        if (!SettingsConfig.SEMI_ASYNC_TRACKER && SettingsConfig.ASYNC_ENTITY_TRACKER && SettingsConfig.AWAIT_ENTITY_TRACKER) {
            while (entityTrackerTasks.peek() != null) {
                try {
                    entityTrackerTasks.poll().join(); // TODO: Find whether changing this to a get() could fix crashes. I dont believe it would, but if we get crashes, then this is a potential solution
                } catch (Throwable t) {
                    warning("Error while ticking server: ");
                    t.printStackTrace();
                }
            }
            while (((Queue<NetworkManager>) disabledFlushes).peek() != null) {
                ((Queue<NetworkManager>) disabledFlushes).poll().enableAutomaticFlush();
            }
        }

        // Paper start - move executeAll() into full server tick timing
        this.runAllRunnable();
        // Paper end

        // Await the delayed entity tracker tasks
        // These all *should* have completed ages ago, but we need to be sure in case the server is overloaded.
        if (SettingsConfig.AWAIT_ENTITY_UPDATES) {
            long delaySoFar = 0;
            long startTime = System.currentTimeMillis();
            while (awaitableTasks.peek() != null) {
                final CompletableFuture<?> awaitableTask = awaitableTasks.poll();
                try {
                    awaitableTask.get(30, TimeUnit.SECONDS);
                } catch (TimeoutException | InterruptedException e) {
                    delaySoFar += (System.currentTimeMillis() - startTime);
                    startTime = System.currentTimeMillis();

                    warning("A tick-complete-await task has failed to complete in time: " + (delaySoFar / 1000));

                    if (delaySoFar > Duration.ofSeconds(5).toMillis()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spark profiler --upload"); // give us debug info

                        try {
                            Thread.sleep(10000); // wait for the upload
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }

                        final CrashReport crashreport = CrashReport.a(e, "Exception ticking world");
                        // Spigot End
                        throw new ReportedException(crashreport);
                    }
                } catch (CancellationException e) {
                    warning("A tick-complete-await task was cancelled before completion.");
                } catch (ExecutionException e) {
                    warning("A tick-complete-await task encountered an error during execution.");
                    e.printStackTrace();
                }
            }
        } else {
            awaitableTasks.clear();
        }

//        if (getSnooperEnabled() && !this.n.d() && this.ticks > 100) {  // Spigot
//            this.n.a();
//        }
//
//        if (getSnooperEnabled() && this.ticks % 6000 == 0) { // Spigot
//            this.n.b();
//        }

        // cSpigot start - backport modern tick loop
        this.h[this.ticks % 100] = System.nanoTime() - i;
        // cSpigot end

        if (SettingsConfig.QUEUE_LIGHT_UPDATES) {
            PaperLightingQueue.processQueue(tickStartTime); // Paper
        }

        org.spigotmc.WatchdogThread.tick(); // Spigot

        // Paper start
        long endTime = System.nanoTime();
        long remaining = (TICK_TIME - (endTime - lastTick)) - catchupTime;
        double lastMspt = ((double) (endTime - lastTick) / 1000000D);
        this.server.getPluginManager().callEvent(new com.destroystokyo.paper.event.server.ServerTickEndEvent(this.ticks, lastMspt, remaining));
        // Paper end

        // cSpigot start - Async Monitor
        if (this.ticks % 40 == 0.0) {
            if (awaitableTasks.size() > 700) {
                warning("[cSpigot AsyncMonitor] AwaitableTasks is incredibly large: " + awaitableTasks.size());
            }

            if (entityTrackerTasks.size() > 450) {
                warning("[cSpigot AsyncMonitor] EntityTrackerTasks is incredibly large: " + entityTrackerTasks.size());
            }

            if (processQueue.size() > 500) {
                warning("[cSpigot AsyncMonitor] ProcessQueue is incredibly large: " + processQueue.size());
            }

            int taskSize = this.j.size();
            if (taskSize > 700) {
                warning("[cSpigot AsyncMonitor] Main Thread Task Queue is incredibly large: " + taskSize);
            }

            if (disabledFlushes.size() > 1000 && getPlayerCount() < disabledFlushes.size() * 2) {
                warning("[cSpigot AsyncMonitor] There seems to be a large amount of disabled network managers. " +
                        "This is likely a bug: " + disabledFlushes.size() + " - " + getPlayerCount());
            }
        }
        // cSpigot end

        //co.aikar.timings.TimingsManager.FULL_SERVER_TICK.stopTiming(); // Paper // Purpur
    }

    public void B(BooleanSupplier shouldKeepTicking) {
        //SpigotTimings.minecraftSchedulerTimer.startTiming(); // Spigot // Purpur

        // Spigot start
        Queue<FutureTask<?>> queue = this.j;
        FutureTask<?> entry;
        int count = queue.size();
        while (count-- > 0 && (entry = queue.poll()) != null) {
            SystemUtils.a(entry, MinecraftServer.LOGGER);
        }
        // Spigot end
        //SpigotTimings.minecraftSchedulerTimer.stopTiming(); // Spigot // Purpur

        if (SettingsConfig.ASYNC_CHUNKS) AsyncChunkProvider.processMainThreadQueue(this); // Paper

        //SpigotTimings.bukkitSchedulerTimer.startTiming(); // Spigot // Purpur
        // CraftBukkit start
        this.server.getScheduler().mainThreadHeartbeat(this.ticks);
        //SpigotTimings.bukkitSchedulerTimer.stopTiming(); // Spigot // Purpur

        // Run tasks that are waiting on processing
        //SpigotTimings.processQueueTimer.startTiming(); // Spigot // Purpur
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
        //SpigotTimings.processQueueTimer.stopTiming(); // Spigot // Purpur

        //SpigotTimings.chunkIOTickTimer.startTiming(); // Spigot // Purpur
        if (!SettingsConfig.ASYNC_CHUNKS) {
            ChunkIOExecutor.tick();
        }
        //SpigotTimings.chunkIOTickTimer.stopTiming(); // Spigot // Purpur

        //SpigotTimings.timeUpdateTimer.startTiming(); // Spigot // Purpur
        // Paper start - optimize time updates
        if ((this.ticks % 20) == 0) {
            for (WorldServer world : this.worlds) {
                final boolean doDaylight = world.getGameRules().getBoolean("doDaylightCycle");
                final long dayTime = world.getDayTime();
                long worldTime = world.getTime();
                final PacketPlayOutUpdateTime worldPacket = new PacketPlayOutUpdateTime(worldTime, dayTime, doDaylight);
                for (EntityHuman entityhuman : world.players) {
                    if (!(entityhuman instanceof EntityPlayer)) {//|| (ticks + entityhuman.getId()) % 20 != 0
                        continue;
                    }
                    if (entityhuman.world == world) {
                        EntityPlayer entityplayer = (EntityPlayer) entityhuman;
                        long playerTime = entityplayer.getPlayerTime();
                        PacketPlayOutUpdateTime packet = (playerTime == dayTime) ? worldPacket : new PacketPlayOutUpdateTime(worldTime, playerTime, doDaylight);
                        entityplayer.playerConnection.sendPacket(packet); // Add support for per player time
                    }
                }
            }
        }
        // Paper end

        //SpigotTimings.timeUpdateTimer.stopTiming(); // Spigot // Purpur

        for (WorldServer worldserver : this.worlds) {
            if (SettingsConfig.ASYNC_CHUNKS) AsyncChunkProvider.processMainThreadQueue(worldserver); // Paper
            worldserver.hasPhysicsEvent = org.bukkit.event.block.BlockPhysicsEvent.getHandlerList().getRegisteredListeners().length > 0; // Paper

            CrashReport crashreport;

            try {
                //worldserver.timings.doTick.startTiming(); // Spigot // Purpur
                worldserver.doTick(shouldKeepTicking);
                //worldserver.timings.doTick.stopTiming(); // Spigot // Purpur
            } catch (Throwable throwable) {
                // Spigot Start
                try {
                    crashreport = CrashReport.a(throwable, "Exception ticking world");
                } catch (Throwable t) {
                    throw new RuntimeException("Error generating crash report", t);
                }
                // Spigot End
                worldserver.a(crashreport);
                throw new ReportedException(crashreport);
            }

            try {
                //worldserver.timings.tickEntities.startTiming(); // Spigot // Purpur
                worldserver.tickEntities();
                //worldserver.timings.tickEntities.stopTiming(); // Spigot // Purpur
            } catch (Throwable throwable1) {
                // Spigot Start
                try {
                    crashreport = CrashReport.a(throwable1, "Exception ticking world entities");
                } catch (Throwable t) {
                    throw new RuntimeException("Error generating crash report", t);
                }
                // Spigot End
                worldserver.a(crashreport);
                throw new ReportedException(crashreport);
            }

            //worldserver.timings.tracker.startTiming(); // Spigot //Purpur
            if (MinecraftServer.getServer().getPlayerList().getPlayerCount() != 0) // Tuinity
            {
                // Tuinity start - controlled flush for entity tracker packets
                for (EntityPlayer player : MinecraftServer.getServer().getPlayerList().players) {
                    PlayerConnection connection = player.playerConnection;
                    if (connection != null) {
                        connection.networkManager.disableAutomaticFlush();
                        disabledFlushes.add(connection.networkManager);
                    }
                }
                // Tuinity end

                // cSpigot start
                Runnable runnable = () -> {
                    // Tuinity start - controlled flush for entity tracker packets
                    try {
                        if (this.getPlayerList().getPlayerCount() > 0) { // cSpigot - Skip updating entity tracker if zero players online
                            worldserver.getTracker().updatePlayers();
                        }
                    } finally {
                        final Iterator<NetworkManager> iterator = disabledFlushes.iterator();
                        while (iterator.hasNext()) {
                            iterator.next().enableAutomaticFlush();
                            iterator.remove();
                        }
                    }
                    // Tuinity end
                };
                if (SettingsConfig.ASYNC_ENTITY_TRACKER) {
                    if (SettingsConfig.SEMI_ASYNC_TRACKER) {
                        runnable.run();
                    } else if (SettingsConfig.AWAIT_ENTITY_TRACKER) {
                        entityTrackerTasks.add(AsyncUtil.runAsyncWithFuture(worldserver.updateTracker, cSpigot.getInstance().getThreadManager().getTrackerThreadPool()));
                    } else {
                        AsyncUtil.runTimed(runnable, cSpigot.getInstance().getThreadManager().getDispatcherThread());
                    }
                } else {
                    runnable.run();
                }
                // cSpigot end
            }

            if (!SettingsConfig.AWAIT_ENTITY_TRACKER && (!SettingsConfig.ASYNC_ENTITY_TRACKER || SettingsConfig.SEMI_ASYNC_TRACKER) && disabledFlushes.size() > 0) {
                LOGGER.info("[cSpigot] The entity tracker failed to re-enable network managers completely! " + disabledFlushes.size() + " managers were not re-enabled. This is a bug! Please report this to the cSpigot team!");
            }

            //worldserver.timings.tracker.stopTiming(); // Spigot // Pupur
            worldserver.explosionDensityCache.clear(); // PaperSpigot - Optimize explosions
            worldserver.movementCache.clear(); // IonSpigot - Movement Cache
        }

        // cSpigot start - priority process queue
        while (!priorityProcessQueue.isEmpty()) {
            priorityProcessQueue.poll().run();
        }
        // cSpigot end

        //SpigotTimings.connectionTimer.startTiming(); // Spigot // Purpur
        this.aq().c();
        //SpigotTimings.connectionTimer.stopTiming(); // Spigot // Purpur

        //SpigotTimings.playerListTimer.startTiming(); // Spigot // Purpur
        this.v.tick();
        //SpigotTimings.playerListTimer.stopTiming(); // Spigot // Purpur

        //SpigotTimings.tickablesTimer.startTiming(); // Spigot // Purpur

        // DO NOT CHANGE THIS, Retarded anticheats check for the size method being called.
        for (int i = 0; i < this.p.size(); ++i) {
            this.p.get(i).c();
        }
        //SpigotTimings.tickablesTimer.stopTiming(); // Spigot // Purpur

    }

    public boolean getAllowNether() {
        return true;
    }

    public void a(IUpdatePlayerListBox iupdateplayerlistbox) {
        this.p.add(iupdateplayerlistbox);
    }

    public void C() {
        /* CraftBukkit start - prevent abuse
        this.serverThread = new Thread(this, "Server thread");
        this.serverThread.start();
        // CraftBukkit end */
    }

    public File d(String s) {
        return new File(this.y(), s);
    }

    public void info(String s) {
        MinecraftServer.LOGGER.info(s);
    }

    public void warning(String s) {
        MinecraftServer.LOGGER.warn(s);
    }

    public WorldServer getWorldServer(int i) {
        // CraftBukkit start
        for (WorldServer world : worlds) {
            if (world.dimension == i) {
                return world;
            }
        }
        return worlds.get(0);
        // CraftBukkit end
    }

    public String E() {
        return this.serverIp;
    }

    public int F() {
        return this.u;
    }

    public String G() {
        return this.motd;
    }

    public String getVersion() {
        return "1.8.8";
    }

    public int I() {
        return this.v.getPlayerCount();
    }

    public int J() {
        return this.v.getMaxPlayers();
    }

    public String[] getPlayers() {
        return this.v.f();
    }

    public GameProfile[] L() {
        return this.v.g();
    }

    public boolean isDebugging() {
        return this.getPropertyManager().getBoolean("debug", false); // CraftBukkit - don't hardcode
    }

    public void g(String s) {
        MinecraftServer.LOGGER.error(s);
    }

    public void h(String s) {
        if (this.isDebugging()) {
            MinecraftServer.LOGGER.info(s);
        }

    }

    public String getServerModName() {
        return "cSpigot"; // cSpigot - cSpigot > // PaperSpigot - PaperSpigot > // Spigot - Spigot > // CraftBukkit - cb > vanilla!
    }

    public CrashReport b(CrashReport crashreport) {
        crashreport.g().a("Profiler Position", new Callable() {
            public String a() throws Exception {
                return "N/A (disabled)";
            }

            public Object call() throws Exception {
                return this.a();
            }
        });
        if (this.v != null) {
            crashreport.g().a("Player Count", new Callable() {
                public String a() {
                    return MinecraftServer.this.v.getPlayerCount() + " / " + MinecraftServer.this.v.getMaxPlayers() + "; " + MinecraftServer.this.v.v();
                }

                public Object call() throws Exception {
                    return this.a();
                }
            });
        }

        return crashreport;
    }

    public List<String> tabCompleteCommand(ICommandListener icommandlistener, String s, BlockPosition blockposition) {
        return server.tabComplete(icommandlistener, s, blockposition); // PaperSpigot - add Location argument
        // CraftBukkit end
    }

    public static MinecraftServer getServer() {
        return MinecraftServer.l;
    }

    public Iterable<WorldServer> getWorlds() {
        return this.worlds;
    }

    public boolean O() {
        return true; // CraftBukkit
    }

    public String getName() {
        return "Server";
    }

    public void sendMessage(IChatBaseComponent ichatbasecomponent) {
        MinecraftServer.LOGGER.info(org.bukkit.craftbukkit.util.CraftChatMessage.fromComponent(ichatbasecomponent, net.minecraft.server.EnumChatFormat.WHITE));// Paper - Log message with colors
    }

    public boolean a(int i, String s) {
        return true;
    }

    public ICommandHandler getCommandHandler() {
        return this.b;
    }

    public KeyPair Q() {
        return this.H;
    }

    public int R() {
        return this.u;
    }

    public void setPort(int i) {
        this.u = i;
    }

    public String S() {
        return this.I;
    }

    public void i(String s) {
        this.I = s;
    }

    public boolean T() {
        return this.I != null;
    }

    public String U() {
        return this.J;
    }

    public void setWorld(String s) {
        this.J = s;
    }

    public void a(KeyPair keypair) {
        this.H = keypair;
    }

    public void a(EnumDifficulty enumdifficulty) {
        // CraftBukkit start
        for (int i = 0; i < this.worlds.size(); ++i) {
            WorldServer worldserver = this.worlds.get(i);
            // CraftBukkit end

            if (worldserver != null) {
                if (worldserver.getWorldData().isHardcore()) {
                    worldserver.getWorldData().setDifficulty(EnumDifficulty.HARD);
                    worldserver.setSpawnFlags(true, true);
                } else if (this.T()) {
                    worldserver.getWorldData().setDifficulty(enumdifficulty);
                    worldserver.setSpawnFlags(worldserver.getDifficulty() != EnumDifficulty.PEACEFUL, true);
                } else {
                    worldserver.getWorldData().setDifficulty(enumdifficulty);
                    worldserver.setSpawnFlags(this.getSpawnMonsters(), this.spawnAnimals);
                }
            }
        }

    }

    protected boolean getSpawnMonsters() {
        return true;
    }

    public boolean X() {
        return this.demoMode;
    }

    public void b(boolean flag) {
        this.demoMode = flag;
    }

    public Convertable getConvertable() {
        return this.convertable;
    }

    public void aa() {
        // cSpigot start - not needed
//        this.N = true;
//        this.getConvertable().d();
//
//        // CraftBukkit start
//        for (int i = 0; i < this.worlds.size(); ++i) {
//            WorldServer worldserver = this.worlds.get(i);
//            // CraftBukkit end
//
//            if (worldserver != null) {
//                worldserver.saveLevel();
//            }
//        }
//
//        this.getConvertable().e(this.worlds.get(0).getDataManager().g()); // CraftBukkit
//        this.safeShutdown();
        // cSpigot end
    }

    public String getResourcePack() {
        return this.O;
    }

    public String getResourcePackHash() {
        return this.P;
    }

    public void setResourcePack(String s, String s1) {
        this.O = s;
        this.P = s1;
    }

    // cSpigot start - Disable snooper

    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
//        mojangstatisticsgenerator.a("whitelist_enabled", Boolean.valueOf(false));
//        mojangstatisticsgenerator.a("whitelist_count", Integer.valueOf(0));
//        if (this.v != null) {
//            mojangstatisticsgenerator.a("players_current", Integer.valueOf(this.I()));
//            mojangstatisticsgenerator.a("players_max", Integer.valueOf(this.J()));
//            mojangstatisticsgenerator.a("players_seen", Integer.valueOf(this.v.getSeenPlayers().length));
//        }
//
//        mojangstatisticsgenerator.a("uses_auth", Boolean.valueOf(this.onlineMode));
//        mojangstatisticsgenerator.a("gui_state", this.as() ? "enabled" : "disabled");
//        mojangstatisticsgenerator.a("run_time", Long.valueOf((az() - mojangstatisticsgenerator.g()) / 60L * 1000L));
//        mojangstatisticsgenerator.a("avg_tick_ms", Integer.valueOf((int) (MathHelper.a(this.h) * 1.0E-6D)));
//        int i = 0;
//
//        if (this.worldServer != null) {
//            // CraftBukkit start
//            for (int j = 0; j < this.worlds.size(); ++j) {
//                WorldServer worldserver = this.worlds.get(j);
//                if (worldserver != null) {
//                    // CraftBukkit end
//                    WorldData worlddata = worldserver.getWorldData();
//
//                    mojangstatisticsgenerator.a("world[" + i + "][dimension]", Integer.valueOf(worldserver.worldProvider.getDimension()));
//                    mojangstatisticsgenerator.a("world[" + i + "][mode]", worlddata.getGameType());
//                    mojangstatisticsgenerator.a("world[" + i + "][difficulty]", worldserver.getDifficulty());
//                    mojangstatisticsgenerator.a("world[" + i + "][hardcore]", Boolean.valueOf(worlddata.isHardcore()));
//                    mojangstatisticsgenerator.a("world[" + i + "][generator_name]", worlddata.getType().name());
//                    mojangstatisticsgenerator.a("world[" + i + "][generator_version]", Integer.valueOf(worlddata.getType().getVersion()));
//                    mojangstatisticsgenerator.a("world[" + i + "][height]", Integer.valueOf(this.F));
//                    mojangstatisticsgenerator.a("world[" + i + "][chunks_loaded]", Integer.valueOf(worldserver.N().getLoadedChunks()));
//                    ++i;
//                }
//            }
//        }
//
//        mojangstatisticsgenerator.a("worlds", Integer.valueOf(i));
    }

    public void b(MojangStatisticsGenerator mojangstatisticsgenerator) {
//        mojangstatisticsgenerator.b("singleplayer", Boolean.valueOf(this.T()));
//        mojangstatisticsgenerator.b("server_brand", this.getServerModName());
//        mojangstatisticsgenerator.b("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
//        mojangstatisticsgenerator.b("dedicated", Boolean.valueOf(this.ae()));
    }

    public boolean getSnooperEnabled() {
        return false; // disable snooper
    }
    // cSpigot end

    public abstract boolean ae();

    public boolean getOnlineMode() {
        return server.getOnlineMode(); // CraftBukkit
    }

    public boolean getSpawnAnimals() {
        return this.spawnAnimals;
    }

    public void setSpawnAnimals(boolean flag) {
        this.spawnAnimals = flag;
    }

    public boolean getSpawnNPCs() {
        return this.spawnNPCs;
    }

    public abstract boolean ai();

    public abstract ServerConnection.EventGroupType getTransport();

    public void setSpawnNPCs(boolean flag) {
        this.spawnNPCs = flag;
    }

    public boolean getPVP() {
        return this.pvpMode;
    }

    public void setPVP(boolean flag) {
        this.pvpMode = flag;
    }

    public boolean getAllowFlight() {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean flag) {
        this.allowFlight = flag;
    }

    public abstract boolean getEnableCommandBlock();

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String s) {
        this.motd = s;
    }

    public int getMaxBuildHeight() {
        return this.F;
    }

    // PAIL: setMaxBuildHeight
    public void c(int i) {
        this.F = i;
    }

    public boolean isStopped() {
        return this.isStopped;
    }

    public PlayerList getPlayerList() {
        return this.v;
    }

    // PAIL: setPlayerList
    public void a(PlayerList playerlist) {
        this.v = playerlist;
    }

    public void setGamemode(WorldSettings.EnumGamemode gameMode) {
        for (WorldServer world : this.worlds) {
            world.getWorldData().setGameType(gameMode);
        }
    }

    public int getPlayerCount() {
        return this.getPlayerList().getPlayerCount();
    }

    public int getMaxPlayers() {
        return this.getPlayerList().getMaxPlayers();
    }

    public ServerPing getServerPing() {
        return this.r;
    }

    // Spigot Start
    public ServerConnection getServerConnection() {
        return this.q;
    }

    // Spigot End
    public ServerConnection aq() {
        return this.q == null ? this.q = new ServerConnection(this) : this.q; // Spigot
    }

    public boolean as() {
        return false;
    }

    public abstract String a(WorldSettings.EnumGamemode worldsettings_enumgamemode, boolean flag);

    public int at() {
        return this.ticks;
    }

    public void au() {
        this.T = true;
    }

    public BlockPosition getChunkCoordinates() {
        return BlockPosition.ZERO;
    }

    public Vec3D d() {
        return new Vec3D(0.0D, 0.0D, 0.0D);
    }

    public World getWorld() {
        return this.worlds.get(0); // CraftBukkit
    }

    public Entity f() {
        return null;
    }

    public int getSpawnProtection() {
        return 16;
    }

    public boolean a(World world, BlockPosition blockposition, EntityHuman entityhuman) {
        return false;
    }

    public void setForceGamemode(boolean flag) {
        this.U = flag;
    }

    public boolean getForceGamemode() {
        return this.U;
    }

    public Proxy ay() {
        return this.e;
    }

    // cSpigot start - backport modern tick loo
    public static long az() {
        return getMillis();
    }

    public static long getMillis() {
        return getNanos() / 1000000L;
    }

    public static long getNanos() {
        return System.nanoTime(); // Paper
    }
    // cSpigot end

    public int getIdleTimeout() {
        return this.G;
    }

    public void setIdleTimeout(int i) {
        this.G = i;
    }

    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatComponentText(this.getName());
    }

    public boolean aB() {
        return true;
    }

    public MinecraftSessionService getMinecraftSessionService() {
        return this.aD();
    } // cSpigot - OBFHELPER

    public MinecraftSessionService aD() {
        return this.W;
    }

    public GameProfileRepository getGameProfileRepository() {
        return this.Y;
    }

    public UserCache getUserCache() {
        return this.Z;
    }

    public ServerPing aG() {
        return this.r;
    }

    public void aH() {
        this.X = 0L;
    }

    public Entity a(UUID uuid) {
        WorldServer[] aworldserver = this.worldServer;
        int i = aworldserver.length;

        // CraftBukkit start
        for (int j = 0; j < worlds.size(); ++j) {
            WorldServer worldserver = worlds.get(j);
            // CraftBukkit end

            if (worldserver != null) {
                Entity entity = worldserver.getEntity(uuid);

                if (entity != null) {
                    return entity;
                }
            }
        }

        return null;
    }

    public boolean getSendCommandFeedback() {
        return getServer().worlds.get(0).getGameRules().getBoolean("sendCommandFeedback");
    }

    public void a(CommandObjectiveExecutor.EnumCommandResult commandobjectiveexecutor_enumcommandresult, int i) {
    }

    public int aI() {
        return 29999984;
    }

    public <V> ListenableFuture<V> a(Callable<V> callable) {
        Validate.notNull(callable);
        if (!this.isMainThread()) { // CraftBukkit && !this.isStopped()) {
            ListenableFutureTask listenablefuturetask = ListenableFutureTask.create(callable);
            // Spigot start
            this.j.add(listenablefuturetask);
            return listenablefuturetask;
            // Spigot end
        } else {
            try {
                return Futures.immediateFuture(callable.call());
            } catch (Exception exception) {
                return Futures.immediateFailedCheckedFuture(exception);
            }
        }
    }

    public ListenableFuture<Object> postToMainThread(Runnable runnable) {
        Validate.notNull(runnable);
        return this.a(Executors.callable(runnable));
    }

    public boolean isMainThread() {
        return Thread.currentThread() == this.serverThread;
    }

    public int aK() {
        return 256;
    }

    public long aL() {
        return this.ab;
    }

    public Thread aM() {
        return this.serverThread;
    }
}
