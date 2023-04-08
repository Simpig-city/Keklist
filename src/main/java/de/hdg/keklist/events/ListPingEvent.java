package de.hdg.keklist.events;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import de.hdg.keklist.Keklist;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.sql.ResultSet;
import java.util.List;
import java.util.Random;

public class ListPingEvent implements Listener {

    //May be customizable in the future
    private final List<String> fakePlayers = List.of("SageSphinx63920", "hdgamer1404Jonas", "SirSchoki", "Kekus", "KlexiKeks", "Oldbear200", "hlf11", "Vierkuhd", "Mommy2341", "dDdBds");

    @EventHandler
    public void onListPing(PaperServerListPingEvent event) {
        String playerIP = event.getAddress().getHostAddress();

        boolean isBlacklisted = false;

        try {
            ResultSet resultSet = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", playerIP);
            isBlacklisted = resultSet.next();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isBlacklisted && Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
            event.setMaxPlayers(69);
            event.setNumPlayers(420);

            event.getPlayerSample().clear();
            event.getPlayerSample().add(Bukkit.createProfile("Kek"));

            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.BLACKLISTED)));

            //set the icon to the file in ./blacklisted.png
            if (!new File(Keklist.getInstance().getConfig().getString("blacklist.icon-file")).exists()) {
                Keklist.getInstance().getLogger().warning("Could not find the blacklisted icon file!");
            } else {
                try {
                    event.setServerIcon(Keklist.getInstance().getServer().loadServerIcon(new File(Keklist.getInstance().getConfig().getString("blacklist.icon-file"))));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (Keklist.getInstance().getConfig().getBoolean("whitelist.enabled") && (Keklist.getInstance().getConfig().getBoolean("whitelist.change-motd") || Keklist.getInstance().getConfig().getBoolean("whitelist.hide-online-players"))) {
            if (Keklist.getInstance().getConfig().getBoolean("whitelist.change-motd"))
                event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.WHITELISTED)));

            //May be customizable in the future
            if (Keklist.getInstance().getConfig().getBoolean("whitelist.hide-online-players")) {
                Random rnd = new Random();
                int players = rnd.nextInt(0, 10);

                event.setMaxPlayers(rnd.nextInt(20, 40));
                event.setNumPlayers(players);

                event.getPlayerSample().clear();
                for (int i = 0; i < players; i++){
                    event.getPlayerSample().add(Bukkit.createProfile(fakePlayers.get(rnd.nextInt(0, fakePlayers.size()))));
                }

            }
        } else {
            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.NORMAL)));
        }
    }
}
