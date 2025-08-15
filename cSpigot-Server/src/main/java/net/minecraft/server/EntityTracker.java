package net.minecraft.server;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.extern.log4j.Log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.Nullable;

import io.github.cspigot.config.SettingsConfig;
import io.github.cspigot.utils.collection.IndexedLinkedSet;

@Log4j2
public class EntityTracker {

    protected static final Logger a = LogManager.getLogger();
    public IntHashMap<EntityTrackerEntry> trackedEntities;

    protected Set<EntityTrackerEntry> c; // Tracking entities
    protected int e; // Tracking Range

    // cSpigot - Used for Async Entity tracker
    public EntityTracker() {
    }
    // cSpigot end

    public EntityTracker(WorldServer worldserver) {
        this.c = new IndexedLinkedSet<>();
        this.trackedEntities = new IntHashMap<>();
        this.e = PlayerChunkMap.getFurthestViewableBlock(worldserver.spigotConfig.viewDistance);
    }

    public void track(Entity entity) {
        track(entity, true);
    }
    public EntityTrackerEntry track(Entity entity, boolean addToList) {
        if (entity instanceof EntityPlayer) {
            return this.addEntity(entity, 512, 2, false, addToList);
        } else if (entity instanceof EntityFishingHook) {
            return this.addEntity(entity, 64, 5, true, addToList);
        } else if (entity instanceof EntityArrow) {
            return this.addEntity(entity, 64, 20, false, addToList);
        } else if (entity instanceof EntitySmallFireball) {
            return this.addEntity(entity, 64, 10, false, addToList);
        } else if (entity instanceof EntityFireball) {
            return this.addEntity(entity, 64, 10, false, addToList);
        } else if (entity instanceof EntitySnowball) {
            return this.addEntity(entity, 64, 10, true, addToList);
        } else if (entity instanceof EntityEnderPearl) {
            return this.addEntity(entity, 64, 10, true, addToList);
        } else if (entity instanceof EntityEnderSignal) {
            return this.addEntity(entity, 64, 4, true, addToList);
        } else if (entity instanceof EntityEgg) {
            return this.addEntity(entity, 64, 10, true, addToList);
        } else if (entity instanceof EntityPotion) {
            return this.addEntity(entity, 64, 10, true, addToList);
        } else if (entity instanceof EntityThrownExpBottle) {
            return this.addEntity(entity, 64, 10, true, addToList);
        } else if (entity instanceof EntityFireworks) {
            return this.addEntity(entity, 64, 10, true, addToList);
        } else if (entity instanceof EntityItem) {
            return this.addEntity(entity, 64, 20, true, addToList);
        } else if (entity instanceof EntityMinecartAbstract) {
            return this.addEntity(entity, 80, 3, true, addToList);
        } else if (entity instanceof EntityBoat) {
            return this.addEntity(entity, 80, 3, true, addToList);
        } else if (entity instanceof EntitySquid) {
            return this.addEntity(entity, 64, 3, true, addToList);
        } else if (entity instanceof EntityWither) {
            return this.addEntity(entity, 80, 3, false, addToList);
        } else if (entity instanceof EntityBat) {
            return this.addEntity(entity, 80, 3, false, addToList);
        } else if (entity instanceof EntityEnderDragon) {
            return this.addEntity(entity, 160, 3, true, addToList);
        } else if (entity instanceof IAnimal) {
            return this.addEntity(entity, 80, 3, true, addToList);
        } else if (entity instanceof EntityTNTPrimed) {
            return this.addEntity(entity, 160, 10, true, addToList);
        } else if (entity instanceof EntityFallingBlock) {
            return this.addEntity(entity, 160, 20, true, addToList);
        } else if (entity instanceof EntityHanging) {
            return this.addEntity(entity, 160, Integer.MAX_VALUE, false, addToList);
        } else if (entity instanceof EntityArmorStand) {
            return this.addEntity(entity, 160, 3, true, addToList);
        } else if (entity instanceof EntityExperienceOrb) {
            return this.addEntity(entity, 160, 20, true, addToList);
        } else if (entity instanceof EntityEnderCrystal) {
            return this.addEntity(entity, 256, Integer.MAX_VALUE, false, addToList);
        }

        return null;
    }

    public void addEntity(Entity entity, int i, int j) {
        this.addEntity(entity, i, j, false);
    }

    public void addEntity(Entity entity, int i, final int j, boolean flag) {
        addEntity(entity, i, j, flag, true);
    }
    public EntityTrackerEntry addEntity(Entity entity, int i, final int j, boolean flag, boolean addToList) {
        // org.spigotmc.AsyncCatcher.catchOp( "entity track"); // Spigot

        @Nullable EntityTrackerEntry entry = null;

        // cSpigot start - Allow hiding of TNT & sand
        if (SettingsConfig.HIDE_SAND && entity instanceof EntityFallingBlock
            || SettingsConfig.HIDE_TNT && entity instanceof EntityTNTPrimed) {
            return entry;
        }
        // cSpigot end

        i = org.spigotmc.TrackingRange.getEntityTrackingRange(entity, i); // Spigot
        if (i > this.e) {
            i = this.e;
        }

        try {
            if (this.trackedEntities.b(entity.getId())) {
                log.error("[cSpigot-Normal] Entity is already tracked, please report this to Drizzy.(ID: {})", entity.getId());
                EntityTrackerEntry entityTrackerEntry = this.trackedEntities.d(entity.getId());
                entityTrackerEntry.addNearPlayers();
                return null;
            }

            EntityTrackerEntry entityTrackerEntry = new EntityTrackerEntry(entity, i, j, flag);
            if (addToList) {
                this.c.add(entityTrackerEntry);
            } else if (entity instanceof EntityTNTPrimed && SettingsConfig.BATCH_DISPENSERS) {
                entry = entityTrackerEntry;
            }
            this.trackedEntities.a(entity.getId(), entityTrackerEntry);
            entityTrackerEntry.addNearPlayers();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Adding entity to track");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Entity To Track");

            crashreportsystemdetails.a("Tracking range", (Object) (i + " blocks"));
            final int finalI = i; // CraftBukkit - fix decompile error
            crashreportsystemdetails.a("Update interval", new Callable() {
                public String a() throws Exception {
                    String s = "Once per " + finalI + " ticks"; // CraftBukkit

                    if (finalI == Integer.MAX_VALUE) { // CraftBukkit
                        s = "Maximum (" + s + ")";
                    }

                    return s;
                }

                public Object call() throws Exception {
                    return this.a();
                }
            });
            entity.appendEntityCrashDetails(crashreportsystemdetails);
            CrashReportSystemDetails crashreportsystemdetails1 = crashreport.a("Entity That Is Likely Already Tracked");

            final @Nullable EntityTrackerEntry entityCause = this.trackedEntities.get(entity.getId());
            if (entityCause != null) {
                entityCause.tracker.appendEntityCrashDetails(crashreportsystemdetails1);
            }

            try {
                throw new ReportedException(crashreport);
            } catch (ReportedException reportedexception) {
                EntityTracker.a.error("\"Silently\" catching entity tracking error.", reportedexception);
            }
        }

        return entry;
    }

    public void untrackEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) entity;

            // DO NOT MIGRATE THIS FROM AN ITERATOR
            // THIS NEEDS TO STAY AS AN ITERATOR FOREVER
            Iterator iterator = this.c.iterator();
            while (iterator.hasNext()) {
                EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry) iterator.next();
                entitytrackerentry.a(entityplayer);
            }
        }

        EntityTrackerEntry entry = this.trackedEntities.d(entity.getId());
        if (entry != null) {
            this.c.remove(entry);
            entry.a();
        }

    }

    public void updatePlayers() {
        // cSpigot start - Async entity tracker
        for (EntityTrackerEntry entry : c) {
            if (entry.isActive()) {
                entry.update();
            }
        }
        // cSpigot end
    }

    public void updatePlayer(EntityPlayer entityplayer) { a(entityplayer); } // Paper - OBFHELPER
    public void a(EntityPlayer entityplayer) {
        Iterator iterator = this.c.iterator();

        while (iterator.hasNext()) {
            EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry) iterator.next();

            if (entitytrackerentry.tracker == entityplayer) {
                entitytrackerentry.addNearPlayers();
            } else {
                entitytrackerentry.updatePlayer(entityplayer);
            }
        }

    }

    // PAIL: broadcastEntityPacket
    public void a(Entity entity, Packet packet) {
        EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry) this.trackedEntities.get(entity.getId());

        if (entitytrackerentry != null) {
            entitytrackerentry.broadcast(packet);
        }

    }

    public void sendPacketToEntity(Entity entity, Packet packet) {
        EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry) this.trackedEntities.get(entity.getId());

        if (entitytrackerentry != null) {
            entitytrackerentry.broadcastIncludingSelf(packet);
        }

    }

    public void untrackPlayer(EntityPlayer entityplayer) {
        Iterator iterator = this.c.iterator();

        while (iterator.hasNext()) {
            EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry) iterator.next();

            entitytrackerentry.clear(entityplayer);
        }

    }

    public void a(EntityPlayer entityplayer, Chunk chunk) {
        Iterator iterator = this.c.iterator();

        while (iterator.hasNext()) {
            EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry) iterator.next();

            if (entitytrackerentry.tracker != entityplayer && entitytrackerentry.tracker.ae == chunk.locX && entitytrackerentry.tracker.ag == chunk.locZ) {
                entitytrackerentry.updatePlayer(entityplayer);
            }
        }
    }

    public Iterator<EntityTrackerEntry> getEntityTrackerEntries() {
        return this.c.iterator();
    }
}
