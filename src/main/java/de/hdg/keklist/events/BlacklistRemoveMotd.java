package de.hdg.keklist.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class BlacklistRemoveMotd implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event){

        String command = event.getMessage();
        String[] args = command.split(" ");

        if(args[0].equals("/blacklist")){
            if(args[1].equalsIgnoreCase("remove") && args.length >= 3){
                if(args[2].contains("(") && args[2].contains(")")){
                    event.setMessage(command.replace(command.substring(command.indexOf("("), command.indexOf(")") + 1), ""));
                }
            }

            if(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("motd") && args.length >= 3){
                if(args[2].contains("(") && args[2].contains(")")){
                    event.setMessage(command.replace(command.substring(command.indexOf("("), command.indexOf(")") + 1), ""));
                }
            }
        }
    }
}
