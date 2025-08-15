package io.github.cspigot.feature.console;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.minecraft.server.DedicatedServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.TabCompleteEvent;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

// cSpigot - Backport from PaperCompleter
public class cSpigotConsoleCompleter implements Completer {

    private final DedicatedServer server;

    public cSpigotConsoleCompleter(DedicatedServer server) {
        this.server = server;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        CraftServer server = this.server.server;

        List<String> completions = new ArrayList<>();
        String buffer = line.line();

        AsyncTabCompleteEvent asyncEvent = new AsyncTabCompleteEvent(server.getConsoleSender(), completions, buffer, true, null);
        boolean cancelled = !asyncEvent.callEvent();
        completions = cancelled ? ImmutableList.of() : asyncEvent.getCompletions();

        if (cancelled || asyncEvent.isHandled()) {
            // Still fire sync event with the provided completions, if someone is listening
            if (!cancelled && TabCompleteEvent.getHandlerList().getRegisteredListeners().length > 0) {
                List<String> finalCompletions = completions;
                Waitable<List<String>> syncCompletions = new Waitable<List<String>>() {
                    @Override
                    protected List<String> evaluate() {
                        TabCompleteEvent syncEvent = new TabCompleteEvent(server.getConsoleSender(), buffer, finalCompletions);
                        return syncEvent.callEvent()
                                ? syncEvent.getCompletions()
                                : ImmutableList.of();
                    }
                };
                server.getServer().processQueue.add(syncCompletions);
                try {
                    completions = syncCompletions.get();
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }

            if (!completions.isEmpty()) {
                candidates.addAll(completions.stream()
                        .map(Candidate::new)
                        .collect(Collectors.toList())
                );
            }
            return;
        }

        Waitable<List<String>> waitable = new Waitable<List<String>>() {
            @Override
            protected List<String> evaluate() {
                List<String> offers = server.getCommandMap().tabComplete(server.getConsoleSender(), buffer);
                if (offers == null) {
                    offers = ImmutableList.of();
                }

                TabCompleteEvent syncEvent = new TabCompleteEvent(server.getConsoleSender(), buffer, offers);
                return syncEvent.callEvent()
                        ? syncEvent.getCompletions()
                        : ImmutableList.of();
            }
        };
        server.getServer().processQueue.add(waitable);
        try {
            List<String> offers = waitable.get(); // wait until completions get processed on main thread
            // cSpigot start - fix NPE while tab complete command in console
            if (offers == null) {
                return;
            }
            // start end

            offers.stream()
                    .map(Candidate::new)
                    .forEach(candidates::add);
        } catch (ExecutionException e) {
            this.server.server.getLogger().log(Level.WARNING, "Unhandled exception when tab completing", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
