package io.github.cspigot.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import io.github.cspigot.api.knockback.IKnockback;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/1/2023
 * @version cSpigot
 */

public class KnockbackSetEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;

    private IKnockback<?, ?, ?> knockback;

    public KnockbackSetEvent(Player player, IKnockback<?, ?, ?> knockback) {
        super(player);
        this.knockback = knockback;
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * The knockback being set
     *
     * @return {@link IKnockback knockback}
     */
    public IKnockback<?, ?, ?> getKnockback() {
        return knockback;
    }

    /**
     * Set the knockback
     *
     * @param knockback {@link IKnockback}
     */
    public void setKnockback(IKnockback<?, ?, ?> knockback) {
        this.knockback = knockback;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
