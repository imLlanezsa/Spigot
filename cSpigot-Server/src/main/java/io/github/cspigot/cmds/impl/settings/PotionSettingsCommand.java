package io.github.cspigot.cmds.impl.settings;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.cSpigot;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

/**
 * This Project is property of Refine Development © 2021
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * Created at 6/8/2021
 * @version cSpigot
 */

@Register(name = "potion", aliases = {"potionSettings", "pot"})
public class PotionSettingsCommand {

    private final ChatColor color = SettingsConfig.THEME;

    private final String[] HELP_MESSAGE = {
            SettingsConfig.CHAT_BAR,
            CC.translate(color + CC.BOLD + "Potion Help"),
            CC.translate(""),
            CC.translate(" &e<> &7= &eRequired &7｜ &e[] &7= &eOptional"),
            CC.translate(""),
            CC.translate("&7 * " + color + "/potion values &7- View current potion settings"),
            CC.translate("&7 * " + color + "/potion speed &7<value> &7- Set Potion Throw Speed"),
            CC.translate("&7 * " + color + "/potion gravity &7<value> &7- Set Potion Throw Gravity"),
            CC.translate("&7 * " + color + "/potion time &7<value> &7- Set Potion Air Time"),
            CC.translate("&7 * " + color + "/potion vertical-offset &7<value> &7- Set Potion Throw vertical-offset"),
            CC.translate("&7 * " + color + "/potion horizontal-offset &7<value> &7- Set Potion Throw horizontal-offset"),
            CC.translate("&7 * " + color + "/potion smoothPots &7<true|false> &7- Set Smooth-Potion Mode"),
            CC.translate("&7 * " + color + "/potion reducedSplash &7<true|false> &7- Set Reduced-Splash Mode"),
            SettingsConfig.CHAT_BAR
    };

    @Command(name = "", aliases = "help", desc = "View Potion Setting commands")
    @Require("cSpigot.potion")
    public void help(@Sender CommandSender sender) {
        sender.sendMessage(HELP_MESSAGE);
    }

    @Command(name = "values", desc = "View Potion values")
    @Require("cSpigot.potion")
    public void values(@Sender CommandSender sender) {
        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(SettingsConfig.THEME + "Potion Values");
        sender.sendMessage("");
        sender.sendMessage(CC.translate("&7 * &eSpeed&7: &e" + SettingsConfig.POTION_SPEED));
        sender.sendMessage(CC.translate("&7 * &eGravity&7: &e" + SettingsConfig.POTION_GRAVITY));
        sender.sendMessage(CC.translate("&7 * &eAir Ticks&7: &e" + SettingsConfig.POTION_AIR_TICKS));
        sender.sendMessage(CC.translate("&7 * &eVertical-Offset&7: &e" + SettingsConfig.POTION_VERTICAL_OFFSET));
        //sender.sendMessage(CC.translate("&7 * &eHorizontal-Offset&7: &e" + SettingsConfig.POTION_HORIZONTAL_OFFSET));
        sender.sendMessage(CC.translate("&7 * &eSmooth-Potions: &e" + SettingsConfig.SMOOTH_POTIONS));
        sender.sendMessage(CC.translate("&7 * &eReduced-Splash: &e" + SettingsConfig.REDUCED_SPLASH));
        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }


    @Command(name = "speed", usage = "<value>", desc = "Set potion speed")
    @Require("cSpigot.potion")
    public void speed(@Sender CommandSender sender, double value) {
        if (Double.isNaN(value)) {
            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            return;
        }
        SettingsConfig.POTION_SPEED = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "speed &7to " + color + value + "&7."));
    }

    @Command(name = "gravity", usage = "<value>", desc = "Set potion gravity")
    @Require("cSpigot.potion")
    public void gravity(@Sender CommandSender sender, double value) {
        if (Double.isNaN(value)) {
            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            return;
        }
        SettingsConfig.POTION_GRAVITY = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "gravity &7to " + color + value + "&7."));
    }

    @Command(name = "time", usage = "<value>", desc = "Set potion time")
    @Require("cSpigot.potion")
    public void time(@Sender CommandSender sender, int value) {
        SettingsConfig.POTION_AIR_TICKS = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "time &7to " + color + value + "&7."));
    }

    @Command(name = "vertical-offset", aliases = "yOffset", usage = "<value>", desc = "Set potion vertical offset")
    @Require("cSpigot.potion")
    public void verticalOff(@Sender CommandSender sender, double value) {
        if (Double.isNaN(value)) {
            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
            return;
        }
        SettingsConfig.POTION_VERTICAL_OFFSET = value;
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "vertical-offset &7to " + color + value + "&7."));
    }

//    @Command(name = "horizontal-offset", aliases = {"zOffset", "xOffset"}, usage = "<value>", desc = "Set potion horizontal offset")
//    @Require("cSpigot.potion")
//    public void horizontalOff(@Sender CommandSender sender, double value) {
//        if (Double.isNaN(value)) {
//            sender.sendMessage(StringUtils.PREFIX + "&7Invalid Value");
//            return;
//        }
//        SettingsConfig.POTION_HORIZONTAL_OFFSET = value;
//        cSpigot.getConfig().writeConfig();
//        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "horizontal-offset &7to " + color + value + "&7."));
//    }

    @Command(name = "smoothpots", aliases = "smoothpotting", usage = "<value>", desc = "Toggle potion smooth-potting")
    @Require("cSpigot.potion")
    public void smoothPots(@Sender CommandSender sender, boolean value) {
        SettingsConfig.SMOOTH_POTIONS = value;
        if (SettingsConfig.REDUCED_SPLASH && SettingsConfig.SMOOTH_POTIONS) {
            SettingsConfig.REDUCED_SPLASH = false;
        }
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "smooth-potting &7to " + color + value + "&7."));
    }
    @Command(name = "reducedsplash", aliases = "expandedRaytrace", usage = "<value>", desc = "Toggle potion expanded-raytrace")
    @Require("cSpigot.potion")
    public void betterYaw(@Sender CommandSender sender, boolean value) {
        SettingsConfig.REDUCED_SPLASH = value;
        if (SettingsConfig.REDUCED_SPLASH && SettingsConfig.SMOOTH_POTIONS) {
            SettingsConfig.SMOOTH_POTIONS = false;
        }
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7You have updated potion " + color + "expanded-raytrace &7to " + color + value + "&7."));
    }
}
