package net.minecraft.server;

// CraftBukkit start
import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.util.Iterator;

import org.bukkit.craftbukkit.util.CraftIconCache;
import org.bukkit.entity.Player;

// CraftBukkit end

public class PacketStatusListener implements PacketStatusInListener {

    private static final IChatBaseComponent a = new ChatComponentText("Status request has been handled.");
    private final MinecraftServer minecraftServer;
    private final NetworkManager networkManager;
    private boolean d;

    public PacketStatusListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.minecraftServer = minecraftserver;
        this.networkManager = networkmanager;
    }

    public void a(IChatBaseComponent ichatbasecomponent) {}

    public void a(PacketStatusInStart packetstatusinstart) {
        if (this.d) {
            this.networkManager.close(PacketStatusListener.a);
            // CraftBukkit start - fire ping event
            return;
        }
        this.d = true;
        com.destroystokyo.paper.network.StandardPaperServerListPingEventImpl.processRequest(this.minecraftServer, this.networkManager);
        // CraftBukkit end
    }

    public void a(PacketStatusInPing packetstatusinping) {
        this.networkManager.handle(new PacketStatusOutPong(packetstatusinping.a()));
        this.networkManager.close(PacketStatusListener.a);
    }
}
