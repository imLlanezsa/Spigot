package io.github.cspigot.feature.eventbus;

import org.jetbrains.annotations.Nullable;
import io.github.cspigot.utils.eventbusutil.InvokerFactory;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.Warning;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPluginLoader;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.eventbusutil.methods.ICaller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class cSpigotPluginLoader extends JavaPluginLoader {

    public static String classLoaderHash = "kZzRBq4nX896BsPNdArq35KnadYrGew2TrEVzUaUt5XrNkvz4yyg4jGLANt75itBfueka7DGkhqH";
    public static String pluginClassLoaderDomain = "7q2NwqK4gsfBtfSUUW3z2Rv2xkZAsqaoTAnbtzMERMSF46heFDohxVUqL8wrouTe8xT3DA8Ugfd5";

    /**
     * This class was not meant to be constructed explicitly
     *
     * @param instance the server instance
     */
    public cSpigotPluginLoader(Server instance) {
        super(instance);
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, final Plugin plugin) {
        Validate.notNull(plugin, "Plugin can not be null");
        Validate.notNull(listener, "Listener can not be null");

        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
        Set<Method> methods;
        try {
            Method[] publicMethods = listener.getClass().getMethods();
            Method[] privateMethods = listener.getClass().getDeclaredMethods();
            methods = new HashSet<>(publicMethods.length + privateMethods.length, 1.0f);
            methods.addAll(Arrays.asList(publicMethods));
            methods.addAll(Arrays.asList(privateMethods));
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().severe("Plugin " + plugin.getDescription().getFullName() + " has failed to register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist.");
            return ret;
        }

        for (final Method method : methods) {
            final EventHandler eh = method.getAnnotation(EventHandler.class);
            if (eh == null) continue;
            // Do not register bridge or synthetic methods to avoid event duplication
            // Fixes SPIGOT-893
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                plugin.getLogger().severe(plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
                continue;
            }

            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);

            Set<RegisteredListener> eventSet = ret.computeIfAbsent(eventClass, k -> new HashSet<>());
            for (Class<?> clazz = eventClass; Event.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
                // This loop checks for extending deprecated events
                if (clazz.getAnnotation(Deprecated.class) != null) {
                    Warning warning = clazz.getAnnotation(Warning.class);
                    Warning.WarningState warningState = server.getWarningState();
                    if (!warningState.printFor(warning)) {
                        break;
                    }
                    plugin.getLogger().log(
                            Level.WARNING,
                            String.format(
                                    "\"%s\" has registered a listener for %s on method \"%s\", but the event is Deprecated." +
                                            " \"%s\"; please notify the authors %s.",
                                    plugin.getDescription().getFullName(),
                                    clazz.getName(),
                                    method.toGenericString(),
                                    (warning != null && warning.reason().length() != 0) ? warning.reason() : "Server performance will be affected",
                                    Arrays.toString(plugin.getDescription().getAuthors().toArray())),
                            warningState == Warning.WarningState.ON ? new AuthorNagException(null) : null);
                    break;
                }
            }

            if (!SettingsConfig.OPTIMIZED_EVENT_BUS) {
                // Spigot
                EventExecutor executor = new co.aikar.timings.TimedEventExecutor(new EventExecutor() { // Spigot
                    public void execute(Listener listener, Event event) throws EventException {
                        try {
                            if (!eventClass.isAssignableFrom(event.getClass())) {
                                return;
                            }
                            method.invoke(listener, event);
                        } catch (@Nullable InvocationTargetException ex) {
                            if (ex != null && ex.getCause() != null) {
                                throw new EventException(ex.getCause());
                            } else {
                                if (ex != null) {
                                    throw new EventException(ex);
                                } else {
                                    throw new EventException("An exception was triggered while invoking a listener.");
                                }
                            }
                        } catch (Throwable t) {
                            throw new EventException(t);
                        }
                    }
                }, plugin, method, eventClass); // Spigot
                eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
            } else {
                // Optimized EventBus
                final ICaller caller = InvokerFactory.getInstance().convert(method, listener);
                final ICastValidator castValidator = !SettingsConfig.LIGHT_INSTANCE_CHECK ? eventClass::isInstance : (obj) -> obj != null && eventClass.isAssignableFrom(obj.getClass());
                // Timings = gay
                // Disable timings system here since timings was removed from cSpigot
                EventExecutor executor = (listener1, event) -> {
                    try {
                        if (!castValidator.check(event)) {
                            return;
                        }

                        caller.call(listener1, event);
                    } catch (@Nullable Throwable t) {
                        if (t != null) {
                            // fix fawe
                            if (t instanceof ClassCastException) {
                                // Revert to bukkit
                                try {
                                    method.invoke(listener, event);
                                } catch (@Nullable InvocationTargetException ex) {
                                    if (ex != null && ex.getCause() != null) {

                                        throw new EventException(ex.getCause(), "An exception was thrown while invoking a listener.");
                                    } else {
                                        if (ex != null) {
                                            throw new EventException(ex, "An exception was thrown while invoking a listener.");
                                        } else {
                                            throw new EventException("An exception was thrown while invoking a listener.");
                                        }
                                    }
                                } catch (Throwable e) {
                                    throw new EventException(e, "An exception was thrown while invoking a listener.");
                                }
                                // End raw bukkit
                            } else {
                                throw new EventException(t, "An exception was thrown while invoking a listener.");
                            }
                        } else throw new EventException("Exception was null");
                    }
                };

                eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
            }
        }
        return ret;
    }
}
