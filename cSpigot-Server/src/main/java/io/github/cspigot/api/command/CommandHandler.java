package io.github.cspigot.api.command;

import org.bukkit.plugin.java.JavaPlugin;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.parametric.binder.DrinkBinder;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.command.CommandService;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 4/4/2022
 * @version CommandAPI
 */

@SuppressWarnings("unused")
public class CommandHandler {

    private final JavaPlugin plugin;
    private final CommandService service;

    public CommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.service = CommandAPI.get(plugin);
    }

    /**
     * Register a Drink command into the Drink Command Service
     * @param handler Object that has the {@link Command} annotated methods
     * @param name The name of the command to init.
     *             The names of methods within the handler object will be sub-cmds to this name.
     *             If you want to create a default command (just /name), set the name here and in the
     * {@link Command} annotation set name = ""
     * @param aliases (Optional) A list of alternate command names that can be used
     */
    public void register(Object handler, String name, String... aliases) {
        service.register(handler, name, aliases);
    }
    /**
     * Start binding a class type to a {@link DrinkProvider} or instance.
     * @param type The Class type to bind to
     * @param <T> The type of class
     * @return A {@link DrinkBinder} instance to finish the binding
     */
    public <T> DrinkBinder<T> bind(Class<T> type) {
        return service.bind(type);
    }

    /**
     * Register all commands in the specified package
     *
     * @param packageName {@link String} package
     */
    public void registerCommands(String packageName) {
        try {
            service.registerCommands(packageName);
        } catch (Exception e) {
            plugin.getLogger().info("There was an error registering commands!");
            plugin.getLogger().info("Error: " + e.getMessage());
        }
    }

    /**
     * Must be called after all of you cmds have been registered into Drink
     *
     * This registers the command into the Bukkit/Spigot CommandMap so that they can be executed on the server.
     */
    public void registerCommands() {
        service.registerCommands();
    }

    /**
     * This registers all the permissions utilized in the commands
     */
    public void registerPermissions() {
        service.registerPermissions();
    }
}
