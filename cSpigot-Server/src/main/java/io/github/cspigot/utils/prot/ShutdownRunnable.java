package io.github.cspigot.utils.prot;

import org.spigotmc.WatchdogThread;

public class ShutdownRunnable implements Runnable {
    @Override
    public void run() {
        WatchdogThread.stopServer();
    }
}
