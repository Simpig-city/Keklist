package de.hdg.keklist.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.events.feats.ListPingEvent;
import de.hdg.keklist.extentions.WebhookManager;
import de.hdg.keklist.util.IpUtil;
import lombok.Cleanup;
import net.kyori.adventure.text.Component;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;

public class PreLoginKickEvent implements Listener {

    private final FileConfiguration config = Keklist.getInstance().getConfig();
    private final OkHttpClient client = new OkHttpClient();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT 1 FROM lastSeen WHERE uuid = ?", event.getUniqueId().toString())) {
            if (!rs.getResultSet().next()) {
                Keklist.getDatabase().onUpdate("INSERT INTO lastSeen (uuid, ip, lastSeen) VALUES (?, ?, ?)", event.getUniqueId().toString(), event.getAddress().getHostAddress(), System.currentTimeMillis());
            } else {
                Keklist.getDatabase().onUpdate("UPDATE lastSeen SET ip = ?, lastSeen = ? WHERE uuid = ?", event.getAddress().getHostAddress(), System.currentTimeMillis(), event.getUniqueId().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!config.getList("blacklist.countries").isEmpty() ||
                !config.getList("blacklist.continents").isEmpty() ||
                !config.getBoolean("ip.proxy-allowed")) {

            IpUtil.IpData data = new IpUtil(event.getAddress().getHostAddress()).getIpData().join(); // We can use .join() because we are in an async event

            if (config.getList("blacklist.continents").stream().map(String::valueOf).anyMatch(data.continentCode()::equalsIgnoreCase)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.CONTINENT)));
                return;
            }

            if (config.getList("blacklist.countries").stream().map(String::valueOf).anyMatch(data.countryCode()::equalsIgnoreCase)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.COUNTRY)));
                return;
            }

            if (config.getBoolean("ip.proxy-allowed") && data.proxy()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.PROXY)));
                return;
            }
        }

        if (config.getBoolean("general.require-server-list-before-join")) {
            if (ListPingEvent.pingedIps.containsKey(event.getAddress().getHostAddress())) {
                if (ListPingEvent.pingedIps.get(event.getAddress().getHostAddress()) > System.currentTimeMillis() - 40000) { // Can not find how often the minecraft server pings the server list, so I just use 40 seconds
                    ListPingEvent.pingedIps.remove(event.getAddress().getHostAddress());
                } else {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Keklist.getInstance().getMiniMessage().deserialize(config.getString("messages.kick.server-list")));
                    return;
                }
            } else {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Keklist.getInstance().getMiniMessage().deserialize(config.getString("messages.kick.server-list")));
                return;
            }
        }

        if (config.getBoolean("blacklist.enabled")) {
            try (DB.QueryResult rsUser = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getUniqueId().toString());
                 DB.QueryResult rsIp = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getRawAddress().getHostName())) {

                boolean isIpBanned = rsIp.getResultSet().next();
                boolean isUserBanned = rsUser.getResultSet().next();

                if (isUserBanned || isIpBanned) {
                    if (config.getBoolean("blacklist.allow-join-with-admin")) {
                        for (Player player : Keklist.getInstance().getServer().getOnlinePlayers()) {
                            if (player.hasPermission(config.getString("blacklist.admin-permission"))) {
                                return;
                            }
                        }
                    }

                    if (config.getBoolean("blacklist.limbo")) {
                        Keklist.getInstance().sendUserToLimbo(event.getUniqueId()); //This might fail, so we need to handle this again in the login event
                        return;
                    }

                    if (isIpBanned) {
                        if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                            Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", event.getRawAddress().getHostName())), "keklist.notify.kicked");

                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getRawAddress().getHostName(), rsIp.getResultSet().getString("byPlayer"), null, System.currentTimeMillis());

                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                        return;
                    }

                    if (isUserBanned) {
                        if (Keklist.getInstance().getFloodgateApi() != null) {
                            if (Keklist.getInstance().getFloodgateApi().isFloodgatePlayer(event.getUniqueId())) {
                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, Keklist.getInstance().getFloodgateApi().getPlayer(event.getUniqueId()).getUsername(), rsUser.getResultSet().getString("byPlayer"), null, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", Keklist.getInstance().getFloodgateApi().getPlayer(event.getUniqueId()).getUsername())), "keklist.notify.kicked");
                            } else {
                                Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                                client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getAddress().getHostAddress(), rsUser.getResultSet().getString("byPlayer"), System.currentTimeMillis()));
                            }
                        } else {
                            Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                            client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getAddress().getHostAddress(), rsUser.getResultSet().getString("byPlayer"), System.currentTimeMillis()));
                        }
                    }

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (config.getBoolean("whitelist.enabled")) {
            try (DB.QueryResult rsUser = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", event.getUniqueId().toString());
                 DB.QueryResult rsIp = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", event.getAddress().getHostAddress())) {


                boolean isWhitelistedUser = rsUser.getResultSet().next();
                boolean isWhitelistedIp = rsIp.getResultSet().next();

                int level = 0;

                // Ip level is lower ranked as user level
                try (DB.QueryResult rsLevelUser = Keklist.getDatabase().onQuery("SELECT whitelistLevel FROM whitelistLevel WHERE entry = ?", event.getUniqueId().toString());
                     DB.QueryResult rsLevelIp = Keklist.getDatabase().onQuery("SELECT whitelistLevel FROM whitelistLevel WHERE entry = ?", event.getAddress().getHostAddress())) {
                    if (rsLevelIp.getResultSet().next())
                        level = rsLevelIp.getResultSet().getInt("whitelistLevel");

                    if (rsLevelUser.getResultSet().next())
                        level = rsLevelUser.getResultSet().getInt("whitelistLevel");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }


                if (!isWhitelistedIp) { // Is not on the ip whitelist
                    @Cleanup DB.QueryResult rsDDNS = Keklist.getDatabase().onQuery("SELECT * FROM whitelistDomain");

                    while (rsDDNS.getResultSet().next()) {
                        InetAddress address = InetAddress.getByName(rsDDNS.getResultSet().getString("domain"));

                        if (address.getHostAddress().equals(event.getAddress().getHostAddress()) ||
                                address.getHostName().equals(event.getAddress().getHostName()) ||
                                address.getHostAddress().equals(event.getAddress().getHostName()) ||
                                address.getHostName().equals(event.getAddress().getHostAddress())) {
                            return;
                        }
                    }

                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", event.getRawAddress().getHostName())), "keklist.notify.kicked");

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED))); // Then disallow them
                } else {
                    // Is on the whitelist so check for level

                    // Level is too low
                    if (level < Keklist.getInstance().getConfig().getInt("whitelist.level")) {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getConfig().getString("messages.kick.whitelisted-level")));
                    } else
                        return; // Level passes
                }


                if (!isWhitelistedUser) { // Is not on the ip whitelist and not on the user whitelist
                    if (Keklist.getInstance().getFloodgateApi() != null) {
                        if (!Keklist.getInstance().getFloodgateApi().isFloodgateId(event.getUniqueId())) {
                            Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                            client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getAddress().getHostAddress(), null, System.currentTimeMillis()));
                        } else {
                            if (Keklist.getWebhookManager() != null)
                                Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getName() + " (" + event.getAddress().getHostAddress() + ")", null, System.currentTimeMillis());

                            if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", event.getName())), "keklist.notify.kicked");
                        }
                    } else {
                        Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                        client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getAddress().getHostAddress(), null, System.currentTimeMillis()));
                    }

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else {
                    // Check for level
                    if (level < Keklist.getInstance().getConfig().getInt("whitelist.level")) {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getConfig().getString("messages.kick.whitelisted-level")));
                    } else
                        event.allow(); // Allow as the connection might be disallowed before as result of the whitelist check -> User > Ip
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        // No need to check for the lastSeenIp table, because we already did this in the prelogin event
        Keklist.getDatabase().onUpdate("UPDATE lastSeen SET protocolId = ?, brand = ? WHERE uuid = ?", event.getPlayer().getProtocolVersion(), (event.getPlayer().getClientBrandName() == null ? "unknown" : event.getPlayer().getClientBrandName()), event.getPlayer().getUniqueId().toString());

        if (config.getBoolean("blacklist.enabled")) {
            try (DB.QueryResult rsUser = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", event.getPlayer().getUniqueId().toString());
                 DB.QueryResult rsIp = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", event.getPlayer().getAddress().getAddress().getHostAddress())) {

                if (rsUser.getResultSet().next() || rsIp.getResultSet().next()) {
                    event.joinMessage(Component.empty());
                    Keklist.getInstance().sendUserToLimbo(event.getPlayer());

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> event.getPlayer().kick(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.BLACKLISTED)), PlayerKickEvent.Cause.BANNED), 10L);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class WebhooknameCallback implements Callback {
        private final WebhookManager.EVENT_TYPE type;
        private final String addedBy;
        private final long unixTime;
        private final String ip;

        public WebhooknameCallback(@NotNull WebhookManager.EVENT_TYPE type, @NotNull String ip, @Nullable String by, long unixTime) {
            this.type = type;
            this.addedBy = by;
            this.unixTime = unixTime;
            this.ip = ip;
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            String body = response.body().string();

            if (checkForGoodResponse(body) != null) {
                Keklist.getInstance().getLogger().severe(checkForGoodResponse(body));
            } else {
                JsonElement responseElement = JsonParser.parseString(body);
                String name = responseElement.getAsJsonObject().get("name").getAsString();

                switch (type) {
                    case BLACKLIST_KICK -> {
                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, name + " (" + ip + ")", addedBy, null, unixTime);

                        if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                            Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", name)), "keklist.notify.kicked");
                    }
                    case WHITELIST_KICK -> {
                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, name + " (" + ip + ")", null, unixTime);

                        if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                            Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", name)), "keklist.notify.kicked");
                    }
                }
            }
        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            Keklist.getInstance().getLogger().warning(Keklist.getTranslations().get("discord.http.namefetch", e.getMessage()));
        }

    }

    @Nullable
    private String checkForGoodResponse(@NotNull String response) {
        JsonElement responseElement = JsonParser.parseString(response);

        if (!responseElement.isJsonNull()) {
            if (responseElement.getAsJsonObject().get("error") != null ||
                    !responseElement.getAsJsonObject().has("id") ||
                    !responseElement.getAsJsonObject().has("name")) {
                return Keklist.getTranslations().get("discord.http.uuid-error", responseElement.getAsJsonObject().get("error").getAsString());
            }
        } else {
            return Keklist.getTranslations().get("http.null-response");
        }

        return null;
    }
}
