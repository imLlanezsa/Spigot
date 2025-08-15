package io.github.cspigot.utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A somewhat unsafe, ReadWriteLock that is more suited to our high read low write but please do not rape the cpu workloads.
 */
public class FastRWLock implements ReadWriteLock {
    private final Object synchronizer = new Object();
    private final Object writeThreadLock = new Object();
    private final ArrayDeque<Thread> unparkQueue = new ArrayDeque<>();
    private Thread writeThread = null;
    private boolean writeLocked = false;
    private final AtomicInteger readLocks = new AtomicInteger(0);
    private final Lock readLock = new Lock() {
        @Override
        public void lock() {
            while (true) {
                synchronized (synchronizer) {
                    if (!writeLocked) {
                        readLocks.incrementAndGet();
                        return;
                    }

                    unparkQueue.add(Thread.currentThread());
                }

                LockSupport.park();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.lock();
        }

        @Override
        public boolean tryLock() {
            synchronized (synchronizer) {
                if (!writeLocked) {
                    readLocks.incrementAndGet();
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            boolean hasTried = false;
            while (true) {
                synchronized (synchronizer) {
                    if (!writeLocked) {
                        readLocks.incrementAndGet();
                        return true;
                    }

                    if (hasTried) {
                        return false;
                    } else {
                        hasTried = true;
                        unparkQueue.add(Thread.currentThread());
                    }
                }

                LockSupport.park();
            }
        }

        @Override
        public void unlock() {
            // DO NOT USE THE SYNCHRONIZED STUFF HERE
            readLocks.decrementAndGet();
            synchronized (writeThreadLock) {
                if (writeThread != null) {
                    LockSupport.unpark(writeThread);
                }
            }
        }

        @NotNull
        @Override
        public Condition newCondition() {
            throw new IllegalStateException("Not supported.");
        }
    };

    private final Lock writeLock = new Lock() {
        @Override
        public void lock() {
            synchronized (synchronizer) {
                while (readLocks.get() > 0) {
                    synchronized (writeThreadLock) {
                        writeThread = Thread.currentThread();
                    }

                    LockSupport.parkNanos(2000); // Park with a timeout to avoid issues. This timeout in theory should never be hit.
                }
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            this.lock();
        }

        @Override
        public boolean tryLock() {
            synchronized (synchronizer) {
                if (!writeLocked) {
                    writeLocked = true;
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
            throw new IllegalStateException("Not supported.");
        }

        @Override
        public void unlock() {
            synchronized (synchronizer) {
                writeLocked = false;
                // Anti race condition, make sure the other threads are *probably* safely locked by the time we unlock them.
                // Plus, 1/3 of a microsecond should not hurt much on a write-lock unlock
                LockSupport.parkNanos(1000 / 3);
                // Unlock the threads.
                Thread thread;
                while ((thread = unparkQueue.poll()) != null) {
                    LockSupport.unpark(thread);
                }
            }
        }

        @NotNull
        @Override
        public Condition newCondition() {
            throw new IllegalStateException("Not supported.");
        }
    };


    @NotNull
    @Override
    public Lock readLock() {
        return readLock;
    }

    @NotNull
    @Override
    public Lock writeLock() {
        return writeLock;
    }
}
