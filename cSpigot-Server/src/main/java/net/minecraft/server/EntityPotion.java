package net.minecraft.server;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PotionSplashEvent;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.knockback.KnockbackProfile;

public class EntityPotion extends EntityProjectile {

    public ItemStack item;

    public EntityPotion(World world) {
        super(world);
    }

    public EntityPotion(World world, EntityLiving entityliving, int i) {
        this(world, entityliving, new ItemStack(Items.POTION, 1, i));
    }

    public EntityPotion(World world, EntityLiving entityliving, ItemStack itemstack) {
        super(world, entityliving);
        this.item = itemstack;
    }

    public EntityPotion(World world, double d0, double d1, double d2, ItemStack itemstack) {
        super(world, d0, d1, d2);
        this.item = itemstack;
    }

    // Configurable gravity
    protected float m() {
        if (this.shooter instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) this.getShooter();
            KnockbackProfile profile = player.getKnockbackProfile();
            return (float) (profile.getOverridePotion().getValue() ? profile.getPotionGravity().getValue() : SettingsConfig.POTION_GRAVITY);
        }
        return 0.05F;
    }

    protected float j() {
        if (this.shooter instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) this.getShooter();
            KnockbackProfile profile = player.getKnockbackProfile();
            return (float) (profile.getOverridePotion().getValue() ? profile.getPotionSpeed().getValue() : SettingsConfig.POTION_SPEED);
        }
        return -0.5F;
    }

    protected float l() {
        if (this.shooter instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) this.getShooter();
            KnockbackProfile profile = player.getKnockbackProfile();
            return (float) (profile.getOverridePotion().getValue() ? profile.getPotionVerticalOffset().getValue() : SettingsConfig.POTION_VERTICAL_OFFSET);
        }
        return -20.0F;
    }

    protected float horizontalOffset() {
        if (this.shooter instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) this.getShooter();
            KnockbackProfile profile = player.getKnockbackProfile();
            return (float) (profile.getOverridePotion().getValue() ? profile.getPotionHorizontalOffset().getValue() : SettingsConfig.POTION_HORIZONTAL_OFFSET);
        }
        return 0.0F;
    }

    public void setPotionValue(int i) {
        if (this.item == null) {
            this.item = new ItemStack(Items.POTION, 1, 0);
        }
        this.item.setData(i);
    }

    public int getPotionValue() {
        if (this.item == null) {
            this.item = new ItemStack(Items.POTION, 1, 0);
        }
        return this.item.getData();
    }

    protected void a(MovingObjectPosition movingobjectposition) {
        if (!this.world.isClientSide) {
            List<MobEffect> list = Items.POTION.h(this.item);

            if (list != null && !list.isEmpty()) {
                AxisAlignedBB affectArea = this.getBoundingBox().grow(4.0D, 2.0D, 4.0D);
                List<Entity> entitiesAround = this.world.a(EntityLiving.class, affectArea);

                if (!entitiesAround.isEmpty()) {
                    Map<LivingEntity, Double> affected = new HashMap<>();
                    for (Entity entity : entitiesAround) {
                        if (!(entity instanceof EntityLiving)) continue;
                        EntityLiving entityliving = (EntityLiving) entity;
                        double d0 = this.h(entityliving);

                        if (d0 < 16.0D) {
                            double intensity = 1.0D - Math.sqrt(d0) / 4.0D;

                            if (entityliving == movingobjectposition.entity) {
                                intensity = 1.0D;
                            } else if (Math.hypot(this.motX, this.motZ) > 0.4D && intensity > 0.8D) {
                                intensity = 1.0D;
                            }

                            affected.put((LivingEntity) entityliving.getBukkitEntity(), intensity);
                        }
                    }

                    PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent(this, affected);

                    if (!event.isCancelled()) {
                        for (LivingEntity victim : event.getAffectedEntities()) {
                            if (!(victim instanceof CraftLivingEntity)) continue;
                            EntityLiving entityliving = ((CraftLivingEntity) victim).getHandle();

                            double intensity = event.getIntensity(victim);

                            for (MobEffect mobeffect : list) {
                                int effectId = mobeffect.getEffectId();

                                if (!this.world.pvpMode && this.getShooter() instanceof EntityPlayer && entityliving instanceof EntityPlayer && entityliving != this.getShooter()) {
                                    // Block harmful effects in no-PvP mode
                                    if (effectId == 2 || effectId == 4 || effectId == 7 || effectId == 15 || effectId == 17 || effectId == 18 || effectId == 19) {
                                        continue;
                                    }
                                }

                                if (MobEffectList.byId[effectId].isInstant()) {
                                    MobEffectList.byId[effectId].applyInstantEffect(this, this.getShooter(), entityliving, mobeffect.getAmplifier(), intensity);
                                } else {
                                    int duration = (int) (intensity * (double) mobeffect.getDuration() + 0.5D);
                                    if (duration > 20) {
                                        entityliving.addEffect(new MobEffect(effectId, duration, mobeffect.getAmplifier()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Particles
            if (this.getShooter() instanceof EntityHuman) {
                BlockPosition pos = new BlockPosition(this.locX, this.locY, this.locZ);
                this.world.a((EntityHuman) this.getShooter(), 2002, pos, this.getPotionValue());
            } else {
                this.world.triggerEffect(2002, new BlockPosition(this), this.getPotionValue());
            }

            this.die();
        }
    }

    public void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        if (nbttagcompound.hasKeyOfType("Potion", 10)) {
            this.item = ItemStack.createStack(nbttagcompound.getCompound("Potion"));
        } else {
            this.setPotionValue(nbttagcompound.getInt("potionValue"));
        }
        if (this.item == null) {
            this.die();
        }
    }

    public void b(NBTTagCompound nbttagcompound) {
        super.b(nbttagcompound);
        if (this.item != null) {
            nbttagcompound.set("Potion", this.item.save(new NBTTagCompound()));
        }
    }
}
