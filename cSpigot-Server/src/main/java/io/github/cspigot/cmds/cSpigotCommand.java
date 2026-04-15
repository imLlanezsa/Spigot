package io.github.cspigot.cmds;

import io.github.cspigot.cSpigot;
import net.minecraft.server.AsyncUtil;
import net.minecraft.server.PacketPlayOutExplosion;
import net.minecraft.server.Vec3D;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.async.profiler.NoOpAsyncProfiler;
import io.github.cspigot.feature.async.profiler.TimeoutAsyncProfiler;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;
import io.github.cspigot.utils.traffic.StaticConstants;

import java.util.Collections;

@Register(name = "cSpigot", aliases = "cSpigot")
@SuppressWarnings("unused")
public class cSpigotCommand {

    private final ChatColor color = SettingsConfig.THEME;

    String[] HELP_MESSAGE = {
            SettingsConfig.CHAT_BAR,
            CC.translate(color + CC.BOLD + "cSpigot Help"),
            CC.translate(""),
            CC.translate(" &e<> &f= &eRequired &f｜ &e[] &f= &eOptional"),
            CC.translate(""),
            CC.translate(color + CC.BOLD + "Core Commands:"),
            CC.translate("&f * " + color + "/cSpigot &f- View this help message"),
            CC.translate("&f * " + color + "/cSpigot reload &f- Reload cSpigot configuration"),
            CC.translate("&f * " + color + "/cSpigot save &f- Save cSpigot configuration"),
            CC.translate(""),
            CC.translate(color + CC.BOLD + "Server Management:"),
            CC.translate("&f * " + color + "/setSlots &f<amount> &f- Set the server's max players"),
            CC.translate("&f * " + color + "/killall &f- Kill all entities in all worlds"),
            CC.translate("&f * " + color + "/hide &f<target> &f- Toggle a player's visibility"),
            CC.translate(""),
            CC.translate(color + CC.BOLD + "Plugin Management:"),
            CC.translate("&f * " + color + "/plugins &f[pageNumber] &f- View plugins list"),
            CC.translate("&f * " + color + "/plugin &f<enable/disable/load> <pluginName>"),
            CC.translate(""),
            CC.translate(color + CC.BOLD + "Game Mechanics:"),
            CC.translate("&f * " + color + "/knockback &f- View knockback commands"),
            CC.translate("&f * " + color + "/potion &f- View potion settings commands"),
            CC.translate("&f * " + color + "/pearl &f- View pearl settings commands"),
            CC.translate(""),
            CC.translate(color + CC.BOLD + "Player Commands:"),
            CC.translate("&f * " + color + "/day &f- Set your own time to day"),
            CC.translate("&f * " + color + "/night &f- Set your own time to night"),
            CC.translate(""),
            CC.translate(color + CC.BOLD + "Chat Commands:"),
            CC.translate("&f * " + color + "/shrug &f- Type ¯\\_(ツ)_/¯ in chat"),
            CC.translate("&f * " + color + "/cry &f- Type ¯(ಥ _ ಥ) in chat"),
            CC.translate("&f * " + color + "/smh &f- Type (￣﹃￣) in chat"),
            CC.translate(""),
            SettingsConfig.CHAT_BAR
    };

    @Command(name = "", aliases = "help", desc = "View cSpigot commands")
    @Require("cSpigot.help")
    public void help(@Sender CommandSender sender) {
        sender.sendMessage(HELP_MESSAGE);
    }

    @Command(name = "reload", desc = "Reload cSpigot settings from file")
    @Require("cSpigot.help")
    public void reload(@Sender CommandSender sender) {
        cSpigot.getConfig().reload();
        cSpigot.getInstance().getResourcePackHandler().init();
        cSpigot.getInstance().setupTps();
        if (!(SettingsConfig.ASYNC_MONITOR_TIMEOUT_ENABLED && AsyncUtil.ASYNC_PROFILER instanceof TimeoutAsyncProfiler)) {
            AsyncUtil.ASYNC_PROFILER = SettingsConfig.ASYNC_MONITOR_TIMEOUT_ENABLED ? new TimeoutAsyncProfiler() : new NoOpAsyncProfiler();
        }

        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eSuccessfully&a re-loaded &eall configuration files!"));
    }

    @Command(name = "save", desc = "Save cSpigot settings to file")
    @Require("cSpigot.help")
    public void save(@Sender CommandSender sender) {
        cSpigot.getConfig().writeConfig();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eSuccessfully&a saved &eall configuration files!"));
    }
}