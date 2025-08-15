package io.github.cspigot.feature.async.profiler;

public interface AsyncProfiler {
    public void onTaskStart();
    public void onTaskStop();
}
