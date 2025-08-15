package io.github.cspigot.feature.console.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/30/2022
 * @version cSpigot
 */
public final class LogFullPolicy implements AsyncQueueFullPolicy {

    @Override
    public EventRoute getRoute(final long backgroundThreadId, final Level level) {
        return EventRoute.ENQUEUE;
    }
}
