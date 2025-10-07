package io.github.cspigot.interceptor;

import net.minecraft.server.PacketPlayInFlying;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface MovementInterceptor {
    default void onLocationUpdate(Player player, Location to, Location from, PacketPlayInFlying packet) {
    }

    default void onRotationUpdate(Player player, Location to, Location from, PacketPlayInFlying packet) {
    }
}
