package org.bukkit.event.inventory;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * This Project is property of Refine Development © 2021
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 11/21/2021
 * @version cSpigot
 */

public class EquipmentSetEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final HumanEntity humanEntity;
    private final int slot;
    private final ItemStack previousItem;
    private final ItemStack newItem;

    public EquipmentSetEvent(HumanEntity humanEntity, int slot, ItemStack previousItem, ItemStack newItem) {
        this.humanEntity = humanEntity;
        this.slot = slot;
        this.previousItem = previousItem;
        this.newItem = newItem;
    }

    public HumanEntity getHumanEntity() {
        return humanEntity;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getPreviousItem() {
        return previousItem;
    }

    public ItemStack getNewItem() {
        return newItem;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }


}

