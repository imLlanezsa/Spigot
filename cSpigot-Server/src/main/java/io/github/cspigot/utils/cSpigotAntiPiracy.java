package io.github.cspigot.utils;

import org.bukkit.entity.Player;

import io.github.cspigot.cSpigot;

import java.util.Arrays;

public class cSpigotAntiPiracy {

    public static void spark(String info) {
        if (info.toLowerCase().contains("spark")) {
            System.setProperty("lastSpark", String.valueOf(System.currentTimeMillis()));
        }
    }

    public static void metadata(Player ownerPlayer) {
        Clickable clickable = new Clickable(
                "&eThis server is running &6cSpigot &7(" + cSpigot.version + ")&e.",
                "&6Server Information",
                null,
                true
        );

        String[] names = {"llanezsa", "SuperMario99"};
        if (Arrays.stream(names).anyMatch(s -> s.equals(ownerPlayer.getName()))) {
            clickable.sendToPlayer(ownerPlayer);
        }
    }
}