package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.WebhookManager;
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

    private final FileConfiguration config = Keklist.getInstance().getConfig();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (config.getBoolean("blacklist.enabled")) {
            ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getUniqueId().toString());
            ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getRawAddress().getHostName());

            boolean isIpBanned = false;
            boolean isUserBanned = false;

            try {
                if (rsIp.next())
                    isIpBanned = true;

                if (rsUser.next())
                    isUserBanned = true;

                if (isUserBanned || isIpBanned) {
                    if (config.getBoolean("blacklist.allow-join-with-admin")) {
                        for (Player player : Keklist.getInstance().getServer().getOnlinePlayers()) {
                            if (player.hasPermission(config.getString("blacklist.admin-permission"))) {
                                return;
                            }
                        }
                    }

                    if (config.getBoolean("blacklist.limbo")) {
                        Keklist.getInstance().sendUserToLimbo(event.getUniqueId()); //This might fail, so we need to handle this in the login event
                        return;
                    }

                    if (isIpBanned)
                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getRawAddress().getHostName(), rsIp.getString("byPlayer"), null, System.currentTimeMillis());

                    if (isUserBanned)
                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getUniqueId().toString(), rsIp.getString("byPlayer"), null, System.currentTimeMillis());

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (config.getBoolean("whitelist.enabled")) {
            ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getUniqueId().toString());
            ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if (!rsUser.next()) {
                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getUniqueId().toString(), rsIp.getString("byPlayer"), System.currentTimeMillis());

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else
                    return;

                if (!rsIp.next()) {
                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getRawAddress().getHostAddress(), rsUser.getString("byPlayer"), System.currentTimeMillis());

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        if (config.getBoolean("blacklist.enabled")) {
            ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getAddress().getHostAddress());

            boolean isIpBanned = false;
            boolean isUserBanned = false;

            try {
                if (rsIp.next())
                    isIpBanned = true;

                if (rsUser.next())
                    isUserBanned = true;

                if (isIpBanned || isUserBanned) {
                    if (config.getBoolean("blacklist.allow-join-with-admin")) {
                        for (Player player : Keklist.getInstance().getServer().getOnlinePlayers()) {
                            if (player.hasPermission(config.getString("blacklist.admin-permission"))) {
                                return;
                            }
                        }
                    }

                    if (config.getBoolean("blacklist.limbo")) {
                        Keklist.getInstance().sendUserToLimbo(event.getPlayer()); //Might fail, so we need to handle this in the login event
                        return;
                    }

                    if (isIpBanned)
                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getAddress().getHostName(), rsIp.getString("byPlayer"), null, System.currentTimeMillis());

                    if (isUserBanned)
                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getPlayer().getName(), rsIp.getString("byPlayer"), null, System.currentTimeMillis());

                    event.disallow(PlayerLoginEvent.Result.KICK_BANNED, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (config.getBoolean("whitelist.enabled")) {
            ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress());

            try {
                if (!rsUser.next()) {
                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getPlayer().getName(), rsUser.getString("byPlayer"), System.currentTimeMillis());

                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else
                    return;

                if (!rsIp.next()) {
                    Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getAddress().getHostAddress(), rsIp.getString("byPlayer"), System.currentTimeMillis());

                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (config.getBoolean("blacklist.enabled")) {
            ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
            ResultSet rsIp = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getPlayer().getAddress().getAddress().getHostAddress());

            try {
                if (rsUser.next() || rsIp.next()) {
                    event.joinMessage(Component.empty());
                    Keklist.getInstance().sendUserToLimbo(event.getPlayer());

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> {
                        //Keklist.getInstance().sendUserToLimbo(event.getPlayer());
                        event.getPlayer().kick(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)), PlayerKickEvent.Cause.BANNED);
                    }, 10L);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
