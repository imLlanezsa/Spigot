package io.github.cspigot.api.command.provider;

import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class TextProvider extends DrinkProvider<String> {

    public static final TextProvider INSTANCE = new TextProvider();

    @Override
    public boolean doesConsumeArgument() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String provide(CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        StringBuilder builder = new StringBuilder(arg.get());
        while (arg.getArgs().hasNext()) {
            builder.append(" ").append(arg.getArgs().next());
        }
        return builder.toString();
    }

    @Override
    public String argumentDescription() {
        return "text";
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return Collections.emptyList();
    }
}
