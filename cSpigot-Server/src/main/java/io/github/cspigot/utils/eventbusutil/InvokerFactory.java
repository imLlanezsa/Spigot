package io.github.cspigot.utils.eventbusutil;

import io.github.cspigot.utils.eventbusutil.methods.ICaller;
import io.github.cspigot.utils.eventbusutil.methods.MethodAccessManager;

import java.lang.reflect.Method;

public class InvokerFactory {

    private static final InvokerFactory INSTANCE = new InvokerFactory();

    public static InvokerFactory getInstance() {
        return INSTANCE;
    }

    private final MethodAccessManager manager = new MethodAccessManager();

    public MethodAccessManager getManager() {
        return manager;
    }

    public ICaller convert(Method method, Object instance) {
        return manager.getMethod(method, instance);
    }
}
