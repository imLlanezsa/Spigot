package io.github.cspigot.api.command.command;

import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.CraftServer;
import io.github.cspigot.api.command.exception.CommandRegistrationException;

public class DrinkSpigotRegistry {

    private final DrinkCommandService commandService;
    private SimpleCommandMap commandMap;

    public DrinkSpigotRegistry(DrinkCommandService commandService) {
        this.commandService = commandService;
        commandMap = ((CraftServer)Bukkit.getServer()).getCommandMap();
    }

    public boolean register(DrinkCommandContainer container, boolean unregisterExisting) throws CommandRegistrationException {
        return commandMap.register(container.getCommandService().getPlugin().getName(), container);
    }
}
