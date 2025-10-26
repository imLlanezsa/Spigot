package io.github.cspigot.interceptor;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;

public interface PacketInterceptor {
    default void onPacketIn(PlayerConnection connection, Packet packet) {
    }

    default void onPacketOut(PlayerConnection connection, Packet packet) {
    }
}
