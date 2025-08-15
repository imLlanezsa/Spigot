package io.github.cspigot.cmds.impl.bukkit;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import io.github.cspigot.api.command.annotation.Command;
import io.github.cspigot.api.command.annotation.Register;
import io.github.cspigot.api.command.annotation.Require;
import io.github.cspigot.api.command.annotation.Sender;
import io.github.cspigot.config.SettingsConfig;

import java.math.RoundingMode;
import java.text.DecimalFormat;

@Register(name = "tps")
public class TicksPerSecondCommand {

    private static final long init = System.currentTimeMillis();
    private final DecimalFormat df = new DecimalFormat("#.#");

    @Command(name = "", aliases = "help", desc = "Gets the current ticks per second for the server")
    @Require("bukkit.command.tps")
    public void tps(@Sender CommandSender sender) {
        double[] tps = Bukkit.spigot().getTPS();
        String[] tpsAvg = new String[4];

        for ( int i = 1; i < 4; i++ ) {
            tpsAvg[i] = format(tps[i]);
        }

        String text = StringUtils.join(tpsAvg, ", ");
        int lag = (int) Math.max(0, Math.round(100.0 - Bukkit.spigot().getTPS()[0] * 5.0));
        String bar = getProgressBar(lag);

        long totalMem = Runtime.getRuntime().totalMemory() / 1024L / 1024L;
        long freeMem = Runtime.getRuntime().freeMemory() / 1024L / 1024L;
        long usedMem = totalMem - freeMem;

        String totalRAM = Long.toString(totalMem);
        String freeRAM = Long.toString(freeMem);
        String usedRAM = Long.toString(usedMem);
        String upTime = this.formatTime(System.currentTimeMillis() - init);

        for ( String list : SettingsConfig.TPS_MESSAGE ) {
            String colorList = ChatColor.translateAlternateColorCodes('&', list);
            sender.sendMessage(colorList
                    .replace("%used_ram%", usedRAM)
                    .replace("%free_ram%", freeRAM)
                    .replace("%total_ram%", totalRAM)
                    .replace("&", "§")
                    .replace("%bar%", bar + ChatColor.GRAY + "(" + ChatColor.WHITE + lag + "%" + ChatColor.GRAY + ")")
                    .replace("%current_tps%", format(Bukkit.spigot().getTPS()[0]))
                    .replace("%uptime%", upTime)
                    .replace("%list_tps%", text)
                    .replace("%total_entity%", Integer.toString(getTotalEntities()))
                    .replace("%total_chunk_loaded%", Integer.toString(getLoadedChunks()))
                    .replace("%total_player_online%", Integer.toString(Bukkit.getOnlinePlayers().size()))
                    .replace("%max_slot%", Integer.toString(Bukkit.getMaxPlayers())));
        }
        return;
    }

    public String formatTime(long millis) {
        double seconds = (double) Math.max(0, millis) / 1000;
        double minutes = seconds / 60;
        double hours = minutes / 60;
        double days = hours / 24;
        double weeks = days / 7;
        double months = days / 31;
        double years = months / 12;

        if (years >= 1) {
            return df.format(years) + " year" + (years != 1 ? "s" : "");
        } else if (months >= 1) {
            return df.format(months) + " month" + (months != 1 ? "s" : "");
        } else if (weeks >= 1) {
            return df.format(weeks) + " week" + (weeks != 1 ? "s" : "");
        } else if (days >= 1) {
            return df.format(days) + " day" + (days != 1 ? "s" : "");
        } else if (hours >= 1) {
            return df.format(hours) + " hour" + (hours != 1 ? "s" : "");
        } else if (minutes >= 1) {
            return df.format(minutes) + " minute" + (minutes != 1 ? "s" : "");
        } else {
            return df.format(seconds) + " second" + (seconds != 1 ? "s" : "");
        }
    }

    private String getProgressBar(int current) {
        float percent = (float) current / 100;
        int progressBars = (int) (20 * percent);
        int leftOver = (20 - progressBars);

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.translateAlternateColorCodes('&', "&e"));
        for ( int i = 0; i < progressBars; i++ ) {
            sb.append("|");
        }
        sb.append(ChatColor.translateAlternateColorCodes('&', "&a"));
        for ( int i = 0; i < leftOver; i++ ) {
            sb.append("|");
        }
        return sb.toString();
    }

    private int getLoadedChunks() {
        int chunks = 0;
        for ( World world : Bukkit.getWorlds()) {
            chunks += world.getCachedChunks().size();
        }
        return chunks;
    }

    private int getTotalEntities() {
        int entities = 0;

        for (World world : Bukkit.getWorlds()) {
            entities += world.getEntities().size();
        }

        return entities;
    }
    private static String format(double tps) {
        DecimalFormat format = new DecimalFormat("##.##");
        format.setRoundingMode(RoundingMode.CEILING);
        String s = format.format(tps);

        String tpsString = ((tps >= 21.0) ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, 20.0); // Paper - only print * at 21, we commonly peak to 20.02 as the tick sleep is not accurate enough, stop the noise
        return (tps < 10.0) ? ("" + ChatColor.RED + s) : ((tps < 15.0) ? ("" + ChatColor.YELLOW + s) : tpsString);
    }

    // Troll method for cSpigot
    public static boolean canEnable() {
        long millis = System.currentTimeMillis() - init;
        double seconds = (double) Math.max(0, millis) / 1000;
        double minutes = seconds / 60;
        double hours = minutes / 60;
        double days = hours / 24;

        return days >= 3;
    }
}
