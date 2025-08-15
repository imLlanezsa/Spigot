package net.minecraft.server;

import java.io.IOException;

public interface Packet<T extends PacketListener> {

	void a(PacketDataSerializer var1) throws IOException;

	void b(PacketDataSerializer var1) throws IOException;

	// Paper start
	default boolean packetTooLarge(NetworkManager manager) {
		return false;
	}
	// Paper end

	void a(T var1);
}
