package net.minecraft.server;

import com.eatthepath.uuid.FastUUID;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import io.github.cspigot.cSpigot;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.feature.async.threading.ThreadingManager;
import io.github.cspigot.knockback.KnockbackProfile;
import io.github.cspigot.utils.DamageUtil;
import io.github.cspigot.utils.ProjectileUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// PaperSpigot end


@Log4j2
public abstract class EntityLiving extends Entity {

    private static final UUID a = FastUUID.parseUUID("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    private static final AttributeModifier b = (new AttributeModifier(EntityLiving.a, "Sprinting speed boost", 0.30000001192092896D, 2)).a(false);
    private AttributeMapBase c;
    public CombatTracker combatTracker = new CombatTracker(this);
    public final Map<Integer, MobEffect> effects = new ConcurrentHashMap<>();
    private final ItemStack[] h = new ItemStack[5];
    public boolean ar;
    public int as;
    public int at;
    public int hurtTicks;
    public int av;
    public float aw;
    public int deathTicks;
    public float ay;
    public float az;
    public float aA;
    public float aB;
    public float aC;
    public int maxNoDamageTicks = 20;
    public float aE;
    public float aF;
    public float aG;
    public float aH;
    public float aI;
    public float aJ;
    public float aK;
    public float aL;
    public float aM = 0.02F;
    public EntityHuman killer;
    protected int lastDamageByPlayerTime;
    protected boolean aP;
    protected int ticksFarFromPlayer;
    protected float aR;
    protected float aS;
    protected float aT;
    protected float aU;
    protected float aV;
    protected int aW;
    public float lastDamage;
    public boolean lastPlayerAttack; // cSpigot - Track previous player attack
    protected boolean aY;
    public float aZ;
    public float ba;
    protected float bb;
    protected int bc;
    protected double bd;
    protected double be;
    protected double bf;
    protected double bg;
    protected double bh;
    public boolean updateEffects = true;
    public EntityLiving lastDamager;
    public int hurtTimestamp;
    private EntityLiving bk;
    private int bl;
    private float bm;
    private int bn;
    private float bo;
    // CraftBukkit start
    public int expToDrop;
    public int maxAirTicks = 300;
    ArrayList<org.bukkit.inventory.ItemStack> drops = null;

    // CraftBukkit end
    // Spigot start
    public void inactiveTick() {
        super.inactiveTick();
        ++this.ticksFarFromPlayer; // Above all the floats
    }
    // Spigot end

    public void G() {
        this.damageEntity(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
    }

    public EntityLiving(World world) {
        super(world);
        this.initAttributes();
        // CraftBukkit - setHealth(getMaxHealth()) inlined and simplified to skip the instanceof check for EntityPlayer, as getBukkitEntity() is not initialized in constructor
        this.datawatcher.watch(6, (float) this.getAttributeInstance(GenericAttributes.maxHealth).getValue());
        this.k = true;
        this.aH = (float) ((Math.random() + 1.0D) * 0.009999999776482582D);
        this.setPosition(this.locX, this.locY, this.locZ);
        this.aG = (float) Math.random() * 12398.0F;
        this.yaw = (float) (Math.random() * 3.1415927410125732D * 2.0D);
        this.aK = this.yaw;
        this.S = 0.6F;
    }

    protected void h() {
        this.datawatcher.a(7, Integer.valueOf(0));
        this.datawatcher.a(8, Byte.valueOf((byte) 0));
        this.datawatcher.a(9, Byte.valueOf((byte) 0));
        this.datawatcher.a(6, Float.valueOf(1.0F));
    }

    protected void initAttributes() {
        this.getAttributeMap().b(GenericAttributes.maxHealth);
        this.getAttributeMap().b(GenericAttributes.c);
        this.getAttributeMap().b(GenericAttributes.MOVEMENT_SPEED);
    }

    protected void a(double d0, boolean flag, Block block, BlockPosition blockposition) {
        if (!this.V()) {
            this.W();
        }

        if (!this.world.isClientSide && this.fallDistance > 3.0F && flag) {
            IBlockData iblockdata = this.world.getType(blockposition);
            Block block1 = iblockdata.getBlock();
            float f = (float) MathHelper.f(this.fallDistance - 3.0F);

            if (block1.getMaterial() != Material.AIR) {
                double d1 = (double) Math.min(0.2F + f / 15.0F, 10.0F);

                if (d1 > 2.5D) {
                    d1 = 2.5D;
                }

                int i = (int) (150.0D * d1);

                // CraftBukkit start - visiblity api
                if (this instanceof EntityPlayer) {
                    ((WorldServer) this.world).sendParticles((EntityPlayer) this, EnumParticle.BLOCK_DUST, false, this.locX, this.locY, this.locZ, i, 0.0D, 0.0D, 0.0D, 0.15000000596046448D, new int[]{Block.getCombinedId(iblockdata)});
                } else {
                    ((WorldServer) this.world).a(EnumParticle.BLOCK_DUST, this.locX, this.locY, this.locZ, i, 0.0D, 0.0D, 0.0D, 0.15000000596046448D, new int[]{Block.getCombinedId(iblockdata)});
                }
                // CraftBukkit end
            }
        }

        super.a(d0, flag, block, blockposition);
    }

    public boolean aY() {
        return false;
    }

    public void K() {
        this.ay = this.az;
        super.K();
        boolean flag = this instanceof EntityHuman;

        if (this.isAlive()) {
            if (noDamageTicks <= maxNoDamageTicks / 2.0F && inBlock()) { // cSpigot - Don't check inBlock if entity is undamageable
                this.damageEntity(DamageSource.STUCK, 1.0F);
            } else if (SettingsConfig.BORDER_DAMAGE_CHECK && flag && !world.getWorldBorder().a(boundingBox)) {
                double d0 = world.getWorldBorder().a(this) + world.getWorldBorder().getDamageBuffer();
                if (d0 < 0.0D) {
                    damageEntity(DamageSource.STUCK, Math.max(1, MathHelper.floor(-d0 * world.getWorldBorder().getDamageAmount())));
                }
            }
        }

        if (this.isFireProof() || this.world.isClientSide) {
            this.extinguish();
        }

        boolean flag1 = flag && ((EntityHuman) this).abilities.isInvulnerable;

        if (this.isAlive()) {
            if (this.a(Material.WATER)) {
                if (!this.aY() && !this.hasEffect(MobEffectList.WATER_BREATHING.id) && !flag1) {
                    this.setAirTicks(this.j(this.getAirTicks()));
                    if (this.getAirTicks() == -20) {
                        this.setAirTicks(0);

                        for ( int i = 0; i < 8; ++i ) {
                            float f = this.random.nextFloat() - this.random.nextFloat();
                            float f1 = this.random.nextFloat() - this.random.nextFloat();
                            float f2 = this.random.nextFloat() - this.random.nextFloat();

                            this.world.addParticle(EnumParticle.WATER_BUBBLE, this.locX + (double) f, this.locY + (double) f1, this.locZ + (double) f2, this.motX, this.motY, this.motZ, new int[0]);
                        }

                        this.damageEntity(DamageSource.DROWN, 2.0F);
                    }
                }

                if (!this.world.isClientSide && this.au() && this.vehicle instanceof EntityLiving) {
                    this.mount((Entity) null);
                }
            } else {
                // CraftBukkit start - Only set if needed to work around a DataWatcher inefficiency
                if (this.getAirTicks() != 300) {
                    this.setAirTicks(maxAirTicks);
                }
                // CraftBukkit end
            }
        }

        if (this.isAlive() && this.U()) {
            this.extinguish();
        }

        this.aE = this.aF;
        if (this.hurtTicks > 0) {
            --this.hurtTicks;
        }

        if (this.noDamageTicks > 0 && !(this instanceof EntityPlayer)) {
            --this.noDamageTicks;
        }

        if (this.getHealth() <= 0.0F) {
            this.aZ();
        }

        if (this.lastDamageByPlayerTime > 0) {
            --this.lastDamageByPlayerTime;
        } else {
            this.killer = null;
        }

        if (this.bk != null && !this.bk.isAlive()) {
            this.bk = null;
        }

        if (this.lastDamager != null) {
            if (!this.lastDamager.isAlive()) {
                this.b((EntityLiving) null);
            } else if (this.ticksLived - this.hurtTimestamp > 100) {
                this.b((EntityLiving) null);
            }
        }

        this.bi();
        this.aU = this.aT;
        this.aJ = this.aI;
        this.aL = this.aK;
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
    }

    // CraftBukkit start
    public int getExpReward() {
        int exp = this.getExpValue(this.killer);

        if (!this.world.isClientSide && (this.lastDamageByPlayerTime > 0 || this.alwaysGivesExp()) && this.ba() && this.world.getGameRules().getBoolean("doMobLoot")) {
            return exp;
        } else {
            return 0;
        }
    }
    // CraftBukkit end

    public boolean isBaby() {
        return false;
    }

    protected void aZ() {
        ++this.deathTicks;
        if (this.deathTicks >= 20 && !this.dead) { // CraftBukkit - (this.deathTicks == 20) -> (this.deathTicks >= 20 && !this.dead)
            int i;

            // CraftBukkit start - Update getExpReward() above if the removed if() changes!
            i = this.expToDrop;
            while (i > 0) {
                int j = EntityExperienceOrb.getOrbValue(i);
                i -= j;
                this.world.addEntity(new EntityExperienceOrb(this.world, this.locX, this.locY, this.locZ, j));
            }
            this.expToDrop = 0;
            // CraftBukkit end

            this.die();

            for ( i = 0; i < 20; ++i ) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;

                this.world.addParticle(EnumParticle.EXPLOSION_NORMAL, this.locX + (double) (this.random.nextFloat() * this.width * 2.0F) - (double) this.width, this.locY + (double) (this.random.nextFloat() * this.length), this.locZ + (double) (this.random.nextFloat() * this.width * 2.0F) - (double) this.width, d0, d1, d2, new int[0]);
            }
        }

    }

    protected boolean ba() {
        return !this.isBaby();
    }

    protected int j(int i) {
        int j = EnchantmentManager.getOxygenEnchantmentLevel(this);

        return j > 0 && this.random.nextInt(j + 1) > 0 ? i : i - 1;
    }

    protected int getExpValue(EntityHuman entityhuman) {
        return 0;
    }

    protected boolean alwaysGivesExp() {
        return false;
    }

    public Random bc() {
        return this.random;
    }

    public EntityLiving getLastDamager() {
        return this.lastDamager;
    }

    public int be() {
        return this.hurtTimestamp;
    }

    public void b(EntityLiving entityliving) {
        this.lastDamager = entityliving;
        this.hurtTimestamp = this.ticksLived;
    }

    public EntityLiving bf() {
        return this.bk;
    }

    public int bg() {
        return this.bl;
    }

    public void p(Entity entity) {
        if (entity instanceof EntityLiving) {
            this.bk = (EntityLiving) entity;
        } else {
            this.bk = null;
        }

        this.bl = this.ticksLived;
    }

    public int bh() {
        return this.ticksFarFromPlayer;
    }

    public void b(NBTTagCompound nbttagcompound) {
        nbttagcompound.setFloat("HealF", this.getHealth());
        nbttagcompound.setShort("Health", (short) ((int) Math.ceil((double) this.getHealth())));
        nbttagcompound.setShort("HurtTime", (short) this.hurtTicks);
        nbttagcompound.setInt("HurtByTimestamp", this.hurtTimestamp);
        nbttagcompound.setShort("DeathTime", (short) this.deathTicks);
        nbttagcompound.setFloat("AbsorptionAmount", this.getAbsorptionHearts());
        ItemStack[] aitemstack = this.getEquipment();
        int i = aitemstack.length;

        int j;
        ItemStack itemstack;

        for ( j = 0; j < i; ++j ) {
            itemstack = aitemstack[j];
            if (itemstack != null) {
                this.c.a(itemstack.B());
            }
        }

        nbttagcompound.set("Attributes", GenericAttributes.a(this.getAttributeMap()));
        aitemstack = this.getEquipment();
        i = aitemstack.length;

        for ( j = 0; j < i; ++j ) {
            itemstack = aitemstack[j];
            if (itemstack != null) {
                this.c.b(itemstack.B());
            }
        }

        if (!this.effects.isEmpty()) {
            NBTTagList nbttaglist = new NBTTagList();
            Iterator iterator = this.effects.values().iterator();

            while (iterator.hasNext()) {
                MobEffect mobeffect = (MobEffect) iterator.next();

                nbttaglist.add(mobeffect.a(new NBTTagCompound()));
            }

            nbttagcompound.set("ActiveEffects", nbttaglist);
        }

    }

    public void a(NBTTagCompound nbttagcompound) {
        this.setAbsorptionHearts(nbttagcompound.getFloat("AbsorptionAmount"));
        if (nbttagcompound.hasKeyOfType("Attributes", 9) && this.world != null && !this.world.isClientSide) {
            GenericAttributes.a(this.getAttributeMap(), nbttagcompound.getList("Attributes", 10));
        }

        if (nbttagcompound.hasKeyOfType("ActiveEffects", 9)) {
            NBTTagList nbttaglist = nbttagcompound.getList("ActiveEffects", 10);

            for ( int i = 0; i < nbttaglist.size(); ++i ) {
                NBTTagCompound nbttagcompound1 = nbttaglist.get(i);
                MobEffect mobeffect = MobEffect.b(nbttagcompound1);

                if (mobeffect != null) {
                    this.effects.put(Integer.valueOf(mobeffect.getEffectId()), mobeffect);
                }
            }
        }

        // CraftBukkit start
        if (nbttagcompound.hasKey("Bukkit.MaxHealth")) {
            NBTBase nbtbase = nbttagcompound.get("Bukkit.MaxHealth");
            if (nbtbase.getTypeId() == 5) {
                this.getAttributeInstance(GenericAttributes.maxHealth).setValue((double) ((NBTTagFloat) nbtbase).c());
            } else if (nbtbase.getTypeId() == 3) {
                this.getAttributeInstance(GenericAttributes.maxHealth).setValue((double) ((NBTTagInt) nbtbase).d());
            }
        }
        // CraftBukkit end

        if (nbttagcompound.hasKeyOfType("HealF", 99)) {
            this.setHealth(nbttagcompound.getFloat("HealF"));
        } else {
            NBTBase nbtbase = nbttagcompound.get("Health");

            if (nbtbase == null) {
                this.setHealth(this.getMaxHealth());
            } else if (nbtbase.getTypeId() == 5) {
                this.setHealth(((NBTTagFloat) nbtbase).h());
            } else if (nbtbase.getTypeId() == 2) {
                this.setHealth((float) ((NBTTagShort) nbtbase).e());
            }
        }

        this.hurtTicks = nbttagcompound.getShort("HurtTime");
        this.deathTicks = nbttagcompound.getShort("DeathTime");
        this.hurtTimestamp = nbttagcompound.getInt("HurtByTimestamp");
    }

    // CraftBukkit start
    private boolean isTickingEffects = false;
    private List<Object> effectsToProcess = Lists.newArrayList();
    // CraftBukkit end

    protected void bi() {
        Iterator iterator = this.effects.keySet().iterator();

        isTickingEffects = true; // CraftBukkit
        while (iterator.hasNext()) {
            Integer integer = (Integer) iterator.next();
            MobEffect mobeffect = (MobEffect) this.effects.get(integer);

            if (!mobeffect.tick(this)) {
                if (!this.world.isClientSide) {
                    PotionEffectExpireEvent event = new PotionEffectExpireEvent((LivingEntity) this.getBukkitEntity(), ProjectileUtil.toBukkit(mobeffect));
                    this.world.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        // Duration must be extended if event is cancelled
                        ProjectileUtil.extendDuration(mobeffect, mobeffect.getCachedDuration());
                        continue;
                    }
                    iterator.remove();
                    this.b(mobeffect);
                }
            } else if (mobeffect.getDuration() % 600 == 0) {
                this.a(mobeffect, false);
            }
        }
        // CraftBukkit start
        isTickingEffects = false;
        for (Object e : effectsToProcess) {
            if (e instanceof MobEffect) {
                addEffect((MobEffect) e);
            } else {
                removeEffect((Integer) e);
            }
        }
        // CraftBukkit end

        if (this.updateEffects) {
            if (!this.world.isClientSide) {
                this.B();
            }

            this.updateEffects = false;
        }

        int i = this.datawatcher.getInt(7);
        boolean flag = this.datawatcher.getByte(8) > 0;

        if (i > 0) {
            boolean flag1 = false;

            if (!this.isInvisible()) {
                flag1 = this.random.nextBoolean();
            } else {
                flag1 = this.random.nextInt(15) == 0;
            }

            if (flag) {
                flag1 &= this.random.nextInt(5) == 0;
            }

            if (flag1 && i > 0) {
                double d0 = (double) (i >> 16 & 255) / 255.0D;
                double d1 = (double) (i >> 8 & 255) / 255.0D;
                double d2 = (double) (i >> 0 & 255) / 255.0D;

                this.world.addParticle(flag ? EnumParticle.SPELL_MOB_AMBIENT : EnumParticle.SPELL_MOB, this.locX + (this.random.nextDouble() - 0.5D) * (double) this.width, this.locY + this.random.nextDouble() * (double) this.length, this.locZ + (this.random.nextDouble() - 0.5D) * (double) this.width, d0, d1, d2, new int[0]);
            }
        }

    }

    protected void B() {
        if (this.effects.isEmpty()) {
            this.bj();
            this.setInvisible(false);
        } else {
            int i = PotionBrewer.a(this.effects.values());

            this.datawatcher.watch(8, Byte.valueOf((byte) (PotionBrewer.b(this.effects.values()) ? 1 : 0)));
            this.datawatcher.watch(7, Integer.valueOf(i));
            this.setInvisible(this.hasEffect(MobEffectList.INVISIBILITY.id));
        }
    }

    protected void bj() {
        this.datawatcher.watch(8, Byte.valueOf((byte) 0));
        this.datawatcher.watch(7, Integer.valueOf(0));
    }

    public void removeAllEffects() {
        Iterator iterator = this.effects.keySet().iterator();

        while (iterator.hasNext()) {
            Integer integer = (Integer) iterator.next();
            MobEffect mobeffect = (MobEffect) this.effects.get(integer);

            if (!this.world.isClientSide) {
                iterator.remove();
                this.b(mobeffect);
            }
        }

    }

    public Collection<MobEffect> getEffects() {
        return this.effects.values();
    }

    public boolean hasEffect(int i) {
        // CraftBukkit - Add size check for efficiency
        return this.effects.size() != 0 && this.effects.containsKey(Integer.valueOf(i));
    }

    public boolean hasEffect(MobEffectList mobeffectlist) {
        return this.effects.containsKey(mobeffectlist.id);
    }

    public MobEffect getEffect(MobEffectList mobeffectlist) {
        return this.effects.get(mobeffectlist.id);
    }

    public void addEffect(MobEffect effect) {
        addEffect(effect, PotionEffectAddEvent.EffectCause.UNKNOWN);
    }

    public void addEffect(MobEffect mobeffect, PotionEffectAddEvent.EffectCause reason) {
        org.spigotmc.AsyncCatcher.catchOp("effect add"); // Spigot
        // CraftBukkit start
        if (isTickingEffects) {
            effectsToProcess.add(mobeffect);
            return;
        }
        // CraftBukkit end
        if (this.d(mobeffect)) {
            if (this.effects.containsKey(mobeffect.getEffectId())) {

                // cSpigot start - fire event
                MobEffect current = this.effects.get(mobeffect.getEffectId());

                PotionEffectExtendEvent event = new PotionEffectExtendEvent((LivingEntity) this.getBukkitEntity(),
                        ProjectileUtil.toBukkit(mobeffect),
                        ProjectileUtil.toBukkit(current), reason);
                this.world.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                // cSpigot end

                this.effects.get(mobeffect.getEffectId()).a(mobeffect);
                this.a(this.effects.get(mobeffect.getEffectId()), true);
            } else {

                // cSpigot start - fire event
                PotionEffectAddEvent event = new PotionEffectAddEvent((LivingEntity) this.getBukkitEntity(), ProjectileUtil.toBukkit(mobeffect), reason);
                this.world.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) return;
                // cSpigot end

                this.effects.put(mobeffect.getEffectId(), mobeffect);
                this.a(mobeffect);
            }

        }
    }


    public boolean d(MobEffect mobeffect) {
        if (this.getMonsterType() == EnumMonsterType.UNDEAD) {
            int i = mobeffect.getEffectId();

            if (i == MobEffectList.REGENERATION.id || i == MobEffectList.POISON.id) {
                return false;
            }
        }

        return true;
    }

    public boolean bm() {
        return this.getMonsterType() == EnumMonsterType.UNDEAD;
    }

    public void removeEffect(int i) {
        // CraftBukkit start
        if (isTickingEffects) {
            effectsToProcess.add(i);
            return;
        }
        // CraftBukkit end
        MobEffect mobeffect = this.effects.remove(i);

        if (mobeffect != null) {
            // cSpigot start
            PotionEffectRemoveEvent event = new PotionEffectRemoveEvent((LivingEntity) this.getBukkitEntity(),
                    ProjectileUtil.toBukkit(mobeffect));
            this.world.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                this.effects.put(i, mobeffect);
                return;
            }
            // cSpigot end
            this.b(mobeffect);
        }

    }

    protected void a(MobEffect mobeffect) {
        this.updateEffects = true;
        if (!this.world.isClientSide) {
            MobEffectList.byId[mobeffect.getEffectId()].b(this, this.getAttributeMap(), mobeffect.getAmplifier());
        }

    }

    protected void a(MobEffect mobeffect, boolean flag) {
        this.updateEffects = true;
        if (flag && !this.world.isClientSide) {
            MobEffectList.byId[mobeffect.getEffectId()].a(this, this.getAttributeMap(), mobeffect.getAmplifier());
            MobEffectList.byId[mobeffect.getEffectId()].b(this, this.getAttributeMap(), mobeffect.getAmplifier());
        }

    }

    protected void b(MobEffect mobeffect) {
        this.updateEffects = true;
        if (!this.world.isClientSide) {
            MobEffectList.byId[mobeffect.getEffectId()].a(this, this.getAttributeMap(), mobeffect.getAmplifier());
        }

    }

    // CraftBukkit start - Delegate so we can handle providing a reason for health being regained
    public void heal(float f) {
        heal(f, EntityRegainHealthEvent.RegainReason.CUSTOM);
    }

    public void heal(float f, EntityRegainHealthEvent.RegainReason regainReason) {
        float f1 = this.getHealth();

        if (f1 > 0.0F) {
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(this.getBukkitEntity(), f, regainReason);
            this.world.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                this.setHealth((float) (this.getHealth() + event.getAmount()));
            }
            // CraftBukkit end
        }

    }

    public final float getHealth() {
        // CraftBukkit start - Use unscaled health
        if (this instanceof EntityPlayer) {
            return (float) ((EntityPlayer) this).getBukkitEntity().getHealth();
        }
        // CraftBukkit end
        return this.datawatcher.getFloat(6);
    }

    public void setHealth(float f) {
        // CraftBukkit start - Handle scaled health
        if (this instanceof EntityPlayer) {
            org.bukkit.craftbukkit.entity.CraftPlayer player = ((EntityPlayer) this).getBukkitEntity();
            // Squeeze
            if (f < 0.0F) {
                player.setRealHealth(0.0D);
            } else if (f > player.getMaxHealth()) {
                player.setRealHealth(player.getMaxHealth());
            } else {
                player.setRealHealth(f);
            }

            // Griffin start - Instant respawn
            // only send the update to anyone if the player has not died.
            // if they do die, we handle all our stuff in EntityPlayer#die(DamageSource)
            if (player.getHealth() != 0 || !SettingsConfig.INSTANT_RESPAWN) {
                this.datawatcher.watch(6, Float.valueOf(player.getScaledHealth()));
            }
            // Griffin end - Instant respawn
            return;
        }
        // CraftBukkit end
        this.datawatcher.watch(6, Float.valueOf(MathHelper.a(f, 0.0F, this.getMaxHealth())));
    }

    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else if (this.world.isClientSide) {
            return false;
        } else {
            this.ticksFarFromPlayer = 0;
            if (this.getHealth() <= 0.0F) {
                return false;
            } else if (damagesource.o() && this.hasEffect(MobEffectList.FIRE_RESISTANCE)) {
                return false;
            } else {
                this.aB = 1.5F;
                boolean flag = true;
                int maxNoDamageTicks = this.maxNoDamageTicks;

                if (this instanceof EntityHuman) {
                    EntityHuman entityHuman = (EntityHuman) this;
                    KnockbackProfile profile = entityHuman.getKnockbackProfile();
                    if (profile.getOverrideHitDelay().getValue()) {
                        maxNoDamageTicks = profile.getHitDelay().getValue();
                    }
                }

                if ((float) this.noDamageTicks > maxNoDamageTicks / 2.0F) {
                    if (f <= this.lastDamage) {
                        this.forceExplosionKnockback = true; // CraftBukkit - SPIGOT-949 - for vanilla consistency, cooldown does not prevent explosion knockback
                        return false;
                    }

                    // cSpigot start - Don't allow player attacks while no damage ticks are active
                    // This fixes the long-standing pvp bug of double hits or knockback suddenly turning combo mode
                    if (SettingsConfig.FIX_DOUBLE_HIT_BUG && lastPlayerAttack && DamageUtil.isPlayerAttack(damagesource)) {
                        SettingsConfig.sendDebug("Blocking player attack");
                        return false;
                    }
                    // cSpigot end

                    // CraftBukkit start
                    if (!this.d(damagesource, f - this.lastDamage)) {
                        return false;
                    }
                    // CraftBukkit end
                    this.lastDamage = f;
                    this.lastPlayerAttack = DamageUtil.isPlayerAttack(damagesource); // cSpigot
                    flag = false;
                } else {
                    // CraftBukkit start
                    if (!this.d(damagesource, f)) {
                        return false;
                    }
                    this.lastDamage = f;
                    this.lastPlayerAttack = DamageUtil.isPlayerAttack(damagesource); // cSpigot
                    this.noDamageTicks = maxNoDamageTicks;
                    // CraftBukkit end
                    this.hurtTicks = this.av = 10;
                }

                // CraftBukkit start
                if (this instanceof EntityAnimal) {
                    ((EntityAnimal) this).cq();
                    if (this instanceof EntityTameableAnimal) {
                        ((EntityTameableAnimal) this).getGoalSit().setSitting(false);
                    }
                }
                // CraftBukkit end

                this.aw = 0.0F;
                Entity entity = damagesource.getEntity();

                if (entity != null) {
                    if (entity instanceof EntityLiving) {
                        this.b((EntityLiving) entity);
                    }

                    if (entity instanceof EntityHuman) {
                        this.lastDamageByPlayerTime = 100;
                        this.killer = (EntityHuman) entity;
                    } else if (entity instanceof EntityWolf) {
                        EntityWolf entitywolf = (EntityWolf) entity;

                        if (entitywolf.isTamed()) {
                            this.lastDamageByPlayerTime = 100;
                            this.killer = null;
                        }
                    }
                }

                // PaperSpigot start - Disable explosion knockback
                boolean knockbackCancelled = false;
                if (flag && !(knockbackCancelled = world.paperSpigotConfig.disableExplosionKnockback && damagesource.isExplosion() && this instanceof EntityHuman)) {
                    // PaperSpigot end
                    this.world.broadcastEntityEffect(this, (byte) 2);

                    // cSpigot start - Toggleable fall damage kb and stuck damage kb
                    this.fallDamageKB = damagesource == DamageSource.FALL;
                    this.stuckKB = damagesource == DamageSource.STUCK;
                    // cSpigot end

                    if (damagesource != DamageSource.DROWN) {
                        this.ac(); // cSpigot - Pail: UpdateVelocity
                    }

                    if (entity != null) {
                        double distanceX = entity.locX - this.locX;

                        double distanceZ;

                        for ( distanceZ = entity.locZ - this.locZ; distanceX * distanceX + distanceZ * distanceZ < 1.0E-4; distanceZ = (Math.random() - Math.random()) * 0.01D ) {
                            distanceX = (Math.random() - Math.random()) * 0.01D;
                        }

                        this.aw = (float) (MathHelper.b(distanceZ, distanceX) * 180.0D / 3.1415927410125732D - (double) this.yaw);
                        this.a(entity, distanceX, distanceZ, f, damagesource);
                    } else {
                        this.aw = (int) (Math.random() * 2.0D) * 180;
                    }
                }

                if (knockbackCancelled) this.world.broadcastEntityEffect(this, (byte) 2); // PaperSpigot

                String s;

                if (this.getHealth() <= 0.0F) {
                    s = this.bp();
                    if (flag && s != null) {
                        this.makeSound(s, this.bB(), this.bC());
                    }

                    this.die(damagesource);
                } else {
                    s = this.bo();
                    if (flag && s != null) {
                        this.makeSound(s, this.bB(), this.bC());
                    }
                }

                return true;
            }
        }
    }

    public void b(ItemStack itemstack) {
        this.makeSound("random.break", 0.8F, 0.8F + this.world.random.nextFloat() * 0.4F);

        for ( int i = 0; i < 5; ++i ) {
            Vec3D vec3d = new Vec3D(((double) this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);

            vec3d = vec3d.a(-this.pitch * 3.1415927F / 180.0F);
            vec3d = vec3d.b(-this.yaw * 3.1415927F / 180.0F);
            double d0 = (double) (-this.random.nextFloat()) * 0.6D - 0.3D;
            Vec3D vec3d1 = new Vec3D(((double) this.random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);

            vec3d1 = vec3d1.a(-this.pitch * 3.1415927F / 180.0F);
            vec3d1 = vec3d1.b(-this.yaw * 3.1415927F / 180.0F);
            vec3d1 = vec3d1.add(this.locX, this.locY + (double) this.getHeadHeight(), this.locZ);
            this.world.addParticle(EnumParticle.ITEM_CRACK, vec3d1.a, vec3d1.b, vec3d1.c, vec3d.a, vec3d.b + 0.05D, vec3d.c, Item.getId(itemstack.getItem()));
        }
    }

    public void die(DamageSource damagesource) {
        Entity entity = damagesource.getEntity();
        EntityLiving entityliving = this.bt();

        if (this.aW >= 0 && entityliving != null) {
            entityliving.b(this, this.aW);
        }

        if (entity != null) {
            entity.a(this);
        }

        this.aP = true;
        this.bs().g();
        if (!this.world.isClientSide) {
            int i = 0;

            if (entity instanceof EntityHuman) {
                i = EnchantmentManager.getBonusMonsterLootEnchantmentLevel((EntityLiving) entity);
            }

            if (this.ba() && this.world.getGameRules().getBoolean("doMobLoot")) {
                this.drops = new ArrayList<>(); // CraftBukkit - Setup drop capture

                this.dropDeathLoot(this.lastDamageByPlayerTime > 0, i);
                this.dropEquipment(this.lastDamageByPlayerTime > 0, i);
                if (this.lastDamageByPlayerTime > 0 && this.random.nextFloat() < 0.025F + (float) i * 0.01F) {
                    this.getRareDrop();
                }
                // CraftBukkit start - Call death event
                CraftEventFactory.callEntityDeathEvent(this, this.drops);
                this.drops = null;
            } else {
                CraftEventFactory.callEntityDeathEvent(this);
                // CraftBukkit end
            }
        }

        this.world.broadcastEntityEffect(this, (byte) 3);
    }

    protected void dropEquipment(boolean flag, int i) {
    }

    public void a(Entity entity, float f, double x, double z) {
        if (this.random.nextDouble() >= this.getAttributeInstance(GenericAttributes.c).getValue()) {
            this.ai = true;
            float magnitude = MathHelper.sqrt((x * x) + (z * z));
            float f2 = 0.4F;

            this.motX /= 2.0D;
            this.motY /= 2.0D;
            this.motZ /= 2.0D;

            this.motX -= x / (double) magnitude * (double) f2;
            this.motY += (double) f2;
            this.motZ -= z / (double) magnitude * (double) f2;

            if (this.motY > 0.4000000059604645D) {
                this.motY = 0.4000000059604645D;
            }
        }
    }

    public void a(Entity entity, double x, double z, float f, DamageSource damageSource) {
        // cSpigot start - custom knockback
        if (this instanceof EntityPlayer && entity instanceof EntityPlayer) {
            EntityPlayer attackedPlayer = (EntityPlayer) this;
            EntityPlayer attackerPlayer = (EntityPlayer) entity;

            try {
                KnockbackProfile profile = attackedPlayer.getKnockbackProfile();
                profile.attackLiving(this, attackerPlayer, x, z, damageSource);
            } catch (Exception e) {
                log.info("[cSpigot] An error occurred while handling knockback!");
                log.info("[cSpigot] Error: " + e.getMessage());
                //e.printStackTrace();
            }
            return;
            //}
        }
        // cSpigot end
        if (this.random.nextDouble() >= this.getAttributeInstance(GenericAttributes.c).getValue()) {
            this.a(entity, f, x, z);
        }
    }

    protected String bo() {
        return "game.neutral.hurt";
    }

    protected String bp() {
        return "game.neutral.die";
    }

    protected void getRareDrop() {
    }

    protected void dropDeathLoot(boolean flag, int i) {
    }

    public boolean k_() {
        int i = MathHelper.floor(this.locX);
        int j = MathHelper.floor(this.getBoundingBox().b);
        int k = MathHelper.floor(this.locZ);
        Block block = this.world.getType(new BlockPosition(i, j, k)).getBlock();

        return (block == Blocks.LADDER || block == Blocks.VINE) && (!(this instanceof EntityHuman) || !((EntityHuman) this).isSpectator());
    }

    public boolean isAlive() {
        return !this.dead && this.getHealth() > 0.0F;
    }

    public void e(float f, float f1) {
        super.e(f, f1);
        MobEffect mobeffect = this.getEffect(MobEffectList.JUMP);
        float f2 = mobeffect != null ? (float) (mobeffect.getAmplifier() + 1) : 0.0F;
        int i = MathHelper.f((f - 3.0F - f2) * f1);

        if (i > 0) {
            // CraftBukkit start
            if (!this.damageEntity(DamageSource.FALL, (float) i)) {
                return;
            }
            // CraftBukkit end
            this.makeSound(this.n(i), 1.0F, 1.0F);
            // this.damageEntity(DamageSource.FALL, (float) i); // CraftBukkit - moved up
            int j = MathHelper.floor(this.locX);
            int k = MathHelper.floor(this.locY - 0.20000000298023224D);
            int l = MathHelper.floor(this.locZ);
            Block block = this.world.getType(new BlockPosition(j, k, l)).getBlock();

            if (block.getMaterial() != Material.AIR) {
                Block.StepSound block_stepsound = block.stepSound;

                this.makeSound(block_stepsound.getStepSound(), block_stepsound.getVolume1() * 0.5F, block_stepsound.getVolume2() * 0.75F);
            }
        }

    }

    protected String n(int i) {
        return i > 4 ? "game.neutral.hurt.fall.big" : "game.neutral.hurt.fall.small";
    }

    public int br() {
        int i = 0;
        ItemStack[] aitemstack = this.getEquipment();
        int j = aitemstack.length;

        for ( int k = 0; k < j; ++k ) {
            ItemStack itemstack = aitemstack[k];

            if (itemstack != null && itemstack.getItem() instanceof ItemArmor) {
                int l = ((ItemArmor) itemstack.getItem()).c;

                i += l;
            }
        }

        return i;
    }

    protected void damageArmor(float f) {
    }

    protected float applyArmorModifier(DamageSource damagesource, float f) {
        if (!damagesource.ignoresArmor()) {
            int i = 25 - this.br();
            float f1 = f * (float) i;

            // this.damageArmor(f); // CraftBukkit - Moved into d(DamageSource, float)
            f = f1 / 25.0F;
        }

        return f;
    }

    protected float applyMagicModifier(DamageSource damagesource, float f) {
        if (damagesource.isStarvation()) {
            return f;
        } else {
            int i;
            int j;
            float f1;

            // CraftBukkit - Moved to d(DamageSource, float)
            if (false && this.hasEffect(MobEffectList.RESISTANCE) && damagesource != DamageSource.OUT_OF_WORLD) {
                i = (this.getEffect(MobEffectList.RESISTANCE).getAmplifier() + 1) * 5;
                j = 25 - i;
                f1 = f * (float) j;
                f = f1 / 25.0F;
            }

            if (f <= 0.0F) {
                return 0.0F;
            } else {
                i = EnchantmentManager.a(this.getEquipment(), damagesource);
                if (i > 20) {
                    i = 20;
                }

                if (i > 0 && i <= 20) {
                    j = 25 - i;
                    f1 = f * (float) j;
                    f = f1 / 25.0F;
                }

                return f;
            }
        }
    }

    // CraftBukkit start
    protected boolean d(final DamageSource damagesource, float f) { // void -> boolean, add final
        if (!this.isInvulnerable(damagesource)) {
            final boolean human = this instanceof EntityHuman;
            float originalDamage = f;
            Function<Double, Double> hardHat = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && EntityLiving.this.getEquipment(4) != null) {
                        return -(f - (f * 0.75F));
                    }
                    return -0.0;
                }
            };
            float hardHatModifier = hardHat.apply((double) f).floatValue();
            f += hardHatModifier;

            Function<Double, Double> blocking = f12 -> {
                if (human) {
                    if (!damagesource.ignoresArmor() && ((EntityHuman) EntityLiving.this).isBlocking() && f12 > 0.0F) {
                        return -(f12 - ((1.0F + f12) * world.paperSpigotConfig.playerBlockingDamageMultiplier));
                    }
                }
                return -0.0;
            };

            float blockingModifier = blocking.apply((double) f).floatValue();
            f += blockingModifier;

            Function<Double, Double> armor = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -(f - EntityLiving.this.applyArmorModifier(damagesource, f.floatValue()));
                }
            };
            float armorModifier = armor.apply((double) f).floatValue();
            f += armorModifier;

            Function<Double, Double> resistance = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    if (!damagesource.isStarvation() && EntityLiving.this.hasEffect(MobEffectList.RESISTANCE) && damagesource != DamageSource.OUT_OF_WORLD) {
                        int i = (EntityLiving.this.getEffect(MobEffectList.RESISTANCE).getAmplifier() + 1) * 5;
                        int j = 25 - i;
                        float f1 = f.floatValue() * (float) j;
                        return -(f - (f1 / 25.0F));
                    }
                    return -0.0;
                }
            };
            float resistanceModifier = resistance.apply((double) f).floatValue();
            f += resistanceModifier;

            Function<Double, Double> magic = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -(f - EntityLiving.this.applyMagicModifier(damagesource, f.floatValue()));
                }
            };
            float magicModifier = magic.apply((double) f).floatValue();
            f += magicModifier;

            Function<Double, Double> absorption = new Function<Double, Double>() {
                @Override
                public Double apply(Double f) {
                    return -(Math.max(f - Math.max(f - EntityLiving.this.getAbsorptionHearts(), 0.0F), 0.0F));
                }
            };
            float absorptionModifier = absorption.apply((double) f).floatValue();

            EntityDamageEvent event = CraftEventFactory.handleLivingEntityDamageEvent(this, damagesource, originalDamage, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, hardHat, blocking, armor, resistance, magic, absorption);
            if (event.isCancelled()) {
                return false;
            }

            f = (float) event.getFinalDamage();

            // Apply damage to helmet
            if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && this.getEquipment(4) != null) {
                this.getEquipment(4).damage((int) (event.getDamage() * 4.0F + this.random.nextFloat() * event.getDamage() * 2.0F), this);
            }

            // Apply damage to armor
            if (!damagesource.ignoresArmor()) {
                float armorDamage = (float) (event.getDamage() + event.getDamage(DamageModifier.BLOCKING) + event.getDamage(DamageModifier.HARD_HAT));
                this.damageArmor(armorDamage);
            }

            absorptionModifier = (float) -event.getDamage(DamageModifier.ABSORPTION);
            this.setAbsorptionHearts(Math.max(this.getAbsorptionHearts() - absorptionModifier, 0.0F));
            if (f != 0.0F) {
                if (human) {
                    // PAIL: Be sure to drag all this code from the EntityHuman subclass each update.
                    ((EntityHuman) this).applyExhaustion(damagesource.getExhaustionCost());
                    if (f < 3.4028235E37F) {
                        ((EntityHuman) this).a(StatisticList.x, Math.round(f * 10.0F));
                    }
                }
                // CraftBukkit end
                float f2 = this.getHealth();

                this.setHealth(f2 - f);
                this.bs().a(damagesource, f2, f);
                // CraftBukkit start
                if (human) {
                    return true;
                }
                // CraftBukkit end
                this.setAbsorptionHearts(this.getAbsorptionHearts() - f);
            }
            return true; // CraftBukkit
        }
        return false; // CraftBukkit
    }

    public CombatTracker bs() {
        return this.combatTracker;
    }

    public EntityLiving bt() {
        return this.combatTracker.c() != null ? this.combatTracker.c() : (this.killer != null ? this.killer : (this.lastDamager != null ? this.lastDamager : null));
    }

    public final float getMaxHealth() {
        return (float) this.getAttributeInstance(GenericAttributes.maxHealth).getValue();
    }

    public final int bv() {
        return this.datawatcher.getByte(9);
    }

    public final void o(int i) {
        this.datawatcher.watch(9, Byte.valueOf((byte) i));
    }

    private int n() {
        return this.hasEffect(MobEffectList.FASTER_DIG) ? 6 - (1 + this.getEffect(MobEffectList.FASTER_DIG).getAmplifier()) * 1 : (this.hasEffect(MobEffectList.SLOWER_DIG) ? 6 + (1 + this.getEffect(MobEffectList.SLOWER_DIG).getAmplifier()) * 2 : 6);
    }

    public void bw() {
        if (!this.ar || this.as >= this.n() / 2 || this.as < 0) {
            this.as = -1;
            this.ar = true;
            if (this.world instanceof WorldServer) {
                EntityTracker entityTracker = ((WorldServer) this.world).getTracker();
                if (SettingsConfig.ASYNC_ARM_ANIMATION) {
                    ThreadingManager threadingManager = cSpigot.getInstance().getThreadManager();
                    threadingManager.getDispatcherThread().execute(() -> entityTracker.a(this, new PacketPlayOutAnimation(this, 0)));
                } else {
                    entityTracker.a(this, new PacketPlayOutAnimation(this, 0));
                }
            }
        }

    }

    protected void O() {
        this.damageEntity(DamageSource.OUT_OF_WORLD, 4.0F);
    }

    protected void bx() {
        int i = this.n();

        if (this.ar) {
            ++this.as;
            if (this.as >= i) {
                this.as = 0;
                this.ar = false;
            }
        } else {
            this.as = 0;
        }

        this.az = (float) this.as / (float) i;
    }

    public AttributeInstance getAttributeInstance(IAttribute iattribute) {
        return this.getAttributeMap().a(iattribute);
    }

    public AttributeMapBase getAttributeMap() {
        if (this.c == null) {
            this.c = new AttributeMapServer();
        }

        return this.c;
    }

    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEFINED;
    }

    public abstract ItemStack bA();

    public abstract ItemStack getEquipment(int i);

    public abstract void setEquipment(int i, ItemStack itemstack);

    public boolean canSprintKB = false; // cSpigot - Add a boolean to check when to deal sprint kb
    public void setSprinting(boolean flag) {
        super.setSprinting(flag);
        this.canSprintKB = flag;

        AttributeInstance attributeinstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);

        if (attributeinstance.a(EntityLiving.a) != null) {
            attributeinstance.c(EntityLiving.b);
        }

        if (flag) {
            attributeinstance.b(EntityLiving.b);
        }

    }

    public abstract ItemStack[] getEquipment();

    protected float bB() {
        return 1.0F;
    }

    protected float bC() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean bD() {
        return this.getHealth() <= 0.0F;
    }

    public void q(Entity entity) {
        double d0 = entity.locX;
        double d1 = entity.getBoundingBox().b + (double) entity.length;
        double d2 = entity.locZ;
        byte b0 = 1;

        for ( int i = -b0; i <= b0; ++i ) {
            for ( int j = -b0; j < b0; ++j ) {
                if (i != 0 || j != 0) {
                    int k = (int) (this.locX + (double) i);
                    int l = (int) (this.locZ + (double) j);
                    AxisAlignedBB axisalignedbb = this.getBoundingBox().c((double) i, 1.0D, (double) j);

                    if (this.world.a(axisalignedbb).isEmpty()) {
                        if (World.a((IBlockAccess) this.world, new BlockPosition(k, (int) this.locY, l))) {
                            this.enderTeleportTo(this.locX + (double) i, this.locY + 1.0D, this.locZ + (double) j);
                            return;
                        }

                        if (World.a((IBlockAccess) this.world, new BlockPosition(k, (int) this.locY - 1, l)) || this.world.getType(new BlockPosition(k, (int) this.locY - 1, l)).getBlock().getMaterial() == Material.WATER) {
                            d0 = this.locX + (double) i;
                            d1 = this.locY + 1.0D;
                            d2 = this.locZ + (double) j;
                        }
                    }
                }
            }
        }

        this.enderTeleportTo(d0, d1, d2);
    }

    protected float bE() {
        return 0.42F;
    }

    // cSpigot start - Backport ArrowDMG fix
    protected long lastJumpTime = 0L;
    protected void bF() {
        long time = System.nanoTime();
        boolean canCrit = true;
        if (this instanceof EntityPlayer) {
            canCrit = false;
            if (time - this.lastJumpTime > (long)(0.250e9)) {
                this.lastJumpTime = time;
                canCrit = true;
            }
        }
        // cSpigot end - Backport ArrowDMG fix

        this.motY = (double) this.bE();
        if (this.hasEffect(MobEffectList.JUMP)) {
            this.motY += (double) ((float) (this.getEffect(MobEffectList.JUMP).getAmplifier() + 1) * 0.1F);
        }

        if (canCrit && this.isSprinting()) {
            float f = this.yaw * 0.017453292F;

            this.motX -= (double) (MathHelper.sin(f) * 0.2F);
            this.motZ += (double) (MathHelper.cos(f) * 0.2F);
        }

        this.ai = true;
    }

    protected void bG() {
        this.motY += 0.03999999910593033D;
    }

    protected void bH() {
        this.motY += 0.03999999910593033D;
    }

    public void g(float f, float f1) {
        double d0;
        float f2;

        if (this.bM()) {
            float f3;
            float f4;

            if (this.V() && (!(this instanceof EntityHuman) || !((EntityHuman) this).abilities.isFlying)) {
                d0 = this.locY;
                f3 = 0.8F;
                f4 = 0.02F;
                f2 = (float) EnchantmentManager.b(this);
                if (f2 > 3.0F) {
                    f2 = 3.0F;
                }

                if (!this.onGround) {
                    f2 *= 0.5F;
                }

                if (f2 > 0.0F) {
                    f3 += (0.54600006F - f3) * f2 / 3.0F;
                    f4 += (this.bI() - f4) * f2 / 3.0F;
                }

                this.a(f, f1, f4);
                this.move(this.motX, this.motY, this.motZ);
                this.motX *= f3;
                this.motY *= 0.800000011920929D;
                this.motZ *= f3;
                this.motY -= 0.02D;
                if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + d0, this.motZ)) {
                    this.motY = 0.30000001192092896D;
                }
            } else if (this.ab() && (!(this instanceof EntityHuman) || !((EntityHuman) this).abilities.isFlying)) {
                d0 = this.locY;
                this.a(f, f1, 0.02F);
                this.move(this.motX, this.motY, this.motZ);
                this.motX *= 0.5D;
                this.motY *= 0.5D;
                this.motZ *= 0.5D;
                this.motY -= 0.02D;
                if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + d0, this.motZ)) {
                    this.motY = 0.30000001192092896D;
                }
            } else {
                float f5 = 0.91F;

                if (this.onGround) {
                    f5 = this.world.getType(new BlockPosition(MathHelper.floor(this.locX), MathHelper.floor(this.getBoundingBox().b) - 1, MathHelper.floor(this.locZ))).getBlock().frictionFactor * 0.91F;
                }

                float f6 = 0.16277136F / (f5 * f5 * f5);

                if (this.onGround) {
                    f3 = this.bI() * f6;
                } else {
                    f3 = this.aM;
                }

                this.a(f, f1, f3);
                f5 = 0.91F;
                if (this.onGround) {
                    f5 = this.world.getType(new BlockPosition(MathHelper.floor(this.locX), MathHelper.floor(this.getBoundingBox().b) - 1, MathHelper.floor(this.locZ))).getBlock().frictionFactor * 0.91F;
                }

                if (this.k_()) {
                    f4 = 0.15F;
                    this.motX = MathHelper.a(this.motX, -f4, f4);
                    this.motZ = MathHelper.a(this.motZ, -f4, f4);
                    this.fallDistance = 0.0F;
                    if (this.motY < -0.15D) {
                        this.motY = -0.15D;
                    }

                    boolean flag = this.isSneaking() && this instanceof EntityHuman;

                    if (flag && this.motY < 0.0D) {
                        this.motY = 0.0D;
                    }
                }

                this.move(this.motX, this.motY, this.motZ);
                if (this.positionChanged && this.k_()) {
                    this.motY = 0.2D;
                }

                if (this.world.isClientSide && (!this.world.isLoaded(new BlockPosition((int) this.locX, 0, (int) this.locZ)) || !this.world.getChunkAtWorldCoords(new BlockPosition((int) this.locX, 0, (int) this.locZ)).o())) {
                    if (this.locY > 0.0D) {
                        this.motY = -0.1D;
                    } else {
                        this.motY = 0.0D;
                    }
                } else {
                    this.motY -= 0.08D;
                }

                this.motY *= 0.9800000190734863D;
                this.motX *= f5;
                this.motZ *= f5;
            }
        }

        this.aA = this.aB;
        d0 = this.locX - this.lastX;
        double d1 = this.locZ - this.lastZ;

        f2 = MathHelper.sqrt(d0 * d0 + d1 * d1) * 4.0F;
        if (f2 > 1.0F) {
            f2 = 1.0F;
        }

        this.aB += (f2 - this.aB) * 0.4F;
        this.aC += this.aB;
    }

    public float bI() {
        return this.bm;
    }

    public void k(float f) {
        this.bm = f;
    }

    public boolean r(Entity entity) {
        this.p(entity);
        return false;
    }

    public boolean isSleeping() {
        return false;
    }

    public void t_() {
        super.t_();
        if (!this.world.isClientSide) {
            int i = this.bv();

            if (i > 0) {
                if (this.at <= 0) {
                    this.at = 20 * (30 - i);
                }

                --this.at;
                if (this.at <= 0) {
                    this.o(i - 1);
                }
            }

            for ( int j = 0; j < 5; ++j ) {
                ItemStack itemstack = this.h[j];
                ItemStack itemstack1 = this.getEquipment(j);

                // if (!ItemStack.matches(itemstack1, itemstack)) {
                if (!ItemStack.fastMatches(itemstack1, itemstack)) { // Spigot
                    ((WorldServer) this.world).getTracker().a(this, new PacketPlayOutEntityEquipment(this.getId(), j, itemstack1, SettingsConfig.OBFUSCATE_ARMOR_PACKET));
                    if (itemstack != null) {
                        this.c.a(itemstack.B());
                    }

                    if (itemstack1 != null) {
                        this.c.b(itemstack1.B());
                    }

                    this.h[j] = itemstack1 == null ? null : itemstack1.cloneItemStack();
                }
            }

            if (this.ticksLived % 20 == 0) {
                this.bs().g();
            }
        }

        this.m();
        double d0 = this.locX - this.lastX;
        double d1 = this.locZ - this.lastZ;
        float f = (float) (d0 * d0 + d1 * d1);
        float f1 = this.aI;
        float f2 = 0.0F;

        this.aR = this.aS;
        float f3 = 0.0F;

        if (f > 0.0025000002F) {
            f3 = 1.0F;
            f2 = (float) Math.sqrt((double) f) * 3.0F;
            // CraftBukkit - Math -> TrigMath
            f1 = (float) org.bukkit.craftbukkit.TrigMath.atan2(d1, d0) * 180.0F / 3.1415927F - 90.0F;
        }

        if (this.az > 0.0F) {
            f1 = this.yaw;
        }

        if (!this.onGround) {
            f3 = 0.0F;
        }

        this.aS += (f3 - this.aS) * 0.3F;
        f2 = this.h(f1, f2);

        while (this.yaw - this.lastYaw < -180.0F) {
            this.lastYaw -= 360.0F;
        }

        while (this.yaw - this.lastYaw >= 180.0F) {
            this.lastYaw += 360.0F;
        }

        while (this.aI - this.aJ < -180.0F) {
            this.aJ -= 360.0F;
        }

        while (this.aI - this.aJ >= 180.0F) {
            this.aJ += 360.0F;
        }

        while (this.pitch - this.lastPitch < -180.0F) {
            this.lastPitch -= 360.0F;
        }

        while (this.pitch - this.lastPitch >= 180.0F) {
            this.lastPitch += 360.0F;
        }

        while (this.aK - this.aL < -180.0F) {
            this.aL -= 360.0F;
        }

        while (this.aK - this.aL >= 180.0F) {
            this.aL += 360.0F;
        }

        this.aT += f2;
    }

    protected float h(float f, float f1) {
        float f2 = MathHelper.g(f - this.aI);

        this.aI += f2 * 0.3F;
        float f3 = MathHelper.g(this.yaw - this.aI);
        boolean flag = f3 < -90.0F || f3 >= 90.0F;

        if (f3 < -75.0F) {
            f3 = -75.0F;
        }

        if (f3 >= 75.0F) {
            f3 = 75.0F;
        }

        this.aI = this.yaw - f3;
        if (f3 * f3 > 2500.0F) {
            this.aI += f3 * 0.2F;
        }

        if (flag) {
            f1 *= -1.0F;
        }

        return f1;
    }

    public void m() {
        if (this.bn > 0) {
            --this.bn;
        }

        if (this.bc > 0) {
            double d0 = this.locX + (this.bd - this.locX) / (double) this.bc;
            double d1 = this.locY + (this.be - this.locY) / (double) this.bc;
            double d2 = this.locZ + (this.bf - this.locZ) / (double) this.bc;
            double d3 = MathHelper.g(this.bg - (double) this.yaw);

            this.yaw = (float) ((double) this.yaw + d3 / (double) this.bc);
            this.pitch = (float) ((double) this.pitch + (this.bh - (double) this.pitch) / (double) this.bc);
            --this.bc;
            this.setPosition(d0, d1, d2);
            this.setYawPitch(this.yaw, this.pitch);
        } else if (!this.bM()) {
            this.motX *= 0.98D;
            this.motY *= 0.98D;
            this.motZ *= 0.98D;
        }

        if (Math.abs(this.motX) < 0.005D) {
            this.motX = 0.0D;
        }

        if (Math.abs(this.motY) < 0.005D) {
            this.motY = 0.0D;
        }

        if (Math.abs(this.motZ) < 0.005D) {
            this.motZ = 0.0D;
        }

        if (this.bD()) {
            this.aY = false;
            this.aZ = 0.0F;
            this.ba = 0.0F;
            this.bb = 0.0F;
        } else if (this.bM()) {
            this.doTick();
        }

        if (this.aY) {
            if (this.V()) {
                this.bG();
            } else if (this.ab()) {
                this.bH();
            } else if (this.onGround && this.bn == 0) {
                this.bF();
                this.bn = 10;
            }
        } else {
            this.bn = 0;
        }

        this.aZ *= 0.98F;
        this.ba *= 0.98F;
        this.bb *= 0.9F;
        this.g(this.aZ, this.ba);
        if (!this.world.isClientSide) {
            this.bL();
        }
    }

    protected void doTick() {
    }

    protected void bL() {
        // cSpigot start - Make Entity collisions configurable
        if (SettingsConfig.ENTITY_COLLISIONS) {
            return;
        }
        // cSpigot end

        // IonSpigot start - Optimise Entity Collisions
        List list = this.world.getEntitiesByAmount(this, this.getBoundingBox().grow(0.20000000298023224D, 0.0D, 0.20000000298023224D),
                input -> IEntitySelector.d.apply(input) && input != null && input.ae(), world.spigotConfig.maxCollisionsPerEntity);
        // IonSpigot end

        if (this.ad() && !list.isEmpty()) { // Spigot: Add this.ad() condition
            numCollisions -= world.spigotConfig.maxCollisionsPerEntity; // Spigot
            for ( int i = 0; i < list.size(); ++i ) {
                if (numCollisions > world.spigotConfig.maxCollisionsPerEntity) {
                    break;
                } // Spigot
                Entity entity = (Entity) list.get(i);

                // TODO better check now?
                // CraftBukkit start - Only handle mob (non-player) collisions every other tick
                if (entity instanceof EntityLiving && !(this instanceof EntityPlayer) && this.ticksLived % 2 == 0) {
                    continue;
                }
                // CraftBukkit end

                entity.numCollisions++; // Spigot
                numCollisions++; // Spigot
                this.s(entity);
            }
            numCollisions = 0; // Spigot
        }

    }

    protected void s(Entity entity) {
        entity.collide(this);
    }

    public void mount(Entity entity) {
        if (this.vehicle != null && entity == null) {
            // CraftBukkit start
            Entity originalVehicle = this.vehicle;
            if ((this.bukkitEntity instanceof LivingEntity) && (this.vehicle.getBukkitEntity() instanceof Vehicle)) {
                VehicleExitEvent event = new VehicleExitEvent((Vehicle) this.vehicle.getBukkitEntity(), (LivingEntity) this.bukkitEntity);
                getBukkitEntity().getServer().getPluginManager().callEvent(event);

                if (event.isCancelled() || vehicle != originalVehicle) {
                    return;
                }
            }
            // CraftBukkit end
            // PaperSpigot start - make dismountEvent cancellable
            EntityDismountEvent dismountEvent = new EntityDismountEvent(this.getBukkitEntity(), this.vehicle.getBukkitEntity()); // Spigot
            Bukkit.getPluginManager().callEvent(dismountEvent);
            if (dismountEvent.isCancelled()) return;
            // PaperSpigot end

            if (!this.world.isClientSide) {
                this.q(this.vehicle);
            }

            if (this.vehicle != null) {
                this.vehicle.passenger = null;
            }

            this.vehicle = null;
        } else {
            super.mount(entity);
        }
    }

    public void ak() {
        super.ak();
        this.aR = this.aS;
        this.aS = 0.0F;
        this.fallDistance = 0.0F;
    }

    public void i(boolean flag) {
        this.aY = flag;
    }

    public void receive(Entity entity, int i) {
        if (!entity.dead && !this.world.isClientSide) {
            EntityTracker entitytracker = ((WorldServer) this.world).getTracker();

            if (entity instanceof EntityItem) {
                entitytracker.a(entity, (new PacketPlayOutCollect(entity.getId(), this.getId())));
            }

            if (entity instanceof EntityArrow) {
                entitytracker.a(entity, (new PacketPlayOutCollect(entity.getId(), this.getId())));
            }

            if (entity instanceof EntityExperienceOrb) {
                entitytracker.a(entity, (new PacketPlayOutCollect(entity.getId(), this.getId())));
            }
        }

    }

    public boolean hasLineOfSight(Entity entity) {
        Vec3D vec = new Vec3D(this.locX, this.locY + (double) this.getHeadHeight(), this.locZ);
        if (entity instanceof EntityPlayer) {
            // Head height is 1,5725
            // Split it into three to get a more accurate line of sight -> 0.52416667

            double parts = entity.getHeadHeight() / 3;

            return this.world.rayTrace(
                    vec,
                    new Vec3D(entity.locX, entity.locY + (parts * 3), entity.locZ)
            ) == null || this.world.rayTrace(
                    vec,
                    new Vec3D(entity.locX, entity.locY + (parts * 2), entity.locZ)
            ) == null || this.world.rayTrace(
                    vec,
                    new Vec3D(entity.locX, entity.locY + (parts * 1), entity.locZ)
            ) == null;
        } else {
            return this.world.rayTrace(
                    vec,
                    new Vec3D(entity.locX, entity.locY + (double) this.getHeadHeight(), entity.locZ)
            ) == null;
        }
    }

    public Vec3D ap() {
        return this.d(1.0F);
    }

    public Vec3D d(float f) {
        // cSpigot start - Use the original yaw that's tracked better, reduces mouse delay
        if (SettingsConfig.OPTIMIZED_YAW_PITCH_TRACK && this instanceof EntityPlayer) {
            return super.d(f);
        }
        // cSpigot end
        if (f == 1.0F) {
            return this.f(this.pitch, this.aK);
        } else {
            float f1 = this.lastPitch + (this.pitch - this.lastPitch) * f;
            float f2 = this.aL + (this.aK - this.aL) * f;

            return this.f(f1, f2);
        }
    }

    public boolean bM() {
        return !this.world.isClientSide;
    }

    public boolean ad() {
        return !this.dead;
    }

    public boolean ae() {
        return !this.dead;
    }

    protected void ac() {
        this.velocityChanged = this.random.nextDouble() >= this.getAttributeInstance(GenericAttributes.c).getValue();
    }

    public float getHeadRotation() {
        return this.aK;
    }

    public void setHeadRotation(float f) {
        this.f(f);
    }

    public void f(float f) {
        this.aK = f;
    }

    public void g(float f) {
        this.aI = f;
    }

    public float getAbsorptionHearts() {
        return this.bo;
    }

    public void setAbsorptionHearts(float f) {
        if (f < 0.0F) {
            f = 0.0F;
        }

        this.bo = f;
    }

    public ScoreboardTeamBase getScoreboardTeam() {
        return this.world.getScoreboard().getPlayerTeam(this.getUniqueID().toString());
    }

    public boolean c(EntityLiving entityliving) {
        return this.a(entityliving.getScoreboardTeam());
    }

    public boolean a(ScoreboardTeamBase scoreboardteambase) {
        return this.getScoreboardTeam() != null ? this.getScoreboardTeam().isAlly(scoreboardteambase) : false;
    }

    public void enterCombat() {
    }

    public void exitCombat() {
    }

    protected void bP() {
        this.updateEffects = true;
    }
}
