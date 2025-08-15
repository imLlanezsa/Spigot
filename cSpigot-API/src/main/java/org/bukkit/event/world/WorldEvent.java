package org.bukkit.event.world;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Represents events within a world
 */
public abstract class WorldEvent extends Event {
    private final World world;

    public WorldEvent(@Nullable World world) {
        this.world = world;
    }

    /**
     * Gets the world primarily involved with this event
     *
     * @return World which caused this event
     */
    public World getWorld() {
        return world;
    }
}
