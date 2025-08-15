package io.github.cspigot.cmds.impl.bukkit;

import com.google.common.collect.ImmutableList;
import net.minecraft.server.CommandAbstract;
import net.minecraft.server.GameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.World;

import org.apache.commons.lang.Validate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2023, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 12/4/2023
 */

public class GameruleCommand extends Command {

    public GameruleCommand() {
        super("gamerule");
        this.setUsage("/gamerule <gamerule> <value>");
        this.setPermission("minecraft.command.gamerule");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) return true;

        if (args.length == 0) {
            sender.sendMessage(CommandAbstract.a(GameRules.getAllGameRules().keySet()));
            return true;
        }

        World world;

        if (sender instanceof ConsoleCommandSender) {
            world = MinecraftServer.getServer().worlds.get(0);
        } else {
            Player player = (Player) sender;
            world = ((CraftWorld)player.getWorld()).getHandle();
        }

        GameRules gameRules = world.getGameRules();
        String gameRule = args[0];

        if (args.length == 1) {
            sender.sendMessage(gameRule + " = " + gameRules.get(gameRule));
            return true;
        }

        String value = args[1];
        gameRules.set(gameRule, value);
        sender.sendMessage(gameRule + " has been set to " + value + " in world '" + world.worldData.getName() + "'.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], GameRules.getAllGameRules().keySet(), new ArrayList<>(GameRules.getAllGameRules().keySet().size()));
        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            for ( String string : GameRules.getAllGameRules().keySet()) {
                if (StringUtil.startsWithIgnoreCase(string, args[1])) {
                    completions.add(string);
                }
            }
            return completions;
        }
        return ImmutableList.of();
    }
}
