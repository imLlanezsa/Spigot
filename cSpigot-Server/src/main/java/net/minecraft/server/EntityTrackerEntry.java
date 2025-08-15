package net.minecraft.server;

import lombok.extern.log4j.Log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;

import io.github.cspigot.config.SettingsConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
// CraftBukkit end

@Log4j2
public class EntityTrackerEntry {

    private static final Logger p = LogManager.getLogger();
    public Entity tracker;
    public int b;
    public int c;
    public int xLoc;
    public int yLoc;
    public int zLoc;
    public int yRot;
    public int xRot;
    public int i;
    public double j;
    public double k;
    public double l;
    protected int m;
    protected double q;
    protected double r;
    protected double s;
    protected boolean isMoving;
    protected boolean u;
    protected int v;
    protected Entity w;
    protected boolean x;
    protected boolean y;
    protected boolean n;

    // PaperSpigot start
    // Replace trackedPlayers Set with enter Map. The value is true until the player receives
    // their first update (which is forced to have absolute coordinates), false afterward.
    public java.util.Map<EntityPlayer, Boolean> trackedPlayerMap = SettingsConfig.ASYNC_ENTITY_TRACKER
            ? new ConcurrentHashMap<>()
            : new java.util.HashMap<>();
    public Set<EntityPlayer> trackedPlayers = trackedPlayerMap.keySet();
    // PaperSpigot end

    // cSpigot start - Async entity tracker
    protected Collection<EntityPlayer> toRemove;
    protected int addRemoveRate;
    protected int addRemoveCooldown;
    protected boolean withinNoTrack = false;
    // cSpigot end

    // For async entity tracker!
    public EntityTrackerEntry() {
    }

    public EntityTrackerEntry(Entity entity, int i, int j, boolean flag) {
        this.toRemove = new ArrayList<>();
        this.tracker = entity;
        this.b = i;
        this.c = j;
        this.u = flag;

        entity.tracker = this; // Paper

        // cSpigot start - Fix Rounding calculation error for Position (Thanks to JT For this)
        if (entity instanceof EntityPlayer && SettingsConfig.RELATIVE_MOVE_FIX) {
            this.xLoc = (int) Math.round(entity.locX * 32.0D);
            this.yLoc = (int) Math.round(entity.locY * 32.0D);
            this.zLoc = (int) Math.round(entity.locZ * 32.0D);
        } else {
            this.xLoc = MathHelper.floor(entity.locX * 32.0D);
            this.yLoc = MathHelper.floor(entity.locY * 32.0D);
            this.zLoc = MathHelper.floor(entity.locZ * 32.0D);
        }
        // cSpigot end

        this.yRot = MathHelper.d(entity.yaw * 256.0F / 360.0F);
        this.xRot = MathHelper.d(entity.pitch * 256.0F / 360.0F);
        this.i = MathHelper.d(entity.getHeadRotation() * 256.0F / 360.0F);
        this.y = entity.onGround;

        // cSpigot start - Async entity tracker
        if (this.tracker instanceof EntityPlayer) {
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_PLAYERS; // players
        } else if (this.tracker instanceof EntityArrow || this.tracker instanceof EntityProjectile) {
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_PROJECTILES; // projectile things
        } else if (this.tracker.activationType == 2) { // animals
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_ANIMALS;
        } else if (this.tracker.activationType == 1) { // monsters
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_MONSTERS;
        } else {
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_OTHERS;
        }
        // cSpigot end

        this.addRemoveCooldown = this.tracker.getId() % this.addRemoveRate;
    }

    protected final Consumer<EntityPlayer> addNearPlayersConsumer = this::updatePlayer;
    // cSpigot end

    public void track(List<EntityHuman> list) {
        this.n = false;

        if (!this.isMoving || this.tracker.e(this.q, this.r, this.s) > 16.0D) {
            this.q = this.tracker.locX;
            this.r = this.tracker.locY;
            this.s = this.tracker.locZ;
            this.isMoving = true;
            this.n = true;
            //this.scanPlayers(list); // cSpigot - Async entity tracker
        }

        if (this.w != this.tracker.vehicle || this.tracker.vehicle != null && this.m % 60 == 0) {
            this.w = this.tracker.vehicle;
            this.broadcast(new PacketPlayOutAttachEntity(0, this.tracker, this.tracker.vehicle));
        }

        if (this.tracker instanceof EntityItemFrame && this.m % 20 == 0) { // Paper
            EntityItemFrame entityitemframe = (EntityItemFrame) this.tracker;
            ItemStack itemstack = entityitemframe.getItem();

            if (itemstack != null && itemstack.getItem() instanceof ItemWorldMap) { // Paper - moved back up
                WorldMap worldmap = Items.FILLED_MAP.getSavedMap(itemstack, this.tracker.world);
                // CraftBukkit
                for (EntityPlayer trackedPlayer : this.trackedPlayers) {
                    worldmap.a(trackedPlayer, itemstack);
                    Packet<?> packet = Items.FILLED_MAP.c(itemstack, this.tracker.world, trackedPlayer);
                    if (packet != null) {
                        trackedPlayer.playerConnection.sendPacket(packet);
                    }
                }
            }

            this.b();
        }

        if (this.m % this.c == 0 || this.tracker.ai || tracker.getDataWatcher().a()) {
            int i;
            int j;
            int k;

            if (this.tracker.vehicle == null) {
                ++this.v;

                // cSpigot start - Relative Move Fix
                if (this.tracker instanceof EntityPlayer && SettingsConfig.RELATIVE_MOVE_FIX) {
                    i = (int) Math.round(this.tracker.locX * 32.0D);
                    j = (int) Math.round(this.tracker.locY * 32.0D);
                    k = (int) Math.round(this.tracker.locZ * 32.0D);
                } else {
                    i = MathHelper.floor(this.tracker.locX * 32.0D);
                    j = MathHelper.floor(this.tracker.locY * 32.0D);
                    k = MathHelper.floor(this.tracker.locZ * 32.0D);
                }
                // cSpigot end

                int l = MathHelper.d(this.tracker.yaw * 256.0F / 360.0F);
                int i1 = MathHelper.d(this.tracker.pitch * 256.0F / 360.0F);

                int j1 = i - this.xLoc; // delta-x
                int k1 = j - this.yLoc; // delta-y
                int l1 = k - this.zLoc; // delta-z

                Packet<?> packet = null; // packet

                if (this.m > 0 || this.tracker instanceof EntityArrow) { // PaperSpigot - Moved up
                    boolean flag = Math.abs(j1) >= 4 || Math.abs(k1) >= 4 || Math.abs(l1) >= 4 || this.m % 60 == 0;
                    // CraftBukkit start - Code moved from below
                    if (flag) {
                        this.xLoc = i;
                        this.yLoc = j;
                        this.zLoc = k;
                    }

                    boolean flag1 = Math.abs(l - this.yRot) >= 4 || Math.abs(i1 - this.xRot) >= 4;
                    if (flag1) {
                        this.yRot = l;
                        this.xRot = i1;
                    }
                    // CraftBukkit end

                    if (j1 >= -128 && j1 < 128 && k1 >= -128 && k1 < 128 && l1 >= -128 && l1 < 128 && this.v <= 400 && !this.x && this.y == this.tracker.onGround) {
                        if ((!flag || !flag1) && !(this.tracker instanceof EntityArrow)) {
                            if (flag) {
                                packet = new PacketPlayOutEntity.PacketPlayOutRelEntityMove(this.tracker.getId(), (byte) j1, (byte) k1, (byte) l1, this.tracker.onGround);
                            } else if (flag1) {
                                packet = new PacketPlayOutEntity.PacketPlayOutEntityLook(this.tracker.getId(), (byte) l, (byte) i1, this.tracker.onGround);
                            }
                        } else {
                            packet = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(this.tracker.getId(), (byte) j1, (byte) k1, (byte) l1, (byte) l, (byte) i1, this.tracker.onGround);
                        }
                    } else {
                        this.y = this.tracker.onGround;
                        this.v = 0;

                        // cSpigot start - Replicate bukkit behaviour of updating targets of this packet first!
                        if (SettingsConfig.UPDATE_BEFORE_TELEPORT && this.tracker instanceof EntityPlayer) {
                            this.addNearPlayers(false);
                        }
                        // cSpigot end

                        packet = new PacketPlayOutEntityTeleport(this.tracker.getId(), i, j, k, (byte) l, (byte) i1, this.tracker.onGround);
                    }
                    if (SettingsConfig.SKIP_USELESS_PACKETS && isUselessPacket(packet)) {
                        packet = null; // Purpur
                    }
                }

                boolean flag3 = this.u || this.tracker.ai;
                if (flag3 && this.m > 0) {
                    double d0 = this.tracker.motX - this.j;
                    double d1 = this.tracker.motY - this.k;
                    double d2 = this.tracker.motZ - this.l;
                    double d3 = 0.02D;
                    double d4 = d0 * d0 + d1 * d1 + d2 * d2;

                    if (d4 > d3 * d3 || d4 > 0.0D && this.tracker.motX == 0.0D && this.tracker.motY == 0.0D && this.tracker.motZ == 0.0D) {
                        this.j = this.tracker.motX;
                        this.k = this.tracker.motY;
                        this.l = this.tracker.motZ;
                        this.broadcast(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.j, this.k, this.l));
                    }
                }

                if (packet != null) {
                    // PaperSpigot start - ensure fresh viewers get an absolute position on their first update,
                    // since we can't be certain what position they received in the spawn packet.
                    if (packet instanceof PacketPlayOutEntityTeleport) {
                        this.broadcast(packet);
                    } else {
                        PacketPlayOutEntityTeleport teleportPacket = null;

                        for (java.util.Map.Entry<EntityPlayer, Boolean> viewer : trackedPlayerMap.entrySet()) {
                            if (viewer.getValue()) {
                                viewer.setValue(false);
                                if (teleportPacket == null) {
                                    teleportPacket = new PacketPlayOutEntityTeleport(this.tracker.getId(), i, j, k, (byte) l, (byte) i1, this.tracker.onGround);
                                }
                                viewer.getKey().playerConnection.sendPacket(teleportPacket);
                            } else {
                                viewer.getKey().playerConnection.sendPacket(packet);
                            }
                        }
                    }
                    // PaperSpigot end
                }

                this.b();
                this.x = false;
            } else {
                i = MathHelper.d(this.tracker.yaw * 256.0F / 360.0F);
                j = MathHelper.d(this.tracker.pitch * 256.0F / 360.0F);
                boolean flag2 = Math.abs(i - this.yRot) >= 4 || Math.abs(j - this.xRot) >= 4;

                if (flag2) {
                    this.broadcast(new PacketPlayOutEntity.PacketPlayOutEntityLook(this.tracker.getId(), (byte) i, (byte) j, this.tracker.onGround));
                    this.yRot = i;
                    this.xRot = j;
                }

                this.xLoc = MathHelper.floor(this.tracker.locX * 32.0D);
                this.yLoc = MathHelper.floor(this.tracker.locY * 32.0D);
                this.zLoc = MathHelper.floor(this.tracker.locZ * 32.0D);
                this.b();
                this.x = true;
            }

            i = MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F);
            if (Math.abs(i - this.i) >= 4) {
                this.broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte) i));
                this.i = i;
            }

            this.tracker.ai = false;
        }

        ++this.m;

        if (this.tracker.velocityChanged) {
            // CraftBukkit start - Create PlayerVelocity event
            boolean cancelled = false;

            if (this.tracker instanceof EntityPlayer) {
                // cSpigot start - Disable fall damage knockback
                if (this.tracker.fallDamageKB) {
                    this.tracker.fallDamageKB = false;

                    if (!SettingsConfig.TOGGLE_FALL_DAMAGE_KB) {
                        this.tracker.velocityChanged = false;
                        return;
                    }
                }

                if (this.tracker.stuckKB) {
                    this.tracker.stuckKB = false;
                    if (SettingsConfig.FIX_SUFFOCATION_SPEED) {
                        this.tracker.velocityChanged = false;
                        return;
                    }
                }

                // cSpigot end

                Player player = (Player) this.tracker.getBukkitEntity();
                org.bukkit.util.Vector velocity = player.getVelocity();

                PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity);
                this.tracker.world.getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    cancelled = true;
                } else if (!velocity.equals(event.getVelocity())) {
                    player.setVelocity(event.getVelocity()); // cSpigot - Fix knockback not registering here
                }
            }

            if (!cancelled) {
                this.broadcastIncludingSelf(new PacketPlayOutEntityVelocity(this.tracker));
            }
            // CraftBukkit end
            this.tracker.velocityChanged = false;
        }
    }

    protected void b() {
        DataWatcher datawatcher = this.tracker.getDataWatcher();
        if (datawatcher.a()) {
            // cSpigot start - Add health obfuscation patch for pvp based servers
            PacketPlayOutEntityMetadata metadata = new PacketPlayOutEntityMetadata(this.tracker.getId(), datawatcher, false);
            if (SettingsConfig.OBFUSCATE_HEALTH_PACKET && this.doHealthObfuscation() && !trackedPlayers.isEmpty()) {
                PacketPlayOutEntityMetadata obfuscated = new PacketPlayOutEntityMetadata(this.tracker.getId(), datawatcher, false);

                if (obfuscated.obfuscateHealth()) {
                    this.broadcast(obfuscated);

                    EntityPlayer player = (EntityPlayer) this.tracker;
                    player.playerConnection.sendPacket(metadata);
                } else {
                    this.broadcastIncludingSelf(metadata);
                }
            } else {
                this.broadcastIncludingSelf(metadata);
            }
            // cSpigot end
        }

        if (this.tracker instanceof EntityLiving) {
            AttributeMapServer attributemapserver = (AttributeMapServer) ((EntityLiving) this.tracker).getAttributeMap();
            Set set = attributemapserver.getAttributes();

            if (!set.isEmpty()) {
                // CraftBukkit start - Send scaled max health
                if (this.tracker instanceof EntityPlayer) {
                    ((EntityPlayer) this.tracker).getBukkitEntity().injectScaledMaxHealth(set, false);
                }
                // CraftBukkit end
                this.broadcastIncludingSelf(new PacketPlayOutUpdateAttributes(this.tracker.getId(), set));
            }

            set.clear();
        }
    }

    public void broadcast(Packet<?> packet) {
        for (EntityPlayer entityplayer : this.trackedPlayers) {
            entityplayer.playerConnection.sendPacket(packet);
        }

    }

    public void broadcastIncludingSelf(Packet<?> packet) {
        this.broadcast(packet);

        if (this.tracker instanceof EntityPlayer) {
            ((EntityPlayer) this.tracker).playerConnection.sendPacket(packet);
        }

    }

    public void a() {
        for (EntityPlayer entityplayer : this.trackedPlayers) {
            entityplayer.d(this.tracker);
        }
    }

    public void a(EntityPlayer entityplayer) {
        if (this.trackedPlayers.contains(entityplayer)) {
            entityplayer.d(this.tracker);
            this.trackedPlayers.remove(entityplayer);
        }

    }


    // Purpur start
    protected boolean isUselessPacket(Packet<?> possibleUselessPacket) {
        if (possibleUselessPacket instanceof PacketPlayOutEntity) {
            PacketPlayOutEntity packet = (PacketPlayOutEntity) possibleUselessPacket;
            if (possibleUselessPacket instanceof PacketPlayOutEntity.PacketPlayOutRelEntityMove) {
                return packet.getX() == 0 && packet.getY() == 0 && packet.getZ() == 0;
            } else if (possibleUselessPacket instanceof PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook) {
                return packet.getX() == 0 && packet.getY() == 0 && packet.getZ() == 0 && packet.getYaw() == 0 && packet.getPitch() == 0;
            } else if (possibleUselessPacket instanceof PacketPlayOutEntity.PacketPlayOutEntityLook) {
                return packet.getYaw() == 0 && packet.getPitch() == 0;
            }
        }
        return false;
    }
    // Purpur end

    public void updatePlayer(EntityPlayer entityplayer) {
        // You don't tick yourself
        if (entityplayer == this.tracker) return;

        // If tracker is tracked but not in range, then just remove them.
        boolean tracked = this.trackedPlayers.contains(entityplayer);
        if (this.isInRange(entityplayer)) {
            if (!tracked && (this.e(entityplayer) || this.tracker.attachedToPlayer)) {
                // Useless check as withinNoTracker is always false
                if (this.tracker instanceof EntityPlayer && withinNoTrack()) {
                    return;
                }

                if (!entityplayer.getBukkitEntity().canSeeEntity(this.tracker.getBukkitEntity())) {
                    return;
                }

                entityplayer.removeQueue.remove(this.tracker.getId());
                this.trackedPlayerMap.put(entityplayer, true);

                // Send spawn packet for the newly tracked entity
                Packet<?> packet = this.createSpawnPacket();
                entityplayer.playerConnection.sendPacket(packet);

                if (!this.tracker.getDataWatcher().d()) {
                    PacketPlayOutEntityMetadata metadataPacket = new PacketPlayOutEntityMetadata(this.tracker.getId(), this.tracker.getDataWatcher(), true);

                    // cSpigot start - Add health obfuscation patch for pvp based servers
                    if (SettingsConfig.OBFUSCATE_HEALTH_PACKET && this.doHealthObfuscation()) {
                        metadataPacket.obfuscateHealth();
                    }
                    // cSpigot end

                    entityplayer.playerConnection.sendPacket(metadataPacket);
                }

                // This returns null for all entities currently?
                NBTTagCompound nbttagcompound = this.tracker.getNBTTag();
                if (nbttagcompound != null) {
                    entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateEntityNBT(this.tracker.getId(), nbttagcompound));
                }

                if (this.tracker instanceof EntityLiving) {
                    AttributeMapServer attributemapserver = (AttributeMapServer) ((EntityLiving) this.tracker).getAttributeMap();
                    Collection<AttributeInstance> collection = attributemapserver.c();

                    // CraftBukkit start - If sending own attributes send scaled health instead of current maximum health
                    if (this.tracker.getId() == entityplayer.getId()) {
                        ((EntityPlayer) this.tracker).getBukkitEntity().injectScaledMaxHealth(collection, false);
                    }
                    // CraftBukkit end

                    if (!collection.isEmpty()) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateAttributes(this.tracker.getId(), collection));
                    }
                }

                this.j = this.tracker.motX;
                this.k = this.tracker.motY;
                this.l = this.tracker.motZ;
                if (this.u && !(packet instanceof PacketPlayOutSpawnEntityLiving)) {
                    entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.tracker.motX, this.tracker.motY, this.tracker.motZ));
                }

                if (this.tracker.vehicle != null) {
                    entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(0, this.tracker, this.tracker.vehicle));
                }

                if (this.tracker instanceof EntityInsentient && ((EntityInsentient) this.tracker).getLeashHolder() != null) {
                    entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(1, this.tracker, ((EntityInsentient) this.tracker).getLeashHolder()));
                }

                if (this.tracker instanceof EntityLiving) {
                    for (int i = 0; i < 5; ++i) {
                        ItemStack itemstack = ((EntityLiving) this.tracker).getEquipment(i);

                        if (itemstack != null) {
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(this.tracker.getId(), i, itemstack, SettingsConfig.OBFUSCATE_ARMOR_PACKET));
                        }
                    }
                }

                if (this.tracker instanceof EntityHuman) {
                    EntityHuman entityhuman = (EntityHuman) this.tracker;

                    if (entityhuman.isSleeping()) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutBed(entityhuman, new BlockPosition(this.tracker)));
                    }
                }

                // CraftBukkit start - Fix for nonsensical head yaw
                if (this.tracker instanceof EntityLiving) { // SportPaper - avoid processing entities that can't change head rotation
                    this.i = MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F);
                    // SportPaper start
                    // This was originally introduced by CraftBukkit, though the implementation is wrong since it's broadcasting
                    // the packet again in a method that is already called for each player. This would create a very serious performance issue
                    // with high player and entity counts (each sendPacket call involves waking up the event loop and flushing the network stream).
                    // this.broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte) i));
                    entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(tracker, (byte) i));
                    // SportPaper end
                }
                // CraftBukkit end

                // Send out entity effects
                if (this.tracker instanceof EntityLiving) {
                    EntityLiving entityliving = (EntityLiving) this.tracker;
                    Iterator iterator = entityliving.getEffects().iterator();

                    while (iterator.hasNext()) {
                        Object next = iterator.next();
                        if (next != null) {
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEffect(this.tracker.getId(), ((MobEffect) next)));
                        }
                    }
                }
            }
        } else if (tracked) {
            this.trackedPlayers.remove(entityplayer);
            entityplayer.d(this.tracker);
        }
    }

    public boolean isInRange(EntityPlayer entityplayer) { return c(entityplayer); }
    public boolean c(EntityPlayer entityplayer) {
        // CraftBukkit start - this.*Loc / 30 -> this.tracker.loc*
        double d0 = entityplayer.locX - this.tracker.locX;
        double d1 = entityplayer.locZ - this.tracker.locZ;
        // CraftBukkit end

        return d0 >= (double) (-this.b) && d0 <= (double) this.b && d1 >= (double) (-this.b) && d1 <= (double) this.b && this.tracker.a(entityplayer);
    }

    protected boolean e(EntityPlayer entityplayer) {
        if (SettingsConfig.DONT_CHECK_COMMON_CHUNK) return true;

        return entityplayer.u().getPlayerChunkMap().a(entityplayer, this.tracker.ae, this.tracker.ag);
    }

    // cSpigot start - Async entity tracker
    /*public void scanPlayers(List<EntityHuman> list) {
        for (int i = 0; i < list.size(); ++i) {
            this.updatePlayer((EntityPlayer) list.get(i));
        }

    }*/
    // cSpigot end

    public Packet<?> createSpawnPacket() {
        return this.c();
    }

    private Packet c() {
        if (this.tracker.dead) {
            // CraftBukkit start - Remove useless error spam, just return
            // EntityTrackerEntry.p.warn("Fetching addPacket for removed entity");
            return null;
            // CraftBukkit end
        }

        if (this.tracker instanceof EntityItem) {
            return new PacketPlayOutSpawnEntity(this.tracker, 2, 1);
        } else if (this.tracker instanceof EntityPlayer) {
            return new PacketPlayOutNamedEntitySpawn((EntityHuman) this.tracker);
        } else if (this.tracker instanceof EntityMinecartAbstract) {
            EntityMinecartAbstract entityminecartabstract = (EntityMinecartAbstract) this.tracker;

            return new PacketPlayOutSpawnEntity(this.tracker, 10, entityminecartabstract.s().a());
        } else if (this.tracker instanceof EntityBoat) {
            return new PacketPlayOutSpawnEntity(this.tracker, 1);
        } else if (this.tracker instanceof IAnimal) {
            this.i = MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F);
            return new PacketPlayOutSpawnEntityLiving((EntityLiving) this.tracker);
        } else if (this.tracker instanceof EntityFishingHook) {
            EntityHuman entityhuman = ((EntityFishingHook) this.tracker).owner;

            return new PacketPlayOutSpawnEntity(this.tracker, 90, entityhuman != null ? entityhuman.getId() : this.tracker.getId());
        } else if (this.tracker instanceof EntityArrow) {
            Entity entity = ((EntityArrow) this.tracker).shooter;

            return new PacketPlayOutSpawnEntity(this.tracker, 60, entity != null ? entity.getId() : this.tracker.getId());
        } else if (this.tracker instanceof EntitySnowball) {
            return new PacketPlayOutSpawnEntity(this.tracker, 61);
        } else if (this.tracker instanceof EntityPotion) {
            return new PacketPlayOutSpawnEntity(this.tracker, 73, ((EntityPotion) this.tracker).getPotionValue());
        } else if (this.tracker instanceof EntityThrownExpBottle) {
            return new PacketPlayOutSpawnEntity(this.tracker, 75);
        } else if (this.tracker instanceof EntityEnderPearl) {
            return new PacketPlayOutSpawnEntity(this.tracker, 65);
        } else if (this.tracker instanceof EntityEnderSignal) {
            return new PacketPlayOutSpawnEntity(this.tracker, 72);
        } else if (this.tracker instanceof EntityFireworks) {
            return new PacketPlayOutSpawnEntity(this.tracker, 76);
        } else {
            PacketPlayOutSpawnEntity packetplayoutspawnentity;

            if (this.tracker instanceof EntityFireball) {
                EntityFireball entityfireball = (EntityFireball) this.tracker;

                byte b0 = 63;

                if (this.tracker instanceof EntitySmallFireball) {
                    b0 = 64;
                } else if (this.tracker instanceof EntityWitherSkull) {
                    b0 = 66;
                }

                if (entityfireball.shooter != null) {
                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, b0, ((EntityFireball) this.tracker).shooter.getId());
                } else {
                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, b0, 0);
                }

                packetplayoutspawnentity.d((int) (entityfireball.dirX * 8000.0D));
                packetplayoutspawnentity.e((int) (entityfireball.dirY * 8000.0D));
                packetplayoutspawnentity.f((int) (entityfireball.dirZ * 8000.0D));
                return packetplayoutspawnentity;
            } else if (this.tracker instanceof EntityEgg) {
                return new PacketPlayOutSpawnEntity(this.tracker, 62);
            } else if (this.tracker instanceof EntityTNTPrimed) {
                return new PacketPlayOutSpawnEntity(this.tracker, 50);
            } else if (this.tracker instanceof EntityEnderCrystal) {
                return new PacketPlayOutSpawnEntity(this.tracker, 51);
            } else if (this.tracker instanceof EntityFallingBlock) {
                EntityFallingBlock entityfallingblock = (EntityFallingBlock) this.tracker;

                return new PacketPlayOutSpawnEntity(this.tracker, 70, Block.getCombinedId(entityfallingblock.getBlock()));
            } else if (this.tracker instanceof EntityArmorStand) {
                return new PacketPlayOutSpawnEntity(this.tracker, 78);
            } else if (this.tracker instanceof EntityPainting) {
                return new PacketPlayOutSpawnEntityPainting((EntityPainting) this.tracker);
            } else {
                BlockPosition blockposition;

                if (this.tracker instanceof EntityItemFrame) {
                    EntityItemFrame entityitemframe = (EntityItemFrame) this.tracker;

                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, 71, entityitemframe.direction.b());
                    blockposition = entityitemframe.getBlockPosition();
                    packetplayoutspawnentity.a(MathHelper.d((float) (blockposition.getX() * 32)));
                    packetplayoutspawnentity.b(MathHelper.d((float) (blockposition.getY() * 32)));
                    packetplayoutspawnentity.c(MathHelper.d((float) (blockposition.getZ() * 32)));
                    return packetplayoutspawnentity;
                } else if (this.tracker instanceof EntityLeash) {
                    EntityLeash entityleash = (EntityLeash) this.tracker;

                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, 77);
                    blockposition = entityleash.getBlockPosition();
                    packetplayoutspawnentity.a(MathHelper.d((float) (blockposition.getX() * 32)));
                    packetplayoutspawnentity.b(MathHelper.d((float) (blockposition.getY() * 32)));
                    packetplayoutspawnentity.c(MathHelper.d((float) (blockposition.getZ() * 32)));
                    return packetplayoutspawnentity;
                } else if (this.tracker instanceof EntityExperienceOrb) {
                    return new PacketPlayOutSpawnEntityExperienceOrb((EntityExperienceOrb) this.tracker);
                } else {
                    throw new IllegalArgumentException("Don't know how to add " + this.tracker.getClass() + "!");
                }
            }
        }
    }

    public void clear(EntityPlayer entityplayer) {
        org.spigotmc.AsyncCatcher.catchOp("player tracker clear"); // Spigot
        if (this.trackedPlayers.contains(entityplayer)) {
            this.trackedPlayers.remove(entityplayer);
            entityplayer.d(this.tracker);
        }
    }

    // cSpigot start - Async entity tracker
    public int getRange() {
        if (this.tracker.passenger == null) {
            return this.b;
        }
        return Math.max(this.b, org.spigotmc.TrackingRange.getEntityTrackingRange(this.tracker.passenger, 0));
    }

    public void update() {
        this.withinNoTrack = this.withinNoTrack();
        if (--this.addRemoveCooldown <= 0) {
            this.removeFarPlayers();
            this.addNearPlayers();
            this.addRemoveCooldown = this.addRemoveRate;
        }

        this.track(null);
    }

    protected void removeFarPlayers() {
        if (this.withinNoTrack) {
            toRemove.addAll(this.trackedPlayers);
            processToRemove();
            return;
        }

        for (EntityPlayer entityplayer : trackedPlayers) {
            double d0 = entityplayer.locX - this.tracker.locX;
            double d1 = entityplayer.locZ - this.tracker.locZ;
            int range = this.getRange();

            if (!(d0 >= (-range) && d0 <= range && d1 >= (-range) && d1 <= range) || withinNoTrack()) {
                toRemove.add(entityplayer);
            }
        }

        this.processToRemove();
    }

    protected void processToRemove() {
        for (EntityPlayer entityPlayer : toRemove) {
            entityPlayer.d(this.tracker);
            this.trackedPlayers.remove(entityPlayer);
        }

        toRemove.clear();
    }

    public void addNearPlayers() {
        addNearPlayers(false);
    }

    protected void addNearPlayers(boolean updateCooldown) {
        if (this.withinNoTrack) {
            return;
        }
        if (updateCooldown) {
            this.addRemoveCooldown = addRemoveRate;
        }
        this.tracker.world.playerMap.forEachNearby(this.tracker.locX, this.tracker.locY, this.tracker.locZ, this.getRange(), false, addNearPlayersConsumer);
    }

    public boolean doHealthObfuscation() {
        return this.tracker.isAlive() && (this.tracker instanceof EntityPlayer);
    }

    protected boolean withinNoTrack() {
        return false; // cSpigot - simplify code since it always returned false - maybe will be used in the future, so I left this method
    }
    // cSpigot end

    public boolean isActive() {
        return true;
    }

    public boolean equals(Object object) {
        return object instanceof EntityTrackerEntry && ((EntityTrackerEntry) object).tracker.getId() == this.tracker.getId();
    }

    public int hashCode() {
        return this.tracker.getId();
    }
}
