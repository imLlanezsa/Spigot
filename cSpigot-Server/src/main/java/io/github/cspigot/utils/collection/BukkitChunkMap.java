package io.github.cspigot.utils.collection;

import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 2/27/2024
 */
public class BukkitChunkMap<T> implements Map<Long, T> {

    private final LongObjectHashMap<T> map = new LongObjectHashMap<>();

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey((long) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue((T) value);
    }

    @Override
    public T get(Object key) {
        return map.get((Long) key);
    }

    @Nullable
    @Override
    public T put(Long key, T value) {
        return map.put(key, value);
    }

    @Override
    public T remove(Object key) {
        return map.remove((Long) key);
    }

    @Override
    public void putAll(@NotNull Map<? extends Long, ? extends T> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @NotNull
    @Override
    public Set<Long> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<T> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<Long, T>> entrySet() {
        return map.entrySet();
    }
}
