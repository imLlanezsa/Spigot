package net.minecraft.server;

import java.io.IOException;

public interface Packet<T extends PacketListener> {

	void a(PacketDataSerializer var1) throws IOException;

	void b(PacketDataSerializer var1) throws IOException;

	default boolean packetTooLarge(NetworkManager manager) {
		return false;
	}

	void a(T var1);
}
