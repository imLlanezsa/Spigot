package io.github.cspigot.cmds.impl.emoji;

import org.bukkit.entity.Player;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Sender;

/**
 * This Project is property of Refine Development © 2021 2023
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 8/11/2022
 * @version cSpigot
 */

@Register(name = "smh")
public class SMHCommand {

    @Command(name = "", aliases = "help", desc = "Type the smh emoji in chat")
    public void execute(@Sender Player sender) {
        sender.chat("(￣﹃￣)");
    }
}
