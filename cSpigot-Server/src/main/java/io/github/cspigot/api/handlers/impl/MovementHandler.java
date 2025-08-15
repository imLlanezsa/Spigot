package io.github.cspigot.api.handlers.impl;

import net.minecraft.server.PacketPlayInFlying;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface MovementHandler {

    /**
     * Calls this method when a player's location is changed/moved
     *
     * @param player {@link Player} the player moving
     * @param to     {@link Location} the location to which the player is moving to
     * @param from   {@link Location} the player's previous location
     * @param packet {@link PacketPlayInFlying} the packet that is sent while the player moves
     */
    void handleUpdateLocation(Player player, Location to, Location from, PacketPlayInFlying packet);

    /**
     * Calls this method when a player's head is rotated
     *
     * @param player {@link Player} the player whose head is rotating
     * @param to     {@link Location} the location to which the player's head is moving to
     * @param from   {@link Location} the player head's previous location
     * @param packet {@link PacketPlayInFlying} the packet that is sent while the player's head moves
     */
    void handleUpdateRotation(Player player, Location to, Location from, PacketPlayInFlying packet);
}
