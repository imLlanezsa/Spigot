package io.github.cspigot.cmds.impl.settings;

import io.github.cspigot.cSpigot;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

/**
 * This Project is property of Refine Development © 2021 2023
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 4/25/2022
 * @version cSpigot
 */

@Register(name = "enderpearl", aliases = {"epearl", "pearl", "ep", "ender"})
@SuppressWarnings("unused")
public class EnderpearlSettingsCommand {

    private final ChatColor color = SettingsConfig.THEME;
    private final String[] HELP_MESSAGE = {
            SettingsConfig.CHAT_BAR,
            CC.translate(color + CC.BOLD + "Enderpearl Help"),
            CC.translate(""),
            CC.translate(" &e<> &7= &eRequired &7｜ &e[] &7= &eOptional"),
            CC.translate(""),
            CC.translate("&7 * " + color + "/enderpearl values &7- View current pearl settings"),
            CC.translate("&7 * " + color + "/enderpearl speed &7<value> &7- Set pearl speed"),
            CC.translate("&7 * " + color + "/enderpearl gravity &7<value> &7- Set pearl gravity"),
            CC.translate("&7 * " + color + "/enderpearl damage &7<value> &7- Set pearl damage"),
            CC.translate("&7 * " + color + "/enderpearl glitchFix &7<value> &7- Set pearl glitch-fix"),
            CC.translate("&7 * " + color + "/enderpearl preventRisky &7<value> &7- Set pearl prevent risky teleport"),
            CC.translate("&7 * " + color + "/enderpearl betterYaw &7<value> &7- Set pearl better-yaw-mode"),
            CC.translate("&7 * " + color + "/enderpearl vertical-offset &7<value> &7- Set Pearl Throw vertical-offset"),
            //CC.translate("&7 * " + color + "/enderpearl horizontal-offset &7<value> &7- Set Pearl Throw horizontal-offset"),
            CC.translate("&7 * " + color + "/enderpearl through &7<gate|bed|slab|tripwire|cobweb> &7<value> &7- Pearl through settings"),
            SettingsConfig.CHAT_BAR
    };

    @Command(name = "", aliases = "help", desc = "View Enderpearl Setting commands")
    @Require("cSpigot.enderpearl")
    public void help(@Sender CommandSender sender) {
        sender.sendMessage(HELP_MESSAGE);
    }

    @Command(name = "values", desc = "View Enderpearl values")
    @Require("cSpigot.enderpearl")
    public void values(@Sender CommandSender sender) {
        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(color + CC.BOLD + "Pearl Values");
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&7* &eSpeed&7: &e" + SettingsConfig.PEARL_SPEED));
        sender.sendMessage(CC.translate("&7* &eGravity&7: &e" + SettingsConfig.PEARL_GRAVITY));
        sender.sendMessage(CC.translate("&7* &eVertical-Offset&7: &e" + SettingsConfig.PEARL_VERTICAL_OFFSET));
        sender.sendMessage(CC.translate("&7* &eGlitch-Fix: &e" + SettingsConfig.PEARL_WALL_GLITCH_FIX));
        sender.sendMessage(CC.translate("&7* &eBetter Yaw: &e" + SettingsConfig.PEARL_BETTER_YAW));
        sender.sendMessage(CC.translate("&7* &eDamage: &e" + SettingsConfig.PEARL_DAMAGE));
        sender.sendMessage(CC.translate("&7* &eThrough Gate: &e" + SettingsConfig.PEARL_FENCE_GATE));
        sender.sendMessage(CC.translate("&7* &eThrough Cobweb: &e" + SettingsConfig.PEARL_COBWEB));
        sender.sendMessage(CC.translate("&7* &eThrough Bed: &e" + SettingsConfig.PEARL_BED));
        sender.sendMessage(CC.translate("&7* &eThrough Slab: &e" + SettingsConfig.PEARL_SLAB));
        sender.sendMessage(CC.translate("&7* &eThrough Tripwire: &e" + SettingsConfig.PEARL_TRIPWIRE));
        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }


    @Command(name = "speed", usage = "<value>", desc = "Set pearl speed")
    @Require("cSpigot.enderpearl")
    public void speed(@Sender CommandSender sender, double value) {
        if (Double.isNaN(value)) {
            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            return;
        }
        SettingsConfig.PEARL_SPEED = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "speed &7to " + color + value + "&7."));
    }

    @Command(name = "gravity", usage = "<value>", desc = "Set pearl gravity")
    @Require("cSpigot.enderpearl")
    public void gravity(@Sender CommandSender sender, double value) {
        if (Double.isNaN(value)) {
            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            return;
        }
        SettingsConfig.PEARL_GRAVITY = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "gravity &7to " + color + value + "&7."));
    }
    @Command(name = "vertical-offset", aliases = "yOffset", usage = "<value>", desc = "Set pearl vertical offset")
    @Require("cSpigot.enderpearl")
    public void yOff(@Sender CommandSender sender, double value) {
        if (Double.isNaN(value)) {
            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            return;
        }
        SettingsConfig.PEARL_VERTICAL_OFFSET = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "vertical-offset &7to " + color + value + "&7."));
    }

//    @Command(name = "horizontal-offset", aliases = "xOffset", usage = "<value>", desc = "Set pearl horizontal offset")
//    @Require("cSpigot.enderpearl")
//    public void horizontalOff(@Sender CommandSender sender, double value) {
//        if (Double.isNaN(value)) {
//            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
//            return;
//        }
//        SettingsConfig.PEARL_HORIZONTAL_OFFSET = value;
//        cSpigot.getConfig().writeConfig();
//        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "horizontal-offset &7to " + color + value + "&7."));
//    }

    @Command(name = "glitchFix", aliases = "glitch", usage = "<value>", desc = "Toggle pearl glitch-fix")
    @Require("cSpigot.enderpearl")
    public void glitchFix(@Sender CommandSender sender, boolean value) {
        SettingsConfig.PEARL_WALL_GLITCH_FIX = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "glitch-fix &7to " + color + value + "&7."));
    }

    @Command(name = "betterYaw", usage = "<value>", desc = "Toggle pearl better yaw")
    @Require("cSpigot.enderpearl")
    public void betterYaw(@Sender CommandSender sender, boolean value) {
        SettingsConfig.PEARL_BETTER_YAW = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "better-yaw &7to " + color + value + "&7."));
    }
    @Command(name = "damage", usage = "<value>", desc = "Toggle pearl damage")
    @Require("cSpigot.enderpearl")
    public void help(@Sender CommandSender sender, boolean value) {
        SettingsConfig.PEARL_DAMAGE = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl " + color + "damage &7to " + color + value + "&7."));
    }

    @Command(name = "through", usage = "<arg> <value>", desc = "Toggle pearl through for blocks")
    @Require("cSpigot.enderpearl")
    public void through(@Sender CommandSender sender, String argument, boolean value) {
        switch (argument) {
            case "gate": {
                SettingsConfig.PEARL_FENCE_GATE = value;
                break;
            }
            case "slab": {
                SettingsConfig.PEARL_SLAB = value;
                break;
            }
            case "cobweb": {
                SettingsConfig.PEARL_COBWEB = value;
                break;
            }
            case "bed": {
                SettingsConfig.PEARL_BED = value;
                break;
            }
            case "tripwire": {
                SettingsConfig.PEARL_TRIPWIRE = value;
                break;
            }
        }
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated pearl through " + color + argument + " &7to " + color + value + "&7."));
    }
}
