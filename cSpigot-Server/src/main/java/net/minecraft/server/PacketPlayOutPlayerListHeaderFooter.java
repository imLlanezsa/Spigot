package net.minecraft.server;

import java.io.IOException;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class PacketPlayOutPlayerListHeaderFooter implements Packet<PacketListenerPlayOut> {
    public BaseComponent[] header;
    public BaseComponent[] footer;
    private IChatBaseComponent a;
    private IChatBaseComponent b;

    public PacketPlayOutPlayerListHeaderFooter() {
    }

    public PacketPlayOutPlayerListHeaderFooter(IChatBaseComponent ichatbasecomponent) {
        this.a = ichatbasecomponent;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.d();
        this.b = packetdataserializer.d();
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        if (this.header != null) {
            packetdataserializer.a(ComponentSerializer.toString(this.header));
        } else {
            packetdataserializer.a(this.a);
        }

        if (this.footer != null) {
            packetdataserializer.a(ComponentSerializer.toString(this.footer));
        } else {
            packetdataserializer.a(this.b);
        }

    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public BaseComponent[] getHeader() {
        return this.header;
    }

    public BaseComponent[] getFooter() {
        return this.footer;
    }

    public IChatBaseComponent getA() {
        return this.a;
    }

    public IChatBaseComponent getB() {
        return this.b;
    }

    public void setHeader(BaseComponent[] header) {
        this.header = header;
    }

    public void setFooter(BaseComponent[] footer) {
        this.footer = footer;
    }

    public void setA(IChatBaseComponent a) {
        this.a = a;
    }

    public void setB(IChatBaseComponent b) {
        this.b = b;
    }
}
