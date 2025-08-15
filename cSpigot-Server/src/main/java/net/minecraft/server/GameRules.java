package net.minecraft.server;

import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;

import java.util.*;
import java.util.Map.Entry;

public class GameRules {

    // Paper start - Optimize GameRules
    private static final int RULES_SIZE = 256;

    private static <K, V> java.util.LinkedHashMap<K, V> linkedMapOf(final TreeMap<K, V> map) {
        final java.util.LinkedHashMap<K, V> ret = new java.util.LinkedHashMap<>(GameRules.RULES_SIZE);
        ret.putAll(map);
        return ret;
    }

    private static final java.util.LinkedHashMap<String, GameRuleDefinition> a = GameRules.linkedMapOf(SystemUtils.a(new TreeMap<>(), (treemap) -> { // Paper - decompile fix
        // Paper end
        treemap.put("doFireTick", new GameRuleDefinition("true", "doFireTick", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("mobGriefing", new GameRuleDefinition("true", "mobGriefing", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("keepInventory", new GameRuleDefinition("false","keepInventory", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("doMobSpawning", new GameRuleDefinition("true","doMobSpawning", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("doMobLoot", new GameRuleDefinition("true", "doMobLoot", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("doTileDrops", new GameRuleDefinition("true","doTileDrops", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("doEntityDrops", new GameRuleDefinition("true","doEntityDrops", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("commandBlockOutput", new GameRuleDefinition("true","commandBlockOutput", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("naturalRegeneration", new GameRuleDefinition("true","naturalRegeneration", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("doDaylightCycle", new GameRuleDefinition("true","doDaylightCycle", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("logAdminCommands", new GameRuleDefinition("true","logAdminCommands", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("showDeathMessages", new GameRuleDefinition("true","showDeathMessages", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("randomTickSpeed", new GameRuleDefinition("3","randomTickSpeed", EnumGameRuleType.NUMERICAL_VALUE));
        treemap.put("sendCommandFeedback", new GameRuleDefinition("true", "sendCommandFeedback", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("reducedDebugInfo", new GameRuleDefinition("false", "reducedDebugInfo", EnumGameRuleType.BOOLEAN_VALUE));
        treemap.put("doWeatherCycle", new GameRuleDefinition("true", "doWeatherCycle", EnumGameRuleType.BOOLEAN_VALUE)); // cSpigot - Add Weather cycle
    })); // Paper - Optimize GameRules

    private final Object2ObjectMap<String, GameRuleValue> b = new Object2ObjectOpenHashMap<>(RULES_SIZE);

    public GameRules() {
        for ( Entry<String, GameRuleDefinition> stringGameRuleDefinitionEntry : GameRules.a.entrySet() ) {
            this.b.put(stringGameRuleDefinitionEntry.getKey(), stringGameRuleDefinitionEntry.getValue().a());
        }
    }

    public void set(String s, String s1) {
        GameRuleValue gamerules_gamerulevalue = this.b.get(s);

        if (gamerules_gamerulevalue != null) {
            gamerules_gamerulevalue.a(s1);
        }

    }

    public boolean getBoolean(String s) {
        GameRuleValue gamerules_gamerulevalue = this.b.get(s);

        return gamerules_gamerulevalue != null && gamerules_gamerulevalue.b();
    }

    public int c(String s) {
        GameRuleValue gamerules_gamerulevalue = this.b.get(s);

        return gamerules_gamerulevalue != null ? gamerules_gamerulevalue.c() : 0;
    }

    public NBTTagCompound a() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        for ( String s : this.b.keySet() ) {
            GameRuleValue gamerules_gamerulevalue = this.b.get(s);
            nbttagcompound.setString(s, gamerules_gamerulevalue.a());
        }
        return nbttagcompound;
    }

    public void a(NBTTagCompound nbttagcompound) {
        Set<String> set = nbttagcompound.c();
        for ( String s : set ) {
            this.set(s, nbttagcompound.getString(s));
        }
    }

    public String get(String s) {
        GameRuleValue var2 = this.b.get(s);
        return var2 != null ? var2.a() : "";
    }

    public boolean a(String var1, EnumGameRuleType var2) {
        GameRuleValue var3 = this.b.get(var1);
        return var3 != null && (var3.getType() == var2 || var2 == GameRules.EnumGameRuleType.ANY_VALUE);
    }

    public boolean contains(String s) {
        return this.b.containsKey(s);
    }

    public static java.util.LinkedHashMap<String, GameRuleDefinition> getAllGameRules() { // Paper - Optimize GameRules
        return GameRules.a;
    }

    public String[] getGameRules() {
        Set<String> var1 = a.keySet();
        return var1.toArray(new String[0]);
    }

    public enum EnumGameRuleType {
        ANY_VALUE,
        BOOLEAN_VALUE,
        NUMERICAL_VALUE;

        private EnumGameRuleType() {
        }
    }

    public static class GameRuleValue {

        private String a;
        private boolean b;
        private int c;
        private double d;
        private final EnumGameRuleType e;

        public GameRuleValue(String s, EnumGameRuleType gamerules_enumgameruletype) {
            this.e = gamerules_enumgameruletype;
            this.a(s);
        }

        public void a(String s) {
            this.a = s;
            this.b = Boolean.parseBoolean(s);
            this.c = this.b ? 1 : 0;

            try {
                this.c = Integer.parseInt(s);
            } catch (NumberFormatException numberformatexception) {
                ;
            }

            try {
                this.d = Double.parseDouble(s);
            } catch (NumberFormatException numberformatexception1) {
                ;
            }
        }

        public String a() {
            return this.a;
        }

        public boolean b() {
            return this.b;
        }

        public int c() {
            return this.c;
        }

        public EnumGameRuleType getType() {
            return this.e;
        }
    }

    public static class GameRuleDefinition {

        @Getter private final String name;
        private final EnumGameRuleType a;
        private final String b;

        public GameRuleDefinition(String s, String name, EnumGameRuleType gamerules_enumgameruletype) {
            this.name = name;
            this.a = gamerules_enumgameruletype;
            this.b = s;
        }

        public GameRuleValue a() {
            return new GameRuleValue(this.b, this.a);
        }

        public EnumGameRuleType b() {
            return this.a;
        }
    }
}
