package io.github.cspigot.knockback.impl;

import net.minecraft.server.*;

import org.bukkit.util.Vector;

import io.github.cspigot.cSpigot;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.knockback.KnockbackHandler;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/4/2021
 * @version cSpigot
 */

@SuppressWarnings("all")
public class DetailedKnockbackProfile extends KnockbackProfile {

    private final KnockbackValue<Double> horizontal = new KnockbackValue<>("horizontal", "HORIZONTAL", "Horizontal", "Horizontal Multiplier", 70, Double.class, 0.44452682D);
    private final KnockbackValue<Double> vertical = new KnockbackValue<>("vertical", "VERTICAL", "Vertical", "Vertical Value", 69, Double.class, 0.3555D);

    private final KnockbackValue<Boolean> inheritH = new KnockbackValue<>("inherith", "INHERIT-H-ENABLED", "Inherit Horizontal", "Add a certain value to horizontal before actual calculations", 68, Boolean.class, true);
    private final KnockbackValue<Double> ihstrH = new KnockbackValue<>("ihstrh", "INHERIT-H-VALUE", "Inherit Strength Horizontal", "", 67, Double.class, 1.4D);

    private final KnockbackValue<Boolean> inheritV = new KnockbackValue<>("inheritv", "INHERIT-V-ENABLED", "Inherit Vertical", "Add a certain value to vertical before actual calculations", 66, Boolean.class, false);
    private final KnockbackValue<Double> ihstrV = new KnockbackValue<>("ihstrv", "INHERIT-V-VALUE", "Inherit Strength Vertical", "", 65, Double.class, 1.0D);

    private final KnockbackValue<Double> frictionH = new KnockbackValue<>("frictionh", "FRICTION-H", "Friction Horizontal", "Friction values, actual knockback is divided by this", 64, Double.class, 2.0D);
    private final KnockbackValue<Double> frictionV = new KnockbackValue<>("frictionv", "FRICTION-V", "Friction Vertical", "", 63, Double.class, 2.0D);

    private final KnockbackValue<Double> groundH = new KnockbackValue<>("groundh", "GROUND-H", "Ground Horizontal Multiplier", "When player is hit but on ground, this value is multiplied with initial kb", 62, Double.class, 1.2D);
    private final KnockbackValue<Double> groundV = new KnockbackValue<>("groundv", "GROUND-V", "Ground Vertical Multiplier", "", 61, Double.class, 1.0D);

    private final KnockbackValue<Double> sprintH = new KnockbackValue<>("sprinth", "SPRINT-H", "Sprint Horizontal Multiplier", "When player is hit but attacker is spriting, this value is multiplied with initial kb", 60, Double.class, 1.2D);
    private final KnockbackValue<Double> sprintV = new KnockbackValue<>("sprintv", "SPRINT-V", "Sprint Vertical Multiplier", "", 59, Double.class, 1.0D);

    private final KnockbackValue<Boolean> limitVertical = new KnockbackValue<>("limitvert", "Y-LIMIT", "Limit Vertical", "Should we dynamically limit player's vertical axis?", 58, Boolean.class, true);
    private final KnockbackValue<Double> ylimit = new KnockbackValue<>("ylimit", "Y-LIMIT-VALUE", "Vertical Limit", "Dynamic vertical axis limit value", 57, Double.class, 1.2D);

    private final KnockbackValue<Boolean> stopSprint = new KnockbackValue<>("stopsprint", "STOP-SPRINT", "StopSprint", "Should the attacker have their sprint reset upon attacking", 56, Boolean.class, true);
    private final KnockbackValue<Double> slowdown = new KnockbackValue<>("slowdown", "SLOWDOWN", "Attacker Slowdown (AutoWtap)", "This value is multiplied in horizontal axis after calculations and sprint reset", 55, Double.class, 0.2D);

    private final KnockbackValue<Boolean> comboMode = new KnockbackValue<>("combo", "COMBO.ENABLED", "Combo Mode", "Toggle combo mode", 54, Boolean.class, false);
    private final KnockbackValue<Integer> comboTicks = new KnockbackValue<>("comboticks", "COMBO.TICKS", "Combo Ticks", "This is used to track when to move player back down from y axis", 53, Integer.class, 10);
    private final KnockbackValue<Double> comboVelocity = new KnockbackValue<>("combovelocity", "COMBO.VELOCITY", "Combo Velocity", "Horizontal velocity of a player in combo mode", 52, Double.class, -0.05D);
    private final KnockbackValue<Double> comboHeight = new KnockbackValue<>("comboheight", "COMBO.HEIGHT", "Combo Height", "Vertical velocity of a player in combo mode", 51, Double.class, 2.5D);

    public DetailedKnockbackProfile(String name) {
        super(name, KnockbackType.DETAILED);
        this.loadValues();
    }

    /**
     * The main method that handles all the knockback upon getting
     * hit and also manages PlayerVelocityEvent
     *
     * @param attacker              {@link EntityPlayer} The Entity attacking
     * @param entity                {@link Entity} The Entity getting attacked
     * @param knockbackEnchantLevel {@link Integer} the knockback level if there is one otherwise its 0
     * @param velocity              {@link Double} An array of the player's velocity
     */
    @Override
    public void attackEntity(EntityPlayer attacker, Entity entity, int knockbackEnchantLevel, double[] velocity) {
        double velX, velY, velZ;

        if (inheritH.getValue()) {
            double entityVelX = entity.motX * ihstrH.getValue();
            double entityVelZ = entity.motZ * ihstrH.getValue();

            velX = entityVelX + Math.sin(Math.toRadians(attacker.yaw)) * -1.0;
            velZ = entityVelZ + Math.cos(Math.toRadians(attacker.yaw));
        } else {
            velX = Math.sin(Math.toRadians(attacker.getHeadRotation())) * -1.0;
            velZ = Math.cos(Math.toRadians(attacker.getHeadRotation()));
        }

        velX *= horizontal.getValue();
        velZ *= horizontal.getValue();

        if (inheritV.getValue()) {
            double entityVelY = entity.motY * ihstrV.getValue();
            velY = entityVelY + vertical.getValue();
        } else {
            velY = vertical.getValue();
        }

        if (entity.onGround) {
            velX *= groundH.getValue();
            velY *= groundV.getValue();
            velZ *= groundH.getValue();
        }

        //Knockback enchantment knockback
        int enchLvl = EnchantmentManager.getEnchantmentLevel(Enchantment.KNOCKBACK.id, attacker.inventory.getItemInHand()) + 1;
        if (enchLvl > 0) {
            velX *= enchLvl;
            velZ *= enchLvl;
        }

        if (attacker.shouldDealSprintKnockback) {
            velX *= sprintH.getValue();
            velY *= sprintV.getValue();
            velZ *= sprintH.getValue();
            attacker.shouldDealSprintKnockback = false;
        }

        if (attacker.isSprinting()) {
            attacker.motX *= slowdown.getValue();
            attacker.motZ *= slowdown.getValue();
            attacker.shouldDealSprintKnockback = false;

            if (stopSprint.getValue()) {
                attacker.setSprinting(false);
            }
        }

        if (comboMode.getValue()) {
            double yOff = entity.locY - attacker.locY;

            if (yOff > comboHeight.getValue()) {
                attacker.ticksDown = MinecraftServer.currentTick;
            }
            if (yOff > comboHeight.getValue() || MinecraftServer.currentTick - attacker.ticksDown < comboTicks.getValue()) {
                velY = comboVelocity.getValue();
                velocity[1] = comboVelocity.getValue();
            }
        }

        double yOff = entity.locY - attacker.locY;
        if (limitVertical.getValue() && yOff > ylimit.getValue()) {
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

            // Legacy knockback support
            KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
            float mutliplier = knockbackHandler.getVersion() <= 3 ? 0.15F : 0.4F;

            if (frictionH.getValue() != 0) {
                entity.motX /= frictionH.getValue();
                entity.motZ /= frictionH.getValue();
            }

            if (frictionV.getValue() != 0) {
                entity.motY /= frictionV.getValue();
            }

            entity.motX -= x / (double) magnitude * (double) mutliplier;
            entity.motY += mutliplier;
            entity.motZ -= z / (double) magnitude * (double) mutliplier;

            if (entity.motY > verticalLimit) {
                entity.motY = verticalLimit;
            }
        }
    }
}
