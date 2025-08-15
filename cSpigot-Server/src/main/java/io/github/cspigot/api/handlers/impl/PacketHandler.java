package io.github.cspigot.api.handlers.impl;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;

public interface PacketHandler {

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
    boolean handleReceivedPacket(PlayerConnection connection, Packet<?> packet);

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
    boolean handleSentPacket(PlayerConnection connection, Packet<?> packet);
}
