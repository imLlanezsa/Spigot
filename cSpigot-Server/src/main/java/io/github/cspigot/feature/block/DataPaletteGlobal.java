package io.github.cspigot.feature.block;

import net.minecraft.server.RegistryID;

import java.util.function.Predicate;

public class DataPaletteGlobal<T> implements DataPalette<T> {
    private final RegistryID<T> a;
    private final T b;

    public DataPaletteGlobal(RegistryID<T> var0, T var1) {
        this.a = var0;
        this.b = var1;
    }

    public int a(T var0) {
        int var1 = this.a.b(var0);
        return var1 == -1 ? 0 : var1;
    }

    public boolean a(Predicate<T> var0) {
        return true;
    }

    public T a(int var0) {
        T var1 = this.a.a(var0);
        return var1 == null ? this.b : var1;
    }
}
