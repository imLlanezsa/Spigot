package io.github.cspigot.feature.optimize;

import com.google.common.collect.Lists;
import lombok.val;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import io.github.cspigot.config.SettingsConfig;

import java.util.Iterator;
import java.util.List;

public class FastExplosion extends Explosion {
    public FastExplosion(World world, Entity entity, double d0, double d1, double d2, float f, boolean flag, boolean flag1) {
        super(world, entity, d0, d1, d2, f, flag, flag1);
    }

    @Override
    public void a(boolean flag) {
        // PaperSpigot start - Configurable TNT explosion volume.
        float volume = source instanceof EntityTNTPrimed ? world.paperSpigotConfig.tntExplosionVolume : 4.0F;

        if (SettingsConfig.EXPLODE_SOUND) {
            this.world.makeSound(this.posX, this.posY, this.posZ, "random.explode", volume, (1.0F + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2F) * 0.7F);
        }
        // PaperSpigot end

        if (SettingsConfig.TNT_SHOW_PARTICLES) {
            if (this.size >= 2.0F && this.b) {
                this.world.addParticle(EnumParticle.EXPLOSION_HUGE, this.posX, this.posY, this.posZ, 1.0D, 0.0D, 0.0D);
            } else {
                this.world.addParticle(EnumParticle.EXPLOSION_LARGE, this.posX, this.posY, this.posZ, 1.0D, 0.0D, 0.0D);
            }
        }

        Iterator<BlockPosition> iterator;
        BlockPosition blockposition;

        if (this.b) {
            // CraftBukkit start
            org.bukkit.World bworld = this.world.getWorld();
            org.bukkit.entity.Entity explode = this.source == null ? null : this.source.getBukkitEntity();
            Location location = new Location(bworld, this.posX, this.posY, this.posZ);

            List<org.bukkit.block.Block> blockList = Lists.newArrayList();
            for (int i1 = this.blocks.size() - 1; i1 >= 0; i1--) {
                BlockPosition cpos = this.blocks.get(i1);
                org.bukkit.block.Block bblock = bworld.getBlockAt(cpos.getX(), cpos.getY(), cpos.getZ());
                if (bblock.getType() != org.bukkit.Material.AIR) {
                    blockList.add(bblock);
                }
            }

            boolean cancelled;
            List<org.bukkit.block.Block> bukkitBlocks;
            float yield;

            if (explode != null) {
                EntityExplodeEvent event = new EntityExplodeEvent(explode, location, blockList, 0.3F);
                this.world.getServer().getPluginManager().callEvent(event);

                cancelled = event.isCancelled();
                bukkitBlocks = event.blockList();
                yield = event.getYield();
            } else {
                BlockExplodeEvent event = new BlockExplodeEvent(location.getBlock(), blockList, 0.3F);
                this.world.getServer().getPluginManager().callEvent(event);

                cancelled = event.isCancelled();
                bukkitBlocks = event.blockList();
                yield = event.getYield();
            }

            this.blocks.clear();

            for ( org.bukkit.block.Block bblock : bukkitBlocks ) {
                BlockPosition coords = new BlockPosition(bblock.getX(), bblock.getY(), bblock.getZ());
                blocks.add(coords);
            }

            if (cancelled) {
                this.wasCanceled = true;
                return;
            }
            // CraftBukkit end
            iterator = this.blocks.iterator();

            while (iterator.hasNext()) {
                blockposition = iterator.next();
                val blockData = this.world.getTypeIfLoaded(blockposition);
                if (blockData == null) continue;
                val block = blockData.getBlock();

                if (flag && SettingsConfig.TNT_SHOW_PARTICLES) {
                    double d0 = ((float) blockposition.getX() + this.world.random.nextFloat());
                    double d1 = ((float) blockposition.getY() + this.world.random.nextFloat());
                    double d2 = ((float) blockposition.getZ() + this.world.random.nextFloat());
                    double d3 = d0 - this.posX;
                    double d4 = d1 - this.posY;
                    double d5 = d2 - this.posZ;
                    double d6 = MathHelper.sqrt(d3 * d3 + d4 * d4 + d5 * d5);

                    d3 /= d6;
                    d4 /= d6;
                    d5 /= d6;
                    double d7 = 0.5D / (d6 / (double) this.size + 0.1D);

                    d7 *= this.world.random.nextFloat() * this.world.random.nextFloat() + 0.3F;
                    d3 *= d7;
                    d4 *= d7;
                    d5 *= d7;

                    this.world.addParticle(EnumParticle.EXPLOSION_NORMAL, (d0 + this.posX) / 2.0D, (d1 + this.posY) / 2.0D, (d2 + this.posZ) / 2.0D, d3, d4, d5);
                    this.world.addParticle(EnumParticle.SMOKE_NORMAL, d0, d1, d2, d3, d4, d5);
                }


                if (block.getMaterial() != Material.AIR) {
                    if (block.a(this)) {
                        // CraftBukkit - add yield
                        block.dropNaturally(this.world, blockposition, this.world.getType(blockposition), yield, 0);
                    }

                    this.world.setTypeAndData(blockposition, Blocks.AIR.getBlockData(), 3);
                    block.wasExploded(this.world, blockposition, this);

                    world.spigotConfig.antiXrayInstance.updateNearbyBlocks(world, blockposition); // Spigot
                }
            }
        }

        if (this.a) {
            iterator = this.blocks.iterator();

            while (iterator.hasNext()) {
                blockposition = iterator.next();
                if (this.world.getType(blockposition).getBlock().getMaterial() == Material.AIR && this.world.getType(blockposition.down()).getBlock().o() && this.world.random.nextInt(3) == 0) {
                    // CraftBukkit start - Ignition by explosion
                    if (!org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(this.world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), this).isCancelled()) {
                        this.world.setTypeUpdate(blockposition, Blocks.FIRE.getBlockData());
                    }
                    // CraftBukkit end
                }
            }
        }

    }
}
