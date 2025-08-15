package com.destroystokyo.paper.utils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CachedSizeConcurrentLinkedQueue<E> extends ConcurrentLinkedQueue<E> {

    private final AtomicLong cachedSize = new AtomicLong();

    @Override
    public boolean add(@NotNull E e) {
        boolean result = super.add(e);
        if (result) {
            cachedSize.incrementAndGet();
        }
        return result;
    }

    @Nullable
    @Override
    public E poll() {
        E result = super.poll();
        if (result != null) {
            cachedSize.decrementAndGet();
        }
        return result;
    }

    @Override
    public int size() {
        return cachedSize.intValue();
    }

    @Override
    public void clear() {
        super.clear();
        cachedSize.set(0);
    }
}