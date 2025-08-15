package io.github.cspigot.utils.collection;

import com.google.common.collect.Lists;

import java.util.*;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 3/20/2024
 */
public class IndexedConcurrentSet<E> implements Set<E> {

    private final List<E> list = Lists.newCopyOnWriteArrayList();

    @Override
    public synchronized boolean add(E e) {
        if (this.contains(e)) {
            return false;
        }
        return this.list.add(e);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return this.list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return new HashSet<>(this.list).containsAll(c);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    public E get(int index) {
        return this.list.get(index);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.list.retainAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (this.add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.list.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.list.toArray(a);
    }

    public int indexOf(Object o) {
        return this.list.indexOf(o);
    }

}
