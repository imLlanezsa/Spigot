package io.github.cspigot.api.command.parametric;

import lombok.Getter;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

@Getter
public abstract class DrinkProvider<T> {

    public abstract boolean doesConsumeArgument();

    public abstract boolean isAsync();

    public boolean allowNullArgument() {
        return true;
    }

    public T defaultNullValue() {
        return null;
    }

    public T provide(CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        return null;
    }

    public T provide(String[] args, CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        return provide(arg, annotations);
    }

    public abstract String argumentDescription();

    public List<String> getSuggestions(String[] args, String prefix) {
        return this.getSuggestions(prefix);
    }

    public List<String> getSuggestions(String prefix) {
        return Collections.emptyList();
    }

    protected boolean hasAnnotation(List<? extends Annotation> list, Class<? extends Annotation> a) {
        return list.stream().anyMatch(annotation -> annotation.annotationType().equals(a));
    }

}
