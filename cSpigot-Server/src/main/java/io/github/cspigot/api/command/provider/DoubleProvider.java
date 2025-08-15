package io.github.cspigot.api.command.provider;

import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class DoubleProvider extends DrinkProvider<Double> {

    public static final DoubleProvider INSTANCE = new DoubleProvider();

    @Override
    public boolean doesConsumeArgument() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean allowNullArgument() {
        return false;
    }

    @Override
    public Double defaultNullValue() {
        return 0D;
    }

    @Override
    public Double provide(CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        String s = arg.get();
        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException ex) {
            throw new CommandExitMessage("Required: Decimal Number, Given: '" + s + "'");
        }
    }

    @Override
    public String argumentDescription() {
        return "decimal number";
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return Collections.emptyList();
    }
}
