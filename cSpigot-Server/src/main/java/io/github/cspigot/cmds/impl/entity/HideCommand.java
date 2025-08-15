package io.github.cspigot.cmds.impl.entity;

import org.bukkit.entity.Player;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;

import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

@Register(name = "hide", aliases = {"toggeplayer", "show"})
@SuppressWarnings("unused")
public class HideCommand{

    @Command(name = "", usage = "<target>", desc = "Toggle a player's visibility for yourself")
    @Require("cSpigot.hide")
    public void execute(@Sender Player player, Player target) {
        if (player == target) {
            player.sendMessage("§cYou can not edit your own visibility!");
            return;
        }

        boolean see = player.canSee(target);
        if (see) {
            player.hidePlayer(target);
            player.sendMessage(CC.translate(StringUtils.PREFIX + "&e" + target.getName() + " &7is now hidden for you."));
        } else {
            player.showPlayer(target);
            player.sendMessage(CC.translate(StringUtils.PREFIX + "&7You can now see &e" + target.getName() + "&7."));
        }
    }
}
