package io.github.cspigot.utils.eventbusutil.modernversion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Represents the entrypoint to method callers. This interface is
 * implemented by the generated classes to invoke targeted methods.
 * <p>
 * To generate, use {@link MethodCaller#wrap(Method)} or {@link MethodCaller#wrap(Method, ClassLoader)}.
 * <p>
 * Implementation note: Generating method callers using ASM to invoke private
 * methods is not possible. Therefore, if a private method is requested, the
 * method handles API will be used to generate method callers.
 */
@FunctionalInterface
public interface MethodCaller {

    /**
     * Invokes the method
     *
     * @param instance  Instance to invoke the method on. Can be null for
     *                  static methods.
     * @param arguments Arguments to invoke the method with
     * @return The returned value of the method. Will return null for void
     * methods
     */
    Object call(@Nullable Object instance, Object... arguments);

    /**
     * Creates a {@link MethodCaller} that wraps the given method
     *
     * @param method Method to wrap
     * @return The method caller responsible for invoking
     * the method
     */
    static MethodCaller wrap(@NotNull Method method) {
        return MethodCallerFactory.createFor(method);
    }

    /**
     * Creates a {@link MethodCaller} that wraps the given method,
     * and defines the generated method caller class on the given classloader
     *
     * @param method Method to wrap
     * @param loader Classloader to define the generated method on.
     * @return The method caller responsible for invoking
     * the method
     */
    static MethodCaller wrap(@NotNull Method method, @NotNull ClassLoader loader) {
        return MethodCallerFactory.createFor(method, loader);
    }
}