package de.hdg.keklist.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.extentions.WebhookManager;
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
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.ResultSet;

public class PreLoginKickEvent implements Listener {

    private final FileConfiguration config = Keklist.getInstance().getConfig();
    private final OkHttpClient client = new OkHttpClient();

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

                    if (isIpBanned) {
                        if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                            Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", event.getRawAddress().getHostName())), "keklist.notify.kicked");

                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getRawAddress().getHostName(), rsIp.getString("byPlayer"), null, System.currentTimeMillis());
                    }

                    if (isUserBanned) {
                        if (Keklist.getInstance().getFloodgateApi() != null) {
                            if (Keklist.getInstance().getFloodgateApi().isFloodgatePlayer(event.getUniqueId())) {
                                if (Keklist.getWebhookManager() != null)
                                    Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, Keklist.getInstance().getFloodgateApi().getPlayer(event.getUniqueId()).getUsername(), rsUser.getString("byPlayer"), null, System.currentTimeMillis());

                                if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                                    Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", Keklist.getInstance().getFloodgateApi().getPlayer(event.getUniqueId()).getUsername())), "keklist.notify.kicked");
                            } else {
                                Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                                client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, rsUser.getString("byPlayer"), System.currentTimeMillis()));
                            }
                        } else {
                            Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                            client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, rsUser.getString("byPlayer"), System.currentTimeMillis()));
                        }
                    }

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
                    Request request = new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/profile/" + event.getUniqueId()).build();
                    client.newCall(request).enqueue(new PreLoginKickEvent.WebhooknameCallback(WebhookManager.EVENT_TYPE.WHITELIST_KICK, null, System.currentTimeMillis()));

                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else
                    return;

                if (!rsIp.next()) {
                    if (Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getRawAddress().getHostAddress(), null, System.currentTimeMillis());

                    if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                        Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", event.getRawAddress().getHostName())), "keklist.notify.kicked");

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

                    if (isIpBanned && Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, event.getAddress().getHostName(), rsIp.getString("byPlayer"), null, System.currentTimeMillis());

                    if (isUserBanned && Keklist.getWebhookManager() != null)
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
                    if (Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getPlayer().getName(), null, System.currentTimeMillis());

                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getInstance().getRandomizedKickMessage(Keklist.RandomType.WHITELISTED)));
                } else
                    return;

                if (!rsIp.next()) {
                    if (Keklist.getWebhookManager() != null)
                        Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, event.getAddress().getHostAddress(), null, System.currentTimeMillis());

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

    private class WebhooknameCallback implements Callback {
        private final WebhookManager.EVENT_TYPE type;
        private final String addedBy;
        private final long unixTime;

        public WebhooknameCallback(@NotNull WebhookManager.EVENT_TYPE type, @Nullable String by, long unixTime) {
            this.type = type;
            this.addedBy = by;
            this.unixTime = unixTime;
        }

        @Override
        public void onResponse(@NotNull Call call, Response response) throws IOException {
            String body = response.body().string();

            if (checkForGoodResponse(body) != null) {
                Keklist.getInstance().getLogger().severe(checkForGoodResponse(body));
            } else {
                JsonElement responseElement = JsonParser.parseString(body);
                String name = responseElement.getAsJsonObject().get("name").getAsString();

                switch (type) {
                    case BLACKLIST_KICK -> {
                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireBlacklistEvent(WebhookManager.EVENT_TYPE.BLACKLIST_KICK, name, addedBy, null, unixTime);

                        if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                            Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", name)), "keklist.notify.kicked");
                    }
                    case WHITELIST_KICK -> {
                        if (Keklist.getWebhookManager() != null)
                            Keklist.getWebhookManager().fireWhitelistEvent(WebhookManager.EVENT_TYPE.WHITELIST_KICK, name, null, unixTime);

                        if (Keklist.getInstance().getConfig().getBoolean("chat-notify"))
                            Bukkit.broadcast(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("notify.kick", name)), "keklist.notify.kicked");
                    }
                }
            }
        }

        @Override
        public void onFailure(@NotNull Call call, IOException e) {
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
