package net.minecraft.server;

import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class RegistryID<T> implements Registry<T> {

    private final IdentityHashMap<T, Integer> a = new IdentityHashMap<>(512);
    private final List<T> b = new ObjectArrayList<>(512);

    public RegistryID() {}

    public void a(T t0, int i) {
        this.a.put(t0, i);

        while (this.b.size() <= i) {
            this.b.add(null);
        }

        this.b.set(i, t0);
    }

    public int getByValue(T value) { return b(value); } // cSpigot - OBFHELPER
    public int b(T t0) {
        Integer integer = this.a.get(t0);

        return integer == null ? -1 : integer;
    }

    public T get(int i) { return a(i); } // cSpigot - OBFHELPER
    public final T a(int i) {
        return i >= 0 && i < this.b.size() ? this.b.get(i) : null;
    }

    public Iterator<T> iterator() {
        return Iterators.filter(this.b.iterator(), Objects::nonNull);
    }
}
