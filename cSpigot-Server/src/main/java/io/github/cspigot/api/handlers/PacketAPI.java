package io.github.cspigot.api.handlers;

import io.github.cspigot.cSpigot;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

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

@Log4j2
public class PacketAPI implements IPacketAPI {

    /**
     * Static instance that ppl can use for their plugins
     */
    @Getter
    private static PacketAPI instance;
    /**
     * cSpigot instance to get knockbackHandler
     */
    private final cSpigot spigot;

    public PacketAPI(cSpigot spigot) {
        PacketAPI.instance = this;
        this.spigot = spigot;
    }

    /**
     * Register a {@link PacketHandler} to cSpigot
     *
     * @param packetHandler {@link PacketHandler instance}
     */
    public void registerPacketHandler(JavaPlugin javaPlugin, PacketHandler packetHandler) {
       log.info("[cSpigot] Registering a PacketHandler from " + javaPlugin.getName());
       this.spigot.registerPacketHandler(packetHandler);
    }

    /**
     * Remove a {@link PacketHandler} from cSpigot
     *
     * @param packetHandler {@link PacketHandler instance}
     */
    public void removePacketHandler(JavaPlugin javaPlugin, PacketHandler packetHandler) {
        log.info("[cSpigot] Removing a PacketHandler from " + javaPlugin.getName());
        this.spigot.removePacketHandler(packetHandler);
    }


    /**
     * Register a {@link MovementHandler} to cSpigot
     *
     * @param movementHandler {@link MovementHandler instance}
     */
    public void registerMovementHandler(JavaPlugin javaPlugin, MovementHandler movementHandler) {
        log.info("[cSpigot] Registering a MovementHandler from " + javaPlugin.getName());
        this.spigot.registerMovementHandler(movementHandler);
    }

    /**
     * Remove a {@link MovementHandler} from cSpigot
     *
     * @param movementHandler {@link MovementHandler instance}
     */
    public void removeMovementHandler(JavaPlugin javaPlugin, MovementHandler movementHandler) {
        log.info("[cSpigot] Registering a MovementHandler from " + javaPlugin.getName());
        this.spigot.removeMovementHandler(movementHandler);
    }
}
