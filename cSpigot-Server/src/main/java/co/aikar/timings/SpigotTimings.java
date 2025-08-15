package co.aikar.timings;

import com.google.common.collect.MapMaker;
import net.minecraft.server.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.craftbukkit.scheduler.CraftTask;

import java.util.Map;

@Deprecated // Titanium (Purpur)
public final class SpigotTimings {

    public static final Timing playerListTimer = Timings.ofSafe("Player List");
    public static final Timing connectionTimer = Timings.ofSafe("Connection Handler");
    public static final Timing tickablesTimer = Timings.ofSafe("Tickables");
    public static final Timing minecraftSchedulerTimer = Timings.ofSafe("Minecraft Scheduler");
    public static final Timing bukkitSchedulerTimer = Timings.ofSafe("Bukkit Scheduler");
    public static final Timing chunkIOTickTimer = Timings.ofSafe("ChunkIOTick");
    public static final Timing timeUpdateTimer = Timings.ofSafe("Time Update");
    public static final Timing serverCommandTimer = Timings.ofSafe("Server Command");
    public static final Timing worldSaveTimer = Timings.ofSafe("World Save");

    public static final Timing tickEntityTimer = Timings.ofSafe("## tickEntity");
    public static final Timing tickTileEntityTimer = Timings.ofSafe("## tickTileEntity");

    public static final Timing processQueueTimer = Timings.ofSafe("processQueue");

    public static final Timing playerCommandTimer = Timings.ofSafe("playerCommand");

    public static final Timing entityActivationCheckTimer = Timings.ofSafe("entityActivationCheck");
    public static final Timing checkIfActiveTimer = Timings.ofSafe("checkIfActive");

    public static final Timing antiXrayUpdateTimer = Timings.ofSafe("anti-xray - update");
    public static final Timing antiXrayObfuscateTimer = Timings.ofSafe("anti-xray - obfuscate");

    private static final Map<Class<?>, String> taskNameCache = new MapMaker().weakKeys().makeMap();

    private SpigotTimings() {}

    /**
     * Gets a timer associated with a plugins tasks.
     */
    public static Timing getPluginTaskTimings(BukkitTask bukkitTask, long period) {
        if (!bukkitTask.isSync()) {
            return null;
        }
        Plugin plugin;

        CraftTask craftTask = (CraftTask) bukkitTask;

        final Class<?> taskClass = craftTask.getTaskClass();
        if (bukkitTask.getOwner() != null) {
            plugin = bukkitTask.getOwner();
        } else {
            plugin = TimingsManager.getPluginByClassloader(taskClass);
        }

        final String taskname = taskNameCache.computeIfAbsent(taskClass, clazz ->
                clazz.isAnonymousClass() || clazz.isLocalClass()
                        ? clazz.getName()
                        : clazz.getCanonicalName());

        StringBuilder name = new StringBuilder(64);
        name.append("Task: ").append(taskname);
        if (period > 0) {
            name.append(" (interval:").append(period).append(")");
        } else {
            name.append(" (Single)");
        }

        if (plugin == null) {
            return Timings.ofSafe(null, name.toString());
        }

        return Timings.ofSafe(plugin, name.toString());
    }

    /**
     * Get a named timer for the specified entity type to track type specific timings.
     */
    public static Timing getEntityTimings(Entity entity) {
        String entityType = entity.getClass().getName();
        return Timings.ofSafe("Minecraft", "## tickEntity - " + entityType, tickEntityTimer);
    }

    /**
     * Get a named timer for the specified tile entity type to track type specific timings.
     */
    public static Timing getTileEntityTimings(TileEntity entity) {
        String entityType = entity.getClass().getName();
        return Timings.ofSafe("Minecraft", "## tickTileEntity - " + entityType, tickTileEntityTimer);
    }
    public static Timing getCancelTasksTimer() {
        return Timings.ofSafe("Cancel Tasks");
    }
    public static Timing getCancelTasksTimer(Plugin plugin) {
        return Timings.ofSafe(plugin, "Cancel Tasks");
    }

    public static void stopServer() {
        TimingsManager.stopServer();
    }

    public static Timing getBlockTiming(Block block) {
        return Timings.ofSafe("## Scheduled Block: " + block.getName());
    }
}
