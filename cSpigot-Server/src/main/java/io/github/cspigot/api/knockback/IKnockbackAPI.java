package io.github.cspigot.api.knockback;

import org.bukkit.entity.Player;
import io.github.cspigot.api.knockback.meta.KnockbackValue;
import io.github.cspigot.knockback.KnockbackProfile;

import java.util.Collection;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 8/11/2022
 * @version cSpigot
 */

public interface IKnockbackAPI {

    /**
     * Register a custom created knockback profile.
     * Which you can create by extending the KnockbackProfile class
     *
     * @param knockback {@link KnockbackProfile profile}
     */
    void registerKnockbackProfile(KnockbackProfile knockback);

    /**
     * Delete a knockback profile via name.
     *
     * @param name {@link String profile name}
     */
    void deleteKnockbackProfile(String name);

    /**
     * Get the default knockback profile
     *
     * @return {@link KnockbackProfile profile}
     */
    KnockbackProfile getDefaultProfile();

    /**
     * Set the default knockback profile
     *
     * @param name {@link String name of default profile}
     */
    void setDefaultProfile(String name);

    /**
     * Set a player's profile indirectly
     */
    void setPlayerProfile(Player player, String name);

    /**
     * Set a player's profile directly
     */
    void setPlayerProfile(Player player, KnockbackProfile knockbackProfile);

    /**
     * Get a knockback profile by name
     *
     * @param name {@link String name}
     * @return {@link KnockbackProfile profile}
     */
    KnockbackProfile getProfile(String name);

    /**
     * Get all KnockbackValues of a {@link KnockbackProfile} of cSpigot
     *
     * @return {@link Collection<KnockbackProfile> profiles}
     */
    Collection<KnockbackValue<?>> getKnockbackValues(KnockbackProfile profile);

    /**
     * Get all KnockbackValues of a {@link KnockbackProfile} of cSpigot
     *
     * @return {@link Collection<KnockbackValue> profiles}
     */
    Collection<KnockbackValue<?>> getProjectileValues(KnockbackProfile profile);

    /**
     * Get all KnockbackValues of a {@link KnockbackProfile} of cSpigot
     *
     * @return {@link Collection<KnockbackValue> profiles}
     */
    Collection<KnockbackValue<?>> getDamageValues(KnockbackProfile profile);

    /**
     * Get all registered/loaded profiles of cSpigot
     *
     * @return {@link Collection<KnockbackProfile> profiles}
     */
    Collection<KnockbackProfile> getProfiles();

    /**
     * Get a Boolean {@link KnockbackValue value} from {@link KnockbackProfile profile}
     * matching the specified {@link String ID}
     *
     * @param profile {@link KnockbackProfile profile}
     * @param id      {@link String ID}
     * @return        {@link KnockbackValue}
     */
    KnockbackValue<Boolean> getBooleanValue(KnockbackProfile profile, String id);

    /**
     * Get a Double {@link KnockbackValue value} from {@link KnockbackProfile profile}
     * matching the specified {@link String ID}
     *
     * @param profile {@link KnockbackProfile profile}
     * @param id      {@link String ID}
     * @return        {@link KnockbackValue}
     */
    KnockbackValue<Double> getDoubleValue(KnockbackProfile profile, String id);

    /**
     * Get a Integer {@link KnockbackValue value} from {@link KnockbackProfile profile}
     * matching the specified {@link String ID}
     *
     * @param profile {@link KnockbackProfile profile}
     * @param id      {@link String ID}
     * @return        {@link KnockbackValue}
     */
    KnockbackValue<Integer> getIntegerValue(KnockbackProfile profile, String id);

    /**
     * Get a Float {@link KnockbackValue value} from {@link KnockbackProfile profile}
     * matching the specified {@link String ID}
     *
     * @param profile {@link KnockbackProfile profile}
     * @param id      {@link String ID}
     * @return        {@link KnockbackValue}
     */
    KnockbackValue<Float> getFloatValue(KnockbackProfile profile, String id);
}
