package io.github.cspigot.cmds.impl.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.PluginManager;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 4/1/2023
 * @version cSpigot
 */

@Register(name = "chunkunload", aliases = "unloadchunks")
public class ChunkUnloadCommand {

    @Command(name = "", aliases = "help", desc = "Force unload all unused chunks")
    @Require("cSpigot.chunkunload")
    public void unload(@Sender CommandSender sender) {
        int chunks = 0;

        PluginManager pluginManager = Bukkit.getPluginManager();
        for ( World world : Bukkit.getServer().getWorlds() ) {
            for ( Chunk chunk : world.getCachedChunks() ) {
                if (world.isChunkInUse(chunk.getX(), chunk.getZ())) continue;

                ChunkUnloadEvent event = new ChunkUnloadEvent(chunk);
                pluginManager.callEvent(event);

                if (chunk.unload(world.isAutoSave())) chunks++;
            }
        }

        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eSuccessfully unloaded " + SettingsConfig.THEME + chunks + "&e chunks"));
    }
}
