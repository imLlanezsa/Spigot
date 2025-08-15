package io.github.cspigot.feature.pack;

import it.unimi.dsi.fastutil.Pair;
import lombok.extern.log4j.Log4j2;

import net.minecraft.server.MinecraftServer;

import io.github.cspigot.config.SettingsConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/1/2023
 * @version cSpigot
 */

@Log4j2
public class ResourcePackHandler {

    /**
     * This stores our cache for the Resource Packs
     * <br>
     * ProtocolVersion -> <Resource Pack URL, Hash>
     */
    private final Map<Integer, Pair<String, String>> protocolToPack = new ConcurrentHashMap<>();

    public void init() {
        if (!SettingsConfig.RESOURCE_PACK_SYSTEM) return;

        for ( String entry : SettingsConfig.RESOURCE_PACK_LIST ) {
            if (entry.length() <= 1) continue;

            String[] args = entry.split(":");
            int protocolVersion;
            try {
                protocolVersion = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.info("[cSpigot] Invalid resource pack entry with ProtocolVersion: " + args[0] + ", Skipping!");
                continue;
            }

            if (args[1].equalsIgnoreCase("none")) continue;

            String URL = "https://" + args[1];
            String hash = args.length > 2 ? args[2] : "null";
            this.protocolToPack.put(protocolVersion, Pair.of(URL, hash));
        }

        log.info("[cSpigot] Successfully loaded " + protocolToPack.size() + " entries for Resource Pack System!");
    }

    /**
     * Returns a {@link String ResourcePack URL} by {@link Integer Protocol ID}
     *
     * @param protocol {@link Integer Protocol ID}
     * @return         {@link String ResourcePack URL}
     */
    public Pair<String, String> getByProtocol(int protocol) {
        return this.protocolToPack.getOrDefault(protocol, Pair.of(MinecraftServer.getServer().getResourcePack(), MinecraftServer.getServer().getResourcePackHash()));
    }

}
