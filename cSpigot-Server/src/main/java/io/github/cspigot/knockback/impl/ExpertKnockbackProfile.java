package io.github.cspigot.knockback.impl;

import net.minecraft.server.*;
import org.bukkit.util.Vector;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;

/**
 * This Project is property of Refine Development © 2021 2023
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 12/7/2022
 * @version cSpigot
 */

public class ExpertKnockbackProfile extends KnockbackProfile {

    private final KnockbackValue<Double> horizontal = new KnockbackValue<>("horizontal", "HORIZONTAL", "Horizontal", "Horizontal Multiplier", 70, Double.class, 0.43D);
    private final KnockbackValue<Double> vertical = new KnockbackValue<>("vertical", "VERTICAL", "Vertical", "Vertical Value", 69, Double.class, 0.33D);

    private final KnockbackValue<Boolean> inherit_horizontal = new KnockbackValue<>("inheritance_horizontal", "INHERIT-H-ENABLED", "Inherit Horizontal", "Add a certain value to horizontal before actual calculations", 68, Boolean.class, true);
    private final KnockbackValue<Boolean> inherit_vertical = new KnockbackValue<>("inheritance_vertical", "INHERIT-V-ENABLED", "Inherit Vertical", "Add a certain value to vertical before actual calculations", 66, Boolean.class, false);

    private final KnockbackValue<Double> friction_horizontal = new KnockbackValue<>("friction_horizontal", "FRICTION-H", "Friction Horizontal", "Friction values, actual knockback is divided by this", 64, Double.class, 1.0D);
    private final KnockbackValue<Double> friction_vertical = new KnockbackValue<>("friction_vertical", "FRICTION-V", "Friction Vertical", "", 63, Double.class, 1.0D);

    private final KnockbackValue<Double> ground_horizontal = new KnockbackValue<>("ground_horizontal", "GROUND-H", "Ground Horizontal Multiplier", "When player is hit but on ground, this value is multiplied with initial kb", 62, Double.class, 1.1D);
    private final KnockbackValue<Double> ground_vertical = new KnockbackValue<>("ground_vertical", "GROUND-V", "Ground Vertical Multiplier", "", 61, Double.class, 1.0D);

    private final KnockbackValue<Double> sprint_horizontal = new KnockbackValue<>("sprint_horizontal", "SPRINT-H", "Sprint Horizontal Multiplier", "When player is hit but attacker is spriting, this value is multiplied with initial kb", 60, Double.class, 1.2D);
    private final KnockbackValue<Double> sprint_vertical = new KnockbackValue<>("sprint_vertical", "SPRINT-V", "Sprint Vertical Multiplier", "", 59, Double.class, 1.0D);

    private final KnockbackValue<Boolean> limitVertical = new KnockbackValue<>("limit_vertical", "Y-LIMIT", "Limit Vertical", "Should we dynamically limit player's vertical axis?", 58, Boolean.class, true);
    private final KnockbackValue<Double> limitVerticalValue = new KnockbackValue<>("limit_vertical_value", "Y-LIMIT-VALUE", "Vertical Limit", "Dynamic vertical axis limit value", 57, Double.class, 1.2D);

    private final KnockbackValue<Boolean> stop_sprint = new KnockbackValue<>("stop_sprint", "STOP-SPRINT", "StopSprint", "Should the attacker have their sprint reset upon attacking", 56, Boolean.class, true);
    private final KnockbackValue<Double> attacker_slowdown = new KnockbackValue<>("attacker_slowdown", "SLOWDOWN", "Attacker Slowdown (AutoWtap)", "This value is multiplied in horizontal axis after calculations and sprint reset", 55, Double.class, 0.6D);

    private final KnockbackValue<Boolean> combo_mode = new KnockbackValue<>("combo", "COMBO.ENABLED", "Combo Mode", "Toggle combo mode", 54, Boolean.class, false);
    private final KnockbackValue<Integer> combo_ticks = new KnockbackValue<>("combo_ticks", "COMBO.TICKS", "Combo Ticks", "This is used to track when to move player back down from y axis", 53, Integer.class, 10);
    private final KnockbackValue<Double> combo_velocity = new KnockbackValue<>("combo_velocity", "COMBO.VELOCITY", "Combo Velocity", "Horizontal velocity of a player in combo mode", 52, Double.class, -0.05D);
    private final KnockbackValue<Double> combo_height = new KnockbackValue<>("combo_height", "COMBO.HEIGHT", "Combo Height", "Vertical velocity of a player in combo mode", 51, Double.class, 2.5D);


    public ExpertKnockbackProfile(String name) {
        super(name, KnockbackType.EXPERT);

        this.loadValues();
    }

    /**
     * The main method that handles all the knockback upon getting
     * hit and also manages PlayerVelocityEvent
     *
     * @param attacker              {@link Entity} The Entity attacking
     * @param entity                {@link Entity} The Entity getting attacked
     * @param knockbackEnchantLevel {@link Integer} the knockback level if there is one otherwise its 0
     */
    public void attackEntity(EntityPlayer attacker, Entity entity, int knockbackEnchantLevel, double[] velocity) {
        double velX, velY, velZ;

        if (inherit_horizontal.getValue()) {
            double entityVelX = entity.motX * friction_horizontal.getValue();
            double entityVelZ = entity.motZ * friction_horizontal.getValue();

            velX = entityVelX + MathHelper.sin((float) Math.toRadians(attacker.yaw)) * -1.0;
            velZ = entityVelZ + MathHelper.cos((float) Math.toRadians(attacker.yaw));
        } else {
            velX = Math.sin(Math.toRadians(attacker.getHeadRotation())) * -1.0;
            velZ = Math.cos(Math.toRadians(attacker.getHeadRotation()));
        }

        velX *= horizontal.getValue();
        velZ *= horizontal.getValue();

        if (inherit_vertical.getValue()) {
            double entityVelY = entity.motY * friction_vertical.getValue();
            velY = entityVelY + vertical.getValue();
        } else {
            velY = vertical.getValue();
        }

        if (entity.onGround) {
            velX *= ground_horizontal.getValue();
            velY *= ground_vertical.getValue();
            velZ *= ground_horizontal.getValue();
        }

        //Knockback enchantment knockback
        int enchLvl = EnchantmentManager.getEnchantmentLevel(Enchantment.KNOCKBACK.id, attacker.inventory.getItemInHand()) + 1;
        if (enchLvl > 0) {
            velX *= enchLvl;
            velZ *= enchLvl;
        }

        if (attacker.shouldDealSprintKnockback) {
            velX *= sprint_horizontal.getValue();
            velY *= sprint_vertical.getValue();
            velZ *= sprint_horizontal.getValue();
            attacker.shouldDealSprintKnockback = false;
        }
        if (attacker.isSprinting()) {
            attacker.motX *= attacker_slowdown.getValue();
            attacker.motZ *= attacker_slowdown.getValue();
            attacker.shouldDealSprintKnockback = false;

            if (stop_sprint.getValue()) {
                attacker.setSprinting(false);
            }
        }

        if (combo_mode.getValue()) {
            double yOff = entity.locY - attacker.locY;

            if (yOff > combo_height.getValue()) {
                attacker.ticksDown = MinecraftServer.currentTick;
            }
            if (yOff > combo_height.getValue() || MinecraftServer.currentTick - attacker.ticksDown < combo_ticks.getValue()) {
                velY = combo_velocity.getValue();
                velocity[1] = combo_velocity.getValue();
            }
        }

        double yOff = entity.locY - attacker.locY;
        if (limitVertical.getValue() && yOff > limitVerticalValue.getValue()) {
            velY = 0.0D;
        }

        if (entity.getBukkitEntity().hasMetadata("NPC") || entity.velocityChanged && entity instanceof EntityPlayer) {
            EntityPlayer attackedPlayer = (EntityPlayer) entity;
            Vector vector = new Vector(velX, velY, velZ);

            if (SettingsConfig.FIX_ENCHANT_KB) {
                vector = this.reduceEnchant(attackedPlayer.getBukkitEntity(), vector);
            }
            this.callVelocity(attackedPlayer, vector, velocity);
        }
    }

    /**
     * This method applies friction and damage ticks, it is
     * utilized in EntityLiving.class
     *
     * @param entity   {@link Entity} The Entity getting Attacked
     * @param attacker {@link Entity} The Entity attacking
     */
    public void attackLiving(Entity entity, EntityPlayer attacker, double x, double z, DamageSource damageSource) {
        if (entity.random.nextDouble() >= ((EntityLiving)entity).getAttributeInstance(GenericAttributes.c).getValue()) {
            entity.ai = true;

            float magnitude = MathHelper.sqrt((x * x) + (z * z));
            double verticalLimit = 0.4000000059604645D;

            // Projectile knockback handled
            if (damageSource instanceof EntityDamageSourceIndirect) {
                this.dealProjectileKnockback(entity, x, z, damageSource);
                return;
            }

            // We keep it relatively normal and default here xd
            float h = 0.4F;
            float v = 0.4F;

            double knockbackReductionHorizontal = 0.0;
            double knockbackReductionVertical = 0.0;

            double frictionHorizontal = 2.0D - knockbackReductionHorizontal;
            double frictionVertical = (2.0D - knockbackReductionVertical) - 0.25D;

            h *= (1.0D - knockbackReductionHorizontal);
            v *= (1.0D - knockbackReductionVertical);

            entity.motX /= frictionHorizontal;
            entity.motY /= frictionVertical;
            entity.motZ /= frictionHorizontal;

            entity.motX -= x / (double) magnitude * (double) h;
            entity.motY += v;
            entity.motZ -= z / (double) magnitude * (double) v;

            if (entity.motY > verticalLimit) {
                entity.motY = verticalLimit;
            }
        }
    }
}
