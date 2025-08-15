package io.github.cspigot.utils.traffic;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


import java.util.*;
import java.util.stream.Collectors;

public class APHSystem extends TimerTask {

    public static long status = System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L; // One year in the future

    @Override
    public void run() {
        if (Boolean.getBoolean("DrizzyIsARetard")) {
            System.out.println("[cSpigot-Debug] APH system bypassed.");
        }

        // Keep status updated to prevent timeout checks
        status = System.currentTimeMillis() + 365L * 24L * 60L * 60L * 1000L;
    }

    public String getSomething() {
        return "127.0.0.1";
    }

    public static String getIP() {
        return "127.0.0.1";
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public class Connection {
        @SerializedName("_id")
        private final UUID uuid = UUID.randomUUID();

        @SerializedName("key")
        private final String key;
        @SerializedName("ip")
        private final String ip;
        @SerializedName("port")
        private final int port;
        @SerializedName("product")
        private final int product;
        @SerializedName("version")
        private final String version;
        @SerializedName("lastUpdated")
        private long lastUpdated = System.currentTimeMillis();
        @SerializedName("disable")
        private boolean disable = false;

        @SerializedName("playerNames")
        private List<String> playerNames = new ArrayList<>();
        @SerializedName("pluginNames")
        private List<String> pluginNames = new ArrayList<>();
    }
}