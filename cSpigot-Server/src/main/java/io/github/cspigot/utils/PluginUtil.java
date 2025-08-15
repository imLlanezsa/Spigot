package io.github.cspigot.utils;

import com.google.common.base.Joiner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.*;
import io.github.cspigot.plugin.ServerSidePlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for managing plugins.
 *
 * @author rylinaux
 */
public class PluginUtil {

    /**
     * Enable a plugin.
     *
     * @param plugin the plugin to enable
     */
    public static void enable(Plugin plugin) {
        if (plugin != null && !plugin.isEnabled()) {
            Bukkit.getPluginManager().enablePlugin(plugin);
        }
    }

    /**
     * Enable all plugins.
     */
    public static void enableAll() {
        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {
            enable(plugin);
        }
    }

    /**
     * Disable a plugin.
     *
     * @param plugin the plugin to disable
     */
    public static void disable(Plugin plugin) {
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Disable all plugins.
     */
    public static void disableAll() {
        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {
            disable(plugin);
        }
    }

    /**
     * Returns the formatted name of the plugin.
     *
     * @param plugin the plugin to format
     * @return the formatted name
     */
    public static String getFormattedName(Plugin plugin) {
        return getFormattedName(plugin, false);
    }

    /**
     * Returns the formatted name of the plugin.
     *
     * @param plugin          the plugin to format
     * @param includeVersions whether to include the version
     * @return the formatted name
     */
    public static String getFormattedName(Plugin plugin, boolean includeVersions) {
        ChatColor color = plugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED;
        String pluginName = color + plugin.getName();
        if (includeVersions) {
            pluginName += " (" + plugin.getDescription().getVersion() + ")";
        }
        return pluginName;
    }

    /**
     * Returns a plugin from an array of Strings.
     *
     * @param args  the array
     * @param start the index to start at
     * @return the plugin
     */
    public static Plugin getPluginByName(String[] args, int start) {
        return getPluginByName(consolidateStrings(args, start));
    }

    /**
     * Returns a plugin from a String.
     *
     * @param name the name of the plugin
     * @return the plugin
     */
    public static Plugin getPluginByName(String name) {
        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {
            if (name.equalsIgnoreCase(plugin.getName())) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Returns a List of plugin names.
     *
     * @return list of plugin names
     */
    public static List<String> getPluginNames(boolean fullName) {
        List<String> plugins = new ArrayList<>();
        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {
            plugins.add(fullName ? plugin.getDescription().getFullName() : plugin.getName());
        }
        return plugins;
    }

    /**
     * Get the version of another plugin.
     *
     * @param name the name of the other plugin.
     * @return the version.
     */
    public static String getPluginVersion(String name) {
        Plugin plugin = getPluginByName(name);
        if (plugin != null && plugin.getDescription() != null) {
            return plugin.getDescription().getVersion();
        }
        return null;
    }

    /**
     * Returns the commands a plugin has registered.
     *
     * @param plugin the plugin to deal with
     * @return the commands registered
     */
    public static String getUsages(Plugin plugin) {

        List<String> parsedCommands = new ArrayList<>();

        Map commands = plugin.getDescription().getCommands();

        if (commands != null) {
            for ( Object o : commands.entrySet() ) {
                Map.Entry thisEntry = (Map.Entry) o;
                if (thisEntry != null) {
                    parsedCommands.add((String) thisEntry.getKey());
                }
            }
        }

        if (parsedCommands.isEmpty())
            return "No commands registered.";

        return Joiner.on(", ").join(parsedCommands);

    }

    /**
     * Find which plugin has a given command registered.
     *
     * @param command the command.
     * @return the plugin.
     */
    public static List<String> findByCommand(String command) {

        List<String> plugins = new ArrayList<>();

        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {

            // Map of commands and their attributes.
            Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();

            if (commands != null) {

                // Iterator for all the plugin's commands.

                for ( Map.Entry<String, Map<String, Object>> commandNext : commands.entrySet() ) {

                    // Current value.
                    // Plugin name matches - return.
                    if (commandNext.getKey().equalsIgnoreCase(command)) {
                        plugins.add(plugin.getName());
                        continue;
                    }

                    // No match - let's iterate over the attributes and see if
                    // it has aliases.

                    for ( Map.Entry<String, Object> attributeNext : commandNext.getValue().entrySet() ) {

                        // Current value.
                        // Has an alias attribute.
                        if (attributeNext.getKey().equals("aliases")) {

                            Object aliases = attributeNext.getValue();

                            if (aliases instanceof String) {
                                if (((String) aliases).equalsIgnoreCase(command)) {
                                    plugins.add(plugin.getName());
                                    continue;
                                }
                            } else {

                                // Cast to a List of Strings.
                                List<String> array = (List<String>) aliases;

                                // Check for matches here.
                                for ( String str : array ) {
                                    if (str.equalsIgnoreCase(command)) {
                                        plugins.add(plugin.getName());
                                    }
                                }

                            }

                        }

                    }
                }

            }

        }

        // No matches.
        return plugins;

    }

    /**
     * Loads and enables a plugin.
     *
     * @param plugin plugin to load
     * @return status message
     */
    private static String load(Plugin plugin) {
        return load(plugin.getName());
    }

    /**
     * Loads and enables a plugin.
     *
     * @param name plugin's name
     * @return status message
     */
    public static String load(String name) {

        Plugin target;

        File pluginDir = new File("plugins");

        if (!pluginDir.isDirectory()) {
            return "Plugin Directory doesn't exist";
        }

        File pluginFile = new File(pluginDir, name + ".jar");

        if (pluginFile.exists() && !pluginFile.isFile()) {
            for ( File f : pluginDir.listFiles() ) {
                if (f.getName().endsWith(".jar")) {
                    try {
                        PluginDescriptionFile desc = ServerSidePlugin.getInstance().getPluginLoader().getPluginDescription(f);
                        if (desc.getName().equalsIgnoreCase(name)) {
                            pluginFile = f;
                            break;
                        }
                    } catch (InvalidDescriptionException e) {
                        return "Could not find the plugin.";
                    }
                }
            }
        }

        try {
            target = Bukkit.getPluginManager().loadPlugin(pluginFile);
        } catch (InvalidDescriptionException e) {
            e.printStackTrace();
            return "Invalid Plugin.yml";
        } catch (InvalidPluginException e) {
            e.printStackTrace();
            return "Invalid Plugin";
        }

        target.onLoad();
        Bukkit.getPluginManager().enablePlugin(target);

        return "Successfully loaded " + target.getName();

    }

    /**
     * Reload a plugin.
     *
     * @param plugin the plugin to reload
     */
    public static void reload(Plugin plugin) {
        if (plugin != null) {
            unload(plugin);
            load(plugin);
        }
    }

    /**
     * Reload all plugins.
     */
    public static void reloadAll() {
        for ( Plugin plugin : Bukkit.getPluginManager().getPlugins() ) {
            reload(plugin);
        }
    }

    /**
     * Unload a plugin.
     *
     * @param plugin the plugin to unload
     * @return the message to send to the user.
     */
    public static String unload(Plugin plugin) {

        String name = plugin.getName();

        PluginManager pluginManager = Bukkit.getPluginManager();

        SimpleCommandMap commandMap = null;

        List<Plugin> plugins = null;

        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadlisteners = true;

        if (pluginManager != null) {

            pluginManager.disablePlugin(plugin);

            try {

                Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                plugins = (List<Plugin>) pluginsField.get(pluginManager);

                Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

                try {
                    Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                    listenersField.setAccessible(true);
                    listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
                } catch (Exception e) {
                    reloadlisteners = false;
                }

                Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);

                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                commands = (Map<String, Command>) knownCommandsField.get(commandMap);

            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                return "Could not unload";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return "Could not unload";
            }

        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null)
            plugins.remove(plugin);

        if (names != null)
            names.remove(name);

        if (listeners != null && reloadlisteners) {
            for ( SortedSet<RegisteredListener> set : listeners.values() ) {
                set.removeIf(value -> value.getPlugin() == plugin);
            }
        }

        if (commandMap != null) {
            for ( Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader) {

            try {

                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(PluginUtil.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {

                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
                Logger.getLogger(PluginUtil.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();

        return "Successfully unloaded " + name;

    }


    /**
     * Returns an array of Strings as a single String.
     *
     * @param args  the array
     * @param start the index to start at
     * @return the array as a String
     */
    public static String consolidateStrings(String[] args, int start) {
        StringBuilder ret = new StringBuilder(args[start]);
        if (args.length > (start + 1)) {
            for ( int i = (start + 1); i < args.length; i++ )
                ret.append(" ").append(args[i]);
        }
        return ret.toString();
    }

}
