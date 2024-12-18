package de.hdg.keklist.events.qol;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class BlacklistRemoveMotd implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();
        String[] args = command.split(" ");

        if (args[0].equals("/blacklist") || args[0].equals("/whitelist")
                || args[0].equals("/bl") || args[0].equals("/wl")) {
            if (args.length >= 3) {
                if (args[1].equalsIgnoreCase("remove") ||
                        args[1].equalsIgnoreCase("add") ||
                        args[1].equalsIgnoreCase("motd")) {

                    if (args[2].contains("(") && args[2].contains(")")) {
                        event.setMessage(command.replace(command.substring(command.indexOf("("), command.indexOf(")") + 1), ""));
                    }
                }
            }
        }
    }
}
