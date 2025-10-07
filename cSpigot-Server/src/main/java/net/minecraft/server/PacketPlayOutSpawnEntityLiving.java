package net.minecraft.server;

import java.io.IOException;
import java.util.List;

public class PacketPlayOutSpawnEntityLiving implements Packet<PacketListenerPlayOut> {
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;
    private int f;
    private int g;
    private int h;
    private byte i;
    private byte j;
    private byte k;
    private DataWatcher l;
    private List<DataWatcher.WatchableObject> m;

    public PacketPlayOutSpawnEntityLiving() {
    }

    public PacketPlayOutSpawnEntityLiving(EntityLiving entityliving) {
        this.a = entityliving.getId();
        this.b = (byte)EntityTypes.a(entityliving);
        this.c = MathHelper.floor(entityliving.locX * (double)32.0F);
        this.d = MathHelper.floor(entityliving.locY * (double)32.0F);
        this.e = MathHelper.floor(entityliving.locZ * (double)32.0F);
        this.i = (byte)((int)(entityliving.yaw * 256.0F / 360.0F));
        this.j = (byte)((int)(entityliving.pitch * 256.0F / 360.0F));
        this.k = (byte)((int)(entityliving.aK * 256.0F / 360.0F));
        double d0 = 3.9;
        double d1 = entityliving.motX;
        double d2 = entityliving.motY;
        double d3 = entityliving.motZ;
        if (d1 < -d0) {
            d1 = -d0;
        }

        if (d2 < -d0) {
            d2 = -d0;
        }

        if (d3 < -d0) {
            d3 = -d0;
        }

        if (d1 > d0) {
            d1 = d0;
        }

        if (d2 > d0) {
            d2 = d0;
        }

        if (d3 > d0) {
            d3 = d0;
        }

        this.f = (int)(d1 * (double)8000.0F);
        this.g = (int)(d2 * (double)8000.0F);
        this.h = (int)(d3 * (double)8000.0F);
        this.l = entityliving.getDataWatcher();
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.e();
        this.b = packetdataserializer.readByte() & 255;
        this.c = packetdataserializer.readInt();
        this.d = packetdataserializer.readInt();
        this.e = packetdataserializer.readInt();
        this.i = packetdataserializer.readByte();
        this.j = packetdataserializer.readByte();
        this.k = packetdataserializer.readByte();
        this.f = packetdataserializer.readShort();
        this.g = packetdataserializer.readShort();
        this.h = packetdataserializer.readShort();
        this.m = DataWatcher.b(packetdataserializer);
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.b(this.a);
        packetdataserializer.writeByte(this.b & 255);
        packetdataserializer.writeInt(this.c);
        packetdataserializer.writeInt(this.d);
        packetdataserializer.writeInt(this.e);
        packetdataserializer.writeByte(this.i);
        packetdataserializer.writeByte(this.j);
        packetdataserializer.writeByte(this.k);
        packetdataserializer.writeShort(this.f);
        packetdataserializer.writeShort(this.g);
        packetdataserializer.writeShort(this.h);
        this.l.a(packetdataserializer);
    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

    public int getA() {
        return this.a;
    }

    public int getB() {
        return this.b;
    }

    public int getC() {
        return this.c;
    }

    public int getD() {
        return this.d;
    }

    public int getE() {
        return this.e;
    }

    public int getF() {
        return this.f;
    }

    public int getG() {
        return this.g;
    }

    public int getH() {
        return this.h;
    }

    public byte getI() {
        return this.i;
    }

    public byte getJ() {
        return this.j;
    }

    public byte getK() {
        return this.k;
    }

    public DataWatcher getL() {
        return this.l;
    }

    public List<DataWatcher.WatchableObject> getM() {
        return this.m;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setC(int c) {
        this.c = c;
    }

    public void setD(int d) {
        this.d = d;
    }

    public void setE(int e) {
        this.e = e;
    }

    public void setF(int f) {
        this.f = f;
    }

    public void setG(int g) {
        this.g = g;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setI(byte i) {
        this.i = i;
    }

    public void setJ(byte j) {
        this.j = j;
    }

    public void setK(byte k) {
        this.k = k;
    }

    public void setL(DataWatcher l) {
        this.l = l;
    }

    public void setM(List<DataWatcher.WatchableObject> m) {
        this.m = m;
    }
}
