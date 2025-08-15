package net.minecraft.server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AttributeMapServer extends AttributeMapBase {

    private final Set<AttributeInstance> e = ConcurrentHashMap.newKeySet();
    protected final Map<String, AttributeInstance> d = new InsensitiveStringMap<>(new LinkedHashMap<>());
    private boolean needsUpdate = false;

    public AttributeMapServer() {
    }

    public AttributeModifiable e(IAttribute iattribute) {
        return (AttributeModifiable) super.a(iattribute);
    }

    public AttributeModifiable b(String s) {
        AttributeInstance attributeinstance = super.a(s);

        if (attributeinstance == null) {
            attributeinstance = this.d.get(s);
        }

        return (AttributeModifiable) attributeinstance;
    }

    public AttributeInstance b(IAttribute iattribute) {
        AttributeInstance attributeinstance = super.b(iattribute);

        if (iattribute instanceof AttributeRanged && ((AttributeRanged) iattribute).g() != null) {
            this.d.put(((AttributeRanged) iattribute).g(), attributeinstance);
        }

        return attributeinstance;
    }

    protected AttributeInstance c(IAttribute iattribute) {
        return new AttributeModifiable(this, iattribute);
    }

    public void a(AttributeInstance attributeinstance) {
        if (attributeinstance.getAttribute().c()) {
            this.e.add(attributeinstance);
            this.needsUpdate = true;
        }

        synchronized (this.c) {
            for (IAttribute iattribute : this.c.get(attributeinstance.getAttribute())) {
                AttributeModifiable attributemodifiable = this.e(iattribute);

                if (attributemodifiable != null) {
                    attributemodifiable.f();
                    attributemodifiable.setNeedsUpdate(true);
                }
            }
        }

    }

    public Set<AttributeInstance> getAttributes() {
        return this.e;
    }

    public Collection<AttributeInstance> c() {
        ConcurrentLinkedDeque<AttributeInstance> hashset = new ConcurrentLinkedDeque<>();

        for (AttributeInstance attributeinstance : this.a()) {
            if (attributeinstance.getAttribute().c()) {
                hashset.add(attributeinstance);
            }
        }

        return hashset;
    }

    public AttributeInstance a(String s) {
        return this.b(s);
    }

    public AttributeInstance a(IAttribute iattribute) {
        return this.e(iattribute);
    }

    static {
        //new TamperCheck(null, "Base",0).e();
    }
}
