package io.github.cspigot.cmds.impl.bukkit;

import io.github.cspigot.api.command.annotation.*;
import io.github.cspigot.cSpigot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import org.spigotmc.SpigotConfig;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

import java.util.List;


@Register(name = "version", aliases = {"ver", "about"})
@SuppressWarnings("unused")
public class VersionCommand {

    @Command(name = "", aliases = "help", desc = "Gets the version of this server including any plugins in use")
    @Require("bukkit.command.version")
    public void execute(@Sender CommandSender sender, @OptArg String pluginName) {
        if (!SettingsConfig.VERSION_COMMAND_ENABLED)  {
            sender.sendMessage(SpigotConfig.unknownCommandMessage);
            return;
        }

        if (pluginName == null) {
            SettingsConfig.VERSION_MESSAGE
                    .stream()
                    .map(string -> string.replace("<version>", cSpigot.version + CC.GRAY + " (" + CraftServer.class.getPackage().getImplementationVersion() + ")"))
                    .map(CC::translate)
                    .forEach(sender::sendMessage);
            return;
        }

        Plugin exactPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (exactPlugin != null) {
            describeToSender(exactPlugin, sender);
            return;
        }

        pluginName = pluginName.toLowerCase();

        boolean found = false;
        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {
            if (plugin.getName().toLowerCase().contains(pluginName)) {
                describeToSender(plugin, sender);
                found = true;
            }
        }

        if (!found) {
            sender.sendMessage(ChatColor.RED + "This server is not running any plugin by that name.");
            sender.sendMessage(ChatColor.RED + "Use /plugins to get a list of plugins.");
        }
    }

    private void describeToSender(Plugin plugin, CommandSender sender) {
        PluginDescriptionFile desc = plugin.getDescription();
        sender.sendMessage(ChatColor.GREEN + desc.getName() + ChatColor.WHITE + " version " + SettingsConfig.THEME + desc.getVersion());

        if (desc.getDescription() != null) {
            sender.sendMessage(desc.getDescription());
        }

        if (desc.getWebsite() != null) {
            sender.sendMessage(CC.WHITE + "Website: " + SettingsConfig.THEME + desc.getWebsite());
        }

        if (!desc.getAuthors().isEmpty()) {
            sender.sendMessage(CC.WHITE + "Authors: " + getAuthors(desc));
        }
    }

    @Command(name = "gitId", usage = "[full]", aliases = "git", desc = "View the git id of the cSpigot build")
    public void pass(@Sender Player sender, @OptArg String password) {
        String gitId = CraftServer.class.getPackage().getImplementationVersion();
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&eGit ID#" + gitId));
    }

    private String getAuthors(final PluginDescriptionFile desc) {
        StringBuilder result = new StringBuilder();
        List<String> authors = desc.getAuthors();

        for ( int i = 0; i < authors.size(); i++ ) {
            if (result.length() > 0) {
                if (i < authors.size() - 1) {
                    result.append(", ");
                } else {
                    result.append(" and ");
                }
            }

            result.append(authors.get(i));
        }

        return result.toString();
    }
}
