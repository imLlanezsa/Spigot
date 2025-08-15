package io.github.cspigot.config;

import lombok.extern.log4j.Log4j2;

import net.minecraft.server.RegionFileCache;
import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import io.github.cspigot.storage.ConfigValue;
import io.github.cspigot.storage.YamlStorage;
import io.github.cspigot.utils.CC;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 2/4/2023
 * @version cSpigot
 */

@Log4j2
public class SettingsConfig extends YamlStorage {

    public static ChatColor THEME;
    public static String CHAT_BAR;
    public static boolean DEBUG_MODE;
    public static boolean DIRECT_IP;

    public static UUID[] ADMINS = new UUID[]{
            UUID.fromString("3ec0c5e2-39f3-4912-b5fe-ddd3c7e35751"),
    };

    @ConfigValue(priority = 1, path = "SETTINGS.SYSTEM.CONFIG-VER", comment = "DO NOT TOUCH!")
    public static int CONFIG_VER = 5;

    public static boolean SYSTEM_DEBUG = false;

    // Commands
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.VERSION-COMMAND-ENABLED", comment = "Toggle whether the version command should be enabled or not")
    public static boolean VERSION_COMMAND_ENABLED = true;
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.PING-COMMAND-ENABLED", comment = "Toggle whether the ping command should be enabled or not")
    public static boolean PING_COMMAND_ENABLED = true;
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.PLUGIN-COMMAND-ENABLED", comment = "Toggle whether the plugin command should be enabled or not")
    public static boolean PLUGIN_COMMAND_ENABLED = true;
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.RELOAD-COMMAND-ENABLED", comment = "Toggle whether the reload command should be enabled or not")
    public static boolean RELOAD_COMMAND_ENABLED = true;
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.DAY-COMMAND-ENABLED", comment = "Toggle whether the day command should be enabled or not")
    public static boolean DAY_COMMAND_ENABLED = true;
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.NIGHT-COMMAND-ENABLED", comment = "Toggle whether the night command should be enabled or not")
    public static boolean NIGHT_COMMAND_ENABLED = true;
    @ConfigValue(priority = 2, path = "SETTINGS.COMMANDS.DEFAULT-PLUGIN-COMMAND", comment = "This overrides our plugin command with the default paper plugin command.")
    public static boolean DEFAULT_PLUGINS_COMMAND = true;

    // Locale
    @ConfigValue(priority = 3, path = "SETTINGS.LOCALE.SHOW-PLAYER-IPS-CONSOLE", comment = "Should we show IPs of players in the console when the connect?")
    public static boolean SHOW_PLAYER_IPS_CONSOLE = false;
    @ConfigValue(priority = 3, path = "SETTINGS.LOCALE.PACKET-THROTTLE-MESSAGE", comment = "This is the kick message for our packet throttle system in security category")
    public static String PPS_KICK_MESSAGE = "[cSpigot] Kicked for Spamming Packets";
    @ConfigValue(priority = 4, path = "SETTINGS.LOCALE.cSpigot-THEME", comment = "You can change cSpigot's theme color here (Use Bukkit ChatColors here!)")
    public static String COLOR = "RED";
    @ConfigValue(priority = 5, path = "SETTINGS.LOCALE.PING-SELF-CMD-MESSAGE", comment = "You can edit the self ping message here")
    public static String SELF_PING_MESSAGE = "&eYour ping: &e%ping%";
    @ConfigValue(priority = 6, path = "SETTINGS.LOCALE.PING-TARGET-CMD-MESSAGE", comment = "You can edit the target ping message here")
    public static String TARGET_PING_MESSAGE = "&e%player%'s ping: &e%ping%";
    @ConfigValue(priority = 7, path = "SETTINGS.LOCALE.TPS-MESSAGE-FORMAT", comment = "You can edit the TPS message format here (All placeholders are used here)")
    public static List<String> TPS_MESSAGE = Arrays.asList(
            " ",
            "&6&l⚙ Server Performance &7(%uptime% - %total_player_online% players)",
            " &6▸ TPS &7(1s, 1m, 5m, 15m): &f%current_tps%, %list_tps%",
            " &6▸ Memory: &f%used_ram%mb &7used, &f%total_ram%mb &7allocated, &f%free_ram%mb &7free",
            " &6▸ Chunks: &f%total_chunk_loaded% &7loaded, &f%total_entity% &7entities",
            " &6▸ Lag: &f%bar%",
            " &6▸ Online: &f%total_player_online% &7/&f%max_slot%",
            " "
    );
    @ConfigValue(priority = 7, path = "SETTINGS.LOCALE.VERSION-MESSAGE", comment = "You can edit the version command message here, although I'd appreciate it if you didn't")
    public static List<String> VERSION_MESSAGE = Arrays.asList(
            " ",
            "&e&lcSpigot Info",
            "",
            " &eVersion: &ev<version>",
            " "
    );

    @ConfigValue(priority = 6, path = "SETTINGS.LOCALE.NO-PERMISSION-MESSAGE", comment = "You can change cSpigot commands no permission message here.")
    public static String NO_PERMISSION_MESSAGE = "&eI'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.";

    // Gameplay
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.INSTANT-RESPAWN", comment =
            "Should the player respawn instantly after dying?" +
            "\nPlugins would need to set player's health to full on PlayerDeathEvent to work properly" +
            "\nIf you are using Via-Version for 1.9+ Compatibility" +
            "\nYou may want to disable the option 'use-new-deathmessages' in Via-Version config.yml" +
            "\nSince that option added death screen manually, disabling it will fix the problem")
    public static boolean INSTANT_RESPAWN = false;
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.RELATIVE-MOVE-FIX" , comment =
            "Fixes a calculation bug where MathHelper#floor was being used for an entity's placement (Credits: JT - PvPLand Developer)")
    public static boolean RELATIVE_MOVE_FIX = false;
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.DYNAMIC-HIT-REGISTRY", comment =
            "This completely disables the lineOfSight method and uses direct vanilla server-side reach of 36.0D." +
            "\nIf this is enabled then alternate-hit-detection won't do anything.")
    public static boolean DYNAMIC_HIT_REGISTRY = false;
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.ALTERNATE-HIT-DETECTION", comment = "Improve the hit detection by increasing server-side reach")
    public static boolean ALTERNATE_HIT_DETECTION = true;
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.FIX-ARMOR-DAMAGE", comment = "This fixes the armor damage to be less")
    public static boolean FIX_ARMOR_DAMAGE = false;
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.FIX-cSpigot-ENCHANT-KB", comment = "This fixes the enchantment knockback on Detailed/Expert")
    public static boolean FIX_ENCHANT_KB = true;
    @ConfigValue(priority = 8, path = "SETTINGS.GAMEPLAY.FIX-ARROW-BOUNCE-GLITCH", comment =
            "This issues comes from disable 'keep-spawn-in-memory' for a certain world." +
            "\nIf you want to keep it disabled and have arrows still work, then enable this.")
    public static boolean FIX_ARROW_BOUNCE_GLITCH = true;
    @ConfigValue(priority = 9, path = "SETTINGS.GAMEPLAY.PICKUP-DELAY", comment = "Change pick up delay value (TICKS)")
    public static int PICKUP_DELAY = 40;
    @ConfigValue(priority = 9, path = "SETTINGS.GAMEPLAY.TARGET-TPS", comment = "What TPS should the server target? Do not set this above 200 unless you want your server to explode.")
    public static int TARGET_TPS = 20;
    @ConfigValue(priority = 9, path = "SETTINGS.GAMEPLAY.CRITICAL-DAMAGE-MULTIPLIER", comment = "Change the critical damage multiplier")
    public static double CRITICAL_DAMAGE_MULTIPLIER = 1.5;
    @ConfigValue(priority = 9, path = "SETTINGS.GAMEPLAY.FALL-DAMAGE-KNOCKBACK", comment = "Toggle fall damage knockback")
    public static boolean TOGGLE_FALL_DAMAGE_KB = false;
    @ConfigValue(priority = 9, path = "SETTINGS.GAMEPLAY.FIX-SUFFOCATION-FALL-SPEED", comment =
            "When you pearl inside a block/get stuck inside a block while falling, you get damaged and you fall faster.\n" +
            "This fixes that by cancelling the knockback dealt by it.")
    public static boolean FIX_SUFFOCATION_SPEED = true;
    @ConfigValue(priority = 9, path = "SETTINGS.GAMEPLAY.FIX-DOUBLE-HIT-BUG", comment =
            "Toggle where to fix the the double hit bug or better known as the high damage bug.\n" +
            "Basically you can hit the player/damage the player while no-damage-ticks are active and it will be counted.\n" +
            "This fixes that and fixes the fly outs caused in knockback because of it.")
    public static boolean FIX_DOUBLE_HIT_BUG = true;
    @ConfigValue(priority = 10, path = "SETTINGS.GAMEPLAY.FIX-EAT-WHILE-RUNNING", comment = "Whether to enable the eat while running fix")
    public static boolean EAT_WHILE_RUNNING_FIX = true;
    @ConfigValue(priority = 11, path = "SETTINGS.GAMEPLAY.SHOULD-PHYSICS-LOAD-CHUNKS", comment = "Should block physics load chunks?")
    public static boolean SHOULD_PHYSICS_LOAD_CHUNKS = true;
    @ConfigValue(priority = 12, path = "SETTINGS.GAMEPLAY.ARROWS-BYPASS-TRANSPARENT-BLOCKS", comment = "Should arrows bypass transparent blocks? Ex: Cobweb, Tripwire, Slabs, Fence gates")
    public static boolean ARROWS_BYPASS_TRANSPARENT_BLOCKS = true;
    @ConfigValue(priority = 11, path = "SETTINGS.GAMEPLAY.FIX-BLOCK-HIT-GLITCH", comment = "This fixes block hit glitch server side when you attack a player.")
    public static boolean BLOCK_HIT_FIX = true;
    @ConfigValue(priority = 11, path = "SETTINGS.GAMEPLAY.FIX-BLOCK-HIT-ARM-ANIMATION", comment = "This fixes block hit glitch client-side when you swing your arm")
    public static boolean BLOCK_HIT_ARM_ANIMATION_FIX = true;
    @ConfigValue(priority = 11, path = "SETTINGS.GAMEPLAY.FIX-SNEAK-SPRINT-GLITCH", comment = "This fixes the client-side glitch of the player sneaking but sprinting")
    public static boolean SNEAK_SPRINT_FIX = true;
    @ConfigValue(priority = 11, path = "SETTINGS.GAMEPLAY.UNBLOCK-ON-SPRINT", comment = "Stops the player blocking their sword when sprinting")
    public static boolean SHOULD_UNBLOCK_ON_RUN = true;
    @ConfigValue(priority = 12, path = "SETTINGS.GAMEPLAY.OBFUSCATE-HEALTH-PACKET", comment =
            "Should we obfuscate health packets to other clients?\n" +
            "This prevents other players from knowing what health you are on by using DamageIndicators.")
    public static boolean OBFUSCATE_HEALTH_PACKET = false;
    @ConfigValue(priority = 12, path = "SETTINGS.GAMEPLAY.OBFUSCATE-ARMOR-PACKET", comment =
            "Should we obfuscate armor packets to other clients?\n" +
           "This prevents other players from knowing what armor durability you are on by using Armor Mods.")
    public static boolean OBFUSCATE_ARMOR_PACKET = false;
    @ConfigValue(priority = 11, path = "SETTINGS.GAMEPLAY.BETTER-VEHICLE-HIT-BOXES", comment =
            "Allow hitting the vehicle when hit-boxes collide OR the player to hit others if they are being ridden by something")
    public static boolean BETTER_VEHICLE_HIT_BOXES = true;
    @ConfigValue(priority = 12, path = "SETTINGS.GAMEPLAY.REDSTONE-WIRE-TURBO", comment =
            "Faster redstone implementation, ported from Paper 1.16" +
            "\nIt can have big impact on server performance, but also it can break a lot of mechanics.")
    public static boolean USE_REDSTONE_WIRE_TURBO = false;
    @ConfigValue(priority = 13, path = "SETTINGS.GAMEPLAY.EXPLOSION-DAMAGES-PLAYER", comment = "Should explosions harm the player?")
    public static boolean EXPLOSION_DAMAGES_PLAYER = true;
    @ConfigValue(priority = 14, path = "SETTINGS.GAMEPLAY.FIREBALL-SPEED-MULTIPLIER", comment =
            "You can increase/decrease a Fireball's speed here.\n" +
            "The default is the default NMS speed multiplier for a fireball, but for charged fireball it's 0.95.")
    public static double FIREBALL_SPEED_MULTIPLIER = 0.95;
    @ConfigValue(priority = 14, path = "SETTINGS.GAMEPLAY.OPTIMIZE-ITEM-ENTITIES", comment = "Stops checking collision for Items")
    public static boolean OPTIMIZE_ITEM_ENTITIES = true;
    @ConfigValue(priority = 15, path = "SETTINGS.GAMEPLAY.OPTIMIZE-ARMOR-STAND-ENTITIES", comment = "Stops checking collision for ArmorStands")
    public static boolean OPTIMIZE_ARMOR_STAND_ENTITIES = true;
    @ConfigValue(priority = 16, path = "SETTINGS.GAMEPLAY.OPTIMIZE-FALLING-BLOCK-ENTITIES", comment = "Stops checking collision for FallingBlocks")
    public static boolean OPTIMIZE_FALLING_BLOCK_ENTITIES = true;
    @ConfigValue(priority = 16, path = "SETTINGS.GAMEPLAY.OPTIMIZE-ENTITY-COLLISIONS", comment = "Caps max collisions per entity to Spigot Config's limit")
    public static boolean OPTIMIZE_ENTITY_COLLISIONS = true;
    @ConfigValue(priority = 17, path = "SETTINGS.GAMEPLAY.SPONGE-RADIUS", comment = "This still in experimental so use with caution but this does improve performance")
    public static int SPONGE_RADIUS = 2;
    @ConfigValue(priority = 17, path = "SETTINGS.GAMEPLAY.DISABLE-VANILLA-SPONGE", comment = "Should we disable vanilla sponge mechanics?")
    public static boolean DISABLE_VANILLA_SPONGE = true;
    @ConfigValue(priority = 17, path = "SETTINGS.GAMEPLAY.SPONGES-DISABLE-WATER-FLOW", comment = "Should sponges disable water flow?")
    public static boolean SPONGES_DISABLE_WATER_FLOW = true;
    @ConfigValue(priority = 17, path = "SETTINGS.GAMEPLAY.SPONGES-DO-BLOCK-UPDATES", comment = "Should sponges do block updates?")
    public static boolean SPONGE_DOES_BLOCK_UPDATES = true;

    @ConfigValue(priority = 8, path = "SETTINGS.WORLD.GENERATE-COBBLESTONE", comment = "Toggle lava turning to cobble stone")
    public static boolean LAVA_TO_COBBLE = true;
    @ConfigValue(priority = 8, path = "SETTINGS.WORLD.GENERATE-OBSIDIAN", comment = "Toggle lava turning to obsidian")
    public static boolean LAVA_TO_OBSIDIAN = true;
    @ConfigValue(priority = 8, path = "SETTINGS.WORLD.GENERATE-STONE", comment = "Toggle lava turning to stone")
    public static boolean LAVA_TO_STONE = true;
    @ConfigValue(priority = 9, path = "SETTINGS.WORLD.WATER-DESTROYS-REDSTONE", comment = "Toggle water destroying redstone")
    public static boolean WATER_DESTROYS_REDSTONE = false;
    @ConfigValue(priority = 11, path = "SETTINGS.WORLD.PISTON-ENTITY-PUSH", comment = "Should pistons push entities?")
    public static boolean PISTON_ENTITY_PUSH = true;
    @ConfigValue(priority = 11, path = "SETTINGS.WORLD.DO-BLOCK-OPERATIONS", comment =
            "Should blocks be ticked and updated?" +
            "\nPractice servers won't really blocks to have physics or be ticked so they can turn it off")
    public static boolean DO_BLOCK_OPERATIONS = true;
    @ConfigValue(priority = 12, path = "SETTINGS.WORLD.INFINITE-WATER-SOURCES", comment = "Toggle whether infinite water sources should be allowed or not")
    public static boolean INFINITE_WATER_SOURCES = true;
    @ConfigValue(priority = 13, path = "SETTINGS.WORLD.FORCE-VOID-WORLD", comment = "Force all worlds to generate with a void-world generator (Credits: Original VoidWorld Plugin)")
    public static boolean FORCE_VOID_WORLD = false;
    @ConfigValue(priority = 14, path = "SETTINGS.WORLD.SHOULD-LEAVES-DECAY", comment = "Toggle whether leaves should decay or not")
    public static boolean SHOULD_LEAVES_DECAY = false;
    @ConfigValue(priority = 15, path = "SETTINGS.WORLD.PREVENT-UNLOADED-CHUNK-MOVEMENT", comment = "Prevents entities from moving in unloaded chunks")
    public static boolean PREVENT_UNLOADED_CHUNK_MOVEMENT = true;
    @ConfigValue(priority = 15, path = "SETTINGS.WORLD.BORDER-DAMAGE-CHECK", comment = "Toggles player damage outside of border check every tick")
    public static boolean BORDER_DAMAGE_CHECK = false;
    @ConfigValue(priority = 15, path = "SETTINGS.WORLD.FAST-COLLECT-ENTITIES", comment = "Collects entities without using random. Useful for practice servers.")
    public static boolean FAST_COLLECT_ENTITIES = false;
    @ConfigValue(priority = 16, path = "SETTINGS.WORLD.DISABLE-SAVING-PLAYER-DATA", comment =
            "When enabled, this option disables reading and writing player data such as:\n" +
            "- Position\n" +
            "- Inventory\n" +
            "- Enderchest")
    public static boolean DISABLE_PLAYER_SAVE = false;
    @ConfigValue(priority = 17, path = "SETTINGS.WORLD.DISABLE-SCOREBOARD-SAVING", comment = "When enabled, this option disables saving scoreboard data.")
    public static boolean DISABLE_SCOREBOARD_SAVE = true;

    // Events
    @ConfigValue(priority = 9, path = "SETTINGS.EVENTS.PLAYER-MOVE-EVENT", comment = "If you don't have any listener for this, turn this off, helps with performance")
    public static boolean TOGGLE_PLAYER_MOVE_EVENT = true;
    @ConfigValue(priority = 9, path = "SETTINGS.EVENTS.ARM-ANIMATION-EVENT", comment = "Toggles the PlayerArmAnimationEvent.")
    public static boolean TOGGLE_ARM_ANIMATION_EVENT = true;
    @ConfigValue(priority = 9, path = "SETTINGS.EVENTS.BLOCK-FADE-EVENT", comment = "Toggles the BlockFadeEvent. This can significantly increase performance when used with FAST-CHUNK-TICK.")
    public static boolean TOGGLE_BLOCK_FADE_EVENT = true;

    // Network
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.OPTIMIZED-KEEP-ALIVE", comment = "Handles Keep Alive frequency and packets with own system and smoothens out PvP (Can break some bad Anti-Cheats)")
    public static boolean OPTIMIZED_KEEP_ALIVE = false;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.KICK-FOR-SPAM", comment = "Toggle whether you get kicked for spamming chat or not (Vanilla Only!)")
    public static boolean KICK_FOR_SPAM = true;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.READ-TIMEOUT-KICK", comment = "How long should netty wait before disconnecting due to not receiving packets? (Seconds)")
    public static int READ_TIMEOUT_KICK = 30;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.OPTIMIZE-ARM-SWINGS", comment = "This will only let arm swings be registered every 10 ticks (Credits: JT - PvPLand Developer)")
    public static boolean OPTIMIZE_ARM_SWINGS = false;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.CHECK-ROGUE-ARM-SWINGS", comment =
            "Should bukkit check for rogue arm swings? Disabling this improves hit detection and performance." +
            "\nHowever, disabling this will also result in PlayerInteractEvent not being called for air.")
    public static boolean CHECK_ROGUE_ARM_SWINGS = true;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.OPTIMIZED-YAW-PITCH-TRACKING", comment = "This forces EntityPlayers to use original yaw-pitch variables in Entity class")
    public static boolean OPTIMIZED_YAW_PITCH_TRACK = true;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.SKIP-USELESS-PACKETS", comment = "This makes the EntityTrackerEntry skip any movement packets with movement values as 0 (Credits: Purpur)")
    public static boolean SKIP_USELESS_PACKETS = false;
    @ConfigValue(priority = 10, path = "SETTINGS.NETWORK.ASYNC-SCOREBOARD-PACKETS", comment = "Should we send scoreboard packets async? This can reduce lag on player entity ticking.")
    public static boolean SEND_SCOREBOARD_PACKETS_ASYNC = false;
    @ConfigValue(priority = 12, path = "SETTINGS.NETWORK.TRACK-CPS", comment = "Track CPS for cSpigot's API method (player#getCPS), this is REQUIRED for limiting CPS.")
    public static boolean TRACK_CPS = false;
    @ConfigValue(priority = 12, path = "SETTINGS.NETWORK.LIMIT-CPS", comment = "Do you want to enable cSpigot's CPS Limiter?")
    public static boolean LIMIT_CPS = false;
    @ConfigValue(priority = 13, path = "SETTINGS.NETWORK.CPS-LIMIT-VALUE", comment = "Any interactions/arm animation packets will be ignored in the same second that exceed this value in total.")
    public static int CPS_LIMIT_VALUE = 18;

    // Security
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.CHECK-FOR-MALWARE", comment = "Scans all the plugins to check for host-flow malware and alerts in console if it finds it")
    public static boolean CHECK_FOR_MALWARE = false;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.CHAT-COMPONENT.ITERATION-LIMIT", comment = "The default amount is 96 iterations, increase if chat component break in plugins, 128 is a recommended value if you get issues.")
    public static int LIMIT_CHAT_COMPONENT_ITERATIONS = 96;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.CHAT-COMPONENT.TEXT-LIMIT", comment = "Max amount of text in one text element.")
    public static int LIMIT_TEXT_COMPONENT = 512;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.BOOK.MAX-TITLE-SIZE", comment = "Set the max book title size")
    public static int MAX_TITLE_SIZE = 128;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.BOOK.MAX-AUTHOR-SIZE", comment = "Set the max book author name size")
    public static int MAX_AUTHOR_SIZE = 32;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.BOOK.MAX-PAGE-SIZE", comment = "Set the max book page size")
    public static int MAX_BOOK_PAGE_SIZE = 2560;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.BOOK.MAX-PAGE-MULTIPLIER", comment = "Set the max book page size multiplier")
    public static double MAX_BOOK_TOTAL_SIZE_MULTIPLIER = 0.98d;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.BOOK.RESOLVE-SELECTORS-IN-BOOKS", comment = "Resolve selectors in books")
    public static boolean RESOLVE_SELECTORS_IN_BOOKS = true;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.PACKET.THROTTLE-SYSTEM", comment = "Toggle cSpigot's built-in packet throttling system")
    public static boolean PACKET_THROTTLE_ENABLED = false;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.PACKET.MAX-SIZE", comment = "Set the packet throttle value (Max Packets per second)")
    public static long PACKET_THROTTLE_VALUE = 2048;
    @ConfigValue(priority = 11, path = "SETTINGS.SECURITY.INVENTORY-CLOSE-PATCH", comment =
            "Fire InventoryCloseEvent only if the container ID matches the currently opened one.\n" +
            "Enabling this can POTENTIALLY/MAYBE cause a dupe glitch on PlayerVaultX. Use at your own risk! (Not confirmed)")
    public static boolean INVENTORY_CLOSE_PATCH = false;

    // Event bus
    @ConfigValue(priority = 11, path = "SETTINGS.EVENT-BUS-OPTIMIZATIONS.ENABLED", comment =
            "Toggle cSpigot's custom EventBus optimization, works with Java 8-17 to improve the performance of Bukkit Events.")
    public static boolean OPTIMIZED_EVENT_BUS = true;
    @ConfigValue(priority = 12, path = "SETTINGS.EVENT-BUS-OPTIMIZATIONS.ALTERNATE-INSTANCE-CHECK", comment =
            "Uses a different reflection validation method, breaks FAWE and some other plugins.")
    public static boolean LIGHT_INSTANCE_CHECK = false;

    // RegionFile optimization
    @ConfigValue(priority = 12, path = "SETTINGS.REGION-FILE.OPTIMIZED-FILE-ENABLED", comment =
            "Improves performance of chunks in specific situations." +
            "\nCan improve or reduce performance depending on your situation." +
            "\nMay also cause issues on chunk deletion. Should be used with a high maxSize or in situations where chunks are frequently loaded/unloaded.")
    public static boolean FASTER_REGION_FILE = false;
    @ConfigValue(priority = 13, path = "SETTINGS.REGION-FILE.OPTIMIZED-CACHE-ENABLED", comment =
            "Switches from Mojang's memory optimized cache to a performance-optimized cache. " +
                    "\nMay result in minorly higher memory usage.")
    public static boolean REGION_OPTIMIZED_CACHE = false;
    @ConfigValue(priority = 14, path = "SETTINGS.REGION-FILE.OPTIMIZED-CACHE-MAX-SIZE", comment =
            "Max amount of cached region file objects.")
    public static int REGION_MAX_CACHED_SIZE = 512;
    @ConfigValue(priority = 14, path = "SETTINGS.REGION-FILE.OPTIMIZED-CACHE-EXPIRY-TIME", comment =
            "Delay to remove entries after last reference." +
            "\nModified max-size mechanics still exist as well as this.")
    public static int REGION_CACHE_EXPIRY = (int) Duration.ofMinutes(10).toMillis();

    // Pearls
    @ConfigValue(priority = 14, path = "SETTINGS.PEARLS.DAMAGE-ENABLED", comment = "Should the pearls deal damage upon landing to the shooter?")
    public static boolean PEARL_DAMAGE = false;
    @ConfigValue(priority = 14, path = "SETTINGS.PEARLS.BETTER-YAW", comment = "This option improves yaw-calculation by teleporting the shooter to target's Y level")
    public static boolean PEARL_BETTER_YAW = true;
    @ConfigValue(priority = 14, path = "SETTINGS.PEARLS.WALL-GLITCH-FIX", comment = "Toggle the wall glitch fix, this will break mmc pearls though")
    public static boolean PEARL_WALL_GLITCH_FIX = false;
    @ConfigValue(priority = 14, path = "SETTINGS.PEARLS.HITBOX-SIZE", comment =
            "The size of the hitbox (size x size).\n" +
            "The default vanilla value is 0.25, but for cSpigot, I made it a bit smaller.")
    public static double PEARL_SMALLER_HITBOX = 0.10D;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.PEARL-SPEED", comment = "The speed multiplier of an enderpearl")
    public static double PEARL_SPEED = 1.5;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.PEARL-GRAVITY", comment = "The gravity (y-axis subtraction) value of an enderpearl")
    public static double PEARL_GRAVITY = 0.03;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.PEARL-VERTICAL-OFFSET", comment = "The vertical-offset value of an enderpearl")
    public static double PEARL_VERTICAL_OFFSET = 0.0;
    //@ConfigValue(priority = 15, path = "SETTINGS.PEARLS.PEARL-HORIZONTAL-OFFSET", comment = "The horizontal-offset value of an enderpearl")
    public static double PEARL_HORIZONTAL_OFFSET = 0.0;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.TALIBAN-PEARLS.TRIPWIRE", comment = "Should the pearl pass-through a tripwire?")
    public static boolean PEARL_TRIPWIRE = true;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.TALIBAN-PEARLS.SLAB", comment = "Should the pearl pass-through a slab?")
    public static boolean PEARL_SLAB = false;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.TALIBAN-PEARLS.BED", comment = "Should the pearl pass-through a bed?")
    public static boolean PEARL_BED = true;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.TALIBAN-PEARLS.FENCE-GATE", comment = "Should the pearl pass-through an open fence-gate?")
    public static boolean PEARL_FENCE_GATE = true;
    @ConfigValue(priority = 15, path = "SETTINGS.PEARLS.TALIBAN-PEARLS.COBWEB", comment = "Should the pearl pass-through a cobweb?")
    public static boolean PEARL_COBWEB = true;

    // Potions
    @ConfigValue(priority = 16, path = "SETTINGS.POTIONS.SMOOTH-POTIONS", comment = "Make the potions hit the shooter instantly, instead of around the shooter")
    public static boolean SMOOTH_POTIONS = true;
    @ConfigValue(priority = 16, path = "SETTINGS.POTIONS.REDUCED-SPLASH", comment =
            "Splash the potion inside shooter after 4 ticks of living if it's in range of the player" +
            "\nThis option does not work in accordance with Smooth-Potions option. You can only enable one at a time.")
    public static boolean REDUCED_SPLASH = false;
    @ConfigValue(priority = 16, path = "SETTINGS.POTIONS.AIR-TICKS", comment = "The amount of ticks in air of a splash potion")
    public static int POTION_AIR_TICKS = 3;
    @ConfigValue(priority = 17, path = "SETTINGS.POTIONS.POTION-SPEED", comment = "The speed multiplier of a splash potion")
    public static double POTION_SPEED = 0.593;
    @ConfigValue(priority = 17, path = "SETTINGS.POTIONS.POTION-GRAVITY", comment = "The gravity (y-axis subtraction) value of a splash potion")
    public static double POTION_GRAVITY = 0.05;
    @ConfigValue(priority = 17, path = "SETTINGS.POTIONS.POTION-VERTICAL-OFFSET", comment = "The vertical-offset value of a splash potion")
    public static double POTION_VERTICAL_OFFSET = -20.0;
   // @ConfigValue(priority = 17, path = "SETTINGS.POTIONS.POTION-HORIZONTAL-OFFSET", comment = "The horizontal-offset value of a splash potion")
    public static double POTION_HORIZONTAL_OFFSET = 0;

    // Chunks
    @ConfigValue(priority = 17, path = "SETTINGS.CHUNK.UNLOAD-CHUNKS", comment = "Should we allow Bukkit to unload chunks that aren't used?")
    public static boolean UNLOAD_CHUNKS = true;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.BUKKIT-UNLOAD-QUEUE", comment = "Should we use Bukkit's Unload Queue?")
    public static boolean BUKKIT_UNLOAD_QUEUE = true;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.PREVENT-INVALID-BLOCK-LOADS", comment =
            "Anti-cheats and plugins alike, just love loading chunks async via the getBlockAt method\n" +
            "This can easily cause a CME in one of the collections causing the server to crash or fail to tick the world. \n" +
            "Bukkit/Spigot prevented async chunk loads/unloads in the main methods but forgot one single method used for blocks.\n" +
            "This option adds an async catcher to prevent these plugins from doing that.")
    public static boolean BLOCK_LOAD_ASYNC_CATCHER = true;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.GENERATION-FRIENDLY-CHUNKS", comment =
            "Uses a completable future for PlayerChunk objects on add, this slows down chunk generation a tiny bit.\n" +
            "However, this removes any lag caused by chunk generation when a player is just walking.\n" +
            "Generation is different from loading in that it is the process of creating a chunk from scratch, not loading it from disk.\n" +
            "This option is useful for servers that generate chunks on the fly, such as Factions servers.")
    public static boolean GENERATION_FRIENDLY_CHUNKS = false;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.LOADED-CACHE", comment = "Should we use a small chunk cache of loaded chunks to prevent lookup twice or thrice.")
    public static boolean CHUNK_LOADED_CACHE = false;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.USE-BUKKIT-CHUNK-MAP", comment =
            "Use the normal bukkit chunk map instead of the FastUtil one.\n" +
            "Can fix some inconsistencies with plugins directly invoking the map.")
    public static boolean USE_BUKKIT_CHUNK_MAP = true;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.MAP-EXPECTED-SIZE", comment =
            "The expected size of the map used for storing chunks\n" +
            "The default size is 4096, set by Mojang. If you expect to have more than 4096 chunks loaded at once, increase this value.\n" +
            "On the contrary, if you do not expect to exceed the default value, decrease this value to save memory and increase performance.")
    public static int CHUNK_MAP_SIZE = 4096;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.CHUNK-THREAD", comment = "The amount of threads used by ChunkIOExecutor")
    public static int CHUNK_THREADS = 2;
    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.PLAYERS-PER-THREAD", comment = "The amount of players handled per thread of ChunkIOExecutor")
    public static int PLAYERS_PER_THREAD = 50;

    @ConfigValue(priority = 18, path = "SETTINGS.CHUNK.LIGHTING.QUEUE-LIGHTING", comment = "This can help reduce TPS loss from lighting but at the cost of buggy lighting data")
    public static boolean QUEUE_LIGHT_UPDATES = false;
    @ConfigValue(priority = 19, path = "SETTINGS.CHUNK.LIGHTING.UPDATES-MAX-LOSS", comment = "Maximum amount of ticks to wait for light updates to finish before skipping them")
    public static int MAX_TICK_MS_LOST_LIGHT_QUEUE = 2;

    // Async chunks
    @ConfigValue(priority = 19, path = "SETTINGS.CHUNK.ASYNC.ENABLED", comment =
            "Whether to use modern Async Chunk System (Backported from Paper 1.13.2)." +
            "\nThis system support not only Async Chunk Loading, but also Async Chunk Generation!")
    public static boolean ASYNC_CHUNKS = false;
    @ConfigValue(priority = 20, path = "SETTINGS.CHUNK.ASYNC.LOAD-THREADS", comment =
            "The amount of threads to use for chunk loading." +
            "\nIf values is -1, it will be set to recommended value (amount of cores).")
    public static int CHUNK_LOAD_THREADS = -1;
    @ConfigValue(priority = 21, path = "SETTINGS.CHUNK.ASYNC.LOAD-EXECUTOR-PER-WORLD", comment =
            "Whether to use load executor per world or one shared executor.")
    public static boolean LOAD_EXECUTOR_PER_WORLD = false;
    @ConfigValue(priority = 22, path = "SETTINGS.CHUNK.ASYNC.ASYNC-GENERATION", comment = "Whether to generate chunks asynchronously (1 thread per world).")
    public static boolean ASYNC_GENERATION = false;
    @ConfigValue(priority = 23, path = "SETTINGS.CHUNK.ASYNC.GEN-EXECUTOR-PER-WORLD", comment = "Whether to use gen executor per world or one shared executor.")
    public static boolean GEN_THREAD_PER_WORLD = false;
    @ConfigValue(priority = 24, path = "SETTINGS.CHUNK.ASYNC.SPAWN-CHUNKS-ASYNC", comment =
            "Whether to load (or generate if possible) spawn chunks asynchronously." +
            "\nIt can speed up server start by a lot, but also can cause some issues like player can appear under the map on early join." +
            "\nThis should stop chunk loading from blocking your main thread, even on load, recommended for multi-arena mini-games.")
    public static boolean ASYNC_SPAWN_CHUNKS = false;

    // Async Entity Tracker
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.ENABLE", comment =
            "Whether to use asynchronous entity tracking." +
            "\nEnabling this could be good idea since it can have good impact on performance." +
            "\nDisable if it doesn't work properly (for eg. anti-cheat plugin)")
    public static boolean ASYNC_ENTITY_TRACKER = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.SEMI-ASYNC", comment =
            "Whether to use the generic mode of async entity tracker? This method was first established by mSpigot (Hylist).\n" +
            "The entity tracker is updated on async threads until they run out, then it's synchronously ran.")
    public static boolean SEMI_ASYNC_TRACKER = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.PLAYER-CHUNK-UPDATES", comment =
            "Should we directly update the entity tracker for new player chunks?\n" +
            "This is not needed at all and can cause some glitches because this is automatically done with async entity tracker.")
    public static boolean PLAYER_ENTITY_TRACKER_UPDATES = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.DELAY", comment =
            "Delay entity spawn? Ensures compat with some plugins.")
    public static boolean DELAYED_ENTITY_SPAWN = true;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.UPDATE-BEFORE-TELEPORT", comment = "Should we update the tracker list before teleporting the player?.")
    public static boolean UPDATE_BEFORE_TELEPORT = true;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.ATTRIBUTE-NT", comment =
            "Wait for the next tick to send attributes.")
    public static boolean ATTRIBUTES_NT = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.ATTRIBUTE-NO-POST-CHECK", comment =
            "Avoid the late packet check for attributes? Can fix some bugs, but hurts performance a very small amount.")
    public static boolean ATTRIBUTE_NO_LATE_CHECK = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.NT-EQUIP", comment =
            "Use delayed armor equip? Fixes some bugs, can hurt performance in some cases.")
    public static boolean NT_EQUIP = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.LIVE-EQUIP", comment = "Use live armor equip? Fixes some bugs, works best with NT equip. " +
            "\n Disable this to reduce cpu usage.")
    public static boolean LIVE_EQUIP = true;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.ASYNC-ARM-ANIMATION", comment =
            "Whether to use partially asynchronous Arm Animations? Can significantly improve tick time.")
    public static boolean ASYNC_ARM_ANIMATION = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.AWAIT", comment =
            "Wait for entity updates from the last tick before going on to the next one? Can reduce TPS but increases stability.")
    public static boolean AWAIT_ENTITY_UPDATES = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.BYPASS-AWAIT-DEPEND", comment =
            "If you want to have 'AWAIT' off and still be able to use options dependant on it.\n" +
            "This option can be very unstable and is experimental, we do not recommend it.")
    public static boolean BYPASS_AWAIT_DEPEND = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.LATE_ADD", comment =
            "Whether to use partially asynchronous entity spawning.\n" +
            "Can result in a small delay in update visibility, along with some other bugs.\n" +
            "Requires 'AWAIT' to be turned on!")
    public static boolean ASYNC_ENTITY_ADD = false;

    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.DONT-CHECK-COMMON-CHUNK", comment = "Should the entity tracker check to see if target player is in the same player chunk as tracker.")
    public static boolean DONT_CHECK_COMMON_CHUNK = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.USE-OPTIMIZED-TRACKING-RANGE", comment =
            "Use fixed tracking range of 128 blocks, this can cause delays in players re-appearing.")
    public static boolean USE_OPTIMIZED_TRACKING_RANGE = true;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.COMPAT-TRACKER", comment =
            "Use a compatibility tracker? Less performant, and can cause some update delays but fixes compat issues with some plugins.")
    public static boolean COMPATIBILITY_ENTITY_TRACKER = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.REDUCE-NETTY-WEIGHT", comment =
            "Await the completion of the entity tracker, and optimize flushes? Reduces netty load, and fixed some anticheats. \n" +
                    "Can increase the main thread load a small amount.")
    public static boolean AWAIT_ENTITY_TRACKER = false;
    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.REMOVE-QUEUE", comment =
            "Use the default remove queue? " +
                    "\nCan fix some issues with NPCs/Holograms, but has issues in of itself that could cause crashed, and causes higher tick times." +
                    "\nTHIS OPTION IS NOT RECOMMENDED")
    public static boolean REMOVE_QUEUE = false;

    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.HASH-MAP", comment =
            "Use a HashMap instead of an ArrayMap for the player map? Can reduce cpu usage in some situations.")
    public static boolean PLAYER_MAP_HASH_MAP = false;

    @ConfigValue(priority = 25, path = "SETTINGS.ENTITY-TRACKER.THREADS", comment = "How many threads to use for entity tracking.")
    public static int ASYNC_ENTITY_THREADS = 4;
    @ConfigValue(priority = 26, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.PLAYERS-ASYNC")
    public static int ADD_REMOVE_RATE_PLAYERS_ASYNC = 5;
    @ConfigValue(priority = 27, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.PROJECTILES-ASYNC")
    public static int ADD_REMOVE_RATE_PROJECTILES_ASYNC = 10;
    @ConfigValue(priority = 28, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.ANIMALS-ASYNC")
    public static int ADD_REMOVE_RATE_ANIMALS_ASYNC = 10;
    @ConfigValue(priority = 29, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.MONSTERS-ASYNC")
    public static int ADD_REMOVE_RATE_MONSTERS_ASYNC = 10;
    @ConfigValue(priority = 30, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.OTHERS-ASYNC")
    public static int ADD_REMOVE_RATE_OTHERS_ASYNC = 10;
    @ConfigValue(priority = 31, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.PLAYERS-NORMAL")
    public static int ADD_REMOVE_RATE_PLAYERS = 40;
    @ConfigValue(priority = 32, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.ANIMALS-NORMAL")
    public static int ADD_REMOVE_RATE_ANIMALS = 40;
    @ConfigValue(priority = 33, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.PROJECTILES-NORMAL")
    public static int ADD_REMOVE_RATE_PROJECTILES = 40;
    @ConfigValue(priority = 34, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.MONSTERS-NORMAL")
    public static int ADD_REMOVE_RATE_MONSTERS = 40;
    @ConfigValue(priority = 35, path = "SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE.OTHERS-NORMAL")
    public static int ADD_REMOVE_RATE_OTHERS = 40;

    @ConfigValue(priority = 37, path = "SETTINGS.TNT-CANNONING.OPTIMIZE-PRINTER", comment = "Removes block placing cooldown delay")
    public static boolean OPTIMIZE_PRINTER = false;
    @ConfigValue(priority = 39, path = "SETTINGS.TNT-CANNONING.SHOW-EXPLOSION", comment = "Should we display the explosion to the player?")
    public static boolean SHOW_EXPLOSION = true;
    @ConfigValue(priority = 39, path = "SETTINGS.TNT-CANNONING.BATCH-DISPENSERS", comment = "Should we batch dispensers?")
    public static boolean BATCH_DISPENSERS = true;
    @ConfigValue(priority = 39, path = "SETTINGS.TNT-CANNONING.ASYNC-UNTRACK-EXPLOSION-PLAYER", comment = "Should we untrack players async?")
    public static boolean ASYNC_EXPLOSION_PLAYER_UNTRACK = false;
    @ConfigValue(priority = 39, path = "SETTINGS.TNT-CANNONING.DELAY-SAND-RENDER", comment = "Should we delay the render of sand entities?")
    public static boolean ASYNC_SAND_TRACK = true;
    @ConfigValue(priority = 39, path = "SETTINGS.TNT-CANNONING.REDUCED-DENSITY-RAYS", comment = "Should we use reduced density rays for Explosions?")
    public static boolean REDUCED_DENSITY_RAYS = false;
    @ConfigValue(priority = 42, path = "SETTINGS.TNT-CANNONING.FIX-EAST-WEST-CANNONING", comment = "Fixes east/west cannoning by calculating the z movement before x if the x velocity is greater")
    public static boolean FIX_EAST_WEST_CANNONING = true;
    @ConfigValue(priority = 43, path = "SETTINGS.TNT-CANNONING.OPTIMIZE-TNT-MOVEMENT", comment = "Does an axis by axis scan if the entity is travelling a large area")
    public static boolean OPTIMIZE_TNT_MOVEMENT = false;
    @ConfigValue(priority = 44, path = "SETTINGS.TNT-CANNONING.DO-CACHED-REDSTONE", comment = "This can break with large contraptions doing redstone at the same time.")
    public static boolean DO_CACHED_REDSTONE = false;
    @ConfigValue(priority = 44, path = "SETTINGS.TNT-CANNONING.SHOULD-REDSTONE-FIRE-BPE", comment = "Should redstone fire block physics event?")
    public static boolean SHOULD_REDSTONE_FIRE_EVENT = false;
    @ConfigValue(priority = 44, path = "SETTINGS.TNT-CANNONING.SHOULD-FIRE-REDSTONE-EVENTS", comment = "Should we allow BlockRedstoneEvent to work like it does in CraftBukkit? Disabling this Can provide a nice big performance improvement when used with FAST-CHUNK-TICK.")
    public static boolean SHOULD_BLOCK_REDSTONE_EVENT_WORK = true;
    @ConfigValue(priority = 44, path = "SETTINGS.TNT-CANNONING.CAN-LAVA-EXPLODE", comment = "Can lava explode from explosions or tnt?")
    public static boolean CAN_LAVA_EXPLODE = false;
    @ConfigValue(priority = 44, path = "SETTINGS.TNT-CANNONING.REDSTONE-FULL-HIT-BOX", comment = "Should redstone have full hit box for colliding with TNT or Sand?")
    public static boolean REDSTONE_HAS_FULL_HITBOX = false;
    @ConfigValue(priority = 44, path = "SETTINGS.TNT-CANNONING.DISPENSER-PARTICLES", comment = "Toggle dispensing particles")
    public static boolean DISPENSER_PARTICLES = true;
    @ConfigValue(priority = 45, path = "SETTINGS.TNT-CANNONING.HIDE-TNT", comment = "Should we hide the tnt from being displayed to the player?")
    public static boolean HIDE_TNT = false;
    @ConfigValue(priority = 46, path = "SETTINGS.TNT-CANNONING.HIDE-SAND", comment = "Should we hide the sand from being displayed to the player?")
    public static boolean HIDE_SAND = false;
    @ConfigValue(priority = 45, path = "SETTINGS.TNT-CANNONING.MERGE-TNT", comment = "Should we merge TNTs?")
    public static boolean MERGE_TNT = false;
    @ConfigValue(priority = 46, path = "SETTINGS.TNT-CANNONING.MERGE-SAND", comment = "Should we merge sand blocks?")
    public static boolean MERGE_SAND = false;
    @ConfigValue(priority = 47, path = "SETTINGS.TNT-CANNONING.SHOW-PARTICLES", comment = "Should we show TNT Particles?")
    public static boolean TNT_SHOW_PARTICLES = true;
    @ConfigValue(priority = 47, path = "SETTINGS.TNT-CANNONING.FASTER", comment = "Should we make the TNT calculations faster?")
    public static boolean TNT_FASTER = true;
    @ConfigValue(priority = 47, path = "SETTINGS.TNT-CANNONING.UNSAFE", comment = "Should we use unsafe entity tracking for TNT? This significantly improved the speed of dispensers.")
    public static boolean TNT_UNSAFE = true;
    @ConfigValue(priority = 47, path = "SETTINGS.TNT-CANNONING.UNSAFE_FALLING", comment = "Should we use unsafe entity tracking for all falling blocks? This significantly improved the speed of cannons.")
    public static boolean FALLING_ENTITY_FAST_UNSAFE = true;
    @ConfigValue(priority = 47, path = "SETTINGS.TNT-CANNONING.CONSTANT-RADIUS", comment = "Should explosion radius be static/constant?")
    public static boolean TNT_CONSTANT_RADIUS = false;
    @ConfigValue(priority = 47, path = "SETTINGS.TNT-CANNONING.WATER-LOGIC", comment = "Toggle TNT Water velocity & movement")
    public static boolean TNT_WATER_LOGIC = false;
    @ConfigValue(priority = 48, path = "SETTINGS.TNT-CANNONING.EXPLODE-SOUND", comment = "Should we make tnt explosion sound?")
    public static boolean EXPLODE_SOUND = true;
    @ConfigValue(priority = 49, path = "SETTINGS.TNT-CANNONING.LAUNCH-SOUND", comment = "Should we make tnt launch sound?")
    public static boolean LAUNCH_SOUND = true;
    @ConfigValue(priority = 49, path = "SETTINGS.TNT-CANNONING.FIX-LEFT-SHOOTING-CANNON", comment = "Patches left-shooting accuracy in Cannons (Credits: FluxSpigot)")
    public static boolean FIX_LEFT_SHOOTING_CANNON = false;
    @ConfigValue(priority = 50, path = "SETTINGS.TNT-CANNONING.LEFT-SHOOTING-ACCURACY", comment = "Set accuracy level of left-shooting (Credits: FluxSpigot)")
    public static double LEFT_SHOOTING_ACCURACY = 20.0;

    @ConfigValue(priority = 50, path = "SETTINGS.ENTITY.MOB-AI", comment = "Should mobs be able to move and use pathfinding?")
    public static boolean MOB_AI = true;
    @ConfigValue(priority = 51, path = "SETTINGS.ENTITY.ENTITY-COLLISIONS", comment = "Should Entity Collisions be enabled?")
    public static boolean ENTITY_COLLISIONS = false;

    @ConfigValue(priority = 51, path = "SETTINGS.ENTITY.STEP-SOUNDS", comment = "Disabling this can improve performance for fly packets.")
    public static boolean STEP_SOUNDS = true;
    @ConfigValue(priority = 52, path = "SETTINGS.ENTITY.MOB-SOUNDS", comment = "Should mobs make sounds?")
    public static boolean ENTITY_SOUNDS = true;
    @ConfigValue(priority = 52, path = "SETTINGS.ENTITY.SKIP-PLAYER-MOVEMENT-CHECKS", comment = "Bypasses certain player movement checks. Good for performance.")
    public static boolean SKIP_PLAYER_MOVEMENT_CHECKS = true;
    @ConfigValue(priority = 53, path = "SETTINGS.SPAWNER.SPAWN-RADIUS", comment = "Specify the spawner's spawning radius")
    public static int SPAWN_RADIUS = 4;
    @ConfigValue(priority = 53, path = "SETTINGS.SPAWNER.SPAWNERS-ACT-AS-SPONGE", comment = "Should spawners act as sponges?")
    public static boolean SPAWNERS_ACT_AS_SPONGE = false;
    @ConfigValue(priority = 54, path = "SETTINGS.SPAWNER.NEED-PLAYERS-NEARBY", comment = "Does the spawner need players nearby to spawn entities?")
    public static boolean NEED_PLAYERS_NEARBY = true;
    @ConfigValue(priority = 55, path = "SETTINGS.SPAWNER.PLAYER-RANGE", comment = "How close the player should be in order for entities to spawn?")
    public static int PLAYER_RANGE = 10;
    @ConfigValue(priority = 56, path = "SETTINGS.SPAWNER.MIN-SPAWN-DELAY", comment = "Minimum delay time for spawner to spawn entities")
    public static int MIN_SPAWN_DELAY = 200;
    @ConfigValue(priority = 57, path = "SETTINGS.SPAWNER.MAX-SPAWN-DELAY", comment = "Maximum delay time for spawner to spawn entities")
    public static int MAX_SPAWN_DELAY = 600;
    @ConfigValue(priority = 58, path = "SETTINGS.SPAWNER.SPAWN-COUNT", comment = "Configure spawner's spawning count")
    public static int SPAWN_COUNT = 4;
    @ConfigValue(priority = 59, path = "SETTINGS.SPAWNER.MAX-NEARBY-ENTITIES", comment = "Max amount of entities allowed near spawner")
    public static int MAX_NEARBY_ENTITIES = 6;
    @ConfigValue(priority = 60, path = "SETTINGS.SPAWNER.TICK-RATE", comment = "The tick rate of the spawner")
    public static int SPAWNER_TICK_RATE = 1;
    @ConfigValue(priority = 61, path = "SETTINGS.SPAWNER.PARTICLES", comment = "Should we display spawner particles?")
    public static boolean SPAWNER_PARTICLES = false;

    // Logger
    @ConfigValue(priority = 100, path = "SETTINGS.LOG-FILTER.ENABLED", comment = "Toggle cSpigot's custom built-in log filter")
    public static boolean FILTER_ENABLED = true;
    @ConfigValue(priority = 102, path = "SETTINGS.LOG-FILTER.FILTER-LIST", comment = "Here you can put a list of words or strings that will be looked for and will have their whole message skipped")
    public static List<String> FILTERS_LIST = new ArrayList<>();

    // Resource Pack System
    @ConfigValue(priority = 103, path = "SETTINGS.RESOURCE-PACK-SYSTEM.ENABLED", comment = "Toggle cSpigot's custom client-version based resource pack system")
    public static boolean RESOURCE_PACK_SYSTEM = false;
    @ConfigValue(priority = 104, path = "SETTINGS.RESOURCE-PACK-SYSTEM.KICK-ON-DENY", comment = "Should we kick players if they deny applying the resource pack?")
    public static boolean RESOURCE_KICK_ON_DENY = false;
    @ConfigValue(priority = 105, path = "SETTINGS.RESOURCE-PACK-SYSTEM.KICK-ON-FAIL", comment = "Should we kick players if their resource pack fails to apply?")
    public static boolean RESOURCE_KICK_ON_FAIL = false;
    @ConfigValue(priority = 106, path = "SETTINGS.RESOURCE-PACK-SYSTEM.KICK-MESSAGE", comment = "Edit the kick message for the resource pack system")
    public static String KICK_MESSAGE = "&eThis server requires you to load the resource pack!\n&ePlease re-log and try again!";
    @ConfigValue(priority = 106, path = "SETTINGS.RESOURCE-PACK-SYSTEM.PACK-LIST", comment =
            "Here you can put a list of resource packs like so 'protocol:resourcePackURL:hash' \n" +
            "DO NOT PUT 'https://' in the link, its automatically added!")
    public static List<String> RESOURCE_PACK_LIST = new ArrayList<>();
    @ConfigValue(priority = 107, path = "SETTINGS.NETTY.WORKER-THREADS", comment =
            "How many netty worker threads should we use? Set to -1 for automatic.")
    public static int NETTY_WORKER_THREADS = -1;
    @ConfigValue(priority = 107, path = "SETTINGS.NETTY.CONTROLLER-THREADS", comment =
            "How many netty controller threads should we use? 2 is the recommended number.")
    public static int NETTY_CONTROLLER_THREADS = 2;
    @ConfigValue(priority = 107, path = "SETTINGS.NETTY.QUEUE-NETWORK-MANAGER-ADD", comment =
            "Should we queue the new network mangers to be added on network tick?\n" +
            "Prevents blocking on adding a new network manager while the server is ticking.\n" +
            "Caution: Might cause issues with anti-cheats or packet APIs wanting network managers instantly.")
    public static boolean QUEUE_NETWORK_MANAGERS = true;
    @ConfigValue(priority = 108, path = "SETTINGS.ASYNC-MONITOR.TASK-TIMEOUT", comment =
            "After how long (in seconds) should async tasks timeout?")
    public static int ASYNC_TASK_TIMEOUT = 10;
    @ConfigValue(priority = 108, path = "SETTINGS.ASYNC-MONITOR.ENABLE-TIMEOUTS", comment =
            "Enable active task timeout monitoring? Is useful when debugging crashes, but can cause some performance load.")
    public static boolean ASYNC_MONITOR_TIMEOUT_ENABLED = false;



    /**
     * Initiation method for the main cSpigot file
     */
    public SettingsConfig(File file) {
        super("settings", file);
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public void addSeparateComments() {
        this.addComment("SETTINGS.SECURITY.CHAT-COMPONENT", "Patches the exploit of overloading the ChatComponent to crash servers");
        this.addComment("SETTINGS.PEARLS.TALIBAN-PEARLS", "Configure the pass-through settings of pearls");
        this.addComment("SETTINGS.ENTITY-TRACKER.ADD-REMOVE-RATE",
                "How often (in ticks) should entity tracker update if entity is visible/hidden for players." +
                "\nBigger values can have good impact on performance, but it can take longer for entity to show/hide." +
                "\nThese values can be much lower with async tracking enabled");

        try {
            THEME = ChatColor.valueOf(COLOR);
        } catch (Exception e) {
            THEME = ChatColor.RED;
            COLOR = "RED";

            log.info("[cSpigot] Invalid theme color, defaulting to red!");
        }

        CHAT_BAR = THEME + CC.translate("&m--------&7&m" + StringUtils.repeat("-", 37) + THEME + "&m--------");

        RegionFileCache.readConfig();

        if (REDUCED_SPLASH && SMOOTH_POTIONS) {
            log.info("[cSpigot] Alert! Smooth Potting and Reduced Splash can not be enabled at the same time.");
            log.info("[cSpigot] We have disabled Reduced Splash option for you, smooth potting is now turned on.");
            REDUCED_SPLASH = false;
        }

        if (FILTERS_LIST.isEmpty()) {
            FILTERS_LIST.addAll(Arrays.asList(
                    "[WorldEdit] WorldEdit has detected you are using Java 7 ",
                    "[WorldEdit] WorldEdit will stop supporting Java less than version 8"));
        }

        // Versions before 4.1, had a bug where this option was inverted meaning people had changed it as false to make it work
        // Now in the next update it was fixed but option was still false, so I'm manually doing it only for this version
        if (!this.getConfiguration().isSet("SETTINGS.SYSTEM.CONFIG-VER")) {
            this.set("SETTINGS.SYSTEM.CONFIG-VER", 4);
            SettingsConfig.EXPLOSION_DAMAGES_PLAYER = true;
        }

        // Versions before 4.2, had this option inverted, so setting it.
        if (CONFIG_VER < 5) {
            this.set("SETTINGS.SYSTEM.CONFIG-VER", 5);
            SettingsConfig.CHECK_ROGUE_ARM_SWINGS = true;
        }

        if (this.getConfiguration().isSet("DEBUG.CHINA-MODE")) {
            DIRECT_IP = this.getConfiguration().getBoolean("DEBUG.CHINA-MODE");
        }

        if (!BUKKIT_UNLOAD_QUEUE && !SYSTEM_DEBUG) {
            BUKKIT_UNLOAD_QUEUE = true;
        }

        if (!TRACK_CPS && LIMIT_CPS) {
            TRACK_CPS = true;
            log.info("[cSpigot] CPS limiter was turned on but tracking cps option was not, enabling CPS Tracking.");
        }

        if (ASYNC_CHUNKS && !BUKKIT_UNLOAD_QUEUE) {
            BUKKIT_UNLOAD_QUEUE = true;
            log.info("[cSpigot] Async chunks do not support non-bukkit unload queue, turning it back on!");
        }

        if (!AWAIT_ENTITY_UPDATES && !BYPASS_AWAIT_DEPEND) {
            if (ASYNC_ENTITY_ADD) {
                log.info("[cSpigot] We detected that 'AWAIT' option in tracker is turned off, but options dependant on it were on.");
                log.info("[cSpigot] Turning on 'AWAIT' option to ensure compatibility.");
                AWAIT_ENTITY_UPDATES = true;
            }
        }

        if ((ASYNC_CHUNKS || ASYNC_GENERATION) && USE_BUKKIT_CHUNK_MAP) {
            log.info("[cSpigot] BukkitChunkMap is not supported by async chunks, turning off...");
            USE_BUKKIT_CHUNK_MAP = false;
        }
    }

    /**
     * Returns the configuration fields with {@link ConfigValue}
     * annotation on them.
     *
     * @return {@link List}
     */
    public List<Field> getConfigFields() {
        List<Field> annotatedFields = new ArrayList<>();

        for ( Field field : SettingsConfig.class.getDeclaredFields() ) {
            if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isStatic(field.getModifiers())) continue;

            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            if (configValue == null) continue;

            annotatedFields.add(field);
        }

        annotatedFields.sort(Comparator.comparingInt(field -> field.getAnnotation(ConfigValue.class).priority()));

        return annotatedFields;
    }

    /**
     * The header for this configuration file
     *
     * @return {@link String[]} header
     */
    public String[] getHeader() {
        return new String[]{
                " This is the main configuration file for cSpigot."
                + "As you can see, there's tons to configure. Some options may impact gameplay, so use"
                + "with caution, and make sure you know what each option does before configuring."
                + "\n"
                + "If you need help with the configuration or have any questions related to cSpigot,"
                + "join us in our Discord."
                + " "
                + "Discord: https://dsc.gg/refine"
        };
    }

    public static void sendDebug(String string) {
        if (!DEBUG_MODE) return;

        for (UUID admin : ADMINS) {
            Player drizzy = Bukkit.getPlayer(admin);
            if (drizzy == null) return;

            drizzy.sendMessage(CC.translate("&e&l[DEBUG] &e" + string));
        }
    }

}
