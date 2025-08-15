package io.github.cspigot.cmds.impl.entity;

import io.github.cspigot.api.command.annotation.*;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityTrackerEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.World;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import org.spigotmc.SpigotConfig;

import io.github.cspigot.api.command.annotation.*;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 1/25/2024
 */

@Register(name = "trackingrange")
public class TrackingRangeCommand {

    @Command(name = "player", desc = "This value determines at what range others can see this player", usage = "<player> <value>")
    @Require("cSpigot.trackingrange")
    public void setPlayerRange(@Sender CommandSender sender, Player target, int value) {
        if (value < 0) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eYou cannot set a player's tracking range to a negative value."));
            return;
        }
        if (value > 128 && value < 256) {
            sender.sendMessage(CC.translate("&e[&4!&e] &eValues above 128 can cause a very heavy impact on performance!"));
        }

        if (value > 256) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eYou cannot set a player's tracking range to a value greater than 256."));
            return;
        }

        target.cSpigot().setTrackingRange(value);
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aSuccessfully set &e" + target.getName() + "&a's tracking range to &e" + value + "&a."));
    }

    @Command(name = "global", desc = "Set a world's tracking range", usage = "global <value>")
    @Require("cSpigot.trackingrange")
    public void setGlobalRange(@Sender CommandSender sender, int value, @OptArg("false") boolean update) {
        if (value < 0) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eYou cannot set tracking range to a negative value."));
            return;
        }
        if (value > 128 && value < 256) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&e[&4!&e] &eValues above 128 can cause a very heavy impact on performance!"));
        }

        if (value > 256) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eYou cannot set tracking range to a value greater than 256."));
            return;
        }

        World world;

        if (sender instanceof ConsoleCommandSender) {
            world = MinecraftServer.getServer().worlds.get(0);
        } else {
            Player player = (Player) sender;
            world = ((CraftWorld)player.getWorld()).getHandle();
        }

        world.spigotConfig.playerTrackingRange = value;
        world.spigotConfig.set("entity-tracking-range.players", value);
        SpigotConfig.save();

        if (update) {
            world.players.forEach(player -> {
                EntityPlayer entityPlayer = ((EntityPlayer)player);
                entityPlayer.getBukkitEntity().cSpigot().setTrackingRange(value);
            });
        } else {
            sender.sendMessage(CC.translate("&7The tracking range has not been updated for the current players, use 'true' at the end of this command to update."));
        }

        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aSuccessfully set global tracking range to &e" + value + "&a."));
    }

    @Command(name = "debug", desc = "")
    @Require("cSpigot.debug")
    public void debug(@Sender Player sender) {
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aYour tracking range is &e" + sender.cSpigot().getTrackingRange() + "&a."));
        EntityTrackerEntry tracker = ((CraftPlayer)sender).getHandle().tracker;
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aYour tracker's tracking range is &e" + tracker.b + "&a."));
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aThe amount of people you are tracking &e" + tracker.trackedPlayers.size() + "&a."));
        for ( EntityPlayer trackedPlayer : tracker.trackedPlayerMap.keySet()) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&e" + trackedPlayer.getName() + "&a's tracking range is &e" + trackedPlayer.tracker.b + "&a."));
        }

    }
}
