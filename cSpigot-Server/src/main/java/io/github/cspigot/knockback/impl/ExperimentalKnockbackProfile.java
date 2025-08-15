package io.github.cspigot.knockback.impl;

import net.minecraft.server.*;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.knockback.KnockbackProfile;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 2/20/2024
 */
public class ExperimentalKnockbackProfile extends KnockbackProfile {

    private final KnockbackValue<Double> horizontal = new KnockbackValue<>("horizontal", "HORIZONTAL", "Horizontal", "Horizontal Multiplier", 70, Double.class, 0.9055D);
    private final KnockbackValue<Double> vertical = new KnockbackValue<>("vertical", "VERTICAL", "Vertical", "Vertical Value", 69, Double.class, 0.25635D);
    private final KnockbackValue<Integer> sprintMultiplier = new KnockbackValue<>("sprintMultiplier", "SPRINT-MULTIPLIER", "Sprint Multiplier", "Sprint Multiplier Value", 69, Integer.class, 1);
    private final KnockbackValue<Double> rangeFactor = new KnockbackValue<>("rangeFactor", "RANGE-FACTOR", "Range Factor", "Range Factor Value", 68, Double.class, 0.025D);
    private final KnockbackValue<Double> maxRangeReduction = new KnockbackValue<>("maxRangeReduction", "MAX-RANGE-REDUCTION", "Max Range Reduction", "Max Range Reduction Value", 67, Double.class, 1.2D);
    private final KnockbackValue<Double> startRangeReduction = new KnockbackValue<>("startRangeReduction", "START-RANGE-REDUCTION", "Start Range", "Starting Range", 66, Double.class, 3.0D);
    //private final KnockbackValue<Double> idleReduction = new KnockbackValue<>("idleReduction", "IDLE-REDUCTION", "Idle Reduction", "Idle Reduction Value", 65, Double.class, 0.6D);
    private final KnockbackValue<Double> minRange = new KnockbackValue<>("minRange", "MIN-RANGE", "Min Range", "Min Range Value", 64, Double.class, 0.12D);
    //private final KnockbackValue<Double> horizontalFriction = new KnockbackValue<>("horizontalFriction", "H-FRICTION", "Horizontal Friction", "Horizontal Friction Value", 63, Double.class, 1.0D);
    //private final KnockbackValue<Double> verticalFriction = new KnockbackValue<>("verticalFriction", "V-FRICTION", "Vertical Friction", "Vertical Friction Value", 62, Double.class, 60.0D);
    private final KnockbackValue<Boolean> verticalLimit = new KnockbackValue<>("verticalLimit", "V-LIMIT-ENABLED", "Vertical Limit Enabled", "Should vertical limit be enabled?", 61, Boolean.class, true);
    private final KnockbackValue<Double> verticalLimitValue = new KnockbackValue<>("verticalLimitValue", "V-LIMIT-VALUE", "Vertical Limit Value", "Vertical limit value, the knockback locks at this vertical motion value", 60, Double.class, 4.0D);


    public ExperimentalKnockbackProfile(String name) {
        super(name, KnockbackType.EXPERIMENTAL);
        this.loadValues();
    }

    /**
     * The main method that handles all the knockback upon getting
     * hit and also manages PlayerVelocityEvent
     *
     * @param attacker              {@link Entity} The Entity attacking
     * @param entity                {@link Entity} The Entity getting attacked
     * @param knockbackEnchantLevel {@link Integer} the knockback level if there is one otherwise its 0
     * @param velocity              {@link Double} An array of the player's velocity
     */
    public void attackEntity(EntityPlayer attacker, Entity entity, int knockbackEnchantLevel, double[] velocity) {
        entity.g(
                -MathHelper.sin(attacker.yaw * (float) Math.PI / 180.0f) * sprintMultiplier.getValue() * 0.5f,
                0.1,
                MathHelper.cos(attacker.yaw * (float) Math.PI / 180.0f) * sprintMultiplier.getValue() * 0.5f
        );
        attacker.canSprintKB = false;
        SettingsConfig.sendDebug("Sending sprint kb ");

        if (entity instanceof EntityPlayer && entity.velocityChanged) {
            EntityPlayer attackedPlayer = (EntityPlayer) entity;

            // Citizens overrides the entity#g method to apply its Events and calculations
            // This breaks our instant-client-packet for knockback system
            // So for NPCs, we just let it be done with normal bukkit ways.
            if (attackedPlayer.getBukkitEntity().hasMetadata("NPC") || attackedPlayer.getBukkitEntity().hasMetadata("bolt-npc")) {
                Vector botVector = attackedPlayer.getBukkitEntity().getVelocity();
                // Reset the motion variables
                // because the method below does += not =
                attackedPlayer.motX = 0;
                attackedPlayer.motY = 0;
                attackedPlayer.motZ = 0;

                attackedPlayer.g(botVector.getX(), botVector.getY(), botVector.getZ());
                attackedPlayer.playerConnection.lastMotionTick = MinecraftServer.currentTick;
                // Remove all fall distance damage, because sometimes kb
                // can go a bit high and this just ruins kb
                if (attackedPlayer.fallDistance > 0) {
                    attackedPlayer.fallDistance = 0;
                }
                return;
            }

            Vector vector = new Vector(velocity[0], velocity[1], velocity[2]);
            PlayerVelocityEvent event = new PlayerVelocityEvent(attackedPlayer.getBukkitEntity(), vector);
            Bukkit.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                if (!vector.equals(event.getVelocity())) {
                    attackedPlayer.getBukkitEntity().setVelocity(vector);
                }
                attackedPlayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(attackedPlayer));

                // Turn off fall damage
                if (attackedPlayer.fallDistance > 0) {
                    attackedPlayer.fallDistance = 0.0f;
                }
            }
            attackedPlayer.playerConnection.lastMotionTick = MinecraftServer.currentTick;

            attackedPlayer.velocityChanged = false;
            attackedPlayer.motX = velocity[0];
            attackedPlayer.motY = velocity[1];
            attackedPlayer.motZ = velocity[2];
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
        Entity opponent = damageSource.getEntity();

        entity.ai = true;

        double distance = this.distance(opponent, entity);
        double rangeReduction = this.calculateModifiedRange(distance);

        double knockbackHorizontal = horizontal.getValue();
        double knockbackVertical = vertical.getValue();

        double frictionHorizontal = 2.0 - (1.0 - knockbackHorizontal);
        //double frictionVertical = 2.0 - (1.0 - knockbackVertical);

        float f2 = (float) (0.4D * (1.0D - 0.4D * (1.0D - knockbackHorizontal)));

        entity.motX /= frictionHorizontal;
        entity.motZ /= frictionHorizontal;
        //entity.motY /= this.verticalFriction.getValue();
        entity.motY = knockbackVertical;

        float f1 = MathHelper.sqrt(x * x + z * z);
        entity.motX -= x / (double)f1 * ((double)f2 - rangeReduction);
        entity.motZ -= z / (double)f1 * ((double)f2 - rangeReduction);

        double value = this.verticalLimitValue.getValue();
        if (verticalLimit.getValue() && entity.motY > value) {
            entity.motY = value;
        }

        SettingsConfig.sendDebug("Sending entity living kb");
    }

    public double distance(Entity entity1, Entity entity2) {
        CraftEntity entity = entity1.getBukkitEntity();
        CraftEntity target = entity2.getBukkitEntity();

        if (entity.getWorld() != target.getWorld()) return -1.0D;
        if (entity == target) return 0D;

        return entity.getLocation().distance(target.getLocation());
    }

    private double calculateModifiedRange(double distance) {
        if (distance <= this.startRangeReduction.getValue()) {
            return 0.0;
        }

        double knockbackRangeFactor = this.rangeFactor.getValue();
        double maxRangeReduction = this.maxRangeReduction.getValue();

        double modifiedRange = knockbackRangeFactor * (distance - maxRangeReduction);
        return Math.min(modifiedRange, minRange.getValue());
    }
}
