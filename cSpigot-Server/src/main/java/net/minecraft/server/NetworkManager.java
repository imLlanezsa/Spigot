package net.minecraft.server;

import ac.artemis.anticheat.sdk.FlushAPI;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.Natives;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import io.github.cspigot.cSpigot;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.api.handlers.impl.PacketHandler;
import io.github.cspigot.utils.netty.VarIntUtil;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetworkManager extends SimpleChannelInboundHandler<Packet<?>> {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Marker ROOT_MARKER = MarkerManager.getMarker("NETWORK");
    public static final Marker PACKET_MARKER = MarkerManager.getMarker("NETWORK_PACKETS", ROOT_MARKER);

    public static final AttributeKey<EnumProtocol> ATTRIBUTE_PROTOCOL = AttributeKey.valueOf("protocol");
    public static final AttributeKey<EnumProtocol> c = ATTRIBUTE_PROTOCOL;

    public static final LazyInitVar<NioEventLoopGroup> d =  new LazyInitVar<>(() -> new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build()));
    public static final LazyInitVar<EpollEventLoopGroup> e = new LazyInitVar<>(() -> new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build()));
    public static final LazyInitVar<DefaultEventLoopGroup> f = new LazyInitVar<>(() -> new DefaultEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build()));

    private final EnumProtocolDirection h;
    private Queue<QueuedPacket> i;
    private ReentrantReadWriteLock j; // Paper - Remove packet queue locking
    public Channel channel;
    
    // Spigot Start // PAIL
    public SocketAddress l;
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;
    public boolean preparing = true;
    // Spigot End
    
    private PacketListener m; // PacketListener
    private IChatBaseComponent n;
    private boolean o;  // Is connected?
    private boolean p; // Disconnection handled?

    // Paper start - NetworkClient implementation
    private static boolean enableExplicitFlush = Boolean.getBoolean("paper.explicit-flush"); // Paper - Disable explicit flushing
    public boolean isPending = true;
    public EnumProtocol protocol;
    public int protocolVersion;
    public java.net.InetSocketAddress virtualHost;
    // Paper end

    public NetworkManager(EnumProtocolDirection enumprotocoldirection) {
        this.h = enumprotocoldirection;
        this.i = Queues.newConcurrentLinkedQueue();
        this.j = new ReentrantReadWriteLock();
    }

    public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
        super.channelActive(channelhandlercontext);
        this.channel = channelhandlercontext.channel();
        this.l = this.channel.remoteAddress();
        // Spigot Start
        this.preparing = false;
        // Spigot End

        try {
            this.setProtocol(EnumProtocol.HANDSHAKING);
        } catch (Throwable throwable) {
            NetworkManager.LOGGER.fatal(throwable);
        }
    }

    // Tuinity start - allow controlled flushing
    volatile boolean canFlush = true;
    private int flushPacketsStart;

    private final Object flushLock = new Object();
    private final AtomicInteger packetWrites = new AtomicInteger();

    public void disableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.flushPacketsStart = this.packetWrites.get(); // must be volatile and before canFlush = false
            this.canFlush = false;
        }
    }

    public void enableAutomaticFlush() {
        synchronized (this.flushLock) {
            this.canFlush = true;
            if (this.packetWrites.get() != this.flushPacketsStart) { // must be after canFlush = true
                this.flush(); // only make the flush call if we need to
            }
        }
    }

    private void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        } else {
            this.channel.eventLoop().execute(() -> {
                this.channel.flush();
            });
        }
    }
    // Tuinity end - allow controlled flushing

    public void setProtocol(EnumProtocol enumProtocol) {
        this.protocol = enumProtocol; // cSpigot
        this.channel.attr(NetworkManager.ATTRIBUTE_PROTOCOL).set(enumProtocol);
        this.channel.config().setAutoRead(true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelhandlercontext) {
        this.close(new ChatMessage("disconnect.endOfStream"));
    }

    public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) {
        // Paper start
        if (throwable instanceof io.netty.handler.codec.EncoderException && throwable.getCause() instanceof PacketEncoder.PacketTooLargeException) {
            if (((PacketEncoder.PacketTooLargeException) throwable.getCause()).getPacket().packetTooLarge(this)) {
                return;
            } else {
                throwable = throwable.getCause();
            }
        }
        // Paper end
        ChatMessage chatmessage;

        if (throwable instanceof TimeoutException) {
            chatmessage = new ChatMessage("disconnect.timeout");
        } else {
            chatmessage = new ChatMessage("disconnect.genericReason", "Internal Exception: " + throwable);
            if (SettingsConfig.SYSTEM_DEBUG && !(throwable instanceof ClosedChannelException)) {
                throwable.printStackTrace();
            }
        }

        this.close(chatmessage);
    }

    protected void a(ChannelHandlerContext channelhandlercontext, Packet packet) throws Exception {
        if (this.channel.isOpen()) {
            // cSpigot start - PPS Filter (Exploit related)
            if (SettingsConfig.PACKET_THROTTLE_ENABLED && (this.m instanceof PlayerConnection)) {
                if (packet instanceof PacketPlayInFlying || packet instanceof PacketPlayInArmAnimation) {
                    // this can otherwise be false by lag spikes
                    if (((PlayerConnection) this.m).player.packetThrottle.addAndGet(0.5) > SettingsConfig.PACKET_THROTTLE_VALUE) {
                        LOGGER.fatal("[cSpigot] Player {} ({}) sent too many packets: {}",
                                ((PlayerConnection) this.m).player.getName(),
                                ((PlayerConnection) this.m).player.getUniqueID(),
                                ((PlayerConnection) this.m).player.packetThrottle.get());

                        // kick on the next tick
                        MinecraftServer.getServer().processQueue.add(() ->
                                ((PlayerConnection) this.m).disconnect(SettingsConfig.PPS_KICK_MESSAGE));

                        return;
                    } else if (packet instanceof PacketPlayInFlying && ((PacketPlayInFlying) packet).hasPos) {
                        final EntityPlayer player = ((PlayerConnection) this.m).player;
                        // Prevent movement exploits
                        if (player.positionThrottle.addAndGet(1) >
                                (SettingsConfig.PACKET_THROTTLE_VALUE / 2d)) {
                            LOGGER.fatal("[cSpigot] Player {} ({}) sent too many movements at once: {}",
                                    player.getName(), player.getUniqueID(), player.packetThrottle.get());

                            MinecraftServer.getServer().processQueue.add(() ->
                                    ((PlayerConnection) this.m).disconnect(SettingsConfig.PPS_KICK_MESSAGE));

                            return;
                        }
                    }
                } else {
                    // Normal
                    final EntityPlayer player = ((PlayerConnection) this.m).player;
                    if (player.packetThrottle.addAndGet(1.0) > SettingsConfig.PACKET_THROTTLE_VALUE) {
                        LOGGER.fatal("[cSpigot] Player {} ({}) sent too many packets: {}", player.getName(), player.getUniqueID(), player.packetThrottle.get());
                        MinecraftServer.getServer().processQueue.add(() ->
                                ((PlayerConnection) this.m).disconnect(SettingsConfig.PPS_KICK_MESSAGE));
                        return;
                    }
                }
            }

            // cSpigot - Add PacketAPI for received packets
            boolean cancelled = false;
            if (this.m instanceof PlayerConnection) {
                try {
                    for ( PacketHandler handler : cSpigot.getInstance().getPacketHandlers()) {
                        cancelled = !handler.handleReceivedPacket((PlayerConnection) this.m, packet);
                    }
                } catch (Exception e) {
                    LOGGER.info("[cSpigot] A PacketHandler failed to successfully handle received packet!");
                    LOGGER.info("[cSpigot] Error: " + e.getMessage());
                    LOGGER.error(e);
                }
            }

            if (cancelled) return;

            // cSpigot end

            try {
                packet.a(this.m);
            } catch (CancelledPacketHandleException e) {
                ;
            }
        }
    }

    public void handle(Packet<?> packet) {
        this.handleDetailedPacket(packet, null, (GenericFutureListener<? extends Future<? super Void>>) null); // PandaSpigot
    }

    @SafeVarargs
    public final void handleDetailedPacket(Packet<?>  packet, GenericFutureListener<? extends Future<? super Void>> genericFutureListener, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
        GenericFutureListener<? extends Future<? super Void>>[] listeners = null;
        if (genericFutureListener != null || agenericfuturelistener != null) {
            listeners = ArrayUtils.add(agenericfuturelistener, 0, genericFutureListener);
        }

        if (this.isConnected()) {
            this.sendPacketQueue();
            this.dispatchPacket(packet, listeners, Boolean.TRUE);
        } else {
            this.j.writeLock().lock();
            try {
                this.i.add(new NetworkManager.QueuedPacket(packet, listeners));
            } finally {
                this.j.writeLock().unlock();
            }
        }
    }

    // cSpigot start - Add flush parameter
    public void dispatchPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>>[] listeners, Boolean flushConditional) {
        this.packetWrites.getAndIncrement(); // must be before using canFlush

        boolean effectiveFlush = flushConditional == null ? this.canFlush : flushConditional;
        final boolean flush = effectiveFlush || VarIntUtil.canSendImmediate(this, packet); // no delay for certain packets

        final EnumProtocol enumprotocol = EnumProtocol.a(packet);
        final EnumProtocol enumprotocol1 = this.channel.attr(NetworkManager.ATTRIBUTE_PROTOCOL).get();

        if (enumprotocol1 != enumprotocol) {
            this.channel.config().setAutoRead(false);
        }

        if (this.channel.eventLoop().inEventLoop()) {
            if (enumprotocol != enumprotocol1) {
                this.setProtocol(enumprotocol);
            }
            ChannelFuture channelfuture = flush ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
            if (listeners != null) {
                channelfuture.addListeners(listeners);
            }
            channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            // Tuinity start - optimise packets that are not flushed
            Runnable choice1 = null;
            AbstractEventExecutor.LazyRunnable choice2 = null;
            // note: since the type is not dynamic here, we need to actually copy the old
            // executor code
            // into two branches. On conflict, just re-copy - no changes were made inside
            // the executor code.
            if (flush) {
                choice1 = () -> {
                    if (enumprotocol != enumprotocol1) {
                        this.setProtocol(enumprotocol);
                    }
                    try {
                        ChannelFuture channelfuture1 = this.channel.writeAndFlush(packet);
                        if (listeners != null) {
                            channelfuture1.addListeners(listeners);
                        }
                        channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    } catch (Exception e) {
                        LOGGER.error("NetworkException: " + getPlayer(), e);
                        close(new ChatMessage("disconnect.genericReason", "Internal Exception: " + e.getMessage()));
                        ;
                    }
                };
            } else {
                // explicitly declare a variable to make the lambda use the type
                choice2 = () -> {
                    if (enumprotocol != enumprotocol1) {
                        this.setProtocol(enumprotocol);
                    }
                    try {
                        ChannelFuture channelfuture1 = this.channel.write(packet);
                        if (listeners != null) {
                            channelfuture1.addListeners(listeners);
                        }
                        channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    } catch (Exception e) {
                        LOGGER.error("NetworkException: " + getPlayer(), e);
                        close(new ChatMessage("disconnect.genericReason", "Internal Exception: " + e.getMessage()));
                    }
                };
            }
            this.channel.eventLoop().execute(choice1 != null ? choice1 : choice2);
            // Tuinity end - optimise packets that are not flushed
        }
    }

    private void sendPacketQueue() {
        if (this.i.isEmpty() ) {
            return; // Avoid lock every packet send
        }

        if (this.isConnected()) {
            boolean needsFlush = this.canFlush;
            boolean hasWrotePacket = false;
            final boolean flushApi = FlushAPI.getApi() != null && this.m instanceof PlayerConnection && this.getPlayer() != null;

            if (flushApi) {
                FlushAPI.getApi().callPre(this.getPlayer().getUniqueID());
            }

            this.j.readLock().lock();
            try {
                Iterator<QueuedPacket> iterator = this.i.iterator();
                while (iterator.hasNext()) {
                    QueuedPacket queued = iterator.next();
                    Packet<?> packet = queued.a;

                    if (hasWrotePacket && (needsFlush || this.canFlush)) {
                        flush();
                    }

                    iterator.remove();
                    this.dispatchPacket(packet, queued.b, (!iterator.hasNext() && (needsFlush || this.canFlush)) ? Boolean.TRUE : Boolean.FALSE);
                    hasWrotePacket = true;
                }
            } finally {
                this.j.readLock().unlock();
            }

            if (flushApi) {
                FlushAPI.getApi().callPost(this.getPlayer().getUniqueID());
            }
        }
    }
    // cSpigot end

    public void tick() {
        this.sendPacketQueue();
        if (this.m instanceof IUpdatePlayerListBox) {
            ((IUpdatePlayerListBox) this.m).c();
        }

        if (enableExplicitFlush) {
            flush();
        }
    }

    public void close(IChatBaseComponent ichatbasecomponent) {
        this.i.clear(); // FlamePaper - Minetick fix memory leaks
        // Spigot Start
        this.preparing = false;
        // Spigot End
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close(); // We can't wait as this may be called from an event loop.
            this.n = ichatbasecomponent;
        }

    }

    // Paper start
    public void setupEncryptionKey(javax.crypto.SecretKey secretkey) {
        if (!this.o) {
            try {
                com.velocitypowered.natives.encryption.VelocityCipher decryption = Natives.cipher.get().forDecryption(secretkey);
                com.velocitypowered.natives.encryption.VelocityCipher encryption = Natives.cipher.get().forEncryption(secretkey);

                this.o = true;
                this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecrypter(decryption));
                this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncrypter(encryption));
            } catch (java.security.GeneralSecurityException e) {
                throw new SecurityException(e);
            }
        }
    }
    // Paper end

    public void setupCompression(int compressionThreshold) {
        if (compressionThreshold >= 0) {
            VelocityCompressor compressor = Natives.compress.get().create(-1); // Paper
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                ((PacketDecompressor) this.channel.pipeline().get("decompress")).a(compressionThreshold);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new PacketDecompressor(compressor, compressionThreshold)); // Paper
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                ((PacketCompressor) this.channel.pipeline().get("decompress")).a(compressionThreshold);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new PacketCompressor(compressor, compressionThreshold)); // Paper
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof PacketDecompressor) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof PacketCompressor) {
                this.channel.pipeline().remove("compress");
            }
        }
    }

    public void handleDisconnection() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (!this.p) {
                this.p = true;
                if (this.j() != null) {
                    this.getPacketListener().a(this.j());
                } else if (this.getPacketListener() != null) {
                    this.getPacketListener().a(new ChatComponentText("Disconnected"));
                }
                this.i.clear(); // Free up packet queue.
            } else {
                // Send and close the channel, this error usually means that someone may be
                // hacking or have more than 20ms of ping (mc netcode shit)
                // This is a partial backport from 1.9+
                flush();
                close(null);
            }
        }
    }

    public void setPacketListener(PacketListener packetListener) {
        Validate.notNull(packetListener, "packetListener");
        this.m = packetListener;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    public PacketListener getPacketListener() {
        return this.m;
    }

    public EntityPlayer getPlayer() {
        if (getPacketListener() instanceof PlayerConnection) {
            return ((PlayerConnection) getPacketListener()).player;
        } else {
            return null;
        }
    }

    public SocketAddress getSocketAddress() {
        return this.l;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet<?>  object) {
        // Netty just ignores the error and we do not want that
        try {
            if (this.isConnected())
                this.a(channelhandlercontext, object);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static class QueuedPacket {

        private final Packet a; private final Packet<?> getPacket() { return this.a; } // PandaSpigot - OBFHELPER
        private final GenericFutureListener<? extends Future<? super Void>>[] b;

        @SafeVarargs
        public QueuedPacket(Packet<?>  packet, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
            this.a = packet;
            this.b = agenericfuturelistener;
        }
    }

    // Spigot Start
    public SocketAddress getRawAddress()
    {
        return this.channel.remoteAddress();
    }
    // Spigot End


    // cSpigot start  - Obfuscation Helpers
    private void a(Packet<?>  packet, final GenericFutureListener<? extends Future<? super Void>>[] agenericfuturelistener) {
        this.dispatchPacket(packet, agenericfuturelistener, Boolean.TRUE);
    }

    public boolean c() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public void a(Packet<?>  packet, GenericFutureListener<? extends Future<? super Void>> genericFutureListener, GenericFutureListener<? extends Future<? super Void>>... agenericfuturelistener) {
        this.handleDetailedPacket(packet, genericFutureListener, agenericfuturelistener);
    }

    public void a(EnumProtocol enumprotocol) {
        this.setProtocol(enumprotocol);
    }

    public void a(PacketListener packetlistener) {
        this.setPacketListener(packetlistener);
    }

    private void m() {
        this.sendPacketQueue();
    }

    public void a() {
        this.tick();
    }

    public void a(javax.crypto.SecretKey secretkey) {
        this.setupEncryptionKey(secretkey);
    }

    public boolean g() {
        return this.isConnected();
    }

    public boolean h() {
        return this.channel == null;
    }

    public IChatBaseComponent j() {
        return this.n;
    }

    public void k() {
        this.channel.config().setAutoRead(false);
    }

    public void a(int i) {
        this.setupCompression(i);
    }

    public void l() {
        this.handleDisconnection();
    }

    // cSpigot end
}
