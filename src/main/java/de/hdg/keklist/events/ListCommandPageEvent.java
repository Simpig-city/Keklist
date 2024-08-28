package de.hdg.keklist.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class ListCommandPageEvent implements Listener {

    // As little as possible, as much as necessary <3
    @EventHandler
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (event.getMessage().trim().startsWith("/whitelist list") ||
                event.getMessage().trim().startsWith("/blacklist list")) {
            String[] args = event.getMessage().split(" ");

            try {
                if (args.length < 3) {
                    event.setMessage(event.getMessage() + " 1");
                } else if (Integer.parseInt(args[2]) < 1) {
                    event.setMessage(event.getMessage().replace(args[2], "1"));
                }
            } catch (NumberFormatException e) {
                event.setMessage(event.getMessage() + " 1");
            }
        }
    }
}
