package net.minecraft.server;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import io.github.cspigot.config.SettingsConfig;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
// CraftBukkit end

public class LoginListener implements PacketLoginInListener, IUpdatePlayerListBox {

    // Paper start - Cache authenticator threads
    private static final AtomicInteger threadId = new AtomicInteger(0);
    private static final java.util.concurrent.ExecutorService authenticatorPool = java.util.concurrent.Executors
            .newCachedThreadPool(r -> { // Yatopia start - make sure produced threads are daemon ones
                        Thread thread = new Thread(r, "User Authenticator #" + threadId.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    } // Yatopia end
            );
    // Paper end

    private static final AtomicInteger b = new AtomicInteger(0);
    private static final Logger c = LogManager.getLogger();
    private static final Random random = new Random();
    private final byte[] e = new byte[4];
    private final MinecraftServer server;
    public final NetworkManager networkManager;
    private net.minecraft.server.LoginListener.EnumProtocolState g; public final net.minecraft.server.LoginListener.EnumProtocolState getLoginState() { return this.g; }; // Paper - OBFHELPER
    private int h;
    private GameProfile i; private void setGameProfile(GameProfile profile) { i = profile; } public final GameProfile getGameProfile() { return i; } // Paper - OBFHELPER
    private String j;
    private SecretKey loginKey;
    private EntityPlayer l;
    public String hostname = ""; // CraftBukkit - add field

    public LoginListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.g = LoginListener.EnumProtocolState.HELLO;
        this.j = "";
        this.server = minecraftserver;
        this.networkManager = networkmanager;
        LoginListener.random.nextBytes(this.e);
    }

    public void c() {
        if (this.g == LoginListener.EnumProtocolState.READY_TO_ACCEPT) {
            // Paper start - prevent logins to be processed even though disconnect was called
            if (networkManager.isConnected()) {
                this.b();
            }
            // Paper end
        } else if (this.g == LoginListener.EnumProtocolState.e) {
            EntityPlayer entityplayer = this.server.getPlayerList().a(this.i.getId());

            if (entityplayer == null) {
                this.g = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                this.server.getPlayerList().a(this.networkManager, this.l);
                this.l = null;
            }
        }

        if (this.h++ == 600) {
            this.disconnect("Took too long to log in");
        }

    }

    public void disconnect(String s) {
        try {
            // cSpigot start - toggleable player ip hiding
            String output = this.d();
            if (!SettingsConfig.SHOW_PLAYER_IPS_CONSOLE) {
                // Replace the address before the ":port" part of the string
                String port = output.split(":") [1];
                output = "hidden-address:" + port;
            }
            LoginListener.c.info("Disconnecting " + output + ": " + s);
            ChatComponentText chatcomponenttext = new ChatComponentText(s);

            this.networkManager.handle(new PacketLoginOutDisconnect(chatcomponenttext));
            this.networkManager.close(chatcomponenttext);
        } catch (Exception exception) {
            LoginListener.c.error("Error whilst disconnecting player", exception);
        }

    }

    // Spigot start
    public void initUUID()
    {
        UUID uuid;
        if ( networkManager.spoofedUUID != null )
        {
            uuid = networkManager.spoofedUUID;
        } else
        {
            uuid = UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + this.i.getName() ).getBytes( Charsets.UTF_8 ) );
        }

        this.i = new GameProfile( uuid, this.i.getName() );

        if (networkManager.spoofedProfile != null)
        {
            for ( com.mojang.authlib.properties.Property property : networkManager.spoofedProfile )
            {
                this.i.getProperties().put( property.getName(), property );
            }
        }
    }
    // Spigot end

    public void b() {
        // CraftBukkit start - fire PlayerLoginEvent
        EntityPlayer s = this.server.getPlayerList().attemptLogin(this, this.i, hostname);

        if (s == null) {
            // this.disconnect(s);
            // CraftBukkit end
        } else {
            this.g = LoginListener.EnumProtocolState.ACCEPTED;
            if (this.server.aK() >= 0 && !this.networkManager.c()) {
                this.networkManager.a(new PacketLoginOutSetCompression(this.server.aK()), new ChannelFutureListener() {
                    public void a(ChannelFuture channelfuture) throws Exception {
                        LoginListener.this.networkManager.a(LoginListener.this.server.aK());
                    }

                    public void operationComplete(ChannelFuture future) throws Exception { // CraftBukkit - fix decompile error
                        this.a(future);
                    }
                });
            }

            this.networkManager.handle(new PacketLoginOutSuccess(this.i));
            EntityPlayer entityplayer = this.server.getPlayerList().a(this.i.getId());

            if (entityplayer != null) {
                this.g = LoginListener.EnumProtocolState.e;
                this.l = this.server.getPlayerList().processLogin(this.i, s); // CraftBukkit - add player reference
            } else {
                this.server.getPlayerList().a(this.networkManager, this.server.getPlayerList().processLogin(this.i, s)); // CraftBukkit - add player reference
            }
        }

    }

    public void a(IChatBaseComponent ichatbasecomponent) {
        // cSpigot start - toggleable player ip hiding
        String output = this.d();
        if (!SettingsConfig.SHOW_PLAYER_IPS_CONSOLE) {
            // Replace the address before the ":port" part of the string
            String port = output.split(":") [1];
            output = "hidden-address:" + port;
        }

        LoginListener.c.info(output + " lost connection: " + ichatbasecomponent.c());
    }

    public String d() {
        return networkManager == null ? null : (networkManager.getSocketAddress() == null ? null : networkManager.getSocketAddress().toString());
    }

    public void a(PacketLoginInStart packetlogininstart) {
        Validate.validState(this.g == LoginListener.EnumProtocolState.HELLO, "Unexpected hello packet");
        this.i = packetlogininstart.a();
        if (this.server.getOnlineMode() && !this.networkManager.c()) {
            this.g = LoginListener.EnumProtocolState.KEY;
            this.networkManager.handle(new PacketLoginOutEncryptionBegin(this.j, this.server.Q().getPublic(), this.e));
        } else {
            authenticatorPool.execute(() -> {
                    try {
                        initUUID();
                        new LoginHandler().fireEvents();
                    } catch (Exception ex) {
                        disconnect("Failed to verify username!");
                        server.server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + i.getName(), ex);
                    }
            });
        }

    }

    public void a(PacketLoginInEncryptionBegin packetlogininencryptionbegin) {
        Validate.validState(this.g == LoginListener.EnumProtocolState.KEY, "Unexpected key packet");
        PrivateKey privatekey = this.server.Q().getPrivate();

        if (!Arrays.equals(this.e, packetlogininencryptionbegin.b(privatekey))) {
            throw new IllegalStateException("Invalid nonce!");
        } else {
            this.loginKey = packetlogininencryptionbegin.a(privatekey);
            this.g = LoginListener.EnumProtocolState.AUTHENTICATING;
            this.networkManager.a(this.loginKey);

            // cSpigot start - Use custom scheduler for this
            authenticatorPool.execute(() -> {
                    GameProfile gameprofile = LoginListener.this.i;

                    try {
                        String s = (new BigInteger(MinecraftEncryption.a(LoginListener.this.j, LoginListener.this.server.Q().getPublic(), LoginListener.this.loginKey))).toString(16);

                        LoginListener.this.i = LoginListener.this.server.aD().hasJoinedServer(new GameProfile((UUID) null, gameprofile.getName()), s);
                        if (LoginListener.this.i != null) {
                            // CraftBukkit start - fire PlayerPreLoginEvent
                            if (!networkManager.g()) {
                                return;
                            }

                            new LoginHandler().fireEvents();
                        } else if (LoginListener.this.server.T()) {
                            LoginListener.c.warn("Failed to verify username but will let them in anyway!");
                            LoginListener.this.i = LoginListener.this.a(gameprofile);
                            LoginListener.this.g = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                        } else {
                            LoginListener.this.disconnect("Failed to verify username!");
                            LoginListener.c.error("Username \'" + gameprofile.getName() + "\' tried to join with an invalid session"); // CraftBukkit - fix null pointer
                        }
                    } catch (AuthenticationUnavailableException authenticationunavailableexception) {
                        if (LoginListener.this.server.T()) {
                            LoginListener.c.warn("Authentication servers are down but will let them in anyway!");
                            LoginListener.this.i = LoginListener.this.a(gameprofile);
                            LoginListener.this.g = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                        } else {
                            LoginListener.this.disconnect("Authentication servers are down. Please try again later, sorry!");
                            LoginListener.c.error("Couldn\'t verify username because servers are unavailable");
                        }
                        // CraftBukkit start - catch all exceptions
                    } catch (Exception exception) {
                        disconnect("Failed to verify username!");
                        server.server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + gameprofile.getName(), exception);
                        // CraftBukkit end
                    }
            });
            // cSpigot end
        }
    }

    // Spigot start
    public class LoginHandler {

        public void fireEvents() throws Exception {
            String playerName = i.getName();
            java.net.InetAddress address = ((java.net.InetSocketAddress) networkManager.getSocketAddress()).getAddress();
            java.util.UUID uniqueId = i.getId();
            final org.bukkit.craftbukkit.CraftServer server = LoginListener.this.server.server;

            // Paper start
            PlayerProfile profile = CraftPlayerProfile.asBukkitMirror(getGameProfile());
            AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address, uniqueId, profile);
            server.getPluginManager().callEvent(asyncEvent);
            // cSpigot start - Only apply if it's been changed
            if (!profile.equals(asyncEvent.getPlayerProfile())) {
                profile = asyncEvent.getPlayerProfile();
                profile.complete(true);
                setGameProfile(CraftPlayerProfile.asAuthlib(profile));
                playerName = i.getName();
                uniqueId = i.getId();
            }
            // cSpigot end
            // Paper end

            if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
                final PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address, uniqueId);
                if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                    event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
                }
                Waitable<PlayerPreLoginEvent.Result> waitable = new Waitable<PlayerPreLoginEvent.Result>() {
                    @Override
                    protected PlayerPreLoginEvent.Result evaluate() {
                        server.getPluginManager().callEvent(event);
                        return event.getResult();
                    }
                };

                LoginListener.this.server.processQueue.add(waitable);
                if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED) {
                    disconnect(event.getKickMessage());
                    return;
                }
            } else {
                if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                    disconnect(asyncEvent.getKickMessage());
                    return;
                }
            }
            // CraftBukkit end
            LoginListener.c.info("UUID of player " + LoginListener.this.i.getName() + " is " + LoginListener.this.i.getId());
            LoginListener.this.g = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
        }
    }
    // Spigot end

    protected GameProfile a(GameProfile gameprofile) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + gameprofile.getName()).getBytes(Charsets.UTF_8));

        return new GameProfile(uuid, gameprofile.getName());
    }

    static enum EnumProtocolState {

        HELLO, KEY, AUTHENTICATING, READY_TO_ACCEPT, e, ACCEPTED;

        private EnumProtocolState() {}
    }
}
