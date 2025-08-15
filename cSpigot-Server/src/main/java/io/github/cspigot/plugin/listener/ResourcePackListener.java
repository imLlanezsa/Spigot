package io.github.cspigot.plugin.listener;

import net.minecraft.server.EntityPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 2/17/2024
 */
public class ResourcePackListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!SettingsConfig.RESOURCE_PACK_SYSTEM || (!SettingsConfig.RESOURCE_KICK_ON_FAIL && !SettingsConfig.RESOURCE_KICK_ON_DENY)) return;

        Player player = event.getPlayer();
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer.waitingForResponse) {
            entityPlayer.waitingForResponse = false;
            player.kickPlayer(CC.translate(SettingsConfig.KICK_MESSAGE));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!SettingsConfig.RESOURCE_PACK_SYSTEM || (!SettingsConfig.RESOURCE_KICK_ON_FAIL && !SettingsConfig.RESOURCE_KICK_ON_DENY)) return;

        Player player = event.getPlayer();
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer.waitingForResponse) {
            entityPlayer.waitingForResponse = false;
            player.kickPlayer(CC.translate(SettingsConfig.KICK_MESSAGE));
        }
    }

}
