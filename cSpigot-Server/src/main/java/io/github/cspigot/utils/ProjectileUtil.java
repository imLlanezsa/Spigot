package io.github.cspigot.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MobEffect;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@UtilityClass
public final class ProjectileUtil {

    public final BlockFace[] FACES = { BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST };

    public PotionEffect toBukkit(MobEffect effect) {
        return new PotionEffect(PotionEffectType.getById(effect.getEffectId()), effect.getDuration(), effect.getAmplifier(), effect.isAmbient());
    }

    public BlockFace direction(Location l) {
        return FACES[Math.round(l.getYaw() / 45f) & 0x7];
    }

    public static MobEffect cloneWithDuration(MobEffect effect, int duration) {
        return new MobEffect(effect.getEffectId(),
                duration,
                effect.getAmplifier(),
                effect.isAmbient(), effect.isShowParticles());
    }

    public static void extendDuration(MobEffect effect, int duration) {
        effect.a(cloneWithDuration(effect, duration));
    }

    public static CraftPlayer getLastAttacker(Player p) {
        EntityLiving lastAttacker = ((CraftPlayer)p).getHandle().lastDamager;
        return !(lastAttacker instanceof EntityPlayer) ? null : ((EntityPlayer)lastAttacker).getBukkitEntity();
    }
}
