package net.minecraft.server;

import io.netty.util.internal.ThreadLocalRandom;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttributeModifier {

    private static Logger LOGGER = LogManager.getLogger();

    private final double a;
    private final int b;
    private final String c;
    private final UUID d;
    private boolean e;

    public AttributeModifier(String s, double d0, int i) {
        this(MathHelper.a((Random) ThreadLocalRandom.current()), s, d0, i);
    }

    public AttributeModifier(UUID uuid, String s, double d0, int i) {
        this.e = true;
        this.d = uuid;
        this.c = s;
        this.a = d0;
        this.b = i;
        Validate.notEmpty(s, "Modifier name cannot be empty", new Object[0]);
    }

    public UUID a() {
        return this.d;
    }

    public String b() {
        return this.c;
    }

    public int c() {
        return this.b;
    }

    public double d() {
        return this.a;
    }

    public boolean e() {
        return this.e;
    }

    public AttributeModifier a(boolean flag) {
        this.e = flag;
        return this;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            AttributeModifier attributemodifier = (AttributeModifier) object;

            if (this.d != null) {
                if (!this.d.equals(attributemodifier.d)) {
                    return false;
                }
            } else if (attributemodifier.d != null) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.d != null ? this.d.hashCode() : 0;
    }

    public String toString() {
        return "AttributeModifier{amount=" + this.a + ", operation=" + this.b + ", name=\'" + this.c + '\'' + ", id=" + this.d + ", serialize=" + this.e + '}';
    }

    public void b(AttributeRanged ranged, Object object) {
    }
}