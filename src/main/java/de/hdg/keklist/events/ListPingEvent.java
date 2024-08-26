package de.hdg.keklist.events;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import de.hdg.keklist.Keklist;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class ListPingEvent implements Listener {

    public static final HashMap<String, Long> pingedIps = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onListPing(@NotNull PaperServerListPingEvent event) {

        if (Keklist.getInstance().getConfig().getBoolean("general.require-server-list-before-join"))
            pingedIps.put(event.getAddress().getHostAddress(), System.currentTimeMillis()); // Also save time of the last ping

        // Blacklist motd
        if (Keklist.getInstance().getConfig().getBoolean("blacklist.enabled") && Keklist.getInstance().getConfig().getBoolean("blacklist.change-motd")) {
            boolean isBlacklisted = false;

            try {
                isBlacklisted = Keklist.getDatabase().onQuery("SELECT 1 FROM blacklistMotd WHERE ip = ?", event.getAddress().getHostAddress()).next();
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (isBlacklisted) {
                event.setMaxPlayers(69);
                event.setNumPlayers(420);

                event.getListedPlayers().clear();
                event.getListedPlayers().add(new PaperServerListPingEvent.ListedPlayerInfo("Kek", UUID.randomUUID()));

                event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.BLACKLISTED)));


                if (!Keklist.getInstance().getConfig().getString("blacklist.icon-file").equals("default")) {
                    if (!new File(Keklist.getInstance().getConfig().getString("blacklist.icon-file")).exists()) {
                        Keklist.getInstance().getLogger().warning(Keklist.getTranslations().get("blacklist.icon.error"));
                    } else {
                        try {
                            event.setServerIcon(Keklist.getInstance().getServer().loadServerIcon(new File(Keklist.getInstance().getConfig().getString("blacklist.icon-file"))));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                }

                return;
            }
        }


        if (Keklist.getInstance().getConfig().getBoolean("whitelist.enabled") && (Keklist.getInstance().getConfig().getBoolean("whitelist.change-motd") || Keklist.getInstance().getConfig().getBoolean("whitelist.hide-online-players"))) {
            boolean isWhitelisted = false;

            try {
                isWhitelisted = Keklist.getDatabase().onQuery("SELECT 1 FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress()).next();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // No need to continue if the player is not whitelisted
            if (isWhitelisted) {

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

                    event.getListedPlayers().clear();
                    List<String> fakePlayers = Keklist.getInstance().getConfig().getStringList("whitelist.fake-players");
                    fakePlayers.forEach(player -> event.getListedPlayers().add(new PaperServerListPingEvent.ListedPlayerInfo(player, UUID.randomUUID())));
                }

                return;
            }
        }

        if (Keklist.getInstance().getConfig().getBoolean("general.enable-default-motd"))
            event.motd(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedMotd(Keklist.RandomType.NORMAL)));
    }
}