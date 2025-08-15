package net.minecraft.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;

public class PacketPlayInTabComplete implements Packet<PacketListenerPlayIn> {

    private String a;
    private BlockPosition b;

    public PacketPlayInTabComplete() {}

    public PacketPlayInTabComplete(String s) {
        this(s, (BlockPosition) null);
    }

    public PacketPlayInTabComplete(String s, BlockPosition blockposition) {
        this.a = s;
        this.b = blockposition;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.c(32767);
        boolean flag = packetdataserializer.readBoolean();

        if (flag) {
            this.b = packetdataserializer.c();
        }

    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.a(StringUtils.substring(this.a, 0, 32767));
        boolean flag = this.b != null;

        packetdataserializer.writeBoolean(flag);
        if (flag) {
            packetdataserializer.a(this.b);
        }

    }

    // cSpigot start - Async tab complete
    private static final ExecutorService TAB_COMPLETION_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("cSpigot TabComplete Thread - %d").build());
    public void a(PacketListenerPlayIn packetlistenerplayin) {
        TAB_COMPLETION_EXECUTOR.submit(() -> packetlistenerplayin.a(this));
        // cSpigot end
    }

    public String a() {
        return this.a;
    }

    public BlockPosition b() {
        return this.b;
    }
}
