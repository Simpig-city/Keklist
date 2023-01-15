package de.hdg.keklist.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PreLoginKickEvent implements Listener {
    
    @EventHandler(ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event){
        //TODO : Manage white and black list
    }

}
