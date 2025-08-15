package io.github.cspigot.cmds.impl.player;

import org.bukkit.entity.Player;
import org.spigotmc.SpigotConfig;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.OptArg;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;

@Register(name = "ping")
@SuppressWarnings("unused")
public class PingCommand {

    @Command(name = "", usage = "[target]", aliases = "help", desc = "View a player's ping")
    public void ping(@Sender Player player, @OptArg Player target) {
        if (!SettingsConfig.PING_COMMAND_ENABLED) {
            player.sendMessage(SpigotConfig.unknownCommandMessage);
            return;
        }
        if (target == null || !player.hasPermission("cSpigot.ping")) {
            player.sendMessage(CC.translate(SettingsConfig.SELF_PING_MESSAGE).replace("%ping%", Integer.toString(player.spigot().getPing())));
            return;
        }

        if (player.hasPermission("cSpigot.ping")) {
            String finalString = CC.translate(SettingsConfig.TARGET_PING_MESSAGE)
                    .replace("%player%", target.getName())
                    .replace("%ping%", Integer.toString(target.spigot().getPing()));
            player.sendMessage(finalString);
        }
    }
}
