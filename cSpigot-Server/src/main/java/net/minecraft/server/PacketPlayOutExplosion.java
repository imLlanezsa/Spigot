package net.minecraft.server;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Packet sent from server to client when an explosion occurs
 */
public class PacketPlayOutExplosion implements Packet<PacketListenerPlayOut> {

    private static final int MAX_BLOCKS = 4096; // Reasonable limit for affected blocks

    // Position coordinates of the explosion
    private double posX;
    private double posY;
    private double posZ;

    /**
     * -- GETTER --
     *  Gets the power/radius of the explosion
     *
     * @return The explosion power
     */
    // Power/radius of the explosion
    @Getter
    private float power;

    // List of affected block positions
    private List<BlockPosition> affectedBlocks;

    /**
     * -- GETTER --
     *  Gets the X component of player motion from the explosion
     *
     * @return The X motion component
     */
    // Player motion vector from the explosion
    @Getter
    private float motionX;
    /**
     * -- GETTER --
     *  Gets the Y component of player motion from the explosion
     *
     * @return The Y motion component
     */
    @Getter
    private float motionY;
    /**
     * -- GETTER --
     *  Gets the Z component of player motion from the explosion
     *
     * @return The Z motion component
     */
    @Getter
    private float motionZ;

    /**
     * Default constructor for deserialization
     */
    public PacketPlayOutExplosion() {
        this.affectedBlocks = Collections.emptyList();
    }

    /**
     * Create a new explosion packet
     *
     * @param x Position X coordinate
     * @param y Position Y coordinate
     * @param z Position Z coordinate
     * @param power Explosion power/radius
     * @param affectedBlocks List of affected block positions
     * @param playerMotion Motion vector to apply to the player, can be null
     */
    public PacketPlayOutExplosion(double x, double y, double z, float power, List<BlockPosition> affectedBlocks, Vec3D playerMotion) {
        // Validate parameters
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.power = Math.max(0.0F, power); // Ensure non-negative power

        // Create a defensive copy of the affected blocks list
        this.affectedBlocks = affectedBlocks != null ?
                Lists.newArrayList(affectedBlocks) : Collections.emptyList();

        // Limit affected blocks to prevent excessive packets
        if (this.affectedBlocks.size() > MAX_BLOCKS) {
            this.affectedBlocks = this.affectedBlocks.subList(0, MAX_BLOCKS);
        }

        // Set player motion if provided
        if (playerMotion != null) {
            this.motionX = (float) playerMotion.a;
            this.motionY = (float) playerMotion.b;
            this.motionZ = (float) playerMotion.c;
        } else {
            this.motionX = 0.0F;
            this.motionY = 0.0F;
            this.motionZ = 0.0F;
        }
    }

    /**
     * Deserializes the packet from raw bytes
     *
     * @param packetDataSerializer The serializer to read from
     * @throws IOException If there's an error reading the data
     */
    public void a(PacketDataSerializer packetDataSerializer) throws IOException {
        try {
            this.posX = (double) packetDataSerializer.readFloat();
            this.posY = (double) packetDataSerializer.readFloat();
            this.posZ = (double) packetDataSerializer.readFloat();
            this.power = packetDataSerializer.readFloat();

            int blockCount = packetDataSerializer.readInt();

            // Validate block count to prevent overallocation
            blockCount = Math.min(blockCount, MAX_BLOCKS);

            this.affectedBlocks = Lists.newArrayListWithCapacity(blockCount);
            int baseX = (int) this.posX;
            int baseY = (int) this.posY;
            int baseZ = (int) this.posZ;

            for (int i = 0; i < blockCount; ++i) {
                int offsetX = packetDataSerializer.readByte() + baseX;
                int offsetY = packetDataSerializer.readByte() + baseY;
                int offsetZ = packetDataSerializer.readByte() + baseZ;

                this.affectedBlocks.add(new BlockPosition(offsetX, offsetY, offsetZ));
            }

            this.motionX = packetDataSerializer.readFloat();
            this.motionY = packetDataSerializer.readFloat();
            this.motionZ = packetDataSerializer.readFloat();
        } catch (Exception e) {
            // Reset to default values if deserialization fails
            this.posX = 0;
            this.posY = 0;
            this.posZ = 0;
            this.power = 0;
            this.affectedBlocks = Collections.emptyList();
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;

            // Rethrow as IOException to maintain API compatibility
            throw new IOException("Failed to deserialize explosion packet", e);
        }
    }

    /**
     * Serializes the packet to raw bytes
     *
     * @param packetDataSerializer The serializer to write to
     * @throws IOException If there's an error writing the data
     */
    public void b(PacketDataSerializer packetDataSerializer) throws IOException {
        try {
            packetDataSerializer.writeFloat((float) this.posX);
            packetDataSerializer.writeFloat((float) this.posY);
            packetDataSerializer.writeFloat((float) this.posZ);
            packetDataSerializer.writeFloat(this.power);

            // Write block count, ensuring it doesn't exceed our maximum
            int blockCount = Math.min(this.affectedBlocks.size(), MAX_BLOCKS);
            packetDataSerializer.writeInt(blockCount);

            int baseX = (int) this.posX;
            int baseY = (int) this.posY;
            int baseZ = (int) this.posZ;

            int count = 0;
            for (BlockPosition blockPosition : this.affectedBlocks) {
                if (count++ >= blockCount) break;

                if (blockPosition == null) continue;

                int offsetX = blockPosition.getX() - baseX;
                int offsetY = blockPosition.getY() - baseY;
                int offsetZ = blockPosition.getZ() - baseZ;

                // Ensure values are within byte range (-128 to 127)
                packetDataSerializer.writeByte(Math.max(-128, Math.min(127, offsetX)));
                packetDataSerializer.writeByte(Math.max(-128, Math.min(127, offsetY)));
                packetDataSerializer.writeByte(Math.max(-128, Math.min(127, offsetZ)));
            }

            packetDataSerializer.writeFloat(this.motionX);
            packetDataSerializer.writeFloat(this.motionY);
            packetDataSerializer.writeFloat(this.motionZ);
        } catch (Exception e) {
            throw new IOException("Failed to serialize explosion packet", e);
        }
    }

    /**
     * Processes this packet with the specified listener
     *
     * @param packetListener The play-out packet listener
     */
    public void a(PacketListenerPlayOut packetListener) {
        Objects.requireNonNull(packetListener, "Packet listener cannot be null");
        packetListener.a(this);
    }

    /**
     * Gets the X coordinate of the explosion
     * @return The X coordinate
     */
    public double getX() {
        return this.posX;
    }

    /**
     * Gets the Y coordinate of the explosion
     * @return The Y coordinate
     */
    public double getY() {
        return this.posY;
    }

    /**
     * Gets the Z coordinate of the explosion
     * @return The Z coordinate
     */
    public double getZ() {
        return this.posZ;
    }

    /**
     * -- GETTER --
     *  Gets the list of affected block positions
     *
     * @return Unmodifiable list of affected block positions
     */
    public List<BlockPosition> getAffectedBlocks() {
        return Collections.unmodifiableList(affectedBlocks);
    }

}