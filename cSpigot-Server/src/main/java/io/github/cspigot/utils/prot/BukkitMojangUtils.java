package io.github.cspigot.utils.prot;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import io.github.cspigot.cSpigot;
import joptsimple.OptionSet;

import lombok.experimental.UtilityClass;

import net.minecraft.server.*;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.async.profiler.NoOpAsyncProfiler;
import io.github.cspigot.feature.async.profiler.TimeoutAsyncProfiler;
import io.github.cspigot.feature.crash.ExploitPacketListener;

import java.io.*;

@UtilityClass
public class BukkitMojangUtils {

    public Object startupServer(OptionSet finalOptions, Thread thread, YggdrasilAuthenticationService yggdrasilAuthenticationService, MinecraftSessionService minecraftSessionService) throws Exception {
        GameProfileRepository gameprofilerepository = yggdrasilAuthenticationService.createProfileRepository();
        UserCache usercache = new UserCache(gameprofilerepository, new File(".", MinecraftServer.a.getName()));

        DedicatedServer dedicatedserver = new DedicatedServer(finalOptions, thread, yggdrasilAuthenticationService, minecraftSessionService, gameprofilerepository, usercache);

        if (finalOptions.has("port")) {
            int port = (Integer) finalOptions.valueOf("port");
            if (port > 0) {
                dedicatedserver.setPort(port);
            }
        }

        if (finalOptions.has("universe")) {
            dedicatedserver.universe = (File) finalOptions.valueOf("universe");
        }

        if (finalOptions.has("world")) {
            dedicatedserver.setWorld((String) finalOptions.valueOf("world"));
        }

        cSpigot spigot = cSpigot.getInstance();

        spigot.getResourcePackHandler().init();
        spigot.getKnockbackHandler().init();
        spigot.getKnockbackHandler().setVersion(6);

        AsyncUtil.ASYNC_PROFILER = SettingsConfig.ASYNC_MONITOR_TIMEOUT_ENABLED ? new TimeoutAsyncProfiler() : new NoOpAsyncProfiler();

        spigot.getPacketHandlers().add(new ExploitPacketListener());

        spigot.initFilter();
        spigot.setupTps();

        return dedicatedserver;
    }

}