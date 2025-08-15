package co.aikar.timings;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;


public class TimingsCommand extends BukkitCommand {

    public TimingsCommand(String name) {
        super(name);
        this.description = "Manages Spigot Timings data to see performance of the server.";
        this.usageMessage = "/timings";// <reset|report|on|off|verbon|verboff>";
        this.setPermission("bukkit.command.timings");
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        sender.sendMessage(ChatColor.DARK_PURPLE + "cSpigot (Purpur)" + ChatColor.GRAY + " has removed timings to save your performance. Please use " + ChatColor.GOLD + "/spark" + ChatColor.GRAY + " instead.");
        sender.sendMessage(ChatColor.GRAY + "For more information, view its documentation at");
        sender.sendMessage(ChatColor.GOLD + "https://spark.lucko.me/docs/Command-Usage");

        return true;
    }
}
