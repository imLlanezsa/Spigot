package com.destroystokyo.paper.util;

import net.minecraft.server.IBlockData;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 1/26/2024
 */
public class ConcurrentIBlockDataList extends IBlockDataList {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public long add(final int location, final IBlockData data) {
        try {
            lock.writeLock().lockInterruptibly();
            final long curr = this.map.get((short) location);

            if (curr == Long.MAX_VALUE) {
                final int index = this.size++;
                final long raw = getRawFromValues(index, location, data);
                this.map.put((short) location, raw);

                if (index >= this.byIndex.length) {
                    this.byIndex = Arrays.copyOf(this.byIndex, (int) Math.max(4L, this.byIndex.length * 2L));
                }

                this.byIndex[index] = raw;
                return raw;
            } else {
                final int index = getIndexFromRaw(curr);
                final long raw = this.byIndex[index] = getRawFromValues(index, location, data);

                this.map.put((short) location, raw);

                return raw;
            }
        } catch (Throwable t) {
            if (lock.writeLock().isHeldByCurrentThread()) {
                lock.writeLock().unlock();
            }
            throw new RuntimeException("Exception While Locked", t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long remove(final int location) {
        try {
            lock.writeLock().lockInterruptibly();
            final long ret = this.map.remove((short) location);
            final int index = getIndexFromRaw(ret);
            if (ret == Long.MAX_VALUE) {
                return ret;
            }

            // move the entry at the end to this index
            final int endIndex = --this.size;
            final long end = this.byIndex[endIndex];
            if (index != endIndex) {
                // not empty after this call
                this.map.put((short) getLocationFromRaw(end), setIndexRawValues(end, index));
            }
            this.byIndex[index] = end;
            this.byIndex[endIndex] = 0L;

            return ret;
        } catch (Throwable t) {
            if (lock.writeLock().isHeldByCurrentThread()) {
                lock.writeLock().unlock();
            }
            throw new RuntimeException("Exception While Locked", t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        try {
            lock.readLock().lockInterruptibly();
            return this.size;
        } catch (Throwable t) {
            lock.readLock().unlock();
            throw new RuntimeException("Exception While Locked", t);
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getRaw(final int index) {
        try {
            lock.readLock().lockInterruptibly();
            return this.byIndex[index];
        } catch (Throwable t) {
            lock.readLock().unlock();
            throw new RuntimeException("Exception While Locked", t);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        try {
            lock.writeLock().lockInterruptibly();
            this.size = 0;
            this.map.clear();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

}
