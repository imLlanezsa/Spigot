package io.github.cspigot.feature.async.profiler;

import lombok.extern.log4j.Log4j2;
import io.github.cspigot.config.SettingsConfig;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class TimeoutAsyncProfiler implements AsyncProfiler {
    private final Timer timer = new Timer("AsyncProfiler");
    private final Map<Thread, Long> threadTimes = new ConcurrentHashMap<>();

    {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final long now = System.currentTimeMillis();
                final long maxDelta = SettingsConfig.ASYNC_TASK_TIMEOUT * 1000L;
                for (Map.Entry<Thread, Long> threadLongEntry : threadTimes.entrySet()) {
                    if (now - threadLongEntry.getValue() > maxDelta) {
                        log.warn("A task on {} took too long to run ({}ms)", threadLongEntry.getKey().getName(), now - threadLongEntry.getValue());
                        log.warn("Attempting Interrupt...");
                        try {
                            for ( StackTraceElement stackTraceElement : threadLongEntry.getKey().getStackTrace() ) {
                                log.warn(stackTraceElement.toString());
                            }

                            threadLongEntry.getKey().interrupt();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        }, 10L, 1000L);
    }
    @Override
    public void onTaskStart() {
        threadTimes.put(Thread.currentThread(), System.currentTimeMillis());
    }

    @Override
    public void onTaskStop() {
        threadTimes.remove(Thread.currentThread());
    }
}
