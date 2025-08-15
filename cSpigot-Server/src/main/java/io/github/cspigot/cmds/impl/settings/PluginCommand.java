package io.github.cspigot.cmds.impl.settings;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.PluginUtil;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 3/28/2023
 * @version cSpigot
 */

@Register(name = "plugin")
public class PluginCommand {

    private final static ChatColor color = SettingsConfig.THEME;

    private static final String[] HELP_MESSAGE = {
            SettingsConfig.CHAT_BAR,
            CC.translate(color + CC.BOLD + "Plugin Manage Help"),
            CC.translate(""),
            CC.translate(" &e<> &7= &eRequired") + CC.GRAY + " | " + CC.translate("&e[] &7= &eOptional"),
            CC.translate(""),
            CC.translate("&7* " + color + "/plugin reload <plugin> &7- Reload a plugin"),
            CC.translate("&7* " + color + "/plugin load <file-name> &7- Load a plugin from file inside plugins folder"),
            CC.translate("&7* " + color + "/plugin enable <plugin> &7- Enable a disabled plugin"),
            CC.translate("&7* " + color + "/plugin disable <plugin> &7- Disable an enabled plugin"),
            SettingsConfig.CHAT_BAR
    };

    @Command(name = "", aliases = "help", desc = "View plugin commands")
    @Require("cSpigot.plugin")
    public void help(@Sender CommandSender sender) {
        sender.sendMessage(HELP_MESSAGE);
    }

    @Command(name = "reload", usage = "<plugin>", desc = "Reload a plugin")
    @Require("cSpigot.plugin")
    public void reload(@Sender CommandSender sender, Plugin plugin) {
        PluginUtil.reload(plugin);
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aSuccessfully reloaded the specified plugin!"));
    }

    @Command(name = "load", usage = "<file-name>", desc = "Load a plugin from file")
    @Require("cSpigot.plugin")
    public void load(@Sender CommandSender sender, String pluginName) {
        sender.sendMessage(PluginUtil.load(pluginName));
    }

    @Command(name = "enable", usage = "<plugin>", desc = "Enable a plugin")
    @Require("cSpigot.plugin")
    public void enable(@Sender CommandSender sender, Plugin plugin) {
        if (plugin.isEnabled()) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eThe specified plugin is already enabled!"));
            return;
        }
        PluginUtil.enable(plugin);
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aSuccessfully enabled the specified plugin!"));
    }

    @Command(name = "disable", usage = "<plugin>", desc = "Disable a plugin")
    @Require("cSpigot.plugin")
    public void disable(@Sender CommandSender sender, Plugin plugin) {
        if (!plugin.isEnabled()) {
            sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eThe specified plugin is already disabled!"));
            return;
        }
        PluginUtil.disable(plugin);
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&aSuccessfully disabled the specified plugin!"));
    }
}
