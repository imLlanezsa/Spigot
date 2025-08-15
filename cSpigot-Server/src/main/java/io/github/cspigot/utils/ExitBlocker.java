package io.github.cspigot.utils;

import java.security.Permission;

/**
 * Security manager implementation to prevent JVM exits.
 *
 * @author mathssw, Sakiio_
 * @version cSpigot
 * @since 2025-05-03
 */
public class ExitBlocker extends SecurityManager {

    /**
     * Activates the exit blocking security manager.
     */
    public static void activate() {
        System.setSecurityManager(new ExitBlocker());
    }

    /**
     * Intercepts exit calls and logs the requested status code instead of exiting.
     *
     * @param status The exit status code being requested
     */
    @Override
    public void checkExit(int status) {
        System.out.println("Exiting with status code " + status);
        Thread.dumpStack();
    }

    /**
     * Empty permission check to allow other operations.
     *
     * @param perm The permission being checked
     */
    @Override
    public void checkPermission(Permission perm) {
    }

    /**
     * Empty contextual permission check to allow other operations.
     *
     * @param perm The permission being checked
     * @param context The context for the permission check
     */
    @Override
    public void checkPermission(Permission perm, Object context) {
    }
}