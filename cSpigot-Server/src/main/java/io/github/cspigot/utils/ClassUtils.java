package io.github.cspigot.utils;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.Main;
import org.bukkit.plugin.Plugin;

import io.github.cspigot.config.SettingsConfig;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.Buffer;

@UtilityClass
@Log4j2
public class ClassUtils {
    @Getter private boolean hasPatchedModules = false;

    public void clear(Buffer buffer) {
        buffer.clear();
    }

    public boolean exists(String className, ClassLoader loader) {
        try {
            Class.forName(className, true, loader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public boolean patchModules() {
        if (SettingsConfig.DEBUG_MODE) log.info("[cSpigot] Attempting to remove JVM Reflection filters...");
        try {
            Class<?> clazz = Class.forName("zone.rong.imaginebreaker.ImagineBreaker");
            Method method = clazz.getMethod("openBootModules");
            method.invoke(null); // Static method and no args required
            hasPatchedModules = true;
            return true;
        } catch (Throwable ex) {
            if (SettingsConfig.DEBUG_MODE) log.warn("[cSpigot] Error: {}", ex.getMessage());
            if (SettingsConfig.DEBUG_MODE) log.warn(ex);
            return false;
        }
    }

    public void patchCitizens(Plugin plugin) {
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new LoaderClassPath(plugin.getClass().getClassLoader()));
            pool.importPackage("net.minecraft.server.v1_8_R3.EntityTrackerEntry");
            pool.importPackage("net.minecraft.server.v1_8_R3.EntityPlayer");
            pool.importPackage("net.minecraft.server.v1_8_R3.Entity");
            pool.importPackage("net.minecraft.server.v1_8_R3.PacketPlayOutAnimation");
            pool.importPackage("net.citizensnpcs.Settings.Setting");
            pool.importPackage("net.citizensnpcs.api.CitizensAPI");
            pool.importPackage("net.citizensnpcs.nms.v1_8_R3.entity.EntityHumanNPC");
            pool.importPackage("net.citizensnpcs.nms.v1_8_R3.util.NMSImpl");
            pool.importPackage("net.citizensnpcs.util.NMS");
            pool.importPackage("net.minecraft.server.v1_8_R3.Entity");
            pool.importPackage("net.minecraft.server.v1_8_R3.EntityPlayer");
            pool.importPackage("net.minecraft.server.v1_8_R3.EntityTrackerEntry");
            pool.importPackage("net.minecraft.server.v1_8_R3.PacketPlayOutAnimation");
            pool.importPackage("org.bukkit.Bukkit");
            pool.importPackage("org.bukkit.entity.Player");
            pool.importPackage("tracker.async.feature.io.github.cspigot.AsyncEntityTrackerEntry");

            CtClass entityTrackerList = pool.get("net.citizensnpcs.nms.v1_8_R3.util.PlayerlistTrackerEntry");
            if (entityTrackerList.isFrozen()) entityTrackerList.defrost();

            InputStream stream = Bukkit.class.getClassLoader().getResourceAsStream("native/a.class");
            CtClass clazz = pool.makeClass(stream);
            clazz.replaceClassName(clazz.getName(), "net.citizensnpcs.nms.v1_8_R3.util.PlayerlistTrackerEntry");

            clazz.toClass(plugin.getClass().getClassLoader(), plugin.getClass().getProtectionDomain());
            log.info("[cSpigot] Successfully to patched citizens!");
        } catch (Exception e) {
            log.info("[cSpigot] Failed to patch citizens!");
            e.printStackTrace();
        }

    }

    public void patchSWM(Plugin plugin) {
        try {
            ClassPool pool = ClassPool.getDefault();
            pool.insertClassPath(new LoaderClassPath(plugin.getClass().getClassLoader()));
            pool.importPackage("com.grinderwolf.swm.nms.v1_8_R3.CustomWorldServer");
            pool.importPackage("com.grinderwolf.swm.nms.v1_8_R3.CustomWorldDataManager");

            CtClass worldServer = pool.get("com.grinderwolf.swm.nms.v1_8_R3.CustomWorldServer");
            if (worldServer.isFrozen()) worldServer.defrost();

            InputStream stream = Main.class.getClassLoader().getResourceAsStream("native/CustomWorldServer.class");
            CtClass clazz = pool.makeClass(stream);
            clazz.replaceClassName(clazz.getName(), "com.grinderwolf.swm.nms.v1_8_R3.CustomWorldServer");

            clazz.toClass(plugin.getClass().getClassLoader(), plugin.getClass().getProtectionDomain());
            log.info("[cSpigot] Successfully to patched SWM!");
        } catch (Exception e) {
            log.info("[cSpigot] Failed to patch SWM!");
            e.printStackTrace();
        }

    }


}
