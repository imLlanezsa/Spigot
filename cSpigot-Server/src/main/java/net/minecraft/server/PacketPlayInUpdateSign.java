package net.minecraft.server;

import lombok.extern.log4j.Log4j2;
import io.github.cspigot.feature.crash.cSpigotAntiCrash;
import io.github.cspigot.utils.prot.CrashException;

import java.io.IOException;

@Log4j2
public class PacketPlayInUpdateSign implements Packet<PacketListenerPlayIn> {

    private BlockPosition a;
    private IChatBaseComponent[] b;

    public PacketPlayInUpdateSign() {}

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.c();
        this.b = new IChatBaseComponent[4];

        String[] lines = new String[4];
        for (int i = 0; i < 4; ++i) {
            String s = packetdataserializer.c(384);
            lines[i] = s;
        }

        if (cSpigotAntiCrash.checkSignExploit(lines)) {
            throw new CrashException("[cSpigot] Sign Crash Exploit detected!");
        }

        for (int i = 0; i < 4; ++i) {
            this.b[i] = IChatBaseComponent.ChatSerializer.a(lines[i]);
        }
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.a(this.a);

        for (int i = 0; i < 4; ++i) {
            IChatBaseComponent ichatbasecomponent = this.b[i];
            String s = IChatBaseComponent.ChatSerializer.a(ichatbasecomponent);

            packetdataserializer.a(s);
        }

    }

    public void a(PacketListenerPlayIn packetlistenerplayin) {
        packetlistenerplayin.a(this);
    }

    public BlockPosition a() {
        return this.a;
    }

    public IChatBaseComponent[] b() {
        return this.b;
    }
}
