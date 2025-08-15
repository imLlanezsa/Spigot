package net.minecraft.server;

import java.util.concurrent.*;
import java.util.function.Supplier;
import org.bukkit.craftbukkit.util.Waitable;
import io.github.cspigot.feature.async.profiler.AsyncProfiler;
import io.github.cspigot.feature.async.profiler.NoOpAsyncProfiler;

public class AsyncUtil {
	private static final Object NONE_RETURN_VALUE = new Object();
	public static AsyncProfiler ASYNC_PROFILER = new NoOpAsyncProfiler();

	/**
	 * Runs a given task async
	 * @param runnable The task to run
	 */
	public static void run(Runnable runnable) {
		ForkJoinPool.commonPool().execute(runnable);
	}
	
	/**
	 * Runs a given task on a specified Executor
	 * @param runnable The task to run
	 * @param executor The executor to run this task on
	 */
	public static void run(Runnable runnable, Executor executor) {
		executor.execute(runnable);
	}

	public static void runTimed(Runnable runnable, Executor executor) {
		executor.execute(() -> {
			ASYNC_PROFILER.onTaskStart();
			try {
				runnable.run();
			} catch (Throwable t) {
				t.printStackTrace();
			}finally {
				ASYNC_PROFILER.onTaskStop();
			}
		});
	}

	public static void runEntityTracker(int i, int trackerThreads, Runnable runnable, Executor executor) {
		Runnable wrapper = () -> {
			ASYNC_PROFILER.onTaskStart();
			try {
				runnable.run();
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				ASYNC_PROFILER.onTaskStop();
			}
		};
		if (i < trackerThreads) {
			executor.execute(wrapper);
		} else {
			wrapper.run();
		}

	}

	/**
	 * Runs a given task on a specified Executor and returns the result
	 * @param runnable The task to run
	 * @param executor The executor to run this task on
	 */
	public static <T> CompletableFuture<T> runSupplyingAsyncWithFuture(Supplier<T> runnable, Executor executor) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		executor.execute(() -> {
			try {
				ASYNC_PROFILER.onTaskStart();
				future.complete(
						runnable.get()
				);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			} finally {
				ASYNC_PROFILER.onTaskStop();
			}
		});

		return future;
	}

	/**
	 * Runs a given task on a specified Executor and returns the result
	 * @param runnable The task to run
	 * @param executor The executor to run this task on
	 */
	public static CompletableFuture<Object> runAsyncWithFuture(Runnable runnable, Executor executor) {
		final CompletableFuture<Object> future = new CompletableFuture<>();
		executor.execute(() -> {
			try {
				ASYNC_PROFILER.onTaskStart();
				runnable.run();
				future.complete(NONE_RETURN_VALUE);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			} finally {
				ASYNC_PROFILER.onTaskStop();
			}
		});

		return future;
	}

	/**
	 * Runs a given task the next tick on the main thread
	 * @param runnable The task to run
	 */
	public static void runNextTick(Runnable runnable) {
		MinecraftServer.getServer().processQueue.add(runnable);
	}


	/**
	 * Runs a given task after the current tick on the main thread
	 * @param runnable The task to run
	 */
	public static void runPostTick(Runnable runnable) {
		MinecraftServer.getServer().priorityProcessQueue.add(runnable);
	}

	/**
	 * Runs a given task the next tick on the main thread and return value
	 * @param supplier The task to run
	 */
	public static <T> T supplyNextTick(Supplier<T> supplier) {
		Waitable<T> wait = new Waitable<T>() {
			@Override
			protected T evaluate() {
				return supplier.get();
			}
		};

		MCUtil.getProcessQueue().add(wait);
		try {
			return wait.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Runs a given task if it is synchronized on an object
	 * @param monitor The object to check for locking
	 * @param runnable The task to run
	 */
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public static void runSynchronized(Object monitor, Runnable runnable) {
		if (Thread.holdsLock(monitor) ) {
			runnable.run();
		} else {
			synchronized (monitor) {
				runnable.run();
			}
		}
	}

	/**
	 * Runs a given task if it is synchronized on an object
	 * @param monitor The object to check for locking
	 * @param supplier The task to run
	 */
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public static <T> T runSynchronized(Object monitor, Supplier<T> supplier) {
		if (Thread.holdsLock(monitor) ) {
			return supplier.get();
		} else {
			synchronized (monitor) {
				return supplier.get();
			}
		}
	}
}
