package io.github.cspigot.interceptor;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;

public interface PacketInterceptor {
    default void onPacketIn(PlayerConnection connection, Packet<?> packet) {
    }

    default boolean onPacketInCancellable(PlayerConnection connection, Packet<?> packet) {
        return false;
    }

    default void onPacketOut(PlayerConnection connection, Packet<?> packet) {
    }
}
