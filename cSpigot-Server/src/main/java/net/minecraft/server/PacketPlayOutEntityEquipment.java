package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutEntityEquipment implements Packet<PacketListenerPlayOut> {

    private int a;
    private int b;
    private ItemStack c;

    public PacketPlayOutEntityEquipment() {}

    public PacketPlayOutEntityEquipment(int var1, int var2, ItemStack var3) {
        this.a = var1;
        this.b = var2;
        this.c = var3 == null ? null : var3.cloneItemStack();
    }

    public PacketPlayOutEntityEquipment(int var1, int var2, ItemStack var3, boolean obfuscate) {
        this.a = var1;
        this.b = var2;
        this.c = var3 == null ? null : var3.cloneItemStack();

        // cSpigot start - Add obfuscation to attributes of this armor item
        if (obfuscate && c != null) {
            boolean hadEnchants = c.hasEnchantments();
            if (c.e()) c.setData(c.j());
            c.r();
            if (c.getTag() == null) c.setTag(new NBTTagCompound());

            if (hadEnchants) {
                NBTTagList obfuscatedEnchantments = new NBTTagList();
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setShort("id", (short) 0);
                nbttagcompound.setShort("lvl", (short) 0);
                obfuscatedEnchantments.add(nbttagcompound);
                c.getTag().set("ench", obfuscatedEnchantments);
            }
        }
        // cSpigot end
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.e();
        this.b = packetdataserializer.readShort();
        this.c = packetdataserializer.i();
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.b(this.a);
        packetdataserializer.writeShort(this.b);
        packetdataserializer.a(this.c);
    }

    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }
}
