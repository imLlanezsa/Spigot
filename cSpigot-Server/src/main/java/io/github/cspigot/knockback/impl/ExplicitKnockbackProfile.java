package io.github.cspigot.knockback.impl;

import net.minecraft.server.*;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;

/**
 * This Project is property of Refine Development © 2021 2023
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 9/30/2022
 * @version cSpigot
 */

public class ExplicitKnockbackProfile extends KnockbackProfile {

    private final KnockbackValue<Boolean> onePointSevenKnockback = new KnockbackValue<>("onePointSeven", "ONE-POINT-SEVEN", "One Point Seven", "Should we use 1.7 Knockback?", 57, Boolean.class, false);
    private final KnockbackValue<Double> horizontal = new KnockbackValue<>("horizontal", "HORIZONTAL", "Horizontal", "Horizontal Multiplier", 70, Double.class, 0.44452682D);
    private final KnockbackValue<Double> vertical = new KnockbackValue<>("vertical", "VERTICAL", "Vertical", "Vertical Value", 69, Double.class, 0.3555D);

    private final KnockbackValue<Double> extraH = new KnockbackValue<>("extraH", "EXTRA-H", "Extra Horizontal", "", 67, Double.class, 0.435D);
    private final KnockbackValue<Double> extraV = new KnockbackValue<>("extraV", "EXTRA-V", "Extra Vertical", "", 65, Double.class, 0.0D);

    private final KnockbackValue<Double> frictionH = new KnockbackValue<>("frictionh", "FRICTION-H", "Friction Horizontal", "Friction values, actual knockback is divided by this", 64, Double.class, 2.0D);
    private final KnockbackValue<Double> frictionV = new KnockbackValue<>("frictionv", "FRICTION-V", "Friction Vertical", "", 63, Double.class, 2.0D);

    private final KnockbackValue<Double> groundH = new KnockbackValue<>("groundh", "GROUND-H", "Ground Horizontal Multiplier", "When player is hit but on ground, this value is multiplied with initial kb", 62, Double.class, 1.2D);
    private final KnockbackValue<Double> groundV = new KnockbackValue<>("groundv", "GROUND-V", "Ground Vertical Multiplier", "", 61, Double.class, 1.0D);

    private final KnockbackValue<Double> sprintH = new KnockbackValue<>("sprinth", "SPRINT-H", "Sprint Horizontal Multiplier", "When player is hit but attacker is spriting, this value is multiplied with initial kb", 60, Double.class, 1.2D);
    private final KnockbackValue<Double> sprintV = new KnockbackValue<>("sprintv", "SPRINT-V", "Sprint Vertical Multiplier", "", 59, Double.class, 1.0D);

    private final KnockbackValue<Boolean> limitVertDifference = new KnockbackValue<>("limitVertDifference", "LIMIT-Y-DIFFERENCE", "Limit Vertical Difference", "Should we limit y limit based on distance between player and opponent?", 58, Boolean.class, true);
    private final KnockbackValue<Double> limitDifference = new KnockbackValue<>("limitYValue", "MAX-Y-DIFFERENCE", "Max Vertical Difference", "Maximum y level difference between player and opponent.", 57, Double.class, 1.2D);
    private final KnockbackValue<Double> verticalLimit = new KnockbackValue<>("verticalLimit", "Y-LIMIT", "Vertical Limit", "Y-Axis Limit for a player's velocity", 53, Double.class, 0.4000000059604645D);
    private final KnockbackValue<Boolean> limitHorizontal = new KnockbackValue<>("limitHor", "LIMIT-HORIZONTAL", "Limit Horizontal", "Should we limit horizontal movement?", 54, Boolean.class, false);
    private final KnockbackValue<Double> horizontalLimit = new KnockbackValue<>("horizontalLimit", "H-LIMIT", "Horizontal Limit", "X/Z-Axis Limit for a player's velocity", 53, Double.class, 0.45D);

    private final KnockbackValue<Boolean> stopSprint = new KnockbackValue<>("stopsprint", "STOP-SPRINT", "StopSprint", "Should the attacker have their sprint reset upon attacking", 56, Boolean.class, true);
    private final KnockbackValue<Double> slowdown = new KnockbackValue<>("slowdown", "SLOWDOWN", "Attacker Slowdown (AutoWtap)", "This value is multiplied in horizontal axis after calculations and sprint reset", 55, Double.class, 0.2D);

    public ExplicitKnockbackProfile(String name) {
        super(name, KnockbackType.EXPLICIT);

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
        double yaw = Math.toRadians(attacker.yaw);

        boolean onePointSeven = onePointSevenKnockback.getValue();
        boolean sameDirection = attacker.positiveXMovement == ((EntityPlayer)entity).positiveXMovement && attacker.positiveZMovement == ((EntityPlayer)entity).positiveZMovement;

        if (onePointSeven && !sameDirection || knockbackEnchantLevel > 0) {
            entity.g(
                    -Math.sin(yaw * knockbackEnchantLevel) * extraH.getValue(),
                    extraV.getValue(),
                    Math.cos(yaw * knockbackEnchantLevel) * extraH.getValue()
            );
        }

        attacker.motX *= slowdown.getValue();
        attacker.motZ *= slowdown.getValue();

        if (attacker.isSprinting() && stopSprint.getValue()) {
            attacker.setSprinting(false);
            attacker.shouldDealSprintKnockback = false;
        }

        if (entity.getBukkitEntity().hasMetadata("NPC") || entity.velocityChanged) {
            EntityPlayer attackedPlayer = (EntityPlayer) entity;
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

            if (entity.onGround) {
                h *= groundH.getValue();
                v *= groundV.getValue();
                h *= groundH.getValue();
            }

            if (attacker.shouldDealSprintKnockback) {
                h *= sprintH.getValue();
                v *= sprintV.getValue();
                h *= sprintH.getValue();
                attacker.shouldDealSprintKnockback = false;
            }

            if (frictionH.getValue() != 0) {
                entity.motX /= frictionH.getValue();
                entity.motZ /= frictionH.getValue();
            }

            if (frictionV.getValue() != 0) {
                entity.motY /= frictionV.getValue();
            }

            entity.motX -= x / (double) magnitude * h;
            entity.motY += v;
            entity.motZ -= z / (double) magnitude * h;

            double yOff = entity.locY - attacker.locY;

            if (limitVertDifference.getValue()) {
                if (yOff > limitDifference.getValue()) {
                    entity.motY = 0.0;
                }
            } else {
                if (entity.motY > verticalLimit.getValue()) {
                    entity.motY = verticalLimit.getValue();
                }
            }

            if (limitHorizontal.getValue()) {
                if (Math.abs(entity.motX) > horizontalLimit.getValue()) entity.motX = entity.motX > 0 ? horizontalLimit.getValue() : -horizontalLimit.getValue();
                if (Math.abs(entity.motZ) > horizontalLimit.getValue()) entity.motZ = entity.motZ > 0 ? horizontalLimit.getValue() : -horizontalLimit.getValue();
            }
        }
    }
}
