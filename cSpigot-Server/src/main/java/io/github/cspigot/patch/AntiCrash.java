package io.github.cspigot.patch;

import io.github.cspigot.interceptor.PacketInterceptor;
import net.minecraft.server.*;

public class AntiCrash implements PacketInterceptor {
    public AntiCrash() {
    }

    public void onPacketIn(PlayerConnection connection, Packet packet) {
        if (packet instanceof PacketPlayInCustomPayload) {
            PacketPlayInCustomPayload payload = (PacketPlayInCustomPayload)packet;
            PacketDataSerializer ab = payload.b();
            if (ab.refCnt() < 1) {
                connection.networkManager.close(new ChatMessage("Wrong ref count!", new Object[0]));
                return;
            }

            if (ab.readableBytes() > 25780) {
                connection.networkManager.close(new ChatMessage("Readable bytes exceeds limit!", new Object[0]));
            }
        }

    }
}
