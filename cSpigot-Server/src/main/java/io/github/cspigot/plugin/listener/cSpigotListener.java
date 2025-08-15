package io.github.cspigot.plugin.listener;

import net.minecraft.server.MathHelper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.plugin.ServerSidePlugin;
import io.github.cspigot.utils.CC;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 1/26/2023
 * @version cSpigot
 */

public class cSpigotListener implements Listener {


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArrowVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        if (player.getLastDamageCause() == null) return;

        // Per Knockback configuration
        KnockbackProfile profile = (KnockbackProfile) player.cSpigot().getKnockbackProfile();
        if (!profile.getStraightBow().getValue()) return;

        Vector velocity = event.getVelocity();
        EntityDamageEvent damageEvent = player.getLastDamageCause();
        if (damageEvent == null || damageEvent.isCancelled() || !(damageEvent instanceof EntityDamageByEntityEvent)) return;

        // Need it to be a EntityDamageByEntityEvent to get the attacker entity
        Entity attacker = ((EntityDamageByEntityEvent) damageEvent).getDamager();
        if (!(attacker instanceof Arrow)) return;

        Arrow arrow = (Arrow) attacker;
        // Arrow needs to be self shot
        if (!arrow.getShooter().equals(player)) return;

        // We add 0.2 at the end to increase the launch distance, it's very low for vanilla instances
        double magnitude = MathHelper.sqrt(Math.pow(velocity.getX(), 2) + Math.pow(velocity.getZ(), 2)) + 0.2;
        // Unit vector for direction of where arrow was heading
        Vector dir = arrow.getLocation().getDirection().normalize();

        double x = dir.getX() * magnitude * -1.0D;
        double z = dir.getZ() * magnitude;

        event.setVelocity(new Vector(x, velocity.getY(), z));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEmpty(PlayerBucketEmptyEvent event) {
        if (!Bukkit.getPluginManager().isPluginEnabled("mBedwars")) return;

        Runnable wrapper = () -> event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
        Bukkit.getScheduler().runTaskLater(ServerSidePlugin.getInstance(), wrapper, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onResourcePack(PlayerResourcePackStatusEvent event) {
        if (!SettingsConfig.RESOURCE_PACK_SYSTEM) return;

        Bukkit.getScheduler().runTask(ServerSidePlugin.getInstance(), () -> {
            switch (event.getStatus()) {
                case DECLINED: {
                    if (SettingsConfig.RESOURCE_KICK_ON_DENY) {
                        Player player = event.getPlayer();
                        player.kickPlayer(CC.translate(SettingsConfig.KICK_MESSAGE));
                    }
                    break;
                }
                case FAILED_DOWNLOAD: {
                    if (SettingsConfig.RESOURCE_KICK_ON_FAIL) {

                        Player player = event.getPlayer();
                        player.kickPlayer(CC.translate(SettingsConfig.KICK_MESSAGE));
                    }
                    break;
                }
            }
        });
    }
}
