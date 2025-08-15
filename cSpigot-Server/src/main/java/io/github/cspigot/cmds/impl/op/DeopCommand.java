package io.github.cspigot.cmds.impl.op;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang.Validate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.CC;
import io.github.cspigot.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DeopCommand extends Command {

    private final ChatColor color = SettingsConfig.THEME;
    private static final String ALERT_PREFIX = "&4[Alerts] ";

    String[] HELP_MESSAGE = {
            SettingsConfig.CHAT_BAR,
            CC.translate(color + CC.BOLD + "Operator Help"),
            CC.translate(""),
            CC.translate(" &e<> &7= &eRequired &7｜ &e[] &7= &eOptional"),
            CC.translate(""),
            CC.translate("&7 * " + color + "/op &7<player> - Grant operator to player"),
            CC.translate("&7 * " + color + "/deop &7<player> - Revoke operator from a player"),
            CC.translate(""),
            SettingsConfig.CHAT_BAR,
    };

    public DeopCommand() {
        super("deop");
        this.description = "Remove specified player's operator status.";
        this.usageMessage = String.join("\n", HELP_MESSAGE);
        this.setPermission("bukkit.command.deop");
    }

    @Override
    public boolean execute(CommandSender sender, String currentAlias, String[] args) {
        if (!this.testPermission(sender)) return false;

        if (args.length != 1 || args[0].length() == 0) {
            sender.sendMessage(HELP_MESSAGE);
            return true;
        }

        if (args.length == 1) {
            String playerName = args[0];
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);

            if (player == null || player.getName() == null) {
                sender.sendMessage(CC.translate("&6Invalid player name!"));
                return true;
            }

            if (!player.isOp()) {
                sender.sendMessage(CC.translate("&e" + player.getName() + " &6is not an operator!"));
                return true;
            }

            try {
                player.setOp(false);
                sender.sendMessage(CC.translate("&7You've taken " + color + player.getName() + "'s &7permissions for " + color + "operator&7!"));

                if (player.isOnline() && player.getPlayer() != null) {
                    Player onlinePlayer = player.getPlayer();
                    onlinePlayer.sendMessage(" ");
                    onlinePlayer.sendMessage(CC.translate(StringUtils.PREFIX + "&7Your " + color + "operator status &7has been removed."));
                    onlinePlayer.sendMessage(" ");
                }

                sendOperatorAlert(sender, player.getName());
            } catch (Exception e) {
                sender.sendMessage(CC.translate("&6Failed to deop player: " + e.getMessage()));
                Bukkit.getLogger().warning("Failed to deop player: " + e.getMessage());
            }
        }
        return true;
    }

    private void sendOperatorAlert(CommandSender sender, String targetName) {
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        String alertMessage = CC.translate(ALERT_PREFIX + "&e" + senderName + " &7has removed operator status from &e" + targetName);

        Bukkit.getConsoleSender().sendMessage(alertMessage);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(alertMessage);
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (OfflinePlayer player : Bukkit.getOperators()) {
                String playerName = player.getName();
                if (playerName != null && StringUtil.startsWithIgnoreCase(playerName, args[0])) {
                    completions.add(playerName);
                }
            }
            return completions;
        }
        return ImmutableList.of();
    }
}