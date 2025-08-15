package net.minecraft.server;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.*;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker PACKET_MARKER = MarkerManager.getMarker("PACKET_SENT", NetworkManager.PACKET_MARKER);
	private final EnumProtocolDirection c;

	public PacketEncoder(EnumProtocolDirection enumprotocoldirection) {
		this.c = enumprotocoldirection;
	}

	protected void a(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf bytebuf) throws Exception {
		Integer packetId = ctx.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get().a(c, packet);

		/*
		 * if (PacketEncoder.a.isDebugEnabled()) {
		 * PacketEncoder.a.debug(PacketEncoder.b, "OUT: [{}:{}] {}",
		 * ctx.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get(), packetId,
		 * packet.getClass().getName()); }
		 */

		if (packetId == null) {
			throw new IOException("Can't serialize unregistered packet");
		} else {
			PacketDataSerializer serializer = new PacketDataSerializer(bytebuf);
			serializer.b(packetId);

			try {
				packet.b(serializer);
			} catch (IOException ex) {
				LOGGER.log(Level.ERROR, PACKET_MARKER, "Exploit exception: " + ctx.channel().attr(NetworkManager.ATTRIBUTE_PROTOCOL).get());
			}

			// Paper start
			int packetLength = bytebuf.readableBytes();
			if (packetLength > MAX_PACKET_SIZE) {
				throw new PacketTooLargeException(packet, packetLength);
			}
			// Paper end
		}
	}

	// Paper start
	private static int MAX_PACKET_SIZE = 2097152;
	public static class PacketTooLargeException extends RuntimeException {
		private final Packet<?> packet;
		PacketTooLargeException(Packet<?> packet, int packetLength) {
			super("PacketTooLarge - " + packet.getClass().getSimpleName() + " is " + packetLength +". Max is " + MAX_PACKET_SIZE);
			this.packet = packet;
		}

		public Packet<?> getPacket() {
			return packet;
		}
	}
	// Paper end

	@Override
	protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf)
			throws Exception {
		this.a(channelHandlerContext, packet, byteBuf);
	}
}
