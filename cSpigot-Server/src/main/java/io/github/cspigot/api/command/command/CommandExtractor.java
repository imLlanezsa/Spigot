package io.github.cspigot.api.command.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.exception.CommandRegistrationException;
import io.github.cspigot.api.command.exception.CommandStructureException;
import io.github.cspigot.api.command.exception.MissingProviderException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandExtractor {

    private final DrinkCommandService commandService;

    public CommandExtractor(DrinkCommandService commandService) {
        this.commandService = commandService;
    }

    public Map<String, DrinkCommand> extractCommands(Object handler) throws MissingProviderException, CommandStructureException {
        Preconditions.checkNotNull(handler, "Handler object cannot be null");
        final Map<String, DrinkCommand> commands = new HashMap<>();
        for (Method method : handler.getClass().getDeclaredMethods()) {
            Optional<DrinkCommand> o = extractCommand(handler, method);
            if (o.isPresent()) {
                DrinkCommand drinkCommand = o.get();
                commands.put(commandService.getCommandKey(drinkCommand.getName()), drinkCommand);
            }
        }
        return commands;
    }

    private Optional<DrinkCommand> extractCommand(Object handler, Method method) throws MissingProviderException, CommandStructureException {
        Preconditions.checkNotNull(handler, "Handler object cannot be null");
        Preconditions.checkNotNull(method, "Method cannot be null");
        if (method.isAnnotationPresent(Command.class)) {
            try {
                method.setAccessible(true);
            }
            catch (SecurityException ex) {
                throw new CommandRegistrationException("Couldn't access method " + method.getName());
            }
            Command command = method.getAnnotation(Command.class);
            String perm = "";
            if (method.isAnnotationPresent(Require.class)) {
                Require require = method.getAnnotation(Require.class);
                perm = require.value();
            }
            DrinkCommand drinkCommand = new DrinkCommand(
                    commandService, command.name(), Sets.newHashSet(command.aliases()), command.desc(), command.usage(),
                    perm, handler, method
            );
            return Optional.of(drinkCommand);
        }
        return Optional.empty();
    }

}
