package net.minecraft.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.github.cspigot.utils.netty.VarIntUtil;

/**
  * Optimize Packet Prepender
  * Don't create PacketDataSerializer and use byteBuf more
  * And use only 1 instance of all connections
  */
@ChannelHandler.Sharable
public class PacketPrepender extends MessageToByteEncoder<ByteBuf> {

	public static final PacketPrepender INSTANCE = new PacketPrepender(); // Paper

	private PacketPrepender() {} // Paper - private

	protected void a(ChannelHandlerContext channelhandlercontext, ByteBuf bytebuf, ByteBuf bytebuf1) throws Exception {
		// Paper start
		VarIntUtil.writeVarInt(bytebuf1, bytebuf.readableBytes());
		bytebuf1.writeBytes(bytebuf);
		// Paper end
	}

	protected void encode(ChannelHandlerContext channelhandlercontext, ByteBuf object, ByteBuf bytebuf) throws Exception {
		this.a(channelhandlercontext, (ByteBuf) object, bytebuf);
	}
	// Paper start
	@Override
	protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
		int anticipatedRequiredCapacity = VarIntUtil.varIntBytes(msg.readableBytes()) + msg.readableBytes();

		return ctx.alloc().directBuffer(anticipatedRequiredCapacity);
	}
	// Paper end
}