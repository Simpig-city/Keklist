package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.ResultSet;

public class PreLoginKickEvent implements Listener {

    FileConfiguration config = Keklist.getInstance().getConfig();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event){
        if(config.getBoolean("blacklist.enabled")){
            ResultSet rsUser = DB.onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getAddress().getHostName());

            try {
                if(rsUser.next() || rsIp.next()){
                    if(config.getBoolean("blacklist.allow-join-with-admin")){
                        for(Player player : Keklist.getInstance().getServer().getOnlinePlayers()){
                            if(player.hasPermission(config.getString("blacklist.admin-permission"))){
                                return;
                            }
                        }
                    }

                    if(config.getBoolean("blacklist.limbo")){
                        Keklist.getInstance().sendUserToLimbo(event.getUniqueId());
                        return;
                    }

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(config.getBoolean("whitelist")){
            ResultSet rsUser = DB.onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if(!rsUser.next()){
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }else
                    return;

                if(rsIp.next()){
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event){
        if(config.getBoolean("blacklist.enabled")){
            ResultSet rsUser = DB.onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if(rsUser.next() || rsIp.next()){
                    if(config.getBoolean("blacklist.allow-join-with-admin")){
                        for(Player player : Keklist.getInstance().getServer().getOnlinePlayers()){
                            if(player.hasPermission(config.getString("blacklist.admin-permission"))){
                                return;
                            }
                        }
                    }

                    if(config.getBoolean("blacklist.limbo")){
                        Keklist.getInstance().sendUserToLimbo(event.getPlayer());
                        return;
                    }

                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(config.getBoolean("whitelist")){
            ResultSet rsUser = DB.onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = DB.onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if(!rsUser.next()){
                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }else
                    return;

                if(rsIp.next()){
                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
