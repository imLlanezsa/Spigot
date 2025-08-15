package io.github.cspigot.utils.eventbusutil.methods;

import lombok.Getter;

import java.util.function.Function;

public class CallerImpl implements ICaller {

    @Getter
    private final Function<Object, ?> function;

    public CallerImpl(Function<Object, ?> function) {
        this.function = function;
    }

    @Override
    public Object call(Object... parameters) {
        return function.apply(parameters);
    }
}
