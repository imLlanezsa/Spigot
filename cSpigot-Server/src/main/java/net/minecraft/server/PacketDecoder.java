package net.minecraft.server;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.log4j.Log4j2;
import io.github.cspigot.utils.prot.CrashException;

@Log4j2
public class PacketDecoder extends ByteToMessageDecoder {

	private final EnumProtocolDirection c;

	public PacketDecoder(EnumProtocolDirection var1) {
		this.c = var1;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (!in.isReadable()) return;

		PacketDataSerializer packetDataHelper = new PacketDataSerializer(in);
		int packetId = packetDataHelper.readVarInt();

		Packet<?> packet = ctx.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().a(this.c, packetId);
		if (packet == null) {
			throw new IOException("Bad packet id " + packetId);
		}

		try {
			packet.a(packetDataHelper);
		} catch (CrashException e) {
			log.fatal("[cSpigot] Detected crash exploit in packet " + packet.getClass().getSimpleName() + " with id " + packetId, e);
			log.fatal("[cSpigot] Mitigating exploit by disconnecting player...");
			throw e;
		}
//
//		// cSpigot start - Fix disconnect on latest clients
//		// We currently let them bypass regardless because it seems like a ViaVersion issue
//		boolean latest_client =
//				packetId == 75
//				&& packet.getClass().getSimpleName().equalsIgnoreCase("wd")
//				&& packetDataHelper.readableBytes() == 51;

		if (packetDataHelper.readableBytes() > 0) {
			// cSpigot end
			throw new IOException("Packet " + ctx.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().getClass()
					+ "/" + packetId + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found "
					+ packetDataHelper.readableBytes() + " bytes extra whilst reading packet " + packetId);
		} else {
			out.add(packet);
		}
	}
}