package io.github.cspigot.plugin;

import io.github.cspigot.cmds.impl.bukkit.*;
import io.github.cspigot.cmds.impl.settings.*;
import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.cspigot.api.command.CommandHandler;
import io.github.cspigot.api.command.provider.EnumProvider;

import io.github.cspigot.cSpigot;
import io.github.cspigot.cmds.impl.entity.TrackingRangeCommand;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.cmds.cSpigotCommand;
import io.github.cspigot.cmds.binds.KnockbackProfileProvider;
import io.github.cspigot.cmds.binds.KnockbackValueProvider;
import io.github.cspigot.cmds.binds.PluginProvider;
import io.github.cspigot.cmds.impl.emoji.CryCommand;
import io.github.cspigot.cmds.impl.emoji.SMHCommand;
import io.github.cspigot.cmds.impl.emoji.ShrugCommand;
import io.github.cspigot.cmds.impl.entity.ClearEntitiesCommand;
import io.github.cspigot.cmds.impl.entity.HideCommand;
import io.github.cspigot.cmds.impl.player.DayCommand;
import io.github.cspigot.cmds.impl.player.NightCommand;
import io.github.cspigot.cmds.impl.player.PingCommand;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;
import io.github.cspigot.feature.cps.CPSTracker;
import io.github.cspigot.plugin.listener.cSpigotListener;
import io.github.cspigot.plugin.listener.ResourcePackListener;

/**
 * This Project is property of Refine Development © 2021 2023
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 7/10/2022
 * @version cSpigot
 */

@Getter
public class ServerSidePlugin {

    /**
     * This is the plugin instance for cSpigot
     */
    @Getter private static JavaPlugin instance;

    private CommandHandler commandHandler;

    public void enable(cSpigot spigot) {
        instance = new JavaPlugin("spigot") {};
        Bukkit.getPluginManager().registerEvents(new cSpigotListener(), instance);

        if (SettingsConfig.TRACK_CPS) {
            CPSTracker tracker = new CPSTracker();
            Bukkit.getPluginManager().registerEvents(tracker, instance);
            spigot.registerPacketHandler(tracker);
        }

        if (SettingsConfig.RESOURCE_PACK_SYSTEM) {
            Bukkit.getPluginManager().registerEvents(new ResourcePackListener(), instance);
        }

        this.registerTasks(spigot);
        this.registerExpansions();
    }

    /**
     * Register our custom APIs for usage
     */
    public void registerExpansions() {
        this.commandHandler = new CommandHandler(instance);
        this.commandHandler.bind(Plugin.class).toProvider(new PluginProvider());
        this.commandHandler.bind(KnockbackProfile.class).toProvider(new KnockbackProfileProvider());
        this.commandHandler.bind(KnockbackType.class).toProvider(new EnumProvider<>(KnockbackType.class));
        this.commandHandler.bind(KnockbackValue.class).toProvider(new KnockbackValueProvider());
        this.registerCommands();
    }

    public void registerCommands() {
        this.commandHandler.register(new ChunkUnloadCommand(), "chunkunload", "unloadchunks", "unloadchunk");
        if (SettingsConfig.PLUGIN_COMMAND_ENABLED) {
            this.commandHandler.register(new PluginsCommand(), "plugins", "pl");
        }
        if (SettingsConfig.RELOAD_COMMAND_ENABLED) {
            this.commandHandler.register(new ReloadCommand(), "reload", "rl");
        }
        if (SettingsConfig.VERSION_COMMAND_ENABLED) {
            this.commandHandler.register(new VersionCommand(), "ver", "version", "about");
        }
        if (SettingsConfig.DAY_COMMAND_ENABLED) {
            this.commandHandler.register(new DayCommand(), "day");
        }
        if (SettingsConfig.NIGHT_COMMAND_ENABLED) {
            this.commandHandler.register(new NightCommand(), "night");
        }
        if (SettingsConfig.PING_COMMAND_ENABLED) {
            this.commandHandler.register(new PingCommand(), "ping");
        }
        this.commandHandler.register(new SetSlotsCommand(), "setSlots", "setMaxPlayers");
        this.commandHandler.register(new TicksPerSecondCommand(), "tps", "tickspersecond");
        this.commandHandler.register(new CryCommand(), "cry");
        this.commandHandler.register(new SMHCommand(), "smh");
        this.commandHandler.register(new ShrugCommand(), "shrug");
        this.commandHandler.register(new ClearEntitiesCommand(), "killall", "clearentities");
        this.commandHandler.register(new TrackingRangeCommand(),"trackingrange", "tracker", "ptracker");
        this.commandHandler.register(new HideCommand(), "hide",  "show", "togglePlayer");
        this.commandHandler.register(new EnderpearlSettingsCommand(), "enderpearl", "epearl", "pearl", "pearlsettings");
        this.commandHandler.register(new PotionSettingsCommand(), "pot", "potion", "potionsettings");
        this.commandHandler.register(new DebugCommand(), "cdebug", "cSpigotdebug");
        this.commandHandler.register(new KnockbackCommand(), "knockback", "kb");
        this.commandHandler.register(new PluginCommand(), "plugin");
        this.commandHandler.register(new cSpigotCommand(), "cSpigot");

        this.commandHandler.registerCommands();
        this.commandHandler.registerPermissions();
    }

    // Secondary FunnyProt check
    //TODO: second license check
    public void registerTasks(cSpigot spigot) {

    }
}
