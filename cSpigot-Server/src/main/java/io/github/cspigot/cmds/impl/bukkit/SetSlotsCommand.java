package io.github.cspigot.cmds.impl.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

@Register(name = "setSlots", aliases = "setMaxPlayers")
@SuppressWarnings("unused")
public class SetSlotsCommand {

    @Command(name = "", usage = "<amount>", aliases = "help", desc = "Set maximum amount of players")
    @Require("cSpigot.setslots")
    public void slots(@Sender CommandSender sender, int amount) {
        Bukkit.getServer().setMaxPlayers(amount);
        sender.sendMessage(CC.translate(StringUtils.PREFIX + "&7Set the maximum players to " + SettingsConfig.THEME + amount + "&7!"));
    }
}
