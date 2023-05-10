package de.hdg.keklist.events;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import de.hdg.keklist.Keklist;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.sql.ResultSet;
import java.util.List;
import java.util.Random;

public class ListPingEvent implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
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

            if (!new File(Keklist.getInstance().getConfig().getString("blacklist.icon-file")).exists() && !Keklist.getInstance().getConfig().getString("blacklist.icon-file").equals("default")) {
                Keklist.getInstance().getLogger().warning(Keklist.getTranslations().get("blacklist.icon.error"));
            } else {
                try {
                    if (!Keklist.getInstance().getConfig().getString("blacklist.icon-file").equals("default"))
                        event.setServerIcon(Keklist.getInstance().getServer().loadServerIcon(new File(Keklist.getInstance().getConfig().getString("blacklist.icon-file"))));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (Keklist.getInstance().getConfig().getBoolean("whitelist.enabled") && (Keklist.getInstance().getConfig().getBoolean("whitelist.change-motd") || Keklist.getInstance().getConfig().getBoolean("whitelist.hide-online-players"))) {
            if (Keklist.getInstance().getConfig().getBoolean("whitelist.change-motd"))
                event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.WHITELISTED)));

            if (Keklist.getInstance().getConfig().getBoolean("whitelist.hide-online-players")) {
                Random rnd = new Random();

                String onlineRange = Keklist.getInstance().getConfig().getString("whitelist.fake-online-range");
                int fromOnline = Integer.parseInt(onlineRange.split("-")[0].replace("-", ""));
                int toOnline = Integer.parseInt(onlineRange.split("-")[1].replace("-", ""));

                int onlinePlayers = rnd.nextInt(fromOnline, toOnline);

                String maxRange = Keklist.getInstance().getConfig().getString("whitelist.fake-max-range");
                int fromMax = Integer.parseInt(maxRange.split("-")[0].replace("-", ""));
                int toMax = Integer.parseInt(maxRange.split("-")[1].replace("-", ""));

                event.setMaxPlayers(rnd.nextInt(fromMax, toMax));
                event.setNumPlayers(onlinePlayers);

                event.getPlayerSample().clear();
                List<String> fakePlayers = Keklist.getInstance().getConfig().getStringList("whitelist.fake-players");
                fakePlayers.forEach(player -> event.getPlayerSample().add(Bukkit.createProfile(player)));
            }
        } else {
            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.NORMAL)));
        }
    }
}
