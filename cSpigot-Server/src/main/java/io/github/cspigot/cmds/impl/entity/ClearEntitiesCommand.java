package io.github.cspigot.cmds.impl.entity;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.StringUtils;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Register(name = "killall", aliases = "clearentities")
@SuppressWarnings("unused")
public class ClearEntitiesCommand {

    private static final Logger LOGGER = Logger.getLogger(ClearEntitiesCommand.class.getName());

    private static final Set<EntityType> ENTITIES_TO_REMOVE = EnumSet.of(
            EntityType.DROPPED_ITEM,
            EntityType.BOAT,
            EntityType.PRIMED_TNT,
            EntityType.SKELETON,
            EntityType.ZOMBIE,
            EntityType.COW,
            EntityType.SHEEP,
            EntityType.ENDERMAN,
            EntityType.PIG_ZOMBIE,
            EntityType.PIG,
            EntityType.CREEPER,
            EntityType.BAT,
            EntityType.CHICKEN,
            EntityType.CAVE_SPIDER,
            EntityType.SPIDER,
            EntityType.WITCH,
            EntityType.HORSE
    );

    /**
     * Command to remove all non-player entities of specified types from all worlds
     *
     * @param sender The command sender
     */
    @Command(name = "", aliases = "help", desc = "Kill all entities")
    @Require("cSpigot.killall")
    public void kill(@Sender CommandSender sender) {
        int removedCount = 0;

        try {
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    EntityType entityType = entity.getType();

                    if (ENTITIES_TO_REMOVE.contains(entityType)) {
                        try {
                            entity.remove();
                            removedCount++;
                        } catch (Exception e) {
                            // Log errors but continue processing other entities
                            LOGGER.log(Level.WARNING, "Failed to remove entity " + entityType + " in world " +
                                    world.getName(), e);
                        }
                    }
                }
            }

            sender.sendMessage(StringUtils.PREFIX + ChatColor.GRAY + "You have removed a total of " +
                    SettingsConfig.THEME + removedCount + ChatColor.GRAY + " entities");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing killall command", e);
            sender.sendMessage(StringUtils.PREFIX + ChatColor.RED + "An error occurred while removing entities");
        }
    }
}