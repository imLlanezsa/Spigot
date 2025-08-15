package io.github.cspigot.feature.async.tracker;

import net.minecraft.server.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.jetbrains.annotations.Nullable;
import io.github.cspigot.config.SettingsConfig;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 2/29/2024
 */
public class AsyncEntityTrackerEntry extends EntityTrackerEntry {

    protected long lastArmor = 0;
    private PacketPlayOutUpdateAttributes attributeSnapshot = null;
    private double scaledHealthSnapshot = 0;
    public boolean isActive = true;

    public AsyncEntityTrackerEntry(Entity entity, int i, int j, boolean flag) {
        super();

        this.toRemove = new ConcurrentLinkedQueue<>();
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
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_PLAYERS_ASYNC; // players
        } else if (this.tracker instanceof EntityArrow || this.tracker instanceof EntityProjectile) {
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_PROJECTILES_ASYNC; // projectile things
        } else if (this.tracker.activationType == 2) { // animals
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_ANIMALS_ASYNC;
        } else if (this.tracker.activationType == 1) { // monsters
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_MONSTERS_ASYNC;
        } else {
            this.addRemoveRate = SettingsConfig.ADD_REMOVE_RATE_OTHERS_ASYNC;
        }
        this.addRemoveCooldown = this.tracker.getId() % this.addRemoveRate;
    }

    public void track(List<EntityHuman> list) {
        this.n = false;

        if (!this.isMoving || this.tracker.e(this.q, this.r, this.s) > 16.0D) {
            this.q = this.tracker.locX;
            this.r = this.tracker.locY;
            this.s = this.tracker.locZ;
            this.isMoving = true;
            this.n = true;
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

        // cSpigot start - Async entity tracker
        boolean dataWatcherDirty = ((Supplier<Boolean>) () -> {
            try {
                return tracker.getDataWatcher().a();
            } catch (NullPointerException e) {
                if (e.getMessage() == null || e.getMessage().contains("Cannot invoke")) {
                    return false;
                }
                throw e;
            }
        }).get();

        if (this.m % this.c == 0 || this.tracker.ai || dataWatcherDirty) { // cSpigot end
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
                            EntityPlayer player = viewer.getKey();
                            if (viewer.getValue()) {
                                viewer.setValue(false);
                                if (teleportPacket == null) {
                                    teleportPacket = new PacketPlayOutEntityTeleport(this.tracker.getId(), i, j, k, (byte) l, (byte) i1, this.tracker.onGround);
                                }
                                player.playerConnection.sendPacket(teleportPacket);
                            } else if (!SettingsConfig.DELAYED_ENTITY_SPAWN || player.isRealPlayer || player.getTicksExisted() > 20) {
                                player.playerConnection.sendPacket(packet);
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
        try {
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
        } catch (NullPointerException e) {
            if (Arrays.stream(e.getStackTrace()).noneMatch(a -> a.getClassName().contains("citizens"))) {
                throw e;
            }
        }

        if (!SettingsConfig.COMPATIBILITY_ENTITY_TRACKER && this.tracker instanceof EntityLiving) {
            AttributeMapServer attributemapserver = (AttributeMapServer) ((EntityLiving) this.tracker).getAttributeMap();
            Set<AttributeInstance> set = attributemapserver.getAttributes();

            if (!set.isEmpty()) {
                // Why do this? It reduces the amount of garbage that the garbage collector needs to collect
                final @Nullable CraftPlayer craftPlayer = this.tracker instanceof EntityPlayer ? ((EntityPlayer) this.tracker).getBukkitEntity() : null;

                // CraftBukkit start - Send scaled max health
                if (this.tracker instanceof EntityPlayer) {
                    craftPlayer.injectScaledMaxHealth(set, false);
                }
                // CraftBukkit end
                final PacketPlayOutUpdateAttributes packet = new PacketPlayOutUpdateAttributes(this.tracker.getId(), set);
                if (!packet.equals(this.attributeSnapshot) || SettingsConfig.ATTRIBUTE_NO_LATE_CHECK) {
                    if (SettingsConfig.ATTRIBUTES_NT) {
                        AsyncUtil.runNextTick(() -> this.broadcastIncludingSelf(packet));
                    } else {
                        this.broadcastIncludingSelf(packet);
                    }
                    this.attributeSnapshot = packet;
                    this.scaledHealthSnapshot = craftPlayer != null ? craftPlayer.getRawScaledHealth() : 0;
                }
                set.clear();
            }

            if (SettingsConfig.LIVE_EQUIP && lastArmor != Arrays.hashCode(this.tracker.getEquipment())) {
                for (int i = 0; i < 5; ++i) {
                    ItemStack itemstack = ((EntityLiving) this.tracker).getEquipment(i);

                    if (itemstack != null) {
                        this.broadcast(new PacketPlayOutEntityEquipment(this.tracker.getId(), i, itemstack, SettingsConfig.OBFUSCATE_ARMOR_PACKET));
                    }
                }
                lastArmor = Arrays.hashCode(this.tracker.getEquipment());
            }
        }
    }

    public void updatePlayer(EntityPlayer entityplayer) {
        if (!this.isActive()) return; // cSpigot - Race conditions.

        // You don't tick yourself
        if (entityplayer == this.tracker) {
            if (this.tracker != null && SettingsConfig.COMPATIBILITY_ENTITY_TRACKER) {
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
            return;
        }

        // If tracker is tracked but not in range, then just remove them.
        boolean tracked = this.trackedPlayers.contains(entityplayer);
        if (this.isInRange(entityplayer)) {
            boolean realPlayer = (!(tracker instanceof EntityPlayer)) || ((EntityPlayer) tracker).isRealPlayer;
            boolean shouldUpdate = (realPlayer || entityplayer.getTicksExisted() > 20 || !SettingsConfig.DELAYED_ENTITY_SPAWN);
            if (!tracked && shouldUpdate && (this.e(entityplayer) || this.tracker.attachedToPlayer)) {
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
                    if (Bukkit.isPrimaryThread()) {
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateEntityNBT(this.tracker.getId(), nbttagcompound));
                    } else {
                        AsyncUtil.runNextTick(() -> entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateEntityNBT(this.tracker.getId(), nbttagcompound)));
                    }
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

                // Yes this is a race condition. Can this be resolved easily? Yes. Am I going to resolve it? Fuck no I'm too lazy for that.
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
                    // Only do this for async entity tracker, normal tracker does not need this.
                    if (!Bukkit.isPrimaryThread() && SettingsConfig.NT_EQUIP) {
                        // Running next could give time for the server to get the latest items.
                        AsyncUtil.runNextTick(() -> {
                            for (int i = 0; i < 5; ++i) {
                                ItemStack itemstack = ((EntityLiving) this.tracker).getEquipment(i);

                                if (itemstack != null) {
                                    entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(this.tracker.getId(), i, itemstack, SettingsConfig.OBFUSCATE_ARMOR_PACKET));
                                }
                            }
                        });
                    } else {
                        for (int i = 0; i < 5; ++i) {
                            ItemStack itemstack = ((EntityLiving) this.tracker).getEquipment(i);

                            if (itemstack != null) {
                                entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(this.tracker.getId(), i, itemstack, SettingsConfig.OBFUSCATE_ARMOR_PACKET));
                            }
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

    public boolean isActive() {
        return isActive;
    }

//    protected void addNearPlayers(boolean updateCooldown) {
//        if (this.withinNoTrack) {
//            return;
//        }
//        if (updateCooldown) {
//            this.addRemoveCooldown = addRemoveRate;
//        }
//        this.tracker.world.playerMap.forEachNearby(this.tracker.locX, this.tracker.locY, this.tracker.locZ, this.getRange(), true, addNearPlayersConsumer);
//    }
}
