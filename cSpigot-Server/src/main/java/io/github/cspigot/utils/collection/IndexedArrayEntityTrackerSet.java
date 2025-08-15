package io.github.cspigot.utils.collection;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityTrackerEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.github.cspigot.utils.FastRWLock;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class IndexedArrayEntityTrackerSet implements Set<EntityTrackerEntry> {

    private EntityTrackerEntry[] list = new EntityTrackerEntry[0];
    private int realLength = 0;
    private final ReadWriteLock lock = Boolean.getBoolean("FastLock") ? new FastRWLock() : new ReentrantReadWriteLock();

    @Override
    public boolean add(EntityTrackerEntry e) {
        lock.writeLock().lock();
        if (this.indexOfNoLock(e) >= 0) {
            lock.writeLock().unlock();
            return false;
        }
        addInternal(e);
        lock.writeLock().unlock();
        return true;
    }

    /**
     * Internal method to add an entry, no lock required.
     *
     * @param e
     */
    private void addInternal(EntityTrackerEntry e) {
        if (list.length <= realLength + 1 || list[list.length - 1] != null) {
            list = Arrays.copyOf(list, list.length + Math.min(Math.max(realLength * 2, 4), 100));
        }

        list[realLength] = e;
        realLength++;
    }

    public boolean fastAdd(EntityTrackerEntry e) {
        lock.writeLock().lock();
        addInternal(e);
        lock.writeLock().unlock();
        return true;
    }

    public boolean fastAdd(Iterable<EntityTrackerEntry> e) {
        lock.writeLock().lock();
        for (EntityTrackerEntry entityTrackerEntry : e) {
            addInternal(entityTrackerEntry);
        }
        lock.writeLock().unlock();
        return true;
    }

    @Override
    public boolean remove(Object o) {
        lock.writeLock().lock();
        final int index = indexOfNoLock(o);
        if (index > 0) {
            try {
                list[index] = null;

                IntStream.range(index + 1, realLength).forEach(i2 -> list[i2 - 1] = list[i2]);

                realLength -= 1;

                attemptShrink();
            } catch (IndexOutOfBoundsException e) {
                lock.writeLock().unlock();
                throw e;
            }

            lock.writeLock().unlock();
            return true;
        } else {
            lock.writeLock().unlock();
            return false;
        }
    }

    /**
     * Requires Lock
     */
    private void attemptShrink() {
        if ((this.list.length / 2) + 1 > realLength && this.list.length > 20) {
            list = Arrays.copyOf(list, this.realLength + 1);
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public void clear() {
        this.lock.writeLock().lock();
        realLength = 0;
        this.list = new EntityTrackerEntry[1];
        this.lock.writeLock().unlock();
    }

    public EntityTrackerEntry get(int index) {
        this.lock.readLock().lock();
        try {
            final EntityTrackerEntry returnValue = this.list[index];

            this.lock.readLock().unlock();
            return returnValue;
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            this.lock.readLock().unlock();
            throw arrayIndexOutOfBoundsException;
        }
    }

    public EntityTrackerEntry fastGet(int index) {
        return this.list[index];
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return c.stream().allMatch(this::remove);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        this.lock.writeLock().lock();
        boolean modified = false;
        for (int i = 0; i < list.length; i++) {
            try {
                if (!c.contains(list[i])) {
                    modified = true;

                    list[i] = null;

                    IntStream.range(i + 1, realLength).forEach(i2 -> {
                        list[i2 - 1] = list[i2];
                    });

                    realLength -= 1;
                }
            } catch (IndexOutOfBoundsException e) {
                lock.writeLock().unlock();
                throw e;
            }
        }

        // Shrink the array if needed
        if (modified) {
            final int diff = Math.abs(realLength - list.length);

            if (diff > 5 && diff > realLength / 2) {
                list = Arrays.copyOf(list, realLength + 1);
            }
        }

        this.lock.writeLock().unlock();
        return modified;
    }

    @Override
    public boolean addAll(Collection<? extends EntityTrackerEntry> c) {
        boolean modified = false;
        this.lock.writeLock().lock();
        for (EntityTrackerEntry e : c) {
            if (this.indexOfNoLock(e) >= 0) {
                continue;
            }
            this.addInternal(e);
            modified = true;
        }
        this.lock.writeLock().unlock();
        return modified;
    }

    @Override
    public int size() {
        lock.readLock().lock();
        final int len = realLength;
        lock.readLock().unlock();
        return len;
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        final boolean returnValue = realLength == 0;
        lock.readLock().unlock();

        return returnValue;
    }

    @Override
    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    @Override
    public @NotNull Iterator<EntityTrackerEntry> iterator() {
        return new Iterator<EntityTrackerEntry>() {
            private int index = 0;
            private @Nullable EntityTrackerEntry next = null;

            @Override
            public boolean hasNext() {
                lock.readLock().lock();
                boolean hasNext = index < realLength;
                // Cache next
                if (hasNext) {
                    next = list[index];
                } else {
                    next = null;
                }
                lock.readLock().unlock();
                return hasNext;
            }

            @Override
            public EntityTrackerEntry next() {
                index++;
                return next;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return list;
    }

    @Override
    public <T> T[] toArray(T @NotNull [] a) {
        lock.readLock().lock();
        if (realLength > a.length) {
            lock.readLock().unlock();
            return (T[]) list;
        }

        for (int i = 0; i < realLength - 1; i++) {
            a[i] = (T) list[i];
        }

        lock.readLock().unlock();

        return a;
    }

    public int indexOf(Object o) {
        lock.readLock().lock();

        try {
            if (!(o instanceof EntityTrackerEntry) || ((EntityTrackerEntry) o).tracker == null) {
                lock.readLock().unlock();
                return -1;
            }
            final int wantedId = ((EntityTrackerEntry) o).tracker.getId();
            for (int i = 0; i < realLength; i++) {
                final EntityTrackerEntry entityTrackerEntry = list[i];
                if (entityTrackerEntry == null) continue;
                if (entityTrackerEntry.tracker.getId() == wantedId) {
                    lock.readLock().unlock();
                    return i;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        lock.readLock().unlock();

        return -1;
    }

    private int indexOfNoLock(Object o) {
        if (!(o instanceof EntityTrackerEntry)) return -1;

        try {
            final @Nullable Entity tracker = ((EntityTrackerEntry) o).tracker;

            if (tracker == null) return -1;

            final int wantedId = tracker.getId();
            for (int i = 0; i < realLength; i++) {
                final EntityTrackerEntry entityTrackerEntry = list[i];
                if (entityTrackerEntry == null) continue;
                if (entityTrackerEntry.tracker.getId() == wantedId) {
                    return i;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return -1;
    }

    public Stream<EntityTrackerEntry> unsafeStream() {
        return Arrays.stream(this.list);
    }

    @Override
    public void forEach(Consumer<? super EntityTrackerEntry> action) {
        Objects.requireNonNull(action);
        try {
            this.lock.readLock().lock();
            for (EntityTrackerEntry t : this.list) {
                if (t != null) action.accept(t);
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

}