package io.github.cspigot.knockback;

import io.github.cspigot.cSpigot;
import lombok.Getter;

import lombok.SneakyThrows;
import net.minecraft.server.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import org.simpleyaml.configuration.file.YamlFile;

import io.github.cspigot.api.knockback.IKnockback;
import io.github.cspigot.api.knockback.meta.KnockbackType;
import io.github.cspigot.api.knockback.meta.KnockbackValue;
import io.github.cspigot.cmds.impl.settings.KnockbackCommand;
import io.github.cspigot.utils.ProjectileUtil;

import java.lang.reflect.Field;
import java.util.*;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 9/4/2021
 * @version cSpigot
 */

@Getter
public abstract class KnockbackProfile implements IKnockback<EntityPlayer, Entity, DamageSource> {

    public static double REDUCER = 2.25;

    private final List<KnockbackValue<?>> values;

    private final KnockbackValue<Boolean> overrideHitDelay = new KnockbackValue<>("overridedamageticks", "DAMAGE-TICKS.OVERRIDE", "Override MaxNoDamageTicks", "Override vanilla damage ticks with cSpigot's", 50, Boolean.class, true);
    private final KnockbackValue<Integer> hitDelay = new KnockbackValue<>("maxnodamageticks", "DAMAGE-TICKS.VALUE", "Max NoDamage Ticks", "The delay between a player's ability to damage an entity", 49, Integer.class, 20);

    private final KnockbackValue<Boolean> limitVertProjectile = new KnockbackValue<>("limitVertProjectile", "PROJECTILES.Y-LIMIT.ENABLED", "Projectile YLimit", "Should we limit projectile's vertical kb?", 47, Boolean.class, true);
    private final KnockbackValue<Double> projectileVertLimit = new KnockbackValue<>("vLimitProjectile", "PROJECTILES.Y-LIMIT.VALUE", "Projectile YLimit Value", "Vertical limit value of projectiles", 46, Double.class, 0.4000000059604645D);

    private final KnockbackValue<Boolean> bowEnabled = new KnockbackValue<>("overrideBowKB", "PROJECTILES.ARROW.OVERRIDE", "Enable Bow KB", "Override vanilla arrow knockback with cSpigot's", 45, Boolean.class, true);
    private final KnockbackValue<Double> bowH = new KnockbackValue<>("bowH", "PROJECTILES.ARROW.HORIZONTAL", "Bow Horizontal Multiplier", "Horizontal knockback dealt by an Arrow", 44, Double.class, 0.4D);
    private final KnockbackValue<Double> bowV = new KnockbackValue<>("bowV", "PROJECTILES.ARROW.VERTICAL", "Bow Vertical Multiplier", "Vertical knockback dealt by an Arrow", 43, Double.class, 0.4D);
    private final KnockbackValue<Boolean> straightBow = new KnockbackValue<>("straightBow", "PROJECTILES.ARROW.STRAIGHT-BOOST", "Straight Bow KB", "Toggle whether an arrow moves you in a straight line", 42, Boolean.class, false);

    private final KnockbackValue<Boolean> rodEnabled = new KnockbackValue<>("overrideRodKB", "PROJECTILES.FISH-ROD.OVERRIDE", "Enable Rod KB", "Override vanilla rod knockback with cSpigot's", 41, Boolean.class, true);
    private final KnockbackValue<Double> rodH = new KnockbackValue<>("rodH", "PROJECTILES.FISH-ROD.HORIZONTAL", "Rod Horizontal Multiplier", "Horizontal knockback dealt by a Fish-Rod", 40, Double.class, 0.4D);
    private final KnockbackValue<Double> rodV = new KnockbackValue<>("rodV", "PROJECTILES.FISH-ROD.VERTICAL", "Rod Vertical Multiplier", "Vertical knockback dealt by a Fish-Rod", 39, Double.class, 0.4D);
    private final KnockbackValue<Double> rodSpeed = new KnockbackValue<>("rodSpeed", "PROJECTILES.FISH-ROD.SPEED", "Rod Speed Multiplier", "Multiplies the amount given with the rod's vector magnitude (Faster/Smoother Rodding)", 38, Double.class, 2.2D);

    private final KnockbackValue<Boolean> pearlEnabled = new KnockbackValue<>("overridePearlKB", "PROJECTILES.PEARL.OVERRIDE", "Enable Pearl KB", "Override vanilla pearl knockback with cSpigot's", 37, Boolean.class, true);
    private final KnockbackValue<Double> pearlH = new KnockbackValue<>("pearlH", "PROJECTILES.PEARL.HORIZONTAL", "Pearl Horizontal Multiplier", "Horizontal knockback dealt by a Pearl", 36, Double.class, 0.4D);
    private final KnockbackValue<Double> pearlV = new KnockbackValue<>("pearlV", "PROJECTILES.PEARL.VERTICAL", "Pearl Vertical Multiplier", "Vertical knockback dealt by a Pearl", 35, Double.class, 0.4D);

    private final KnockbackValue<Boolean> explosionEnabled = new KnockbackValue<>("overrideExplosionKB", "PROJECTILES.EXPLODE.OVERRIDE", "Enable Explosion KB", "Override vanilla explosion knockback with cSpigot's", 34, Boolean.class, true);
    private final KnockbackValue<Double> explosionH = new KnockbackValue<>("explosionH", "PROJECTILES.EXPLODE.HORIZONTAL", "Explosion Horizontal Multiplier", "Horizontal knockback dealt by a Explosion", 33, Double.class, 0.4D);
    private final KnockbackValue<Double> explosionV = new KnockbackValue<>("explosionV", "PROJECTILES.EXPLODE.VERTICAL", "Explosion Vertical Multiplier", "Vertical knockback dealt by a Explosion", 32, Double.class, 0.4D);
    private final KnockbackValue<Boolean> straightExplosion = new KnockbackValue<>("straightExplosion", "PROJECTILES.EXPLODE.STRAIGHT-BOOST", "Straight Explode KB", "Toggle whether an explosion moves you in a straight line", 31, Boolean.class, false);

    private final KnockbackValue<Boolean> snowballEnabled = new KnockbackValue<>("overrideSnowballKB", "PROJECTILES.SNOW-BALL.OVERRIDE", "Enable Snowball KB", "Override vanilla snowball knockback with cSpigot's", 30, Boolean.class, true);
    private final KnockbackValue<Double> snowballH = new KnockbackValue<>("snowballH", "PROJECTILES.SNOW-BALL.HORIZONTAL", "Snowball Horizontal Multiplier", "Horizontal knockback dealt by a Snowball", 29, Double.class, 0.4D);
    private final KnockbackValue<Double> snowballV = new KnockbackValue<>("snowballV", "PROJECTILES.SNOW-BALL.VERTICAL", "Snowball Vertical Multiplier", "Vertical knockback dealt by a Snowball", 28, Double.class, 0.4D);

    private final KnockbackValue<Boolean> eggEnabled = new KnockbackValue<>("overrideEggKB", "PROJECTILES.EGG.OVERRIDE", "Enable Egg KB", "Override vanilla egg knockback with cSpigot's", 27, Boolean.class, true);
    private final KnockbackValue<Double> eggH = new KnockbackValue<>("eggH", "PROJECTILES.EGG.HORIZONTAL", "Egg Horizontal Multiplier", "Horizontal knockback dealt by a Egg", 26, Double.class, 0.4D);
    private final KnockbackValue<Double> eggV = new KnockbackValue<>("eggV", "PROJECTILES.EGG.VERTICAL", "Egg Vertical Multiplier", "Vertical knockback dealt by a Egg", 25, Double.class, 0.4D);

    private final KnockbackValue<Boolean> overridePotion = new KnockbackValue<>("overridePotion", "PROJECTILES.POTION.OVERRIDE", "Override Potion Settings", "Override settings.yml potion settings with this", 24, Boolean.class, false);
    private final KnockbackValue<Double> potionSpeed = new KnockbackValue<>("potionSpeed", "PROJECTILES.POTION.SPEED", "Potion's speed", "Splash potion's falling speed", 23, Double.class, 0.593D);
    private final KnockbackValue<Double> potionGravity = new KnockbackValue<>("potionGravity", "PROJECTILES.POTION.GRAVITY", "Potion's gravity", "Splash potion's negative y-velocity", 22, Double.class, 0.05D);
    private final KnockbackValue<Double> potionVerticalOffset = new KnockbackValue<>("potionVerticalOffset", "PROJECTILES.POTION.VERTICAL-OFFSET", "Potion's vertical offset", "Splash potion's vertical offset", 21, Double.class, -20.0D);
    private final KnockbackValue<Double> potionHorizontalOffset = new KnockbackValue<>("potionHorizontalOffset", "PROJECTILES.POTION.HORIZONTAL-OFFSET", "Potion's horizontal offset", "Splash potion's horizontal offset", 21, Double.class, 0D);


    private final String name;
    private final KnockbackType type;

    public KnockbackProfile(String name, KnockbackType type) {
        this.name = name;
        this.type = type;

        this.values = new ArrayList<>();
    }

    public void callVelocity(EntityPlayer attackedPlayer, Vector vector, double[] velocity) {
        KnockbackHandler knockbackHandler = cSpigot.getInstance().getKnockbackHandler();
        // Citizens overrides the entity#g method to apply its Events and calculations
        // This breaks our instant-client-packet for knockback system
        // So for NPCs, we just let it be done with normal bukkit ways.
        if (attackedPlayer.getBukkitEntity().hasMetadata("NPC") || attackedPlayer.getBukkitEntity().hasMetadata("bolt-npc")) {
            // Reset the motion variables
            // because the method below does += not =
            attackedPlayer.motX = 0;
            attackedPlayer.motY = 0;
            attackedPlayer.motZ = 0;

            attackedPlayer.g(vector.getX(), vector.getY(), vector.getZ());
            attackedPlayer.playerConnection.lastMotionTick = MinecraftServer.currentTick;
            // Remove all fall distance damage, because sometimes kb
            // can go a bit high and this just ruins kb
            if (attackedPlayer.fallDistance > 0) {
                attackedPlayer.fallDistance = 0;
            }
            return;
        }

        PlayerVelocityEvent event = new PlayerVelocityEvent(attackedPlayer.getBukkitEntity(), vector);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            vector = event.getVelocity();

            // Legacy knockback support.
            if (knockbackHandler.getVersion() <= 5) {
                attackedPlayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(attackedPlayer.getId(), vector.getX(), vector.getY() + 0.5, vector.getZ()));
            } else {
                attackedPlayer.getBukkitEntity().setVelocity(vector);
                attackedPlayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(attackedPlayer.getId(), vector.getX(), vector.getY(), vector.getZ()));
            }

            // Turn off fall damage
            if (attackedPlayer.fallDistance > 0) {
                attackedPlayer.fallDistance = 0.0f;
            }
        }
        attackedPlayer.playerConnection.lastMotionTick = MinecraftServer.currentTick;

        attackedPlayer.velocityChanged = false;
        attackedPlayer.motX = velocity[0];
        attackedPlayer.motY = knockbackHandler.getVersion() <= 5 ? velocity[0] : velocity[1];
        attackedPlayer.motZ = velocity[2];
    }

    /**
     * Handle and edit projectile knockback here
     */
    public double[] handleProjectileKnockback(DamageSource source) {
        double[] original = new double[2];

        original[0] = 0.4D;
        original[1] = 0.4D;

        if (source instanceof EntityDamageSourceIndirect) {
            Entity indirect = ((EntityDamageSourceIndirect) source).getProximateDamageSource();
            if (indirect instanceof EntityFishingHook) {
                if (!this.rodEnabled.getValue()) return original;

                original[0] = rodH.getValue();
                original[1] = rodV.getValue();
            } else if (indirect instanceof EntityArrow || source.translationIndex.equalsIgnoreCase("arrow")) {
                if (!this.bowEnabled.getValue()) return original;

                original[0] = bowH.getValue();
                original[1] = bowV.getValue();
            } else if (indirect instanceof EntityEnderPearl) {
                if (!this.pearlEnabled.getValue()) return original;

                original[0] = pearlH.getValue();
                original[1] = pearlV.getValue();
            } else if (indirect instanceof EntitySnowball) {
                if (!this.snowballEnabled.getValue()) return original;

                original[0] = snowballH.getValue();
                original[1] = snowballV.getValue();
            } else if (indirect instanceof EntityEgg) {
                if (!this.eggEnabled.getValue()) return original;

                original[0] = eggH.getValue();
                original[1] = eggV.getValue();
            } else if (source.isExplosion()) {
                if (!this.explosionEnabled.getValue()) return original;

                original[0] = explosionH.getValue();
                original[1] = explosionV.getValue();
            }
        }
        return original;
    }

    public void dealProjectileKnockback(Entity entity, double x, double z, DamageSource damageSource) {
        float magnitude = MathHelper.sqrt((x * x) + (z * z));
        double[] doubles = this.handleProjectileKnockback(damageSource);

        double h = doubles[0];
        double v = doubles[1];

        entity.motX /= 2.0D;
        entity.motY /= 2.0D;
        entity.motZ /= 2.0D;

        entity.motX -= x / (double) magnitude * h;
        entity.motY += v;
        entity.motZ -= z / (double) magnitude * h;

        if (limitVertProjectile.getValue() && entity.motY > projectileVertLimit.getValue()) {
            entity.motY = projectileVertLimit.getValue();
        }
    }

    /**
     * This fixes detailed/experts enchantment knockback
     */
    protected Vector reduceEnchant(Player player, Vector vector) {
        Player entity = ProjectileUtil.getLastAttacker(player);
        if (entity == null) return vector;

        ItemStack itemStack = entity.getItemInHand();
        if (itemStack == null) return vector;

        int level = itemStack.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.KNOCKBACK);
        if (level >= 1) {
            double x = vector.getX() / REDUCER;
            double z = vector.getZ() / REDUCER;

            vector.setX(x);
            vector.setZ(z);
        }
        return vector;
    }

    public void load(YamlFile config, String path) {
        for ( KnockbackValue value : this.getValues() ) {
            if (!config.contains(path + value.getPath())) {
                config.set(path + value.getPath(), value.getValue());
                continue;
            }

            if (value.getValue() instanceof Integer) {
                int val = config.getInt(path + value.getPath());
                value.setValue(val);
            } else if (value.getValue() instanceof Double) {
                double val = config.getDouble(path + value.getPath());
                value.setValue(val);
            } else if (value.getValue() instanceof Boolean) {
                boolean val = config.getBoolean(path + value.getPath());
                value.setValue(val);
            }
        }
    }

    public void save(YamlFile config, String path) {
        config.set(path + "TYPE", this.type.name());
        for ( KnockbackValue<?> value : this.getValues() ) {
            config.set(path + value.getPath(), value.getValue());

            if (value.getComment().length() > 0) { // No blank comments please.
                config.setComment(path + value.getPath(), value.getComment());
            }
        }
    }

    /**
     * This method is used for the {@link KnockbackCommand#projectile} command
     *
     * @return {@link List projectile values}
     */
    @SneakyThrows
    public List<KnockbackValue<?>> getProjectileValues() {
        List<KnockbackValue<?>> values = new ArrayList<>();
        for ( Field f : KnockbackProfile.class.getDeclaredFields() ) {
            if (f.getType() == KnockbackValue.class) {
                f.setAccessible(true);
                KnockbackValue<?> value = (KnockbackValue<?>) f.get(this);
                values.add(value);
            }
        }

        values.sort(Comparator.comparingInt(KnockbackValue::getPriority));
        Collections.reverse(values);

        values.remove(overrideHitDelay);
        values.remove(hitDelay);

        return values;
    }

    /**
     * This method is used for the {@link KnockbackCommand#damage} command
     *
     * @return {@link List damage-tick values}
     */
    public List<KnockbackValue<?>> getDamageTickValues() {
        List<KnockbackValue<?>> values = new ArrayList<>();

        values.add(overrideHitDelay);
        values.add(hitDelay);

        return values;
    }

    /**
     * This method is used for the {@link KnockbackCommand#view} command
     *
     * @return {@link List profile-only values}
     */
    @SneakyThrows
    public List<KnockbackValue<?>> getProfileValues() {
        List<KnockbackValue<?>> values = new ArrayList<>();
        for ( Field f : this.getClass().getDeclaredFields() ) {
            if (f.getType() == KnockbackValue.class) {
                f.setAccessible(true);
                KnockbackValue<?> value = (KnockbackValue<?>) f.get(this);
                values.add(value);
            }
        }
        values.sort(Comparator.comparingInt(KnockbackValue::getPriority));
        Collections.reverse(values);
        return values;
    }

    @SneakyThrows
    public void loadValues() {
        this.getValues().clear();

        List<Field> fields = new ArrayList<>();
        fields.addAll(Arrays.asList(this.getClass().getDeclaredFields()));
        fields.addAll(Arrays.asList(KnockbackProfile.class.getDeclaredFields()));

        for ( Field f : fields ) {
            if (f.getType() == KnockbackValue.class) {
                f.setAccessible(true);
                KnockbackValue<?> value = (KnockbackValue<?>) f.get(this);
                this.getValues().add(value);
            }
        }

        this.getValues().sort(Comparator.comparingInt(KnockbackValue::getPriority));
        Collections.reverse(this.getValues()); // Reversed for descending order
    }
}
