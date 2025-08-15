package io.github.cspigot.feature.block;

import net.minecraft.server.NBTTagList;
import net.minecraft.server.PacketDataSerializer;

import java.util.function.Predicate;

public interface DataPalette<T> {

    default int getOrCreateIdFor(T object) { return this.a(object); } // Paper - OBFHELPER
    int a(T t0);

    boolean a(Predicate<T> predicate);

    default T getObject(int dataBits) { return this.a(dataBits); } // Paper - OBFHELPER

    T a(int i);


}