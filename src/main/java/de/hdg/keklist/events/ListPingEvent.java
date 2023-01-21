package de.hdg.keklist.events;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.sql.ResultSet;

public class ListPingEvent implements Listener {
    @EventHandler
    public void onListPing(PaperServerListPingEvent event) {
        String playerIP = event.getAddress().getHostAddress();

        boolean isBlacklisted = false;

        try {
            ResultSet resultSet = DB.onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", playerIP);
            isBlacklisted = resultSet.next();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isBlacklisted) {
            event.setMaxPlayers(69);
            event.setNumPlayers(420);
            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.BLACKLISTED)));


            //set the icon to the file in ./blacklisted.png
            if(!new File("blacklisted.jpeg").exists()) {
                System.out.println("Blacklisted.png not found!");
            }else {
                try {
                    event.setServerIcon(Keklist.getInstance().getServer().loadServerIcon(new File("blacklisted.jpeg")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }else{
            //Not blacklisted
            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.NORMAL)));

            if(!new File("logo.png").exists()) {
                System.out.println("logo.png not found!");
            }else {
                try {
                    event.setServerIcon(Keklist.getInstance().getServer().loadServerIcon(new File("logo.png")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
