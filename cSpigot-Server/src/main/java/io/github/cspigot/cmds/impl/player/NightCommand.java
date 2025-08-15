package io.github.cspigot.cmds.impl.player;

import org.bukkit.entity.Player;
import org.spigotmc.SpigotConfig;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;

/**
 * This Project is property of Refine Development © 2021
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 9/6/2021
 * @version cSpigot
 */

@Register(name = "night")
@SuppressWarnings("unused")
public class NightCommand {

    @Command(name = "", aliases = "help", desc = "Set player time to night")
    public void day(@Sender Player player) {
        if (!SettingsConfig.NIGHT_COMMAND_ENABLED) {
            player.sendMessage(SpigotConfig.unknownCommandMessage);
            return;
        }
        player.setPlayerTime(18000L, false);
        player.sendMessage(CC.translate("&aTime set to night!"));
    }
}
