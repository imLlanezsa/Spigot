package io.github.cspigot.api.command.provider;

import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class StringProvider extends DrinkProvider<String> {

    public static final StringProvider INSTANCE = new StringProvider();

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
        return true;
    }

    @Override
    public String defaultNullValue() {
        return null;
    }

    @Override
    public String provide(CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        return arg.get();
    }

    @Override
    public String argumentDescription() {
        return "string";
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return Collections.emptyList();
    }
}
