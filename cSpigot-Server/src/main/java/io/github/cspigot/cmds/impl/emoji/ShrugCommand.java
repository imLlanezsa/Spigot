package io.github.cspigot.cmds.impl.emoji;

import org.bukkit.entity.Player;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Sender;

@Register(name = "shrug")
public class ShrugCommand {

    @Command(name = "", aliases = "help", desc = "Type the shrug emoji in chat")
    public void execute(@Sender Player sender) {
        sender.chat("¯\\_(ツ)_/¯");
    }
}