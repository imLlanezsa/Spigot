package io.github.cspigot.cmds.impl.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;

@Register(name = "reload", aliases = "rl")
@SuppressWarnings("unused")
public class ReloadCommand {

    @Command(name = "", aliases = "help", desc = "Reloads the server configuration and plugins")
    @Require("bukkit.command.reload")
    public void reload(@Sender CommandSender sender) {
        if (!SettingsConfig.RELOAD_COMMAND_ENABLED) return;

        sender.sendMessage(ChatColor.RED + "Please note that this command is not supported and may cause issues when using some plugins.");
        sender.sendMessage(ChatColor.RED + "If you encounter any issues please use the /stop command to restart your server.");
        Bukkit.reload();
        sender.sendMessage(ChatColor.GREEN + "Reload complete.");
    }
}
