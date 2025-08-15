package io.github.cspigot.feature.cps;

import net.minecraft.server.*;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.github.cspigot.api.handlers.impl.PacketHandler;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.plugin.ServerSidePlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2023, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 11/17/2023
 */
public class CPSTracker implements PacketHandler, Listener {


    // This is concurrent to make sure that scoreboard won't throw a CME
    private static final Map<UUID, Integer> cpsCount = new ConcurrentHashMap<>();
    private static final Set<UUID> mining = new HashSet<>();
    private static final Set<UUID> cancelNextSwing = new HashSet<>();

    /**
     * Handles a received {@link Packet} for the given
     * player connection
     * </p>
     * This method is way better and causes less lag than other
     * packet handlers
     *
     * @param connection {@link PlayerConnection} the player's connection
     * @param packet     {@link Packet} the packet being received
     */
    public boolean handleReceivedPacket(PlayerConnection connection, Packet<?> packet) {
        if (packet instanceof PacketPlayInArmAnimation) {
            Player player = connection.getPlayer();
            UUID uuid = player.getUniqueId();

            int cps = getCPS(player);

            if (SettingsConfig.DEBUG_MODE) {
                player.sendActionBar("§7CPS: §f" + cps);
            }

            if (SettingsConfig.LIMIT_CPS && cps >= SettingsConfig.CPS_LIMIT_VALUE) {
                return false;
            }

            if (mining.contains(uuid)) {
                return true;
            }
            if (cancelNextSwing.contains(uuid)) {
                cancelNextSwing.remove(uuid);
                return true;
            }

            cpsCount.put(uuid, cpsCount.get(uuid) + 1);
            Bukkit.getScheduler().runTaskLater(ServerSidePlugin.getInstance(), () -> {
                if (cpsCount.containsKey(uuid)) {
                    cpsCount.put(uuid, Math.max(0, cpsCount.get(uuid) - 1));
                }
            }, 20L);
        }
        if (packet instanceof PacketPlayInBlockDig) {
            UUID uuid = connection.getPlayer().getUniqueId();
            PacketPlayInBlockDig digPacket = ((PacketPlayInBlockDig) packet);
            BlockPosition blockPosition = digPacket.a();
            PacketPlayInBlockDig.EnumPlayerDigType digType = digPacket.c();
            Bukkit.getScheduler().runTask(ServerSidePlugin.getInstance(), () -> {
                switch (digType) {
                    case START_DESTROY_BLOCK:
                        // Checks for hardness -> the abort/stop won't be sent to the player when it's a block like tnt
                        float hardness = CraftMagicNumbers.getBlock(connection.getPlayer().getWorld().getBlockAt(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ())).g(null, null);
                        if (hardness > 0.0F) {
                            mining.add(uuid);
                        } else {
                            cancelNextSwing.add(uuid);
                            Bukkit.getScheduler().runTaskLater(ServerSidePlugin.getInstance(), () -> {
                                cancelNextSwing.remove(uuid);
                            }, 1L);
                        }
                        break;
                    case ABORT_DESTROY_BLOCK:
                        mining.remove(uuid);
                        break;
                    case STOP_DESTROY_BLOCK:
                        // Some delay remove to prevent swing packets from the time between the end of the mining and the start of mining for the same block
                        Bukkit.getScheduler().runTaskLater(ServerSidePlugin.getInstance(), () -> {
                            mining.remove(uuid);
                        }, 6L); // 6 ticks some magic value which probs isn't that good for laggy connects
                        break;
                }
            });
        }
        return true;
    }

    /**
     * Handles a {@link Packet} being sent for the given
     * player connection
     * </p>
     * This method is way better and causes less lag than other
     * packet handlers
     *
     * @param connection {@link PlayerConnection} the player's connection
     * @param packet     {@link Packet} the packet being sent
     */
    public boolean handleSentPacket(PlayerConnection connection, Packet<?> packet) {
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        cpsCount.putIfAbsent(event.getPlayer().getUniqueId(), 0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cpsCount.remove(event.getPlayer().getUniqueId());
    }

    public static int getCPS(Player player) {
        return cpsCount.get(player.getUniqueId());
    }
}
