package io.github.cspigot.utils.eventbusutil.methods;

import java.lang.reflect.InvocationTargetException;

public interface ICaller {

    Object call(Object... parameters) throws InvocationTargetException, IllegalAccessException;
}
