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

public class SmoothKnockbackProfile extends KnockbackProfile {

    private final KnockbackValue<Boolean> onePointSevenKnockback = new KnockbackValue<>("onePointSeven", "ONE-POINT-SEVEN", "One Point Seven", "Should we use 1.7 Knockback?", 57, Boolean.class, false);
    private final KnockbackValue<Double> horizontal = new KnockbackValue<>("horizontal", "HORIZONTAL", "Horizontal", "Horizontal Multiplier", 60, Double.class, 0.53D);
    private final KnockbackValue<Double> vertical = new KnockbackValue<>("vertical", "VERTICAL", "Vertical", "Vertical Value", 59, Double.class, 0.5D);
    private final KnockbackValue<Double> friction = new KnockbackValue<>("friction", "FRICTION", "Friction", "Friction Value (Knockback is divided by this)", 58, Double.class, 2.5D);
    private final KnockbackValue<Double> extraHorizontal = new KnockbackValue<>("extraHorizontal", "EXTRA-HORIZONTAL", "Extra Horizontal", "Add a certain value to horizontal/vertical before actual calculations", 57, Double.class, 0.435D);
    private final KnockbackValue<Double> extraVertical = new KnockbackValue<>("extraVertical", "EXTRA-VERTICAL", "Extra Vertical", "", 56, Double.class, 0.0D);
    private final KnockbackValue<Double> verticalLimit = new KnockbackValue<>("verticalLimit", "Y-LIMIT", "Vertical Limit", "Y-Axis Limit for a player's velocity", 55, Double.class, 0.36D);
    private final KnockbackValue<Boolean> limitHorizontal = new KnockbackValue<>("limitHor", "LIMIT-HORIZONTAL", "Limit Horizontal", "Should we limit horizontal movement?", 54, Boolean.class, false);
    private final KnockbackValue<Double> horizontalLimit = new KnockbackValue<>("horizontalLimit", "H-LIMIT", "Horizontal Limit", "X/Z-Axis Limit for a player's velocity", 53, Double.class, 0.45D);
    private final KnockbackValue<Boolean> dynamicLimit = new KnockbackValue<>("dynamicYLimit", "DYNAMIC-LIMIT", "Dynamic Y-Limit", "Should the vertical velocity be set to 0 after reaching limit?", 52, Boolean.class, false);

    private final KnockbackValue<Double> slowdown = new KnockbackValue<>("slowdown", "SLOWDOWN", "Attacker Slowdown", "This value is multiplied in horizontal axis after calculations and sprint reset", 51, Double.class, 0.6D);

    private final KnockbackValue<Boolean> comboMode = new KnockbackValue<>("combo", "COMBO.ENABLED", "Combo Mode", "Toggle combo mode", 50, Boolean.class, false);
    private final KnockbackValue<Double> comboVelocity = new KnockbackValue<>("combovelocity", "COMBO.VELOCITY", "Combo Velocity", "Horizontal velocity of a player in combo mode", 49, Double.class, -0.05D);
    private final KnockbackValue<Double> comboHeight = new KnockbackValue<>("comboheight", "COMBO.HEIGHT", "Combo Height", "Vertical velocity of a player in combo mode", 48, Double.class, 2.5D);


    public SmoothKnockbackProfile(String name) {
        super(name, KnockbackType.SMOOTH);
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
                    (-MathHelper.sin((float) (attacker.yaw * Math.PI / 180.0D)) * knockbackEnchantLevel) * extraHorizontal.getValue(),
                    extraVertical.getValue(),
                    (MathHelper.cos((float) (attacker.yaw * Math.PI / 180.0D)) * knockbackEnchantLevel) * extraHorizontal.getValue());
        }

        attacker.motX *= slowdown.getValue();
        attacker.motZ *= slowdown.getValue();
        attacker.setSprinting(false);

        if (comboMode.getValue()) {
            double deltaY = entity.locY - attacker.locY; // Δy
            if (deltaY > comboHeight.getValue()) {
                attacker.motY = comboVelocity.getValue();
                velocity[1] = comboVelocity.getValue();
            }
        }

        if (entity.getBukkitEntity().hasMetadata("NPC") || entity.velocityChanged) {
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

            entity.motX -= x / magnitude * h;
            entity.motY += v;
            entity.motZ -= z / magnitude * h;

            if (entity.motY > verticalLimit.getValue()) entity.motY = verticalLimit.getValue();

            if (limitHorizontal.getValue()) {
                if (Math.abs(entity.motX) > horizontalLimit.getValue()) entity.motX = entity.motX > 0 ? horizontalLimit.getValue() : -horizontalLimit.getValue();
                if (Math.abs(entity.motZ) > horizontalLimit.getValue()) entity.motZ = entity.motZ > 0 ? horizontalLimit.getValue() : -horizontalLimit.getValue();
            }
        }
    }
}
