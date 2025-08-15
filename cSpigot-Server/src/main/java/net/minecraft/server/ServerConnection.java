package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.cspigot.config.SettingsConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

public class ServerConnection {

    private static final boolean disableFlushConsolidation = Boolean.getBoolean("Paper.disableFlushConsolidate"); // PandaSpigot
    private static final Logger LOGGER = LogManager.getLogger();
    private static final WriteBufferWaterMark SERVER_WRITE_MARK = new WriteBufferWaterMark(1 << 20, 1 << 21);

    private final EventGroupType eventGroupType;
    public static LazyInitVar<EventLoopGroup> a/*, b*/; // a = boss, b = worker
    public static final LazyInitVar<DefaultEventLoopGroup> c = new LazyInitVar<>(() -> new DefaultEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Server IO #%d").setDaemon(true).build()));

    private final MinecraftServer server;
    public volatile boolean started;

    private final List<ChannelFuture> g = Collections.synchronizedList(Lists.newArrayList());
    private final List<NetworkManager> h = Collections.synchronizedList(Lists.newArrayList());

    public ServerConnection(MinecraftServer minecraftserver) {
        this.server = minecraftserver;
        this.started = true;

        if (server.ai()) {
            /*
           // cSpigot start - Added support for IOUring
            if (SettingsConfig.SUPPORT_IO_URING && IOUring.isAvailable()) {
                this.eventGroupType = EventGroupType.URING;
                return;
            // cSpigot end
            } else*/

            if (Epoll.isAvailable()) {
                this.eventGroupType = EventGroupType.EPOLL;
                return;
            } else if (KQueue.isAvailable()) {
                this.eventGroupType = EventGroupType.KQUEUE;
                return;
            }
        }

        this.eventGroupType = server.getTransport();
    }

    // Paper start - prevent blocking on adding a new network manager while the
    // server is ticking
    public final java.util.Queue<NetworkManager> pending = SettingsConfig.QUEUE_NETWORK_MANAGERS ? new java.util.concurrent.ConcurrentLinkedQueue<>() : null;

    private void addPending() {
        if (!SettingsConfig.QUEUE_NETWORK_MANAGERS) return;

        NetworkManager manager;
        while ((manager = pending.poll()) != null) {
            this.getConnectedChannels().add(manager);
        }
    }
    // Paper end

    public void a(InetAddress ip, int port) throws IOException {
        synchronized (this.getListeningChannels()) {
            Class<? extends ServerChannel> channel = null;
            // cSpigot Start: Customisable netty threads
            final int workerThreadCount = SettingsConfig.NETTY_WORKER_THREADS <= 0
                    ? Runtime.getRuntime().availableProcessors()
                    : SettingsConfig.NETTY_WORKER_THREADS;

            switch (eventGroupType) {
                default:
                case DEFAULT: {
                    LOGGER.info("Finding best event group type using fall-through");
                }

                case EPOLL: {
                    if (Epoll.isAvailable()) {
                        a = new LazyInitVar<>(() -> new EpollEventLoopGroup(SettingsConfig.NETTY_CONTROLLER_THREADS));
                        //b = new LazyInitVar<>(() -> new EpollEventLoopGroup(workerThreadCount));

                        channel = EpollServerSocketChannel.class;

                        LOGGER.info("Using epoll");

                        break;
                    }
                }

//                // cSpigot start - Added support for IOUring
//                case URING: {
//                    if (SettingsConfig.SUPPORT_IO_URING && IOUring.isAvailable()) {
//                        a = new LazyInitVar<>(() -> new IOUringEventLoopGroup(SettingsConfig.NETTY_CONTROLLER_THREADS));
//                        b = new LazyInitVar<>(() -> new IOUringEventLoopGroup(workerThreadCount));
//
//                        channel = io.netty.incubator.channel.uring.IOUringServerSocketChannel.class;
//
//                        LOGGER.info("Using IO-Uring");
//
//                        break;
//                    }
//                }
//                // cSpigot end
                case KQUEUE: {
                    if (KQueue.isAvailable()) {
                        a = new LazyInitVar<>(() -> new KQueueEventLoopGroup(SettingsConfig.NETTY_CONTROLLER_THREADS));
                        //b = new LazyInitVar<>(() -> new KQueueEventLoopGroup(workerThreadCount));

                        channel = KQueueServerSocketChannel.class;

                        LOGGER.info("Using kqueue");

                        break;
                    }
                }
                case NIO: {
                    a = new LazyInitVar<>(() -> new NioEventLoopGroup(SettingsConfig.NETTY_CONTROLLER_THREADS));
                    //b = new LazyInitVar<>(() -> new NioEventLoopGroup(workerThreadCount));

                    channel = NioServerSocketChannel.class;

                    LOGGER.info("Using NIO");

                    break;
                }
            }

            // cSpigot End: Customisable netty threads

            this.g.add((new ServerBootstrap())
                    .channel(channel)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, SERVER_WRITE_MARK)
                    .childHandler(new ChannelInitializer() {
                        protected void initChannel(Channel channel) throws Exception {
                            if (channel == null) return;

                            try {
                                ChannelConfig config = channel.config();
                                config.setOption(ChannelOption.TCP_NODELAY, true);
                                config.setOption(ChannelOption.TCP_FASTOPEN, 1);
                                config.setOption(ChannelOption.TCP_FASTOPEN_CONNECT, true);
                                config.setOption(ChannelOption.IP_TOS, 0x18);
                                config.setAllocator(ByteBufAllocator.DEFAULT);
                            } catch (ChannelException e) {
                                e.printStackTrace();
                            }

                            if (!disableFlushConsolidation) channel.pipeline().addFirst(new io.netty.handler.flush.FlushConsolidationHandler()); // Paper

                            channel.pipeline()
                                    .addLast("timeout", new ReadTimeoutHandler(SettingsConfig.READ_TIMEOUT_KICK))
                                    .addLast("legacy_query", new LegacyPingHandler(ServerConnection.this))
                                    .addLast("splitter", new PacketSplitter())
                                    .addLast("decoder", new PacketDecoder(EnumProtocolDirection.SERVERBOUND))
                                    .addLast("prepender", PacketPrepender.INSTANCE)
                                    .addLast("encoder", new PacketEncoder(EnumProtocolDirection.CLIENTBOUND));

                            NetworkManager networkmanager = new NetworkManager(EnumProtocolDirection.SERVERBOUND);
                            if (SettingsConfig.QUEUE_NETWORK_MANAGERS) {
                                pending.add(networkmanager);
                            } else {
                                h.add(networkmanager);
                            }

                            channel.pipeline().addLast("packet_handler", networkmanager);
                            networkmanager.a(new HandshakeListener(server, networkmanager));
                            io.papermc.paper.network.ChannelInitializeListenerHolder.callListeners(channel); // Paper's injection API
                        }
                    }).group(a.get())
                    .localAddress(ip, port)
                    .bind().syncUninterruptibly());
        }
    }

    public void stopServer() throws InterruptedException {
        this.started = false;
        LOGGER.info("Shutting down event loops");
        for (ChannelFuture channelfuture : this.g) {
            try {
                channelfuture.channel().close().sync();
            } finally {
                a.get().shutdownGracefully();
                //b.get().shutdownGracefully();
                c.get().shutdownGracefully();
            }
        }

    }

    public void b() {
        this.shutdown();
    }

    public void shutdown() {
        try {
            this.stopServer();
        } catch (InterruptedException e) {
            LOGGER.info("Error occurred while flushing and closing netty channels!");
        }
    }

    // PAIL: processPackets
    public void c() {
        synchronized (this.h) {
            // Spigot Start
            if (SettingsConfig.QUEUE_NETWORK_MANAGERS) {
                this.addPending(); // Paper
            }
            // This prevents players from 'gaming' the server, and strategically relogging to increase their position in the tick order
            if (org.spigotmc.SpigotConfig.playerShuffle > 0 && MinecraftServer.currentTick % org.spigotmc.SpigotConfig.playerShuffle == 0) {
                Collections.shuffle(this.h);
            }
            // Spigot End
            Iterator iterator = this.h.iterator();

            while (iterator.hasNext()) {
                final NetworkManager networkmanager = (NetworkManager) iterator.next();

                if (!networkmanager.h()) {
                    if (!networkmanager.g()) {
                        // Spigot Start
                        // Fix a race condition where a NetworkManager could be unregistered just before connection.
                        if (networkmanager.preparing) continue;
                        // Spigot End
                        iterator.remove();
                        networkmanager.l();
                    } else {
                        try {
                            networkmanager.a();
                        } catch (Exception exception) {
                            if (networkmanager.c()) {
                                CrashReport crashreport = CrashReport.a(exception, "Ticking memory connection");
                                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Ticking connection");

                                crashreportsystemdetails.a("Connection", new Callable() {
                                    public String a() throws Exception {
                                        return networkmanager.toString();
                                    }

                                    public Object call() throws Exception {
                                        return this.a();
                                    }
                                });
                                throw new ReportedException(crashreport);
                            }

                            ServerConnection.LOGGER.warn("Failed to handle packet for " + networkmanager.getSocketAddress(), exception);
                            ChatComponentText chatcomponenttext = new ChatComponentText("Internal server error");

                            networkmanager.handleDetailedPacket(new PacketPlayOutKickDisconnect(chatcomponenttext), future -> networkmanager.close(chatcomponenttext));
                            networkmanager.k();
                        }
                    }
                }
            }

        }
    }

    public enum EventGroupType {
        EPOLL, KQUEUE, NIO, DEFAULT
    }

    public MinecraftServer d() {
        return this.server;
    }

    public List<ChannelFuture> getListeningChannels() {
        return this.g;
    }

    public List<NetworkManager> getConnectedChannels() {
        return this.h;
    }

    public List<NetworkManager> getNetworkManagers() { //OBF Helpers
        return this.h;
    }

    public List<ChannelFuture> getListeners() { //OBF Helpers
        return this.g;
    }
}
