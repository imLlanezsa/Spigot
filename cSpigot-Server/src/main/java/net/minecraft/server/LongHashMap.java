package net.minecraft.server;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LongHashMap<V> extends Long2ObjectOpenHashMap<V> {


    public LongHashMap() {
    }

    public LongHashMap(int capacity) {
        super(capacity);
    }

    public V getEntry(long key) {
        return this.get(key);
    }

}
