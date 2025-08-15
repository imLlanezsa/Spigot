package io.github.cspigot.api.command.command;

import org.bukkit.plugin.java.JavaPlugin;
import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Modifier;
import io.github.cspigot.api.command.modifier.DrinkModifier;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.parametric.binder.DrinkBinder;
import io.github.cspigot.api.command.CommandAPI;

import java.lang.annotation.Annotation;

/**
 * Drink CommandServices are {@link JavaPlugin}-specific.
 * Meaning one per JavaPlugin (Spigot/Bukkit Plugin).
 * Since the cmds are associated with the specified command,
 * and are dynamically registered into the server's CommandMap
 * <p>
 * See {@link CommandAPI#get(JavaPlugin)} for getting an instance of a functional {@link CommandService}
 */
public interface CommandService {

    /**
     * Register a Drink command into the Drink Command Service
     * @param handler Object that has the {@link Command} annotated methods
     * @param name The name of the command to init.
     *             The names of methods within the handler object will be sub-cmds to this name.
     *             If you want to create a default command (just /name), set the name here and in the
     * {@link Command} annotation set name = ""
     * @param aliases (Optional) A list of alternate command names that can be used
     * @return The {@link DrinkCommandContainer} containing the command you registered
     */
    DrinkCommandContainer register(Object handler, String name, String... aliases);

    /**
     * Register a sub-command into the specified root command container
     *
     * @param root    The {@link DrinkCommandContainer} super-command to init your sub-cmds into
     * @param handler The object that has the {@link Command}
     *                annotated methods to init
     * @return The {@link DrinkCommandContainer} containing the command you registered (same as the root passed in)
     */
    DrinkCommandContainer registerSub(DrinkCommandContainer root, Object handler);

    /**
     * Must be called after all of you cmds have been registered into Drink with
     * {@link #register(Object, String, String...)} and {@link #registerSub(DrinkCommandContainer, Object)}
     *
     * This registers the command into the Bukkit/Spigot CommandMap so that they can be executed on the server.
     */
    void registerCommands();

    /**
     * This registers all the permissions utilized in the commands
     */
    void registerPermissions();

    /**
     * Register all commands in the specified package
     *
     * @param packageName {@link String} package
     */
    void registerCommands(String packageName) throws InstantiationException, IllegalAccessException;

    /**
     * Start binding a class type to a {@link DrinkProvider} or instance.
     * @param type The Class type to bind to
     * @param <T> The type of class
     * @return A {@link DrinkBinder} instance to finish the binding
     */
    <T> DrinkBinder<T> bind(Class<T> type);

    /**
     * Registers a modifier to modify provided arguments for a specific type
     * @param annotation The annotation to use for the modifier (must have {@link Modifier} annotated in it's class)
     * @param type The type to modify
     * @param modifier The modifier
     * @param <T> The type of class to modify
     */
    <T> void registerModifier(Class<? extends Annotation> annotation, Class<T> type, DrinkModifier<T> modifier);

    /**
     *
     * @param name The primary name of the {@link DrinkCommandContainer} you want to get
     * @return The {@link DrinkCommandContainer} with the specified name
     */
    DrinkCommandContainer get(String name);

    /**
     * Set the authorizer that Drink uses.
     * This will allow you to edit the behavior for checking if a {@link org.bukkit.command.CommandSender} or
     * {@link org.bukkit.entity.Player} has permission to run a command.
     * You can also edit the no-permission message by modifying this, or use {@link DrinkAuthorizer#setNoPermissionMessage(String)}
     * @param authorizer A {@link DrinkAuthorizer} instance to be used for
     *                                  checking authorization for command execution
     */
    void setAuthorizer(DrinkAuthorizer authorizer);

}
