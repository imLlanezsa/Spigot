package io.github.cspigot.utils.eventbusutil.util;

import lombok.SneakyThrows;

import net.minecraft.server.SystemUtils;
import io.github.cspigot.utils.eventbusutil.methods.CallerImpl;
import io.github.cspigot.utils.eventbusutil.methods.ICaller;
import io.github.cspigot.utils.eventbusutil.writer.MethodAccessWriter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.function.Function;

public class UnsafeAccessor {

    private static final Class UNSAFE_CLASS;
    private static final Object UNSAFE;

    private static final ICaller DEFINE_ANONYMOUS;
    private static final ICaller ALLOCATE;
    private static final MethodHandle DEFINE_CLASS_HANDLE;

    public static final boolean IS_SERVER_GAY;

    static {
        IS_SERVER_GAY = SystemUtils.getVersion() >= 15;

        try {

            Class unsafeClass;

            try {
                unsafeClass = Class.forName("sun.misc.Unsafe");

                if (SystemUtils.getVersion() >= 15) throw new IllegalStateException("this should not be possible.");
            } catch (Throwable t) {
                unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
            }

            UNSAFE_CLASS = unsafeClass;

            if (!IS_SERVER_GAY) {
                Object copy = null;
                try {
                    Field unsafeField = UNSAFE_CLASS.getDeclaredField("theUnsafe");

                    if (!unsafeField.isAccessible()) unsafeField.setAccessible(true);
                    copy = unsafeField.get(null);
                } catch (final Throwable suppressed) {
                    try {
                        copy = ModuleSupport.makeMethodAccessor(UNSAFE_CLASS.getDeclaredMethod("getUnsafe")).invoke();
                    } catch (Throwable t) {
                        t.addSuppressed(suppressed);
                        t.printStackTrace();

                        throw new ExceptionInInitializerError(t);
                    }
                }

                UNSAFE = copy;
            } else {
                Method unsafeMethod = UNSAFE_CLASS.getDeclaredMethod("getUnsafe");
                UNSAFE = ModuleSupport.makeMethodAccessor(unsafeMethod).invoke();
            }
            if (IS_SERVER_GAY) {
                DEFINE_ANONYMOUS = null;
                ALLOCATE = null;

                final Method defineClassMethod = UNSAFE_CLASS
                        .getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class,
                                ClassLoader.class, ProtectionDomain.class);

                DEFINE_CLASS_HANDLE = ModuleSupport.makeMethodAccessor(defineClassMethod);
            } else {
                final Method DEFINE_ANONYMOUS_METHOD = UNSAFE_CLASS.getDeclaredMethod("defineAnonymousClass", Class.class, byte[].class, Object[].class);
                final Method ALLOCATE_METHOD = UNSAFE_CLASS.getDeclaredMethod("allocateInstance", Class.class);

                DEFINE_ANONYMOUS = new CallerImpl((Function) ALLOCATE_METHOD.invoke(UNSAFE, DEFINE_ANONYMOUS_METHOD.invoke(UNSAFE, DEFINE_ANONYMOUS_METHOD.getDeclaringClass(), new MethodAccessWriter(DEFINE_ANONYMOUS_METHOD).toByteArray(), null)));
                ALLOCATE = new CallerImpl((Function) ALLOCATE_METHOD.invoke(UNSAFE, DEFINE_ANONYMOUS_METHOD.invoke(UNSAFE, ALLOCATE_METHOD.getDeclaringClass(), new MethodAccessWriter(ALLOCATE_METHOD).toByteArray(), null)));

                DEFINE_CLASS_HANDLE = null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    public static Class defineAnonymous(Class clz, byte[] bytes) {
        return (Class) DEFINE_ANONYMOUS.call(UNSAFE, clz, bytes, null);
    }

    @SneakyThrows
    public static Class defineClass(String name, byte[] b, int off, int len,
                                    ClassLoader loader,
                                    ProtectionDomain protectionDomain) {
        return (Class) DEFINE_CLASS_HANDLE.invoke(UNSAFE, name, b, off, len, loader, protectionDomain);
    }

    @SneakyThrows
    public static <T> T allocate(Class<T> clz) {
        return (T) ALLOCATE.call(UNSAFE, clz);
    }

}
