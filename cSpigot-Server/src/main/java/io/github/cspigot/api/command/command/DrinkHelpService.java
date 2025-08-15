package io.github.cspigot.api.command.command;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.github.cspigot.api.command.util.CC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter @Setter
public class DrinkHelpService {

    private final DrinkCommandService commandService;
    private HelpFormatter helpFormatter;

    public DrinkHelpService(DrinkCommandService commandService) {
        this.commandService = commandService;
        this.helpFormatter = (sender, container) -> {
            String containerName = container.getName();
            String formattedName = containerName.substring(0, 1).toUpperCase() + containerName.substring(1);

            sender.sendMessage(CC.CHAT_BAR);
            sender.sendMessage(CC.translate( "&e&l" + formattedName + " Help"));
            sender.sendMessage("");

            // This is to have it organized.
            List<DrinkCommand> commands = new ArrayList<>(container.getCommands().values());
            Collections.reverse(commands);

            // Console
            if (!(sender instanceof Player)) {
                for ( DrinkCommand c : commands) {
                    sender.sendMessage(CC.translate(" &7* &e/" + container.getName() + (c.getName().length() > 0 ? " &e" + c.getName() : "") + (c.getMostApplicableUsage().length() > 0 ? " &7" + c.getMostApplicableUsage() : "") + " - &7" + c.getDescription()));
                }
                sender.sendMessage(CC.CHAT_BAR);
                return;
            }

            // Player
            for ( DrinkCommand c : commands) {
                         // the command itself
                String text = CC.translate(" &7* &e/" + container.getName() + (c.getName().length() > 0 ? " &e" + c.getName() : "") +
                        // usage
                        (c.getMostApplicableUsage().length() > 0 ? " &7" + c.getMostApplicableUsage() : "") +
                        // description
                        " &7- " + c.getDescription());

                String hover = ChatColor.GRAY + "/" + container.getName() + " " + c.getName() + " - " + ChatColor.WHITE + c.getDescription();
                String command = "/" + container.getName() + " " + c.getName();

                TextComponent msg = new TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', text));
                msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
                msg.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));

                Player player = (Player) sender;
                player.spigot().sendMessage(msg);
            }
            sender.sendMessage(CC.CHAT_BAR);
        };
    }

    public void sendHelpFor(CommandSender sender, DrinkCommandContainer container) {
        this.helpFormatter.sendHelpFor(sender, container);
    }

    public void sendUsageMessage(CommandSender sender, DrinkCommandContainer container, DrinkCommand command) {
        sender.sendMessage(getUsageMessage(container, command));
    }

    public String getUsageMessage(DrinkCommandContainer container, DrinkCommand command) {
        String usage = ChatColor.GRAY + "Usage: /" + container.getName() + " ";
        if (command.getName().length() > 0) {
            usage += command.getName() + " ";
        }
        if (command.getUsage() != null && command.getUsage().length() > 0) {
            usage += command.getUsage();
        } else {
            usage += command.getGeneratedUsage();
        }
        return usage;
    }

}
