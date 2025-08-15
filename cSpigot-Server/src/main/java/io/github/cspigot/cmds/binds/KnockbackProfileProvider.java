package io.github.cspigot.cmds.binds;

import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.cSpigot;
import io.github.cspigot.knockback.KnockbackHandler;
import io.github.cspigot.knockback.KnockbackProfile;

import java.lang.annotation.Annotation;
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
public class KnockbackProfileProvider extends DrinkProvider<KnockbackProfile> {

    public KnockbackProfile provide(CommandArg commandArg, List<? extends Annotation> list) throws CommandExitMessage {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        String arg = commandArg.get();

        KnockbackProfile profile = knockbackHandler.getKnockbackProfile(arg);
        if (profile == null) {
            throw new CommandExitMessage("A knockback profile with that name does not exist!");
        }

        return profile;
    }

    public String argumentDescription() {
        return "knockback-profile";
    }

    public List<String> getSuggestions(String prefix) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        return knockbackHandler.getProfiles()
                .stream()
                .map(KnockbackProfile::getName)
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
