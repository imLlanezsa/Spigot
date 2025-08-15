package io.github.cspigot.cmds.impl.bukkit;

import io.github.cspigot.api.command.annotation.*;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.Clickable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.ChatPaginator;
import org.spigotmc.SpigotConfig;

@Register(name = "plugins", aliases = "pl")
public class PluginsCommand {

    @Require("cSpigot.plugins")
    @Command(name = "", usage = "[pageNumber]", aliases = "help", desc = "Gets a list of plugins running on the server")
    public void plugins(@Sender CommandSender sender, @OptArg("1") int pageNumber) {
        if (!SettingsConfig.PLUGIN_COMMAND_ENABLED) {
            sender.sendMessage(SpigotConfig.unknownCommandMessage);
            return;
        }

        Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
        if (plugins == null || plugins.length == 0) {
            sender.sendMessage(CC.translate("&eThis server isn't currently running any plugins!"));
            return;
        }

        if (SettingsConfig.DEFAULT_PLUGINS_COMMAND) {
            displayDefaultList(sender, plugins);
        } else {
            displayCustomList(sender, plugins, pageNumber);
        }
    }

    private void displayDefaultList(CommandSender sender, Plugin[] plugins) {
        StringBuilder list = new StringBuilder();
        for (Plugin plugin : plugins) {
            if (list.length() > 0) {
                list.append(ChatColor.WHITE).append(", ");
            }
            list.append(plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED)
                    .append(plugin.getName());
        }
        sender.sendMessage("Plugins (" + plugins.length + "): " + list);
    }

    private void displayCustomList(CommandSender sender, Plugin[] plugins, int pageNumber) {
        String list = buildCustomList(plugins);
        ChatPaginator.ChatPage page = ChatPaginator.paginate(list, pageNumber);

        sender.sendMessage(SettingsConfig.CHAT_BAR);
        sender.sendMessage(CC.translate(SettingsConfig.THEME + "&lPlugins List" +
                (page.getTotalPages() > 1 ? " &7(" + page.getPageNumber() + ")" : "")));
        sender.sendMessage(" ");
        sender.sendMessage(CC.translate("&7&oThis server is currently running " +
                SettingsConfig.THEME + plugins.length + " &7&oplugin(s)!"));
        sender.sendMessage(" ");

        for (String line : page.getLines()) {
            sender.sendMessage(line);
        }

        sender.sendMessage(" ");
        displayPaginationButtons(sender, pageNumber, page);
        sender.sendMessage(SettingsConfig.CHAT_BAR);
    }

    private String buildCustomList(Plugin[] plugins) {
        StringBuilder list = new StringBuilder();
        for (Plugin plugin : plugins) {
            if (list.length() > 0) {
                list.append("\n");
            }
            list.append(ChatColor.GRAY).append(" * ")
                    .append(plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED)
                    .append(plugin.getName()).append(ChatColor.DARK_GRAY)
                    .append(" [").append(ChatColor.GRAY)
                    .append(plugin.getDescription().getVersion())
                    .append(ChatColor.DARK_GRAY).append("]");
        }
        return list.toString();
    }

    private void displayPaginationButtons(CommandSender sender, int pageNumber, ChatPaginator.ChatPage page) {
        if (!(sender instanceof Player) || page.getTotalPages() <= 1) {
            return;
        }

        Player player = (Player) sender;
        Clickable clickable;

        if (pageNumber == page.getTotalPages()) {
            clickable = new Clickable("&7 * &e[Previous Page]", "&7Click here to view previous page",
                    "/plugins " + (pageNumber - 1));
        } else if (pageNumber > 1) {
            clickable = new Clickable("&7 * &e[Previous Page]", "&7Click here to view previous page",
                    "/plugins " + (pageNumber - 1));
            clickable.add(" &a[Next Page]", "&7Click here to view next page",
                    "/plugins " + (pageNumber + 1));
        } else {
            clickable = new Clickable("&7 * &a[Next Page]", "&7Click here to view next page",
                    "/plugins " + (pageNumber + 1));
        }

        clickable.sendToPlayer(player);
    }
}