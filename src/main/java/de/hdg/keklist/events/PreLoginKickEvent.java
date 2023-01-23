package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.ResultSet;

public class PreLoginKickEvent implements Listener {

    FileConfiguration config = Keklist.getInstance().getConfig();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (config.getBoolean("blacklist.enabled")) {
            ResultSet rsUser = DB.onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getAddress().getHostName());

            try {
                if (rsUser.next() || rsIp.next()) {
                    if (config.getBoolean("blacklist.allow-join-with-admin")) {
                        for (Player player : Keklist.getInstance().getServer().getOnlinePlayers()) {
                            if (player.hasPermission(config.getString("blacklist.admin-permission"))) {
                                return;
                            }
                        }
                    }

                    if (config.getBoolean("blacklist.limbo")) {
                        Keklist.getInstance().sendUserToLimbo(event.getUniqueId()); //This might fail so we need to handle this in the login event
                        return;
                    }

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (config.getBoolean("whitelist")) {
            ResultSet rsUser = DB.onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if (!rsUser.next()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else
                    return;

                if (!rsIp.next()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (config.getBoolean("blacklist.enabled")) {
            ResultSet rsUser = DB.onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if (rsUser.next() || rsIp.next()) {
                    if (config.getBoolean("blacklist.allow-join-with-admin")) {
                        for (Player player : Keklist.getInstance().getServer().getOnlinePlayers()) {
                            if (player.hasPermission(config.getString("blacklist.admin-permission"))) {
                                return;
                            }
                        }
                    }

                    if (config.getBoolean("blacklist.limbo")) {
                        Keklist.getInstance().sendUserToLimbo(event.getPlayer()); //Might fail so we need to handle this in the login event
                        return;
                    }

                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (config.getBoolean("whitelist")) {
            ResultSet rsUser = DB.onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if (!rsUser.next()) {
                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else
                    return;

                if (!rsIp.next()) {
                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (config.getBoolean("blacklist.enabled")) {
            ResultSet rsUser = DB.onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());

            try {
                if (rsUser.next()) {
                    event.joinMessage(Component.empty());
                    Keklist.getInstance().sendUserToLimbo(event.getPlayer());

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            //Keklist.getInstance().sendUserToLimbo(event.getPlayer());
                            event.getPlayer().kick(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)), PlayerKickEvent.Cause.BANNED);
                        }
                    }, 10L);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
