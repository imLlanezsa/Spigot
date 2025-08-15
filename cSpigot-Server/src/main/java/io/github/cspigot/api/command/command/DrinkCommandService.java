package io.github.cspigot.api.command.command;

import com.google.common.base.Preconditions;
import io.github.cspigot.api.command.exception.*;
import io.github.cspigot.api.command.provider.*;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.cspigot.api.command.annotation.Text;
import io.github.cspigot.api.command.exception.*;
import io.github.cspigot.api.command.modifier.DrinkModifier;
import io.github.cspigot.api.command.parametric.BindingContainer;
import io.github.cspigot.api.command.parametric.DrinkBinding;
import io.github.cspigot.api.command.parametric.DrinkProvider;
import io.github.cspigot.api.command.parametric.ProviderAssigner;
import io.github.cspigot.api.command.parametric.binder.DrinkBinder;
import io.github.cspigot.api.command.provider.*;
import io.github.cspigot.api.command.provider.spigot.CommandSenderProvider;
import io.github.cspigot.api.command.provider.spigot.PlayerProvider;
import io.github.cspigot.api.command.provider.spigot.PlayerSenderProvider;
import io.github.cspigot.api.command.util.ClassUtil;
import io.github.cspigot.api.command.annotation.Duration;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.api.command.argument.ArgumentParser;
import io.github.cspigot.api.command.argument.CommandArgs;
import io.github.cspigot.api.command.modifier.ModifierService;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class DrinkCommandService implements CommandService {

    public static String DEFAULT_KEY = "DRINK_DEFAULT";

    private final JavaPlugin plugin;
    private final CommandExtractor extractor;
    private final DrinkHelpService helpService;
    private final ProviderAssigner providerAssigner;
    private final ArgumentParser argumentParser;
    private final ModifierService modifierService;
    private final DrinkSpigotRegistry spigotRegistry;
    private final FlagExtractor flagExtractor;
    private DrinkAuthorizer authorizer;

    private final ConcurrentMap<String, DrinkCommandContainer> commands = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, BindingContainer<?>> bindings = new ConcurrentHashMap<>();

    public DrinkCommandService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.extractor = new CommandExtractor(this);
        this.helpService = new DrinkHelpService(this);
        this.providerAssigner = new ProviderAssigner(this);
        this.argumentParser = new ArgumentParser(this);
        this.modifierService = new ModifierService(this);
        this.spigotRegistry = new DrinkSpigotRegistry(this);
        this.flagExtractor = new FlagExtractor(this);
        this.authorizer = new DrinkAuthorizer();

        this.bindDefaults();
    }

    private void bindDefaults() {
        bind(Boolean.class).toProvider(BooleanProvider.INSTANCE);
        bind(boolean.class).toProvider(BooleanProvider.INSTANCE);
        bind(Double.class).toProvider(DoubleProvider.INSTANCE);
        bind(double.class).toProvider(DoubleProvider.INSTANCE);
        bind(Integer.class).toProvider(IntegerProvider.INSTANCE);
        bind(int.class).toProvider(IntegerProvider.INSTANCE);
        bind(Long.class).toProvider(LongProvider.INSTANCE);
        bind(long.class).toProvider(LongProvider.INSTANCE);
        bind(String.class).toProvider(StringProvider.INSTANCE);
        bind(String.class).annotatedWith(Text.class).toProvider(TextProvider.INSTANCE);
        bind(Date.class).toProvider(DateProvider.INSTANCE);
        bind(Date.class).annotatedWith(Duration.class).toProvider(DurationProvider.INSTANCE);

        bind(CommandArgs.class).toProvider(CommandArgsProvider.INSTANCE);

        bind(CommandSender.class).annotatedWith(Sender.class).toProvider(CommandSenderProvider.INSTANCE);
        bind(Player.class).annotatedWith(Sender.class).toProvider(PlayerSenderProvider.INSTANCE);
        bind(Player.class).toProvider(new PlayerProvider(plugin));
    }

    @Override
    public void setAuthorizer(DrinkAuthorizer authorizer) {
        Preconditions.checkNotNull(authorizer, "Authorizer cannot be null");
        this.authorizer = authorizer;
    }

    @Override
    public void registerCommands() {
        commands.values().forEach(cmd -> spigotRegistry.register(cmd, cmd.isOverrideExistingCommands()));
    }

    @Override
    public void registerPermissions() {
        for ( DrinkCommandContainer container : this.commands.values() ) {
            container.getCommands().values().forEach(cmd -> {
                if (cmd.getPermission() != null) {
                    PluginManager pluginManager = plugin.getServer().getPluginManager();
                    Permission permission = new Permission(cmd.getPermission(), "A permission registered by Refine CommandAPI", PermissionDefault.OP);

                    if (pluginManager.getPermissions().stream().noneMatch(perm -> perm.getName().equalsIgnoreCase(permission.getName()))) {
                        pluginManager.addPermission(permission);
                    }
                }
            });
        }
    }


    /**
     * Register all commands in the specified package
     *
     * @param packageName {@link String} package
     */
    @Override
    public void registerCommands(String packageName) throws InstantiationException, IllegalAccessException {
        for (Class<?> clazz : ClassUtil.getClassesInPackage(plugin, packageName)) {
            if (clazz.isInstance(DrinkProvider.class)) continue; // provider check
            if (clazz.getAnnotation(Register.class) == null) continue;

            Register annotation = clazz.getAnnotation(Register.class);
            Object instance = clazz.newInstance();

            this.register(instance, annotation.name(), annotation.aliases());
        }
    }

    @Override
    public DrinkCommandContainer register(Object handler, String name, String... aliases) throws CommandRegistrationException {
        Preconditions.checkNotNull(handler, "Handler object cannot be null");
        Preconditions.checkNotNull(name, "Name cannot be null");
        Preconditions.checkState(name.length() > 0, "Name cannot be empty (must be > 0 characters in length)");
        Set<String> aliasesSet = new HashSet<>();
        if (aliases != null) {
            aliasesSet.addAll(Arrays.asList(aliases));
            aliasesSet.removeIf(s -> s.length() == 0);
        }
        try {
            Map<String, DrinkCommand> extractCommands = extractor.extractCommands(handler);
            if (extractCommands.isEmpty()) {
                throw new CommandRegistrationException("There were no cmds to init in the " + handler.getClass().getSimpleName() + " class (" + extractCommands.size() + ")");
            }
            DrinkCommandContainer container = new DrinkCommandContainer(this, handler, name, aliasesSet, extractCommands);
            commands.put(getCommandKey(name), container);
            return container;
        } catch (MissingProviderException | CommandStructureException ex) {
            throw new CommandRegistrationException("Could not init command '" + name + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public DrinkCommandContainer registerSub(DrinkCommandContainer root, Object handler) {
        Preconditions.checkNotNull(root, "Root command container cannot be null");
        Preconditions.checkNotNull(handler, "Handler object cannot be null");
        try {
            Map<String, DrinkCommand> extractCommands = extractor.extractCommands(handler);
            extractCommands.forEach((s, d) -> root.getCommands().put(s, d));
            return root;
        } catch (MissingProviderException | CommandStructureException ex) {
            throw new CommandRegistrationException("Could not init sub-command in root '" + root + "' with handler '" + handler.getClass().getSimpleName() + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public <T> void registerModifier(Class<? extends Annotation> annotation, Class<T> type, DrinkModifier<T> modifier) {
        modifierService.registerModifier(annotation, type, modifier);
    }

    void executeCommand(CommandSender sender, DrinkCommand command, String label, String[] args) {
        Preconditions.checkNotNull(sender, "Sender cannot be null");
        Preconditions.checkNotNull(command, "Command cannot be null");
        Preconditions.checkNotNull(label, "Label cannot be null");
        Preconditions.checkNotNull(args, "Args cannot be null");
        if (authorizer.isAuthorized(sender, command)) {
            if (command.isRequiresAsync()) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> finishExecution(sender, command, label, args));
            } else {
                finishExecution(sender, command, label, args);
            }
        }
    }

    private void finishExecution(CommandSender sender, DrinkCommand command, String label, String[] args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        try {
            argList = argumentParser.combineMultiWordArguments(argList);
            Map<Character, CommandFlag> flags = flagExtractor.extractFlags(argList);
            final CommandArgs commandArgs = new CommandArgs(this, sender, label, argList, flags);
            CommandExecution execution = new CommandExecution(this, sender, argList, commandArgs, command);
            Object[] parsedArguments = argumentParser.parseArguments(args, execution, command, commandArgs);
            if (!execution.isCanExecute()) {
                return;
            }
            try {
                command.getMethod().invoke(command.getHandler(), parsedArguments);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                sender.sendMessage(ChatColor.RED + "Could not perform command.  Notify an administrator");
                throw new DrinkException("Failed to execute command '" + command.getName() + "' with arguments '" + StringUtils.join(Collections.singletonList(args), ' ') + " for sender " + sender.getName(), ex);
            }
        }
        catch (CommandExitMessage ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        } catch (CommandArgumentException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
            helpService.sendUsageMessage(sender, getContainerFor(command), command);
        }
    }

    public DrinkCommandContainer getContainerFor(DrinkCommand command) {
        Preconditions.checkNotNull(command, "DrinkCommand cannot be null");
        for (DrinkCommandContainer container : commands.values()) {
            if (container.getCommands().containsValue(command)) {
                return container;
            }
        }
        return null;
    }

    public <T> BindingContainer<T> getBindingsFor(Class<T> type) {
        Preconditions.checkNotNull(type, "Type cannot be null");
        if (bindings.containsKey(type)) {
            return (BindingContainer<T>) bindings.get(type);
        }
        return null;
    }

    @Override
    public DrinkCommandContainer get(String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");
        return commands.get(getCommandKey(name));
    }

    public String getCommandKey(String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");
        if (name.length() == 0) {
            return DEFAULT_KEY;
        }
        return name.toLowerCase();
    }

    @Override
    public <T> DrinkBinder<T> bind(Class<T> type) {
        Preconditions.checkNotNull(type, "Type cannot be null for bind");
        return new DrinkBinder<>(this, type);
    }

    public <T> void bindProvider(Class<T> type, Set<Class<? extends Annotation>> annotations, DrinkProvider<T> provider) {
        Preconditions.checkNotNull(type, "Type cannot be null");
        Preconditions.checkNotNull(annotations, "Annotations cannot be null");
        Preconditions.checkNotNull(provider, "Provider cannot be null");
        BindingContainer<T> container = getBindingsFor(type);
        if (container == null) {
            container = new BindingContainer<>(type);
            bindings.put(type, container);
        }
        DrinkBinding<T> binding = new DrinkBinding<>(type, annotations, provider);
        container.getBindings().add(binding);
    }

}
