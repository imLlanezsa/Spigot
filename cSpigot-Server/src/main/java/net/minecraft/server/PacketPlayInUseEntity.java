package net.minecraft.server;

import java.io.IOException;

public class PacketPlayInUseEntity implements Packet<PacketListenerPlayIn> {
    private int a;
    private EnumEntityUseAction action;
    private Vec3D c;

    public int getEntityId() {
        return this.a;
    }

    public PacketPlayInUseEntity() {
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.e();
        this.action = (EnumEntityUseAction)packetdataserializer.a(EnumEntityUseAction.class);
        if (this.action == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
            this.c = new Vec3D((double)packetdataserializer.readFloat(), (double)packetdataserializer.readFloat(), (double)packetdataserializer.readFloat());
        }

    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.b(this.a);
        packetdataserializer.a(this.action);
        if (this.action == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
            packetdataserializer.writeFloat((float)this.c.a);
            packetdataserializer.writeFloat((float)this.c.b);
            packetdataserializer.writeFloat((float)this.c.c);
        }

    }

    public void a(PacketListenerPlayIn packetlistenerplayin) {
        packetlistenerplayin.a(this);
    }

    public Entity a(World world) {
        return world.a(this.a);
    }

    public EnumEntityUseAction a() {
        return this.action;
    }

    public Vec3D b() {
        return this.c;
    }

    public int getA() {
        return this.a;
    }

    public EnumEntityUseAction getAction() {
        return this.action;
    }

    public Vec3D getC() {
        return this.c;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setAction(EnumEntityUseAction action) {
        this.action = action;
    }

    public void setC(Vec3D c) {
        this.c = c;
    }

    public static enum EnumEntityUseAction {
        INTERACT,
        ATTACK,
        INTERACT_AT;

        private EnumEntityUseAction() {
        }
    }
}
