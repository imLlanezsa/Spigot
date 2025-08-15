package net.minecraft.server;

/**
 * This Project is property of Refine Development.
 * Copyright © 2023, All Rights Reserved.
 * Redistribution of this Project is not allowed.
 *
 * @author mathssw , Sakiio_
 * @since 8/24/2023
 * @version cSpigot
 */
public class ImmutableBlockPosition extends BlockPosition {
    private final int hashCode;

    public ImmutableBlockPosition(int a, int b, int c) {
        super(a, b, c);
        this.hashCode = (b + c * 31) * 31 + a;
    }

    public ImmutableBlockPosition(double a, double b, double c) {
        super(a, b, c);
        this.hashCode = (this.getY() + this.getZ() * 31) * 31 + this.getX();
    }

    public ImmutableBlockPosition(BlockPosition blockPosition) {
        super(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
        this.hashCode = (this.getY() + this.getZ() * 31) * 31 + this.getX();
    }

    public int hashCode() {
        return this.hashCode;
    }
}

