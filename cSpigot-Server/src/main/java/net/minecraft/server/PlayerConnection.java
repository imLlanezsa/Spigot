package net.minecraft.server;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.util.NumberConversions;
import org.github.paperspigot.PaperSpigotConfig;

// cSpigot start
import org.jetbrains.annotations.Nullable;
import io.github.cspigot.cSpigot;
import io.github.cspigot.api.handlers.impl.MovementHandler;
import io.github.cspigot.api.handlers.impl.PacketHandler;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.crash.cSpigotAntiCrash;
// cSpigot end

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class PlayerConnection implements PacketListenerPlayIn, IUpdatePlayerListBox {

    private static final Logger c = LogManager.getLogger();
    public final NetworkManager networkManager;
    private final MinecraftServer minecraftServer;
    public EntityPlayer player;
    private int e;
    private int f;
    private int g;
    private boolean h;
    private int i;
    private long j;
    private long k;
    // CraftBukkit start - multithreaded fields
    private volatile int chatThrottle;
    private static final AtomicIntegerFieldUpdater chatSpamField = AtomicIntegerFieldUpdater.newUpdater(PlayerConnection.class, "chatThrottle");
    // CraftBukkit end
    private int m;
    private IntHashMap<Short> n = new IntHashMap();
    private double o;
    private double p;
    private double q;
    private boolean checkMovement = true;
    boolean processedDisconnect; // CraftBukkit - added
    public long lastMotionTick = 0L;
    // cSpigot - Exploit Patches
    private long lastCustomPayloadPacketTS = -1L;
    private int lastBookTick;
    private boolean isExploiter = false;

    public boolean isPassable(BlockPosition position) {
        IBlockData data = player.world.getType(position);

        Block block = data.getBlock();

        if (block instanceof BlockFenceGate) return data.get(BlockFenceGate.OPEN);

        return block.a(player.world, position, data) == null || !block.a(data, false);
    }

    public boolean isFullCube(BlockPosition position) {
        IBlockData data = player.world.getType(position);

        Block block = data.getBlock();

        AxisAlignedBB blockBB = block.a(player.world, position, data);

        if (blockBB == null)
            return false;

        return block.minX == 0.0D && block.minY == 0.0D && block.minZ == 0.0D && block.maxX == 1.0D && block.maxY == 1.0D && block.maxZ == 1.0D;
    }
    // cSpigot end

    public PlayerConnection(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        this.minecraftServer = minecraftserver;
        this.networkManager = networkmanager;
        this.lastBookTick = MinecraftServer.currentTick;
        this.player = entityplayer;
        this.server = minecraftserver.server; // CraftBukkit start - add fields and methods

        networkmanager.a(this);
        entityplayer.playerConnection = this;
    }

    private final org.bukkit.craftbukkit.CraftServer server;
    private int lastTick = MinecraftServer.currentTick;
    private long nextValidSwingTick = MinecraftServer.currentTick;
    private int lastDropTick = MinecraftServer.currentTick;
    private int dropCount = 0;

    private static final int SURVIVAL_PLACE_DISTANCE_SQUARED = 6 * 6;
    private static final int CREATIVE_PLACE_DISTANCE_SQUARED = 7 * 7;

    // Get position of last block hit for BlockDamageLevel.STOPPED
    private double lastPosX = Double.MAX_VALUE;
    private double lastPosY = Double.MAX_VALUE;
    private double lastPosZ = Double.MAX_VALUE;
    private float lastPitch = Float.MAX_VALUE;
    private float lastYaw = Float.MAX_VALUE;
    private boolean justTeleported = false;
    private boolean hasMoved; // Spigot

    // cSpigot start - backport 1.12.2 keepalive
    private boolean awaitingKeepAlive;
    private boolean disconnecting;
    private boolean noKeepalives; // Prevent spamming the warning

    public static long KEEPALIVE_LIMIT = 30000;
    // cSpigot end

    public CraftPlayer getPlayer() {
        return (this.player == null) ? null : this.player.getBukkitEntity();
    }
    private final static HashSet<Integer> invalidItems = new HashSet<>(Arrays.asList(8, 9, 10, 11, 26, 34, 36, 43, 51, 52, 55, 59, 60, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94, 104, 105, 115, 117, 118, 119, 125, 127, 132, 140, 141, 142, 144)); // TODO: Check after every update.
    // CraftBukkit end

    //PAIL: Rename -> tick()
    public void c() {
        this.h = false;
        ++this.e;

        // cSpigot - back port 1.12.2 keepalive handling
        if (!SettingsConfig.OPTIMIZED_KEEP_ALIVE) {
            if ((long) this.e - this.k > 40L) {
                this.k = this.e;
                this.j = this.d();
                this.i = (int) this.j;
                this.sendPacket(new PacketPlayOutKeepAlive(this.i));
            }
        } else {
            // Paper Start - give clients enter longer time to respond to pings as per pre 1.12.2 timings
            // This should effectively place the keepalive handling back to "as it was" before 1.12.2
            final long currentTime = this.d();
            final long elapsedTime = currentTime - this.getLastPing();

            if (this.isPendingPing()) {
                if (!this.processedDisconnect && elapsedTime >= KEEPALIVE_LIMIT) { // check keepalive limit, don't fire if already disconnected
                    c.warn("{} was kicked due to keepalive timeout!", this.player.getName()); // more info
                    this.disconnect("Timed out");
                }
            } else {
                if (elapsedTime >= 15000L) { // 15 seconds
                    this.setPendingPing(true);
                    this.setLastPing(currentTime);
                    this.setKeepAliveID((int) currentTime);
                    this.sendPacket(new PacketPlayOutKeepAlive(this.getKeepAliveID()));
                }
            }
            // Paper end
        }
        // cSpigot end


        // CraftBukkit start
        for (int spam; (spam = this.chatThrottle) > 0 && !chatSpamField.compareAndSet(this, spam, spam - 1); ) ;
        /* Use thread-safe field access instead
        if (this.chatThrottle > 0) {
            --this.chatThrottle;
        }
        */
        // CraftBukkit end

        if (this.m > 0) {
            --this.m;
        }

        if (this.player.D() > 0L && this.minecraftServer.getIdleTimeout() > 0 && MinecraftServer.az() - this.player.D() > (long) (this.minecraftServer.getIdleTimeout() * 1000 * 60)) {
            this.player.resetIdleTimer(); // CraftBukkit - SPIGOT-854
            this.disconnect("You have been idle for too long!");
        }

    }

    // cSpigot start - Improve KeepAlive handling
    public void a(PacketPlayInKeepAlive packetplayinkeepalive) {
        if (!SettingsConfig.OPTIMIZED_KEEP_ALIVE) {
            if (packetplayinkeepalive.a() == this.i) {
                int i = (int) (this.d() - this.j);

                this.player.ping = (this.player.ping * 3 + i) / 4;
            }
            return;
        }


        // Prevent spam
        if (this.noKeepalives) {
            return;
        }

        if (this.awaitingKeepAlive && packetplayinkeepalive.a() == getKeepAliveID()) {
            int i = (int) (this.d() - this.getLastPing());
            this.player.ping = (this.player.ping * 3 + i) / 4;
            this.awaitingKeepAlive = false;
            return;
        }

        // Allow 1.8.8 clients to safely download and load worlds
        // without being disconnected for not responding during that
        if (packetplayinkeepalive.a() == 0) {
            return;
        }

        this.noKeepalives = true;
        c.warn("{} sent an invalid keepalive! pending keepalive: {} got id: {} expected id: {}", this.player.getName(), this.isPendingPing(), packetplayinkeepalive.a(), this.getKeepAliveID());
        this.minecraftServer.postToMainThread(() -> disconnect("invalid keepalive"));
    }
    // cSpigot end

    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    public NetworkManager a() {
        return this.networkManager;
    }

    public void disconnect(String s) {
        // cSpigot start - Ensure thread-safety
        if (!Bukkit.isPrimaryThread()) {
            final String reason = s;
            MinecraftServer.getServer().processQueue.add(() -> this.disconnect(reason));
            return;
        }
        // cSpigot end

        if (disconnecting) return;
        // CraftBukkit start - fire PlayerKickEvent
        String leaveMessage = EnumChatFormat.YELLOW + this.player.getName() + " left the game.";

        PlayerKickEvent event = new PlayerKickEvent(this.server.getPlayer(this.player), s, leaveMessage);

        if (this.server.getServer().isRunning()) {
            this.server.getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            // Do not kick the player
            return;
        }
        // Send the possibly modified leave message
        s = event.getReason();

        disconnecting = true;

        // CraftBukkit end
        final ChatComponentText chatcomponenttext = new ChatComponentText(s);

        this.networkManager.a(new PacketPlayOutKickDisconnect(chatcomponenttext), future -> PlayerConnection.this.networkManager.close(chatcomponenttext));
        this.a(chatcomponenttext); // CraftBukkit - fire quit instantly
        this.networkManager.k();
        // CraftBukkit - Don't wait
        this.minecraftServer.postToMainThread(this.networkManager::l);
    }

    public void a(PacketPlayInSteerVehicle packetplayinsteervehicle) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsteervehicle, this, this.player.u());
        this.player.a(packetplayinsteervehicle.a(), packetplayinsteervehicle.b(), packetplayinsteervehicle.c(), packetplayinsteervehicle.d());
    }

    private boolean b(PacketPlayInFlying packetplayinflying) {
        // cSpigot Start: Fix flying packet exploits
        return !cSpigotAntiCrash.checkPosition(packetplayinflying);
        // cSpigot End
    }

    public void a(PacketPlayInFlying packetplayinflying) {
        PlayerConnectionUtils.ensureMainThread(packetplayinflying, this, this.player.u());

        if (this.b(packetplayinflying)) {
            this.disconnect("Invalid move packet received");
            return;
        }

        WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);

        this.h = true;
        if (!this.player.viewingCredits) {
            double d0 = this.player.locX;
            double d1 = this.player.locY;
            double d2 = this.player.locZ;
            double d3 = 0.0D;
            double d4 = packetplayinflying.a() - this.o;
            double d5 = packetplayinflying.b() - this.p;
            double d6 = packetplayinflying.c() - this.q;

            if (packetplayinflying.g()) {
                d3 = d4 * d4 + d5 * d5 + d6 * d6;
                if (!this.checkMovement && d3 < 0.25D) {
                    this.checkMovement = true;
                }
            }
            // CraftBukkit start - fire PlayerMoveEvent
            Player player = this.getPlayer();
            // Spigot Start
            if (!hasMoved) {
                Location curPos = player.getLocation();
                lastPosX = curPos.getX();
                lastPosY = curPos.getY();
                lastPosZ = curPos.getZ();
                lastYaw = curPos.getYaw();
                lastPitch = curPos.getPitch();
                hasMoved = true;
            }
            // Spigot End
            Location from = new Location(player.getWorld(), lastPosX, lastPosY, lastPosZ, lastYaw, lastPitch); // Get the Players previous Event location.
            Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

            // If the packet contains movement information then we update the To location with the correct XYZ.
            if (packetplayinflying.hasPos && !(packetplayinflying.hasPos && packetplayinflying.y == -999.0D)) {
                to.setX(packetplayinflying.x);
                to.setY(packetplayinflying.y);
                to.setZ(packetplayinflying.z);
            }

            // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
            if (packetplayinflying.hasLook) {
                to.setYaw(packetplayinflying.yaw);
                to.setPitch(packetplayinflying.pitch);
            }

            // Prevent 40 event-calls for less than enter single pixel of movement >.>
            double delta = Math.pow(this.lastPosX - to.getX(), 2) + Math.pow(this.lastPosY - to.getY(), 2) + Math.pow(this.lastPosZ - to.getZ(), 2);
            float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

            if (this.checkMovement && !this.player.dead) {
                // cSpigot start - Add API for Movement Listeners (Safer and better than PlayerMoveEvent)
                // First check if there is a condition matching our if statements below
                // Then go to enter the for loop, otherwise it's just eating up raw performance.
                boolean location = packetplayinflying.hasPos && delta > 0.0D;
                boolean rotation = packetplayinflying.hasLook && deltaAngle > 0.0F;
                if (location || rotation) {
                    for ( MovementHandler handler : cSpigot.getInstance().getMovementHandlers() ) {
                        if (location) {
                            try {
                                handler.handleUpdateLocation(player, to, from, packetplayinflying);
                            } catch (Exception e) {
                                c.info("[cSpigot] A MovementHandler failed to successfully handle location update!");
                                c.info("[cSpigot] Error: " + e.getMessage());
                                c.error(e);
                            }
                        }
                        if (rotation) {
                            try {
                                handler.handleUpdateRotation(player, to, from, packetplayinflying);
                            } catch (Exception e) {
                                c.info("[cSpigot] A MovementHandler failed to successfully handle rotation update!");
                                c.info("[cSpigot] Error: " + e.getMessage());
                                c.error(e);
                            }
                        }
                    }
                }
                // cSpigot end
            }

            if ((delta > 1f / 256 || deltaAngle > 10f) && (this.checkMovement && !this.player.dead)) {
                this.lastPosX = to.getX();
                this.lastPosY = to.getY();
                this.lastPosZ = to.getZ();
                this.lastYaw = to.getYaw();
                this.lastPitch = to.getPitch();

                // Skip the first time we do this
                if (SettingsConfig.TOGGLE_PLAYER_MOVE_EVENT && PlayerMoveEvent.getHandlerList().getRegisteredListeners().length > 0) { // cSpigot - make it toggleable
                    Location oldTo = to.clone();
                    PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                    this.server.getPluginManager().callEvent(event);

                    // If the event is cancelled we move the player back to their old location.
                    if (event.isCancelled()) {
                        this.player.playerConnection.sendPacket(new PacketPlayOutPosition(from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch(), Collections.<PacketPlayOutPosition.EnumPlayerTeleportFlags>emptySet()));
                        return;
                    }

                        /* If enter Plugin has changed the To destination then we teleport the Player
                        there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                        We only do this if the Event was not cancelled. */
                    if (!oldTo.equals(event.getTo()) && !event.isCancelled()) {
                        this.player.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        return;
                    }

                        /* Check to see if the Players Location has somehow changed during the call of the event.
                        This can happen due to enter plugin teleporting the player instead of using .setTo() */
                    if (!from.equals(this.getPlayer().getLocation()) && this.justTeleported) {
                        this.justTeleported = false;
                        return;
                    }
                }
            }

            if (this.checkMovement && !this.player.dead) {
                // CraftBukkit end
                this.f = this.e;
                double d7;
                double d8;
                double d9;

                if (this.player.vehicle != null) {
                    float f = this.player.yaw;
                    float f1 = this.player.pitch;

                    this.player.vehicle.al();
                    d7 = this.player.locX;
                    d8 = this.player.locY;
                    d9 = this.player.locZ;
                    if (packetplayinflying.h()) {
                        f = packetplayinflying.d();
                        f1 = packetplayinflying.e();
                    }

                    this.player.onGround = packetplayinflying.f();
                    this.player.l();
                    this.player.setLocation(d7, d8, d9, f, f1);
                    if (this.player.vehicle != null) {
                        this.player.vehicle.al();
                    }

                    this.minecraftServer.getPlayerList().d(this.player);
                    if (this.player.vehicle != null) {
                        this.player.vehicle.ai = true; // CraftBukkit - moved from below
                        if (d3 > 4.0D) {
                            Entity entity = this.player.vehicle;

                            this.player.playerConnection.sendPacket(new PacketPlayOutEntityTeleport(entity));
                            this.a(this.player.locX, this.player.locY, this.player.locZ, this.player.yaw, this.player.pitch);
                        }

                        // this.player.vehicle.ai = true; // CraftBukkit - moved up
                    }

                    if (this.checkMovement) {
                        this.o = this.player.locX;
                        this.p = this.player.locY;
                        this.q = this.player.locZ;
                    }

                    worldserver.g(this.player);
                    return;
                }

                if (this.player.isSleeping()) {
                    this.player.l();
                    this.player.setLocation(this.o, this.p, this.q, this.player.yaw, this.player.pitch);
                    worldserver.g(this.player);
                    return;
                }

                double d10 = this.player.locY;

                this.o = this.player.locX;
                this.p = this.player.locY;
                this.q = this.player.locZ;
                d7 = this.player.locX;
                d8 = this.player.locY;
                d9 = this.player.locZ;
                float f2 = this.player.yaw;
                float f3 = this.player.pitch;

                if (packetplayinflying.g() && packetplayinflying.b() == -999.0D) {
                    packetplayinflying.a(false);
                }

                if (packetplayinflying.g()) {
                    d7 = packetplayinflying.a();
                    d8 = packetplayinflying.b();
                    d9 = packetplayinflying.c();
                    if (Math.abs(packetplayinflying.a()) > 3.0E7D || Math.abs(packetplayinflying.c()) > 3.0E7D) {
                        this.disconnect("Illegal position");
                        return;
                    }
                }

                if (packetplayinflying.h()) {
                    f2 = packetplayinflying.d();
                    f3 = packetplayinflying.e();
                }

                this.player.l();
                this.player.setLocation(this.o, this.p, this.q, f2, f3);
                if (!this.checkMovement) {
                    return;
                }

                double d11 = d7 - this.player.locX;
                double d12 = d8 - this.player.locY;
                double d13 = d9 - this.player.locZ;
                double d14 = this.player.motX * this.player.motX + this.player.motY * this.player.motY + this.player.motZ * this.player.motZ;
                double d15 = d11 * d11 + d12 * d12 + d13 * d13;

                // Spigot: make "moved too quickly" limit configurable
                if (d15 - d14 > org.spigotmc.SpigotConfig.movedTooQuicklyThreshold && this.checkMovement && (!this.minecraftServer.T() || !this.minecraftServer.S().equals(this.player.getName()))) { // CraftBukkit - Added this.checkMovement condition to solve this check being triggered by teleports
                    PlayerConnection.c.warn(this.player.getName() + " moved too quickly! " + d11 + "," + d12 + "," + d13 + " (" + d11 + ", " + d12 + ", " + d13 + ")");
                    this.a(this.o, this.p, this.q, this.player.yaw, this.player.pitch);
                    return;
                }

                float f4 = 0.0625F;
                boolean flag = SettingsConfig.SKIP_PLAYER_MOVEMENT_CHECKS ?
                        worldserver.getCubesNoEntities(this.player, this.player.getBoundingBox().shrink((double) f4, (double) f4, (double) f4)).isEmpty()
                        : worldserver.getCubes(this.player, this.player.getBoundingBox().shrink((double) f4, (double) f4, (double) f4)).isEmpty();

                if (this.player.onGround && !packetplayinflying.f() && d12 > 0.0D) {
                    this.player.bF();
                }

                this.player.world.movementCache.clear(); // IonSpigot - Movement Cache

                this.player.move(d11, d12, d13);
                this.player.onGround = packetplayinflying.f();
                double d16 = d12;

                d11 = d7 - this.player.locX;
                d12 = d8 - this.player.locY;
                if (d12 > -0.5D || d12 < 0.5D) {
                    d12 = 0.0D;
                }

                d13 = d9 - this.player.locZ;
                d15 = d11 * d11 + d12 * d12 + d13 * d13;
                boolean flag1 = false;

                // Spigot: make "moved wrongly" limit configurable
                if (d15 > org.spigotmc.SpigotConfig.movedWronglyThreshold && !this.player.isSleeping() && !this.player.playerInteractManager.isCreative()) {
                    flag1 = true;
                    PlayerConnection.c.warn(this.player.getName() + " moved wrongly!");
                }

                this.player.setLocation(d7, d8, d9, f2, f3);
                this.player.checkMovement(this.player.locX - d0, this.player.locY - d1, this.player.locZ - d2);
                if (!this.player.noclip) {
                    if (SettingsConfig.SKIP_PLAYER_MOVEMENT_CHECKS) {
                        boolean flag2 = worldserver.getCubesNoEntities(this.player, this.player.getBoundingBox().shrink(f4,  f4, f4)).isEmpty();
                        boolean changedBlock = (int) d0 != (int) d7 || (int) d2 != (int) d9;
                        if (((flag && flag2) || (changedBlock && (!isPassable(new BlockPosition(d7, d8 + 1, d9)) || isFullCube(new BlockPosition(d7, d8 + 0.8D, d9)))))) {
                            MovingObjectPosition rayTrace = worldserver.rayTracePlayerCollision(new Vec3D(d0, d1 + 1, d2), new Vec3D(d7, d8 + 1, d9), false, true, false, this.player);

                            if (rayTrace == null && !flag && !flag2) {
                                rayTrace = worldserver.rayTracePlayerCollision(new Vec3D(d0, d1 + 0.8, d2), new Vec3D(d7, d8 + 0.8, d9), false, true, false, this.player);
                            }

                            if (rayTrace != null) {
                                IBlockData data = worldserver.getType(new BlockPosition(rayTrace.pos.a, rayTrace.pos.b, rayTrace.pos.c));
                                Block block = data.getBlock();
                                if (block != Blocks.END_PORTAL) {
                                    this.a(this.o, this.p, this.q, f2, f3);
                                    return;
                                }
                            }
                        }

                        if (flag && (flag1 || !flag2) && !this.player.isSleeping()) {
                            this.a(this.o, this.p, this.q, f2, f3);
                            return;
                        }
                    } else {
                        boolean flag2 = worldserver.getCubes(this.player, this.player.getBoundingBox().shrink((double) f4, (double) f4, (double) f4)).isEmpty();

                        if (flag && (flag1 || !flag2) && !this.player.isSleeping()) {
                            this.a(this.o, this.p, this.q, f2, f3);
                            return;
                        }
                    }
                }

                AxisAlignedBB axisalignedbb = this.player.getBoundingBox().grow(f4, f4, f4).a(0.0D, -0.55D, 0.0D);

                if (!this.minecraftServer.getAllowFlight() && !this.player.abilities.canFly && !worldserver.c(axisalignedbb)) {
                    if (d16 >= -0.03125D) {
                        ++this.g;
                        if (this.g > 80) {
                            PlayerConnection.c.warn(this.player.getName() + " was kicked for floating too long!");
                            this.disconnect("Flying is not enabled on this server");
                            return;
                        }
                    }
                } else {
                    this.g = 0;
                }

                this.player.onGround = packetplayinflying.f();
                this.minecraftServer.getPlayerList().d(this.player);
                this.player.a(this.player.locY - d10, packetplayinflying.f());
            } else if (this.e - this.f > 20) {
                this.a(this.o, this.p, this.q, this.player.yaw, this.player.pitch);
            }
        }
    }

    public void a(double d0, double d1, double d2, float f, float f1) {
        this.a(d0, d1, d2, f, f1, Collections.emptySet()); // CraftBukkit fix decompile errors
    }

    public void a(double d0, double d1, double d2, float f, float f1, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set) {
        // CraftBukkit start - Delegate to teleport(Location)
        Player player = this.getPlayer();
        Location from = player.getLocation();

        double x = d0;
        double y = d1;
        double z = d2;
        float yaw = f;
        float pitch = f1;
        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
            x += from.getX();
        }
        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
            y += from.getY();
        }
        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
            z += from.getZ();
        }
        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
            yaw += from.getYaw();
        }
        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
            pitch += from.getPitch();
        }


        Location to = new Location(this.getPlayer().getWorld(), x, y, z, yaw, pitch);
        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from.clone(), to.clone(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        this.server.getPluginManager().callEvent(event);

        if (event.isCancelled() || to.equals(event.getTo())) {
            set.clear(); // Can't relative teleport
            to = event.isCancelled() ? event.getFrom() : event.getTo();
            d0 = to.getX();
            d1 = to.getY();
            d2 = to.getZ();
            f = to.getYaw();
            f1 = to.getPitch();
        }

        this.internalTeleport(d0, d1, d2, f, f1, set);
    }

    public void teleport(Location dest) {
        internalTeleport(dest.getX(), dest.getY(), dest.getZ(), dest.getYaw(), dest.getPitch(), Collections.emptySet());
    }

    private void internalTeleport(double d0, double d1, double d2, float f, float f1, Set set) {
        if (Float.isNaN(f)) {
            f = 0;
        }

        if (Float.isNaN(f1)) {
            f1 = 0;
        }
        this.justTeleported = true;
        // CraftBukkit end
        this.checkMovement = false;
        this.o = d0;
        this.p = d1;
        this.q = d2;
        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X)) {
            this.o += this.player.locX;
        }

        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y)) {
            this.p += this.player.locY;
        }

        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Z)) {
            this.q += this.player.locZ;
        }

        float f2 = f;
        float f3 = f1;

        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.Y_ROT)) {
            f2 = f + this.player.yaw;
        }

        if (set.contains(PacketPlayOutPosition.EnumPlayerTeleportFlags.X_ROT)) {
            f3 = f1 + this.player.pitch;
        }

        // CraftBukkit start - update last location
        this.lastPosX = this.o;
        this.lastPosY = this.p;
        this.lastPosZ = this.q;
        this.lastYaw = f2;
        this.lastPitch = f3;
        // CraftBukkit end

        this.player.setLocation(this.o, this.p, this.q, f2, f3);
        this.player.playerConnection.sendPacket(new PacketPlayOutPosition(d0, d1, d2, f, f1, set));
    }

    public void a(PacketPlayInBlockDig packetplayinblockdig) {
        PlayerConnectionUtils.ensureMainThread(packetplayinblockdig, this, this.player.u());
        if (this.player.dead) return; // CraftBukkit
        WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
        BlockPosition blockposition = packetplayinblockdig.a();

        this.player.resetIdleTimer();
        // CraftBukkit start
        switch (SyntheticClass_1.a[packetplayinblockdig.c().ordinal()]) {
            case 1: // DROP_ITEM
                if (!this.player.isSpectator()) {
                    // limit how quickly items can be dropped
                    // If the ticks aren't the same then the count starts from 0 and we update the lastDropTick.
                    if (this.lastDropTick != MinecraftServer.currentTick) {
                        this.dropCount = 0;
                        this.lastDropTick = MinecraftServer.currentTick;
                    } else {
                        // Else we increment the drop count and check the amount.
                        this.dropCount++;
                        if (this.dropCount >= 20) {
                            this.c.warn(this.player.getName() + " dropped their items too quickly!");
                            this.disconnect("You dropped your items too quickly (Hacking?)");
                            return;
                        }
                    }
                    // CraftBukkit end
                    this.player.a(false);
                }

                return;

            case 2: // DROP_ALL_ITEMS
                if (!this.player.isSpectator()) {
                    this.player.a(true);
                }

                return;

            case 3: // RELEASE_USE_ITEM
                this.player.bU();
                return;

            case 4: // START_DESTROY_BLOCK
            case 5: // ABORT_DESTROY_BLOCK
            case 6: // STOP_DESTROY_BLOCK
                // Paper start - Don't allow digging in unloaded chunks
                if (!worldserver.isChunkLoaded(blockposition.getX() >> 4, blockposition.getZ() >> 4, true)) {
                    return;
                }
                // Paper end - Don't allow digging in unloaded chunks
                double d0 = this.player.locX - ((double) blockposition.getX() + 0.5D);
                double d1 = this.player.locY - ((double) blockposition.getY() + 0.5D) + 1.5D;
                double d2 = this.player.locZ - ((double) blockposition.getZ() + 0.5D);
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;

                if (d3 > 36.0D) {
                    if (worldserver.isChunkLoaded(blockposition.getX() >> 4, blockposition.getZ() >> 4, true)) // Paper - Fix block break desync - Don't send for unloaded chunks
                        this.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition)); // Paper - Fix block break desync
                    return;
                } else if (blockposition.getY() >= this.minecraftServer.getMaxBuildHeight()) {
                    int chunkX = blockposition.getX() >> 4;
                    int chunkZ = blockposition.getZ() >> 4;
                    if (worldserver.isChunkLoaded(chunkX, chunkZ >> 4, true)) { // Paper - Fix block break desync - Don't send for unloaded chunks
                        // cSpigot start - fix for Nocom exploit (I think)
                        int deltaX = chunkX - player.getChunkX();
                        int deltaZ = chunkZ - player.getChunkZ();
                        int distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                        if (distanceSquared <= Math.pow(worldserver.spigotConfig.viewDistance, 2)) {
                            // cSpigot end
                            this.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition)); // Paper - Fix block break desync
                        }
                    }
                    return;
                } else {
                    if (packetplayinblockdig.c() == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK) {
                        if (!this.minecraftServer.a(worldserver, blockposition, this.player) && worldserver.getWorldBorder().a(blockposition)) {
                            this.player.playerInteractManager.a(blockposition, packetplayinblockdig.b());
                        } else {
                            // CraftBukkit start - fire PlayerInteractEvent
                            CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockposition, packetplayinblockdig.b(), this.player.inventory.getItemInHand());
                            this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
                            // Update any tile entity data for this block
                            TileEntity tileentity = worldserver.getTileEntity(blockposition);
                            if (tileentity != null) {
                                this.player.playerConnection.sendPacket(tileentity.getUpdatePacket());
                            }
                            // CraftBukkit end
                        }
                    } else {
                        if (packetplayinblockdig.c() == PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK) {
                            this.player.playerInteractManager.a(blockposition);
                        } else if (packetplayinblockdig.c() == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
                            this.player.playerInteractManager.e();
                        }

                        if (worldserver.getType(blockposition).getBlock().getMaterial() != Material.AIR) {
                            this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
                        }
                    }

                    return;
                }

            default:
                throw new IllegalArgumentException("Invalid player action");
        }
        // CraftBukkit end
    }

    // Spigot start - limit place/interactions
    private long lastPlace = -1;
    private int packets = 0;

    public void a(PacketPlayInBlockPlace packetplayinblockplace) {
        PlayerConnectionUtils.ensureMainThread(packetplayinblockplace, this, this.player.u());
        WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
        boolean throttled = false;
        // PaperSpigot - Allow disabling the player interaction limiter
        if (PaperSpigotConfig.interactLimitEnabled && lastPlace != -1 && packetplayinblockplace.timestamp - lastPlace < 30 && packets++ >= 4) {
            throttled = true;
        } else if ( packetplayinblockplace.timestamp - lastPlace >= 30 || lastPlace == -1 )
        {
            this.lastPlace = (SettingsConfig.OPTIMIZE_PRINTER ? -1L : packetplayinblockplace.timestamp);
            packets = 0;
        }
        // Spigot end

        // CraftBukkit start
        if (this.player.dead) return;

        // CraftBukkit - if rightclick decremented the item, always send the update packet. */
        // this is not here for CraftBukkit's own functionality; rather it is to fix
        // a notch bug where the item doesn't update correctly.
        boolean always = false;
        // CraftBukkit end

        ItemStack itemstack = this.player.inventory.getItemInHand();
        boolean flag = false;

        BlockPosition blockposition = packetplayinblockplace.a();
        EnumDirection enumdirection = EnumDirection.fromType1(packetplayinblockplace.getFace());

        this.player.resetIdleTimer();
        if (packetplayinblockplace.getFace() == 255) {
            if (itemstack == null) {
                return;
            }

            // CraftBukkit start
            int itemstackAmount = itemstack.count;
            // Spigot start - skip the event if throttled
            if (!throttled) {
                // Raytrace to look for 'rogue armswings'
                float f1 = this.player.pitch;
                float f2 = this.player.yaw;
                double d0 = this.player.locX;
                double d1 = this.player.locY + (double) this.player.getHeadHeight();
                double d2 = this.player.locZ;
                Vec3D vec3d = new Vec3D(d0, d1, d2);

                float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
                float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
                float f5 = -MathHelper.cos(-f1 * 0.017453292F);
                float f6 = MathHelper.sin(-f1 * 0.017453292F);
                float f7 = f4 * f5;
                float f8 = f3 * f5;
                double d3 = player.playerInteractManager.getGameMode() == WorldSettings.EnumGamemode.CREATIVE ? 5.0D : 4.5D;
                Vec3D vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
                MovingObjectPosition movingobjectposition = this.player.world.rayTrace(vec3d, vec3d1, false);

                boolean cancelled;
                if (movingobjectposition == null || movingobjectposition.type != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                    PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.RIGHT_CLICK_AIR, itemstack);
                    cancelled = event.useItemInHand() == Event.Result.DENY;
                } else {
                    if (player.playerInteractManager.firedInteract) {
                        player.playerInteractManager.firedInteract = false;
                        cancelled = player.playerInteractManager.interactResult;
                    } else {
                        PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, movingobjectposition.a(), movingobjectposition.direction, itemstack, true);
                        cancelled = event.useItemInHand() == Event.Result.DENY;
                    }
                }

                // cSpigot start - Update cancelled interactions
                boolean needsUpdate = cancelled;
                if (!cancelled) {
                    needsUpdate = !this.player.playerInteractManager.useItem(this.player, this.player.world, itemstack) && (itemstack.getItem() instanceof ItemBucket || itemstack.getItem() instanceof ItemWaterLily);
                }

                if (cancelled || needsUpdate) {
                    this.player.getBukkitEntity().updateInventory(); // SPIGOT-2524

                    if (itemstack.getItem() instanceof ItemWaterLily) {
                        ItemWaterLily itemWaterLily = (ItemWaterLily) itemstack.getItem();
                        MovingObjectPosition mop = itemWaterLily.a(this.player.world, this.player, true);
                        if (mop != null && mop.type == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                            BlockPosition blockPosition = mop.a().up();
                            org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(worldserver, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ()).update(true, false);
                        }
                    } else if (itemstack.getItem() instanceof ItemBucket) {
                        ItemBucket itemBucket = (ItemBucket) itemstack.getItem();
                        MovingObjectPosition mop = itemBucket.a(this.player.world, this.player, itemBucket.a == Blocks.AIR);
                        if (mop != null && mop.type == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                            BlockPosition block = mop.a();
                            block = block.shift(mop.direction);
                            org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(worldserver, block.getX(), block.getY(), block.getZ()).update(true, false);
                        }
                    }
                }
                // cSpigot end
            } else if (MinecraftServer.currentTick - lastDropTick > 1) {
                this.player.playerInteractManager.useItem(this.player, this.player.world, itemstack);
            }
            // Spigot end

            // CraftBukkit - notch decrements the counter by 1 in the above method with food,
            // snowballs and so forth, but he does it in enter place that doesn't cause the
            // inventory update packet to get sent
            always = (itemstack.count != itemstackAmount) || itemstack.getItem() == Item.getItemOf(Blocks.WATERLILY);
            // CraftBukkit end
        } else if (blockposition.getY() >= this.minecraftServer.getMaxBuildHeight() - 1 && (enumdirection == EnumDirection.UP || blockposition.getY() >= this.minecraftServer.getMaxBuildHeight())) {
            ChatMessage chatmessage = new ChatMessage("build.tooHigh", this.minecraftServer.getMaxBuildHeight());

            chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
            this.player.playerConnection.sendPacket(new PacketPlayOutChat(chatmessage));
            flag = true;
        } else {
            // CraftBukkit start - Check if we can actually do something over this large a distance
            final Location eyeLoc = this.getPlayer().getEyeLocation();
            final double reachDistance = NumberConversions.square(eyeLoc.getX() - blockposition.getX()) + NumberConversions.square(eyeLoc.getY() - blockposition.getY()) + NumberConversions.square(eyeLoc.getZ() - blockposition.getZ());
            if (reachDistance > (this.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE ? CREATIVE_PLACE_DISTANCE_SQUARED : SURVIVAL_PLACE_DISTANCE_SQUARED)) {
                // cSpigot start - Update invalidly placed blocks (Credits: Quadflame/Refrains)
                this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition)); // block they placed on
                this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition.shift(enumdirection))); // block they placed
                if (this.player.activeContainer != null) {
                    final @Nullable Slot slot = this.player.activeContainer.getSlot(this.player.inventory, this.player.inventory.itemInHandIndex);
                    if (slot != null) {
                        this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, slot.rawSlotIndex, this.player.inventory.getItemInHand())); // item update
                    }
                }
                // cSpigot end
                return;
            }

            if (!worldserver.getWorldBorder().a(blockposition)) {
                // cSpigot start - Update invalidly placed blocks (Credits: Quadflame/Refrains)
                this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition)); // block they placed on
                this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition.shift(enumdirection))); // block they placed
                if (this.player.activeContainer != null) {
                    final @Nullable Slot slot = this.player.activeContainer.getSlot(this.player.inventory, this.player.inventory.itemInHandIndex);
                    if (slot != null) {
                        this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, slot.rawSlotIndex, this.player.inventory.getItemInHand())); // item update
                    }
                }
                // cSpigot end
                return;
            }

            if (this.checkMovement && this.player.e((double) blockposition.getX() + 0.5D, (double) blockposition.getY() + 0.5D, (double) blockposition.getZ() + 0.5D) < 64.0D && !this.minecraftServer.a(worldserver, blockposition, this.player) && worldserver.getWorldBorder().a(blockposition)) {
                always = throttled || !this.player.playerInteractManager.interact(this.player, worldserver, itemstack, blockposition, enumdirection, packetplayinblockplace.d(), packetplayinblockplace.e(), packetplayinblockplace.f());
            }

            flag = true;
        }

        if (flag) {
            this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition));
            this.player.playerConnection.sendPacket(new PacketPlayOutBlockChange(worldserver, blockposition.shift(enumdirection)));
        }

        itemstack = this.player.inventory.getItemInHand();
        if (itemstack != null && itemstack.count == 0) {
            this.player.inventory.items[this.player.inventory.itemInHandIndex] = null;
            itemstack = null;
        }

        if (itemstack == null || itemstack.l() == 0) {
            this.player.g = true;
            this.player.inventory.items[this.player.inventory.itemInHandIndex] = ItemStack.b(this.player.inventory.items[this.player.inventory.itemInHandIndex]);
            Slot slot = this.player.activeContainer.getSlot(this.player.inventory, this.player.inventory.itemInHandIndex);

            this.player.activeContainer.b();
            this.player.g = false;
            // CraftBukkit - TODO CHECK IF NEEDED -- new if structure might not need 'always'. Kept it in for now, but may be able to remove in future
            if (!ItemStack.matches(this.player.inventory.getItemInHand(), packetplayinblockplace.getItemStack()) || always) {
                this.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, slot.rawSlotIndex, this.player.inventory.getItemInHand()));
            }
        }

    }

    public void a(PacketPlayInSpectate packetplayinspectate) {
        PlayerConnectionUtils.ensureMainThread(packetplayinspectate, this, this.player.u());
        if (this.player.isSpectator()) {
            Entity entity = null;
            WorldServer[] aworldserver = this.minecraftServer.worldServer;
            int i = aworldserver.length;

            // CraftBukkit - use the worlds array list
            for (WorldServer worldserver : minecraftServer.worlds) {

                if (worldserver != null) {
                    entity = packetplayinspectate.a(worldserver);
                    if (entity != null) {
                        break;
                    }
                }
            }

            if (entity != null) {
                this.player.setSpectatorTarget(this.player);
                this.player.mount(null);
                this.player.getBukkitEntity().teleport(entity.getBukkitEntity(), PlayerTeleportEvent.TeleportCause.SPECTATE);
            }
        }

    }

    // CraftBukkit start
    public void a(PacketPlayInResourcePackStatus packetplayinresourcepackstatus) {
        // cSpigot start - Resource pack system
        if (player.waitingForResponse) {
            player.waitingForResponse = false;
        }
        // cSpigot end
        this.server.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(getPlayer(), PlayerResourcePackStatusEvent.Status.values()[packetplayinresourcepackstatus.b.ordinal()]));
    }
    // CraftBukkit end

    public void a(IChatBaseComponent ichatbasecomponent) {
        // CraftBukkit start - Rarely it would send a disconnect line twice
        if (this.processedDisconnect) {
            return;
        } else {
            this.processedDisconnect = true;
        }

        PlayerConnection.c.info(this.player.getName() + " lost connection: " + ichatbasecomponent.c()); // CraftBukkit: Don't toString(). // PAIL: Rename

        // CraftBukkit end

        this.player.q();
        String quitMessage = this.minecraftServer.getPlayerList().disconnect(this.player);
        if ((quitMessage != null) && (quitMessage.length() > 0)) {
            this.minecraftServer.getPlayerList().sendMessage(CraftChatMessage.fromString(quitMessage));
        }
        // CraftBukkit end
//        if (this.minecraftServer.T() && this.player.getName().equals(this.minecraftServer.S())) {
//            PlayerConnection.c.info("Stopping singleplayer server as player logged out");
//            this.minecraftServer.safeShutdown();
//        }

    }

    public void sendPacket(Packet<?> packet) {
        if (packet instanceof PacketPlayOutChat) {
            PacketPlayOutChat packetplayoutchat = (PacketPlayOutChat) packet;
            EntityHuman.EnumChatVisibility entityhuman_enumchatvisibility = this.player.getChatFlags();

            if (entityhuman_enumchatvisibility == EntityHuman.EnumChatVisibility.HIDDEN) {
                return;
            }

            if (entityhuman_enumchatvisibility == EntityHuman.EnumChatVisibility.SYSTEM && !packetplayoutchat.b()) {
                return;
            }
        }

        // CraftBukkit start
        if (packet == null || this.processedDisconnect) { // Spigot
            return;
        } else if (packet instanceof PacketPlayOutSpawnPosition) {
            PacketPlayOutSpawnPosition packet6 = (PacketPlayOutSpawnPosition) packet;
            this.player.compassTarget = new Location(this.getPlayer().getWorld(), packet6.position.getX(), packet6.position.getY(), packet6.position.getZ());
        }
        // CraftBukkit end

        // cSpigot start - Add PacketAPI for sent packets
        boolean cancelled = false;
        try {
            for ( PacketHandler handler : cSpigot.getInstance().getPacketHandlers()) {
                cancelled = !handler.handleSentPacket(this, packet);
            }
        } catch (Exception e) {
            c.info("[cSpigot] A PacketHandler failed to successfully handle sent packet!");
            c.info("[cSpigot] Error: " + e.getMessage());
            c.error(e);
        }

        if (cancelled) return;

        // cSpigot end

        try {
            this.networkManager.handle(packet);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Sending packet");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Packet being sent");

            crashreportsystemdetails.a("Packet class", new Callable() {
                public String a() throws Exception {
                    return packet.getClass().getCanonicalName();
                }

                public Object call() throws Exception {
                    return this.a();
                }
            });
            throw new ReportedException(crashreport);
        }
    }

    public void a(PacketPlayInHeldItemSlot packetplayinhelditemslot) {
        // CraftBukkit start
        if (this.player.dead) return;
        PlayerConnectionUtils.ensureMainThread(packetplayinhelditemslot, this, this.player.u());
        if (packetplayinhelditemslot.a() >= 0 && packetplayinhelditemslot.a() < PlayerInventory.getHotbarSize()) {
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayer(), this.player.inventory.itemInHandIndex, packetplayinhelditemslot.a());
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                this.sendPacket(new PacketPlayOutHeldItemSlot(this.player.inventory.itemInHandIndex));
                this.player.resetIdleTimer();
                return;
            }
            // CraftBukkit end
            this.player.inventory.itemInHandIndex = packetplayinhelditemslot.a();
            this.player.resetIdleTimer();
        } else {
            PlayerConnection.c.warn(this.player.getName() + " tried to set an invalid carried item");
            this.disconnect("Invalid hotbar selection (Hacking?)"); // CraftBukkit //Spigot "Nope" -> Descriptive reason
        }
    }

    public void a(PacketPlayInChat packetplayinchat) {
        // CraftBukkit start - async chat
        boolean isSync = packetplayinchat.a().startsWith("/");
        if (isSync) {
            PlayerConnectionUtils.ensureMainThread(packetplayinchat, this, this.player.u());
        }
        // CraftBukkit end
        if (this.player.dead || this.player.getChatFlags() == EntityHuman.EnumChatVisibility.HIDDEN) { // CraftBukkit - dead men tell no tales
            ChatMessage chatmessage = new ChatMessage("chat.cannotSend");

            chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
            this.sendPacket(new PacketPlayOutChat(chatmessage));
        } else {
            this.player.resetIdleTimer();
            String s = packetplayinchat.a();

            s = StringUtils.normalizeSpace(s);

            for (int i = 0; i < s.length(); ++i) {
                if (!SharedConstants.isAllowedChatCharacter(s.charAt(i))) {
                    // CraftBukkit start - threadsafety
                    if (!isSync) {
                        Waitable waitable = new Waitable() {
                            @Override
                            protected Object evaluate() {
                                PlayerConnection.this.disconnect("Illegal characters in chat");
                                return null;
                            }
                        };

                        this.minecraftServer.processQueue.add(waitable);

                        try {
                            waitable.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        this.disconnect("Illegal characters in chat");
                    }
                    // CraftBukkit end
                    return;
                }
            }

            String text = packetplayinchat.a();
            if (text.equalsIgnoreCase("mv ^(.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.++)$^") || text.contains("^(.*") || text.contains("++)$^")) {
                c.fatal("[cSpigot] {} has tried to execute Multiverse crash exploit!", player.getName());
                if (isSync) {
                    this.disconnect("You tried to crash the server!");
                } else {
                    AsyncUtil.runNextTick(() -> this.disconnect("You tried to crash the server!"));
                }
            }

            // CraftBukkit start
            if (isSync) {
                try {
                    this.minecraftServer.server.playerCommandState = true;
                    this.handleCommand(s);
                } finally {
                    this.minecraftServer.server.playerCommandState = false;
                }
            } else if (s.isEmpty()) {
                c.warn(this.player.getName() + " tried to send an empty message");
            } else if (getPlayer().isConversing()) {
                // Spigot start
                final String message = s;
                this.minecraftServer.processQueue.add( new Waitable()
                {
                    @Override
                    protected Object evaluate()
                    {
                        getPlayer().acceptConversationInput( message );
                        return null;
                    }
                } );
                // Spigot end
            } else if (this.player.getChatFlags() == EntityHuman.EnumChatVisibility.SYSTEM) { // Re-add "Command Only" flag check
                ChatMessage chatmessage = new ChatMessage("chat.cannotSend");

                chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
                this.sendPacket(new PacketPlayOutChat(chatmessage));
            } else if (true) {
                this.chat(s, true);
                // CraftBukkit end - the below is for reference. :)
            } else {
                ChatMessage chatmessage1 = new ChatMessage("chat.type.text", this.player.getScoreboardDisplayName(), s);

                this.minecraftServer.getPlayerList().sendMessage(chatmessage1, false);
            }

            // Spigot start - spam exclusions
            boolean counted = true;
            for ( String exclude : org.spigotmc.SpigotConfig.spamExclusions )
            {
                if ( exclude != null && s.startsWith( exclude ) )
                {
                    counted = false;
                    break;
                }
            }
            // Spigot end
            // CraftBukkit start - replaced with thread safe throttle
            // this.chatThrottle += 20;
            if (counted && chatSpamField.addAndGet(this, 20) > 200 && SettingsConfig.KICK_FOR_SPAM && !this.minecraftServer.getPlayerList().isOp(this.player.getProfile())) { // Spigot
                if (!isSync) {
                    Waitable waitable = new Waitable() {
                        @Override
                        protected Object evaluate() {
                            PlayerConnection.this.disconnect("You have been kicked for spamming!");
                            return null;
                        }
                    };

                    this.minecraftServer.processQueue.add(waitable);

                    try {
                        waitable.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    this.disconnect("You have been kicked for spamming!");
                }
                // CraftBukkit end
            }

        }
    }

    // CraftBukkit start - add method
    public void chat(String s, boolean async) {
        if (s.isEmpty() || this.player.getChatFlags() == EntityHuman.EnumChatVisibility.HIDDEN) {
            return;
        }

        if (!async && s.startsWith("/")) {
            // PaperSpigot Start
            if (!org.bukkit.Bukkit.isPrimaryThread()) {
                final String fCommandLine = s;
                MinecraftServer.LOGGER.log(org.apache.logging.log4j.Level.ERROR, "Command Dispatched Async: " + fCommandLine);
                MinecraftServer.LOGGER.log(org.apache.logging.log4j.Level.ERROR, "Please notify author of plugin causing this execution to fix this bug! see: http://bit.ly/1oSiM6C", new Throwable());
                Waitable wait = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        chat(fCommandLine, false);
                        return null;
                    }
                };
                minecraftServer.processQueue.add(wait);
                try {
                    wait.get();
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
                } catch (Exception e) {
                    throw new RuntimeException("Exception processing chat command", e.getCause());
                }
            }
            // PaperSpigot End
            this.handleCommand(s);
        } else if (this.player.getChatFlags() == EntityHuman.EnumChatVisibility.SYSTEM) {
            // Do nothing, this is coming from a plugin
        } else {
            Player player = this.getPlayer();
            AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, s, new LazyPlayerSet());
            this.server.getPluginManager().callEvent(event);

            if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0) {
                // Evil plugins still listening to deprecated event
                final PlayerChatEvent queueEvent = new PlayerChatEvent(player, event.getMessage(), event.getFormat(), event.getRecipients());
                queueEvent.setCancelled(event.isCancelled());
                Waitable waitable = new Waitable() {
                    @Override
                    protected Object evaluate() {
                        org.bukkit.Bukkit.getPluginManager().callEvent(queueEvent);

                        if (queueEvent.isCancelled()) {
                            return null;
                        }

                        String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                        PlayerConnection.this.minecraftServer.console.sendMessage(message);
                        if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy()) {
                            for (Object player : PlayerConnection.this.minecraftServer.getPlayerList().players) {
                                ((EntityPlayer) player).sendMessage(CraftChatMessage.fromString(message));
                            }
                        } else {
                            for (Player player : queueEvent.getRecipients()) {
                                player.sendMessage(message);
                            }
                        }
                        return null;
                    }};
                if (async) {
                    minecraftServer.processQueue.add(waitable);
                } else {
                    waitable.run();
                }
                try {
                    waitable.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
                } catch (ExecutionException e) {
                    throw new RuntimeException("Exception processing chat event", e.getCause());
                }
            } else {
                if (event.isCancelled()) {
                    return;
                }

                s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
                minecraftServer.console.sendMessage(s);
                if (((LazyPlayerSet) event.getRecipients()).isLazy()) {
                    for (Object recipient : minecraftServer.getPlayerList().players) {
                        ((EntityPlayer) recipient).sendMessage(CraftChatMessage.fromString(s));
                    }
                } else {
                    for (Player recipient : event.getRecipients()) {
                        recipient.sendMessage(s);
                    }
                }
            }
        }
    }
    // CraftBukkit end

    private void handleCommand(String s) {
        //SpigotTimings.playerCommandTimer.startTiming(); // Spigot // Purpur
        // CraftBukkit start - whole method
        if ( org.spigotmc.SpigotConfig.logCommands ) // Spigot
            c.info(this.player.getName() + " issued server command: " + s);

        CraftPlayer player = this.getPlayer();

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, s, new LazyPlayerSet());
        this.server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            //SpigotTimings.playerCommandTimer.stopTiming(); // Spigot // Purpur
            return;
        }

        try {
            if (this.server.dispatchCommand(event.getPlayer(), event.getMessage().substring(1))) {
                //SpigotTimings.playerCommandTimer.stopTiming(); // Spigot // Purpur
                return;
            }
        } catch (org.bukkit.command.CommandException ex) {
            player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(PlayerConnection.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            //SpigotTimings.playerCommandTimer.stopTiming(); // Spigot // Purpur
            return;
        }
        //SpigotTimings.playerCommandTimer.stopTiming(); // Spigot // Purpur
        // this.minecraftServer.getCommandHandler().a(this.player, s);
        // CraftBukkit end
    }

    public void a(PacketPlayInArmAnimation packetplayinarmanimation) {
        if (this.player.dead) return; // CraftBukkit
        PlayerConnectionUtils.ensureMainThread(packetplayinarmanimation, this, this.player.u());
        this.player.resetIdleTimer();
        // CraftBukkit start - Raytrace to look for 'rogue armswings'
        if (SettingsConfig.CHECK_ROGUE_ARM_SWINGS) {
            float f1 = this.player.pitch;
            float f2 = this.player.yaw;
            double d0 = this.player.locX;
            double d1 = this.player.locY + (double) this.player.getHeadHeight();
            double d2 = this.player.locZ;
            Vec3D vec3d = new Vec3D(d0, d1, d2);

            float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
            float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            float f7 = f4 * f5;
            float f8 = f3 * f5;
            double d3 = player.playerInteractManager.getGameMode() == WorldSettings.EnumGamemode.CREATIVE ? 5.0D : 4.5D;
            Vec3D vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
            MovingObjectPosition movingobjectposition = this.player.world.rayTrace(vec3d, vec3d1, false);

            if (movingobjectposition == null || movingobjectposition.type != MovingObjectPosition.EnumMovingObjectType.BLOCK) {
                CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_AIR, this.player.inventory.getItemInHand());
            }
        }
        // CraftBukkit end

        // cSpigot start - Optimize Arm Swing
        // Arm swing animation
        if (SettingsConfig.TOGGLE_ARM_ANIMATION_EVENT) {
            PlayerAnimationEvent event = new PlayerAnimationEvent(this.getPlayer());
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled()) return;
        }

        // Only send swings to client every 5 ticks (current tick + 5)
        if (SettingsConfig.OPTIMIZE_ARM_SWINGS && (MinecraftServer.currentTick >= this.nextValidSwingTick)) {
            this.player.bw();
            this.nextValidSwingTick = MinecraftServer.currentTick + 5;
        } else {
            this.player.bw();
        }

        // stop blocking after swinging in case the state was wrong
        if (this.player.isBlocking() && SettingsConfig.BLOCK_HIT_ARM_ANIMATION_FIX) {
            this.player.bU();
        }
        // cSpigot end
    }

    public void a(PacketPlayInEntityAction packetplayinentityaction) {
        PlayerConnectionUtils.ensureMainThread(packetplayinentityaction, this, this.player.u());
        // CraftBukkit start
        if (this.player.dead) return;
        switch (packetplayinentityaction.b()) {
            case START_SNEAKING:
            case STOP_SNEAKING:
                PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getPlayer(), packetplayinentityaction.b() == PacketPlayInEntityAction.EnumPlayerAction.START_SNEAKING);
                this.server.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }
                break;
            case START_SPRINTING:
            case STOP_SPRINTING:
                PlayerToggleSprintEvent e2 = new PlayerToggleSprintEvent(this.getPlayer(), packetplayinentityaction.b() == PacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING);
                this.server.getPluginManager().callEvent(e2);

                if (e2.isCancelled()) {
                    return;
                }
                break;
        }
        // CraftBukkit end

        this.player.resetIdleTimer();

        switch (packetplayinentityaction.b()) {
            case START_SNEAKING:
                this.player.setSneaking(true);
                break;

            case STOP_SNEAKING:
                this.player.setSneaking(false);
                break;

            case START_SPRINTING:
                this.player.setSprinting(true);

                // cSpigot start - Fix the sneak sprinting glitch lmao
                if (SettingsConfig.SNEAK_SPRINT_FIX) {
                    this.player.setSneaking(false);
                }

                // Fix Block hit glitch
                if (SettingsConfig.SHOULD_UNBLOCK_ON_RUN && this.player.shouldUnblock()) {
                    this.player.bU();
                }
                // cSpigot end

                break;

            case STOP_SPRINTING:
                this.player.setSprinting(false);
                break;

            case STOP_SLEEPING:
                this.player.a(false, true, true);
                // this.checkMovement = false; // CraftBukkit - this is handled in teleport
                break;

            case RIDING_JUMP:
                if (this.player.vehicle instanceof EntityHorse) {
                    ((EntityHorse) this.player.vehicle).v(packetplayinentityaction.c());
                }
                break;

            case OPEN_INVENTORY:
                if (this.player.vehicle instanceof EntityHorse) {
                    ((EntityHorse) this.player.vehicle).g(this.player);
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid client command!");
        }

    }

    public void a(PacketPlayInUseEntity packetplayinuseentity) {
        if (this.player.dead) return; // CraftBukkit
        PlayerConnectionUtils.ensureMainThread(packetplayinuseentity, this, this.player.u());
        WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
        Entity entity = packetplayinuseentity.a(worldserver);
        // Spigot Start
        if ( entity == player && !player.isSpectator() )
        {
            disconnect( "Cannot interact with self!" );
            return;
        }
        // Spigot End

        this.player.resetIdleTimer();
        if (entity != null) {
            // KigPaper start - allow hitting the vehicle when hitboxes collide / allow the player to hit others if they are being ridden by something
            if (SettingsConfig.BETTER_VEHICLE_HIT_BOXES) {
                if (entity instanceof EntityPlayer && entity.vehicle != null && !((EntityPlayer) entity).collidesWithEntities) {
                    // Allow hitting the vehicle when hitboxes collide
                    // Ray trace to see if the player should hit the vehicle instead
                    double maxDist = 3.0;
                    Vec3D look = player.ap();
                    Vec3D eyePos = new Vec3D(player.locX, player.locY + player.getBukkitEntity().getEyeHeight(), player.locZ);
                    Vec3D maxDistLook = eyePos.add(look.a * maxDist, look.b * maxDist, look.c * maxDist);
                    Entity vehicle = entity.vehicle;
                    List<Entity> collisions = worldserver.a(player, player.getBoundingBox()
                            .a(look.a * maxDist, look.b * maxDist, look.c * maxDist) // add coords
                            .grow(1D, 1D, 1D), e -> e == vehicle); // Only check for the vehicle
                    for (Entity match : collisions) {
                        AxisAlignedBB expandedHitbox = match.getBoundingBox().grow(0.1D, 0.1D, 0.1D); // Hit box is collision box + 0.1
                        if (expandedHitbox.a(eyePos) || expandedHitbox.a(eyePos, maxDistLook) != null) {
                            entity = match;
                            break;
                        }
                    }
                } else if (entity == player.passenger) { // player hit passenger
                    // Allow the player to hit others if they are being ridden by something
                    // Ray trace to see if the player should hit someone else instead
                    double maxDist = 3.0;
                    Vec3D look = player.ap();
                    Vec3D eyePos = new Vec3D(player.locX, player.locY + player.getBukkitEntity().getEyeHeight(), player.locZ);
                    Vec3D maxDistLook = eyePos.add(look.a * maxDist, look.b * maxDist, look.c * maxDist);
                    Entity passenger = entity.passenger;
                    List<Entity> collisions = worldserver.a(player, player.getBoundingBox()
                            .a(look.a * maxDist, look.b * maxDist, look.c * maxDist) // add coords
                            .grow(1D, 1D, 1D), e -> e != passenger); // Don't include passenger
                    for (Entity match : collisions) {
                        AxisAlignedBB expandedHitbox = match.getBoundingBox().grow(0.1D, 0.1D, 0.1D); // Hit box is collision box + 0.1
                        if (expandedHitbox.a(eyePos) || expandedHitbox.a(eyePos, maxDistLook) != null) {
                            entity = match;
                            break;
                        }
                    }
                }
            }
            // KigPaper end
            boolean flag = SettingsConfig.DYNAMIC_HIT_REGISTRY || this.player.hasLineOfSight(entity);
            double d0 = 36.0D;

            if (!flag) {
                d0 = SettingsConfig.ALTERNATE_HIT_DETECTION ? 12.75 : 9.0;
            }

            if (this.player.h(entity) < d0) {
                ItemStack itemInHand = this.player.inventory.getItemInHand(); // CraftBukkit

                if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT
                        || packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
                    // CraftBukkit start
                    boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD
                            && entity instanceof EntityInsentient;
                    Item origItem = this.player.inventory.getItemInHand() == null ? null
                            : this.player.inventory.getItemInHand().getItem();
                    PlayerInteractEntityEvent event;
                    if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
                        event = new PlayerInteractEntityEvent(this.getPlayer(), entity.getBukkitEntity());
                    } else {
                        Vec3D target = packetplayinuseentity.b();
                        event = new PlayerInteractAtEntityEvent(this.getPlayer(), entity.getBukkitEntity(),
                                new org.bukkit.util.Vector(target.a, target.b, target.c));
                    }
                    this.server.getPluginManager().callEvent(event);

                    if (triggerLeashUpdate && (event.isCancelled() || this.player.inventory.getItemInHand() == null
                            || this.player.inventory.getItemInHand().getItem() != Items.LEAD)) {
                        // Refresh the current leash state
                        this.sendPacket(
                                new PacketPlayOutAttachEntity(1, entity, ((EntityInsentient) entity).getLeashHolder()));
                    }

                    if (event.isCancelled() || this.player.inventory.getItemInHand() == null || this.player.inventory.getItemInHand().getItem() != origItem) {
                        // Refresh the current entity metadata
                        entity.tracker.broadcast(new PacketPlayOutEntityMetadata(entity.getId(), entity.datawatcher, true)); // Paper - update entity for all players
                    }

                    if (event.isCancelled()) {
                        this.player.updateInventory(this.player.activeContainer); // Paper - Refresh player inventory
                        return;
                    }
                    // CraftBukkit end
                }
                if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
                    this.player.u(entity);

                    // CraftBukkit start
                    if (itemInHand != null && itemInHand.count <= -1) {
                        this.player.updateInventory(this.player.activeContainer);
                    }
                    // CraftBukkit end
                } else if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT_AT) {
                    entity.a(this.player, packetplayinuseentity.b());

                    // CraftBukkit start
                    if (itemInHand != null && itemInHand.count <= -1) {
                        this.player.updateInventory(this.player.activeContainer);
                    }
                    // CraftBukkit end
                } else if (packetplayinuseentity.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
                    if (entity instanceof EntityItem || entity instanceof EntityExperienceOrb || entity instanceof EntityArrow || (entity == this.player && !player.isSpectator())) { // CraftBukkit
                        this.disconnect("Attempting to attack an invalid entity");
                        this.minecraftServer.warning("Player " + this.player.getName() + " tried to attack an invalid entity");
                        return;
                    }

                    this.player.attack(entity);

                    // cSpigot start - Fix Block hit glitch
                    if (SettingsConfig.BLOCK_HIT_FIX && this.player.isBlocking()) {
                        this.player.bU();
                    }
                    // cSpigot end

                    // CraftBukkit start
                    if (itemInHand != null && itemInHand.count <= -1) {
                        this.player.updateInventory(this.player.activeContainer);
                    }
                    // CraftBukkit end
                }
            }
        }

    }

    public void a(PacketPlayInClientCommand packetplayinclientcommand) {
        PlayerConnectionUtils.ensureMainThread(packetplayinclientcommand, this, this.player.u());
        this.player.resetIdleTimer();
        PacketPlayInClientCommand.EnumClientCommand packetplayinclientcommand_enumclientcommand = packetplayinclientcommand.a();

        switch (SyntheticClass_1.c[packetplayinclientcommand_enumclientcommand.ordinal()]) {
            case 1:
                if (this.player.viewingCredits) {
                    // this.player = this.minecraftServer.getPlayerList().moveToWorld(this.player, 0, true);
                    this.minecraftServer.getPlayerList().changeDimension(this.player, 0, PlayerTeleportEvent.TeleportCause.END_PORTAL); // CraftBukkit - reroute logic through custom portal management
                } else if (this.player.u().getWorldData().isHardcore()) {
//                    if (this.minecraftServer.T() && this.player.getName().equals(this.minecraftServer.S())) {
//                        this.player.playerConnection.disconnect("You have died. Game over, man, it\'s game over!");
//                        this.minecraftServer.aa();
//                    } else {
                    GameProfileBanEntry gameprofilebanentry = new GameProfileBanEntry(this.player.getProfile(), (Date) null, "(You just lost the game)", (Date) null, "Death in Hardcore");

                    this.minecraftServer.getPlayerList().getProfileBans().add(gameprofilebanentry);
                    this.player.playerConnection.disconnect("You have died. Game over, man, it\'s game over!");

                } else {
                    if (this.player.getHealth() > 0.0F) {
                        return;
                    }

                    this.player = this.minecraftServer.getPlayerList().moveToWorld(this.player, 0, false);
                }
                break;

            case 2:
                this.player.getStatisticManager().a(this.player);
                break;

            case 3:
                this.player.b((Statistic) AchievementList.f);
        }

    }

    public void a(PacketPlayInCloseWindow packetplayinclosewindow) {
        if (this.player.dead) return; // CraftBukkit
        PlayerConnectionUtils.ensureMainThread(packetplayinclosewindow, this, this.player.u());

        // cSpigot start - only fire InventoryCloseEvent if inventory is open
        if (!SettingsConfig.INVENTORY_CLOSE_PATCH || packetplayinclosewindow.getId() == player.activeContainer.windowId) {
            CraftEventFactory.handleInventoryCloseEvent(this.player); // CraftBukkit
            this.player.p();
        } else {
            this.player.p();
        }
        // cSpigot end
    }

    public void a(PacketPlayInWindowClick packetplayinwindowclick) {
        if (this.player.dead) return; // CraftBukkit
        PlayerConnectionUtils.ensureMainThread(packetplayinwindowclick, this, this.player.u());
        this.player.resetIdleTimer();

        if (this.player.activeContainer.windowId == packetplayinwindowclick.a() && this.player.activeContainer.c(this.player)) {
            boolean cancelled = this.player.isSpectator(); // CraftBukkit - see below if
            if (false) { // this.player.isSpectator()) {
                ArrayList arraylist = Lists.newArrayList();

                for (int i = 0; i < this.player.activeContainer.c.size(); ++i) {
                    arraylist.add(((Slot) this.player.activeContainer.c.get(i)).getItem());
                }

                this.player.a(this.player.activeContainer, (List) arraylist);
            } else {
                // ItemStack itemstack = this.player.activeContainer.clickItem(packetplayinwindowclick.b(), packetplayinwindowclick.c(), packetplayinwindowclick.f(), this.player);
                // CraftBukkit start - Call InventoryClickEvent
                if (packetplayinwindowclick.b() < -1 && packetplayinwindowclick.b() != -999) {
                    return;
                }

                InventoryView inventory = this.player.activeContainer.getBukkitView();
                SlotType type = CraftInventoryView.getSlotType(inventory, packetplayinwindowclick.b());

                InventoryClickEvent event = null;
                ClickType click = ClickType.UNKNOWN;
                InventoryAction action = InventoryAction.UNKNOWN;

                ItemStack itemstack = null;

                if (packetplayinwindowclick.b() == -1) {
                    type = SlotType.OUTSIDE; // override
                    click = packetplayinwindowclick.c() == 0 ? ClickType.WINDOW_BORDER_LEFT : ClickType.WINDOW_BORDER_RIGHT;
                    action = InventoryAction.NOTHING;
                } else if (packetplayinwindowclick.f() == 0) {
                    if (packetplayinwindowclick.c() == 0) {
                        click = ClickType.LEFT;
                    } else if (packetplayinwindowclick.c() == 1) {
                        click = ClickType.RIGHT;
                    }
                    if (packetplayinwindowclick.c() == 0 || packetplayinwindowclick.c() == 1) {
                        action = InventoryAction.NOTHING; // Don't want to repeat ourselves
                        if (packetplayinwindowclick.b() == -999) {
                            if (player.inventory.getCarried() != null) {
                                action = packetplayinwindowclick.c() == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
                            }
                        } else {
                            Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());
                            if (slot != null) {
                                ItemStack clickedItem = slot.getItem();
                                ItemStack cursor = player.inventory.getCarried();
                                if (clickedItem == null) {
                                    if (cursor != null) {
                                        action = packetplayinwindowclick.c() == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
                                    }
                                } else if (slot.isAllowed(player)) {
                                    if (cursor == null) {
                                        action = packetplayinwindowclick.c() == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
                                    } else if (slot.isAllowed(cursor)) {
                                        if (clickedItem.doMaterialsMatch(cursor) && ItemStack.equals(clickedItem, cursor)) {
                                            int toPlace = packetplayinwindowclick.c() == 0 ? cursor.count : 1;
                                            toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.count);
                                            toPlace = Math.min(toPlace, slot.inventory.getMaxStackSize() - clickedItem.count);
                                            if (toPlace == 1) {
                                                action = InventoryAction.PLACE_ONE;
                                            } else if (toPlace == cursor.count) {
                                                action = InventoryAction.PLACE_ALL;
                                            } else if (toPlace < 0) {
                                                action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE; // this happens with oversized stacks
                                            } else if (toPlace != 0) {
                                                action = InventoryAction.PLACE_SOME;
                                            }
                                        } else if (cursor.count <= slot.getMaxStackSize()) {
                                            action = InventoryAction.SWAP_WITH_CURSOR;
                                        }
                                    } else if (cursor.getItem() == clickedItem.getItem() && (!cursor.usesData() || cursor.getData() == clickedItem.getData()) && ItemStack.equals(cursor, clickedItem)) {
                                        if (clickedItem.count >= 0) {
                                            if (clickedItem.count + cursor.count <= cursor.getMaxStackSize()) {
                                                // As of 1.5, this is result slots only
                                                action = InventoryAction.PICKUP_ALL;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (packetplayinwindowclick.f() == 1) {
                    if (packetplayinwindowclick.c() == 0) {
                        click = ClickType.SHIFT_LEFT;
                    } else if (packetplayinwindowclick.c() == 1) {
                        click = ClickType.SHIFT_RIGHT;
                    }
                    if (packetplayinwindowclick.c() == 0 || packetplayinwindowclick.c() == 1) {
                        if (packetplayinwindowclick.b() < 0) {
                            action = InventoryAction.NOTHING;
                        } else {
                            Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());
                            if (slot != null && slot.isAllowed(this.player) && slot.hasItem()) {
                                action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        }
                    }
                } else if (packetplayinwindowclick.f() == 2) {
                    if (packetplayinwindowclick.c() >= 0 && packetplayinwindowclick.c() < 9) {
                        click = ClickType.NUMBER_KEY;
                        Slot clickedSlot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());
                        if (clickedSlot.isAllowed(player)) {
                            ItemStack hotbar = this.player.inventory.getItem(packetplayinwindowclick.c());
                            boolean canCleanSwap = hotbar == null || (clickedSlot.inventory == player.inventory && clickedSlot.isAllowed(hotbar)); // the slot will accept the hotbar item
                            if (clickedSlot.hasItem()) {
                                if (canCleanSwap) {
                                    action = InventoryAction.HOTBAR_SWAP;
                                } else {
                                    int firstEmptySlot = player.inventory.getFirstEmptySlotIndex();
                                    if (firstEmptySlot > -1) {
                                        action = InventoryAction.HOTBAR_MOVE_AND_READD;
                                    } else {
                                        action = InventoryAction.NOTHING; // This is not sane! Mojang: You should test for other slots of same type
                                    }
                                }
                            } else if (!clickedSlot.hasItem() && hotbar != null && clickedSlot.isAllowed(hotbar)) {
                                action = InventoryAction.HOTBAR_SWAP;
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        } else {
                            action = InventoryAction.NOTHING;
                        }
                        // Special constructor for number key
                        event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.b(), click, action, packetplayinwindowclick.c());
                    }
                } else if (packetplayinwindowclick.f() == 3) {
                    if (packetplayinwindowclick.c() == 2) {
                        click = ClickType.MIDDLE;
                        if (packetplayinwindowclick.b() == -999) {
                            action = InventoryAction.NOTHING;
                        } else {
                            Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());
                            if (slot != null && slot.hasItem() && player.abilities.canInstantlyBuild && player.inventory.getCarried() == null) {
                                action = InventoryAction.CLONE_STACK;
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        }
                    } else {
                        click = ClickType.UNKNOWN;
                        action = InventoryAction.UNKNOWN;
                    }
                } else if (packetplayinwindowclick.f() == 4) {
                    if (packetplayinwindowclick.b() >= 0) {
                        if (packetplayinwindowclick.c() == 0) {
                            click = ClickType.DROP;
                            Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());
                            if (slot != null && slot.hasItem() && slot.isAllowed(player) && slot.getItem() != null && slot.getItem().getItem() != Item.getItemOf(Blocks.AIR)) {
                                action = InventoryAction.DROP_ONE_SLOT;
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        } else if (packetplayinwindowclick.c() == 1) {
                            click = ClickType.CONTROL_DROP;
                            Slot slot = this.player.activeContainer.getSlot(packetplayinwindowclick.b());
                            if (slot != null && slot.hasItem() && slot.isAllowed(player) && slot.getItem() != null && slot.getItem().getItem() != Item.getItemOf(Blocks.AIR)) {
                                action = InventoryAction.DROP_ALL_SLOT;
                            } else {
                                action = InventoryAction.NOTHING;
                            }
                        }
                    } else {
                        // Sane default (because this happens when they are holding nothing. Don't ask why.)
                        click = ClickType.LEFT;
                        if (packetplayinwindowclick.c() == 1) {
                            click = ClickType.RIGHT;
                        }
                        action = InventoryAction.NOTHING;
                    }
                } else if (packetplayinwindowclick.f() == 5) {
                    itemstack = this.player.activeContainer.clickItem(packetplayinwindowclick.b(), packetplayinwindowclick.c(), 5, this.player);
                } else if (packetplayinwindowclick.f() == 6) {
                    click = ClickType.DOUBLE_CLICK;
                    action = InventoryAction.NOTHING;
                    if (packetplayinwindowclick.b() >= 0 && this.player.inventory.getCarried() != null) {
                        ItemStack cursor = this.player.inventory.getCarried();
                        action = InventoryAction.NOTHING;
                        // Quick check for if we have any of the item
                        if (inventory.getTopInventory().contains(org.bukkit.Material.getMaterial(Item.getId(cursor.getItem()))) || inventory.getBottomInventory().contains(org.bukkit.Material.getMaterial(Item.getId(cursor.getItem())))) {
                            action = InventoryAction.COLLECT_TO_CURSOR;
                        }
                    }
                }
                // TODO check on updates

                if (packetplayinwindowclick.f() != 5) {
                    if (click == ClickType.NUMBER_KEY) {
                        event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.b(), click, action, packetplayinwindowclick.c());
                    } else {
                        event = new InventoryClickEvent(inventory, type, packetplayinwindowclick.b(), click, action);
                    }

                    org.bukkit.inventory.Inventory top = inventory.getTopInventory();
                    if (packetplayinwindowclick.b() == 0 && top instanceof CraftingInventory) {
                        org.bukkit.inventory.Recipe recipe = ((CraftingInventory) top).getRecipe();
                        if (recipe != null) {
                            if (click == ClickType.NUMBER_KEY) {
                                event = new CraftItemEvent(recipe, inventory, type, packetplayinwindowclick.b(), click, action, packetplayinwindowclick.c());
                            } else {
                                event = new CraftItemEvent(recipe, inventory, type, packetplayinwindowclick.b(), click, action);
                            }
                        }
                    }

                    event.setCancelled(cancelled);
                    server.getPluginManager().callEvent(event);

                    switch (event.getResult()) {
                        case ALLOW:
                        case DEFAULT:
                            itemstack = this.player.activeContainer.clickItem(packetplayinwindowclick.b(), packetplayinwindowclick.c(), packetplayinwindowclick.f(), this.player);
                            // PaperSpigot start - Stackable Buckets
                            if (itemstack != null &&
                                    ((itemstack.getItem() == Items.LAVA_BUCKET && PaperSpigotConfig.stackableLavaBuckets) ||
                                            (itemstack.getItem() == Items.WATER_BUCKET && PaperSpigotConfig.stackableWaterBuckets) ||
                                            (itemstack.getItem() == Items.MILK_BUCKET && PaperSpigotConfig.stackableMilkBuckets))) {
                                if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                                    this.player.updateInventory(this.player.activeContainer);
                                } else {
                                    this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventory.getCarried()));
                                    this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, packetplayinwindowclick.b(), this.player.activeContainer.getSlot(packetplayinwindowclick.b()).getItem()));
                                }
                            }
                            // PaperSpigot end
                            break;
                        case DENY:
                            /* Needs enum constructor in InventoryAction
                            if (action.modifiesOtherSlots()) {

                            } else {
                                if (action.modifiesCursor()) {
                                    this.player.playerConnection.sendPacket(new Packet103SetSlot(-1, -1, this.player.inventory.getCarried()));
                                }
                                if (action.modifiesClicked()) {
                                    this.player.playerConnection.sendPacket(new Packet103SetSlot(this.player.activeContainer.windowId, packet102windowclick.slot, this.player.activeContainer.getSlot(packet102windowclick.slot).getItem()));
                                }
                            }*/
                            switch (action) {
                                // Modified other slots
                                case PICKUP_ALL:
                                case MOVE_TO_OTHER_INVENTORY:
                                case HOTBAR_MOVE_AND_READD:
                                case HOTBAR_SWAP:
                                case COLLECT_TO_CURSOR:
                                case UNKNOWN:
                                    this.player.updateInventory(this.player.activeContainer);
                                    break;
                                // Modified cursor and clicked
                                case PICKUP_SOME:
                                case PICKUP_HALF:
                                case PICKUP_ONE:
                                case PLACE_ALL:
                                case PLACE_SOME:
                                case PLACE_ONE:
                                case SWAP_WITH_CURSOR:
                                    this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventory.getCarried()));
                                    this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, packetplayinwindowclick.b(), this.player.activeContainer.getSlot(packetplayinwindowclick.b()).getItem()));
                                    break;
                                // Modified clicked only
                                case DROP_ALL_SLOT:
                                case DROP_ONE_SLOT:
                                    this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.activeContainer.windowId, packetplayinwindowclick.b(), this.player.activeContainer.getSlot(packetplayinwindowclick.b()).getItem()));
                                    break;
                                // Modified cursor only
                                case DROP_ALL_CURSOR:
                                case DROP_ONE_CURSOR:
                                case CLONE_STACK:
                                    this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, this.player.inventory.getCarried()));
                                    break;
                                // Nothing
                                case NOTHING:
                                    break;
                            }
                            return;
                    }

                    if (event instanceof CraftItemEvent) {
                        // Need to update the inventory on crafting to
                        // correctly support custom recipes
                        player.updateInventory(player.activeContainer);
                    }
                }
                // CraftBukkit end

                if (ItemStack.matches(packetplayinwindowclick.e(), itemstack)) {
                    this.player.playerConnection.sendPacket(new PacketPlayOutTransaction(packetplayinwindowclick.a(), packetplayinwindowclick.d(), true));
                    this.player.g = true;
                    this.player.activeContainer.b();
                    this.player.broadcastCarriedItem();
                    this.player.g = false;
                } else {
                    this.n.a(this.player.activeContainer.windowId, Short.valueOf(packetplayinwindowclick.d()));
                    this.player.playerConnection.sendPacket(new PacketPlayOutTransaction(packetplayinwindowclick.a(), packetplayinwindowclick.d(), false));
                    this.player.activeContainer.a(this.player, false);
                    ArrayList arraylist1 = Lists.newArrayList();

                    for (int j = 0; j < this.player.activeContainer.c.size(); ++j) {
                        arraylist1.add(((Slot) this.player.activeContainer.c.get(j)).getItem());
                    }

                    this.player.a(this.player.activeContainer, (List) arraylist1);
                }
            }
        }

    }

    public void a(PacketPlayInEnchantItem packetplayinenchantitem) {
        PlayerConnectionUtils.ensureMainThread(packetplayinenchantitem, this, this.player.u());
        this.player.resetIdleTimer();
        if (this.player.activeContainer.windowId == packetplayinenchantitem.a() && this.player.activeContainer.c(this.player) && !this.player.isSpectator()) {
            this.player.activeContainer.a(this.player, packetplayinenchantitem.b());
            this.player.activeContainer.b();
        }

    }

    public void a(PacketPlayInSetCreativeSlot packetplayinsetcreativeslot) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsetcreativeslot, this, this.player.u());
        if (this.player.playerInteractManager.isCreative()) {
            boolean flag = packetplayinsetcreativeslot.a() < 0;
            ItemStack itemstack = packetplayinsetcreativeslot.getItemStack();

            if (itemstack != null && itemstack.hasTag() && itemstack.getTag().hasKeyOfType("BlockEntityTag", 10)) {
                NBTTagCompound nbttagcompound = itemstack.getTag().getCompound("BlockEntityTag");

                if (nbttagcompound.hasKey("x") && nbttagcompound.hasKey("y") && nbttagcompound.hasKey("z")) {
                    BlockPosition blockposition = new BlockPosition(nbttagcompound.getInt("x"), nbttagcompound.getInt("y"), nbttagcompound.getInt("z"));
                    TileEntity tileentity = this.player.world.getTileEntity(blockposition);

                    if (tileentity != null) {
                        NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                        tileentity.b(nbttagcompound1);
                        nbttagcompound1.remove("x");
                        nbttagcompound1.remove("y");
                        nbttagcompound1.remove("z");
                        itemstack.a("BlockEntityTag", (NBTBase) nbttagcompound1);
                    }
                }
            }

            boolean flag1 = packetplayinsetcreativeslot.a() >= 1 && packetplayinsetcreativeslot.a() < 36 + PlayerInventory.getHotbarSize();
            // CraftBukkit - Add invalidItems check
            boolean flag2 = itemstack == null || itemstack.getItem() != null && (!invalidItems.contains(Item.getId(itemstack.getItem())) || !org.spigotmc.SpigotConfig.filterCreativeItems); // Spigot
            boolean flag3 = itemstack == null || itemstack.getData() >= 0 && itemstack.count <= 64 && itemstack.count > 0;
            // CraftBukkit start - Call click event
            if (flag || (flag1 && !ItemStack.matches(this.player.defaultContainer.getSlot(packetplayinsetcreativeslot.a()).getItem(), packetplayinsetcreativeslot.getItemStack()))) { // Insist on valid slot

                org.bukkit.entity.HumanEntity player = this.player.getBukkitEntity();
                InventoryView inventory = new CraftInventoryView(player, player.getInventory(), this.player.defaultContainer);
                org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(packetplayinsetcreativeslot.getItemStack());

                SlotType type = SlotType.QUICKBAR;
                if (flag) {
                    type = SlotType.OUTSIDE;
                } else if (packetplayinsetcreativeslot.a() < 36) {
                    if (packetplayinsetcreativeslot.a() >= 5 && packetplayinsetcreativeslot.a() < 9) {
                        type = SlotType.ARMOR;
                    } else {
                        type = SlotType.CONTAINER;
                    }
                }
                InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : packetplayinsetcreativeslot.a(), item);
                server.getPluginManager().callEvent(event);

                itemstack = CraftItemStack.asNMSCopy(event.getCursor());

                switch (event.getResult()) {
                    case ALLOW:
                        // Plugin cleared the id / stacksize checks
                        flag2 = flag3 = true;
                        break;
                    case DEFAULT:
                        break;
                    case DENY:
                        // Reset the slot
                        if (packetplayinsetcreativeslot.a() >= 0) {
                            this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(this.player.defaultContainer.windowId, packetplayinsetcreativeslot.a(), this.player.defaultContainer.getSlot(packetplayinsetcreativeslot.a()).getItem()));
                            this.player.playerConnection.sendPacket(new PacketPlayOutSetSlot(-1, -1, null));
                        }
                        return;
                }
            }
            // CraftBukkit end

            if (flag1 && flag2 && flag3) {
                if (itemstack == null) {
                    this.player.defaultContainer.setItem(packetplayinsetcreativeslot.a(), (ItemStack) null);
                } else {
                    this.player.defaultContainer.setItem(packetplayinsetcreativeslot.a(), itemstack);
                }

                this.player.defaultContainer.a(this.player, true);
            } else if (flag && flag2 && flag3 && this.m < 200) {
                this.m += 20;
                EntityItem entityitem = this.player.drop(itemstack, true);

                if (entityitem != null) {
                    entityitem.j();
                }
            }
        }

    }

    public void a(PacketPlayInTransaction packetplayintransaction) {
        if (this.player.dead) return; // CraftBukkit
        PlayerConnectionUtils.ensureMainThread(packetplayintransaction, this, this.player.u());
        Short oshort = (Short) this.n.get(this.player.activeContainer.windowId);

        if (oshort != null && packetplayintransaction.b() == oshort.shortValue() && this.player.activeContainer.windowId == packetplayintransaction.a() && !this.player.activeContainer.c(this.player) && !this.player.isSpectator()) {
            this.player.activeContainer.a(this.player, true);
        }

    }

    public void a(PacketPlayInUpdateSign packetplayinupdatesign) {
        if (this.player.dead) return; // CraftBukkit
        if (packetplayinupdatesign.b().length > 4) return; // cSpigot: Prevent excessive lines, vanilla max is 4.
        PlayerConnectionUtils.ensureMainThread(packetplayinupdatesign, this, this.player.u());
        this.player.resetIdleTimer();
        WorldServer worldserver = this.minecraftServer.getWorldServer(this.player.dimension);
        BlockPosition blockposition = packetplayinupdatesign.a();

        if (worldserver.isLoaded(blockposition)) {
            TileEntity tileentity = worldserver.getTileEntity(blockposition);

            if (!(tileentity instanceof TileEntitySign)) {
                return;
            }

            TileEntitySign tileentitysign = (TileEntitySign) tileentity;

            if (!tileentitysign.b() || tileentitysign.c() != this.player) {
                this.minecraftServer.warning("Player " + this.player.getName() + " just tried to change non-editable sign");
                this.sendPacket(new PacketPlayOutUpdateSign(tileentity.world, packetplayinupdatesign.a(), tileentitysign.lines)); // CraftBukkit
                return;
            }

            IChatBaseComponent[] aichatbasecomponent = packetplayinupdatesign.b();

            // CraftBukkit start
            Player player = this.server.getPlayer(this.player);
            int x = packetplayinupdatesign.a().getX();
            int y = packetplayinupdatesign.a().getY();
            int z = packetplayinupdatesign.a().getZ();
            String[] lines = new String[4];

            for (int i = 0; i < aichatbasecomponent.length; ++i) {
                // PandaSpigot start - cap line length - modified clients can send longer data than normal
                String line = aichatbasecomponent[i].c();
                if (line.length() > TileEntitySign.MAX_SIGN_LINE_LENGTH && TileEntitySign.MAX_SIGN_LINE_LENGTH > 0) {
                    int offset = line.codePoints().limit(TileEntitySign.MAX_SIGN_LINE_LENGTH).map(Character::charCount).sum();
                    if (line.length() > offset) {
                        line = line.substring(0, offset);
                    }
                }
                lines[i] = EnumChatFormat.a(line);
                // PandaSpigot end
            }

            SignChangeEvent event = new SignChangeEvent(player.getWorld().getBlockAt(x, y, z), this.server.getPlayer(this.player), lines);
            this.server.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                System.arraycopy(CraftSign.sanitizeLines(event.getLines()), 0, tileentitysign.lines, 0, 4);
                tileentitysign.isEditable = false;
            }
            // CraftBukkit end

            tileentitysign.update();
            worldserver.notify(blockposition);
        }

    }


    private long d() {
        return System.nanoTime() / 1000000L;
    }

    public void a(PacketPlayInAbilities packetplayinabilities) {
        PlayerConnectionUtils.ensureMainThread(packetplayinabilities, this, this.player.u());
        // CraftBukkit start
        if (this.player.abilities.canFly && this.player.abilities.isFlying != packetplayinabilities.isFlying()) {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.server.getPlayer(this.player), packetplayinabilities.isFlying());
            this.server.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.player.abilities.isFlying = packetplayinabilities.isFlying(); // Actually set the player's flying status
            } else {
                this.player.updateAbilities(); // Tell the player their ability was reverted
            }
        }
        // CraftBukkit end
    }

    public void a(PacketPlayInTabComplete packetplayintabcomplete) {
        // CraftBukkit start
        if (chatSpamField.addAndGet(this, 10) > 500 && SettingsConfig.KICK_FOR_SPAM  && !this.minecraftServer.getPlayerList().isOp(this.player.getProfile())) {
            minecraftServer.postToMainThread(() -> this.disconnect("disconnect.spam")); // Paper
            return;
        }
        // CraftBukkit end

        // cSpigot start - Backport modern tab completion API
        List<String> completions = new ArrayList<>();
        String buffer = packetplayintabcomplete.a();
        Location location = packetplayintabcomplete.b() == null ? null : MCUtil.toLocation(player.getWorld(), packetplayintabcomplete.b());

        AsyncTabCompleteEvent event = new AsyncTabCompleteEvent(this.getPlayer(), completions, buffer, buffer.startsWith("/"), location);
        boolean cancelled = !event.callEvent();

        // If the event isn't handled, we can assume that we have no completions, and so we'll ask the server
        if (!event.isHandled() && !cancelled) {
            completions = AsyncUtil.supplyNextTick(() -> this.minecraftServer.tabCompleteCommand(this.player, packetplayintabcomplete.a(), packetplayintabcomplete.b()));
            if (completions == null) {
                completions = ImmutableList.of();
            }
        } else if (!completions.isEmpty()) {
            completions = event.getCompletions();
        }
        this.player.playerConnection.sendPacket(new PacketPlayOutTabComplete(completions.toArray(new String[0])));
        // cSpigot end
    }


    // Paper start
    private boolean validateBook(ItemStack testStack) {
        NBTTagList pageList = testStack.getTag().getList("pages", 8);
        long byteTotal = 0;
        int maxBookPageSize = SettingsConfig.MAX_BOOK_PAGE_SIZE;
        double multiplier = Math.max(0.3D, Math.min(1D, SettingsConfig.MAX_BOOK_TOTAL_SIZE_MULTIPLIER));
        long byteAllowed = maxBookPageSize;
        for (int i = 0; i < pageList.size(); ++i) {
            String testString = pageList.getString(i);
            int byteLength = testString.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            byteTotal += byteLength;

            int length = testString.length();
            int multibytes = 0;
            if (length != byteLength) {
                for (char c : testString.toCharArray()) {
                    if (c > 127) {
                        multibytes++;
                    }
                }
            }
            byteAllowed += (maxBookPageSize * Math.min(1, Math.max(0.1D, (double) length / 255D))) * multiplier;

            if (multibytes > 1) {
                // penalize MB
                byteAllowed -= multibytes;
            }
        }

        if (byteTotal > byteAllowed) {
            PlayerConnection.c.warn(this.player.getName() + " tried to send too large of a book. Book Size: " + byteTotal + " - Allowed:  "+ byteAllowed + " - Pages: " + pageList.size());
            minecraftServer.postToMainThread(() -> this.disconnect("Book too large!"));
            return false;
        }

        return true;
    }
    // Paper end

    public void a(PacketPlayInSettings packetplayinsettings) {
        PlayerConnectionUtils.ensureMainThread(packetplayinsettings, this, this.player.u());
        this.player.a(packetplayinsettings);
    }

    public void a(PacketPlayInCustomPayload packetplayincustompayload) {
        PlayerConnectionUtils.ensureMainThread(packetplayincustompayload, this, this.player.u());
        if (isExploiter) {
            return; // cSpigot - ignore if they have exploited
        }

        PacketDataSerializer packetdataserializer;
        ItemStack itemstack;
        ItemStack itemstack1;

        try { // CraftBukkit
            try {
                String channelName = packetplayincustompayload.a();
                if ("MC|BEdit".equals(channelName) || "MC|BSign".equals(channelName)) {
                    if (this.lastCustomPayloadPacketTS == -1L
                            || System.currentTimeMillis() - this.lastCustomPayloadPacketTS > 100) {
                        this.lastCustomPayloadPacketTS = System.currentTimeMillis();
                    } else {
                        throw new IOException("Packet influx");
                    }
                    // TODO Check NBT data here
                }
            } catch (Throwable ex) {
                this.isExploiter = true;
                c.error(this.player.getName() + " has tried to crash the server...", ex);
                this.disconnect("Chill man, dam!");
                return;
            }

            if ("MC|BEdit".equals(packetplayincustompayload.a())) {
                // cSpigot start - Cooldown on book edit
                if (this.lastBookTick + 20 > MinecraftServer.currentTick) {
                    disconnect("Book edited too quickly!");
                    return;
                }
                this.lastBookTick = MinecraftServer.currentTick;
                // cSpigot end
                packetdataserializer = new PacketDataSerializer(Unpooled.wrappedBuffer(packetplayincustompayload.b()));

                try {
                    itemstack = packetdataserializer.i();
                    if (itemstack == null) {
                        return;
                    }

                    if (!ItemBookAndQuill.b(itemstack.getTag())) {
                        throw new IOException("Invalid book tag!");
                    }

                    itemstack1 = this.player.inventory.getItemInHand();
                    if (itemstack1 != null) {
                        if (itemstack.getItem() == Items.WRITABLE_BOOK && itemstack.getItem() == itemstack1.getItem()) {
                            if (!validateBook(itemstack)) return; // Paper
                            itemstack1 = new ItemStack(Items.WRITABLE_BOOK); // CraftBukkit
                            itemstack1.a("pages", (NBTBase) itemstack.getTag().getList("pages", 8));
                            CraftEventFactory.handleEditBookEvent(player, itemstack1); // CraftBukkit
                        }
                        return;
                    }
                } catch (Exception exception) {
                    PlayerConnection.c.error("Couldn't handle book info", exception);
                    this.disconnect("Invalid book data!"); // CraftBukkit
                    return;
                } finally {
                    packetdataserializer.release();
                }

                return;
            } else if ("MC|BSign".equals(packetplayincustompayload.a())) {
                // cSpigot start - Cooldown on book sign
                if (this.lastBookTick + 20 > MinecraftServer.currentTick) {
                    disconnect("Book edited too quickly!");
                    return;
                }
                this.lastBookTick = MinecraftServer.currentTick;
                // cSpigot end
                packetdataserializer = new PacketDataSerializer(Unpooled.wrappedBuffer(packetplayincustompayload.b()));

                try {
                    itemstack = packetdataserializer.i();
                    if (itemstack == null) {
                        return;
                    }

                    if (!ItemWrittenBook.b(itemstack.getTag())) {
                        throw new IOException("Invalid book tag!");
                    }

                    // cSpigot start - Additional book fixes
                    if (itemstack.getTag().getString("title").length() > SettingsConfig.MAX_TITLE_SIZE ||
                            itemstack.getTag().getString("author").length() > SettingsConfig.MAX_AUTHOR_SIZE) {
                        throw new IOException("Invalid book tag!");
                    }
                    // cSpigot end

                    itemstack1 = this.player.inventory.getItemInHand();
                    if (itemstack1 != null) {
                        if (itemstack.getItem() == Items.WRITTEN_BOOK && itemstack1.getItem() == Items.WRITABLE_BOOK) {
                            if (!validateBook(itemstack)) return; // Paper
                            // CraftBukkit start
                            itemstack1 = new ItemStack(Items.WRITTEN_BOOK);
                            itemstack1.a("author", (NBTBase) (new NBTTagString(this.player.getName())));
                            itemstack1.a("title", (NBTBase) (new NBTTagString(itemstack.getTag().getString("title"))));
                            itemstack1.a("pages", (NBTBase) itemstack.getTag().getList("pages", 8));
                            itemstack1.setItem(Items.WRITTEN_BOOK);
                            CraftEventFactory.handleEditBookEvent(player, itemstack1);
                            // CraftBukkit end
                        }

                        return;
                    }
                } catch (Exception exception1) {
                    PlayerConnection.c.error("Couldn't sign book", exception1);
                    this.disconnect("Invalid book data!"); // CraftBukkit
                    return;
                } finally {
                    packetdataserializer.release();
                }

                return;
            } else if ("MC|TrSel".equals(packetplayincustompayload.a())) {
                try {
                    int i = packetplayincustompayload.b().readInt();
                    Container container = this.player.activeContainer;

                    if (container instanceof ContainerMerchant) {
                        ((ContainerMerchant) container).d(i);
                    }
                } catch (Exception exception2) {
                    PlayerConnection.c.error("Couldn\'t select trade", exception2);
                    this.disconnect("Invalid trade data!"); // CraftBukkit
                }
            } else if ("MC|AdvCdm".equals(packetplayincustompayload.a())) {
                if (!this.minecraftServer.getEnableCommandBlock()) {
                    this.player.sendMessage(new ChatMessage("advMode.notEnabled"));
                } else if (this.player.getBukkitEntity().isOp() && this.player.abilities.canInstantlyBuild) { // CraftBukkit - Change to Bukkit OP versus Vanilla OP
                    packetdataserializer = packetplayincustompayload.b();

                    try {
                        byte b0 = packetdataserializer.readByte();
                        CommandBlockListenerAbstract commandblocklistenerabstract = null;

                        if (b0 == 0) {
                            TileEntity tileentity = this.player.world.getTileEntity(new BlockPosition(packetdataserializer.readInt(), packetdataserializer.readInt(), packetdataserializer.readInt()));

                            if (tileentity instanceof TileEntityCommand) {
                                commandblocklistenerabstract = ((TileEntityCommand) tileentity).getCommandBlock();
                            }
                        } else if (b0 == 1) {
                            Entity entity = this.player.world.a(packetdataserializer.readInt());

                            if (entity instanceof EntityMinecartCommandBlock) {
                                commandblocklistenerabstract = ((EntityMinecartCommandBlock) entity).getCommandBlock();
                            }
                        }

                        String s = packetdataserializer.c(packetdataserializer.readableBytes());
                        boolean flag = packetdataserializer.readBoolean();

                        if (commandblocklistenerabstract != null) {
                            commandblocklistenerabstract.setCommand(s);
                            commandblocklistenerabstract.a(flag);
                            if (!flag) {
                                commandblocklistenerabstract.b((IChatBaseComponent) null);
                            }

                            commandblocklistenerabstract.h();
                            this.player.sendMessage(new ChatMessage("advMode.setCommand.success", s));
                        }
                    } catch (Exception exception3) {
                        PlayerConnection.c.error("Couldn't set command block", exception3);
                        this.disconnect("Invalid CommandBlock data!"); // CraftBukkit
                    } finally {
                        packetdataserializer.release();
                    }
                } else {
                    this.player.sendMessage(new ChatMessage("advMode.notAllowed"));
                }
            } else if ("MC|Beacon".equals(packetplayincustompayload.a())) {
                if (this.player.activeContainer instanceof ContainerBeacon) {
                    try {
                        packetdataserializer = packetplayincustompayload.b();
                        int j = packetdataserializer.readInt();
                        int k = packetdataserializer.readInt();
                        ContainerBeacon containerbeacon = (ContainerBeacon) this.player.activeContainer;
                        Slot slot = containerbeacon.getSlot(0);

                        if (slot.hasItem()) {
                            slot.a(1);
                            IInventory iinventory = containerbeacon.e();

                            iinventory.b(1, j);
                            iinventory.b(2, k);
                            iinventory.update();
                        }
                    } catch (Exception exception4) {
                        PlayerConnection.c.error("Couldn't set beacon", exception4);
                        this.disconnect("Invalid beacon data!"); // CraftBukkit
                    }
                }
            } else if ("MC|ItemName".equals(packetplayincustompayload.a()) && this.player.activeContainer instanceof ContainerAnvil) {
                ContainerAnvil containeranvil = (ContainerAnvil) this.player.activeContainer;

                if (packetplayincustompayload.b() != null && packetplayincustompayload.b().readableBytes() >= 1) {
                    String s1 = SharedConstants.a(packetplayincustompayload.b().c(32767));

                    if (s1.length() <= 30) {
                        containeranvil.a(s1);
                    }
                } else {
                    containeranvil.a("");
                }
            }
            // CraftBukkit start
            else if (packetplayincustompayload.a().equals("REGISTER")) {
                // cSpigot start - Disconnect for payload errors
                try {
                    String channels = packetplayincustompayload.b().toString(com.google.common.base.Charsets.UTF_8);
                    for (String channel : channels.split("\0")) {
                        if (!getPlayer().addChannel(channel)) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    PlayerConnection.c.error("Couldn\'t register custom payload", ex);
                    this.disconnect("Invalid payload REGISTER!");
                }
            } else if (packetplayincustompayload.a().equals("UNREGISTER")) {
                try {
                    String channels = packetplayincustompayload.b().toString(com.google.common.base.Charsets.UTF_8);
                    for (String channel : channels.split("\0")) {
                        getPlayer().removeChannel(channel);
                    }
                } catch (Exception ex) {
                    PlayerConnection.c.error("Couldn\'t register custom payload", ex);
                    this.disconnect("Invalid payload UNREGISTER!");
                }
            } else {
                try {
                    byte[] data = new byte[packetplayincustompayload.b().readableBytes()];
                    packetplayincustompayload.b().readBytes(data);
                    server.getMessenger().dispatchIncomingMessage(player.getBukkitEntity(), packetplayincustompayload.a(), data);
                } catch (Exception ex) {
                    PlayerConnection.c.error("Couldn\'t register custom payload", ex);
                    this.disconnect("Invalid custom payload!");
                }
                // cSpigot end
            }
        } finally {
            if (packetplayincustompayload.b().refCnt() > 0) {
                packetplayincustompayload.b().release();
            }
        }
        // CraftBukkit end
    }

    // CraftBukkit start - Add "isDisconnected" method
    public boolean isDisconnected() { // Spigot
        return !this.player.joining && !this.networkManager.channel.config().isAutoRead();
    }

    static class SyntheticClass_1 {

        static final int[] a;
        static final int[] b;
        static final int[] c = new int[PacketPlayInClientCommand.EnumClientCommand.values().length];

        static {
            try {
                SyntheticClass_1.c[PacketPlayInClientCommand.EnumClientCommand.PERFORM_RESPAWN.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror) {
                ;
            }

            try {
                SyntheticClass_1.c[PacketPlayInClientCommand.EnumClientCommand.REQUEST_STATS.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror1) {
                ;
            }

            try {
                SyntheticClass_1.c[PacketPlayInClientCommand.EnumClientCommand.OPEN_INVENTORY_ACHIEVEMENT.ordinal()] = 3;
            } catch (NoSuchFieldError nosuchfielderror2) {
                ;
            }

            b = new int[PacketPlayInEntityAction.EnumPlayerAction.values().length];

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.START_SNEAKING.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror3) {
                ;
            }

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.STOP_SNEAKING.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror4) {
                ;
            }

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING.ordinal()] = 3;
            } catch (NoSuchFieldError nosuchfielderror5) {
                ;
            }

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.STOP_SPRINTING.ordinal()] = 4;
            } catch (NoSuchFieldError nosuchfielderror6) {
                ;
            }

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.STOP_SLEEPING.ordinal()] = 5;
            } catch (NoSuchFieldError nosuchfielderror7) {
                ;
            }

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.RIDING_JUMP.ordinal()] = 6;
            } catch (NoSuchFieldError nosuchfielderror8) {
                ;
            }

            try {
                SyntheticClass_1.b[PacketPlayInEntityAction.EnumPlayerAction.OPEN_INVENTORY.ordinal()] = 7;
            } catch (NoSuchFieldError nosuchfielderror9) {
                ;
            }

            a = new int[PacketPlayInBlockDig.EnumPlayerDigType.values().length];

            try {
                SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.DROP_ITEM.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror10) {
                ;
            }

            try {
                SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.DROP_ALL_ITEMS.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror11) {
                ;
            }

            try {
                SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.RELEASE_USE_ITEM.ordinal()] = 3;
            } catch (NoSuchFieldError nosuchfielderror12) {
                ;
            }

            try {
                SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK.ordinal()] = 4;
            } catch (NoSuchFieldError nosuchfielderror13) {
                ;
            }

            try {
                SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK.ordinal()] = 5;
            } catch (NoSuchFieldError nosuchfielderror14) {
                ;
            }

            try {
                SyntheticClass_1.a[PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK.ordinal()] = 6;
            } catch (NoSuchFieldError nosuchfielderror15) {
                ;
            }

        }
    }


    private void setPendingPing(boolean isPending) {
        this.awaitingKeepAlive = isPending;
    }
    private boolean isPendingPing() {
        return this.awaitingKeepAlive;
    }


    public boolean isDisconnecting() {
        return disconnecting;
    }
    public void setDisconnecting(boolean disconnecting) {
        this.disconnecting = disconnecting;
    }

    private void setLastPing(final long lastPing) {
        this.j = lastPing;
    }

    private long getLastPing() {
        return this.j;
    }

    private void setKeepAliveID(final int keepAliveID) {
        this.i = keepAliveID;
    }

    private int getKeepAliveID() {
        return this.i;
    }
}
