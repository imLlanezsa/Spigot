package net.minecraft.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class PacketPlayOutEntityMetadata implements Packet<PacketListenerPlayOut> {

    private int a;
    private List<DataWatcher.WatchableObject> b;

    public PacketPlayOutEntityMetadata() {}

    public PacketPlayOutEntityMetadata(int i, DataWatcher datawatcher, boolean flag) {
        this.a = i;
        if (flag) {
            this.b = datawatcher.c();
        } else {
            this.b = datawatcher.b();
        }
    }

    /**
     * <p>
     * Send fake health to other clients.<br>
     * This fixes/patches exploits or unfair advantage given to people with
     * DamageIndicator mods or texture packs.
     * </p>
     */
    public boolean obfuscateHealth() {
        boolean found = false;

        if (b == null) return found;

        for ( Iterator<DataWatcher.WatchableObject> it = b.iterator(); it.hasNext(); ) {
            DataWatcher.WatchableObject watchable = it.next();
            if (watchable.a() == 6) {
                it.remove();
                found = true;
            }
        }

        if (found) {
            this.b.add(new DataWatcher.WatchableObject(3, 6, 1.0F));
        }
        return found;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = packetdataserializer.e();
        this.b = DataWatcher.b(packetdataserializer);
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.b(this.a);
        DataWatcher.a(this.b, packetdataserializer);
    }
    public void a(PacketListenerPlayOut packetlistenerplayout) {
        packetlistenerplayout.a(this);
    }

}
