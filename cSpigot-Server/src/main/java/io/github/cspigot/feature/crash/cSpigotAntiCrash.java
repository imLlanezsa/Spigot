package io.github.cspigot.feature.crash;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import lombok.experimental.UtilityClass;
import net.minecraft.server.IChatBaseComponent;
import net.minecraft.server.PacketPlayInFlying;

@UtilityClass
public class cSpigotAntiCrash {
    public boolean checkPosition(final PacketPlayInFlying flying) {
        final double[] chunkValues = new double[]{flying.getX(), flying.getZ()};
        final double[] allValues = new double[]{flying.getX(), flying.getY(), flying.getZ(), flying.getPitch(), flying.getYaw()};

        for (double moveValue : chunkValues) {
            if (moveValue > 29999999 || moveValue < -29999999) return true;
        }

        for (double allValue : allValues) {
            if (Double.isNaN(allValue) || Double.isInfinite(allValue)) return true;
        }

        return !Float.isInfinite(flying.getYaw()) && !Float.isInfinite(flying.getPitch());
    }

    public boolean checkSignExploit(String[] lines) {
        try {
            for ( String s : lines ) {
                JsonElement json = IChatBaseComponent.ChatSerializer.a.fromJson(s, JsonElement.class);

                if (json.isJsonPrimitive()) {
                    JsonPrimitive jsonPrimitive = (JsonPrimitive) json;
                    return !jsonPrimitive.isString();
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }
}