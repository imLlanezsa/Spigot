package io.github.cspigot.cmds.binds;

import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.knockback.meta.KnockbackType;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 8/1/2023
 * @version cSpigot
 */
public class KnockbackTypeProvider extends DrinkProvider<KnockbackType> {

    public KnockbackType provide(CommandArg arg, List<? extends Annotation> list) throws CommandExitMessage {
        String name = arg.get();
        try {
            return KnockbackType.valueOf(name.toUpperCase());
        } catch (Exception e) {
            throw new CommandExitMessage("A KnockbackType with that name does not exist!");
        }
    }

    public String argumentDescription() {
        return "type";
    }

    public List<String> getSuggestions(String prefix) {
        return Arrays.stream(KnockbackType.values())
                .map(KnockbackType::name)
                .filter(s -> prefix.length() == 0 || s.startsWith(prefix))
                .collect(Collectors.toList());
    }

    public boolean doesConsumeArgument() {
        return true;
    }

    public boolean isAsync() {
        return true;
    }
}
