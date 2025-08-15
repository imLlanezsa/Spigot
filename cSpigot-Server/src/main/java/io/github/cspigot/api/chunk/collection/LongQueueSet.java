package io.github.cspigot.api.chunk.collection;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/5/2023
 * @version cSpigot
 */
public class LongQueueSet implements Queue<Long> {

    private final LongOpenHashSet set = new LongOpenHashSet();

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Long)) return false;
        return set.contains((long) o);
    }

    @NotNull
    @Override
    public Iterator<Long> iterator() {
        return set.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(Long t) {
        return this.set.add((long) t);
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Long)) return false;
        return this.set.remove((long) o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Long> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        this.set.clear();
    }

    @Override
    public boolean offer(Long t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long poll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long peek() {
        throw new UnsupportedOperationException();
    }
}
