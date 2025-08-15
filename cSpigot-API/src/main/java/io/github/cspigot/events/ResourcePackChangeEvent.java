package io.github.cspigot.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/1/2023
 * @version cSpigot
 */
public class ResourcePackChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private String URL;
    private String hash;

    public ResourcePackChangeEvent(Player player, String URL, String hash) {
        super(player);

        this.URL = URL;
        this.hash = hash;
    }

    /**
     * Gets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins
     *
     * @return true if this event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancellation state of this event. A cancelled event will not
     * be executed in the server, but will still pass to other plugins.
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Resource Pack URL
     */
    public String getURL() {
        return URL;
    }

    /**
     * Resource Pack Hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * Set the Resource Pack URL
     */
    public void setURL(String URL) {
        this.URL = URL;
    }

    /**
     * Set the Resource Pack Hash
     */
    public void setHash(String hash) {
        this.hash = hash;
    }


    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
