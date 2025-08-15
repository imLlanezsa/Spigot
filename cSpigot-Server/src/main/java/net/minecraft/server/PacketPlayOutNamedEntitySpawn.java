package net.minecraft.server;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 4/9/2022
 * @version cSpigot
 */

public class PacketPlayOutNamedEntitySpawn implements Packet<PacketListenerPlayOut> {

    public int a; public int getEntityId() { return this.a; } public void setEntityId(int id) { this.a = id; }
    public UUID b; public UUID getUUID() { return this.b; } public void setUUID(UUID uuid) { this.b = uuid; }
    public int c; public int getRawX() { return this.c; }
    public int d; public int getRawY() { return this.d; }
    public int e; public int getRawZ() { return this.e; }
    public byte f; public byte getRawPitch() { return this.f; }
    public byte g; public byte getRawYaw() { return this.g; }
    public int h; public int getData() { return this.h; }
    public DataWatcher i;
    public List<DataWatcher.WatchableObject> j;

    public PacketPlayOutNamedEntitySpawn() {
    }

    public PacketPlayOutNamedEntitySpawn(EntityHuman var1) {
        this.a = var1.getId();
        this.b = var1.getProfile().getId();
        this.c = MathHelper.floor(var1.locX * 32.0D);
        this.d = MathHelper.floor(var1.locY * 32.0D);
        this.e = MathHelper.floor(var1.locZ * 32.0D);
        this.f = (byte)((int)(var1.yaw * 256.0F / 360.0F));
        this.g = (byte)((int)(var1.pitch * 256.0F / 360.0F));
        ItemStack var2 = var1.inventory.getItemInHand();
        this.h = var2 == null ? 0 : Item.getId(var2.getItem());
        try {
            this.i = var1.getDataWatcher();
        } catch (NullPointerException e) {
            this.i = var1.getDataWatchercSpigot();
        }
    }

    public void a(PacketDataSerializer var1) throws IOException {
        this.a = var1.e();
        this.b = var1.g();
        this.c = var1.readInt();
        this.d = var1.readInt();
        this.e = var1.readInt();
        this.f = var1.readByte();
        this.g = var1.readByte();
        this.h = var1.readShort();
        this.j = DataWatcher.b(var1);
    }

    public void b(PacketDataSerializer var1) throws IOException {
        var1.b(this.a);
        var1.a(this.b);
        var1.writeInt(this.c);
        var1.writeInt(this.d);
        var1.writeInt(this.e);
        var1.writeByte(this.f);
        var1.writeByte(this.g);
        var1.writeShort(this.h);
        this.i.a(var1);
    }

    public void a(PacketListenerPlayOut var1) {
        var1.a(this);
    }
}

