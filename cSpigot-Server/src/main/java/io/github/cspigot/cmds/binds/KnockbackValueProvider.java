package io.github.cspigot.cmds.binds;

import io.github.cspigot.cSpigot;
import lombok.extern.log4j.Log4j2;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.knockback.KnockbackHandler;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.api.knockback.meta.KnockbackValue;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
@Log4j2
public class KnockbackValueProvider extends DrinkProvider<KnockbackValue> {

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
    public KnockbackValue provide(String[] args, CommandArg commandArg, List<? extends Annotation> list) throws CommandExitMessage {
        List<String> arguments = Arrays.asList(args);
        int index = arguments.indexOf(commandArg.get());
        try {
            KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
            String profile = arguments.get(index - 1);
            KnockbackProfile knockbackProfile = knockbackHandler.getKnockbackProfile(profile.toLowerCase());
            if (knockbackProfile == null) {
                throw new CommandExitMessage("");
            }
            KnockbackValue value = knockbackProfile.getValues()
                    .stream()
                    .filter(v -> v.getId().equalsIgnoreCase(commandArg.get()))
                    .findFirst()
                    .orElse(null);

            if (value == null) {
                throw new CommandExitMessage("");
            }

            return value;
        } catch (Exception e) {
            throw new CommandExitMessage("A knockback field with that ID does not exist!");
        }
    }

    @Override
    public String argumentDescription() {
        return "field";
    }

    @Override
    public List<String> getSuggestions(String[] args, String prefix) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();

        List<String> arguments = Arrays.asList(args);
        int index = arguments.indexOf(prefix);
        try {
            String profile = arguments.get(index - 1);
            KnockbackProfile knockbackProfile = knockbackHandler.getKnockbackProfile(profile.toLowerCase());
            if (knockbackProfile == null) {
               return Collections.emptyList();
            }

            return knockbackProfile.getValues()
                    .stream()
                    .sorted(Comparator.comparingInt(KnockbackValue::getPriority))
                    .map(KnockbackValue::getId)
                    .filter(id -> prefix.length() == 0 || id.startsWith(prefix))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.info("[cSpigot] Failed to tab-complete knockback values for {}", prefix);
            log.error(e);
            return Collections.emptyList();
        }
    }
}
