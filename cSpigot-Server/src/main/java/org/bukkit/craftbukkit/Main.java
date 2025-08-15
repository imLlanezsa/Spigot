package org.bukkit.craftbukkit;

import java.io.File;
import java.io.IOException;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import io.github.cspigot.cSpigot;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.*;

import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import lombok.Getter;

import net.minecrell.terminalconsole.TerminalConsoleAppender;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.ExitBlocker;
import io.github.cspigot.utils.prot.BukkitMojangUtils;

@Log4j2
public class Main {

    private static final String[] LOGO = {
             "╔════════════════════════════════════════════════════════════════════╗\n" +
             "║                                                                    ║\n" +
             "║                    ✦✦  Welcome to cSpigot  ✦✦                     ║\n" +
             "║                                                                    ║\n" +
             "║   ➤ Version     : " + cSpigot.version + "                             ║\n" +
             "║   ➤ Date        : " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "                               ║\n" +
             "║   ➤ Maintainers : Sakiio_ & mathssw                               ║\n" +
             "║                                                                    ║\n" +
             "║   ✧ Fast. ✧ Lightweight. ✧ Powerful. ✧                            ║\n" +
             "║                                                                    ║\n" +
             "╚════════════════════════════════════════════════════════════════════╝\n"

    };

    public static boolean useJline = true;
    public static boolean useConsole = true;

    @Getter private static SettingsConfig config;

    public static void main(String[] args) {
        // cSpigot start - Add LOGO on launch
        System.out.print(String.join("\n", LOGO));

        if (System.getProperty("jdk.nio.maxCachedBufferSize") == null) {
            System.setProperty("jdk.nio.maxCachedBufferSize", "262144"); // SportPaper - cap per-thread NIO cache size
        }

        // cSpigot - Add a recommendation for JDK 17
        if (SystemUtils.getVersion() < 17) {
            System.out.println("[cSpigot] It is recommended to use JDK 17 with cSpigot for maximum performance.");
        }
        // cSpigot end

        OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList("?", "help"), "Show the help");

                acceptsAll(asList("c", "config"), "Properties file to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("server.properties"))
                        .describedAs("Properties file");

                acceptsAll(asList("P", "plugins"), "Plugin directory to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("plugins"))
                        .describedAs("Plugin directory");

                acceptsAll(asList("h", "host", "server-ip"), "Host to listen on")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("Hostname or IP");

                acceptsAll(asList("W", "world-dir", "universe", "world-container"), "World container")
                        .withRequiredArg()
                        .ofType(File.class)
                        .describedAs("Directory containing worlds");

                acceptsAll(asList("w", "world", "level-name"), "World name")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("World name");

                acceptsAll(asList("p", "port", "server-port"), "Port to listen on")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Port");

                acceptsAll(asList("o", "online-mode"), "Whether to use online authentication")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .describedAs("Authentication");

                acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Server size");

                acceptsAll(asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
                        .withRequiredArg()
                        .ofType(SimpleDateFormat.class)
                        .describedAs("Log date format");

                acceptsAll(asList("log-pattern"), "Specfies the log filename pattern")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("server.log")
                        .describedAs("Log filename");

                acceptsAll(asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(0)
                        .describedAs("Max log size");

                acceptsAll(asList("log-count"), "Specified how many log files to cycle through")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(1)
                        .describedAs("Log count");

                acceptsAll(asList("log-append"), "Whether to append to the log file")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .defaultsTo(true)
                        .describedAs("Log append");

                acceptsAll(asList("log-strip-color"), "Strips color codes from log file");

                acceptsAll(asList("b", "bukkit-settings"), "File for bukkit settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("bukkit.yml"))
                        .describedAs("Yml file");

                acceptsAll(asList("C", "commands-settings"), "File for command settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("commands.yml"))
                        .describedAs("Yml file");

                acceptsAll(asList("nojline"), "Disables jline and emulates the vanilla console");

                acceptsAll(asList("noconsole"), "Disables the console");

                acceptsAll(asList("v", "version"), "Show the CraftBukkit Version");

                acceptsAll(asList("demo"), "Demo mode");

                // Spigot Start
                acceptsAll(asList("S", "spigot-settings"), "File for spigot settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("spigot.yml"))
                        .describedAs("Yml file");
                // Spigot End

                // PaperSpigot Start
                acceptsAll(asList("paper", "paper-settings"), "File for paperspigot settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("paper.yml"))
                        .describedAs("Yml file");
                // PaperSpigot End

                // cSpigot start
                acceptsAll(asList("settings", "cSpigot-settings"), "File for cSpigot settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("settings.yml"))
                        .describedAs("Yml file");
                // cSpigot end


                // TacoSpigot start
                acceptsAll(asList("taco", "taco-settings"), "File for tacospigot settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("taco.yml"))
                        .describedAs("Yml file");
                // TacoSpigot end
            }
        };


        try {
            OptionSet options = null;

            try {
                options = parser.parse(args);
            } catch (joptsimple.OptionException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
            }

            if ((options == null) || (options.has("?"))) {
                try {
                    parser.printHelpOn(System.out);
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (options.has("v")) {
                System.out.println(CraftServer.class.getPackage().getImplementationVersion());
            } else {
                // Do you love Java using + and ! as string based identifiers? I sure do!
                String path = new File(".").getAbsolutePath();
                if (path.contains("!") || path.contains("+")) {
                    System.err.println("Cannot run server in a directory with ! or + in the pathname. Please rename the affected folders and try again.");
                    return;
                }


                // Paper start
                if (options.has("nojline")) {
                    System.setProperty(TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY, "false");
                    useJline = false;
                }
                // Paper end

                if (options.has("noconsole")) {
                    useConsole = false;
                    // Paper start
                    useJline = false;
                    System.setProperty(TerminalConsoleAppender.JLINE_OVERRIDE_PROPERTY, "false");
                    // Paper end
                }

                // Spigot Start
                int maxPermGen = 0; // In kb
                for ( String s : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments() ) {
                    if (s.startsWith("-XX:MaxPermSize")) {
                        maxPermGen = Integer.parseInt(s.replaceAll("[^\\d]", ""));
                        maxPermGen <<= 10 * ("kmg".indexOf(Character.toLowerCase(s.charAt(s.length() - 1))));
                    }
                }
                if (Float.parseFloat(System.getProperty("java.class.version")) < 52 && maxPermGen < (128 << 10)) // 128mb
                {
                    System.out.println("Warning, your max perm gen size is not set or less than 128mb. It is recommended you restart Java with the following argument: -XX:MaxPermSize=128M");
                    System.out.println("Please see http://www.spigotmc.org/wiki/changing-permgen-size/ for more details and more in-depth instructions.");
                }
                // Spigot End

                // cSpigot - Port old configs
                config = new SettingsConfig((File) options.valueOf("settings"));

                File cSpigotFile = new File("cSpigot.yml");

                if (cSpigotFile.exists()) {
                    try {
                        cSpigotFile.delete();
                    } catch (Exception e) {
                        System.out.println("[cSpigot] Error! Unable to delete old configs.");
                    }
                }
                config.clearConfig();
                config.writeConfig();

                net.techcable.tacospigot.TacoSpigotConfig.init((File) options.valueOf("taco-settings")); // TacoSpigot - load config before we load libraries to allow access while loading

                System.setProperty("library.jansi.version", "cSpigot"); // cSpigot - set meaningless jansi version to prevent git builds from crashing on Windows
                log.info("Loading libraries, please wait...");

                // cSpigot - System Debug
                SettingsConfig.SYSTEM_DEBUG = Boolean.getBoolean("CDebug");
                if (SettingsConfig.SYSTEM_DEBUG) {
                    try {
                        ExitBlocker.activate();
                        log.info("[cSpigot] Debug-mode is enabled, registering ExitBlocker...");
                    } catch (Exception e) {
                        log.info("[cSpigot] Failed to setup SecurityManager!");
                    }
                }

                DispenserRegistry.c();
                OptionSet finalOptions = options;

                MinecraftServer.spin(thread -> {
                    YggdrasilAuthenticationService yggdrasilauthenticationservice = new com.destroystokyo.paper.profile.PaperAuthenticationService(Proxy.NO_PROXY, UUID.randomUUID().toString()); // Paper
                    MinecraftSessionService minecraftsessionservice = yggdrasilauthenticationservice.createMinecraftSessionService();

                    try {
                        return (DedicatedServer) BukkitMojangUtils.startupServer(finalOptions, thread, yggdrasilauthenticationservice, minecraftsessionservice);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            }
            // cSpigot start - Fix missing -noverify crash for jdk 11+
        } catch (ClassFormatError e) {
            if (SystemUtils.getVersion() > 8) {
                System.out.println("[cSpigot] WARNING! You don't have -noverify in your startup arguments, but you are using a jdk higher than 8!");
                System.out.println("[cSpigot] cSpigot will not start due to obfuscation issues, please add -noverify in your jvm arguments!");
                System.out.println("Shutting down...");
            } else {
                e.printStackTrace();
            }
            // cSpigot end
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static List<String> asList(String... params) {
        return Arrays.asList(params);
    }
}
