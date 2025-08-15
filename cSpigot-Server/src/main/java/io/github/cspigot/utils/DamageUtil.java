package io.github.cspigot.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.server.DamageSource;
import net.minecraft.server.EntityDamageSource;
import net.minecraft.server.EntityDamageSourceIndirect;
import net.minecraft.server.EntityPlayer;
import org.bukkit.entity.Player;

/**
 * Utility class for damage-related operations.
 *
 * @author mathssw, Sakiio_
 * @version cSpigot
 * @since 03/05/2025
 */

@UtilityClass
public class DamageUtil {

    /**
     * Determines if the damage source is a direct player attack.
     *
     * @param damageSource The source of damage to check
     * @return true if the damage is directly from a player, false otherwise
     */
    public boolean isPlayerAttack(DamageSource damageSource) {
        if (damageSource == null) return false;
        if (!(damageSource instanceof EntityDamageSource)) return false;
        if (damageSource instanceof EntityDamageSourceIndirect) return false;

        EntityDamageSource entityDamageSource = (EntityDamageSource) damageSource;
        if (entityDamageSource.p().contains("explosion")) return false;

        return entityDamageSource.getEntity() instanceof EntityPlayer;
    }
}