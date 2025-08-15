package io.github.cspigot.utils.eventbusutil.modernversion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import io.github.cspigot.utils.eventbusutil.util.UnsafeAccessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

/**
 * Handles the magic of generating classes
 */
// Credits: https://github.com/Revxrsal/asm-invoke-util
public final class MethodCallerFactory {
    private static final AtomicInteger IDS = new AtomicInteger();
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final String OBJECT = OBJECT_TYPE.getInternalName();
    private static final String[] INTERFACES = new String[]{Type.getInternalName(MethodCaller.class)};
    private static final Method CONSTRUCTOR = new Method("<init>", "()V");
    private static final Method INVOKE = new Method("call", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");

    private MethodCallerFactory() {}

    public static MethodCaller createFor(@NotNull java.lang.reflect.Method method) {
        return createFor(method, method.getDeclaringClass().getClassLoader());
    }

    public static MethodCaller createFor(@NotNull java.lang.reflect.Method method, @NotNull ClassLoader loader) {
        Objects.requireNonNull(method, "method cannot be null!");
        Objects.requireNonNull(loader, "class loader cannot be null!");
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        if (Modifier.isPrivate(method.getModifiers())) {
            return getReflectionCaller(method, isStatic);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String internalClassName = getCallerName(method);
        String className = internalClassName.replace('/', '.');
        Type declaringClass = Type.getType(method.getDeclaringClass());

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, internalClassName, null, OBJECT, INTERFACES);

        GeneratorAdapter gen = new GeneratorAdapter(ACC_PUBLIC, CONSTRUCTOR, null, null, writer);
        gen.loadThis();
        gen.invokeConstructor(OBJECT_TYPE, CONSTRUCTOR);
        gen.returnValue();
        gen.endMethod();

        gen = new GeneratorAdapter(ACC_PUBLIC, INVOKE, null, null, writer);

        if (!isStatic) {
            gen.loadArg(0);
            gen.checkCast(declaringClass);
        }
        int index = 0;
        for (Parameter parameter : method.getParameters()) {
            gen.loadArg(1);
            gen.push(index++);
            gen.arrayLoad(OBJECT_TYPE);
            if (parameter.getType().isPrimitive())
                gen.unbox(Type.getType(parameter.getType()));
            else
                gen.checkCast(Type.getType(parameter.getType()));
        }
        if (isStatic)
            gen.invokeStatic(declaringClass, Method.getMethod(method));
        else
            gen.invokeVirtual(declaringClass, Method.getMethod(method));

        if (method.getReturnType() == Void.TYPE)
            gen.push((String) null);
        else if (method.getReturnType().isPrimitive())
            gen.box(Type.getType(method.getReturnType()));

        gen.returnValue();
        gen.endMethod();

        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        try {
            MethodCaller caller = (MethodCaller) UnsafeAccessor.defineClass(className, bytes, 0, bytes.length, loader, null)
                    .asSubclass(MethodCaller.class)
                    .newInstance();
            int paramCount = method.getParameterCount();
            return wrap(isStatic, paramCount, className, caller);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull private static MethodCaller getReflectionCaller(java.lang.reflect.Method method, boolean isStatic) {
        MethodHandle handle;
        try {
            method.setAccessible(true);
            handle = MethodHandles.lookup().unreflect(method);
            return wrap(isStatic, method.getParameterCount(), null, (instance, arguments) -> {
                try {
                    List<Object> argumentsList = new ArrayList<>();
                    if (!isStatic) argumentsList.add(instance);
                    Collections.addAll(argumentsList, arguments);
                    return handle.invokeWithArguments(argumentsList);
                } catch (InvocationTargetException e) {
                    sneakyThrow(e.getCause());
                    return null;
                } catch (Throwable e) {
                    sneakyThrow(e);
                    return null;
                }
            });
        } catch (Exception e) {
            sneakyThrow(e);
            return null;
        }
    }

    private static MethodCaller wrap(boolean isStatic,
                                     int paramCount,
                                     String className,
                                     MethodCaller caller) {
        return (instance, arguments) -> {
            try {
                if (instance == null && !isStatic)
                    throw new IllegalStateException("This method is not static, and no instance was provided!");
                if ((arguments.length) != paramCount)
                    throw new IllegalStateException("Invalid argument type: " + arguments.length + ". Expected " + paramCount);
                return caller.call(instance, arguments);
            } catch (Throwable throwable) {
                sanitizeStackTrace(className, throwable);
                sneakyThrow(throwable);
                return null;
            }
        };
    }

    private static final List<String> STRIPPED_CLASS_NAMES = Arrays.asList(
            MethodCallerFactory.class.getName(),
            MethodCaller.class.getName()
    );

    /**
     * Strips all the stack trace elements that meet the criteria of any
     * filter.
     *
     * @param className The generated class name to drop
     * @param throwable Throwable to strip
     */
    private static void sanitizeStackTrace(@Nullable String className, @NotNull Throwable throwable) {
        if (throwable.getCause() != null)
            sanitizeStackTrace(className, throwable.getCause());
        List<StackTraceElement> trace = new ArrayList<>();
        Collections.addAll(trace, throwable.getStackTrace());
        trace.removeIf(element -> element.getClassName().equals(className) || STRIPPED_CLASS_NAMES.contains(element.getClassName()));
        throwable.setStackTrace(trace.toArray(new StackTraceElement[0]));
    }

    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    private static boolean isPackagePrivate(java.lang.reflect.Method method) {
        int mods = method.getModifiers();
        return !Modifier.isPrivate(mods) && !Modifier.isProtected(mods) && !Modifier.isPublic(mods);
    }

    @NotNull private static String getCallerName(@NotNull java.lang.reflect.Method method) {
        return (method.getDeclaringClass().getName() + "MethodCaller" + IDS.getAndIncrement()).replace('.', '/');
    }
}