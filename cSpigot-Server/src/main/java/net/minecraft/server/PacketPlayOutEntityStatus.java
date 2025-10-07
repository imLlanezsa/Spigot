package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutEntityStatus implements Packet<PacketListenerPlayOut> {
    private int a;
    private byte b;

    public PacketPlayOutEntityStatus() {
    }

    public PacketPlayOutEntityStatus(Entity entity, byte b0) {
        this.a = entity.getId();
        this.b = b0;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.readInt();
        this.b = packetdataserializer.readByte();
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.writeInt(this.a);
        packetdataserializer.writeByte(this.b);
    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public int getA() {
        return this.a;
    }

    public byte getB() {
        return this.b;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setB(byte b) {
        this.b = b;
    }
}
