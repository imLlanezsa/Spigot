package io.github.cspigot.knockback;

import io.github.cspigot.knockback.impl.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.knockback.impl.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @version cSpigot
 * @since 12/9/2021
 */

@Log4j2
@Getter
public class KnockbackHandler {

    private static final String[] header = {
            "This is the configuration file for all the knockback profiles, in here you can",
            "easily setup profiles, change knockback values, and change default knockback",
            "or create knockback profiles manually. Be careful while editing these manually",
            "though, because this configuration can easily break!",
            "",
    };
    @Setter private int version;

    private final Map<String, KnockbackProfile> profiles = new ConcurrentHashMap<>();
    private YamlFile config;
    private String defaultKnockback = "default";

    /**
     * Loading/initiating logic of the handler
     */
    public void init() {
        if (!profiles.isEmpty()) profiles.clear();

        log.info("[cSpigot] Loading knockback profiles from profiles.yml!");

        File file = new File("profiles.yml");
        config = new YamlFile(file);

        if (!file.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.initConfig();

            if (this.checkPreviousConfig()) return;

            this.createDefaultKnockback();
            this.save();
            return;
        }

        this.initConfig();
        this.load();
        this.createDefaultKnockback();

        log.info("[cSpigot] Loaded knockback profiles from profiles.yml (" + profiles.size() + ")");
    }

    private void initConfig() {
        try {
            config.loadWithComments();
        } catch (Exception e) {
            e.printStackTrace();
        }
        config.options().copyDefaults(true);
    }

    private boolean checkPreviousConfig() {
        File file = new File("knockback.yml");
        if (file.exists()) {
            log.info("[cSpigot] Old knockback config found, converting....");
            YamlFile oldConfig;
            try {
                oldConfig = YamlFile.loadConfiguration(file);
            } catch (Exception e) {
                log.error("[cSpigot] An error occurred while loading the old knockback config, please check for syntax errors!");
                return false;
            }

            // Converts old configs to new ones
            KnockbackConverter converter = new KnockbackConverter();
            converter.convertOld(oldConfig, file);
            return true;
        }
        return false;
    }

    /**
     * Loading logic of the handler
     */
    public void load() {
        try {
            this.defaultKnockback = config.getString("DEFAULT-KNOCKBACK", "default");

            ConfigurationSection section = config.getConfigurationSection("PROFILES");
            if (section == null) section = config.createSection("PROFILES");
            if (section.getKeys(false).isEmpty()) return;

            for (String key : section.getKeys(false)) {
                KnockbackType type;

                try {
                    type = KnockbackType.valueOf(section.getString(key + ".TYPE"));
                } catch (Exception e) {
                    log.error("[cSpigot] Failed to load " + key + " because the type is invalid!");
                    continue;
                }

                KnockbackProfile knockback;

                switch (type) {
                    case EXPLICIT: {
                        knockback = new ExplicitKnockbackProfile(key.toLowerCase());
                        break;
                    }
                    case EXPERT: {
                        knockback = new ExpertKnockbackProfile(key.toLowerCase());
                        break;
                    }
                    case SMOOTH: {
                        knockback = new SmoothKnockbackProfile(key.toLowerCase());
                        break;
                    }
                    case DETAILED: {
                        knockback = new DetailedKnockbackProfile(key.toLowerCase());
                        break;
                    }
                    case EXPERIMENTAL: {
                        knockback = new ExperimentalKnockbackProfile(key.toLowerCase());
                        break;
                    }
                    default: {
                        knockback = new EasyKnockbackProfile(key.toLowerCase());
                        break;
                    }
                }
                knockback.load(config, "PROFILES." + key + ".");
                this.profiles.put(key.toLowerCase(), knockback);
                config.save();
            }
        } catch (Exception e) {
            log.error("[cSpigot] There was an error loading the knockback profiles, please check your file for syntax errors.");
        }
        this.save();
    }

    /**
     * Saving logic of the handler
     */
    public void save() {
        try {
            config.options().header(String.join("\n", header));
            config.set("DEFAULT-KNOCKBACK", this.defaultKnockback);

            for (KnockbackProfile profile : this.profiles.values()) {
                profile.save(config, "PROFILES." + profile.getName().toUpperCase() + ".");
            }

            config.save();
        } catch (Exception e) {
            log.info("[cSpigot] There was an error saving the knockback profiles!");
        }
    }

    public void createDefaultKnockback() {
        if (!this.profiles.containsKey(defaultKnockback)) {
            this.profiles.put(defaultKnockback, new EasyKnockbackProfile(defaultKnockback));
        }
    }

    /**
     * Add a knockback profile to the {@link Map}
     *
     * @param profile The knockback profile being added
     */
    public void addKnockbackProfile(KnockbackProfile profile) {
        this.profiles.put(profile.getName().toLowerCase(), profile);
    }

    /**
     * Remove a knockback profile from the {@link Map}
     *
     * @param name The knockback profile being removed
     */
    public void removeKnockbackProfile(String name) {
        this.profiles.remove(name);
        this.config.set("PROFILES." + name.toUpperCase(), null);
        try {
            this.config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Does the knockback profile exist in the {@link Map}
     *
     * @param name The knockback profile being checked
     * @return {@link Boolean} does it or exist or not
     */
    public boolean hasKnockbackProfile(String name) {
        return this.profiles.containsKey(name);
    }

    /**
     * Returns a collection of all knockback profiles
     *
     * @return {@link Collection<KnockbackProfile>} profiles
     */
    public List<KnockbackProfile> getProfiles() {
        return new ArrayList<>(this.profiles.values());
    }

    /**
     * Get a knockback profile from the {@link Map} using its name
     *
     * @param name the name of the profile
     * @return {@link KnockbackProfile} the knockback profile
     */
    public KnockbackProfile getKnockbackProfile(String name) {
        return this.profiles.get(name.toLowerCase());
    }

    /**
     * This method returns the default knockback profile
     *
     * @return {@link KnockbackProfile} the default knockback profile
     */
    public KnockbackProfile getDefaultKnockback() {
        KnockbackProfile knockbackProfile = this.profiles.get(defaultKnockback);
        if (knockbackProfile == null) {
            knockbackProfile = new EasyKnockbackProfile(defaultKnockback.toLowerCase());
            this.addKnockbackProfile(knockbackProfile);
        }
        return knockbackProfile;
    }

    /**
     * Set the default knockback profile's name
     *
     * @param name the name of default knockback profile
     */
    public void setDefaultKnockback(String name) {
        this.defaultKnockback = name;
    }
}
