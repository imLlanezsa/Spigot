package io.github.cspigot.api.command;

import com.google.common.base.Preconditions;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.cspigot.api.command.command.CommandService;
import io.github.cspigot.api.command.command.DrinkCommandService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This is the main class of Drink
 * Drink can be shaded or used as a plugin
 * This class provides the plugin functionality
 * As well, this class can be used to get an instance of a
 * {@link CommandService} for your plugin to init cmds via.
 *
 * @author jonahseguin
 */
public class CommandAPI {

    private static final ConcurrentMap<String, CommandService> services = new ConcurrentHashMap<>();

    /**
     * Get a {@link CommandService} instance to init cmds via
     * - JavaPlugin specific (one per plugin instance)
     *
     * @param javaPlugin your {@link JavaPlugin} instance
     * @return The {@link CommandService} instance
     */
    public static CommandService get(JavaPlugin javaPlugin) {
        Preconditions.checkNotNull(javaPlugin, "JavaPlugin cannot be null");
        return services.computeIfAbsent(javaPlugin.getName(), name -> new DrinkCommandService(javaPlugin));
    }

}
