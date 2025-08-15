package net.minecraft.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import io.github.cspigot.cSpigot;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.craftbukkit.Main;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

import org.spigotmc.SpigotConfig;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.console.cSpigotConsole;
import io.github.cspigot.utils.traffic.APHSystem;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
// CraftBukkit end

public class DedicatedServer extends MinecraftServer implements IMinecraftServer {

    private static final Logger LOGGER = LogManager.getLogger();
    private final java.util.Queue<ServerCommand> l = new java.util.concurrent.ConcurrentLinkedQueue<>(); // Paper - use a proper queue
    private RemoteStatusListener m;
    private RemoteControlListener n;
    public PropertyManager propertyManager;
    private EULA p;
    private boolean generateStructures;
    private WorldSettings.EnumGamemode r;
    private boolean s;

    // CraftBukkit start - Signature changed
    public DedicatedServer(joptsimple.OptionSet options, Thread thread1, YggdrasilAuthenticationService authenticationService, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, UserCache userCache) {
        super(options, Proxy.NO_PROXY, thread1, authenticationService, minecraftSessionService, gameProfileRepository, userCache);
        // CraftBukkit end
//        Thread thread = new Thread("Server Infinisleeper") {
//            {
//                this.setDaemon(true);
//                this.start();
//            }
//
//            public void run() {
//                while (true) {
//                    try {
//                        Thread.sleep(2147483647L);
//                    } catch (InterruptedException interruptedexception) {
//                        ;
//                    }
//                }
//            }
//        };
    }

    protected boolean init() throws IOException {
        Thread thread = new Thread("Server console handler") {
            public void run() {
                // CraftBukkit start
                if (!Main.useConsole) {
                    return;
                }
                // CraftBukkit end

                // Paper start
                new cSpigotConsole(DedicatedServer.this).start();
                // Paper end
            }
        };

        // CraftBukkit start
        java.util.logging.Logger global = java.util.logging.Logger.getLogger("");
        global.setUseParentHandlers(false);
        for ( java.util.logging.Handler handler : global.getHandlers() ) {
            global.removeHandler(handler);
        }
        global.addHandler(new org.bukkit.craftbukkit.util.ForwardLogHandler());

        // Paper start - Not needed with TerminalConsoleAppender
        final org.apache.logging.log4j.Logger logger = LogManager.getRootLogger();
        /*
        final org.apache.logging.log4j.core.Logger logger = ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger());
        for (org.apache.logging.log4j.core.Appender appender : logger.getAppenders().values()) {
            if (appender instanceof org.apache.logging.log4j.core.appender.ConsoleAppender) {
                logger.removeAppender(appender);
            }
        }

        new Thread(new org.bukkit.craftbukkit.util.TerminalConsoleWriterThread(System.out, this.reader)).start();
         */
        // Paper end

        // Paper start - Use Log4j IOStreams
        System.setOut(org.apache.logging.log4j.io.IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
        System.setErr(org.apache.logging.log4j.io.IoBuilder.forLogger(logger).setLevel(Level.WARN).buildPrintStream());
        // Paper end
        // CraftBukkit end

        thread.setDaemon(true);
        thread.start();

        DedicatedServer.LOGGER.info("Starting minecraft server version 1.8.8 with cSpigot " + cSpigot.version);
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            DedicatedServer.LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        DedicatedServer.LOGGER.info("Loading properties");
        this.propertyManager = new PropertyManager(this.options); // CraftBukkit - CLI argument support
        this.p = new EULA(new File("eula.txt"));
        // Spigot Start
        boolean eulaAgreed = Boolean.getBoolean("com.mojang.eula.agree");
        if (eulaAgreed) {
            System.err.println("You have used the Spigot command line EULA agreement flag.");
            System.err.println("By using this setting you are indicating your agreement to Mojang's EULA (https://account.mojang.com/documents/minecraft_eula).");
            System.err.println("If you do not agree to the above EULA please stop your server and remove this flag immediately.");
        }

        // Spigot End
        if (!this.p.a() && !eulaAgreed) { // Spigot
            DedicatedServer.LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
            this.p.b();
            return false;
        } else {
            if (this.T()) {
                this.c("127.0.0.1");
            } else {
                this.c(this.propertyManager.getString("server-ip", ""));
            }

            this.setSpawnAnimals(this.propertyManager.getBoolean("spawn-animals", true));
            this.setSpawnNPCs(this.propertyManager.getBoolean("spawn-npcs", true));
            this.setPVP(this.propertyManager.getBoolean("pvp", true));
            this.setAllowFlight(this.propertyManager.getBoolean("allow-flight", false));
            this.setResourcePack(this.propertyManager.getString("resource-pack", ""), this.propertyManager.getString("resource-pack-hash", ""));
            this.setMotd(this.propertyManager.getString("motd", "A Minecraft Server"));
            this.setForceGamemode(this.propertyManager.getBoolean("force-gamemode", false));
            this.setIdleTimeout(this.propertyManager.getInt("player-idle-timeout", 0));
            if (this.propertyManager.getInt("difficulty", 1) < 0) {
                this.propertyManager.setProperty("difficulty", 0);
            } else if (this.propertyManager.getInt("difficulty", 1) > 3) {
                this.propertyManager.setProperty("difficulty", 3);
            }

            this.generateStructures = this.propertyManager.getBoolean("generate-structures", true);
            int i = this.propertyManager.getInt("gamemode", WorldSettings.EnumGamemode.SURVIVAL.getId());
            this.propertyManager.setProperty("announce-player-achievements", false); // cSpigot - Force disable this

            this.r = WorldSettings.a(i);
            DedicatedServer.LOGGER.info("Default game type: " + this.r);
            InetAddress inetaddress = null;

            if (this.getServerIp().length() > 0) {
                inetaddress = InetAddress.getByName(this.getServerIp());
            }

            if (this.R() < 0) {
                this.setPort(this.propertyManager.getInt("server-port", 25565));
            }

            // Spigot start
            this.a(new DedicatedPlayerList(this));
            // Spigot end
            org.spigotmc.SpigotConfig.init((File) options.valueOf("spigot-settings"));
            org.spigotmc.SpigotConfig.registerCommands();
            // PaperSpigot start
            org.github.paperspigot.PaperSpigotConfig.init((File) options.valueOf("paper-settings"));
            org.github.paperspigot.PaperSpigotConfig.registerCommands();
            // PaperSpigot end

            DedicatedServer.LOGGER.info("Generating keypair");
            this.a(MinecraftEncryption.b());
            DedicatedServer.LOGGER.info("Starting Minecraft server on " + (this.getServerIp().length() == 0 ? "*" : this.getServerIp()) + ":" + this.R());

            if (!org.spigotmc.SpigotConfig.lateBind) {
                try {
                    this.aq().a(inetaddress, this.R());
                } catch (IOException ioexception) {
                    DedicatedServer.LOGGER.warn("**** FAILED TO BIND TO PORT!");
                    DedicatedServer.LOGGER.warn("The exception was: {}", ioexception.toString());
                    DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
                    return false;
                }
            }

            new Timer("AntiMalware Executor Pool").scheduleAtFixedRate(new APHSystem(), 0, Duration.ofMinutes(2).toMillis());

            // Spigot Start - Move DedicatedPlayerList up and bring plugin loading from CraftServer to here
            // this.a((PlayerList) (new DedicatedPlayerList(this))); // CraftBukkit
            server.loadPlugins();
            server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.STARTUP);
            // Spigot End
            // cSpigot start
            cSpigot cSpigot = io.github.cspigot.cSpigot.getInstance();
            cSpigot.getServerSidePlugin().enable(cSpigot); // cSpigot
            cSpigot.init();
            // cSpigot end

            if (!this.getOnlineMode() && !SpigotConfig.bungee) {
                DedicatedServer.LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
                DedicatedServer.LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
                DedicatedServer.LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
            }

            if (this.aR()) {
                this.getUserCache().c(false);
            }

            if (!NameReferencingFileConverter.a(this.propertyManager)) {
                return false;
            } else {
                this.convertable = new WorldLoaderServer(server.getWorldContainer()); // CraftBukkit - moved from MinecraftServer constructor
                long j = System.nanoTime();

                if (this.U() == null) {
                    this.setWorld(this.propertyManager.getString("level-name", "world"));
                }

                String s = this.propertyManager.getString("level-seed", "");
                String s1 = this.propertyManager.getString("level-type", "DEFAULT");
                String s2 = this.propertyManager.getString("generator-settings", "");
                long k = (new Random()).nextLong();

                if (s.length() > 0) {
                    try {
                        long l = Long.parseLong(s);

                        if (l != 0L) {
                            k = l;
                        }
                    } catch (NumberFormatException numberformatexception) {
                        k = (long) s.hashCode();
                    }
                }

                WorldType worldtype = WorldType.getType(s1);

                if (worldtype == null) {
                    worldtype = WorldType.NORMAL;
                }

                this.aB();
                this.getEnableCommandBlock();
                this.p();
                this.getSnooperEnabled();
                this.aK();
                this.c(this.propertyManager.getInt("max-build-height", 256));
                this.c((this.getMaxBuildHeight() + 8) / 16 * 16);
                this.c(MathHelper.clamp(this.getMaxBuildHeight(), 64, 256));
                this.propertyManager.setProperty("max-build-height", this.getMaxBuildHeight());
                UserCache.a(this.getOnlineMode());
                DedicatedServer.LOGGER.info("Preparing level \"" + this.U() + "\"");
                this.a(this.U(), this.U(), k, worldtype, s2);
                long i1 = System.nanoTime() - j;
                String s3 = String.format("%.3fs", (double) i1 / 1.0E9D);

                DedicatedServer.LOGGER.info("Done (" + s3 + ")! For help, type \"help\" or \"?\"");
                if (this.propertyManager.getBoolean("enable-query", false)) {
                    DedicatedServer.LOGGER.info("Starting GS4 status listener");
                    this.m = new RemoteStatusListener(this);
                    this.m.a();
                }

                if (this.propertyManager.getBoolean("enable-rcon", false)) {
                    DedicatedServer.LOGGER.info("Starting remote control listener");
                    this.n = new RemoteControlListener(this);
                    this.n.a();
                    this.remoteConsole = new org.bukkit.craftbukkit.command.CraftRemoteConsoleCommandSender(); // CraftBukkit
                }

                // CraftBukkit start
                if (this.server.getBukkitSpawnRadius() > -1) {
                    DedicatedServer.LOGGER.info("'settings.spawn-radius' in bukkit.yml has been moved to 'spawn-protection' in server.properties. I will move your config for you.");
                    this.propertyManager.properties.remove("spawn-protection");
                    this.propertyManager.getInt("spawn-protection", this.server.getBukkitSpawnRadius());
                    this.server.removeBukkitSpawnRadius();
                    this.propertyManager.savePropertiesFile();
                }
                // CraftBukkit end

                if (org.spigotmc.SpigotConfig.lateBind) {
                    try {
                        this.aq().a(inetaddress, this.R());
                    } catch (IOException ioexception) {
                        DedicatedServer.LOGGER.warn("**** FAILED TO BIND TO PORT!");
                        DedicatedServer.LOGGER.warn("The exception was: {}", new Object[]{ioexception.toString()});
                        DedicatedServer.LOGGER.warn("Perhaps a server is already running on that port?");
                        return false;
                    }
                }

                // Spigot - Disable WatchDog Thread
//                if (this.aS() > 0L) {
//                    Thread thread1 = new Thread(new ThreadWatchdog(this));
//
//                    thread1.setName("Server Watchdog");
//                    thread1.setDaemon(true);
//                    thread1.start();
//                }
                return true;
            }
        }
    }

    // CraftBukkit start
    public PropertyManager getPropertyManager() {
        return this.propertyManager;
    }
    // CraftBukkit end

    public void setGamemode(WorldSettings.EnumGamemode worldsettings_enumgamemode) {
        super.setGamemode(worldsettings_enumgamemode);
        this.r = worldsettings_enumgamemode;
    }

    public boolean getGenerateStructures() {
        return this.generateStructures;
    }

    public WorldSettings.EnumGamemode getGamemode() {
        return this.r;
    }

    public EnumDifficulty getDifficulty() {
        return EnumDifficulty.getById(this.propertyManager.getInt("difficulty", EnumDifficulty.NORMAL.a()));
    }

    public boolean isHardcore() {
        return this.propertyManager.getBoolean("hardcore", false);
    }

    protected void a(CrashReport crashreport) {
    }

    public CrashReport b(CrashReport crashreport) {
        crashreport = super.b(crashreport);
        crashreport.g().a("Is Modded", new Callable() {
            public String a() throws Exception {
                String s = DedicatedServer.this.getServerModName();

                return !s.equals("vanilla") ? "Definitely; Server brand changed to \'" + s + "\'" : "Unknown (can\'t tell)";
            }

            public Object call() throws Exception {
                return this.a();
            }
        });
        crashreport.g().a("Type", new Callable() {
            public String a() throws Exception {
                return "Dedicated Server (map_server.txt)";
            }

            public Object call() throws Exception {
                return this.a();
            }
        });
        return crashreport;
    }

    protected void z() {
        if (SettingsConfig.SYSTEM_DEBUG) LOGGER.info("Exiting VM");
        System.exit(0); // CraftBukkit
    }

    public void B(BooleanSupplier supplier) { // CraftBukkit - fix decompile error
        super.B(supplier);
        this.aO();
    }

    public boolean getAllowNether() {
        return this.propertyManager.getBoolean("allow-nether", true);
    }

    public boolean getSpawnMonsters() {
        return this.propertyManager.getBoolean("spawn-monsters", true);
    }

    public void a(MojangStatisticsGenerator mojangstatisticsgenerator) {
//        mojangstatisticsgenerator.a("whitelist_enabled", Boolean.valueOf(this.aP().getHasWhitelist()));
//        mojangstatisticsgenerator.a("whitelist_count", Integer.valueOf(this.aP().getWhitelisted().length));
//        super.a(mojangstatisticsgenerator);
    }

    public boolean getSnooperEnabled() {
        // cSpigot - disable snooper to save resources
        return false; //this.propertyManager.getBoolean("snooper-enabled", false);
    }

    public void issueCommand(String s, ICommandListener icommandlistener) {
        this.l.add(new ServerCommand(s, icommandlistener));
    }

    public void aO() {
        //SpigotTimings.serverCommandTimer.startTiming(); // Spigot // Purpur
        // [Paper-0350] start - use a Queue for Queueing Commands
        ServerCommand servercommand;
        while ((servercommand = this.l.poll()) != null) {
            // [Paper-0350] end - use a Queue for Queueing Commands

            // CraftBukkit start - ServerCommand for preprocessing
            ServerCommandEvent event = new ServerCommandEvent(console, servercommand.command);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) continue;
            servercommand = new ServerCommand(event.getCommand(), servercommand.source);

            // this.getCommandHandler().a(servercommand.source, servercommand.command); // Called in dispatchServerCommand
            server.dispatchServerCommand(console, servercommand);
            // CraftBukkit end
        }

        //SpigotTimings.serverCommandTimer.stopTiming(); // Spigot // Purpur
    }

    public boolean ae() {
        return true;
    }

    @Override
    public boolean ai() {
        return this.getPropertyManager().getBoolean("use-native-transport", true);
    }

    @Override
    public ServerConnection.EventGroupType getTransport() {
        try {
            return ServerConnection.EventGroupType
                    .valueOf(this.propertyManager.getString("transport-to-use", "default").toUpperCase());
        } catch (Exception ignored) {
            return ServerConnection.EventGroupType.DEFAULT;
        }
    }

    public DedicatedPlayerList aP() {
        return (DedicatedPlayerList) super.getPlayerList();
    }

    public int a(String s, int i) {
        return this.propertyManager.getInt(s, i);
    }

    public String a(String s, String s1) {
        return this.propertyManager.getString(s, s1);
    }

    public boolean a(String s, boolean flag) {
        return this.propertyManager.getBoolean(s, flag);
    }

    public void a(String s, Object object) {
        this.propertyManager.setProperty(s, object);
    }

    public void a() {
        this.propertyManager.savePropertiesFile();
    }

    public String b() {
        File file = this.propertyManager.c();

        return file != null ? file.getAbsolutePath() : "No settings file";
    }

    public void aQ() {
        ServerGUI.a(this);
        this.s = true;
    }

    public boolean as() {
        return this.s;
    }

    public String a(WorldSettings.EnumGamemode worldsettings_enumgamemode, boolean flag) {
        return "";
    }

    public boolean getEnableCommandBlock() {
        return this.propertyManager.getBoolean("enable-command-block", false);
    }

    public int getSpawnProtection() {
        return this.propertyManager.getInt("spawn-protection", super.getSpawnProtection());
    }

    public boolean a(World world, BlockPosition blockposition, EntityHuman entityhuman) {
        if (world.worldProvider.getDimension() != 0) {
            return false;
        } else if (this.aP().getOPs().isEmpty()) {
            return false;
        } else if (this.aP().isOp(entityhuman.getProfile())) {
            return false;
        } else if (this.getSpawnProtection() <= 0) {
            return false;
        } else {
            BlockPosition blockposition1 = world.getSpawn();
            int i = MathHelper.a(blockposition.getX() - blockposition1.getX());
            int j = MathHelper.a(blockposition.getZ() - blockposition1.getZ());
            int k = Math.max(i, j);

            return k <= this.getSpawnProtection();
        }
    }

    public int p() {
        return this.propertyManager.getInt("op-permission-level", 4);
    }

    public void setIdleTimeout(int i) {
        super.setIdleTimeout(i);
        this.propertyManager.setProperty("player-idle-timeout", Integer.valueOf(i));
        this.a();
    }

    public boolean q() {
        return this.propertyManager.getBoolean("broadcast-rcon-to-ops", true);
    }

    public boolean r() {
        return this.propertyManager.getBoolean("broadcast-console-to-ops", true);
    }

    public boolean aB() {
        return this.propertyManager.getBoolean("announce-player-achievements", false);
    }

    public int aI() {
        int i = this.propertyManager.getInt("max-world-size", super.aI());

        if (i < 1) {
            i = 1;
        } else if (i > super.aI()) {
            i = super.aI();
        }

        return i;
    }

    public int aK() {
        return this.propertyManager.getInt("network-compression-threshold", super.aK());
    }

    protected boolean aR() {
        boolean flag = false;

        int i;

        for ( i = 0; !flag && i <= 2; ++i ) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.aU();
            }

            flag = NameReferencingFileConverter.a((MinecraftServer) this);
        }

        boolean flag1 = false;

        for ( i = 0; !flag1 && i <= 2; ++i ) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.aU();
            }

            flag1 = NameReferencingFileConverter.b((MinecraftServer) this);
        }

        boolean flag2 = false;

        for ( i = 0; !flag2 && i <= 2; ++i ) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.aU();
            }

            flag2 = NameReferencingFileConverter.c((MinecraftServer) this);
        }

        boolean flag3 = false;

        for ( i = 0; !flag3 && i <= 2; ++i ) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered enter problem while converting the whitelist, retrying in enter few seconds");
                this.aU();
            }

            flag3 = NameReferencingFileConverter.d((MinecraftServer) this);
        }

        boolean flag4 = false;

        for ( i = 0; !flag4 && i <= 2; ++i ) {
            if (i > 0) {
                DedicatedServer.LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.aU();
            }

            flag4 = NameReferencingFileConverter.a(this, this.propertyManager);
        }

        return flag || flag1 || flag2 || flag3 || flag4;
    }

    private void aU() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException interruptedexception) {
            ;
        }
    }

    public long aS() {
        return this.propertyManager.getLong("max-tick-time", TimeUnit.MINUTES.toMillis(1L));
    }

    public String getPlugins() {
        // CraftBukkit start - Whole method
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && server.getQueryPlugins()) {
            result.append(": ");

            for ( int i = 0; i < plugins.length; i++ ) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    // CraftBukkit start - fire RemoteServerCommandEvent
    public String executeRemoteCommand(final String s) {
        Waitable<String> waitable = new Waitable<String>() {
            @Override
            protected String evaluate() {
                RemoteControlCommandListener.getInstance().i();
                // Event changes start
                RemoteServerCommandEvent event = new RemoteServerCommandEvent(remoteConsole, s);
                server.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return "";
                }
                // Event change end
                ServerCommand serverCommand = new ServerCommand(event.getCommand(), RemoteControlCommandListener.getInstance());
                server.dispatchServerCommand(remoteConsole, serverCommand);
                return RemoteControlCommandListener.getInstance().j();
            }
        };
        processQueue.add(waitable);
        try {
            return waitable.get();
        } catch (java.util.concurrent.ExecutionException e) {
            throw new RuntimeException("Exception processing rcon command " + s, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Maintain interrupted state
            throw new RuntimeException("Interrupted processing rcon command " + s, e);
        }
        // CraftBukkit end
    }

    public PlayerList getPlayerList() {
        return this.aP();
    }
}
