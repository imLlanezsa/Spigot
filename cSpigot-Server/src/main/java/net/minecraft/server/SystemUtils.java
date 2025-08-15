package net.minecraft.server;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.Hash.Strategy;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SystemUtils {

    public static LongSupplier a = System::nanoTime;
    private static final Logger b = LogManager.getLogger();
    private static final Pattern c = Pattern.compile(".*\\.|(?:CON|PRN|AUX|NUL|COM1|COM2|COM3|COM4|COM5|COM6|COM7|COM8|COM9|LPT1|LPT2|LPT3|LPT4|LPT5|LPT6|LPT7|LPT8|LPT9)(?:\\..*)?", 2);

    public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> a() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <T extends Comparable<T>> String a(IBlockState<T> iblockstate, T object) {
        return iblockstate.a(object); // Paper - decompile fix
    }

    public static long getMonotonicMillis() {
        return getMonotonicNanos() / 1000000L;
    }

    public static long getMonotonicNanos() {
        return System.nanoTime(); // Paper
    }

    public static long getTimeMillis() {
        return Instant.now().toEpochMilli();
    }

    public static OS e() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        return s.contains("win") ? OS.WINDOWS : (s.contains("mac") ? OS.OSX : (s.contains("solaris") ? OS.SOLARIS : (s.contains("sunos") ? OS.SOLARIS : (s.contains("linux") ? OS.LINUX : (s.contains("unix") ? OS.LINUX : OS.UNKNOWN)))));
    }

    public static Stream<String> f() {
        RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();

        return runtimemxbean.getInputArguments().stream().filter((s) -> {
            return s.startsWith("-X");
        });
    }

    public static boolean a(java.nio.file.Path java_nio_file_path) {
        java.nio.file.Path java_nio_file_path1 = java_nio_file_path.normalize();

        return java_nio_file_path1.equals(java_nio_file_path);
    }

    public static boolean b(java.nio.file.Path java_nio_file_path) {
        Iterator iterator = java_nio_file_path.iterator();

        java.nio.file.Path java_nio_file_path1;

        do {
            if (!iterator.hasNext()) {
                return true;
            }

            java_nio_file_path1 = (java.nio.file.Path) iterator.next();
        } while (!SystemUtils.c.matcher(java_nio_file_path1.toString()).matches());

        return false;
    }

    public static java.nio.file.Path a(java.nio.file.Path java_nio_file_path, String s, String s1) {
        String s2 = s + s1;
        java.nio.file.Path java_nio_file_path1 = Paths.get(s2);

        if (java_nio_file_path1.endsWith(s1)) {
            throw new InvalidPathException(s2, "empty resource name");
        } else {
            return java_nio_file_path.resolve(java_nio_file_path1);
        }
    }

    public static <V> V a(FutureTask<V> var0, Logger var1) {
        try {
            var0.run();
            return var0.get();
        } catch (ExecutionException | InterruptedException var3) {
            var1.fatal("Error executing task", var3);
        }
        return null;
    }

    public static <T> T a(List<T> list) {
        return list.get(list.size() - 1);
    }

    public static <T> T a(Iterable<T> iterable, @Nullable T t0) {
        Iterator<T> iterator = iterable.iterator();
        T t1 = iterator.next();

        if (t0 != null) {
            Object object = t1;

            while (object != t0) {
                if (iterator.hasNext()) {
                    object = iterator.next();
                }
            }

            if (iterator.hasNext()) {
                return iterator.next();
            }
        }

        return t1;
    }

    public static <T> T b(Iterable<T> iterable, @Nullable T t0) {
        Iterator<T> iterator = iterable.iterator();

        T object; // Paper - decompile fix
        T object1; // Paper - decompile fix

        for (object1 = null; iterator.hasNext(); object1 = object) {
            object = iterator.next();
            if (object == t0) {
                if (object1 == null) {
                    object1 = iterator.hasNext() ? Iterators.getLast(iterator) : t0;
                }
                break;
            }
        }

        return object1;
    }

    public static <T> T a(Supplier<T> supplier) {
        return supplier.get();
    }

    public static <T> T a(T t0, Consumer<T> consumer) {
        consumer.accept(t0);
        return t0;
    }

    public static <K> Strategy<K> g() {
        return (Strategy<K>) IdentityHashingStrategy.INSTANCE; // Paper - decompile fix
    }

    static enum IdentityHashingStrategy implements Strategy<Object> {

        INSTANCE;

        private IdentityHashingStrategy() {}

        public int hashCode(Object object) {
            return System.identityHashCode(object);
        }

        public boolean equals(Object object, Object object1) {
            return object == object1;
        }
    }

    public static int getVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        } return Integer.parseInt(version);
    }

    public static enum OS {

        LINUX, SOLARIS, WINDOWS {
        },
        OSX {
        },
        UNKNOWN;

        private OS() {}
    }
}
