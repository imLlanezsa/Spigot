package org.bukkit.craftbukkit.scheduler;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.apache.commons.lang.UnhandledException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitWorker;

class CraftAsyncTask extends CraftTask {
    private final LinkedList<BukkitWorker> workers = new LinkedList();
    private final Map<Integer, CraftTask> runners;

    CraftAsyncTask(Map<Integer, CraftTask> runners, Plugin plugin, Runnable task, int id, long delay) {
        super(plugin, task, id, delay);
        this.runners = runners;
    }

    public boolean isSync() {
        return false;
    }

    public void run() {
        Thread thread = Thread.currentThread();
        synchronized(this.workers) {
            if (this.getPeriod() == -2L) {
                return;
            }

            this.workers.add(new BukkitWorker() {
                @Override
                public int getTaskId() {
                    return CraftAsyncTask.this.getTaskId();
                }

                @Override
                public Plugin getOwner() {
                    return CraftAsyncTask.this.getOwner();
                }

                @Override
                public Thread getThread() {
                    return thread;
                }
            });
        }

        Throwable thrown = null;
        boolean var35 = false;

        try {
            var35 = true;
            super.run();
            var35 = false;
        } catch (Throwable t) {
            thrown = t;
            throw new UnhandledException(String.format("Plugin %s generated an exception while executing task %s", this.getOwner().getDescription().getFullName(), this.getTaskId()), t);
        } finally {
            if (var35) {
                synchronized(this.workers) {
                    try {
                        Iterator<BukkitWorker> workers = this.workers.iterator();
                        boolean removed = false;

                        while(workers.hasNext()) {
                            if (((BukkitWorker)workers.next()).getThread() == thread) {
                                workers.remove();
                                removed = true;
                                break;
                            }
                        }

                        if (!removed) {
                            throw new IllegalStateException(String.format("Unable to remove worker %s on task %s for %s", thread.getName(), this.getTaskId(), this.getOwner().getDescription().getFullName()), thrown);
                        }
                    } finally {
                        if (this.getPeriod() < 0L && this.workers.isEmpty()) {
                            this.runners.remove(this.getTaskId());
                        }

                    }

                }
            }
        }

        synchronized(this.workers) {
            try {
                Iterator<BukkitWorker> workers = this.workers.iterator();
                boolean removed = false;

                while(workers.hasNext()) {
                    if (((BukkitWorker)workers.next()).getThread() == thread) {
                        workers.remove();
                        removed = true;
                        break;
                    }
                }

                if (!removed) {
                    throw new IllegalStateException(String.format("Unable to remove worker %s on task %s for %s", thread.getName(), this.getTaskId(), this.getOwner().getDescription().getFullName()), thrown);
                }
            } finally {
                if (this.getPeriod() < 0L && this.workers.isEmpty()) {
                    this.runners.remove(this.getTaskId());
                }

            }

        }
    }

    LinkedList<BukkitWorker> getWorkers() {
        return this.workers;
    }

    boolean cancel0() {
        synchronized(this.workers) {
            this.setPeriod(-2L);
            if (this.workers.isEmpty()) {
                this.runners.remove(this.getTaskId());
            }

            return true;
        }
    }
}
