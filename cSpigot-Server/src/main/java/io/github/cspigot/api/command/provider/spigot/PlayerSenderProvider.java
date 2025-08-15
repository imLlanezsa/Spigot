package io.github.cspigot.api.command.provider.spigot;

import org.bukkit.entity.Player;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class PlayerSenderProvider extends DrinkProvider<Player> {

    public static final PlayerSenderProvider INSTANCE = new PlayerSenderProvider();

    @Override
    public boolean doesConsumeArgument() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Player provide(CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        if (arg.isSenderPlayer()) {
            return arg.getSenderAsPlayer();
        }
        throw new CommandExitMessage("This is a player-only command.");
    }

    @Override
    public String argumentDescription() {
        return "player sender";
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return Collections.emptyList();
    }
}
