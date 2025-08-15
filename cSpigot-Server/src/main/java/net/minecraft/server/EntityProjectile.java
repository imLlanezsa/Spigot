package net.minecraft.server;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import io.github.cspigot.config.SettingsConfig;

import com.eatthepath.uuid.FastUUID;

import java.util.List;
import java.util.stream.IntStream;

public abstract class EntityProjectile extends Entity implements IProjectile {

    private int blockX = -1;
    private int blockY = -1;
    private int blockZ = -1;
    private Block inBlockId;
    protected boolean inGround;
    public int shake;
    public EntityLiving shooter;
    public String shooterName;
    private int i;
    private int ar;

    public EntityProjectile(World world) {
        super(world);

        // cSpigot start - Add smaller hit boxes
        float width = 0.25F;
        float length = 0.25F;
        if (this instanceof EntityEnderPearl) {
            width = (float) SettingsConfig.PEARL_SMALLER_HITBOX;
            length = (float) SettingsConfig.PEARL_SMALLER_HITBOX;
        }
        this.setSize(width, length);
        // cSpigot end
    }

    protected void h() {
    }

    public EntityProjectile(World world, EntityLiving entityliving) {
        super(world);

        this.shooter = entityliving;

        // cSpigot start - Use proper projectile pitch and yaw
        if (getShooter().getBukkitEntity() instanceof CraftPlayer) {
            Player player = (CraftPlayer) getShooter().getBukkitEntity();
            this.pitch = player.getEyeLocation().getPitch();
            this.yaw = player.getEyeLocation().getYaw();
        }

        this.projectileSource = (org.bukkit.entity.LivingEntity) entityliving.getBukkitEntity(); // CraftBukkit

        // cSpigot - Add smaller hit boxes
        float width = 0.25F;
        float length = 0.25F;
        if (this instanceof EntityEnderPearl) {
            width = (float) SettingsConfig.PEARL_SMALLER_HITBOX;
            length = (float) SettingsConfig.PEARL_SMALLER_HITBOX;
        }
        this.setSize(width, length);
        // cSpigot end

        float projectileYaw = this instanceof EntityEnderPearl ? 0.0F : this instanceof EntityPotion ? 0.05F : 1.0F; // Keep at 0.05 for centered animation but throws with full accuracy if at 0.0
        float projectileHeight = this instanceof EntityEnderPearl ? 0.0F : 0.10000000149011612F;
        if (this instanceof EntityPotion) {
            projectileHeight = 0.05F;
        }

        this.setPositionRotation(entityliving.locX, entityliving.locY + (double) entityliving.getHeadHeight(), entityliving.locZ, entityliving.yaw, entityliving.pitch);

        // cSpigot - Apply original yaw/pitch
       if (this instanceof EntityEnderPearl || this instanceof EntityPotion) {
            this.locX -= (MathHelper.cos(this.yaw / 180.0F * 3.1415927F) * projectileYaw);
            this.locY -= projectileHeight;
            this.locZ -= (MathHelper.sin(this.yaw / 180.0F * 3.1415927F) * projectileYaw);
        } else {
            this.locX -= MathHelper.cos(yaw / 180.0F * 3.1415927F) * 0.16F;
            this.locY -= projectileHeight;
            this.locZ -= MathHelper.sin(yaw / 180.0F * 3.1415927F) * 0.16F;
        }

        this.setPosition(this.locX, this.locY, this.locZ);

        float f = 0.4F;

        this.motX = (-MathHelper.sin(this.yaw / 180.0F * 3.1415927F) * MathHelper.cos(this.pitch / 180.0F * 3.1415927F) * f);
        this.motZ = (MathHelper.cos(this.yaw / 180.0F * 3.1415927F) * MathHelper.cos(this.pitch / 180.0F * 3.1415927F) * f);
        this.motY = (-MathHelper.sin((this.pitch + this.l()) / 180.0F * 3.1415927F) * f);

        this.shoot(this.motX, this.motY, this.motZ, this.j(), 1.0F);
    }

    public EntityProjectile(World world, double d0, double d1, double d2) {
        super(world);
        this.i = 0;
        this.setSize(0.25F, 0.25F);
        this.setPosition(d0, d1, d2);
    }

    // PAIL: speed
    protected float j() {
        return 1.5F;
    }

    // PAIL: verticalOffset
    protected float l() {
        return 0.0F;
    }

//    protected float horizontalOffset() {
//        return 0.0F;
//    }

    public void shoot(double d0, double d1, double d2, float f, float f1) {
        float f2 = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

        d0 /= f2;
        d1 /= f2;
        d2 /= f2;

        d0 += this.random.nextGaussian() * 0.007499999832361937D * (double) f1;
        d1 += this.random.nextGaussian() * 0.007499999832361937D * (double) f1;
        d2 += this.random.nextGaussian() * 0.007499999832361937D * (double) f1;

        d0 *= f;
        d1 *= f;
        d2 *= f;

        this.motX = d0;
        this.motY = d1;
        this.motZ = d2;

        float f3 = MathHelper.sqrt(d0 * d0 + d2 * d2);

        this.lastYaw = this.yaw = (float) (MathHelper.b(d0, d2) * 180.0D / 3.1415927410125732D);
        this.lastPitch = this.pitch = (float) (MathHelper.b(d1, f3) * 180.0D / 3.1415927410125732D);
        this.i = 0;
    }

    public void t_() {
        this.P = this.locX;
        this.Q = this.locY;
        this.R = this.locZ;
        super.t_();
        if (this.shake > 0) {
            --this.shake;
        }

        if (this.inGround) {
            if (this.world.getType(new BlockPosition(this.blockX, this.blockY, this.blockZ)).getBlock() == this.inBlockId) {
                ++this.i;
                if (this.i == 1200) {
                    this.die();
                }

                return;
            }

            this.inGround = false;
            this.motX *= (this.random.nextFloat() * 0.2F);
            this.motY *= (this.random.nextFloat() * 0.2F);
            this.motZ *= (this.random.nextFloat() * 0.2F);
            this.i = 0;
            this.ar = 0;
        } else {
            ++this.ar;
        }

        Vec3D vec3d = new Vec3D(this.locX, this.locY, this.locZ);
        Vec3D vec3d1 = new Vec3D(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);

        MovingObjectPosition movingobjectposition = this instanceof EntityEnderPearl ? this.world.rayTrace(vec3d, vec3d1, false, false, false) : this.world.rayTrace(vec3d, vec3d1);

        vec3d = new Vec3D(this.locX, this.locY, this.locZ);
        vec3d1 = new Vec3D(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);

        if (movingobjectposition != null) {
            vec3d1 = new Vec3D(movingobjectposition.pos.a, movingobjectposition.pos.b, movingobjectposition.pos.c);
        }

        if (!this.world.isClientSide) {
            Entity entity = null;
            List<Entity> list = this.world.getEntities(this, this.getBoundingBox().a(this.motX, this.motY, this.motZ).grow(1.0D, 1.0D, 1.0D));
            double d0 = 0.0D;

            EntityLiving entityliving = this.getShooter();
            for ( Entity queryEntity : list ) {
                // cSpigot start - Configurable Air Time or Collision time? (IonSpigot)
                int airTime = this instanceof EntityPotion ? SettingsConfig.POTION_AIR_TICKS : 5;
                if (queryEntity.ad() && (queryEntity != entityliving || this.ar >= airTime)) {

                    final float f = 0.3F;
                    AxisAlignedBB axisalignedbb = queryEntity.getBoundingBox().grow(f, f, f);
                    MovingObjectPosition queryEntityMBP = axisalignedbb.a(vec3d, vec3d1);

                    // cSpigot start - Smooth Potting (IonSpigot)
                    if (this instanceof EntityPotion && SettingsConfig.SMOOTH_POTIONS && queryEntityMBP == null && getBoundingBox().b(queryEntity.getBoundingBox())) {
                        queryEntityMBP = new MovingObjectPosition(queryEntity);
                    }
                    // cSpigot end (IonSpigot)

                    if (queryEntityMBP != null) {
                        double d1 = vec3d.distanceSquared(queryEntityMBP.pos);

                        if (d1 < d0 || d0 == 0.0D) {
                            entity = queryEntity;
                            d0 = d1;
                        }
                    }
                }
            }

            if (entity != null) {
                movingobjectposition = new MovingObjectPosition(entity);
            }
        }

        // cSpigot start - splash the potion after 4 ticks of living if it's in range of the player
        if (SettingsConfig.REDUCED_SPLASH && this instanceof EntityPotion && this.getShooter() != null && this.getShooter().isSprinting() && ar > 5) {
            double diffX = Math.abs(this.getShooter().locX - (this.locX + this.motX));
            double diffY = 2.0D - Math.abs(this.getShooter().locY - (this.locY + this.motY));
            double diffZ = Math.abs(this.getShooter().locZ - (this.locZ + this.motZ));

            if ((diffX + diffZ) / 2.0D < 0.75D && diffY > -0.35D) {
                this.die();
                this.a(new MovingObjectPosition(this.getShooter()));
                org.bukkit.craftbukkit.event.CraftEventFactory.callProjectileHitEvent(this);
                return;
            }
        }
        // cSpigot end

        // PaperSpigot start - Allow projectiles to fly through vanished players the shooter can't see
        if (movingobjectposition != null && movingobjectposition.entity instanceof EntityPlayer && this.getShooter() != null && this.getShooter() instanceof EntityPlayer) {
            if (!((EntityPlayer) this.getShooter()).getBukkitEntity().canSee(((EntityPlayer) movingobjectposition.entity).getBukkitEntity())) {
                movingobjectposition = null;
            }
        }
        // PaperSpigot end

        if (movingobjectposition != null) {
            if (movingobjectposition.type == MovingObjectPosition.EnumMovingObjectType.BLOCK && this.world.getType(movingobjectposition.a()).getBlock() == Blocks.PORTAL) {
                this.d(movingobjectposition.a());
            } else {
                this.a(movingobjectposition);
                // CraftBukkit start
                if (this.dead) {
                    org.bukkit.craftbukkit.event.CraftEventFactory.callProjectileHitEvent(this);
                }
                // CraftBukkit end
            }
        }

        this.locX += this.motX;
        this.locY += this.motY;
        this.locZ += this.motZ;
        float f1 = MathHelper.sqrt(this.motX * this.motX + this.motZ * this.motZ);

        this.yaw = (float) (MathHelper.b(this.motX, this.motZ) * 180.0D / 3.1415927410125732D);

        for ( this.pitch = (float) (MathHelper.b(this.motY, f1) * 180.0D / 3.1415927410125732D); this.pitch - this.lastPitch < -180.0F; this.lastPitch -= 360.0F) {
            ;
        }

        while (this.pitch - this.lastPitch >= 180.0F) {
            this.lastPitch += 360.0F;
        }

        while (this.yaw - this.lastYaw < -180.0F) {
            this.lastYaw -= 360.0F;
        }

        while (this.yaw - this.lastYaw >= 180.0F) {
            this.lastYaw += 360.0F;
        }

        this.pitch = this.lastPitch + (this.pitch - this.lastPitch) * 0.2F;
        this.yaw = this.lastYaw + (this.yaw - this.lastYaw) * 0.2F;
        float f2 = 0.99F;
        float f3 = this.m();

        if (this.V()) {
            final float f4 = 0.25F;

            IntStream.range(0, 4).forEach(i -> this.world.addParticle(EnumParticle.WATER_BUBBLE, this.locX - this.motX * (double) f4, this.locY - this.motY * (double) f4, this.locZ - this.motZ * (double) f4, this.motX, this.motY, this.motZ));

            f2 = 0.8F;
        }

        this.motX *= f2;
        this.motY *= f2;
        this.motZ *= f2;
        this.motY -= f3;
        this.setPosition(this.locX, this.locY, this.locZ);
    }

    protected float m() {
        return 0.03F;
    }

    protected abstract void a(MovingObjectPosition movingobjectposition);

    public void b(NBTTagCompound nbttagcompound) {
        nbttagcompound.setShort("xTile", (short) this.blockX);
        nbttagcompound.setShort("yTile", (short) this.blockY);
        nbttagcompound.setShort("zTile", (short) this.blockZ);
        MinecraftKey minecraftkey = (MinecraftKey) Block.REGISTRY.c(this.inBlockId);

        nbttagcompound.setString("inTile", minecraftkey == null ? "" : minecraftkey.toString());
        nbttagcompound.setByte("shake", (byte) this.shake);
        nbttagcompound.setByte("inGround", (byte) (this.inGround ? 1 : 0));
        if ((this.shooterName == null || this.shooterName.length() == 0) && this.shooter instanceof EntityHuman) {
            this.shooterName = this.shooter.getName();
        }

        nbttagcompound.setString("ownerName", this.shooterName == null ? "" : this.shooterName);
    }

    public void a(NBTTagCompound nbttagcompound) {
        this.blockX = nbttagcompound.getShort("xTile");
        this.blockY = nbttagcompound.getShort("yTile");
        this.blockZ = nbttagcompound.getShort("zTile");
        if (nbttagcompound.hasKeyOfType("inTile", 8)) {
            this.inBlockId = Block.getByName(nbttagcompound.getString("inTile"));
        } else {
            this.inBlockId = Block.getById(nbttagcompound.getByte("inTile") & 255);
        }

        this.shake = nbttagcompound.getByte("shake") & 255;
        this.inGround = nbttagcompound.getByte("inGround") == 1;
        this.shooter = null;
        this.shooterName = nbttagcompound.getString("ownerName");
        if (this.shooterName != null && this.shooterName.length() == 0) {
            this.shooterName = null;
        }

        this.shooter = this.getShooter();
    }

    public EntityLiving getShooter() {
        if (this.shooter == null && this.shooterName != null && this.shooterName.length() > 0) {
            this.shooter = this.world.a(this.shooterName);
            if (this.shooter == null && this.world instanceof WorldServer) {
                try {
                    Entity entity = ((WorldServer) this.world).getEntity(FastUUID.parseUUID(this.shooterName));

                    if (entity instanceof EntityLiving) {
                        this.shooter = (EntityLiving) entity;
                    }
                } catch (Throwable throwable) {
                    this.shooter = null;
                }
            }
        }

        return this.shooter;
    }
}