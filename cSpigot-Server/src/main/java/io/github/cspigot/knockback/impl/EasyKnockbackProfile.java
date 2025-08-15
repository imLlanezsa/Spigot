package io.github.cspigot.knockback.impl;

import net.minecraft.server.*;
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

public class EasyKnockbackProfile extends KnockbackProfile {

    private final KnockbackValue<Boolean> onePointSevenKnockback = new KnockbackValue<>("onePointSeven", "ONE-POINT-SEVEN", "One Point Seven", "Should we use 1.7 Knockback?", 57, Boolean.class, false);
    private final KnockbackValue<Double> horizontal = new KnockbackValue<>("horizontal", "HORIZONTAL", "Horizontal", "Horizontal Multiplier", 56, Double.class, 0.4D);
    private final KnockbackValue<Double> vertical = new KnockbackValue<>("vertical", "VERTICAL", "Vertical", "Vertical Value", 55, Double.class, 0.4D);

    private final KnockbackValue<Double> extraHorizontal = new KnockbackValue<>("extraHorizontal", "EXTRA-HORIZONTAL", "Extra Horizontal", "Add a certain value to horizontal/vertical before actual calculations", 54, Double.class, 0.5D);
    private final KnockbackValue<Double> extraVertical = new KnockbackValue<>("extraVertical", "EXTRA-VERTICAL", "Extra Vertical", "", 53, Double.class, 0.1D);

    private final KnockbackValue<Double> friction = new KnockbackValue<>("friction", "FRICTION", "Friction", "Friction Value (Knockback is divided by this)", 52, Double.class, 2.0D);
    private final KnockbackValue<Double> verticalLimit = new KnockbackValue<>("verticalLimit", "Y-LIMIT", "Vertical Limit", "Y-Axis Limit for a player's velocity", 51, Double.class, 0.4000000059604645D);
    private final KnockbackValue<Boolean> dynamicLimit = new KnockbackValue<>("dynamicYLimit", "DYNAMIC-LIMIT", "Dynamic Y-Limit", "Should the vertical velocity be set to 0 after reaching limit?", 50, Boolean.class, false);
    private final KnockbackValue<Boolean> limitHorizontal = new KnockbackValue<>("limitHor", "LIMIT-HORIZONTAL", "Limit Horizontal", "Should we limit horizontal movement?", 49, Boolean.class, false);
    private final KnockbackValue<Double> horizontalLimit = new KnockbackValue<>("horizontalLimit", "H-LIMIT", "Horizontal Limit", "X/Z-Axis Limit for a player's velocity", 48, Double.class, 0.45D);


    public EasyKnockbackProfile(String name) {
        super(name, KnockbackType.EASY);

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
        boolean onePointSeven = onePointSevenKnockback.getValue();
        boolean sameDirection = attacker.positiveXMovement == ((EntityPlayer)entity).positiveXMovement && attacker.positiveZMovement == ((EntityPlayer)entity).positiveZMovement;

        if (onePointSeven && !sameDirection || knockbackEnchantLevel > 0) {
            entity.g(
                    (double) -MathHelper.sin((float) (attacker.yaw * 3.1415927F / 180.0D)) * (knockbackEnchantLevel) * extraHorizontal.getValue(),
                    extraVertical.getValue(),
                    (double) MathHelper.cos((float) (attacker.yaw * 3.1415927F / 180.0D)) * (knockbackEnchantLevel) * extraHorizontal.getValue());
        }

        attacker.motX *= 0.6D;
        attacker.motZ *= 0.6D;
        attacker.setSprinting(false);

        if (entity.getBukkitEntity().hasMetadata("NPC") || entity.velocityChanged && entity instanceof EntityPlayer) {
            EntityPlayer attackedPlayer = (EntityPlayer) entity;
            if (entity.motY >= verticalLimit.getValue() && dynamicLimit.getValue()) {
                velocity[1] = 0.0D;
            }

            this.callVelocity(attackedPlayer, attackedPlayer.getBukkitEntity().getVelocity(), velocity);
        }
    }

    /**
     * This method applies friction and damage ticks, it is
     * utilized in EntityLiving
     *
     * @param entity   {@link Entity} The Entity getting Attacked
     * @param attacker {@link Entity} The Entity attacking
     */
    public void attackLiving(Entity entity, EntityPlayer attacker, double x, double z, DamageSource damageSource) {
        if (entity.random.nextDouble() >= ((EntityLiving)entity).getAttributeInstance(GenericAttributes.c).getValue()) {
            entity.ai = true;
            float magnitude = MathHelper.sqrt((x * x) + (z * z));

            // Projectile knockback handled
            if (damageSource instanceof EntityDamageSourceIndirect) {
                this.dealProjectileKnockback(entity, x, z, damageSource);
                return;
            }

            double h = horizontal.getValue();
            double v = vertical.getValue();

            if (friction.getValue() != 0) {
                entity.motX /= friction.getValue();
                entity.motY /= friction.getValue();
                entity.motZ /= friction.getValue();
            }

            entity.motX -= x / (double) magnitude * h;
            entity.motY += v;
            entity.motZ -= z / (double) magnitude * h;

            if (entity.motY > verticalLimit.getValue()) {
                entity.motY = verticalLimit.getValue();
            }

            if (limitHorizontal.getValue()) {
                if (entity.motX > horizontalLimit.getValue()) entity.motX = horizontalLimit.getValue();
                if (entity.motZ > horizontalLimit.getValue()) entity.motZ = horizontalLimit.getValue();
            }
        }
    }
}
