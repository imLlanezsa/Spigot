package net.minecraft.server;

// CraftBukkit start

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerTeleportEvent;
// CraftBukkit end

import io.github.cspigot.config.SettingsConfig;

import java.util.stream.IntStream;

public class EntityEnderPearl extends EntityProjectile {

    private EntityLiving c;

    public EntityEnderPearl(World world) {
        super(world);
        this.loadChunks = world.paperSpigotConfig.loadUnloadedEnderPearls; // PaperSpigot
    }

    public EntityEnderPearl(World world, EntityLiving entityLiving) {
        super(world, entityLiving);
        this.c = entityLiving;
        this.loadChunks = world.paperSpigotConfig.loadUnloadedEnderPearls; // PaperSpigot
    }

    protected float m() {
        return this.shooter != null && this.shooter instanceof EntityPlayer ? (float) SettingsConfig.PEARL_GRAVITY : 0.03F;
    }

    protected float j() {
        return this.shooter != null && this.shooter instanceof EntityPlayer ? (float) SettingsConfig.PEARL_SPEED : 1.5F;
    }

    protected float l() {
        return this.shooter != null && this.shooter instanceof EntityPlayer ? (float) SettingsConfig.PEARL_VERTICAL_OFFSET : 0.0F;
    }

    protected float horizontalOffset() {
        return this.shooter != null && this.shooter instanceof EntityPlayer ? (float) SettingsConfig.PEARL_HORIZONTAL_OFFSET : 0.0F;
    }

    protected void a(MovingObjectPosition movingobjectposition) {
        EntityLiving entityliving = this.getShooter();

        if (movingobjectposition.entity != null) {
            if (movingobjectposition.entity == this.c) {
                return;
            }

            //if (SettingsConfig.PEARL_DAMAGE) {
            movingobjectposition.entity.damageEntity(DamageSource.projectile(this, entityliving), 0.0F);
            //}
        }

        // PaperSpigot start - Remove entities in unloaded chunks
        if (this.inUnloadedChunk && world.paperSpigotConfig.removeUnloadedEnderPearls) {
            this.die();
        }
        // PaperSpigot end

        // cSpigot start - Tali Pearls system
        BlockPosition blockPosition = movingobjectposition.a();

        if (blockPosition != null) {
            IBlockData blockData = world.getType(blockPosition);
            Block block = blockData.getBlock();
            boolean bypass =
                    (SettingsConfig.PEARL_TRIPWIRE && block == Blocks.TRIPWIRE)
                            || (SettingsConfig.PEARL_COBWEB && block == Blocks.WEB)
                            || (SettingsConfig.PEARL_BED && block == Blocks.BED)
                            || (SettingsConfig.PEARL_FENCE_GATE && (block == Blocks.FENCE_GATE || block == Blocks.SPRUCE_FENCE_GATE || block == Blocks.BIRCH_FENCE_GATE || block == Blocks.JUNGLE_FENCE_GATE || block == Blocks.DARK_OAK_FENCE_GATE || block == Blocks.ACACIA_FENCE_GATE) && blockData.get(BlockFenceGate.OPEN))
                            || (SettingsConfig.PEARL_SLAB && (block == Blocks.STONE_SLAB || block == Blocks.WOODEN_SLAB || block == Blocks.STONE_SLAB2));

            if (bypass) {
                return;
            }
        }

        // Switch to IntStream
        IntStream.range(0, 32).forEach(i -> this.world.addParticle(EnumParticle.PORTAL, this.locX, this.locY + this.random.nextDouble() * 2.0D, this.locZ, this.random.nextGaussian(), 0.0D, this.random.nextGaussian()));
        // cSpigot end

        if (!this.world.isClientSide) {
            if (entityliving instanceof EntityPlayer) {
                EntityPlayer entityplayer = (EntityPlayer) entityliving;

                if (entityplayer.playerConnection.getNetworkManager().g() && entityplayer.world == this.world && !entityplayer.isSleeping()) {
                    CraftPlayer player = entityplayer.getBukkitEntity();
                    Location location = this.getBukkitEntity().getLocation();

                    // cSpigot start - lock enderpearl Y to player
                    // check if there is an entity in the position
                    if (SettingsConfig.PEARL_BETTER_YAW && movingobjectposition.entity != null) {
                        // set the Y to that entity's
                        location.setY(movingobjectposition.entity.locY);
                    }

                    // cSpigot - Fix an issue with the player not landing on
                    // the ground, instead of going into the air when pearling.
                    location.setY((int) location.getY());

                    location.setPitch(player.getLocation().getPitch());
                    location.setYaw(player.getLocation().getYaw());

                    // cSpigot start - Fix Glitch Fix
                    // Teleport player to the middle of block if it lands on a wall to prevent phasing.
                    CraftBlock block = (CraftBlock) location.getBlock();
                    Block nmsBlock = block.getNMSBlock();
                    if (SettingsConfig.PEARL_WALL_GLITCH_FIX && (nmsBlock.getMaterial().isBuildable() &&
                            (movingobjectposition.direction == EnumDirection.EAST ||
                                    movingobjectposition.direction == EnumDirection.WEST ||
                                    movingobjectposition.direction == EnumDirection.NORTH ||
                                    movingobjectposition.direction == EnumDirection.SOUTH))) {

                        location.setX(location.getBlockX() + 0.5D);
                        location.setZ(location.getBlockZ() + 0.5D);
                    }
                    // cSpigot end

                    PlayerTeleportEvent teleportEvent = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
                    Bukkit.getPluginManager().callEvent(teleportEvent);

                    if (!teleportEvent.isCancelled() && !entityplayer.playerConnection.isDisconnected()) {
                        if (entityliving.au()) { // Is mounted
                            entityliving.mount(null);
                        }

                        entityplayer.playerConnection.teleport(teleportEvent.getTo());
                        entityliving.fallDistance = 0.0F;

                        // cSpigot start - Toggleable Pearl damage
                        if (SettingsConfig.PEARL_DAMAGE) {
                            CraftEventFactory.entityDamage = this;
                            entityliving.damageEntity(DamageSource.FALL, 5.0F);
                            CraftEventFactory.entityDamage = null;
                        }
                        // cSpigot end
                    }
                    // CraftBukkit end
                }
            } else if (entityliving != null) {
                entityliving.enderTeleportTo(this.locX, this.locY, this.locZ);
                entityliving.fallDistance = 0.0F;
            }
            this.die();
        }
    }

    public void t_() {
        EntityLiving entityliving = this.getShooter();

        if (entityliving != null && entityliving instanceof EntityHuman && !entityliving.isAlive()) {
            this.die();
        } else {
            super.t_();
        }

    }
}
