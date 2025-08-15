package io.github.cspigot.api.knockback;

import org.bukkit.entity.Entity;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/4/2021
 * @version cSpigot
 */

public interface IKnockback<E, P, T> {

    /**
     * The main method that handles all the knockback upon getting
     * hit and also manages PlayerVelocityEvent
     *
     * @param attacker              {@link Entity} The Entity attacking
     * @param player                {@link Entity} The Entity getting attacked
     * @param knockbackEnchantLevel {@link Integer} the knockback level if there is one otherwise its 0
     * @param velocity              {@link Double} An array of the player's velocity
     */
    void attackEntity(E attacker, P player, int knockbackEnchantLevel, double[] velocity);

    /**
     * This method applies friction and damage ticks, it is
     * utilized in EntityLiving.class
     *
     * @param entity {@link Entity} The Entity getting attacked
     */
    void attackLiving(P entity, E attacker, double x, double z, T damageSource);

}
