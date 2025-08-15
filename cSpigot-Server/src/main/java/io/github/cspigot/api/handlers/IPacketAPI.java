package io.github.cspigot.api.handlers;

import org.bukkit.plugin.java.JavaPlugin;
import io.github.cspigot.api.handlers.impl.MovementHandler;
import io.github.cspigot.api.handlers.impl.PacketHandler;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/10/2022
 * @version cSpigot
 */

public interface IPacketAPI {

    /**
     * Register a {@link PacketHandler} to cSpigot
     *
     * @param packetHandler {@link PacketHandler instance}
     */
    void registerPacketHandler(JavaPlugin plugin, PacketHandler packetHandler);

    /**
     * Register a {@link MovementHandler} to cSpigot
     *
     * @param movementHandler {@link MovementHandler instance}
     */
    void registerMovementHandler(JavaPlugin plugin, MovementHandler movementHandler);
}
