package io.github.cspigot.feature.console.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import io.github.cspigot.config.SettingsConfig;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 4/13/2022
 * @version cSpigot
 */

public class LogFilter implements Filter {

    public Filter.Result checkMessage(String msg) {

        // cSpigot start - Hide annoying messages that clog the whole console
        String[] strings = {
                "[ViaVersion] This version of Minecraft is extremely outdated and support for it has reached its end of life",
                "Cluster created with settings",
                "Opened connection [connectionId",
                "Monitor thread successfully connected to server with description ServerDescription"
                //"PacketEvents caught an unhandled exception while calling your listener.",
                //"readerIndex(1) + length(1)",
                //"exceeds writerIndex(1)",
                //"PooledUnsafeDirectByteBuf",
                //"java.lang.IndexOutOfBoundsException: readerIndex(1) + length(1) exceeds writerIndex(1): PooledUnsafeDirectByteBuf(ridx: 1, widx: 1, cap: 256)"
        };
        for (String string : strings) {
            if (msg.contains(string) || msg.equalsIgnoreCase(string)) {
                return Result.DENY;
            }
        }
        // cSpigot end

        if (SettingsConfig.FILTERS_LIST.isEmpty()) {
            return Result.NEUTRAL;
        }

        for ( String message : SettingsConfig.FILTERS_LIST ) {
            if (msg.contains(message) || msg.equalsIgnoreCase(message)) {
                return Result.DENY;
            }
        }

        return Result.NEUTRAL;
    }

    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

    public Result filter(LogEvent event) {
        return checkMessage(event.getMessage().getFormattedMessage());
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object... arg4) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, Object message, Throwable arg4) {
        return checkMessage(message.toString());
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, Message message, Throwable arg4) {
        return checkMessage(message.getFormattedMessage());
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
        return checkMessage(message);
    }

    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13) {
        return checkMessage(message);
    }

    public void initialize() {
    }

    public boolean isStarted() {
        return true;
    }

    public boolean isStopped() {
        return false;
    }

    public void start() {
    }

    public void stop() {
    }

    public State getState() {
        try {
            return State.STARTED;
        } catch (Exception var2) {
            return null;
        }
    }
}
