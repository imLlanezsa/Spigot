package org.bukkit.craftbukkit.util;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.ExceptionWorldConflict;
import net.minecraft.server.MinecraftServer;

@Log4j2
public class ServerShutdownThread extends Thread {

    private final MinecraftServer server;

    public static boolean handledConsole = false;

    public ServerShutdownThread(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            org.spigotmc.AsyncCatcher.enabled = false; // Spigot
            org.spigotmc.AsyncCatcher.shuttingDown = true; // Paper
            if (!server.hasStopped()) {
                server.stop();
            }
        } catch (ExceptionWorldConflict ex) {
            ex.printStackTrace();
        }
    }
}
