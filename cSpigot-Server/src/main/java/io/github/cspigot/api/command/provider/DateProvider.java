package io.github.cspigot.api.command.provider;

import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.argument.CommandArg;
import io.github.cspigot.api.command.exception.CommandExitMessage;

import java.lang.annotation.Annotation;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateProvider extends DrinkProvider<Date> {

    public static final DateProvider INSTANCE = new DateProvider();

    public static final String FORMAT_STR = "yyyy-MM-dd@HH:mm";
    public static final DateFormat FORMAT = new SimpleDateFormat(FORMAT_STR, Locale.ENGLISH);

    @Override
    public boolean doesConsumeArgument() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public Date provide(CommandArg arg, List<? extends Annotation> annotations) throws CommandExitMessage {
        String s = arg.get();
        try {
            return FORMAT.parse(s);
        } catch (ParseException e) {
            throw new CommandExitMessage("Date must be in format: " + FORMAT_STR);
        }
    }

    @Override
    public String argumentDescription() {
        return "date: " + FORMAT_STR;
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        Calendar calendar = Calendar.getInstance();
        return Collections.singletonList(String.format("%d-%02d-%02d@%02d:%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)));
    }
}
