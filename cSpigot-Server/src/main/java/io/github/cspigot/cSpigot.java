package io.github.cspigot;

import io.github.cspigot.api.chunk.ChunkAPI;
import io.github.cspigot.api.handlers.PacketAPI;
import io.github.cspigot.api.handlers.impl.MovementHandler;
import io.github.cspigot.api.handlers.impl.PacketHandler;
import io.github.cspigot.api.knockback.KnockbackAPI;
import io.github.cspigot.cmds.impl.op.DeopCommand;
import io.github.cspigot.cmds.impl.op.OpCommand;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.async.threading.ThreadingManager;
import io.github.cspigot.feature.console.filter.LogFilter;
import io.github.cspigot.feature.pack.ResourcePackHandler;
import io.github.cspigot.knockback.KnockbackHandler;
import io.github.cspigot.plugin.ServerSidePlugin;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.Main;

import java.util.LinkedList;
import java.util.List;

@Getter @Log4j2
public class cSpigot {

    public static final String version = "5.0.0";

    private static volatile cSpigot instance;
    private static final Object INSTANCE_LOCK = new Object();

    @Getter private static KnockbackAPI knockbackAPI;
    @Getter private static PacketAPI packetAPI;
    @Getter private static ChunkAPI chunkAPI;

    @Getter private static SettingsConfig config;

    private boolean instantiated;

    private final List<MovementHandler> movementHandlers = new LinkedList<>();
    private final List<PacketHandler> packetHandlers = new LinkedList<>();

    private final KnockbackHandler knockbackHandler = new KnockbackHandler();
    private final ServerSidePlugin serverSidePlugin = new ServerSidePlugin();
    private final ThreadingManager threadManager = new ThreadingManager();
    private final ResourcePackHandler resourcePackHandler = new ResourcePackHandler();

    private cSpigot(SettingsConfig settingsConfig) {
        config = settingsConfig;
        knockbackAPI = new KnockbackAPI(this);
        packetAPI = new PacketAPI(this);
        chunkAPI = new ChunkAPI();
    }

    public static cSpigot getInstance() {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new cSpigot(Main.getConfig());
                }
            }
        }
        return instance;
    }

    public void init() {
        try {
            registerPlugin();
            instantiated = true;
            log.info("cSpigot " + version + " initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize cSpigot", e);
        }
    }

    public void initFilter() {
        if (!SettingsConfig.FILTER_ENABLED) {
            return;
        }

        try {
            LogFilter logFilter = new LogFilter();
            Logger logger = (Logger) LogManager.getRootLogger();
            logger.addFilter(logFilter);
            log.info("Log filter installed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize log filter", e);
        }
    }

    public void registerPlugin() {
        try {
            Bukkit.getServer().getCommandMap().register("spigot", new OpCommand());
            Bukkit.getServer().getCommandMap().register("spigot", new DeopCommand());
        } catch (Exception e) {
            log.error("Failed to register plugin commands", e);
        }
    }

    public void setupTps() {
        try {
            MinecraftServer.TPS = SettingsConfig.TARGET_TPS;
            MinecraftServer.TICK_TIME = 1000000000 / MinecraftServer.TPS;
            log.info("TPS configured to: " + SettingsConfig.TARGET_TPS);
        } catch (Exception e) {
            log.error("Failed to setup TPS", e);
        }
    }

    public void shutDown() {
        if (!instantiated) {
            return;
        }

        try {
            log.info("Shutting down cSpigot...");

            if (ServerSidePlugin.getInstance() != null && ServerSidePlugin.getInstance().isEnabled()) {
                Bukkit.getScheduler().cancelTasks(ServerSidePlugin.getInstance());
            }

            this.threadManager.shutdown();
            this.knockbackHandler.save();

            log.info("cSpigot shutdown complete");
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }
    }

    public void registerMovementHandler(MovementHandler movementHandler) {
        if (movementHandler == null) {
            log.warn("Attempted to register null MovementHandler");
            return;
        }
        this.movementHandlers.add(movementHandler);
    }

    public void removeMovementHandler(MovementHandler movementHandler) {
        this.movementHandlers.remove(movementHandler);
    }

    public void registerPacketHandler(PacketHandler packetHandler) {
        if (packetHandler == null) {
            log.warn("Attempted to register null PacketHandler");
            return;
        }
        this.packetHandlers.add(packetHandler);
    }

    public void removePacketHandler(PacketHandler packetHandler) {
        this.packetHandlers.remove(packetHandler);
    }
}