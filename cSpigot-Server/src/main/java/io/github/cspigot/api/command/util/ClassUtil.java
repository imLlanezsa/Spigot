package io.github.cspigot.api.command.util;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import net.minecraft.server.MinecraftServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author mathssw , Sakiio_
 * @since 4/4/2022
 * @version CommandAPI
 */

@UtilityClass
public final class ClassUtil {

    public Collection<Class<?>> getClassesInPackage(JavaPlugin plugin, String packageName) {
        JarFile jarFile;
        Collection<Class<?>> classes = new ArrayList<>();
        CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
        URL resource = codeSource.getLocation();

        String relPath = packageName.replace('.', '/');
        String resPath = resource.getPath().replace("%20", " ");
        String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");

        try {
            jarFile = new JarFile(jarPath);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IOException reading JAR File '" + jarPath + "'", e);
        }

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            String className = null;
            if (entryName.endsWith(".class") && entryName.startsWith(relPath) && entryName.length() > relPath.length() + "/".length()) {
                className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
            }
            if (className != null) {
                Class<?> clazz;
                try {
                    clazz = plugin.getClass().getClassLoader().loadClass(className);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    continue;
                }
                classes.add(clazz);
            }
        }

        try {
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ImmutableSet.copyOf(classes);
    }

    public static boolean isNthTick(int tick){
        return MinecraftServer.currentTick % tick == 0;
    }
}