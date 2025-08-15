package io.github.cspigot.api.command.modifier;

import io.github.cspigot.api.command.command.CommandExecution;
import io.github.cspigot.api.command.exception.CommandExitMessage;
import io.github.cspigot.api.command.parametric.CommandParameter;
import java.util.Optional;

public interface DrinkModifier<T> {

    Optional<T> modify(CommandExecution execution, CommandParameter commandParameter, T argument) throws CommandExitMessage;

}
