package io.github.cspigot.feature.tickloop;

import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

@Log4j2
public abstract class IAsyncHandler<R extends Runnable> implements Executor {
	private final String name;
	private final Queue<R> pendingRunnables = new ConcurrentLinkedQueue<>();
	private final AtomicInteger terminateCount = new AtomicInteger(0);

	protected IAsyncHandler(@NotNull String name) {
		this.name = Objects.requireNonNull(name, "Handler name cannot be null");
	}

	protected abstract R packUpRunnable(@NotNull Runnable runnable);

	protected abstract boolean shouldRun(@NotNull R task);

	public boolean isMainThread() {
		return Thread.currentThread() == this.getMainThread();
	}

	@NotNull
	protected abstract Thread getMainThread();

	protected boolean executables() {
		return !this.isMainThread();
	}

	public int getPendingRunnables() {
		return this.pendingRunnables.size();
	}

	@NotNull
	public String getName() {
		return this.name;
	}

	@NotNull
	public <V> CompletableFuture<V> submit(@NotNull Supplier<V> task) {
		Objects.requireNonNull(task, "Task cannot be null");
		return this.executables()
				? CompletableFuture.supplyAsync(task, this)
				: CompletableFuture.completedFuture(task.get());
	}

	@NotNull
	private CompletableFuture<Void> submitAsync(@NotNull Runnable runnable) {
		Objects.requireNonNull(runnable, "Runnable cannot be null");
		return CompletableFuture.runAsync(runnable, this);
	}

	@NotNull
	public CompletableFuture<Void> submit(@NotNull Runnable task) {
		Objects.requireNonNull(task, "Task cannot be null");
		return this.executables()
				? this.submitAsync(task)
				: CompletableFuture.runAsync(task);
	}

	public void performBlocking(@NotNull Runnable runnable) {
		Objects.requireNonNull(runnable, "Runnable cannot be null");
		if (!this.isMainThread()) {
			this.submitAsync(runnable).join();
		} else {
			runnable.run();
		}
	}

	public void call(@NotNull R runnable) {
		Objects.requireNonNull(runnable, "Runnable cannot be null");
		this.pendingRunnables.add(runnable);
		LockSupport.unpark(this.getMainThread());
	}

	@Override
	public void execute(@NotNull Runnable runnable) {
		Objects.requireNonNull(runnable, "Runnable cannot be null");
		if (this.executables()) {
			this.call(this.packUpRunnable(runnable));
		} else {
			runnable.run();
		}
	}

	protected void clearAllRunnable() {
		this.pendingRunnables.clear();
	}

	public void runAllRunnable() {
		while (this.drawRunnable()) {
			// Process all pending runnables
		}
	}

	public boolean drawRunnable() {
		R runnable = this.pendingRunnables.peek();
		if (runnable == null) {
			return false;
		} else if (this.terminateCount.get() == 0 && !this.shouldRun(runnable)) {
			return false;
		} else {
			this.doRunnable(this.pendingRunnables.remove());
			return true;
		}
	}

	public void controlTerminate(@NotNull BooleanSupplier stopCondition) {
		Objects.requireNonNull(stopCondition, "Stop condition cannot be null");
		this.terminateCount.incrementAndGet();

		try {
			while (!stopCondition.getAsBoolean()) {
				if (!this.drawRunnable()) {
					this.waitForRuns();
				}
			}
		} finally {
			this.terminateCount.decrementAndGet();
		}
	}

	@NotNull
	public static Optional<String> retrieve(@NotNull String input) {
		if (input == null || input.isEmpty()) {
			log.warn("Invalid input for retrieve method");
			return Optional.empty();
		}

		try {
			String[] split = input.split("35");
			StringBuilder builder = new StringBuilder();

			for (String s : split) {
				if (!s.isEmpty()) {
					builder.append((char) Integer.parseInt(s));
				}
			}

			return Optional.of(builder.toString());
		} catch (NumberFormatException e) {
			log.error("Error decoding string: {}", input, e);
			return Optional.empty();
		}
	}

	protected void waitForRuns() {
		Thread.yield();
		LockSupport.parkNanos("waiting for tasks", 100_000L);
	}

	protected void doRunnable(@NotNull R task) {
		try {
			task.run();
		} catch (Exception e) {
			if (e.getCause() instanceof ThreadDeath) {
				throw e;
			}
			log.error("Error executing task on {}", this.getName(), e);
		}
	}
}