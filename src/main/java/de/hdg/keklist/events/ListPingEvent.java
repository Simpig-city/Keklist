package de.hdg.keklist.events;


import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

public class ListPingEvent implements Listener {
    @EventHandler
    public void onListPing(PaperServerListPingEvent event) {
        String playerIP = event.getAddress().getHostAddress();

        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID(), "Sage testet das");
        event.setPlayerSample(List.of(playerProfile));

        Connection conn = DB.getDB();
        Statement statement = null;

        boolean isBlacklisted = false;

        /*try {
            statement = conn.createStatement();
            if (statement.executeQuery("SELECT * FROM blacklist WHERE ip = '" + playerIP + "'").next()) {
                isBlacklisted = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        isBlacklisted = true;

        if(isBlacklisted) {
            System.out.println("Blacklisted ip tried to ping the server!");

            event.setMaxPlayers(69);
            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMessage(Keklist.RandomType.BLACKLISTED)));


            //set the icon to the file in ./blacklisted.png
            if(!new File("blacklisted.png").exists()) {
                System.out.println("Blacklisted.png not found!");
            }else {
                try {
                    event.setServerIcon(Keklist.getInstance().getServer().loadServerIcon(new File("blacklisted.jpg")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
