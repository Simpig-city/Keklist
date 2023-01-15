package de.hdg.keklist.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PreLoginKickEvent implements Listener {
    
    @EventHandler(ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event){
        System.out.println("Player tried to join the server!");

        //Jonas?
        //SAGE HALLO BIN DA

    }

}
